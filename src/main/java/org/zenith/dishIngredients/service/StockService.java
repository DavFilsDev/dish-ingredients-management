package org.zenith.dishIngredients.service;

import org.springframework.stereotype.Service;
import org.zenith.dishIngredients.dto.StockMovementRequestDTO;
import org.zenith.dishIngredients.entity.*;
import org.zenith.dishIngredients.repository.DishRepository;
import org.zenith.dishIngredients.repository.IngredientRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final IngredientRepository ingredientRepository;
    private final DishRepository dishRepository;
    private final UnitConversionService conversionService;

    public StockService(IngredientRepository ingredientRepository,
                        DishRepository dishRepository,
                        UnitConversionService conversionService) {
        this.ingredientRepository = ingredientRepository;
        this.dishRepository = dishRepository;
        this.conversionService = conversionService;
    }

    public StockValue getStockValueAt(int ingredientId, Instant at, Unit targetUnit) {
        Ingredient ingredient = ingredientRepository.findByIdWithStockMovements(ingredientId);
        StockValue stockInKg = ingredient.getStockValueAt(at);

        if (targetUnit == Unit.KG) {
            return stockInKg;
        }

        double convertedQuantity = conversionService.getConversionFactor(
                ingredient.getName(),
                Unit.KG,
                targetUnit
        ) * stockInKg.getQuantity();

        return new StockValue(convertedQuantity, targetUnit);
    }

    public boolean hasSufficientStock(int ingredientId, double requiredQuantity,
                                      Unit requiredUnit, Instant at) {
        Ingredient ingredient = ingredientRepository.findByIdWithStockMovements(ingredientId);
        StockValue currentStock = ingredient.getStockValueAt(at);

        double requiredInKg = conversionService.convertToKg(
                ingredient.getName(),
                requiredQuantity,
                requiredUnit
        );

        return currentStock.getQuantity() >= requiredInKg;
    }

    public void checkStockOrThrow(Map<Integer, Double> requiredQuantities, Instant at) {
        for (Map.Entry<Integer, Double> entry : requiredQuantities.entrySet()) {
            int ingredientId = entry.getKey();
            double requiredQuantity = entry.getValue();

            Ingredient ingredient = ingredientRepository.findByIdWithStockMovements(ingredientId);
            StockValue currentStock = ingredient.getStockValueAt(at);

            if (currentStock.getQuantity() < requiredQuantity) {
                throw new RuntimeException(
                        "Not enough stock for ingredient: " + ingredient.getName() +
                                ". Required: " + requiredQuantity + " KG, Available: " +
                                currentStock.getQuantity() + " KG"
                );
            }
        }
    }

    public Map<Integer, Double> computeRequiredQuantities(Map<Integer, Integer> dishQuantities) {
        Map<Integer, Double> requiredQuantities = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : dishQuantities.entrySet()) {
            int dishId = entry.getKey();
            int quantityOrdered = entry.getValue();

            List<DishIngredient> dishIngredients = dishRepository.findDishIngredientsByDishId(dishId);

            for (DishIngredient di : dishIngredients) {
                int ingredientId = di.getIngredient().getId();
                double quantityPerDish = di.getQuantity();
                Unit unit = di.getUnit();

                double quantityPerDishInKg = conversionService.convertToKg(
                        di.getIngredient().getName(),
                        quantityPerDish,
                        unit
                );

                double totalRequired = quantityPerDishInKg * quantityOrdered;
                requiredQuantities.merge(ingredientId, totalRequired, Double::sum);
            }
        }

        return requiredQuantities;
    }

    public double getAvailableStockInKg(int ingredientId, Instant at) {
        Ingredient ingredient = ingredientRepository.findByIdWithStockMovements(ingredientId);
        return ingredient.getStockValueAt(at).getQuantity();
    }

    public List<StockMouvement> getStockMovements(int ingredientId, Instant from, Instant to) {
        ingredientRepository.findById(ingredientId);

        return ingredientRepository.findStockMovementsByIngredientId(ingredientId, from, to);
    }

    public List<StockMouvement> addStockMovements(int ingredientId, List<StockMouvement> movements) {
        ingredientRepository.findById(ingredientId);

        if (movements == null || movements.isEmpty()) {
            throw new IllegalArgumentException("Stock movements list cannot be null or empty");
        }

        for (StockMouvement movement : movements) {
            StockValue value = movement.getValue();
            if (value == null) {
                throw new IllegalArgumentException("Stock value cannot be null");
            }
            if (value.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (value.getUnit() == null) {
                throw new IllegalArgumentException("Unit cannot be null");
            }
            if (movement.getType() == null) {
                throw new IllegalArgumentException("Movement type cannot be null");
            }
        }

        return ingredientRepository.saveStockMovements(ingredientId, movements);
    }

    public List<StockMouvement> convertToStockMovements(List<StockMovementRequestDTO> requestDTOs) {
        if (requestDTOs == null || requestDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockMouvement> movements = new ArrayList<>();
        for (StockMovementRequestDTO dto : requestDTOs) {
            StockValue value = new StockValue(dto.getQuantity(), dto.getUnit());
            StockMouvement movement = new StockMouvement(
                    0,
                    value,
                    dto.getType(),
                    null
            );
            movements.add(movement);
        }
        return movements;
    }
}