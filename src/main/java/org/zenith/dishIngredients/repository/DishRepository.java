package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.Dish;
import org.zenith.dishIngredients.entity.DishTypeEnum;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DishRepository {

    private final DataSource dataSource;

    public DishRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Dish> findAll() {
        String sql = "SELECT id, name, dish_type, price FROM dish ORDER BY id";

        List<Dish> dishes = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Dish dish = mapDish(rs);
                dishes.add(dish);
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all dishes", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Dish findById(int id) {
        String sql = "SELECT id, name, dish_type, price FROM dish WHERE id = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapDish(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching dish with id: " + id, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Dish save(Dish dish) {
        String sql = """
            INSERT INTO dish(id, name, dish_type, price)
            VALUES (?, ?, ?::dish_type, ?)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                dish_type = EXCLUDED.dish_type,
                price = EXCLUDED.price
            RETURNING id, name, dish_type, price
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int id = dish.getId() > 0 ? dish.getId() : getNextId(conn, "dish");
            ps.setInt(1, id);
            ps.setString(2, dish.getName());
            ps.setString(3, dish.getDishType().name());
            if (dish.getPrice() != null) {
                ps.setDouble(4, dish.getPrice());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapDish(rs);
            }
            throw new RuntimeException("Failed to save dish");
        } catch (SQLException e) {
            throw new RuntimeException("Error saving dish", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM dish WHERE id = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking dish existence", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<Dish> findByIngredientName(String ingredientName) {
        String sql = """
            SELECT DISTINCT d.id, d.name, d.dish_type, d.price
            FROM dish d
            JOIN dish_ingredient di ON di.id_dish = d.id
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE i.name ILIKE ?
            """;

        List<Dish> dishes = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + ingredientName + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                dishes.add(mapDish(rs));
            }
            return dishes;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching dishes by ingredient name", e);
        } finally {
            dataSource.closeConnection(conn);
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