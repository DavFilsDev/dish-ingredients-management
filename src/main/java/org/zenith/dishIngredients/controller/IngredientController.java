package org.zenith.dishIngredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenith.dishIngredients.dto.IngredientResponseDTO;
import org.zenith.dishIngredients.dto.StockValueResponseDTO;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.repository.IngredientRepository;
import org.zenith.dishIngredients.service.StockService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;
    private final StockService stockService;

    public IngredientController(IngredientRepository ingredientRepository,
                                StockService stockService) {
        this.ingredientRepository = ingredientRepository;
        this.stockService = stockService;
    }

    @GetMapping
    public ResponseEntity<List<IngredientResponseDTO>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        List<IngredientResponseDTO> response = ingredients.stream()
                .map(IngredientResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponseDTO> getIngredientById(@PathVariable int id) {
        try {
            Ingredient ingredient = ingredientRepository.findById(id);
            return ResponseEntity.ok(new IngredientResponseDTO(ingredient));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStockValue(
            @PathVariable int id,
            @RequestParam String at,
            @RequestParam String unit) {

        if (at == null || at.isBlank()) {
            return ResponseEntity.badRequest().body("Missing mandatory query parameter 'at'");
        }
        if (unit == null || unit.isBlank()) {
            return ResponseEntity.badRequest().body("Missing mandatory query parameter 'unit'");
        }

        Unit targetUnit;
        try {
            targetUnit = Unit.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid unit. Must be one of: PCS, KG, L");
        }

        try {
            ingredientRepository.findById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }

        Instant instant;
        try {
            instant = Instant.parse(at);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO-8601 format (ex: 2024-01-06T12:00:00Z)");
        }

        StockValue stockValue = stockService.getStockValueAt(id, instant, targetUnit);
        StockValueResponseDTO response = new StockValueResponseDTO(stockValue);

        return ResponseEntity.ok(response);
    }
}