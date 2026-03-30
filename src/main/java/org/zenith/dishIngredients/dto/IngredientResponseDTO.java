package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.CategoryEnum;
import org.zenith.dishIngredients.entity.Ingredient;

public class IngredientResponseDTO {
    private final int id;
    private final String name;
    private final CategoryEnum category;
    private final double price;

    public IngredientResponseDTO(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.category = ingredient.getCategory();
        this.price = ingredient.getPrice();
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }
}