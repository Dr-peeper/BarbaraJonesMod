package com.barbarajones.v2.internet;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Every Forge-bus hook the outage/boss module needs, self-registered so the
 * orchestrator never has to touch {@code EventHandler} - see the "own your
 * registries" rule in the module doc. Default bus is FORGE, same split
 * {@code AbilityEvents} uses for its own nested MOD-bus registrar.
 *
 * <ul>
 *   <li>{@link #onServerTick} drives {@link OutageEvent#tick} for every
 *       loaded level, once per level per server tick - this is the entire
 *       scheduler. It is deliberately NOT scoped to the overworld only:
 *       {@link OutageEvent} is per-{@code ServerLevel} SavedData, so a
 *       modded dimension with its own housed Cayden gets its own independent
 *       schedule for free.</li>
 *   <li>{@link #onPlayerTick} drives {@link LatencyTracker#tick}, which has
 *       to run for every player every tick regardless of whether an outage
 *       or a boss is anywhere nearby - a player can be stuttering from a
 *       LATENCY hit while the boss that caused it is thirty blocks away.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class InternetOutageEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    private InternetOutageEvents() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            try {
                OutageEvent.get(level).tick(level);
            } catch (Throwable err) {
                // Never let a scheduler fault wedge the server tick - the same
                // defensive shape KraveApocalypse.tickAll() uses. Logged rather
                // than silently swallowed, so a real bug is still discoverable.
                LOGGER.error("Internet outage scheduler faulted in {}", level.dimension().location(), err);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            LatencyTracker.tick(player);
        }
    }

    /** MOD-bus registrar, split out exactly the way {@code AbilityEvents.TabInjector} is. */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registrar {

        private Registrar() { }

        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(InternetContent.INTERNET_MANAGER.get(), InternetManagerBoss.createAttributes().build());
        }
    }
}
