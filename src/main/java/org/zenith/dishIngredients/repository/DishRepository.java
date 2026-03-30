package org.zenith.dishIngredients.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.zenith.dishIngredients.entity.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DishRepository {

    private final JdbcTemplate jdbcTemplate;
    private final IngredientRepository ingredientRepository;

    public DishRepository(JdbcTemplate jdbcTemplate, IngredientRepository ingredientRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.ingredientRepository = ingredientRepository;
    }

    private static final RowMapper<Dish> DISH_ROW_MAPPER = (rs, rowNum) -> {
        Double price = rs.getObject("price") != null ? rs.getDouble("price") : null;
        return new Dish(
                rs.getInt("id"),
                rs.getString("name"),
                DishTypeEnum.valueOf(rs.getString("dish_type")),
                price
        );
    };

    public List<Dish> findAllWithIngredients() {
        List<Dish> dishes = findAll();
        for (Dish dish : dishes) {
            dish.setDishIngredients(findDishIngredientsByDishId(dish.getId()));
        }
        return dishes;
    }

    public List<Dish> findAll() {
        String sql = "SELECT id, name, dish_type, price FROM dish ORDER BY id";
        return jdbcTemplate.query(sql, DISH_ROW_MAPPER);
    }

    public Dish findByIdWithIngredients(int id) {
        String sql = "SELECT id, name, dish_type, price FROM dish WHERE id = ?";
        List<Dish> results = jdbcTemplate.query(sql, DISH_ROW_MAPPER, id);

        if (results.isEmpty()) {
            throw new RuntimeException("Dish not found (id=" + id + ")");
        }

        Dish dish = results.get(0);
        dish.setDishIngredients(findDishIngredientsByDishId(dish.getId()));
        return dish;
    }

    public List<DishIngredient> findDishIngredientsByDishId(int dishId) {
        String sql = """
            SELECT di.id_dish, di.id_ingredient, di.quantity_required, di.unit,
                   d.id AS dish_id, d.name AS dish_name, d.dish_type, d.price AS dish_price,
                   i.id AS ingredient_id, i.name AS ingredient_name, 
                   i.price AS ingredient_price, i.category
            FROM dish_ingredient di
            JOIN dish d ON d.id = di.id_dish
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE di.id_dish = ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Dish dish = new Dish(
                    rs.getInt("dish_id"),
                    rs.getString("dish_name"),
                    DishTypeEnum.valueOf(rs.getString("dish_type")),
                    rs.getObject("dish_price") != null ? rs.getDouble("dish_price") : null
            );

            Ingredient ingredient = new Ingredient(
                    rs.getInt("ingredient_id"),
                    rs.getString("ingredient_name"),
                    rs.getDouble("ingredient_price"),
                    CategoryEnum.valueOf(rs.getString("category").toUpperCase())
            );

            double quantity = rs.getDouble("quantity_required");
            Unit unit = Unit.valueOf(rs.getString("unit"));

            return new DishIngredient(dish, ingredient, quantity, unit);
        }, dishId);
    }

    public Dish save(Dish dish) {
        int dishId;

        if (dish.getId() > 0) {
            // Update
            String sql = "UPDATE dish SET name = ?, dish_type = ?::dish_type, price = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    dish.getName(),
                    dish.getDishType().name(),
                    dish.getSellingPrice(),
                    dish.getId()
            );
            dishId = dish.getId();
        } else {
            // Insert
            String sql = "INSERT INTO dish (name, dish_type, price) VALUES (?, ?::dish_type, ?) RETURNING id";
            dishId = jdbcTemplate.queryForObject(sql, Integer.class,
                    dish.getName(),
                    dish.getDishType().name(),
                    dish.getSellingPrice()
            );
        }

        updateDishIngredients(dishId, dish.getDishIngredients());

        return findByIdWithIngredients(dishId);
    }

    public void updateDishIngredients(int dishId, List<DishIngredient> dishIngredients) {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        jdbcTemplate.update(deleteSql, dishId);

        if (dishIngredients == null || dishIngredients.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit)
            VALUES (?, ?, ?, ?::unit_type)
        """;

        List<Object[]> batchArgs = new ArrayList<>();
        for (DishIngredient di : dishIngredients) {
            batchArgs.add(new Object[]{
                    dishId,
                    di.getIngredient().getId(),
                    di.getQuantity(),
                    di.getUnit().name()
            });
        }

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    public void associateIngredients(int dishId, List<Integer> ingredientIds) {
        String deleteSql = "DELETE FROM dish_ingredient WHERE id_dish = ?";
        jdbcTemplate.update(deleteSql, dishId);

        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return;
        }

        String insertSql = """
            INSERT INTO dish_ingredient (id_dish, id_ingredient, quantity_required, unit)
            VALUES (?, ?, 1.0, 'KG')
        """;

        List<Object[]> batchArgs = new ArrayList<>();
        for (Integer ingredientId : ingredientIds) {
            batchArgs.add(new Object[]{dishId, ingredientId});
        }

        jdbcTemplate.batchUpdate(insertSql, batchArgs);
    }

    public List<Dish> findByIngredientName(String ingredientName) {
        String sql = """
            SELECT DISTINCT d.id, d.name, d.dish_type, d.price
            FROM dish d
            JOIN dish_ingredient di ON di.id_dish = d.id
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE i.name ILIKE ?
            ORDER BY d.id
        """;

        List<Dish> dishes = jdbcTemplate.query(sql, DISH_ROW_MAPPER, "%" + ingredientName + "%");

        for (Dish dish : dishes) {
            dish.setDishIngredients(findDishIngredientsByDishId(dish.getId()));
        }

        return dishes;
    }

    public double calculateDishCost(int dishId) {
        String sql = """
            SELECT SUM(i.price * di.quantity_required) as total_cost
            FROM dish_ingredient di
            JOIN ingredient i ON i.id = di.id_ingredient
            WHERE di.id_dish = ?
        """;

        Double total = jdbcTemplate.queryForObject(sql, Double.class, dishId);
        return total != null ? total : 0.0;
    }

    public double calculateGrossMargin(int dishId) {
        String sql = "SELECT price FROM dish WHERE id = ?";
        Double sellingPrice = jdbcTemplate.queryForObject(sql, Double.class, dishId);

        if (sellingPrice == null) {
            throw new IllegalStateException("Selling price not set for dish id=" + dishId);
        }

        double cost = calculateDishCost(dishId);
        return sellingPrice - cost;
    }
}