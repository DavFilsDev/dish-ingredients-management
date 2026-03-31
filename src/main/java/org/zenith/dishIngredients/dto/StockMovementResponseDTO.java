package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.MouvementTypeEnum;
import org.zenith.dishIngredients.entity.StockMouvement;
import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;

import java.time.Instant;

public class StockMovementResponseDTO {

    private int id;
    private Instant creationDateTime;
    private Unit unit;
    private double quantity;
    private MouvementTypeEnum type;

    public StockMovementResponseDTO() {
    }

    public StockMovementResponseDTO(int id, Instant creationDateTime, Unit unit, double quantity, MouvementTypeEnum type) {
        this.id = id;
        this.creationDateTime = creationDateTime;
        this.unit = unit;
        this.quantity = quantity;
        this.type = type;
    }

    public static StockMovementResponseDTO fromEntity(StockMouvement movement) {
        StockValue value = movement.getValue();
        return new StockMovementResponseDTO(
                movement.getId(),
                movement.getCreationDateTime(),
                value.getUnit(),
                value.getQuantity(),
                movement.getType()
        );
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(Instant creationDateTime) {
        this.creationDateTime = creationDateTime;
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
}