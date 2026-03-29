package org.zenith.dishIngredients;

import java.time.Instant;
import java.util.*;

public class Order {
    private final int id;
    private final String reference;
    private final Instant creationDateTime;
    private final java.util.List<DishOrder> dishOrders;
    private final TableOrder tableOrder;

    public Order(int id, String reference, Instant creationDateTime, List<DishOrder> dishOrders, TableOrder tableOrder) {
        this.id = id;
        this.reference = reference;
        this.creationDateTime = creationDateTime;
        this.dishOrders = dishOrders;
        this.tableOrder = tableOrder;
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

    public Double getTotalAmountWithoutVAT() {
        return dishOrders.stream()
                .mapToDouble(
                        dishOrder -> {
                            Dish dish = dishOrder.getDish();
                            Double price = dish.getPrice();
                            if (price == null) {
                                throw new IllegalStateException("Price not set for dish " + dish.getName());
                            }
                            return price * dishOrder.getQuantity();
                        })
                .sum();
    }

    public Double getTotalAmountWithVAT() {
        return getTotalAmountWithoutVAT() * 1.2;
    }

    public TableOrder getTableOrder() {
        return tableOrder;
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
}

