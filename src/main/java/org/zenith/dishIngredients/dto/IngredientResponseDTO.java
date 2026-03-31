package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.CategoryEnum;
import org.zenith.dishIngredients.entity.Ingredient;

public class IngredientResponseDTO {

    private int id;
    private String name;
    private CategoryEnum category;
    private double price;

    public IngredientResponseDTO() {
    }

    public IngredientResponseDTO(int id, String name, CategoryEnum category, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public static IngredientResponseDTO fromEntity(Ingredient ingredient) {
        return new IngredientResponseDTO(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getCategory(),
                ingredient.getPrice()
        );
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}