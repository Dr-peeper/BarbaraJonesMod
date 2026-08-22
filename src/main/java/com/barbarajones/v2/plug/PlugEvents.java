package com.barbarajones.v2.plug;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.ThePlug;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The three hooks the job board needs into the running game.
 *
 * <p>Registered by annotation on the Forge bus, so there is nothing for anyone to
 * wire up centrally and no way for this module to end up written but never
 * connected. The module registers no items, blocks or entities of its own - it
 * runs entirely on things the mod already had - so there is deliberately no
 * {@code init(IEventBus)} anywhere in the package for a mod-bus registration to
 * hide behind.
 *
 * <h2>The slow timer</h2>
 * A job is a return time on the overworld clock, so "has he finished" costs one
 * comparison. That check runs every {@value #CHECK_INTERVAL} ticks and only on a
 * world where somebody has actually hired him - {@link PlugJobData#getExisting}
 * returns null otherwise and nothing else happens. There is no scanning, no
 * entity search, and nothing at all is ticked per Plug.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class PlugEvents {

    /** How often finished jobs are noticed. Two seconds is well inside "he just got back". */
    private static final int CHECK_INTERVAL = 40;

    /** How long after swinging on him before it counts as a fresh offence. */
    private static final int SCOLD_COOLDOWN = 100;

    /**
     * Last time each player was docked for hitting him, in overworld game time.
     *
     * <p>Purely transient anti-spam - a sword swing fires the hurt event several
     * times a second, and without this one fight would wipe out a relationship
     * and print thirty lines of chat doing it. Nothing here is world state: the
     * reputation it protects lives in {@link PlugJobData}, and losing this map on
     * restart costs at most one extra telling-off.
     */
    private static final Map<UUID, Long> LAST_SCOLDED = new HashMap<>();

    private PlugEvents() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null || overworld.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        PlugJobData data = PlugJobData.getExisting(server);
        if (data == null) {
            return;
        }
        data.refresh(server);
    }

    /** Swinging on your own Plug. Costs standing, and he has something to say about it. */
    @SubscribeEvent
    public static void onPlugHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ThePlug plug) || plug.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        MinecraftServer server = plug.level().getServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        Long last = LAST_SCOLDED.get(player.getUUID());
        if (last != null && now - last < SCOLD_COOLDOWN) {
            return;
        }
        LAST_SCOLDED.put(player.getUUID(), now);
        PlugBusiness.assaulted(plug, player);
    }

    /**
     * A job outlives a session, so the first thing a returning player hears is
     * where it got to. This is also the proof that the timer is real save data
     * and not something being kept alive in memory while you are online.
     */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        PlugJobData data = PlugJobData.getExisting(server);
        if (data == null) {
            return;
        }
        data.refresh(server);
        PlugContract contract = data.contract(player.getUUID());
        if (contract == null) {
            return;
        }
        if (contract.isReady()) {
            PlugLines.say(player, "where you BEEN. I been holdin your "
                    + contract.job().label() + " bag this whole time.");
            return;
        }
        PlugLines.note(player, "(The Plug is still out on " + contract.job().label()
                + " - about " + contract.secondsLeft(server.overworld().getGameTime())
                + " seconds out.)");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SCOLDED.remove(event.getEntity().getUUID());
    }
}
