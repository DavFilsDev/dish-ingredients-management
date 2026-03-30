package org.zenith.dishIngredients;

import org.zenith.dishIngredients.entity.CategoryEnum;
import org.zenith.dishIngredients.entity.Unit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Ingredient {
    private final int id;
    private final String name;
    private final double price;
    private final CategoryEnum category;
    private Double quantity;
    private List<StockMouvement> stockMouvementList;

    public Ingredient(int id, String name, double price, CategoryEnum category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public List<StockMouvement> getStockMovementList() {
        return stockMouvementList;
    }

    public void setStockMovementList(List<StockMouvement> stockMouvementList) {
        this.stockMouvementList = stockMouvementList;
    }

    public StockValue getStockValueAt(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("instant must not be null");
        }

        if (stockMouvementList == null || stockMouvementList.isEmpty()) {
            return new StockValue(0.0, Unit.KG);
        }

        double totalQuantity = 0.0;
        Unit unit = null;

        for (StockMouvement movement : stockMouvementList) {
            if (movement == null) {
                continue;
            }

            Instant movementTime = movement.getCreationDateTime();
            if (movementTime == null || movementTime.isAfter(instant)) {
                continue;
            }

            StockValue value = movement.getValue();
            if (value == null) {
                continue;
            }

            if (unit == null) {
                unit = value.getUnit() == null ? Unit.KG : value.getUnit();
            }

            double qty = value.getQuantity();
            if (movement.getType() == MouvementTypeEnum.IN) {
                totalQuantity += qty;
            } else if (movement.getType() == MouvementTypeEnum.OUT) {
                totalQuantity -= qty;
            }
        }

        if (unit == null) {
            unit = Unit.KG;
        }

        return new StockValue(totalQuantity, unit);
    }

    @Override
    public String toString() {
        return "Ingredient{"
                + "id="
                + id
                + ", name='"
                + name
                + '\''
                + ", price="
                + price
                + ", category="
                + category
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ingredient that)) return false;
        return id == that.id
                && Double.compare(that.price, price) == 0
                && Objects.equals(name, that.name)
                && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, price, category);
    }
}
