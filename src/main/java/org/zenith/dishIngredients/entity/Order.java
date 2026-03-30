package org.zenith.dishIngredients.entity;

import org.zenith.dishIngredients.TableOrder;
import org.zenith.dishIngredients.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    private final int id;
    private final String reference;
    private final Instant creationDateTime;
    private List<DishOrder> dishOrders;
    private TableOrder tableOrder;

    public Order(int id, String reference, Instant creationDateTime,
                 List<DishOrder> dishOrders, TableOrder tableOrder) {
        this.id = id;
        this.reference = reference;
        this.creationDateTime = creationDateTime;
        this.dishOrders = dishOrders != null ? new ArrayList<>(dishOrders) : new ArrayList<>();
        this.tableOrder = tableOrder;
    }

    public Order(int id, String reference, Instant creationDateTime, List<DishOrder> dishOrders) {
        this(id, reference, creationDateTime, dishOrders, null);
    }

    public int getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public List<DishOrder> getDishOrders() {
        return dishOrders;
    }

    public void setDishOrders(List<DishOrder> dishOrders) {
        this.dishOrders.clear();
        if (dishOrders != null) {
            this.dishOrders.addAll(dishOrders);
        }
    }

    public TableOrder getTableOrder() {
        return tableOrder;
    }

    public void setTableOrder(TableOrder tableOrder) {
        this.tableOrder = tableOrder;
    }

    public Instant getArrivalDateTime() {
        return tableOrder != null ? tableOrder.getArrivalDateTime() : null;
    }

    public Instant getDepartureDateTime() {
        return tableOrder != null ? tableOrder.getDepartureDateTime() : null;
    }

    public Table getTable() {
        return tableOrder != null ? tableOrder.getTable() : null;
    }

    public Double getTotalAmountWithoutVAT() {
        if (dishOrders == null || dishOrders.isEmpty()) {
            return 0.0;
        }

        return dishOrders.stream()
                .mapToDouble(dishOrder -> {
                    Dish dish = dishOrder.getDish();
                    Double price = dish.getSellingPrice();
                    if (price == null) {
                        throw new IllegalStateException("Selling price not set for dish: " + dish.getName());
                    }
                    return price * dishOrder.getQuantity();
                })
                .sum();
    }

    public Double getTotalAmountWithVAT() {
        return getTotalAmountWithoutVAT() * 1.2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return id == order.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDateTime=" + creationDateTime +
                ", tableOrder=" + tableOrder + "}";
    }
}