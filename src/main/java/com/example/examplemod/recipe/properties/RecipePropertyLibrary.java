package com.example.examplemod.recipe.properties;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Holds the available recipe property definitions and assists with script generation.
 */
public final class RecipePropertyLibrary {
    private static final List<PropertyDefinition> PROPERTIES = List.of(
        PropertyDefinition.forSelect(
            "heatRequirement",
            "Heat Requirement",
            "heatRequirement: \"%s\",",
            List.of(
                PropertyOption.skip("None"),
                PropertyOption.emit("Heated", "heated"),
                PropertyOption.emit("Superheated", "superheated")
            )
        ),
        PropertyDefinition.forSelect(
            "requiredFluid",
            "Required Fluid",
            "requiredFluid: \"%s\",",
            List.of(
                PropertyOption.skip("None"),
                PropertyOption.emit("Water", "minecraft:water"),
                PropertyOption.emit("Lava", "minecraft:lava")
            )
        ),
        PropertyDefinition.forSelect(
            "keepHeldItem",
            "Keep Held Item",
            "keepHeldItem: %s,",
            List.of(
                PropertyOption.skip("Default"),
                PropertyOption.emit("True", "true"),
                PropertyOption.emit("False", "false")
            )
        ),
        PropertyDefinition.forValue(
            "processingTime",
            "Processing Time",
            "processingTime: %s,",
            "100"
        )
    );

    private RecipePropertyLibrary() {
    }

    /**
     * Returns an immutable list of all known property definitions.
     */
    public static List<PropertyDefinition> getProperties() {
        return Collections.unmodifiableList(PROPERTIES);
    }

    /**
     * Appends the selected property snippets to the script builder.
     */
    public static void appendSelectedProperties(StringBuilder script, RecipePropertyState state) {
        for (PropertyDefinition definition : PROPERTIES) {
            Optional<String> snippet = definition.buildSnippet(state);
            if (snippet.isPresent()) {
                script.append("    ").append(snippet.get()).append('\n');
            }
        }
    }

    /**
     * Supported property types.
     */
    public enum PropertyType {
        SELECT,
        VALUE
    }

    /**
     * Definition for an individual property.
     */
    public static final class PropertyDefinition {
        private final String id;
        private final String label;
        private final PropertyType type;
        private final List<PropertyOption> options;
        private final String snippetTemplate;
        private final String defaultValue;

        private PropertyDefinition(String id, String label, PropertyType type,
                                   List<PropertyOption> options, String snippetTemplate,
                                   String defaultValue) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.options = options;
            this.snippetTemplate = snippetTemplate;
            this.defaultValue = defaultValue;
        }

        public static PropertyDefinition forSelect(String id, String label, String snippetTemplate,
                                                    List<PropertyOption> options) {
            return new PropertyDefinition(id, label, PropertyType.SELECT, options, snippetTemplate, "");
        }

        public static PropertyDefinition forValue(String id, String label, String snippetTemplate,
                                                   String defaultValue) {
            return new PropertyDefinition(id, label, PropertyType.VALUE, List.of(), snippetTemplate, defaultValue);
        }

        public String getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }

        public PropertyType getType() {
            return type;
        }

        public List<PropertyOption> getOptions() {
            return options;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        private Optional<String> buildSnippet(RecipePropertyState state) {
            if (type == PropertyType.SELECT) {
                if (options.isEmpty()) {
                    return Optional.empty();
                }
                int index = state.getOptionIndex(id);
                if (index < 0 || index >= options.size()) {
                    index = 0;
                }
                PropertyOption option = options.get(index);
                if (!option.shouldEmit()) {
                    return Optional.empty();
                }
                return Optional.of(String.format(snippetTemplate, option.getScriptValue()));
            } else if (type == PropertyType.VALUE) {
                if (!state.hasValue(id)) {
                    return Optional.empty();
                }
                String value = state.getValue(id);
                if (value == null || value.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(String.format(snippetTemplate, value));
            }
            return Optional.empty();
        }
    }

    /**
     * Individual option for select-style properties.
     */
    public static final class PropertyOption {
        private final String displayName;
        private final String scriptValue;
        private final boolean emit;

        private PropertyOption(String displayName, String scriptValue, boolean emit) {
            this.displayName = displayName;
            this.scriptValue = scriptValue;
            this.emit = emit;
        }

        public static PropertyOption emit(String displayName, String scriptValue) {
            return new PropertyOption(displayName, scriptValue, true);
        }

        public static PropertyOption skip(String displayName) {
            return new PropertyOption(displayName, "", false);
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getScriptValue() {
            return scriptValue;
        }

        public boolean shouldEmit() {
            return emit;
        }
    }
}
