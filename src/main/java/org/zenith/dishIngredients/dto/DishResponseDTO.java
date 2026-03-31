package org.zenith.dishIngredients.dto;

import org.zenith.dishIngredients.entity.Dish;
import org.zenith.dishIngredients.entity.DishIngredient;
import org.zenith.dishIngredients.entity.Ingredient;

import java.util.ArrayList;
import java.util.List;


public class DishResponseDTO {

    private int id;
    private String name;
    private Double sellingPrice;
    private List<IngredientResponseDTO> ingredients;

    public DishResponseDTO() {
        this.ingredients = new ArrayList<>();
    }

    public DishResponseDTO(int id, String name, Double sellingPrice, List<IngredientResponseDTO> ingredients) {
        this.id = id;
        this.name = name;
        this.sellingPrice = sellingPrice;
        this.ingredients = ingredients;
    }

    public static DishResponseDTO fromEntity(Dish dish, List<DishIngredient> dishIngredients) {
        List<IngredientResponseDTO> ingredientDTOs = new ArrayList<>();

        for (DishIngredient di : dishIngredients) {
            Ingredient ingredient = di.getIngredient();

            IngredientResponseDTO ingredientDTO = IngredientResponseDTO.fromEntity(ingredient);
            ingredientDTOs.add(ingredientDTO);
        }

        return new DishResponseDTO(
                dish.getId(),
                dish.getName(),
                dish.getPrice(),
                ingredientDTOs
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

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public List<IngredientResponseDTO> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<IngredientResponseDTO> ingredients) {
        this.ingredients = ingredients;
    }
}