package com.example.examplemod.menu;

import com.example.examplemod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Container menu for the recipe editor GUI.
 * Handles flexible input/output slots that adjust based on recipe type.
 */
public class RecipeEditorMenu extends AbstractContainerMenu {

    // Maximum slots we might need (e.g., 3x3 crafting = 9 inputs, some recipes have multiple outputs)
    private static final int MAX_INPUT_SLOTS = 9;
    private static final int MAX_OUTPUT_SLOTS = 4;

    private static final int SLOT_SPACING = 18;
    private static final int INPUT_START_X = 8;
    private static final int INPUT_START_Y = 17;
    private static final int OUTPUT_START_X = 116;
    private static final int OUTPUT_START_Y = 26;

    private final ItemStackHandler inputItems = new ItemStackHandler(MAX_INPUT_SLOTS);
    private final ItemStackHandler outputItems = new ItemStackHandler(MAX_OUTPUT_SLOTS);

    private int activeInputSlots = 1;  // How many input slots are currently visible
    private int activeOutputSlots = 1; // How many output slots are currently visible
    private int inputColumns = 1;
    private int outputColumns = 1;

    public RecipeEditorMenu(int id, Inventory playerInventory) {
        super(ModMenuTypes.RECIPE_EDITOR.get(), id);

        // Add input slots (left side)
        for (int i = 0; i < MAX_INPUT_SLOTS; i++) {
            final int slotIndex = i;
            addSlot(new MovableSlotItemHandler(inputItems, i,
                INPUT_START_X + (i % 3) * SLOT_SPACING,
                INPUT_START_Y + (i / 3) * SLOT_SPACING) {
                @Override
                public boolean isActive() {
                    return slotIndex < activeInputSlots;
                }
            });
        }

        // Add output slots (right side)
        for (int i = 0; i < MAX_OUTPUT_SLOTS; i++) {
            final int slotIndex = i;
            addSlot(new MovableSlotItemHandler(outputItems, i,
                OUTPUT_START_X + (i % 2) * SLOT_SPACING,
                OUTPUT_START_Y + (i / 2) * SLOT_SPACING) {
                @Override
                public boolean isActive() {
                    return slotIndex < activeOutputSlots;
                }
            });
        }

        // Add player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public ItemStackHandler getInputItems() {
        return inputItems;
    }

    public ItemStackHandler getOutputItems() {
        return outputItems;
    }

    public void setActiveSlots(int inputs, int outputs) {
        setActiveSlots(inputs, outputs, 3, 2);
    }

    public void setActiveSlots(int inputs, int outputs, int requestedInputColumns, int requestedOutputColumns) {
        this.activeInputSlots = Math.min(Math.max(inputs, 0), MAX_INPUT_SLOTS);
        this.activeOutputSlots = Math.min(Math.max(outputs, 0), MAX_OUTPUT_SLOTS);

        this.inputColumns = Math.max(1, Math.min(requestedInputColumns, MAX_INPUT_SLOTS));
        this.outputColumns = Math.max(1, Math.min(requestedOutputColumns, MAX_OUTPUT_SLOTS));

        updateInputSlotPositions();
        updateOutputSlotPositions();
    }

    public int getActiveInputSlots() {
        return activeInputSlots;
    }

    public int getActiveOutputSlots() {
        return activeOutputSlots;
    }

    private void updateInputSlotPositions() {
        int totalSlots = Math.min(MAX_INPUT_SLOTS, this.slots.size());
        for (int i = 0; i < totalSlots; i++) {
            Slot slot = this.slots.get(i);
            if (slot instanceof MovableSlotItemHandler movableSlot) {
                int column = i % inputColumns;
                int row = i / inputColumns;

                movableSlot.setPosition(INPUT_START_X + column * SLOT_SPACING, INPUT_START_Y + row * SLOT_SPACING);
            }
        }
    }

    private void updateOutputSlotPositions() {
        int baseIndex = MAX_INPUT_SLOTS;
        for (int i = 0; i < MAX_OUTPUT_SLOTS; i++) {
            int slotListIndex = baseIndex + i;
            if (slotListIndex >= this.slots.size()) {
                break;
            }

            Slot slot = this.slots.get(slotListIndex);
            if (slot instanceof MovableSlotItemHandler movableSlot) {
                int column = i % outputColumns;
                int row = i / outputColumns;

                movableSlot.setPosition(OUTPUT_START_X + column * SLOT_SPACING, OUTPUT_START_Y + row * SLOT_SPACING);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            int containerSlotEnd = MAX_INPUT_SLOTS + MAX_OUTPUT_SLOTS;

            if (index < containerSlotEnd) {
                // Moving from container to player inventory
                if (!this.moveItemStackTo(stack, containerSlotEnd, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to container (inputs only)
                if (activeInputSlots == 0 || !this.moveItemStackTo(stack, 0, activeInputSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
