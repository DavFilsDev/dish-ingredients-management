package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.MouvementTypeEnum;
import org.zenith.dishIngredients.entity.StockMouvement;
import org.zenith.dishIngredients.entity.Unit;

import java.time.Instant;

public class StockMovementResponseDTO {
    private final int id;
    private final Instant creationDateTime;
    private final Unit unit;
    private final double quantity;
    private final MouvementTypeEnum type;

    public StockMovementResponseDTO(StockMouvement movement) {
        this.id = movement.getId();
        this.creationDateTime = movement.getCreationDateTime();
        this.unit = movement.getValue().getUnit();
        this.quantity = movement.getValue().getQuantity();
        this.type = movement.getType();
    }

    // Getters
    public int getId() {
        return id;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }

    public Unit getUnit() {
        return unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public MouvementTypeEnum getType() {
        return type;
    }
}