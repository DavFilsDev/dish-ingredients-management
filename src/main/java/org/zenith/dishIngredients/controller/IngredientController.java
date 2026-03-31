package org.zenith.dishIngredients.controller;

import org.zenith.dishIngredients.dto.IngredientResponseDTO;
import org.zenith.dishIngredients.dto.IngredientStockResponseDTO;
import org.zenith.dishIngredients.dto.StockMovementRequestDTO;
import org.zenith.dishIngredients.dto.StockMovementResponseDTO;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.service.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public ResponseEntity<List<IngredientResponseDTO>> getIngredients() {
        List<IngredientResponseDTO> ingredients = ingredientService.getAllIngredients();
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponseDTO> getIngredientById(@PathVariable int id) {
        IngredientResponseDTO ingredient = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(ingredient);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<IngredientStockResponseDTO> getIngredientStock(
            @PathVariable int id,
            @RequestParam String at,
            @RequestParam String unit) {

        if (at == null || at.isBlank()) {
            throw new IllegalArgumentException("Either mandatory query parameter `at` or `unit` is not provided.");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Either mandatory query parameter `at` or `unit` is not provided.");
        }

        Instant instant;
        try {
            instant = Instant.parse(at);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format for parameter `at`. Expected ISO-8601 format (e.g., 2024-01-06T12:00:00Z)");
        }

        Unit unitEnum;
        try {
            unitEnum = Unit.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid unit. Supported units: PCS, KG, L");
        }

        IngredientStockResponseDTO stock = ingredientService.getIngredientStock(id, instant, unitEnum);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/{id}/stockMovements")
    public ResponseEntity<List<StockMovementResponseDTO>> getStockMovements(
            @PathVariable int id,
            @RequestParam String from,
            @RequestParam String to) {

        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'from' is required");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'to' is required");
        }

        Instant fromInstant;
        Instant toInstant;
        try {
            fromInstant = Instant.parse(from);
            toInstant = Instant.parse(to);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected ISO-8601 format (e.g., 2024-01-06T12:00:00Z)");
        }

        List<StockMovementResponseDTO> movements = ingredientService.getStockMovementsBetweenDates(id, fromInstant, toInstant);
        return ResponseEntity.ok(movements);
    }

    @PostMapping("/{id}/stockMovements")
    public ResponseEntity<List<StockMovementResponseDTO>> addStockMovements(
            @PathVariable int id,
            @RequestBody List<StockMovementRequestDTO> movements) {

        if (movements == null) {
            throw new IllegalArgumentException("Request body must not be empty");
        }

        List<StockMovementResponseDTO> createdMovements = ingredientService.addStockMovements(id, movements);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMovements);
    }
}