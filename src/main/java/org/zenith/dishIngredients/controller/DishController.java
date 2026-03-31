package org.zenith.dishIngredients.controller;

import org.zenith.dishIngredients.dto.DishIngredientRequestDTO;
import org.zenith.dishIngredients.dto.DishResponseDTO;
import org.zenith.dishIngredients.service.DishService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ResponseEntity<List<DishResponseDTO>> getDishes() {
        List<DishResponseDTO> dishes = dishService.getAllDishes();
        return ResponseEntity.ok(dishes);
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<DishResponseDTO> updateDishIngredients(
            @PathVariable int id,
            @RequestBody List<DishIngredientRequestDTO> ingredients) {

        if (ingredients == null) {
            throw new IllegalArgumentException("Request body must not be empty");
        }

        DishResponseDTO updatedDish = dishService.updateDishIngredients(id, ingredients);
        return ResponseEntity.ok(updatedDish);
    }
}