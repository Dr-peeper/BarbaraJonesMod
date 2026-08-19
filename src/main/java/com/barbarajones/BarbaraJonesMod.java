package com.barbarajones;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModFeatures;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.content.ModTabs;
import com.barbarajones.net.ModNetwork;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BarbaraJonesMod.MODID)
public class BarbaraJonesMod {

    public static final String MODID = "barbarajones";

    public BarbaraJonesMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(bus);
        ModFluids.FLUID_TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        com.barbarajones.content.extra.ExtraRegistry.ENTITIES.register(bus);
        ModFeatures.FEATURES.register(bus);
        ModTabs.TABS.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
