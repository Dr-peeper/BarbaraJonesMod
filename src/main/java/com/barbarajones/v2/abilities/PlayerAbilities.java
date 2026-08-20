package com.barbarajones.v2.abilities;

import com.barbarajones.v2.abilities.net.AbilityNetwork;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * The module's one entry point, per rule #3.
 *
 * <p>Add exactly one line to {@code BarbaraJonesMod}'s constructor, next to
 * every other module's {@code init(modBus)} call:
 *
 * <pre>{@code
 *     com.barbarajones.v2.abilities.PlayerAbilities.init(modEventBus);
 * }</pre>
 *
 * <p>Everything else in this module ({@code AbilityEvents}, the keybinds in
 * {@code client.AbilityKeys}, the HUD in {@code client.AbilityHud}) is a
 * {@code @Mod.EventBusSubscriber} and wires itself up the moment Forge scans
 * the classpath - nothing about those needs to be called from here or from
 * anywhere central.
 *
 * <p><b>Load-order note:</b> {@code AbilityEvents} reads
 * {@code v2.quests.QuestApi.hasAbility} once, inside a try/catch, as
 * {@link AbilityId#GODCORE}'s alternate unlock path (see that class's doc) -
 * a method call at runtime, never a class-init-time reference - so it does
 * not matter whether this module's {@code init(bus)} line runs before or
 * after the quest module's.
 */
public final class PlayerAbilities {

    private PlayerAbilities() { }

    public static void init(IEventBus modBus) {
        AbilityItems.ITEMS.register(modBus);
        AbilityNetwork.register();
    }
}
