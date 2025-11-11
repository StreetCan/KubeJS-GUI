package com.example.examplemod.registry;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.RecipeEditorMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, ExampleMod.MODID);

    public static final RegistryObject<MenuType<RecipeEditorMenu>> RECIPE_EDITOR =
        MENUS.register("recipe_editor",
            () -> IForgeMenuType.create((windowId, inv, data) -> new RecipeEditorMenu(windowId, inv)));
}
