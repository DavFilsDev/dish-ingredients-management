package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.Dish;
import org.zenith.dishIngredients.entity.DishIngredient;
import org.zenith.dishIngredients.entity.DishTypeEnum;

import java.util.List;
import java.util.stream.Collectors;

public class DishResponseDTO {
    private final int id;
    private final String name;
    private final DishTypeEnum dishType;
    private final Double sellingPrice;
    private final List<IngredientResponseDTO> ingredients;

    public DishResponseDTO(Dish dish) {
        this.id = dish.getId();
        this.name = dish.getName();
        this.dishType = dish.getDishType();
        this.sellingPrice = dish.getSellingPrice();

        this.ingredients = dish.getDishIngredients().stream()
                .map(DishIngredient::getIngredient)
                .map(IngredientResponseDTO::new)
                .collect(Collectors.toList());
    }

    public DishResponseDTO(Dish dish, List<IngredientResponseDTO> ingredients) {
        this.id = dish.getId();
        this.name = dish.getName();
        this.dishType = dish.getDishType();
        this.sellingPrice = dish.getSellingPrice();
        this.ingredients = ingredients;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public List<IngredientResponseDTO> getIngredients() {
        return ingredients;
    }
}