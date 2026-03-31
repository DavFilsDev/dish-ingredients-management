package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.MouvementTypeEnum;
import org.zenith.dishIngredients.entity.Unit;

import java.time.Instant;

public class StockMovementRequestDTO {

    private Unit unit;
    private double quantity;
    private MouvementTypeEnum type;
    private Instant creationDateTime;

    public StockMovementRequestDTO() {
    }

    public StockMovementRequestDTO(Unit unit, double quantity, MouvementTypeEnum type, Instant creationDateTime) {
        this.unit = unit;
        this.quantity = quantity;
        this.type = type;
        this.creationDateTime = creationDateTime;
    }

    public boolean isValid() {
        return unit != null && quantity > 0 && type != null;
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

    public MouvementTypeEnum getType() {
        return type;
    }

    public void setType(MouvementTypeEnum type) {
        this.type = type;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(Instant creationDateTime) {
        this.creationDateTime = creationDateTime;
    }
}