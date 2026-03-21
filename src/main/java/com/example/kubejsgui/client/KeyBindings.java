package com.example.kubejsgui.client;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "key.categories.kubejsgui";
    public static final String OPEN_GUI = "key.kubejsgui.open_gui";

    public static KeyMapping OPEN_GUI_KEY;

    public static void register(RegisterKeyMappingsEvent event) {
        if (OPEN_GUI_KEY == null) {
            OPEN_GUI_KEY = new KeyMapping(
                    OPEN_GUI,
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    CATEGORY
            );
        }
        event.register(OPEN_GUI_KEY);
    }
}
