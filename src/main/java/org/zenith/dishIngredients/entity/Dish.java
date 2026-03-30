package org.zenith.dishIngredients.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private final int id;
    private final String name;
    private final DishTypeEnum dishType;
    private Double sellingPrice;
    private List<DishIngredient> dishIngredients;

    public Dish(int id, String name, DishTypeEnum dishType, Double sellingPrice) {
        this.id = id;
        this.name = name;
        this.dishType = dishType;
        this.sellingPrice = sellingPrice;
        this.dishIngredients = new ArrayList<>();
    }

    public Dish(int id, String name, DishTypeEnum dishType) {
        this(id, name, dishType, null);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DishTypeEnum getDishType() {
        return dishType;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public List<DishIngredient> getDishIngredients() {
        return dishIngredients;
    }

    public void setDishIngredients(List<DishIngredient> dishIngredients) {
        this.dishIngredients.clear();
        if (dishIngredients != null) {
            this.dishIngredients.addAll(dishIngredients);
        }
    }

    public List<Ingredient> getIngredients() {
        List<Ingredient> ingredients = new ArrayList<>();
        for (DishIngredient di : dishIngredients) {
            Ingredient ingredient = di.getIngredient();
            ingredient.setQuantity(di.getQuantity());
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    public void addIngredient(Ingredient ingredient) {
        if (ingredient != null) {
            this.dishIngredients.add(new DishIngredient(this, ingredient, 1.0, Unit.KG));
        }
    }

    public void addIngredient(Ingredient ingredient, double quantity, Unit unit) {
        if (ingredient != null) {
            this.dishIngredients.add(new DishIngredient(this, ingredient, quantity, unit));
        }
    }

    public Double getDishCost() {
        double total = 0.0;
        for (DishIngredient di : dishIngredients) {
            total += di.getIngredient().getPrice() * di.getQuantity();
        }
        return total;
    }

    public Double getGrossMargin() {
        if (sellingPrice == null) {
            throw new IllegalStateException("Selling price not set for dish: " + name);
        }
        return sellingPrice - getDishCost();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dish dish)) return false;
        return id == dish.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dish{id=" + id + ", name='" + name + "', dishType=" + dishType + ", sellingPrice=" + sellingPrice + "}";
    }
}