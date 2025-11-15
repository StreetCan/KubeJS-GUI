package com.example.examplemod.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class RecipeEditorOpener {

    private static final Component TITLE = Component.literal("Recipe Editor");

    private RecipeEditorOpener() {
    }

    public static void open(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (windowId, inv, serverPlayer) -> new RecipeEditorMenu(windowId, inv),
            TITLE
        ));
    }
}
