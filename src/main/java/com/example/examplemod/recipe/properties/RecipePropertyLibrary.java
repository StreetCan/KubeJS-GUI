package com.example.examplemod.recipe.properties;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds the available recipe property definitions scoped by mod/recipe type
 * and assists with script generation.
 */
public final class RecipePropertyLibrary {

    public static final String INGREDIENT_COUNT_ID = "common.ingredientCount";
    public static final String RESULT_COUNT_ID = "common.resultCount";

    private static final Map<String, Map<String, List<PropertyDefinition>>> PROPERTIES_BY_MOD = new HashMap<>();

    private static final PropertyDefinition INGREDIENT_COUNT = PropertyDefinition.builder(INGREDIENT_COUNT_ID, "Ingredient Count")
        .value("1", "%1$s")
        .numeric(true)
        .valueHint("Count")
        .slot(SlotDomain.INPUT, true)
        .emitter((definition, state, context) -> Optional.empty())
        .build();

    private static final PropertyDefinition RESULT_COUNT = PropertyDefinition.builder(RESULT_COUNT_ID, "Result Count")
        .value("1", "%1$s")
        .numeric(true)
        .valueHint("Count")
        .slot(SlotDomain.OUTPUT, true)
        .emitter((definition, state, context) -> Optional.empty())
        .build();

    private static final PropertyDefinition CREATE_HEAT_REQUIREMENT = PropertyDefinition.builder("create.heatRequirement", "Heat Requirement")
        .select("heatRequirement: \"%1$s\",", List.of(
            PropertyOption.skip("None"),
            PropertyOption.emit("Heated", "heated"),
            PropertyOption.emit("Superheated", "superheated")
        ))
        .build();

    private static final PropertyDefinition CREATE_REQUIRED_FLUID = PropertyDefinition.builder("create.requiredFluid", "Required Fluid")
        .select("requiredFluid: \"%1$s\",", List.of(
            PropertyOption.skip("None"),
            PropertyOption.emit("Water", "minecraft:water"),
            PropertyOption.emit("Lava", "minecraft:lava")
        ))
        .build();

    private static final PropertyDefinition CREATE_PROCESSING_TIME = PropertyDefinition.builder("create.processingTime", "Processing Time")
        .value("100", "processingTime: %1$s,")
        .numeric(true)
        .valueHint("Ticks")
        .build();

    private static final PropertyDefinition COOKING_TIME = PropertyDefinition.builder("farmersdelight.cookingTime", "Cooking Time")
        .value("200", "cookingTime: %1$s,")
        .numeric(true)
        .valueHint("Ticks")
        .build();

    private static final PropertyDefinition COOKING_EXPERIENCE = PropertyDefinition.builder("farmersdelight.experience", "Experience")
        .value("0.35", "experience: %1$s,")
        .numeric(false)
        .valueHint("Value")
        .build();

    private static final PropertyDefinition FARMERS_DELIGHT_TOOL_DAMAGE = PropertyDefinition.builder("farmersdelight.toolDamage", "Tool Damage")
        .value("1", "toolDamage: %1$s,")
        .numeric(true)
        .valueHint("Points")
        .build();

    private static final PropertyDefinition KEEP_HELD_ITEM = PropertyDefinition.builder("common.keepHeldItem", "Keep Held Item")
        .select("keepHeldItem: %1$s,", List.of(
            PropertyOption.skip("Default"),
            PropertyOption.emit("True", "true"),
            PropertyOption.emit("False", "false")
        ))
        .slot(SlotDomain.INPUT, true)
        .emitter((definition, state, context) -> {
            List<PropertyOption> options = definition.getOptions();
            if (options.isEmpty()) {
                return Optional.empty();
            }

            int index = state.getOptionIndex(definition.getId());
            if (index < 0 || index >= options.size()) {
                index = 0;
            }

            PropertyOption option = options.get(index);
            if (!option.shouldEmit()) {
                return Optional.empty();
            }

            int slotIndex = state.getSlotIndex(definition.getId());
            if (slotIndex < 0) {
                return Optional.empty();
            }

            String heldItemId = context.getInputItemId(slotIndex);
            if (heldItemId == null || heldItemId.isBlank()) {
                return Optional.empty();
            }

            StringBuilder snippet = new StringBuilder();
            snippet.append("heldItem: { item: \"").append(heldItemId).append("\" },\n");
            snippet.append("    keepHeldItem: ").append(option.getScriptValue()).append(',');
            return Optional.of(snippet.toString());
        })
        .build();

    static {
        registerCreate();
        registerFarmersDelight();
    }

