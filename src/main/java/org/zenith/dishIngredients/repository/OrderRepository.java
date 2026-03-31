package org.zenith.dishIngredients.repository;

import org.zenith.dishIngredients.config.DataSource;
import org.zenith.dishIngredients.entity.*;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {

    private final DataSource dataSource;
    private final DishRepository dishRepository;
    private final TableRepository tableRepository;

    public OrderRepository(DataSource dataSource,
                           DishRepository dishRepository,
                           TableRepository tableRepository) {
        this.dataSource = dataSource;
        this.dishRepository = dishRepository;
        this.tableRepository = tableRepository;
    }

    public Order findByReference(String reference) {
        String sql = """
            SELECT o.id, o.reference, o.creation_datetime,
                   o.id_table, o.arrival_datetime, o.departure_datetime
            FROM "order" o
            WHERE o.reference = ?
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int orderId = rs.getInt("id");
                String savedReference = rs.getString("reference");
                Instant creationDateTime = rs.getTimestamp("creation_datetime").toInstant();
                int tableId = rs.getInt("id_table");
                Instant arrivalDateTime = rs.getTimestamp("arrival_datetime").toInstant();
                Instant departureDateTime = rs.getTimestamp("departure_datetime").toInstant();

                Table table = tableRepository.findById(tableId);
                TableOrder tableOrder = new TableOrder(table, arrivalDateTime, departureDateTime);

                List<DishOrder> dishOrders = findDishOrdersByOrderId(orderId);

                return new Order(orderId, savedReference, creationDateTime, dishOrders, tableOrder);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching order by reference: " + reference, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Order findById(int id) {
        String sql = """
            SELECT o.id, o.reference, o.creation_datetime,
                   o.id_table, o.arrival_datetime, o.departure_datetime
            FROM "order" o
            WHERE o.id = ?
            """;

        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int orderId = rs.getInt("id");
                String reference = rs.getString("reference");
                Instant creationDateTime = rs.getTimestamp("creation_datetime").toInstant();
                int tableId = rs.getInt("id_table");
                Instant arrivalDateTime = rs.getTimestamp("arrival_datetime").toInstant();
                Instant departureDateTime = rs.getTimestamp("departure_datetime").toInstant();

                Table table = tableRepository.findById(tableId);
                TableOrder tableOrder = new TableOrder(table, arrivalDateTime, departureDateTime);

                List<DishOrder> dishOrders = findDishOrdersByOrderId(orderId);

                return new Order(orderId, reference, creationDateTime, dishOrders, tableOrder);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching order by id: " + id, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    public Order save(Order order) {
        String sql = """
            INSERT INTO "order"(id, reference, creation_datetime, 
                               id_table, arrival_datetime, departure_datetime)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE
            SET reference = EXCLUDED.reference,
                creation_datetime = EXCLUDED.creation_datetime,
                id_table = EXCLUDED.id_table,
                arrival_datetime = EXCLUDED.arrival_datetime,
                departure_datetime = EXCLUDED.departure_datetime
            RETURNING id, reference, creation_datetime,
                      id_table, arrival_datetime, departure_datetime
            """;

        Connection conn = dataSource.getConnection();

        try {
            int id = order.getId() > 0 ? order.getId() : getNextId(conn, "\"order\"");
            String reference = order.getReference();
            if (reference == null || reference.isBlank() || !reference.matches("ORD\\d{5}")) {
                reference = generateOrderReference(conn);
            }

            Instant creationDateTime = order.getCreationDateTime() != null
                    ? order.getCreationDateTime()
                    : Instant.now();

            TableOrder tableOrder = order.getTableOrder();
            if (tableOrder == null || tableOrder.getTable() == null) {
                throw new IllegalArgumentException("Table must be specified for the order");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setString(2, reference);
                ps.setTimestamp(3, Timestamp.from(creationDateTime));
                ps.setInt(4, tableOrder.getTable().getId());
                ps.setTimestamp(5, Timestamp.from(tableOrder.getArrivalDateTime()));
                ps.setTimestamp(6, Timestamp.from(tableOrder.getDepartureDateTime()));

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int savedOrderId = rs.getInt("id");
                    String savedReference = rs.getString("reference");
                    Instant savedCreationDateTime = rs.getTimestamp("creation_datetime").toInstant();
                    int savedTableId = rs.getInt("id_table");
                    Instant savedArrivalDateTime = rs.getTimestamp("arrival_datetime").toInstant();
                    Instant savedDepartureDateTime = rs.getTimestamp("departure_datetime").toInstant();

                    Table savedTable = tableRepository.findById(savedTableId);
                    TableOrder savedTableOrder = new TableOrder(savedTable, savedArrivalDateTime, savedDepartureDateTime);

                    // Sauvegarder les DishOrder
                    saveDishOrders(conn, savedOrderId, order.getDishOrders());

                    return new Order(savedOrderId, savedReference, savedCreationDateTime,
                            order.getDishOrders(), savedTableOrder);
                }
            }
            throw new RuntimeException("Failed to save order");
        } catch (SQLException e) {
            throw new RuntimeException("Error saving order", e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    private List<DishOrder> findDishOrdersByOrderId(int orderId) {
        String sql = """
            SELECT dor.id AS dish_order_id,
                   dor.id_dish AS id_dish,
                   dor.quantity AS quantity,
                   d.id AS dish_id,
                   d.name AS dish_name,
                   d.dish_type AS dish_type,
                   d.price AS dish_price
            FROM dish_order dor
            JOIN dish d ON d.id = dor.id_dish
            WHERE dor.id_order = ?
            """;

        List<DishOrder> dishOrders = new ArrayList<>();
        Connection conn = dataSource.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Dish dish = new Dish(
                        rs.getInt("dish_id"),
                        rs.getString("dish_name"),
                        DishTypeEnum.valueOf(rs.getString("dish_type")),
                        rs.getObject("dish_price") != null ? rs.getDouble("dish_price") : null
                );
                int dishOrderId = rs.getInt("dish_order_id");
                int quantity = rs.getInt("quantity");
                dishOrders.add(new DishOrder(dishOrderId, dish, quantity));
            }
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching dish orders for order id: " + orderId, e);
        } finally {
            dataSource.closeConnection(conn);
        }
    }

    private void saveDishOrders(Connection conn, int orderId, List<DishOrder> dishOrders) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_order WHERE id_order = ?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }

        if (dishOrders != null && !dishOrders.isEmpty()) {
            String sql = "INSERT INTO dish_order(id_order, id_dish, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (DishOrder dishOrder : dishOrders) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, dishOrder.getDish().getId());
                    ps.setInt(3, dishOrder.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private String generateOrderReference(Connection conn) throws SQLException {
        String sql = "SELECT nextval('order_reference_seq') AS seq";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long seq = rs.getLong("seq");
                return String.format("ORD%05d", seq);
            }
        }
        throw new RuntimeException("Unable to generate order reference from sequence");
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