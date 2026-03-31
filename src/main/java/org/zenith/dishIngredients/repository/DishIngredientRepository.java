package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishIngredientRepository {

    private final DataSource dataSource;
    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    public DishIngredientRepository(DataSource dataSource,
                                    DishRepository dishRepository,
                                    IngredientRepository ingredientRepository) {
        this.dataSource = dataSource;
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public List<DishIngredient> findByDishId(int dishId) {
        String sql = """
            SELECT di.id_dish,
                   di.id_ingredient,
                   di.quantity_required,
                   di.unit,
                   d.id AS dish_id,
                   d.name AS dish_name,
                   d.dish_type,
                   d.price AS dish_price,
                   i.id AS ingredient_id,
                   i.name AS ingredient_name,
                   i.price AS ingredient_price,
                   i.category
            FROM dish_ingredient di
            JOIN dish d ON d.id = di.id_dish
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE di.id_dish = ?
            """;

        List<DishIngredient> dishIngredients = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Dish dish = mapDish(rs);
                Ingredient ingredient = mapIngredient(rs);
                double quantity = rs.getDouble("quantity_required");
                Unit unit = Unit.valueOf(rs.getString("unit"));

                dishIngredients.add(new DishIngredient(dish, ingredient, quantity, unit));
            }
            return dishIngredients;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching dish ingredients for dish id: " + dishId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public void deleteByDishId(int dishId) {
        String sql = "DELETE FROM dish_ingredient WHERE id_dish = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting dish ingredients for dish id: " + dishId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public void saveAll(int dishId, List<DishIngredient> dishIngredients) {
        String sql = "INSERT INTO dish_ingredient(id_dish, id_ingredient, quantity_required, unit) VALUES (?, ?, ?, ?::unit_type)";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishIngredient di : dishIngredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, di.getIngredient().getId());
                ps.setDouble(3, di.getQuantity());
                ps.setString(4, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving dish ingredients for dish id: " + dishId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public void updateDishIngredients(int dishId, List<DishIngredient> newDishIngredients) {
        deleteByDishId(dishId);
        if (newDishIngredients != null && !newDishIngredients.isEmpty()) {
            saveAll(dishId, newDishIngredients);
        }
    }

    public boolean isIngredientUsedInAnyDish(int ingredientId) {
        String sql = "SELECT COUNT(*) FROM dish_ingredient WHERE id_ingredient = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking if ingredient is used", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    private Dish mapDish(ResultSet rs) throws SQLException {
        Double price = rs.getObject("dish_price") != null ? rs.getDouble("dish_price") : null;
        return new Dish(
                rs.getInt("dish_id"),
                rs.getString("dish_name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                price
        );
    }

    private Ingredient mapIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("ingredient_id"),
                rs.getString("ingredient_name"),
                rs.getDouble("ingredient_price"),
                CategoryEnum.valueOf(rs.getString("category").toUpperCase())
        );
    }
}