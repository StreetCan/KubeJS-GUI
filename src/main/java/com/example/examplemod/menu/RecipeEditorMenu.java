package com.example.examplemod.menu;

import com.example.examplemod.registry.ModMenuTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Container menu for the recipe editor GUI.
 * Handles flexible input/output slots that adjust based on recipe type.
 */
public class RecipeEditorMenu extends AbstractContainerMenu {

    // Maximum slots we might need (e.g., 3x3 crafting = 9 inputs, some recipes have multiple outputs)
    private static final int MAX_INPUT_SLOTS = 9;
    private static final int MAX_OUTPUT_SLOTS = 4;

    private final ItemStackHandler inputItems = new ItemStackHandler(MAX_INPUT_SLOTS);
    private final ItemStackHandler outputItems = new ItemStackHandler(MAX_OUTPUT_SLOTS);

    private int activeInputSlots = 1;  // How many input slots are currently visible
    private int activeOutputSlots = 1; // How many output slots are currently visible

    public RecipeEditorMenu(int id, Inventory playerInventory) {
        super(ModMenuTypes.RECIPE_EDITOR.get(), id);

        // Add input slots (left side)
        for (int i = 0; i < MAX_INPUT_SLOTS; i++) {
            final int slotIndex = i;
            addSlot(new SlotItemHandler(inputItems, i, 8 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }

                @Override
                public boolean isActive() {
                    return slotIndex < activeInputSlots;
                }
            });
        }

        // Add output slots (right side)
        for (int i = 0; i < MAX_OUTPUT_SLOTS; i++) {
            final int slotIndex = i;
            addSlot(new SlotItemHandler(outputItems, i, 116 + (i % 2) * 18, 26 + (i / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }

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
        this.activeInputSlots = Math.min(inputs, MAX_INPUT_SLOTS);
        this.activeOutputSlots = Math.min(outputs, MAX_OUTPUT_SLOTS);
    }

    public int getActiveInputSlots() {
        return activeInputSlots;
    }

    public int getActiveOutputSlots() {
        return activeOutputSlots;
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
