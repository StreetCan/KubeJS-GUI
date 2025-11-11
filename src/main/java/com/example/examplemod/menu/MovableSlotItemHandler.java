package com.example.examplemod.menu;

import java.lang.reflect.Field;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * A {@link SlotItemHandler} that can adjust its on-screen position after construction.
 */
class MovableSlotItemHandler extends SlotItemHandler {
    private static final Field X_FIELD;
    private static final Field Y_FIELD;

    static {
        try {
            X_FIELD = Slot.class.getDeclaredField("x");
            Y_FIELD = Slot.class.getDeclaredField("y");
            X_FIELD.setAccessible(true);
            Y_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Failed to access Slot position fields", e);
        }
    }

    MovableSlotItemHandler(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    /**
     * Updates the rendered position of this slot.
     */
    void setPosition(int xPosition, int yPosition) {
        try {
            X_FIELD.setInt(this, xPosition);
            Y_FIELD.setInt(this, yPosition);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to reposition slot", e);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }
}
