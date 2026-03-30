package org.zenith.dishIngredients.service;

import org.springframework.stereotype.Service;
import org.zenith.dishIngredients.entity.Unit;

import java.util.HashMap;
import java.util.Map;

@Service
public class UnitConversionService {

    private static final Map<String, Map<Unit, Double>> CONVERSION_TO_KG = new HashMap<>();

    static {
        // Tomate : 1 KG = 10 PCS
        Map<Unit, Double> tomatoConversion = new HashMap<>();
        tomatoConversion.put(Unit.KG, 1.0);
        tomatoConversion.put(Unit.PCS, 0.1);  // 1 PCS = 0.1 KG
        CONVERSION_TO_KG.put("tomate", tomatoConversion);

        // Laitue : 1 KG = 2 PCS
        Map<Unit, Double> lettuceConversion = new HashMap<>();
        lettuceConversion.put(Unit.KG, 1.0);
        lettuceConversion.put(Unit.PCS, 0.5);  // 1 PCS = 0.5 KG
        CONVERSION_TO_KG.put("laitue", lettuceConversion);

        // Chocolat : 1 KG = 10 PCS = 2.5 L
        Map<Unit, Double> chocolateConversion = new HashMap<>();
        chocolateConversion.put(Unit.KG, 1.0);
        chocolateConversion.put(Unit.PCS, 0.1);   // 1 PCS = 0.1 KG
        chocolateConversion.put(Unit.L, 0.4);     // 1 L = 0.4 KG (car 2.5L = 1KG → 1L = 0.4KG)
        CONVERSION_TO_KG.put("chocolat", chocolateConversion);

        // Poulet : 1 KG = 8 PCS
        Map<Unit, Double> chickenConversion = new HashMap<>();
        chickenConversion.put(Unit.KG, 1.0);
        chickenConversion.put(Unit.PCS, 0.125); // 1 PCS = 0.125 KG
        CONVERSION_TO_KG.put("poulet", chickenConversion);

        // Beurre : 1 KG = 4 PCS = 5 L
        Map<Unit, Double> butterConversion = new HashMap<>();
        butterConversion.put(Unit.KG, 1.0);
        butterConversion.put(Unit.PCS, 0.25);   // 1 PCS = 0.25 KG
        butterConversion.put(Unit.L, 0.2);      // 1 L = 0.2 KG (car 5L = 1KG → 1L = 0.2KG)
        CONVERSION_TO_KG.put("beurre", butterConversion);
    }

    public double convertToKg(String ingredientName, double quantity, Unit unit) {
        if (unit == Unit.KG) {
            return quantity;
        }

        Map<Unit, Double> conversionMap = CONVERSION_TO_KG.get(ingredientName.toLowerCase());
        if (conversionMap == null) {
            throw new IllegalArgumentException(
                    "No conversion defined for ingredient: " + ingredientName
            );
        }

        Double factor = conversionMap.get(unit);
        if (factor == null) {
            throw new IllegalArgumentException(
                    "Cannot convert " + unit + " to KG for ingredient: " + ingredientName
            );
        }

        return quantity * factor;
    }

    public boolean isConvertible(String ingredientName, Unit fromUnit, Unit toUnit) {
        if (fromUnit == toUnit) {
            return true;
        }

        try {
            convertToKg(ingredientName, 1.0, fromUnit);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public double getConversionFactor(String ingredientName, Unit fromUnit, Unit toUnit) {
        if (fromUnit == toUnit) {
            return 1.0;
        }

        double inKg = convertToKg(ingredientName, 1.0, fromUnit);

        if (toUnit == Unit.KG) {
            return inKg;
        }

        Map<Unit, Double> conversionMap = CONVERSION_TO_KG.get(ingredientName.toLowerCase());
        if (conversionMap == null) {
            throw new IllegalArgumentException("No conversion defined for ingredient: " + ingredientName);
        }

        Double factorToKg = conversionMap.get(toUnit);
        if (factorToKg == null) {
            throw new IllegalArgumentException(
                    "Cannot convert KG to " + toUnit + " for ingredient: " + ingredientName
            );
        }

        return inKg / factorToKg;
    }
}