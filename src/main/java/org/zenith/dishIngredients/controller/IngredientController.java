package org.zenith.dishIngredients.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenith.dishIngredients.dto.IngredientResponseDTO;
import org.zenith.dishIngredients.dto.StockMovementRequestDTO;
import org.zenith.dishIngredients.dto.StockMovementResponseDTO;
import org.zenith.dishIngredients.dto.StockValueResponseDTO;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.StockMouvement;
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

    @GetMapping("/{id}/stockMovements")
    public ResponseEntity<?> getStockMovements(
            @PathVariable int id,
            @RequestParam String from,
            @RequestParam String to) {

        if (from == null || from.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Missing mandatory query parameter 'from'");
        }
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("Missing mandatory query parameter 'to'");
        }

        try {
            ingredientRepository.findById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }

        Instant fromInstant;
        Instant toInstant;
        try {
            fromInstant = Instant.parse(from);
            toInstant = Instant.parse(to);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Invalid date format. Use ISO-8601 format (ex: 2024-01-06T12:00:00Z)");
        }

        if (fromInstant.isAfter(toInstant)) {
            return ResponseEntity.badRequest()
                    .body("'from' date must be before or equal to 'to' date");
        }

        List<StockMouvement> movements = stockService.getStockMovements(id, fromInstant, toInstant);
        List<StockMovementResponseDTO> response = movements.stream()
                .map(StockMovementResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/stockMovements")
    public ResponseEntity<?> addStockMovements(
            @PathVariable int id,
            @RequestBody List<StockMovementRequestDTO> request) {

        if (request == null || request.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Request body is required and must not be empty");
        }

        try {
            ingredientRepository.findById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }

        for (StockMovementRequestDTO dto : request) {
            if (!dto.isValid()) {
                return ResponseEntity.badRequest()
                        .body("Invalid movement: quantity must be > 0, unit and type are required");
            }
        }

        List<StockMouvement> movements = stockService.convertToStockMovements(request);

        List<StockMouvement> savedMovements = stockService.addStockMovements(id, movements);

        List<StockMovementResponseDTO> response = savedMovements.stream()
                .map(StockMovementResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}