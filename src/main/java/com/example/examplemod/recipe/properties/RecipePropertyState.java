package com.example.examplemod.recipe.properties;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the player's current property selections for recipe exports.
 */
public class RecipePropertyState {
    private final Map<String, Integer> optionSelections = new HashMap<>();
    private final Map<String, String> valueSelections = new HashMap<>();
    private final Map<String, Integer> slotSelections = new HashMap<>();
    private final Map<String, Map<Integer, String>> slotValueSelections = new HashMap<>();

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
     * Returns the selected slot index for the given property, or {@code -1} when unset.
     */
    public int getSlotIndex(String propertyId) {
        return slotSelections.getOrDefault(propertyId, -1);
    }

    /**
     * Stores the selected slot index for the property. Negative values clear the selection.
     */
    public void setSlotIndex(String propertyId, int slotIndex) {
        if (slotIndex < 0) {
            slotSelections.remove(propertyId);
        } else {
            slotSelections.put(propertyId, slotIndex);
        }
    }

    /**
     * Retrieves the stored value for a specific slot-aware property slot, or {@code null} when absent.
     */
    public String getSlotValue(String propertyId, int slotIndex) {
        Map<Integer, String> perSlot = slotValueSelections.get(propertyId);
        if (perSlot == null) {
            return null;
        }
        return perSlot.get(slotIndex);
    }

    /**
     * Stores a value for the specific slot of a property. Blank values remove the slot entry.
     */
    public void setSlotValue(String propertyId, int slotIndex, String value) {
        if (slotIndex < 0) {
            return;
        }

        if (value == null || value.isBlank()) {
            Map<Integer, String> perSlot = slotValueSelections.get(propertyId);
            if (perSlot != null) {
                perSlot.remove(slotIndex);
                if (perSlot.isEmpty()) {
                    slotValueSelections.remove(propertyId);
                }
            }
            return;
        }

        slotValueSelections
            .computeIfAbsent(propertyId, key -> new HashMap<>())
            .put(slotIndex, value);
    }

    /**
     * Clears all stored slot selections and values.
     */
    public void clearSlots() {
        slotSelections.clear();
        slotValueSelections.clear();
    }

    /**
     * Clears every saved selection and value.
     */
    public void clear() {
        clearOptions();
        clearValues();
        clearSlots();
    }
}
