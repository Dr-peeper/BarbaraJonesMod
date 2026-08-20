package com.barbarajones.v2.machines;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModTabs;
import com.barbarajones.v2.machines.recipe.RecipeLookup;
import com.barbarajones.v2.machines.village.KraveVillageBridge;
import com.barbarajones.v2.machines.village.VillageLink;

/**
 * The module's own event subscribers.
 *
 * <p>All three are discovered by Forge's annotation scan, so none of them needs a
 * line in {@code BarbaraJonesMod} or {@code EventHandler}. The only central
 * wiring this whole module asks for is {@code KraveMachines.init(bus)}.
 */
public final class MachinesEvents {

    private MachinesEvents() { }

    /**
     * Puts the machines and their intermediates in the mod's creative tab.
     *
     * <p>{@code ModTabs.MAIN} builds its contents from {@code ModItems.ITEMS}
     * only. This module owns a separate {@code DeferredRegister}, so without this
     * append its items would exist, craft and work but be invisible in creative -
     * appending here rather than editing ModTabs keeps that file untouched.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class CreativeTab {

        private CreativeTab() { }

        @SubscribeEvent
        public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
            if (!event.getTabKey().equals(ModTabs.MAIN.getKey())) {
                return;
            }
            for (RegistryObject<net.minecraft.world.item.Item> item : KraveMachines.ITEMS.getEntries()) {
                event.accept(item.get());
            }
        }
    }

    /**
     * Connects the Depot to the village module.
     *
     * <p>Done from this module's own common setup rather than asking the
     * orchestrator to add a line, and guarded on nothing else having claimed the
     * slot first - so if the village module (or the orchestrator) registers its
     * own sink, that one wins and this is a no-op.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class VillageWiring {

        private VillageWiring() { }

        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                if (VillageLink.sink() == null) {
                    VillageLink.setSink(new KraveVillageBridge());
                }
            });
        }
    }

    /**
     * Server-side half of the recipe-index invalidation.
     *
     * <p>Fires on world load and on every {@code /reload}. Machines cache the
     * recipe they matched; without this a reload that changed a Grinder recipe
     * would leave every already-placed Grinder running the old one until someone
     * disturbed its input slots.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
    public static final class ServerRecipeSync {

        private ServerRecipeSync() { }

        @SubscribeEvent
        public static void onReload(AddReloadListenerEvent event) {
            RecipeLookup.invalidate();
        }
    }
}
