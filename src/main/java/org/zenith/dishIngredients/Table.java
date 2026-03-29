package org.zenith.dishIngredients;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Table {
    private final int id;
    private final int number;
    private final List<Order> orders;

    public Table(int id, int number) {
        this.id = id;
        this.number = number;
        this.orders = new ArrayList<>();
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
                // Si t est entre arrival et departure, la table n'est pas disponible
                if (!t.isBefore(arrival) && !t.isAfter(departure)) {
                    return false;
                }
            }
        }
        return true;
    }
}
