package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.CategoryEnum;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.StockMouvement;
import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.entity.MouvementTypeEnum;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Ingredient> findAll() {
        String sql = "SELECT id, name, price, category FROM ingredient ORDER BY id";

        List<Ingredient> ingredients = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Ingredient ingredient = mapIngredient(rs);
                ingredients.add(ingredient);
            }
            return ingredients;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all ingredients", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Ingredient findById(int id) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapIngredient(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching ingredient with id: " + id, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM ingredient WHERE name = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking ingredient existence", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Ingredient save(Ingredient ingredient) {
        String sql = """
            INSERT INTO ingredient(id, name, price, category)
            VALUES (?, ?, ?, ?::category)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                price = EXCLUDED.price,
                category = EXCLUDED.category
            RETURNING id, name, price, category
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int id = ingredient.getId() > 0 ? ingredient.getId() : getNextId(conn, "ingredient");
            ps.setInt(1, id);
            ps.setString(2, ingredient.getName());
            ps.setDouble(3, ingredient.getPrice());
            ps.setString(4, ingredient.getCategory().name());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapIngredient(rs);
            }
            throw new RuntimeException("Failed to save ingredient");
        } catch (SQLException e) {
            throw new RuntimeException("Error saving ingredient", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<StockMouvement> findStockMovementsByIngredientId(int ingredientId) {
        String sql = "SELECT id, quantity, type, unit, creation_datetime FROM stock_movement WHERE id_ingredient = ?";

        List<StockMouvement> movements = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                StockValue value = new StockValue(
                        rs.getDouble("quantity"),
                        Unit.valueOf(rs.getString("unit"))
                );
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
            throw new RuntimeException("Error fetching stock movements", e);
        } finally {
            dataSource.closeConnection(conn);
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

    private int getNextId(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT nextval(pg_get_serial_sequence('" + tableName + "', 'id'))";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new RuntimeException("Unable to generate new id for " + tableName);
    }
}