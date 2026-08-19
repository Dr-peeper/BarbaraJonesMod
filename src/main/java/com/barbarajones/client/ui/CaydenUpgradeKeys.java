package com.barbarajones.client.ui;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.CaydenCobb;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * The upgrade keybind (U by default) and the one client-side door into
 * {@link CaydenUpgradeScreen}.
 *
 * <p>Everything here is client only. {@code CaydenCobb.mobInteract} reaches the
 * {@link #open(CaydenCobb)} helper through {@code DistExecutor}, so the class is
 * never loaded on a dedicated server despite being named from common code.
 *
 * <p>Registration goes on the MOD bus and the key poll on the FORGE bus, which
 * is why the registrar is a nested subscriber - the same shape {@link KraveKeys}
 * uses.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class CaydenUpgradeKeys {

    /** How far the keybind will look for a Cayden of your own. */
    private static final double REACH = 20.0D;

    public static final KeyMapping OPEN_UPGRADES = new KeyMapping(
            "key." + BarbaraJonesMod.MODID + ".cayden_upgrades",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_U,
            KraveKeys.CATEGORY);

    private CaydenUpgradeKeys() { }

    /** The bound key as the player would read it, for the codex controls list. */
    public static String keyName() {
        try {
            return OPEN_UPGRADES.getTranslatedKeyMessage().getString();
        } catch (Throwable ignored) {
            return "U";
        }
    }

    /** Opens the ledger on a specific Cayden. Safe to call with anything. */
    public static void open(@Nullable CaydenCobb cayden) {
        if (cayden == null || !cayden.isAlive()) {
            return;
        }
        try {
            CaydenUpgradeScreen.open(cayden);
        } catch (Throwable ignored) {
            // a broken screen must never wedge the client
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // consumeClick has to drain even when the press is ignored, or the count
        // carries over and the screen pops open the moment another one closes
        boolean pressed = false;
        while (OPEN_UPGRADES.consumeClick()) {
            pressed = true;
        }
        if (!pressed || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        CaydenCobb found = nearestOwned(mc.player);
        if (found == null) {
            mc.player.displayClientMessage(Component.literal(ChatFormatting.GRAY
                    + "Cayden is not close enough to talk to."), true);
            return;
        }
        open(found);
    }

    /**
     * The nearest Cayden the player owns.
     *
     * <p>Deliberately short-ranged: the screen reads his live entity data, and
     * an entity outside the client's tracking distance would draw a stale ladder.
     */
    @Nullable
    private static CaydenCobb nearestOwned(LocalPlayer player) {
        AABB box = player.getBoundingBox().inflate(REACH);
        CaydenCobb best = null;
        double bestD = Double.MAX_VALUE;
        for (CaydenCobb c : player.level().getEntitiesOfClass(CaydenCobb.class, box)) {
            if (!c.isAlive() || !c.isOwnedBy(player)) {
                continue;
            }
            double d = c.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Mod-bus half: hands the mapping to Forge so it shows up in Controls. */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registrar {

        private Registrar() { }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_UPGRADES);
        }
    }
}
