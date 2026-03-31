package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.Table;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TableRepository {

    private final DataSource dataSource;

    public TableRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Table findById(int id) {
        String sql = "SELECT id, number FROM restaurant_table WHERE id = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Table(rs.getInt("id"), rs.getInt("number"));
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching table with id: " + id, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public boolean isTableAvailable(int tableId, Instant arrival, Instant departure) {
        String sql = """
            SELECT COUNT(*) as overlapping_orders
            FROM "order"
            WHERE id_table = ?
            AND arrival_datetime < ?
            AND departure_datetime > ?
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setTimestamp(2, Timestamp.from(departure));
            ps.setTimestamp(3, Timestamp.from(arrival));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("overlapping_orders") == 0;
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking table availability", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<Integer> findAvailableTables(Instant arrival, Instant departure) {
        String sql = """
            SELECT rt.id
            FROM restaurant_table rt
            WHERE NOT EXISTS (
                SELECT 1 
                FROM "order" o 
                WHERE o.id_table = rt.id
                AND o.arrival_datetime < ?
                AND o.departure_datetime > ?
            )
            ORDER BY rt.number
            """;

        List<Integer> availableTables = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(departure));
            ps.setTimestamp(2, Timestamp.from(arrival));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                availableTables.add(rs.getInt("id"));
            }
            return availableTables;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding available tables", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<Table> findAll() {
        String sql = "SELECT id, number FROM restaurant_table ORDER BY number";

        List<Table> tables = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tables.add(new Table(rs.getInt("id"), rs.getInt("number")));
            }
            return tables;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all tables", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }
}