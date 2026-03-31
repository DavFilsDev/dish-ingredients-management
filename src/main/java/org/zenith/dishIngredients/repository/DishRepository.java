package org.zenith.dishIngredients.repository;

import org.springframework.stereotype.Repository;
import org.zenith.dishIngredients.entity.*;
import org.zenith.dishIngredients.utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {

    private final DBConnection dbConnection;
    private final IngredientRepository ingredientRepository;

    public DishRepository(DBConnection dbConnection, IngredientRepository ingredientRepository) {
        this.dbConnection = dbConnection;
        this.ingredientRepository = ingredientRepository;
    }

    public List<Dish> findAllWithIngredients() {
        List<Dish> dishes = findAll();
        for (Dish dish : dishes) {
            dish.setDishIngredients(findDishIngredientsByDishId(dish.getId()));
        }
        return dishes;
    }

    public List<Dish> findAll() {
        String sql = "SELECT id, name, dish_type, price FROM dish ORDER BY id";
        Connection conn = dbConnection.getDBConnection();
        List<Dish> dishes = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dishes.add(mapDish(rs));
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public Dish findByIdWithIngredients(int id) {
        Dish dish = findById(id);
        dish.setDishIngredients(findDishIngredientsByDishId(id));
        return dish;
    }

    public Dish findById(int id) {
        String sql = "SELECT id, name, dish_type, price FROM dish WHERE id = ?";
        Connection conn = dbConnection.getDBConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapDish(rs);
            }
            throw new RuntimeException("Dish not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<DishIngredient> findDishIngredientsByDishId(int dishId) {
        String sql = """
            SELECT di.id_dish, di.id_ingredient, di.quantity_required, di.unit,
                   d.id AS dish_id, d.name AS dish_name, d.dish_type, d.price AS dish_price,
                   i.id AS ingredient_id, i.name AS ingredient_name, 
                   i.price AS ingredient_price, i.category
            FROM dish_ingredient di
            JOIN dish d ON d.id = di.id_dish
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE di.id_dish = ?
        """;

        Connection conn = dbConnection.getDBConnection();
        List<DishIngredient> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish dish = new Dish(
                        rs.getInt("dish_id"),
                        rs.getString("dish_name"),
                        DishTypeEnum.valueOf(rs.getString("dish_type")),
                        rs.getObject("dish_price") != null ? rs.getDouble("dish_price") : null
                );

                Ingredient ingredient = new Ingredient(
                        rs.getInt("ingredient_id"),
                        rs.getString("ingredient_name"),
                        rs.getDouble("ingredient_price"),
                        CategoryEnum.valueOf(rs.getString("category").toUpperCase())
                );

                result.add(new DishIngredient(dish, ingredient, rs.getDouble("quantity_required"), Unit.valueOf(rs.getString("unit"))));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public Dish save(Dish dish) {
        Connection conn = dbConnection.getDBConnection();
        int dishId;

        try {
            if (dish.getId() > 0) {
                String sql = "UPDATE dish SET name = ?, dish_type = ?::dish_type, price = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dish.getName());
                    ps.setString(2, dish.getDishType().name());
                    ps.setObject(3, dish.getSellingPrice());
                    ps.setInt(4, dish.getId());
                    ps.executeUpdate();
                }
                dishId = dish.getId();
            } else {
                String sql = "INSERT INTO dish (name, dish_type, price) VALUES (?, ?::dish_type, ?) RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dish.getName());
                    ps.setString(2, dish.getDishType().name());
                    ps.setObject(3, dish.getSellingPrice());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        dishId = rs.getInt(1);
                    } else {
                        throw new RuntimeException("Failed to insert dish");
                    }
                }
            }

            updateDishIngredients(conn, dishId, dish.getDishIngredients());

            return findByIdWithIngredients(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    private void updateDishIngredients(Connection conn, int dishId, List<DishIngredient> dishIngredients) throws SQLException {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }

        if (dishIngredients == null || dishIngredients.isEmpty()) {
            return;
        }

        String insertSql = "INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit) VALUES (?, ?, ?, ?::unit_type)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (DishIngredient di : dishIngredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, di.getIngredient().getId());
                ps.setDouble(3, di.getQuantity());
                ps.setString(4, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void associateIngredients(int dishId, List<Integer> ingredientIds) {
        Connection conn = dbConnection.getDBConnection();

        try {
            String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }

            if (ingredientIds == null || ingredientIds.isEmpty()) {
                return;
            }

            String insertSql = "INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit) VALUES (?, ?, 1.0, 'KG')";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Integer ingredientId : ingredientIds) {
                    ps.setInt(1, dishId);
                    ps.setInt(2, ingredientId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<Dish> findByIngredientName(String ingredientName) {
        String sql = """
            SELECT DISTINCT d.id, d.name, d.dish_type, d.price
            FROM dish d
            JOIN dish_ingredient di ON di.id_dish = d.id
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE i.name ILIKE ?
            ORDER BY d.id
        """;

        Connection conn = dbConnection.getDBConnection();
        List<Dish> dishes = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + ingredientName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish dish = mapDish(rs);
                dishes.add(dish);
            }

            for (Dish dish : dishes) {
                dish.setDishIngredients(findDishIngredientsByDishId(dish.getId()));
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public double calculateDishCost(int dishId) {
        String sql = "SELECT SUM(i.price * di.quantity_required) as total_cost FROM dish_ingredient di JOIN ingredient i ON i.id = di.id_ingredient WHERE di.id_dish = ?";
        Connection conn = dbConnection.getDBConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("total_cost");
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    private Dish mapDish(ResultSet rs) throws SQLException {
        Double price = rs.getObject("price") != null ? rs.getDouble("price") : null;
        return new Dish(
                rs.getInt("id"),
                rs.getString("name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                price
        );
    }
}