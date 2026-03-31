package org.zenith.dishIngredients.service;

import org.zenith.dishIngredients.dto.DishIngredientRequestDTO;
import org.zenith.dishIngredients.dto.DishResponseDTO;
import org.zenith.dishIngredients.entity.Dish;
import org.zenith.dishIngredients.entity.DishIngredient;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.exception.ResourceNotFoundException;
import org.zenith.dishIngredients.repository.DishIngredientRepository;
import org.zenith.dishIngredients.repository.DishRepository;
import org.zenith.dishIngredients.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final IngredientRepository ingredientRepository;
    private final DishIngredientRepository dishIngredientRepository;

    public DishService(DishRepository dishRepository,
                       IngredientRepository ingredientRepository,
                       DishIngredientRepository dishIngredientRepository) {
        this.dishRepository = dishRepository;
        this.ingredientRepository = ingredientRepository;
        this.dishIngredientRepository = dishIngredientRepository;
    }

    public List<DishResponseDTO> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();
        List<DishResponseDTO> result = new ArrayList<>();

        for (Dish dish : dishes) {
            List<DishIngredient> dishIngredients = dishIngredientRepository.findByDishId(dish.getId());
            result.add(DishResponseDTO.fromEntity(dish, dishIngredients));
        }

        return result;
    }

    public DishResponseDTO updateDishIngredients(int dishId, List<DishIngredientRequestDTO> ingredientRequests) {
        Dish dish = dishRepository.findById(dishId);
        if (dish == null) {
            throw new ResourceNotFoundException("Dish", "id=" + dishId);
        }

        List<DishIngredient> dishIngredients = new ArrayList<>();

        if (ingredientRequests != null && !ingredientRequests.isEmpty()) {
            for (DishIngredientRequestDTO request : ingredientRequests) {
                if (!request.isValid()) {
                    continue;
                }

                Ingredient ingredient = ingredientRepository.findById(request.getIngredientId());
                if (ingredient != null) {
                    Unit unit = request.getUnit() != null ? request.getUnit() : Unit.KG;
                    double quantity = request.getQuantity() != null ? request.getQuantity() : 1.0;

                    DishIngredient dishIngredient = new DishIngredient(dish, ingredient, quantity, unit);
                    dishIngredients.add(dishIngredient);
                }
            }
        }

        dishIngredientRepository.updateDishIngredients(dishId, dishIngredients);

        List<DishIngredient> updatedDishIngredients = dishIngredientRepository.findByDishId(dishId);

        return DishResponseDTO.fromEntity(dish, updatedDishIngredients);
    }

    public boolean dishExists(int id) {
        return dishRepository.findById(id) != null;
    }

    public Dish getDishWithIngredients(int id) {
        Dish dish = dishRepository.findById(id);
        if (dish == null) {
            return null;
        }
        List<DishIngredient> dishIngredients = dishIngredientRepository.findByDishId(id);
        dish.setDishIngredients(dishIngredients);
        return dish;
    }

    public double getDishCost(int dishId) {
        Dish dish = getDishWithIngredients(dishId);
        if (dish == null) {
            throw new ResourceNotFoundException("Dish", "id=" + dishId);
        }
        return dish.getDishCost();
    }

    public double getDishGrossMargin(int dishId) {
        Dish dish = getDishWithIngredients(dishId);
        if (dish == null) {
            throw new ResourceNotFoundException("Dish", "id=" + dishId);
        }
        return dish.getGrossMargin();
    }
}