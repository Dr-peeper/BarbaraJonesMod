package com.barbarajones.v2.abilities.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.v2.abilities.net.AbilityNetwork;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * One keybind per ability, all unbound by default (the player picks their
 * own six keys in Controls -> Barbara Jones, the same
 * {@code key.categories.barbarajones} category {@code KraveKeys.OPEN_CODEX}
 * already uses) so nothing here can collide with a key someone else is
 * already using.
 *
 * <p>A press sends {@code AbilityId.index}, not an item or a slot - the
 * server works out which carried item that maps to. That single indirection
 * is the entire "keybind system for activating whichever abilities the
 * player has equipped" the brief asks for: bind six keys once, and whichever
 * of the six items you are actually carrying that day responds to them.
 *
 * <p>Same split as {@code KraveKeys}: registration happens on the MOD bus
 * (the nested {@link Registrar}), polling happens on the FORGE bus (the
 * outer class, which is this class's default bus).
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class AbilityKeys {

    private static final String CATEGORY = "key.categories." + BarbaraJonesMod.MODID;

    private static final KeyMapping[] KEYS = new KeyMapping[AbilityId.COUNT];

    static {
        for (AbilityId id : AbilityId.VALUES) {
            KEYS[id.index] = new KeyMapping(
                    "key." + BarbaraJonesMod.MODID + ".ability_" + id.id,
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY);
        }
    }

    private AbilityKeys() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        AbilityClientState.advanceClock();

        Minecraft mc = Minecraft.getInstance();
        for (AbilityId id : AbilityId.VALUES) {
            boolean pressed = false;
            while (KEYS[id.index].consumeClick()) {
                pressed = true;
            }
            if (pressed && mc.player != null && mc.level != null) {
                try {
                    AbilityNetwork.sendActivate(id.index);
                } catch (Throwable ignored) {
                    // a bad send must never wedge the client's tick loop
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registrar {

        private Registrar() { }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            for (KeyMapping key : KEYS) {
                event.register(key);
            }
        }
    }
}
