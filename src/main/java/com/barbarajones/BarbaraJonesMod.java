package com.barbarajones;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.content.ModTabs;
import com.barbarajones.net.ModNetwork;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BarbaraJonesMod.MODID)
public class BarbaraJonesMod {

    public static final String MODID = "barbarajones";

    public BarbaraJonesMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModTabs.TABS.register(bus);

        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
