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

    private static final int MAX_INPUT_SLOTS = 81;
    private static final int MAX_OUTPUT_SLOTS = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private final ItemStackHandler inputItems = new ItemStackHandler(MAX_INPUT_SLOTS);
    private final ItemStackHandler outputItems = new ItemStackHandler(MAX_OUTPUT_SLOTS);

    private final Slot[] inputSlots = new Slot[MAX_INPUT_SLOTS];
    private final Slot[] outputSlots = new Slot[MAX_OUTPUT_SLOTS];
    private final Slot[] playerInventorySlots = new Slot[PLAYER_INVENTORY_SLOT_COUNT];
    private final Slot[] hotbarSlots = new Slot[HOTBAR_SLOT_COUNT];

    private int activeInputSlots = 1;  // How many input slots are currently visible
    private int activeOutputSlots = 1; // How many output slots are currently visible

    public RecipeEditorMenu(int id, Inventory playerInventory) {
        super(ModMenuTypes.RECIPE_EDITOR.get(), id);

        // Add input slots (left side)
        for (int i = 0; i < MAX_INPUT_SLOTS; i++) {
            final int slotIndex = i;
            Slot slot = new SlotItemHandler(inputItems, i, 8, 17) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }

                @Override
                public boolean isActive() {
                    return slotIndex < activeInputSlots;
                }
            };
            inputSlots[i] = slot;
            addSlot(slot);
        }

        // Add output slots (right side)
        for (int i = 0; i < MAX_OUTPUT_SLOTS; i++) {
            final int slotIndex = i;
            Slot slot = new SlotItemHandler(outputItems, i, 116, 17) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }

                @Override
                public boolean isActive() {
                    return slotIndex < activeOutputSlots;
                }
            };
            outputSlots[i] = slot;
            addSlot(slot);
        }

        // Add player inventory
        int playerSlot = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot slot = new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18);
                playerInventorySlots[playerSlot++] = slot;
                addSlot(slot);
            }
        }

        // Add player hotbar
        int hotbarSlot = 0;
        for (int col = 0; col < 9; col++) {
            Slot slot = new Slot(playerInventory, col, 8 + col * 18, 142);
            hotbarSlots[hotbarSlot++] = slot;
            addSlot(slot);
        }

        configureLayout(9, 3, 8, 17, 1, 3, 116, 17, 84, 142);
    }

    public ItemStackHandler getInputItems() {
        return inputItems;
    }

    public ItemStackHandler getOutputItems() {
        return outputItems;
    }

    public void configureLayout(int inputs, int inputColumns, int inputOffsetX, int inputOffsetY,
                                int outputs, int outputColumns, int outputOffsetX, int outputOffsetY,
                                int playerInventoryOffsetY, int hotbarOffsetY) {
        this.activeInputSlots = Math.min(inputs, MAX_INPUT_SLOTS);
        this.activeOutputSlots = Math.min(outputs, MAX_OUTPUT_SLOTS);

        int resolvedInputColumns = Math.max(1, inputColumns);
        int resolvedOutputColumns = Math.max(1, outputColumns);

        for (int i = 0; i < inputSlots.length; i++) {
            Slot slot = inputSlots[i];
            int column = i % resolvedInputColumns;
            int row = i / resolvedInputColumns;
            slot.x = inputOffsetX + column * 18;
            slot.y = inputOffsetY + row * 18;
        }

        for (int i = 0; i < outputSlots.length; i++) {
            Slot slot = outputSlots[i];
            int column = i % resolvedOutputColumns;
            int row = i / resolvedOutputColumns;
            slot.x = outputOffsetX + column * 18;
            slot.y = outputOffsetY + row * 18;
        }

        for (int i = 0; i < playerInventorySlots.length; i++) {
            Slot slot = playerInventorySlots[i];
            int column = i % 9;
            int row = i / 9;
            slot.x = 8 + column * 18;
            slot.y = playerInventoryOffsetY + row * 18;
        }

        for (int i = 0; i < hotbarSlots.length; i++) {
            Slot slot = hotbarSlots[i];
            slot.x = 8 + i * 18;
            slot.y = hotbarOffsetY;
        }
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
