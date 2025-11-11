package com.example.examplemod.network;

import com.example.examplemod.menu.RecipeEditorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record OpenRecipeEditorPacket() {

    public static void encode(OpenRecipeEditorPacket packet, FriendlyByteBuf buffer) {
        // No payload needed
    }

    public static OpenRecipeEditorPacket decode(FriendlyByteBuf buffer) {
        return new OpenRecipeEditorPacket();
    }

    public static void handle(OpenRecipeEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (windowId, inv, serverPlayer) -> new RecipeEditorMenu(windowId, inv),
                    Component.literal("Recipe Editor")
                ));
            }
        });
        context.setPacketHandled(true);
    }
}
