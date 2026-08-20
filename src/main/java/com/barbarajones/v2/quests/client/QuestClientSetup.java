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
            while (OPEN_QUESTS.consumeClick()) {
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
