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

    private static final int INPUT_SLOT_COUNT = 81;
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
    private LayoutConfig currentLayout;

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
        String recipeType = selectedRecipeType.toLowerCase();

        LayoutConfig layout;
        if (recipeType.contains("mechanical_crafting")) {
            layout = LayoutConfig.mechanical();
        } else if (recipeType.contains("crafting_shaped") || recipeType.contains("crafting_shapeless")
            || recipeType.equals("minecraft:crafting") || recipeType.endsWith(":crafting")) {
            layout = LayoutConfig.standardCrafting();
        } else if (recipeType.contains("shaped") || recipeType.contains("shapeless")) {
            layout = LayoutConfig.standardCrafting();
        } else if (recipeType.contains("smelting") || recipeType.contains("blasting") ||
                   recipeType.contains("smoking") || recipeType.contains("campfire")) {
            layout = LayoutConfig.singleInputCooker();
        } else if (recipeType.contains("stonecutting")) {
            layout = LayoutConfig.singleInputCooker();
        } else if (recipeType.contains("smithing")) {
            layout = LayoutConfig.smithing();
        } else {
            layout = LayoutConfig.genericProcessing();
        }

        applyLayout(layout);
    }

    private void applyLayout(LayoutConfig layout) {
        this.currentLayout = layout;
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
        this.inventoryLabelY = layout.inventoryLabelY;

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        menu.configureLayout(
            layout.activeInputSlots,
            layout.inputColumns,
            layout.inputOffsetX,
            layout.inputOffsetY,
            layout.activeOutputSlots,
            layout.outputColumns,
            layout.outputOffsetX,
            layout.outputOffsetY,
            layout.playerInventoryY,
            layout.hotbarY
        );

        updateLayout();
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
            this.minecraft.setScreen(new RecipePropertiesScreen(
                this,
                propertyState,
                selectedRecipeType,
                menu.getActiveInputSlots(),
                menu.getActiveOutputSlots()
            ));
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
        int recipeBottom = currentLayout != null ? currentLayout.recipePanelBottom : 78;
        guiGraphics.fill(x + 3, y + 3, x + imageWidth - 3, y + recipeBottom, PANEL_BORDER_COLOR);
        guiGraphics.fill(x + 4, y + 4, x + imageWidth - 4, y + recipeBottom - 1, PANEL_BACKGROUND_COLOR);

        // Player inventory panel
        int inventoryTop = currentLayout != null ? currentLayout.inventoryPanelTop : 79;
        guiGraphics.fill(x + 3, y + inventoryTop, x + imageWidth - 3, y + imageHeight - 3, PANEL_BORDER_COLOR);
        guiGraphics.fill(x + 4, y + inventoryTop + 1, x + imageWidth - 4, y + imageHeight - 4, PANEL_BACKGROUND_COLOR);

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

    private static LayoutConfig createLayout(int inputs, int inputColumns, int inputOffsetX, int inputOffsetY,
                                             int outputs, int outputColumns, int outputOffsetX, int outputOffsetY,
                                             int playerInventoryY, int hotbarY, int imageWidth, int imageHeight) {
        return new LayoutConfig(
            inputs,
            inputColumns,
            inputOffsetX,
            inputOffsetY,
            outputs,
            outputColumns,
            outputOffsetX,
            outputOffsetY,
            playerInventoryY,
            hotbarY,
            imageWidth,
            imageHeight
        );
    }

    private static final class LayoutConfig {
        final int activeInputSlots;
        final int inputColumns;
        final int inputOffsetX;
        final int inputOffsetY;
        final int activeOutputSlots;
        final int outputColumns;
        final int outputOffsetX;
        final int outputOffsetY;
        final int playerInventoryY;
        final int hotbarY;
        final int imageWidth;
        final int imageHeight;
        final int recipePanelBottom;
        final int inventoryPanelTop;
        final int inventoryLabelY;

        private LayoutConfig(int activeInputSlots, int inputColumns, int inputOffsetX, int inputOffsetY,
                              int activeOutputSlots, int outputColumns, int outputOffsetX, int outputOffsetY,
                              int playerInventoryY, int hotbarY, int imageWidth, int imageHeight) {
            this.activeInputSlots = activeInputSlots;
            this.inputColumns = Math.max(1, inputColumns);
            this.inputOffsetX = inputOffsetX;
            this.inputOffsetY = inputOffsetY;
            this.activeOutputSlots = activeOutputSlots;
            this.outputColumns = Math.max(1, outputColumns);
            this.outputOffsetX = outputOffsetX;
            this.outputOffsetY = outputOffsetY;
            this.playerInventoryY = playerInventoryY;
            this.hotbarY = hotbarY;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;

            int rows = activeInputSlots <= 0 ? 1 : (int) Math.ceil((double) activeInputSlots / this.inputColumns);
            int slotBottom = inputOffsetY + rows * SLOT_SIZE;
            int inferredRecipeBottom = playerInventoryY - 6;
            this.recipePanelBottom = Math.max(slotBottom + 6, inferredRecipeBottom);
            this.inventoryPanelTop = playerInventoryY - 5;
            this.inventoryLabelY = playerInventoryY - 12;
        }

        static LayoutConfig standardCrafting() {
            return createLayout(9, 3, 8, 17, 1, 3, 116, 17, 84, 142, 176, 166);
        }

        static LayoutConfig genericProcessing() {
            return createLayout(9, 3, 8, 17, 9, 3, 116, 17, 84, 142, 176, 166);
        }

        static LayoutConfig singleInputCooker() {
            return createLayout(1, 1, 44, 35, 1, 1, 116, 35, 84, 142, 176, 166);
        }

        static LayoutConfig smithing() {
            return createLayout(3, 3, 26, 35, 1, 1, 134, 53, 84, 142, 176, 166);
        }

        static LayoutConfig mechanical() {
            int inputOffsetX = 8;
            int inputOffsetY = 17;
            int inputColumns = 9;
            int activeInputs = 81;
            int inputRows = 9;
            int outputOffsetX = inputOffsetX + inputColumns * SLOT_SIZE + 24;
            int outputOffsetY = inputOffsetY + (inputRows * SLOT_SIZE) / 2 - SLOT_SIZE / 2;
            int playerInventoryY = inputOffsetY + inputRows * SLOT_SIZE + 36;
            int hotbarY = playerInventoryY + 58;
            int imageWidth = outputOffsetX + SLOT_SIZE + 8;
            int imageHeight = hotbarY + 24;
            return createLayout(activeInputs, inputColumns, inputOffsetX, inputOffsetY,
                1, 1, outputOffsetX, outputOffsetY, playerInventoryY, hotbarY, imageWidth, imageHeight);
        }
    }
}
