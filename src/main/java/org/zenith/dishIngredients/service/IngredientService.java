package org.zenith.dishIngredients.service;

import org.zenith.dishIngredients.dto.IngredientResponseDTO;
import org.zenith.dishIngredients.dto.IngredientStockResponseDTO;
import org.zenith.dishIngredients.dto.StockMovementRequestDTO;
import org.zenith.dishIngredients.dto.StockMovementResponseDTO;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.StockMouvement;
import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.exception.ResourceNotFoundException;
import org.zenith.dishIngredients.repository.IngredientRepository;
import org.zenith.dishIngredients.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;

    public IngredientService(IngredientRepository ingredientRepository,
                             StockMovementRepository stockMovementRepository) {
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public List<IngredientResponseDTO> getAllIngredients(int page, int size) {
        List<Ingredient> ingredients = ingredientRepository.findAll(page, size);
        return ingredients.stream()
                .map(IngredientResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public IngredientResponseDTO getIngredientById(int id) {
        Ingredient ingredient = ingredientRepository.findById(id);
        if (ingredient == null) {
            throw new ResourceNotFoundException("Ingredient", "id=" + id);
        }
        return IngredientResponseDTO.fromEntity(ingredient);
    }

    public IngredientStockResponseDTO getIngredientStock(int id, Instant at, Unit unit) {
        Ingredient ingredient = ingredientRepository.findById(id);
        if (ingredient == null) {
            throw new ResourceNotFoundException("Ingredient", "id=" + id);
        }

        List<StockMouvement> movements = stockMovementRepository.findByIngredientIdBeforeDate(id, at);

        ingredient.setStockMovementList(movements);

        StockValue stockValue = ingredient.getStockValueAt(at);

        return IngredientStockResponseDTO.fromStockValue(stockValue);
    }

    public boolean ingredientExists(int id) {
        return ingredientRepository.findById(id) != null;
    }

    public Ingredient getIngredientWithMovements(int id) {
        Ingredient ingredient = ingredientRepository.findById(id);
        if (ingredient == null) {
            return null;
        }
        List<StockMouvement> movements = stockMovementRepository.findByIngredientId(id);
        ingredient.setStockMovementList(movements);
        return ingredient;
    }

    public List<StockMovementResponseDTO> getStockMovementsBetweenDates(int ingredientId, Instant from, Instant to) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId);
        if (ingredient == null) {
            throw new ResourceNotFoundException("Ingredient", "id=" + ingredientId);
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to parameters are required");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be before to date");
        }

        List<StockMouvement> movements = stockMovementRepository.findByIngredientIdBetweenDates(ingredientId, from, to);

        return movements.stream()
                .map(StockMovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public List<StockMovementResponseDTO> addStockMovements(int ingredientId, List<StockMovementRequestDTO> requestDTOs) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId);
        if (ingredient == null) {
            throw new ResourceNotFoundException("Ingredient", "id=" + ingredientId);
        }

        if (requestDTOs == null || requestDTOs.isEmpty()) {
            throw new IllegalArgumentException("Request body must not be empty");
        }

        List<StockMouvement> movementsToCreate = new ArrayList<>();

        for (StockMovementRequestDTO dto : requestDTOs) {
            if (!dto.isValid()) {
                throw new IllegalArgumentException("Invalid stock movement: unit, quantity (>0) and type are required");
            }

            Instant creationDateTime = dto.getCreationDateTime() != null
                    ? dto.getCreationDateTime()
                    : Instant.now();

            StockValue value = new StockValue(dto.getQuantity(), dto.getUnit());
            StockMouvement movement = new StockMouvement(
                    0,
                    value,
                    dto.getType(),
                    creationDateTime
            );
            movementsToCreate.add(movement);
        }

        List<StockMouvement> createdMovements = stockMovementRepository.createMovements(ingredientId, movementsToCreate);

        return createdMovements.stream()
                .map(StockMovementResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}