    private RecipePropertyLibrary() {
    }

    private static void registerCreate() {
        register("create", "mixing",
            CREATE_HEAT_REQUIREMENT,
            CREATE_REQUIRED_FLUID,
            CREATE_PROCESSING_TIME,
            INGREDIENT_COUNT,
            RESULT_COUNT
        );

        register("create", "pressing",
            CREATE_PROCESSING_TIME,
            INGREDIENT_COUNT,
            RESULT_COUNT
        );

        register("create", "deploying",
            KEEP_HELD_ITEM,
            INGREDIENT_COUNT,
            RESULT_COUNT
        );

        register("create", "cutting",
            INGREDIENT_COUNT,
            RESULT_COUNT
        );
    }

    private static void registerFarmersDelight() {
        register("farmersdelight", "cooking",
            COOKING_TIME,
            COOKING_EXPERIENCE,
            INGREDIENT_COUNT,
            RESULT_COUNT
        );

        register("farmersdelight", "cutting",
            FARMERS_DELIGHT_TOOL_DAMAGE,
            KEEP_HELD_ITEM,
            INGREDIENT_COUNT,
            RESULT_COUNT
        );
    }

    private static void register(String modId, String recipePath, PropertyDefinition... definitions) {
        Map<String, List<PropertyDefinition>> byPath = PROPERTIES_BY_MOD.computeIfAbsent(modId, key -> new HashMap<>());
        List<PropertyDefinition> list = byPath.computeIfAbsent(recipePath, key -> new ArrayList<>());
        Collections.addAll(list, definitions);
    }

