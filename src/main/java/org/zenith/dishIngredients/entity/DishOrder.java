package org.zenith.dishIngredients.entity;

import java.util.Objects;

public class DishOrder {
    private final int id;
    private final Dish dish;
    private final int quantity;

    public DishOrder(int id, Dish dish, int quantity) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public Dish getDish() {
        return dish;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DishOrder that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DishOrder{id=" + id +
                ", dish=" + dish.getName() +
                ", quantity=" + quantity + "}";
    }
}