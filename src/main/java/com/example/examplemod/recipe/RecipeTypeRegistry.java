package com.example.examplemod.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages detection and categorization of all recipe types from loaded mods.
 */
public class RecipeTypeRegistry {

    private static Map<String, List<RecipeTypeInfo>> recipeTypesByMod = new LinkedHashMap<>();
    private static boolean initialized = false;

    /**
     * Scans all loaded recipe types and organizes them by mod namespace.
     */
    public static void scanRecipeTypes() {
        if (initialized) return;

        recipeTypesByMod.clear();

        // Get all recipe types from the registry
        Set<ResourceLocation> recipeTypes = new HashSet<>();

        // Scan recipe manager for all known recipe types
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getRecipeManager() != null) {
            mc.level.getRecipeManager().getRecipes().forEach(recipe -> {
                String typeId = recipe.getType().toString();
                recipeTypes.add(new ResourceLocation(typeId));
            });
        }

        // Also add standard recipe types from registry
        ForgeRegistries.RECIPE_TYPES.getKeys().forEach(recipeTypes::add);

        // Always expose standard crafting subtypes so users can target them directly.
        recipeTypes.add(new ResourceLocation("minecraft", "crafting_shaped"));
        recipeTypes.add(new ResourceLocation("minecraft", "crafting_shapeless"));

        // Organize by mod namespace
        for (ResourceLocation typeId : recipeTypes) {
            String modId = typeId.getNamespace();
            String recipePath = typeId.getPath();

            if ("crafting".equals(recipePath)) {
                continue;
            }

            RecipeTypeInfo info = new RecipeTypeInfo(typeId, modId, recipePath);

            recipeTypesByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(info);
        }

        // Sort each mod's recipe types alphabetically
        recipeTypesByMod.values().forEach(list ->
            list.sort(Comparator.comparing(RecipeTypeInfo::getPath))
        );

        initialized = true;
    }

    /**
     * Gets all recipe types organized by mod.
     */
    public static Map<String, List<RecipeTypeInfo>> getRecipeTypesByMod() {
        if (!initialized) {
            scanRecipeTypes();
        }
        return Collections.unmodifiableMap(recipeTypesByMod);
    }

    /**
     * Gets all mod namespaces that have recipes, sorted alphabetically.
     */
    public static List<String> getModNamespaces() {
        if (!initialized) {
            scanRecipeTypes();
        }
        return new ArrayList<>(recipeTypesByMod.keySet());
    }

    /**
     * Gets recipe types for a specific mod.
     */
    public static List<RecipeTypeInfo> getRecipeTypesForMod(String modId) {
        if (!initialized) {
            scanRecipeTypes();
        }
        return recipeTypesByMod.getOrDefault(modId, Collections.emptyList());
    }

    /**
     * Information about a recipe type.
     */
    public static class RecipeTypeInfo {
        private final ResourceLocation id;
        private final String modId;
        private final String path;

        public RecipeTypeInfo(ResourceLocation id, String modId, String path) {
            this.id = id;
            this.modId = modId;
            this.path = path;
        }

        public ResourceLocation getId() {
            return id;
        }

        public String getModId() {
            return modId;
        }

        public String getPath() {
            return path;
        }

        public String getFullId() {
            return id.toString();
        }

        @Override
        public String toString() {
            return getFullId();
        }
    }
}
