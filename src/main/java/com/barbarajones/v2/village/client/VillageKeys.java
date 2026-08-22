package com.barbarajones.v2.village.client;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * One keybind: V opens the village screen.
 *
 * <p>V is unbound in vanilla, and the mod's existing codex already owns K, so this
 * adds a key without stepping on either. The mapping is registered from
 * {@link VillageClientSetup} on the mod bus; the poll lives here on the Forge bus,
 * because those are two different buses and the registration event only fires on
 * the first.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class VillageKeys {

    public static final String CATEGORY = "key.categories." + BarbaraJonesMod.MODID;

    public static final KeyMapping OPEN_VILLAGE = new KeyMapping(
            "key." + BarbaraJonesMod.MODID + ".village",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            CATEGORY);

    private VillageKeys() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // consumeClick has to be drained even when the press is ignored, or the
        // queued click fires the moment the next screen closes.
        boolean pressed = false;
        while (OPEN_VILLAGE.consumeClick()) {
            pressed = true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!pressed || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        // V is also the fifth finisher key. See KraveQteClient: a live prompt
        // owns its key, so this screen does not open on top of the cinematic
        // that same press just started.
        if (com.barbarajones.client.KraveQteClient.active()) {
            return;
        }
        try {
            mc.setScreen(new VillageScreen());
        } catch (Throwable ignored) {
            // a broken screen must never wedge the client
        }
    }
}
