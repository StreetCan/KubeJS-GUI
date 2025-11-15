package com.example.examplemod.recipe.properties;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the player's current property selections for recipe exports.
 */
public class RecipePropertyState {
    private final Map<String, Integer> optionSelections = new HashMap<>();
    private final Map<String, String> valueSelections = new HashMap<>();

    /**
     * Returns the saved option index for the given property. Defaults to 0.
     */
    public int getOptionIndex(String propertyId) {
        return optionSelections.getOrDefault(propertyId, 0);
    }

    /**
     * Stores the selected option index for the property.
     */
    public void setOptionIndex(String propertyId, int index) {
        optionSelections.put(propertyId, index);
    }

    /**
     * Clears all stored option selections.
     */
    public void clearOptions() {
        optionSelections.clear();
    }

    /**
     * Returns true if a custom value has been saved for the property.
     */
    public boolean hasValue(String propertyId) {
        return valueSelections.containsKey(propertyId);
    }

    /**
     * Retrieves the saved value for the property or {@code null} if none exists.
     */
    public String getValue(String propertyId) {
        return valueSelections.get(propertyId);
    }

    /**
     * Stores a value for the property. Blank or null values remove the entry.
     */
    public void setValue(String propertyId, String value) {
        if (value == null || value.isBlank()) {
            valueSelections.remove(propertyId);
        } else {
            valueSelections.put(propertyId, value);
        }
    }

    /**
     * Clears all saved values.
     */
    public void clearValues() {
        valueSelections.clear();
    }

    /**
     * Clears every saved selection and value.
     */
    public void clear() {
        clearOptions();
        clearValues();
    }
}
