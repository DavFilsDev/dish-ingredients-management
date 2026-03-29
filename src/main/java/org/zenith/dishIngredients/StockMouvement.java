package org.zenith.dishIngredients;

import java.time.Instant;

public class StockMouvement {

    private final int id;
    private final StockValue value;
    private final MovementTypeEnum type;
    private final Instant creationDateTime;

    public StockMouvement(int id, StockValue value, MovementTypeEnum type, Instant creationDateTime) {
        this.id = id;
        this.value = value;
        this.type = type;
        this.creationDateTime = creationDateTime;
    }

    public int getId() {
        return id;
    }

    public StockValue getValue() {
        return value;
    }

    public MovementTypeEnum getType() {
        return type;
    }

    public Instant getCreationDateTime() {
        return creationDateTime;
    }
}
