package com.example.examplemod.client;

import com.example.examplemod.menu.RecipeEditorMenu;
import com.example.examplemod.recipe.RecipeTypeRegistry;
import com.example.examplemod.util.KubeJSExporter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen for the recipe editor GUI.
 * Allows users to visually create recipes and export them as KubeJS scripts.
 */
public class RecipeEditorScreen extends AbstractContainerScreen<RecipeEditorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    private EditBox recipeIdBox;
    private Button modFilterButton;
    private Button recipeTypeButton;
    private Button exportButton;
    private Button clearButton;

    private String selectedMod = "minecraft";
    private String selectedRecipeType = "minecraft:crafting_shaped";
    private List<String> availableMods;
    private List<RecipeTypeRegistry.RecipeTypeInfo> availableRecipeTypes;

    private int modFilterIndex = 0;
    private int recipeTypeIndex = 0;

    public RecipeEditorScreen(RecipeEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = -10; // Move title off screen to prevent slot misalignment

        // Initialize recipe type registry
        RecipeTypeRegistry.scanRecipeTypes();
        availableMods = RecipeTypeRegistry.getModNamespaces();
        updateAvailableRecipeTypes();
    }

    @Override
    protected void init() {
        super.init();

        // Position buttons to the left of the GUI (outside the container)
        int buttonX = this.leftPos - 90;
        int buttonY = this.topPos + 20;

        // Recipe ID input field (left side)
        this.recipeIdBox = new EditBox(this.font, buttonX, buttonY, 85, 16, Component.literal("Recipe ID"));
        this.recipeIdBox.setMaxLength(128);
        this.recipeIdBox.setValue("my_recipe");
        this.recipeIdBox.setHint(Component.literal("Recipe ID..."));
        this.addRenderableWidget(recipeIdBox);

        // Mod filter button (left side)
        this.modFilterButton = Button.builder(
            Component.literal("Mod: " + selectedMod),
            button -> cycleModFilter()
        ).bounds(buttonX, buttonY + 22, 85, 20).build();
        this.addRenderableWidget(modFilterButton);

        // Recipe type button (left side)
        this.recipeTypeButton = Button.builder(
            Component.literal(getShortRecipeTypeName()),
            button -> cycleRecipeType()
        ).bounds(buttonX, buttonY + 44, 85, 20).build();
        this.addRenderableWidget(recipeTypeButton);

        // Export button (left side)
        this.exportButton = Button.builder(
            Component.literal("Export"),
            button -> exportRecipe()
        ).bounds(buttonX, buttonY + 66, 85, 20).build();
        this.addRenderableWidget(exportButton);

        // Clear button (left side)
        this.clearButton = Button.builder(
            Component.literal("Clear"),
            button -> clearRecipe()
        ).bounds(buttonX, buttonY + 88, 85, 20).build();
        this.addRenderableWidget(clearButton);

        updateSlotConfiguration();
    }

    private void cycleModFilter() {
        if (availableMods.isEmpty()) return;

        modFilterIndex = (modFilterIndex + 1) % availableMods.size();
        selectedMod = availableMods.get(modFilterIndex);
        modFilterButton.setMessage(Component.literal("Mod: " + selectedMod));

        updateAvailableRecipeTypes();
        recipeTypeIndex = 0;
        if (!availableRecipeTypes.isEmpty()) {
            selectedRecipeType = availableRecipeTypes.get(0).getFullId();
            recipeTypeButton.setMessage(Component.literal(getShortRecipeTypeName()));
            updateSlotConfiguration();
        }
    }

    private void cycleRecipeType() {
        if (availableRecipeTypes.isEmpty()) return;

        recipeTypeIndex = (recipeTypeIndex + 1) % availableRecipeTypes.size();
        selectedRecipeType = availableRecipeTypes.get(recipeTypeIndex).getFullId();
        recipeTypeButton.setMessage(Component.literal(getShortRecipeTypeName()));
        updateSlotConfiguration();
    }

    private String getShortRecipeTypeName() {
        if (availableRecipeTypes.isEmpty()) return "No types";
        String fullType = selectedRecipeType;
        String[] parts = fullType.split(":");
        return parts.length > 1 ? parts[1] : fullType;
    }

    private void updateAvailableRecipeTypes() {
        availableRecipeTypes = RecipeTypeRegistry.getRecipeTypesForMod(selectedMod);
        if (!availableRecipeTypes.isEmpty()) {
            selectedRecipeType = availableRecipeTypes.get(0).getFullId();
        }
    }

    private void updateSlotConfiguration() {
        // Configure slots based on recipe type
        String recipeType = selectedRecipeType.toLowerCase();

        if (recipeType.contains("shaped") || recipeType.contains("crafting_shaped")) {
            menu.setActiveSlots(9, 1); // 3x3 crafting grid, 1 output
        } else if (recipeType.contains("shapeless") || recipeType.contains("crafting_shapeless")) {
            menu.setActiveSlots(9, 1); // Up to 9 inputs, 1 output
        } else if (recipeType.contains("smelting") || recipeType.contains("blasting") ||
                   recipeType.contains("smoking") || recipeType.contains("campfire")) {
            menu.setActiveSlots(1, 1); // 1 input, 1 output
        } else if (recipeType.contains("stonecutting")) {
            menu.setActiveSlots(1, 1); // 1 input, 1 output
        } else if (recipeType.contains("smithing")) {
            menu.setActiveSlots(3, 1); // Template, base, addition -> output
        } else {
            // Default configuration
            menu.setActiveSlots(3, 2); // 3 inputs, 2 outputs
        }
    }

    private void exportRecipe() {
        String recipeId = recipeIdBox.getValue();
        if (recipeId.isEmpty()) {
            recipeId = "my_recipe";
        }

        String script = KubeJSExporter.generateRecipeScript(
            selectedRecipeType,
            recipeId,
            menu.getInputItems(),
            menu.getOutputItems(),
            menu.getActiveInputSlots(),
            menu.getActiveOutputSlots()
        );

        boolean success = KubeJSExporter.exportToFile(script, recipeId);

        if (success && minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                Component.literal("§aRecipe exported to kubejs/server_scripts/" + recipeId + ".js")
            );
        } else if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                Component.literal("§cFailed to export recipe. Check console for details.")
            );
        }
    }

    private void clearRecipe() {
        // Clear all input and output slots
        for (int i = 0; i < menu.getActiveInputSlots(); i++) {
            menu.getInputItems().setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        for (int i = 0; i < menu.getActiveOutputSlots(); i++) {
            menu.getOutputItems().setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draw main background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (recipeIdBox.isFocused() && recipeIdBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (recipeIdBox.isFocused() && recipeIdBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
