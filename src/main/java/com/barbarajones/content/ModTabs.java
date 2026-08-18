package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** One creative tab holding every item in the mod. */
public final class ModTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BarbaraJonesMod.MODID);

    private ModTabs() { }

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Barbara Jones"))
                    .icon(() -> new ItemStack(ModItems.HANDFUL_OF_GRASS.get()))
                    .displayItems((params, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());
}
