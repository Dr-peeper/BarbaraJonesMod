package com.barbarajones.client.ui;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The mod's keybinds: K opens the Krave Codex, G answers the boss-fight
 * finisher prompt.
 *
 * <p>Registration has to happen on the MOD bus and the key poll on the FORGE bus,
 * which is why the registrar is a nested subscriber rather than another method here.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveKeys {

    public static final String CATEGORY = "key.categories." + BarbaraJonesMod.MODID;

    public static final KeyMapping OPEN_CODEX = new KeyMapping(
            "key." + BarbaraJonesMod.MODID + ".codex",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY);

    /**
     * The boss-fight finisher prompt.
     *
     * <p>G rather than a mouse button or a movement key: the prompt appears
     * mid-fight while the player is already holding something down, and binding
     * it to anything they might be pressing anyway would let the game answer the
     * prompt for them.
     */
    public static final KeyMapping FINISHER = new KeyMapping(
            "key." + BarbaraJonesMod.MODID + ".finisher",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            CATEGORY);

    /** The bound key as the player would read it, for the on-screen prompt. */
    public static String finisherKeyName() {
        try {
            return FINISHER.getTranslatedKeyMessage().getString().toUpperCase(java.util.Locale.ROOT);
        } catch (Throwable ignored) {
            return "G";
        }
    }

    private KraveKeys() { }

    /** The bound key as the player would read it, for the Controls list in the codex. */
    public static String openCodexKeyName() {
        try {
            return OPEN_CODEX.getTranslatedKeyMessage().getString();
        } catch (Throwable ignored) {
            return "K";
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // consumeClick has to drain even when we ignore the press, or the click
        // count carries over and the codex pops open the moment a screen closes
        boolean pressed = false;
        while (OPEN_CODEX.consumeClick()) {
            pressed = true;
        }
        if (!pressed || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        try {
            mc.setScreen(new KraveCodexScreen());
        } catch (Throwable ignored) {
            // a broken screen must never wedge the client
        }
    }

    /** Mod-bus half: hands the mapping to Forge so it shows up in Controls. */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registrar {

        private Registrar() { }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_CODEX);
            event.register(FINISHER);
        }
    }
}
