package com.example.examplemod.client;

import com.example.examplemod.menu.RecipeEditorMenu;
import com.example.examplemod.recipe.RecipeTypeRegistry;
import com.example.examplemod.recipe.properties.RecipePropertyState;
import com.example.examplemod.util.KubeJSExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

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

    private static final int INPUT_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private EditBox recipeIdBox;
    private Button modFilterButton;
    private Button recipeTypeButton;
    private Button exportButton;
    private Button clearButton;
    private Button propertiesButton;

    private String selectedMod = "minecraft";
    private String selectedRecipeType = "minecraft:crafting_shaped";
    private List<String> availableMods;
    private List<RecipeTypeRegistry.RecipeTypeInfo> availableRecipeTypes;

    private int modFilterIndex = 0;
    private int recipeTypeIndex = 0;

    private final RecipePropertyState propertyState = new RecipePropertyState();

    public RecipeEditorScreen(RecipeEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
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

        // Properties button (left side)
        this.propertiesButton = Button.builder(
            Component.literal("Properties"),
            button -> openProperties()
        ).bounds(buttonX, buttonY + 66, 85, 20).build();
        this.addRenderableWidget(propertiesButton);

        // Export button (left side)
        this.exportButton = Button.builder(
            Component.literal("Export"),
            button -> exportRecipe()
        ).bounds(buttonX, buttonY + 88, 85, 20).build();
        this.addRenderableWidget(exportButton);

        // Clear button (left side)
        this.clearButton = Button.builder(
            Component.literal("Clear"),
            button -> clearRecipe()
        ).bounds(buttonX, buttonY + 110, 85, 20).build();
        this.addRenderableWidget(clearButton);

        updateSlotConfiguration();
        updateLayout();
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

        if (recipeType.contains("crafting_shaped") || recipeType.contains("crafting_shapeless")
            || recipeType.equals("minecraft:crafting") || recipeType.contains(":crafting")) {
            // Vanilla crafting recipes all report the generic "minecraft:crafting" type, so make sure
            // we always expose the full 3x3 input grid and the 2x2 output grid for them.
            menu.setActiveSlots(9, 1);
        } else if (recipeType.contains("shaped") || recipeType.contains("shapeless")) {
            // Fallback for any custom shaped/shapeless identifiers that still deserve the full grid.
            menu.setActiveSlots(9, 1);
        } else if (recipeType.contains("smelting") || recipeType.contains("blasting") ||
                   recipeType.contains("smoking") || recipeType.contains("campfire")) {
            menu.setActiveSlots(1, 1); // 1 input, 1 output
        } else if (recipeType.contains("stonecutting")) {
            menu.setActiveSlots(1, 1); // 1 input, 1 output
        } else if (recipeType.contains("smithing")) {
            menu.setActiveSlots(3, 1); // Template, base, addition -> output
        } else {
            // Default configuration
            menu.setActiveSlots(9, 9); // 3 inputs, 2 outputs
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
            menu.getActiveOutputSlots(),
            propertyState
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

        propertyState.clear();
    }

    private void updateLayout() {
        final int margin = 6;
        final int controlWidth = 85;
        final int rowGap = 22;

        // Prefer left side of the container; if that would go off-screen, place to the right.
        int leftSideX = this.leftPos - (controlWidth + margin);
        int rightSideX = this.leftPos + this.imageWidth + margin;

        int buttonX = leftSideX >= margin ? leftSideX : rightSideX;

        // Keep inside the screen horizontally
        buttonX = Math.max(margin, Math.min(buttonX, this.width - controlWidth - margin));

        int buttonY = this.topPos + 20;

        // EditBox
        recipeIdBox.setX(buttonX);
        recipeIdBox.setY(buttonY);

        // Buttons stacked vertically
        modFilterButton.setPosition(buttonX, buttonY + rowGap);
        recipeTypeButton.setPosition(buttonX, buttonY + rowGap * 2);
        propertiesButton.setPosition(buttonX, buttonY + rowGap * 3);
        exportButton.setPosition(buttonX, buttonY + rowGap * 4);
        clearButton.setPosition(buttonX, buttonY + rowGap * 5);
    }

    private void openProperties() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new RecipePropertiesScreen(this, propertyState));
        }
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h); // re-runs init()
        // Defensive: if widgets persist across versions, ensure a correct layout after resize.
        if (recipeIdBox != null) {
            updateLayout();
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

        // Draw input slots
        drawSlotGroup(guiGraphics, 0, menu.getActiveInputSlots());

        // Draw output slots
        drawSlotGroup(guiGraphics, INPUT_SLOT_COUNT, menu.getActiveOutputSlots());

        // Draw player inventory slots
        drawSlotGroup(guiGraphics, INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT, PLAYER_INVENTORY_SLOT_COUNT);

        // Draw player hotbar slots
        drawSlotGroup(guiGraphics, INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT, HOTBAR_SLOT_COUNT);
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

    private void drawSlotGroup(GuiGraphics guiGraphics, int startIndex, int slotCount) {
        int maxIndex = Math.min(startIndex + slotCount, this.menu.slots.size());
        for (int slotIndex = startIndex; slotIndex < maxIndex; slotIndex++) {
            Slot slot = this.menu.slots.get(slotIndex);
            int slotX = this.leftPos + slot.x - 1;
            int slotY = this.topPos + slot.y - 1;
            drawSlot(guiGraphics, slotX, slotY);
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
