package com.example.examplemod.client;

import com.example.examplemod.menu.RecipeEditorMenu;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }
}

@Mod.EventBusSubscriber(value = Dist.CLIENT)
class ClientPlayEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (KeyBindings.OPEN_GUI_KEY != null && KeyBindings.OPEN_GUI_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                RecipeEditorMenu menu = new RecipeEditorMenu(0, mc.player.getInventory());
                mc.setScreen(new RecipeEditorScreen(menu, mc.player.getInventory(),
                    Component.literal("Recipe Editor")));
            }
        }
    }
}
