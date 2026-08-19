package com.barbarajones.menu;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BarbaraJonesMod.MODID);

    private ModMenus() { }

    public static final RegistryObject<MenuType<KraftingBenchMenu>> KRAFTING_BENCH =
            MENUS.register("krafting_bench", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new KraftingBenchMenu(id, inv)));
}
