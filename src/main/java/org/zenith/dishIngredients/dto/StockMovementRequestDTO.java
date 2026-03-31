package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.MouvementTypeEnum;
import org.zenith.dishIngredients.entity.Unit;

public class StockMovementRequestDTO {
    private double quantity;
    private Unit unit;
    private MouvementTypeEnum type;

    public StockMovementRequestDTO() {
    }

    public StockMovementRequestDTO(double quantity, Unit unit, MouvementTypeEnum type) {
        this.quantity = quantity;
        this.unit = unit;
        this.type = type;
    }

    // Getters et Setters
    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public MouvementTypeEnum getType() {
        return type;
    }

    public void setType(MouvementTypeEnum type) {
        this.type = type;
    }

    // Validation
    public boolean isValid() {
        return quantity > 0 && unit != null && type != null;
    }
}