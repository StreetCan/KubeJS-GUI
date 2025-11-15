package com.example.examplemod.client;

import com.example.examplemod.recipe.properties.RecipePropertyLibrary;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyDefinition;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyOption;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.PropertyType;
import com.example.examplemod.recipe.properties.RecipePropertyLibrary.SlotDomain;
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
 * Configuration screen for assigning recipe property values. Properties are scoped per
 * recipe type and can optionally target individual slots.
 */
public class RecipePropertiesScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 190;
    private static final int ROW_HEIGHT = 28;
    private static final int LABEL_COLUMN_WIDTH = 100;
    private static final int SLOT_BUTTON_WIDTH = 60;
    private static final int OPTION_BUTTON_WIDTH = 80;
    private static final int VALUE_BOX_WIDTH = 80;
    private static final int CONTROL_SPACING = 4;

    private final RecipeEditorScreen parent;
    private final RecipePropertyState propertyState;
    private final String recipeTypeId;
    private final int activeInputSlots;
    private final int activeOutputSlots;
    private final List<PropertyDefinition> properties;

    private final List<RowMetadata> rows = new ArrayList<>();
    private final Map<String, Integer> pendingOptionIndices = new HashMap<>();
    private final Map<String, Integer> pendingSlotIndices = new HashMap<>();

    public RecipePropertiesScreen(RecipeEditorScreen parent, RecipePropertyState propertyState,
                                  String recipeTypeId, int activeInputSlots, int activeOutputSlots) {
        super(Component.literal("Recipe Properties"));
        this.parent = parent;
        this.propertyState = propertyState;
        this.recipeTypeId = recipeTypeId;
        this.activeInputSlots = activeInputSlots;
        this.activeOutputSlots = activeOutputSlots;
        this.properties = RecipePropertyLibrary.getPropertiesFor(recipeTypeId);
    }

    @Override
    protected void init() {
        super.init();
        rows.clear();
        this.clearWidgets();
        pendingOptionIndices.clear();
        pendingSlotIndices.clear();

        int panelX0 = (this.width - PANEL_WIDTH) / 2;
        int startX = panelX0 + 12;
        int startY = (this.height - PANEL_HEIGHT) / 2 + 38;

        int rowIndex = 0;

        for (PropertyDefinition definition : properties) {
            int slotCount = getSlotCount(definition.getSlotDomain());
            if (definition.requiresSlotSelection() && slotCount <= 0) {
                continue; // skip properties that cannot function without slots
            }

            int rowY = startY + rowIndex * ROW_HEIGHT;
            RowMetadata row = new RowMetadata(definition, startX, rowY);

            int controlX = startX + LABEL_COLUMN_WIDTH;

            if (definition.supportsSlotSelection() && slotCount > 0) {
                int selectedSlot = propertyState.getSlotIndex(definition.getId());
                if (selectedSlot < 0 || selectedSlot >= slotCount) {
                    selectedSlot = 0;
                }
                pendingSlotIndices.put(definition.getId(), selectedSlot);

                Button slotButton = Button.builder(
                        Component.literal(formatSlotLabel(definition.getSlotDomain(), selectedSlot)),
                        button -> cycleSlot(definition, row, button)
                    )
                    .bounds(controlX, rowY - 6, SLOT_BUTTON_WIDTH, 20)
                    .build();
                row.slotButton = slotButton;
                addRenderableWidget(slotButton);

                controlX += SLOT_BUTTON_WIDTH + CONTROL_SPACING;
            }

            if (definition.getType() == PropertyType.SELECT) {
                List<PropertyOption> options = definition.getOptions();
                if (options.isEmpty()) {
                    continue;
                }
                int currentIndex = propertyState.getOptionIndex(definition.getId());
                if (currentIndex < 0 || currentIndex >= options.size()) {
                    currentIndex = 0;
                }
                pendingOptionIndices.put(definition.getId(), currentIndex);

                String label = options.get(currentIndex).getDisplayName();
                Button optionButton = Button.builder(
                        Component.literal(label),
                        button -> cycleOption(definition, button)
                    )
                    .bounds(controlX, rowY - 6, OPTION_BUTTON_WIDTH, 20)
                    .build();
                row.optionButton = optionButton;
                addRenderableWidget(optionButton);
            } else if (definition.getType() == PropertyType.VALUE) {
                String currentValue;
                if (definition.getSlotDomain() == SlotDomain.NONE) {
                    currentValue = propertyState.hasValue(definition.getId())
                        ? propertyState.getValue(definition.getId())
                        : definition.getDefaultValue();
                } else {
                    int selectedSlot = pendingSlotIndices.getOrDefault(definition.getId(), 0);
                    String stored = propertyState.getSlotValue(definition.getId(), selectedSlot);
                    currentValue = stored != null ? stored : definition.getDefaultValue();
                }
                if (currentValue == null) {
                    currentValue = "";
                }

                EditBox valueBox = new EditBox(this.font, controlX, rowY - 6, VALUE_BOX_WIDTH, 20, Component.literal(definition.getLabel()));
                valueBox.setMaxLength(32);
                valueBox.setValue(currentValue);
                if (!definition.getValueHint().isBlank()) {
                    valueBox.setHint(Component.literal(definition.getValueHint()));
                }
                row.valueBox = valueBox;
                addRenderableWidget(valueBox);
            }

            rows.add(row);
            rowIndex++;
        }

        int buttonY = startY + Math.max(rowIndex, 1) * ROW_HEIGHT + 14;

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveSelections())
            .bounds(startX, buttonY, 80, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
            .bounds(startX + 160, buttonY, 80, 20)
            .build());

        if (!rows.isEmpty()) {
            RowMetadata firstRow = rows.get(0);
            if (firstRow.valueBox != null) {
                setInitialFocus(firstRow.valueBox);
            } else if (firstRow.optionButton != null) {
                setInitialFocus(firstRow.optionButton);
            }
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

    private void cycleSlot(PropertyDefinition definition, RowMetadata row, Button button) {
        int slotCount = getSlotCount(definition.getSlotDomain());
        if (slotCount <= 0) {
            return;
        }

        int currentIndex = pendingSlotIndices.getOrDefault(definition.getId(), 0);
        currentIndex = (currentIndex + 1) % slotCount;
        pendingSlotIndices.put(definition.getId(), currentIndex);
        button.setMessage(Component.literal(formatSlotLabel(definition.getSlotDomain(), currentIndex)));

        if (row.valueBox != null && definition.getSlotDomain() != SlotDomain.NONE) {
            String stored = propertyState.getSlotValue(definition.getId(), currentIndex);
            if (stored == null || stored.isBlank()) {
                stored = definition.getDefaultValue();
            }
            row.valueBox.setValue(stored == null ? "" : stored);
        }
    }

    private void saveSelections() {
        for (RowMetadata row : rows) {
            PropertyDefinition definition = row.definition;

            if (definition.getType() == PropertyType.SELECT && row.optionButton != null) {
                int index = pendingOptionIndices.getOrDefault(definition.getId(), 0);
                propertyState.setOptionIndex(definition.getId(), index);

                if (definition.supportsSlotSelection()) {
                    int slot = pendingSlotIndices.getOrDefault(definition.getId(), -1);
                    propertyState.setSlotIndex(definition.getId(), slot);
                }
            } else if (definition.getType() == PropertyType.VALUE && row.valueBox != null) {
                int slot = pendingSlotIndices.getOrDefault(definition.getId(), -1);
                String sanitized = sanitizeValue(definition, row.valueBox.getValue());

                if (definition.getSlotDomain() == SlotDomain.NONE) {
                    propertyState.setValue(definition.getId(), sanitized);
                    row.valueBox.setValue(sanitized);
                } else {
                    propertyState.setSlotIndex(definition.getId(), slot);
                    propertyState.setSlotValue(definition.getId(), slot, sanitized);
                    row.valueBox.setValue(sanitized);
                }
            }
        }

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("§aRecipe properties saved."));
        }
    }

    private String sanitizeValue(PropertyDefinition definition, String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!definition.isNumericValue()) {
            return trimmed;
        }
        return trimmed.replaceAll("[^0-9]", "");
    }

    private int getSlotCount(SlotDomain domain) {
        return switch (domain) {
            case INPUT -> activeInputSlots;
            case OUTPUT -> activeOutputSlots;
            default -> 0;
        };
    }

    private String formatSlotLabel(SlotDomain domain, int slotIndex) {
        return switch (domain) {
            case INPUT -> "Input " + (slotIndex + 1);
            case OUTPUT -> "Output " + (slotIndex + 1);
            default -> "Slot";
        };
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

        int titleY = panelY0 + 14;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        if (rows.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("No properties available"), this.width / 2, titleY + 24, 0xBBBBBB);
        } else {
            for (RowMetadata row : rows) {
                guiGraphics.drawString(this.font, row.definition.getLabel(), row.labelX, row.labelY, 0xFFFFFF, false);
            }
        }
    }

    private static final class RowMetadata {
        final PropertyDefinition definition;
        final int labelX;
        final int labelY;
        Button optionButton;
        EditBox valueBox;
        Button slotButton;

        RowMetadata(PropertyDefinition definition, int labelX, int labelY) {
            this.definition = definition;
            this.labelX = labelX;
            this.labelY = labelY;
        }
    }
}
