package org.zenith.dishIngredients.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.zenith.dishIngredients.entity.CategoryEnum;
import org.zenith.dishIngredients.entity.Ingredient;
import org.zenith.dishIngredients.entity.StockMouvement;
import org.zenith.dishIngredients.entity.StockValue;
import org.zenith.dishIngredients.entity.Unit;
import org.zenith.dishIngredients.entity.MouvementTypeEnum;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final JdbcTemplate jdbcTemplate;

    public IngredientRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Ingredient> INGREDIENT_ROW_MAPPER = (rs, rowNum) ->
            new Ingredient(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("price"),
                    CategoryEnum.valueOf(rs.getString("category").toUpperCase())
            );

    public List<Ingredient> findAll() {
        String sql = "SELECT id, name, price, category FROM ingredient ORDER BY id";
        return jdbcTemplate.query(sql, INGREDIENT_ROW_MAPPER);
    }

    public Ingredient findById(int id) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";
        List<Ingredient> results = jdbcTemplate.query(sql, INGREDIENT_ROW_MAPPER, id);

        if (results.isEmpty()) {
            throw new RuntimeException("Ingredient not found (id=" + id + ")");
        }
        return results.get(0);
    }

    public Ingredient findByIdWithStockMovements(int id) {
        Ingredient ingredient = findById(id);

        String sql = """
            SELECT id, quantity, type, unit, creation_datetime 
            FROM stock_movement 
            WHERE id_ingredient = ? 
            ORDER BY creation_datetime
        """;

        List<StockMouvement> movements = jdbcTemplate.query(sql, (rs, rowNum) -> {
            StockValue value = new StockValue(
                    rs.getDouble("quantity"),
                    Unit.valueOf(rs.getString("unit"))
            );
            return new StockMouvement(
                    rs.getInt("id"),
                    value,
                    MouvementTypeEnum.valueOf(rs.getString("type")),
                    rs.getTimestamp("creation_datetime").toInstant()
            );
        }, id);

        ingredient.setStockMovementList(movements);
        return ingredient;
    }

    public List<Ingredient> findPaginated(int page, int size) {
        int offset = (page - 1) * size;
        String sql = """
            SELECT i.id, i.name, i.price, i.category
            FROM ingredient i
            ORDER BY i.id
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, INGREDIENT_ROW_MAPPER, size, offset);
    }

    public List<Ingredient> findByCriteria(String ingredientName, CategoryEnum category,
                                           String dishName, int page, int size) {
        int offset = (page - 1) * size;
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT i.id, i.name, i.price, i.category
            FROM ingredient i
            LEFT JOIN dish_ingredient di ON di.id_ingredient = i.id
            LEFT JOIN dish d ON d.id = di.id_dish
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (ingredientName != null && !ingredientName.isBlank()) {
            sql.append(" AND i.name ILIKE ?");
            params.add("%" + ingredientName + "%");
        }

        if (category != null) {
            sql.append(" AND i.category = ?::ingredient_category");
            params.add(category.name());
        }

        if (dishName != null && !dishName.isBlank()) {
            sql.append(" AND d.name ILIKE ?");
            params.add("%" + dishName + "%");
        }

        sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), INGREDIENT_ROW_MAPPER, params.toArray());
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM ingredient WHERE name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return count != null && count > 0;
    }

    public Ingredient save(Ingredient ingredient) {
        if (ingredient.getId() > 0) {
            // Update
            String sql = """
                UPDATE ingredient 
                SET name = ?, category = ?::ingredient_category, price = ? 
                WHERE id = ?
            """;
            jdbcTemplate.update(sql,
                    ingredient.getName(),
                    ingredient.getCategory().name(),
                    ingredient.getPrice(),
                    ingredient.getId()
            );
            return ingredient;
        } else {
            // Insert
            String sql = """
                INSERT INTO ingredient (name, category, price) 
                VALUES (?, ?::ingredient_category, ?) 
                RETURNING id
            """;
            Integer generatedId = jdbcTemplate.queryForObject(sql, Integer.class,
                    ingredient.getName(),
                    ingredient.getCategory().name(),
                    ingredient.getPrice()
            );
            return new Ingredient(generatedId, ingredient.getName(),
                    ingredient.getPrice(), ingredient.getCategory());
        }
    }

    public List<Ingredient> createAll(List<Ingredient> ingredients) {
        // Vérifier les doublons dans la liste
        for (int i = 0; i < ingredients.size(); i++) {
            for (int j = i + 1; j < ingredients.size(); j++) {
                if (ingredients.get(i).getName().equalsIgnoreCase(ingredients.get(j).getName())) {
                    throw new RuntimeException("Duplicate ingredient in list: " + ingredients.get(i).getName());
                }
            }
        }

        // Vérifier que les ingrédients n'existent pas déjà en base
        for (Ingredient ingredient : ingredients) {
            if (existsByName(ingredient.getName())) {
                throw new RuntimeException("Ingredient already exists: " + ingredient.getName());
            }
        }

        // Insertion en batch
        String sql = "INSERT INTO ingredient (name, category, price) VALUES (?, ?::ingredient_category, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            batchArgs.add(new Object[]{
                    ingredient.getName(),
                    ingredient.getCategory().name(),
                    ingredient.getPrice()
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);

        List<Ingredient> savedIngredients = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            Ingredient saved = findByName(ingredient.getName());
            savedIngredients.add(saved);
        }

        return savedIngredients;
    }

    public Ingredient findByName(String name) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE name = ?";
        List<Ingredient> results = jdbcTemplate.query(sql, INGREDIENT_ROW_MAPPER, name);
        if (results.isEmpty()) {
            throw new RuntimeException("Ingredient not found: " + name);
        }
        return results.get(0);
    }

    public double getStockQuantityAt(int ingredientId, Instant at) {
        String sql = """
            SELECT COALESCE(
                SUM(CASE WHEN type = 'IN' THEN quantity ELSE -quantity END), 0
            ) as total
            FROM stock_movement
            WHERE id_ingredient = ? AND creation_datetime <= ?
        """;

        Double total = jdbcTemplate.queryForObject(sql, Double.class, ingredientId, at);
        return total != null ? total : 0.0;
    }

    public List<StockMouvement> findStockMovementsByIngredientId(int ingredientId, Instant from, Instant to) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, quantity, type, unit, creation_datetime
            FROM stock_movement
            WHERE id_ingredient = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(ingredientId);

        if (from != null) {
            sql.append(" AND creation_datetime >= ?");
            params.add(Timestamp.from(from));
        }

        if (to != null) {
            sql.append(" AND creation_datetime <= ?");
            params.add(Timestamp.from(to));
        }

        sql.append(" ORDER BY creation_datetime ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            StockValue value = new StockValue(
                    rs.getDouble("quantity"),
                    Unit.valueOf(rs.getString("unit"))
            );
            return new StockMouvement(
                    rs.getInt("id"),
                    value,
                    MouvementTypeEnum.valueOf(rs.getString("type")),
                    rs.getTimestamp("creation_datetime").toInstant()
            );
        }, params.toArray());
    }

    public List<StockMouvement> saveStockMovements(int ingredientId, List<StockMouvement> movements) {
        if (movements == null || movements.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = """
            INSERT INTO stock_movement (id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?::movement_type, ?::unit_type, ?)
            RETURNING id, quantity, type, unit, creation_datetime
        """;

        List<StockMouvement> savedMovements = new ArrayList<>();

        for (StockMouvement movement : movements) {
            StockValue value = movement.getValue();
            Instant creationDateTime = movement.getCreationDateTime() != null
                    ? movement.getCreationDateTime()
                    : Instant.now();

            List<StockMouvement> result = jdbcTemplate.query(sql,
                    (rs, rowNum) -> {
                        StockValue savedValue = new StockValue(
                                rs.getDouble("quantity"),
                                Unit.valueOf(rs.getString("unit"))
                        );
                        return new StockMouvement(
                                rs.getInt("id"),
                                savedValue,
                                MouvementTypeEnum.valueOf(rs.getString("type")),
                                rs.getTimestamp("creation_datetime").toInstant()
                        );
                    },
                    ingredientId,
                    value.getQuantity(),
                    movement.getType().name(),
                    value.getUnit().name(),
                    Timestamp.from(creationDateTime)
            );

            if (!result.isEmpty()) {
                savedMovements.add(result.get(0));
            }
        }

        return savedMovements;
    }
}