    /**
     * Returns an immutable list of all known property definitions for the given recipe type.
     */
    public static List<PropertyDefinition> getPropertiesFor(String recipeTypeId) {
        if (recipeTypeId == null || recipeTypeId.isBlank()) {
            return List.of();
        }

        ResourceLocation id;
        try {
            id = new ResourceLocation(recipeTypeId);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }

        Map<String, List<PropertyDefinition>> byPath = PROPERTIES_BY_MOD.get(id.getNamespace());
        if (byPath == null) {
            return List.of();
        }

        List<PropertyDefinition> results = new ArrayList<>();
        List<PropertyDefinition> wildcard = byPath.get("*");
        if (wildcard != null) {
            results.addAll(wildcard);
        }

        List<PropertyDefinition> specific = byPath.get(id.getPath());
        if (specific != null) {
            results.addAll(specific);
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Appends the selected property snippets to the script builder using the given context.
     */
    public static void appendSelectedProperties(StringBuilder script, String recipeTypeId,
                                                RecipePropertyState state, ItemStackHandler inputs,
                                                ItemStackHandler outputs) {
        List<PropertyDefinition> definitions = getPropertiesFor(recipeTypeId);
        if (definitions.isEmpty()) {
            return;
        }

        RecipeContext context = new RecipeContext(recipeTypeId, inputs, outputs);
        for (PropertyDefinition definition : definitions) {
            Optional<String> snippet = definition.buildSnippet(state, context);
            if (snippet.isPresent()) {
                String value = snippet.get();
                if (!value.isBlank()) {
                    script.append("    ").append(value).append('\n');
                }
            }
        }
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    /**
     * Supported property types.
     */
    public enum PropertyType {
        SELECT,
        VALUE
    }

    /**
     * Slot domains supported by slot-aware properties.
     */
    public enum SlotDomain {
        NONE,
        INPUT,
        OUTPUT
    }

    /**
     * Context passed to snippet emitters for recipe-aware decisions.
     */
    public static final class RecipeContext {
        private final String recipeTypeId;
        private final ItemStackHandler inputs;
        private final ItemStackHandler outputs;

        private RecipeContext(String recipeTypeId, ItemStackHandler inputs, ItemStackHandler outputs) {
            this.recipeTypeId = recipeTypeId;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        public String getRecipeTypeId() {
            return recipeTypeId;
        }

        public String getInputItemId(int slotIndex) {
            if (inputs == null || slotIndex < 0 || slotIndex >= inputs.getSlots()) {
                return "";
            }
            return itemId(inputs.getStackInSlot(slotIndex));
        }

        public String getOutputItemId(int slotIndex) {
            if (outputs == null || slotIndex < 0 || slotIndex >= outputs.getSlots()) {
                return "";
            }
            return itemId(outputs.getStackInSlot(slotIndex));
        }
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
        private final SlotDomain slotDomain;
        private final boolean slotRequired;
        private final boolean numericValue;
        private final String valueHint;
        private final SnippetEmitter emitter;

        private PropertyDefinition(String id, String label, PropertyType type,
                                   List<PropertyOption> options, String snippetTemplate,
                                   String defaultValue, SlotDomain slotDomain,
                                   boolean slotRequired, boolean numericValue,
                                   String valueHint, SnippetEmitter emitter) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.options = options;
            this.snippetTemplate = snippetTemplate;
            this.defaultValue = defaultValue;
            this.slotDomain = slotDomain;
            this.slotRequired = slotRequired;
            this.numericValue = numericValue;
            this.valueHint = valueHint;
            this.emitter = emitter;
        }

        public static Builder builder(String id, String label) {
            return new Builder(id, label);
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

        public SlotDomain getSlotDomain() {
            return slotDomain;
        }

        public boolean requiresSlotSelection() {
            return slotDomain != SlotDomain.NONE && slotRequired;
        }

        public boolean supportsSlotSelection() {
            return slotDomain != SlotDomain.NONE;
        }

        public boolean isNumericValue() {
            return numericValue;
        }

        public String getValueHint() {
            return valueHint;
        }

        private Optional<String> buildSnippet(RecipePropertyState state, RecipeContext context) {
            if (emitter != null) {
                return emitter.emit(this, state, context);
            }

            if (type == PropertyType.SELECT) {
                return buildSelectSnippet(state);
            } else if (type == PropertyType.VALUE) {
                return buildValueSnippet(state);
            }
            return Optional.empty();
        }

        private Optional<String> buildSelectSnippet(RecipePropertyState state) {
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

            if (slotDomain != SlotDomain.NONE) {
                int slotIndex = state.getSlotIndex(id);
                if (slotRequired && slotIndex < 0) {
                    return Optional.empty();
                }
                return Optional.of(String.format(snippetTemplate, option.getScriptValue(), slotIndex));
            }

            return Optional.of(String.format(snippetTemplate, option.getScriptValue()));
        }

        private Optional<String> buildValueSnippet(RecipePropertyState state) {
            if (slotDomain != SlotDomain.NONE) {
                int slotIndex = state.getSlotIndex(id);
                if (slotRequired && slotIndex < 0) {
                    return Optional.empty();
                }

                if (slotIndex < 0) {
                    return Optional.empty();
                }

                String value = state.getSlotValue(id, slotIndex);
                if (value == null || value.isBlank()) {
                    return Optional.empty();
                }

                return Optional.of(String.format(snippetTemplate, value, slotIndex));
            }

            if (!state.hasValue(id)) {
                return Optional.empty();
            }

            String value = state.getValue(id);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(String.format(snippetTemplate, value));
        }

        /**
         * Builder for {@link PropertyDefinition} instances.
         */
        public static final class Builder {
            private final String id;
            private final String label;
            private PropertyType type = PropertyType.SELECT;
            private List<PropertyOption> options = List.of();
            private String snippetTemplate = "";
            private String defaultValue = "";
            private SlotDomain slotDomain = SlotDomain.NONE;
            private boolean slotRequired = false;
            private boolean numericValue = true;
            private String valueHint = "";
            private SnippetEmitter emitter;

            private Builder(String id, String label) {
                this.id = id;
                this.label = label;
            }

            public Builder select(String snippetTemplate, List<PropertyOption> options) {
                this.type = PropertyType.SELECT;
                this.snippetTemplate = snippetTemplate;
                this.options = options;
                return this;
            }

            public Builder value(String defaultValue, String snippetTemplate) {
                this.type = PropertyType.VALUE;
                this.defaultValue = defaultValue;
                this.snippetTemplate = snippetTemplate;
                return this;
            }

            public Builder slot(SlotDomain domain, boolean required) {
                this.slotDomain = domain;
                this.slotRequired = required;
                return this;
            }

            public Builder numeric(boolean numeric) {
                this.numericValue = numeric;
                return this;
            }

            public Builder valueHint(String hint) {
                this.valueHint = hint;
                return this;
            }

            public Builder emitter(SnippetEmitter emitter) {
                this.emitter = emitter;
                return this;
            }

            public PropertyDefinition build() {
                return new PropertyDefinition(id, label, type, options, snippetTemplate,
                    defaultValue, slotDomain, slotRequired, numericValue, valueHint, emitter);
            }
        }
    }

    /**
     * Functional interface for custom snippet emitters.
     */
    @FunctionalInterface
    public interface SnippetEmitter {
        Optional<String> emit(PropertyDefinition definition, RecipePropertyState state, RecipeContext context);
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
