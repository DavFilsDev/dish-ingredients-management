package org.zenith.dishIngredients.repository;

import org.springframework.stereotype.Repository;
import org.zenith.dishIngredients.entity.*;
import org.zenith.dishIngredients.utils.DBConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DBConnection dbConnection;

    public IngredientRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public List<Ingredient> findAll() {
        String sql = "SELECT id, name, price, category FROM ingredient ORDER BY id";
        Connection conn = dbConnection.getDBConnection();
        List<Ingredient> ingredients = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ingredients.add(mapIngredient(rs));
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public Ingredient findById(int id) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";
        Connection conn = dbConnection.getDBConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapIngredient(rs);
            }
            throw new RuntimeException("Ingredient not found (id=" + id + ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public Ingredient findByIdWithStockMovements(int id) {
        Ingredient ingredient = findById(id);
        ingredient.setStockMovementList(findStockMovementsByIngredientId(id, null, null));
        return ingredient;
    }

    public List<Ingredient> findPaginated(int page, int size) {
        int offset = (page - 1) * size;
        String sql = "SELECT id, name, price, category FROM ingredient ORDER BY id LIMIT ? OFFSET ?";
        Connection conn = dbConnection.getDBConnection();
        List<Ingredient> ingredients = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapIngredient(rs));
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<Ingredient> findByCriteria(String ingredientName, CategoryEnum category,
                                           String dishName, int page, int size) {
        int offset = (page - 1) * size;
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT i.id, i.name, i.price, i.category
            FROM ingredient i
            LEFT JOIN dish_ingredient di ON di.id_ingredient = i.id
            LEFT JOIN dish d ON d.id = di.id_dish
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (ingredientName != null && !ingredientName.isBlank()) {
            sql.append(" AND i.name ILIKE ?");
            params.add("%" + ingredientName + "%");
        }
        if (category != null) {
            sql.append(" AND i.category = ?::ingredient_category");
            params.add(category.name());
        }
        if (dishName != null && !dishName.isBlank()) {
            sql.append(" AND d.name ILIKE ?");
            params.add("%" + dishName + "%");
        }
        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        Connection conn = dbConnection.getDBConnection();
        List<Ingredient> ingredients = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                }
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapIngredient(rs));
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM ingredient WHERE name = ?";
        Connection conn = dbConnection.getDBConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public Ingredient save(Ingredient ingredient) {
        Connection conn = dbConnection.getDBConnection();

        try {
            if (ingredient.getId() > 0) {
                String sql = "UPDATE ingredient SET name = ?, category = ?::ingredient_category, price = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, ingredient.getName());
                    ps.setString(2, ingredient.getCategory().name());
                    ps.setDouble(3, ingredient.getPrice());
                    ps.setInt(4, ingredient.getId());
                    ps.executeUpdate();
                }
                return ingredient;
            } else {
                String sql = "INSERT INTO ingredient (name, category, price) VALUES (?, ?::ingredient_category, ?) RETURNING id";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, ingredient.getName());
                    ps.setString(2, ingredient.getCategory().name());
                    ps.setDouble(3, ingredient.getPrice());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        return new Ingredient(generatedId, ingredient.getName(), ingredient.getPrice(), ingredient.getCategory());
                    }
                }
            }
            throw new RuntimeException("Failed to save ingredient");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<Ingredient> createAll(List<Ingredient> ingredients) {
        Connection conn = dbConnection.getDBConnection();

        try {
            conn.setAutoCommit(false);

            for (int i = 0; i < ingredients.size(); i++) {
                for (int j = i + 1; j < ingredients.size(); j++) {
                    if (ingredients.get(i).getName().equalsIgnoreCase(ingredients.get(j).getName())) {
                        throw new RuntimeException("Duplicate ingredient in list: " + ingredients.get(i).getName());
                    }
                }
            }

            for (Ingredient ingredient : ingredients) {
                if (existsByName(ingredient.getName())) {
                    throw new RuntimeException("Ingredient already exists: " + ingredient.getName());
                }
            }

            String sql = "INSERT INTO ingredient (name, category, price) VALUES (?, ?::ingredient_category, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Ingredient ingredient : ingredients) {
                    ps.setString(1, ingredient.getName());
                    ps.setString(2, ingredient.getCategory().name());
                    ps.setDouble(3, ingredient.getPrice());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

            List<Ingredient> savedIngredients = new ArrayList<>();
            for (Ingredient ingredient : ingredients) {
                savedIngredients.add(findByName(ingredient.getName()));
            }
            return savedIngredients;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Rollback failed", ex);
            }
            throw new RuntimeException(e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            dbConnection.close(conn);
        }
    }

    public Ingredient findByName(String name) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE name = ?";
        Connection conn = dbConnection.getDBConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapIngredient(rs);
            }
            throw new RuntimeException("Ingredient not found: " + name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<StockMouvement> findStockMovementsByIngredientId(int ingredientId, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, quantity, type, unit, creation_datetime 
            FROM stock_movement 
            WHERE id_ingredient = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(ingredientId);

        if (from != null) {
            sql.append(" AND creation_datetime >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND creation_datetime <= ?");
            params.add(Timestamp.from(to));
        }
        sql.append(" ORDER BY creation_datetime ASC");

        Connection conn = dbConnection.getDBConnection();
        List<StockMouvement> movements = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof Timestamp) {
                    ps.setTimestamp(i + 1, (Timestamp) param);
                }
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StockValue value = new StockValue(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit")));
                StockMouvement movement = new StockMouvement(
                        rs.getInt("id"),
                        value,
                        MouvementTypeEnum.valueOf(rs.getString("type")),
                        rs.getTimestamp("creation_datetime").toInstant()
                );
                movements.add(movement);
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    public List<StockMouvement> saveStockMovements(int ingredientId, List<StockMouvement> movements) {
        if (movements == null || movements.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = """
            INSERT INTO stock_movement (id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?::movement_type, ?::unit_type, ?)
            RETURNING id, quantity, type, unit, creation_datetime
        """;

        Connection conn = dbConnection.getDBConnection();
        List<StockMouvement> savedMovements = new ArrayList<>();

        try {
            for (StockMouvement movement : movements) {
                StockValue value = movement.getValue();
                Instant creationDateTime = movement.getCreationDateTime() != null ? movement.getCreationDateTime() : Instant.now();

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, ingredientId);
                    ps.setDouble(2, value.getQuantity());
                    ps.setString(3, movement.getType().name());
                    ps.setString(4, value.getUnit().name());
                    ps.setTimestamp(5, Timestamp.from(creationDateTime));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        StockValue savedValue = new StockValue(rs.getDouble("quantity"), Unit.valueOf(rs.getString("unit")));
                        StockMouvement savedMovement = new StockMouvement(
                                rs.getInt("id"),
                                savedValue,
                                MouvementTypeEnum.valueOf(rs.getString("type")),
                                rs.getTimestamp("creation_datetime").toInstant()
                        );
                        savedMovements.add(savedMovement);
                    }
                }
            }
            return savedMovements;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.close(conn);
        }
    }

    private Ingredient mapIngredient(ResultSet rs) throws SQLException {
        return new Ingredient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                CategoryEnum.valueOf(rs.getString("category").toUpperCase())
        );
    }
}