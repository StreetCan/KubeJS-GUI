package com.example.examplemod.client;

import com.example.examplemod.recipe.properties.RecipePropertyLibrary;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyDefinition;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyOption;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyType;
import com.example.examplemod.recipe.properties.RecipePropertyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple configuration screen for assigning recipe property values.
 */
public class RecipePropertiesScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 170;
    private static final int ROW_HEIGHT = 26;

    private final RecipeEditorScreen parent;
    private final RecipePropertyState propertyState;
    private final List<PropertyDefinition> properties;

    private final Map<String, Integer> pendingOptionIndices = new HashMap<>();
    private final List<RowMetadata> rows = new ArrayList<>();

    private EditBox valueEditBox;
    public RecipePropertiesScreen(RecipeEditorScreen parent, RecipePropertyState propertyState) {
        super(Component.literal("Recipe Properties"));
        this.parent = parent;
        this.propertyState = propertyState;
        this.properties = RecipePropertyLibrary.getProperties();
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        this.clearWidgets();

        int startX = (this.width - PANEL_WIDTH) / 2 + 12;
        int startY = (this.height - PANEL_HEIGHT) / 2 + 32;

        for (int i = 0; i < properties.size(); i++) {
            PropertyDefinition definition = properties.get(i);
            int rowY = startY + i * ROW_HEIGHT;
            if (definition.getType() == PropertyType.SELECT) {
                int currentIndex = propertyState.getOptionIndex(definition.getId());
                if (currentIndex < 0 || currentIndex >= definition.getOptions().size()) {
                    currentIndex = 0;
                }
                pendingOptionIndices.put(definition.getId(), currentIndex);

                List<PropertyOption> options = definition.getOptions();
                String label = options.isEmpty() ? "None" : options.get(currentIndex).getDisplayName();

                Button button = Button.builder(
                    Component.literal(label),
                    b -> cycleOption(definition, b)
                ).bounds(startX + 130, rowY - 6, 90, 20).build();
                addRenderableWidget(button);

                rows.add(new RowMetadata(definition, startX, rowY));
            } else if (definition.getType() == PropertyType.VALUE) {
                String currentValue = propertyState.hasValue(definition.getId())
                    ? propertyState.getValue(definition.getId())
                    : definition.getDefaultValue();
                if (currentValue == null) {
                    currentValue = "";
                }
                valueEditBox = new EditBox(this.font, startX + 130, rowY - 6, 90, 20, Component.literal("Value"));
                valueEditBox.setMaxLength(16);
                valueEditBox.setValue(currentValue);
                addRenderableWidget(valueEditBox);

                rows.add(new RowMetadata(definition, startX, rowY));
            }
        }

        int buttonY = startY + properties.size() * ROW_HEIGHT + 10;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveSelections())
            .bounds(startX, buttonY, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
            .bounds(startX + 150, buttonY, 70, 20).build());

        if (valueEditBox != null) {
            setInitialFocus(valueEditBox);
        }
    }

    private void cycleOption(PropertyDefinition definition, Button button) {
        List<PropertyOption> options = definition.getOptions();
        if (options.isEmpty()) {
            return;
        }
        int currentIndex = pendingOptionIndices.getOrDefault(definition.getId(), 0);
        currentIndex = (currentIndex + 1) % options.size();
        pendingOptionIndices.put(definition.getId(), currentIndex);
        button.setMessage(Component.literal(options.get(currentIndex).getDisplayName()));
    }

    private void saveSelections() {
        for (RowMetadata row : rows) {
            PropertyDefinition definition = row.definition();
            if (definition.getType() == PropertyType.SELECT) {
                int index = pendingOptionIndices.getOrDefault(definition.getId(), 0);
                propertyState.setOptionIndex(definition.getId(), index);
            } else if (definition.getType() == PropertyType.VALUE && valueEditBox != null) {
                String sanitized = sanitizeNumeric(valueEditBox.getValue());
                propertyState.setValue(definition.getId(), sanitized);
                valueEditBox.setValue(sanitized);
            }
        }

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§aRecipe properties saved."));
        }
    }

    private String sanitizeNumeric(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        return digitsOnly;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int panelX0 = (this.width - PANEL_WIDTH) / 2;
        int panelY0 = (this.height - PANEL_HEIGHT) / 2;
        int panelX1 = panelX0 + PANEL_WIDTH;
        int panelY1 = panelY0 + PANEL_HEIGHT;

        guiGraphics.fill(panelX0, panelY0, panelX1, panelY1, 0xAA1E1E1E);
        guiGraphics.fill(panelX0, panelY0, panelX1, panelY0 + 1, 0xFF555555);
        guiGraphics.fill(panelX0, panelY1 - 1, panelX1, panelY1, 0xFF000000);
        guiGraphics.fill(panelX0, panelY0, panelX0 + 1, panelY1, 0xFF555555);
        guiGraphics.fill(panelX1 - 1, panelY0, panelX1, panelY1, 0xFF000000);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int titleY = panelY0 + 12;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        for (RowMetadata row : rows) {
            guiGraphics.drawString(this.font, row.definition().getLabel(), row.labelX(), row.labelY(), 0xFFFFFF, false);
        }
    }

    private record RowMetadata(PropertyDefinition definition, int labelX, int labelY) { }
}
