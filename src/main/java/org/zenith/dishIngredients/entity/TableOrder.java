package org.zenith.dishIngredients.entity;

import java.time.Instant;

public class TableOrder {
    private final Table table;
    private final Instant arrivalDateTime;
    private final Instant departureDateTime;

    public TableOrder(Table table, Instant arrivalDateTime, Instant departureDateTime) {
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
}