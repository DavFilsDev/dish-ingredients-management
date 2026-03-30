package org.zenith.dishIngredients.dto;

import java.util.List;

public class UpdateIngredientsRequestDTO {
    private List<Integer> ingredientIds;

    public UpdateIngredientsRequestDTO() {
    }

    public UpdateIngredientsRequestDTO(List<Integer> ingredientIds) {
        this.ingredientIds = ingredientIds;
    }

    // Getter et Setter
    public List<Integer> getIngredientIds() {
        return ingredientIds;
    }

    public void setIngredientIds(List<Integer> ingredientIds) {
        this.ingredientIds = ingredientIds;
    }

    // Validation
    public boolean isValid() {
        return ingredientIds != null && !ingredientIds.isEmpty();
    }
}