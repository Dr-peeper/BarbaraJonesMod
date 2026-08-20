package com.barbarajones.v2.mobs;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * THE single entry point for this whole module. Whoever wires this module
 * into {@code BarbaraJonesMod}'s constructor needs exactly one line:
 *
 * <pre>{@code
 * com.barbarajones.v2.mobs.Mobs.init(bus);
 * }</pre>
 *
 * That registers this module's four OWN DeferredRegisters (entities, items,
 * sounds, blocks - see {@link ModMobEntities}, {@link ModMobItems},
 * {@link ModMobSounds}, {@link ModMobBlocks}) and schedules
 * {@link MobSpawnPlacements#register()} for common setup. Attribute
 * suppliers ({@link MobAttributes}) and the client renderer bindings
 * ({@code client.MobsClientSetup}) register themselves via
 * {@code @Mod.EventBusSubscriber} and need no call from here.
 *
 * <p>See docs/modules/kraveling-mobs.md for what else the orchestrator needs
 * to wire (the creative tab entry, spawn eggs into it, this call itself).
 */
public final class Mobs {

    private Mobs() { }

    public static void init(IEventBus bus) {
        ModMobEntities.ENTITIES.register(bus);
        ModMobItems.ITEMS.register(bus);
        ModMobSounds.SOUNDS.register(bus);
        ModMobBlocks.BLOCKS.register(bus);

        bus.addListener(Mobs::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MobSpawnPlacements::register);
    }
}
