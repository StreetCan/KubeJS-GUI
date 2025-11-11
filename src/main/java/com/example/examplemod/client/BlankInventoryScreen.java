package com.example.examplemod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;

/**
 * A minimal screen that shows the player's inventory (via InventoryScreen)
 * and renders a blank translucent overlay panel as a placeholder GUI.
 */
public class BlankInventoryScreen extends InventoryScreen {

    public BlankInventoryScreen(Player player) {
        super(player);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the vanilla inventory first
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Draw a simple translucent rectangle as the "blank GUI"
        // Centered on screen with fixed size
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int panelW = 120;
        int panelH = 80;
        int x0 = (width - panelW) / 2;
        int y0 = (height - panelH) / 2;
        int x1 = x0 + panelW;
        int y1 = y0 + panelH;

        // Background rectangle (black with 60% opacity)
        int bgColor = 0x99000000; // ARGB
        guiGraphics.fill(x0, y0, x1, y1, bgColor);

        // Optional border (white, mostly opaque)
        int border = 0xDDEEEEEE;
        guiGraphics.fill(x0, y0, x1, y0 + 1, border);
        guiGraphics.fill(x0, y1 - 1, x1, y1, border);
        guiGraphics.fill(x0, y0, x0 + 1, y1, border);
        guiGraphics.fill(x1 - 1, y0, x1, y1, border);
    }
}
