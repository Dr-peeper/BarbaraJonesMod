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
     * The six boss-fight finisher keys, in the order the forms use them.
     *
     * <p>G, K, H, J, V, B - deliberately none of them a key already held during
     * a fight. The prompt appears mid-combat while the player is likely holding
     * movement, attack or sprint, and binding a finisher to any of those would
     * let the game answer the prompt on the player+s behalf.
     *
     * <p>Indexed by the finisher step, so the server decides which one is live
     * and the client only draws what it was told.
     */
    public static final KeyMapping[] FINISHERS = {
        finisher("finisher_g", InputConstants.KEY_G),
        finisher("finisher_k", InputConstants.KEY_K),
        finisher("finisher_h", InputConstants.KEY_H),
        finisher("finisher_j", InputConstants.KEY_J),
        finisher("finisher_v", InputConstants.KEY_V),
        finisher("finisher_b", InputConstants.KEY_B),
    };

    private static KeyMapping finisher(String name, int code) {
        return new KeyMapping("key." + BarbaraJonesMod.MODID + "." + name,
                InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    /** The binding for a letter the server asked for, or null if it is not one of ours. */
    public static KeyMapping finisherFor(String letter) {
        for (KeyMapping k : FINISHERS) {
            if (k.getName().endsWith("_" + letter.toLowerCase(java.util.Locale.ROOT))) {
                return k;
            }
        }
        return null;
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
            for (KeyMapping k : FINISHERS) {
                event.register(k);
            }
        }
    }
}
