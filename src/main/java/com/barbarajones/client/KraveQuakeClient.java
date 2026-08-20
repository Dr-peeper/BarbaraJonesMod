package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side half of {@code apocalypse.KraveQuake}: sky darkening and a
 * real camera shake for a Cayden Cobb ascension. Driven entirely off
 * CaydenCobb's own public, already-synced entity state -
 * {@link CaydenCobb#getTier()} - so it needs no packet of its own and does
 * not touch CaydenCobb or SsjAuraLayer. Every client tick it watches every
 * loaded CaydenCobb for a tier that just went UP and starts a short,
 * tier-scaled, decaying pulse for that entity; several pets ascending at
 * once just take the strongest pulse instead of stacking.
 *
 * <p>Reuses the technique {@code ApocalypseClient} already proved out - a
 * decaying amplitude driving a screen effect - without touching that class
 * or its {@code PacketApocalypse}-driven jumpscare state, which is a
 * different show with different triggers. The shake here is a real camera
 * perturbation ({@link ViewportEvent.ComputeCameraAngles}, fired every
 * frame with the vanilla-computed angles already filled in) rather than
 * ApocalypseClient's GUI-overlay-only shake, since there is no overlay
 * content here to shake.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveQuakeClient {

    /** Roughly how many ticks one pulse lives per tier - SSJ ~2.5s, ULTRA ~9s. */
    private static final int TICKS_PER_TIER = 25;
    /** Beyond this many blocks from the epicenter, the pulse is fully faded out. */
    private static final double MAX_RANGE = 220.0D;

    private static final Map<Integer, Integer> lastTier = new HashMap<>();
    private static final Map<Integer, Pulse> pulses = new HashMap<>();

    private KraveQuakeClient() { }

    private static final class Pulse {
        final int entityId;
        final int tier;
        int age;

        Pulse(int entityId, int tier) {
            this.entityId = entityId;
            this.tier = tier;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!pulses.isEmpty()) {
                pulses.clear();
            }
            if (!lastTier.isEmpty()) {
                lastTier.clear();
            }
            return;
        }

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof CaydenCobb cayden)) {
                continue;
            }
            int id = cayden.getId();
            int tier = cayden.getTier();
            Integer prev = lastTier.put(id, tier);
            if (tier >= AscensionLadder.SSJ && (prev == null || tier > prev)) {
                pulses.put(id, new Pulse(id, tier));
            }
        }

        Iterator<Pulse> it = pulses.values().iterator();
        while (it.hasNext()) {
            Pulse p = it.next();
            p.age++;
            if (p.age > lifespan(p.tier)) {
                it.remove();
            }
        }
    }

    private static int lifespan(int tier) {
        return TICKS_PER_TIER * (tier + 2);
    }

    /** 0..1, sharp attack then a slow curved decay - a shock, not a fade-in. */
    private static float timeIntensity(Pulse p) {
        float life = lifespan(p.tier);
        float t = Mth.clamp(p.age / life, 0.0F, 1.0F);
        float attack = Mth.clamp(p.age / 4.0F, 0.0F, 1.0F);
        float decay = 1.0F - t * t;
        return attack * decay * (0.45F + p.tier * 0.09F);
    }

    /** Falls off with distance from the ascending Cayden's CURRENT position, read fresh each call. */
    private static float rangeFalloff(Pulse p, Player viewer, Minecraft mc) {
        if (mc.level == null) {
            return 0.0F;
        }
        Entity owner = mc.level.getEntity(p.entityId);
        double dist = owner != null ? owner.position().distanceTo(viewer.position()) : MAX_RANGE;
        return (float) Mth.clamp(1.0D - dist / MAX_RANGE, 0.0D, 1.0D);
    }

    /** The strongest live pulse as seen by the current player, or null if nothing is active nearby. */
    private static Pulse strongest(Minecraft mc) {
        if (mc.player == null || pulses.isEmpty()) {
            return null;
        }
        Pulse best = null;
        float bestScore = 0.0F;
        for (Pulse p : pulses.values()) {
            float score = timeIntensity(p) * rangeFalloff(p, mc.player, mc);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }
        return bestScore > 0.005F ? best : null;
    }

    // ---- sky darkening -------------------------------------------------

    @SubscribeEvent
    public static void onFog(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        Pulse p = strongest(mc);
        if (p == null) {
            return;
        }
        float k = Mth.clamp(timeIntensity(p) * rangeFalloff(p, mc.player, mc) * 0.55F, 0.0F, 0.55F);
        event.setRed(event.getRed() * (1.0F - k));
        event.setGreen(event.getGreen() * (1.0F - k));
        event.setBlue(event.getBlue() * (1.0F - k) + 0.035F * k);
    }

    // ---- camera shake ----------------------------------------------------

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        Pulse p = strongest(mc);
        if (p == null) {
            return;
        }
        float amp = timeIntensity(p) * rangeFalloff(p, mc.player, mc) * 2.4F;
        if (amp <= 0.015F) {
            return;
        }
        float t = (System.nanoTime() % 2_000_000_000L) / 1.0e9F;
        float yaw = Mth.sin(t * 23.0F) * amp;
        float pitch = Mth.cos(t * 27.5F) * amp * 0.6F;
        float roll = Mth.sin(t * 15.0F) * amp * 0.4F;
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
