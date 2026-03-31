package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;

public class IngredientStockResponseDTO {

    private Unit unit;
    private double quantity;

    public IngredientStockResponseDTO() {
    }

    public IngredientStockResponseDTO(Unit unit, double quantity) {
        this.unit = unit;
        this.quantity = quantity;
    }

    public static IngredientStockResponseDTO fromStockValue(StockValue stockValue) {
        return new IngredientStockResponseDTO(
                stockValue.getUnit(),
                stockValue.getQuantity()
        );
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
}