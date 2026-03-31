package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StockMovementRepository {

    private final DataSource dataSource;

    public StockMovementRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<StockMouvement> findByIngredientId(int ingredientId) {
        String sql = """
            SELECT id, quantity, type, unit, creation_datetime 
            FROM stock_movement 
            WHERE id_ingredient = ?
            ORDER BY creation_datetime ASC
            """;

        List<StockMouvement> movements = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                StockMouvement movement = mapStockMovement(rs);
                movements.add(movement);
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching stock movements for ingredient id: " + ingredientId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<StockMouvement> findByIngredientIdBeforeDate(int ingredientId, Instant beforeDate) {
        String sql = """
            SELECT id, quantity, type, unit, creation_datetime 
            FROM stock_movement 
            WHERE id_ingredient = ? 
            AND creation_datetime <= ?
            ORDER BY creation_datetime ASC
            """;

        List<StockMouvement> movements = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setTimestamp(2, Timestamp.from(beforeDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                StockMouvement movement = mapStockMovement(rs);
                movements.add(movement);
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching stock movements for ingredient id: " + ingredientId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public void save(StockMouvement movement, int ingredientId) {
        String sqlWithId = """
            INSERT INTO stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?, ?::movement_type, ?::unit_type, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        String sqlWithoutId = """
            INSERT INTO stock_movement(id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?::movement_type, ?::unit_type, ?)
            """;

        Connection conn = dataSource.getConnection();

        try {
            if (movement.getId() > 0) {
                try (PreparedStatement ps = conn.prepareStatement(sqlWithId)) {
                    ps.setInt(1, movement.getId());
                    ps.setInt(2, ingredientId);
                    ps.setDouble(3, movement.getValue().getQuantity());
                    ps.setString(4, movement.getType().name());
                    ps.setString(5, movement.getValue().getUnit().name());
                    ps.setTimestamp(6, Timestamp.from(movement.getCreationDateTime()));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(sqlWithoutId)) {
                    ps.setInt(1, ingredientId);
                    ps.setDouble(2, movement.getValue().getQuantity());
                    ps.setString(3, movement.getType().name());
                    ps.setString(4, movement.getValue().getUnit().name());
                    ps.setTimestamp(5, Timestamp.from(movement.getCreationDateTime()));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error saving stock movement", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public void saveAll(int ingredientId, List<StockMouvement> movements) {
        if (movements == null || movements.isEmpty()) {
            return;
        }

        Connection conn = dataSource.getConnection();

        try {
            conn.setAutoCommit(false);

            for (StockMouvement movement : movements) {
                save(movement, ingredientId);
            }

            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException("Rollback failed", ex);
            }
            throw new RuntimeException("Error saving stock movements batch", e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            dataSource.closeConnection(conn);
        }
    }

    public double calculateStockAtDate(int ingredientId, Instant date, Unit targetUnit) {
        List<StockMouvement> movements = findByIngredientIdBeforeDate(ingredientId, date);

        double totalStock = 0.0;

        for (StockMouvement movement : movements) {
            StockValue value = movement.getValue();
            double quantity = value.getQuantity();


            if (movement.getType() == MouvementTypeEnum.IN) {
                totalStock += quantity;
            } else if (movement.getType() == MouvementTypeEnum.OUT) {
                totalStock -= quantity;
            }
        }

        return totalStock;
    }

    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM stock_movement WHERE id = ?";

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error checking stock movement existence", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    private StockMouvement mapStockMovement(ResultSet rs) throws SQLException {
        StockValue value = new StockValue(
                rs.getDouble("quantity"),
                Unit.valueOf(rs.getString("unit"))
        );
        return new StockMouvement(
                rs.getInt("id"),
                value,
                MouvementTypeEnum.valueOf(rs.getString("type")),
                rs.getTimestamp("creation_datetime").toInstant()
        );
    }

    public List<StockMouvement> findByIngredientIdBetweenDates(int ingredientId, Instant from, Instant to) {
        String sql = """
            SELECT id, quantity, type, unit, creation_datetime 
            FROM stock_movement 
            WHERE id_ingredient = ? 
            AND creation_datetime >= ?
            AND creation_datetime <= ?
            ORDER BY creation_datetime ASC
            """;

        List<StockMouvement> movements = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setTimestamp(2, Timestamp.from(from));
            ps.setTimestamp(3, Timestamp.from(to));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                StockMouvement movement = mapStockMovement(rs);
                movements.add(movement);
            }
            return movements;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching stock movements between dates for ingredient id: " + ingredientId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public StockMouvement createMovement(int ingredientId, StockMouvement movement) {
        String sql = """
            INSERT INTO stock_movement(id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?::movement_type, ?::unit_type, ?)
            RETURNING id, quantity, type, unit, creation_datetime
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ps.setDouble(2, movement.getValue().getQuantity());
            ps.setString(3, movement.getType().name());
            ps.setString(4, movement.getValue().getUnit().name());
            ps.setTimestamp(5, Timestamp.from(movement.getCreationDateTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                StockValue value = new StockValue(
                        rs.getDouble("quantity"),
                        Unit.valueOf(rs.getString("unit"))
                );
                return new StockMouvement(
                        rs.getInt("id"),
                        value,
                        MouvementTypeEnum.valueOf(rs.getString("type")),
                        rs.getTimestamp("creation_datetime").toInstant()
                );
            }
            throw new RuntimeException("Failed to create stock movement");
        } catch (SQLException e) {
            throw new RuntimeException("Error creating stock movement", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public List<StockMouvement> createMovements(int ingredientId, List<StockMouvement> movements) {
        List<StockMouvement> createdMovements = new ArrayList<>();

        for (StockMouvement movement : movements) {
            StockMouvement created = createMovement(ingredientId, movement);
            createdMovements.add(created);
        }

        return createdMovements;
    }
}