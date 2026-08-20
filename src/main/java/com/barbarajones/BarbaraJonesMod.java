package com.barbarajones;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModFeatures;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.content.ModTabs;
import com.barbarajones.menu.ModMenus;
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
        ModMenus.MENUS.register(bus);

        // ---- Krave 2.0 modules -------------------------------------------
        // Each module owns its own DeferredRegisters and exposes exactly one
        // init(bus). That is what let a dozen agents build these in parallel
        // without twelve-way conflicts in this file.
        com.barbarajones.v2.mobs.Mobs.init(bus);
        com.barbarajones.v2.economy.KraveEconomy.init(bus);
        com.barbarajones.v2.build.KraveBuild.init(bus);
        com.barbarajones.v2.houses.KraveHouses.init(bus);
        com.barbarajones.v2.village.KraveVillage.init(bus);
        com.barbarajones.v2.quests.QuestModule.init(bus);
        com.barbarajones.v2.internet.InternetContent.init(bus);
        com.barbarajones.v2.machines.KraveMachines.init(bus);
        com.barbarajones.v2.abilities.PlayerAbilities.init(bus);
        com.barbarajones.v2.bonds.BondsRegistry.init(bus);
        com.barbarajones.v2.manual.ManualModule.init(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
