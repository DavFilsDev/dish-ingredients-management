package org.zenith.dishIngredients.entity;

import java.time.Instant;
import java.util.Objects;

public class TableOrder {
    private final Table table;
    private final Instant arrivalDateTime;
    private final Instant departureDateTime;

    public TableOrder(Table table, Instant arrivalDateTime, Instant departureDateTime) {
        if (table == null) {
            throw new IllegalArgumentException("Table must not be null");
        }
        if (arrivalDateTime == null || departureDateTime == null) {
            throw new IllegalArgumentException("Arrival and departure datetime must not be null");
        }
        if (arrivalDateTime.isAfter(departureDateTime)) {
            throw new IllegalArgumentException("Arrival datetime must be before departure datetime");
        }

        this.table = table;
        this.arrivalDateTime = arrivalDateTime;
        this.departureDateTime = departureDateTime;
    }

    public Table getTable() {
        return table;
    }

    public Instant getArrivalDateTime() {
        return arrivalDateTime;
    }

    public Instant getDepartureDateTime() {
        return departureDateTime;
    }

    public long getDurationInMinutes() {
        return java.time.Duration.between(arrivalDateTime, departureDateTime).toMinutes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableOrder that)) return false;
        return Objects.equals(table, that.table) &&
                Objects.equals(arrivalDateTime, that.arrivalDateTime) &&
                Objects.equals(departureDateTime, that.departureDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table, arrivalDateTime, departureDateTime);
    }

    @Override
    public String toString() {
        return "TableOrder{table=" + table.getNumber() +
                ", arrival=" + arrivalDateTime +
                ", departure=" + departureDateTime + "}";
    }
}