package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.Unit;

public class DishIngredientRequestDTO {

    private int ingredientId;
    private Double quantity;
    private Unit unit;

    public DishIngredientRequestDTO() {
    }

    public DishIngredientRequestDTO(int ingredientId, Double quantity, Unit unit) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
        this.unit = unit;
    }


    public boolean isValid() {
        return ingredientId > 0 && quantity != null && quantity > 0 && unit != null;
    }

    public Ingredient toIngredientReference() {
        Ingredient ingredient = new Ingredient(ingredientId, null, 0, null);
        ingredient.setQuantity(quantity);
        return ingredient;
    }

    public int getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(int ingredientId) {
        this.ingredientId = ingredientId;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}