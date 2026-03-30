package org.zenith.dishIngredients.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Table {
    private final int id;
    private final int number;
    private List<Order> orders;

    public Table(int id, int number) {
        this.id = id;
        this.number = number;
        this.orders = new ArrayList<>();
    }

    public Table(int id, int number, List<Order> orders) {
        this.id = id;
        this.number = number;
        this.orders = orders != null ? new ArrayList<>(orders) : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders.clear();
        if (orders != null) {
            this.orders.addAll(orders);
        }
    }

    public boolean isAvailableAt(Instant t) {
        if (t == null) {
            throw new IllegalArgumentException("Instant must not be null");
        }

        for (Order order : orders) {
            Instant arrival = order.getArrivalDateTime();
            Instant departure = order.getDepartureDateTime();

            if (arrival != null && departure != null) {
                if (!t.isBefore(arrival) && !t.isAfter(departure)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isAvailableForPeriod(Instant arrival, Instant departure) {
        if (arrival == null || departure == null) {
            throw new IllegalArgumentException("Arrival and departure must not be null");
        }
        if (arrival.isAfter(departure)) {
            throw new IllegalArgumentException("Arrival must be before departure");
        }

        for (Order order : orders) {
            Instant orderArrival = order.getArrivalDateTime();
            Instant orderDeparture = order.getDepartureDateTime();

            if (orderArrival != null && orderDeparture != null) {
                if (arrival.isBefore(orderDeparture) && departure.isAfter(orderArrival)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table table)) return false;
        return id == table.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Table{id=" + id + ", number=" + number + "}";
    }
}