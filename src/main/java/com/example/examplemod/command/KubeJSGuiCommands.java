package com.example.examplemod.command;

import com.example.examplemod.menu.RecipeEditorOpener;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class KubeJSGuiCommands {

    private KubeJSGuiCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("kubejsgui")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("open")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        RecipeEditorOpener.open(player);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
    }
}
