package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;

public class StockValueResponseDTO {
    private final double quantity;
    private final Unit unit;

    public StockValueResponseDTO(StockValue stockValue) {
        this.quantity = stockValue.getQuantity();
        this.unit = stockValue.getUnit();
    }

    public StockValueResponseDTO(double quantity, Unit unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    // Getters
    public double getQuantity() {
        return quantity;
    }

    public Unit getUnit() {
        return unit;
    }
}