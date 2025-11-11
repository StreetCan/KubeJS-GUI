package com.example.examplemod.client;

import com.example.examplemod.menu.RecipeEditorMenu;
import com.example.examplemod.recipe.RecipeTypeRegistry;
import com.example.examplemod.util.KubeJSExporter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Main screen for the recipe editor GUI.
 * Allows users to visually create recipes and export them as KubeJS scripts.
 */
public class RecipeEditorScreen extends AbstractContainerScreen<RecipeEditorMenu> {
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_BORDER_COLOR = 0xFF000000;
    private static final int PANEL_BACKGROUND_COLOR = 0xFF2D2D2D;
    private static final int SLOT_BACKGROUND_COLOR = 0xFF3F3F3F;
    private static final int SLOT_HIGHLIGHT_COLOR = 0xFF555555;
    private static final int SLOT_SHADOW_COLOR = 0xFF1B1B1B;

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
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;

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
        int x = this.leftPos;
        int y = this.topPos;

        // Main panel background
        guiGraphics.fill(x - 4, y - 4, x + imageWidth + 4, y + imageHeight + 4, PANEL_BORDER_COLOR);
        guiGraphics.fill(x - 3, y - 3, x + imageWidth + 3, y + imageHeight + 3, PANEL_BACKGROUND_COLOR);

        // Recipe area panel
        guiGraphics.fill(x + 3, y + 3, x + imageWidth - 3, y + 78, PANEL_BORDER_COLOR);
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + 77, PANEL_BACKGROUND_COLOR);

        // Player inventory panel
        guiGraphics.fill(x + 3, y + 79, x + imageWidth - 3, y + imageHeight - 3, PANEL_BORDER_COLOR);
        guiGraphics.fill(x + 4, y + 80, x + imageWidth - 4, y + imageHeight - 4, PANEL_BACKGROUND_COLOR);

        // Draw input grid (3x3)
        drawSlotGrid(guiGraphics, x + 8, y + 17, 3, 3);

        // Draw output grid (2x2)
        drawSlotGrid(guiGraphics, x + 116, y + 26, 2, 2);

        // Draw player inventory grid (3 rows)
        drawSlotGrid(guiGraphics, x + 8, y + 84, 9, 3);

        // Draw player hotbar
        drawSlotGrid(guiGraphics, x + 8, y + 142, 9, 1);
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

    private void drawSlotGrid(GuiGraphics guiGraphics, int startX, int startY, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotX = startX + col * SLOT_SIZE;
                int slotY = startY + row * SLOT_SIZE;
                drawSlot(guiGraphics, slotX, slotY);
            }
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, PANEL_BORDER_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_BACKGROUND_COLOR);

        // Top and left highlight
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + 2, SLOT_HIGHLIGHT_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + SLOT_SIZE - 1, SLOT_HIGHLIGHT_COLOR);

        // Bottom and right shadow
        guiGraphics.fill(x + 1, y + SLOT_SIZE - 2, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_SHADOW_COLOR);
        guiGraphics.fill(x + SLOT_SIZE - 2, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, SLOT_SHADOW_COLOR);
    }
}
