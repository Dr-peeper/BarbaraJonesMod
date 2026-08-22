package com.barbarajones.v2.quests.client;

import com.barbarajones.BarbaraJonesMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

/**
 * Client wiring for the quest module. Registers its own key binding from inside the
 * module, so {@code ClientSetup} - which several other agents are editing - does not
 * have to be touched.
 */
public final class QuestClientSetup {

    private QuestClientSetup() {
    }

    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
            "key.barbarajones.open_quests",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.barbarajones");

    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_QUESTS);
        }
    }

    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {

        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            boolean pressed = false;
            while (OPEN_QUESTS.consumeClick()) {
                pressed = true;
            }
        // A live finisher prompt owns its key for as long as it is up.
        //
        // The finisher keys were chosen from a spec without checking what this
        // mod already binds, and two of the six collided: K opens the quest
        // atlas and the Krave Codex, V opens the village screen. Pressing K
        // mid-finisher therefore answered the prompt AND threw a full-screen GUI
        // over the cinematic it had just started.
        //
        // Suppressed rather than rebound, because the prompt names the key on
        // screen and moving it would make the on-screen instruction wrong; and
        // because this also covers the case that cannot be fixed by choosing
        // better letters - another mod binding over one of them. The click is
        // still drained above, so it does not fire the moment the prompt clears.
            if (pressed && !com.barbarajones.client.KraveQteClient.active()) {
                QuestScreens.open();
            }
        }

        /**
         * Leaving a world must drop the mirror. Carrying one world's completion set
         * into the next is exactly the kind of stale-client bug this rewrite exists
         * to remove, and it would be invisible until a player wondered why a brand
         * new save started half finished.
         */
        @SubscribeEvent
        public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            ClientQuests.clear();
        }
    }
}
