package org.zenith.dishIngredients.entity;

public class StockValue {

    private final double quantity;
    private final Unit unit;

    public StockValue(double quantity, Unit unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }
}