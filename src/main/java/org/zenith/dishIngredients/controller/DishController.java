package org.zenith.dishIngredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenith.dishIngredients.dto.DishResponseDTO;
import org.zenith.dishIngredients.dto.IngredientResponseDTO;
import org.zenith.dishIngredients.dto.UpdateIngredientsRequestDTO;
import org.zenith.dishIngredients.entity.Dish;
import org.zenith.dishIngredients.entity.DishIngredient;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.repository.DishRepository;
import org.zenith.dishIngredients.repository.IngredientRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;

    public DishController(DishRepository dishRepository,
                          IngredientRepository ingredientRepository) {
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public ResponseEntity<List<DishResponseDTO>> getAllDishes() {
        List<Dish> dishes = dishRepository.findAllWithIngredients();
        List<DishResponseDTO> response = dishes.stream()
                .map(DishResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateDishIngredients(
            @PathVariable int id,
            @RequestBody UpdateIngredientsRequestDTO request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body("Request body is required");
        }

        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body("ingredientIds list is required and must not be empty");
        }

        try {
            dishRepository.findByIdWithIngredients(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Dish.id=" + id + " is not found");
        }

        List<Integer> ingredientIds = request.getIngredientIds();
        List<Integer> nonExistingIngredients = ingredientIds.stream()
                .filter(ingredientId -> {
                    try {
                        ingredientRepository.findById(ingredientId);
                        return false;
                    } catch (RuntimeException e) {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        if (!nonExistingIngredients.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Some ingredients do not exist: " + nonExistingIngredients);
        }

        List<DishIngredient> dishIngredients = ingredientIds.stream()
                .map(ingredientId -> {
                    Ingredient ingredient = ingredientRepository.findById(ingredientId);
                    Dish dish = new Dish(id, "", null); // Dummy dish, sera remplacé
                    return new DishIngredient(dish, ingredient, 1.0, Unit.KG);
                })
                .collect(Collectors.toList());

        Dish dish = dishRepository.findByIdWithIngredients(id);

        List<DishIngredient> finalDishIngredients = ingredientIds.stream()
                .map(ingredientId -> {
                    Ingredient ingredient = ingredientRepository.findById(ingredientId);
                    return new DishIngredient(dish, ingredient, 1.0, Unit.KG);
                })
                .collect(Collectors.toList());

        dish.setDishIngredients(finalDishIngredients);

        Dish updatedDish = dishRepository.save(dish);

        return ResponseEntity.ok(new DishResponseDTO(updatedDish));
    }
}
