package com.barbarajones.v2.abilities;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.abilities.item.AscensionCharmItem;
import com.barbarajones.v2.abilities.item.GodCoreItem;
import com.barbarajones.v2.abilities.item.InstinctBandItem;
import com.barbarajones.v2.abilities.item.KraveGauntletItem;
import com.barbarajones.v2.abilities.item.LaserLensItem;
import com.barbarajones.v2.abilities.item.MeteorTotemItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's own item registry - rule #3's pattern, applied here. Nothing
 * outside this package touches this {@code DeferredRegister} directly; the
 * only door in is {@link PlayerAbilities#init}.
 *
 * <p>Every entry is one hard-won power. All six share the same shape: not
 * stackable, not damageable, {@link Rarity#EPIC} so they read as end-game the
 * moment they turn up in an inventory, and completely inert until the quest
 * module's {@code QuestApi.hasAbility} says the matching quest or boss kill
 * has been cleared (see {@code AbilityData#activate}).
 */
public final class AbilityItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    private static Item.Properties props() {
        return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC);
    }

    public static final RegistryObject<Item> KRAVE_GAUNTLET =
            ITEMS.register(AbilityId.GAUNTLET.id, () -> new KraveGauntletItem(props()));
    public static final RegistryObject<Item> LASER_LENS =
            ITEMS.register(AbilityId.LASER.id, () -> new LaserLensItem(props()));
    public static final RegistryObject<Item> ASCENSION_CHARM =
            ITEMS.register(AbilityId.CHARM.id, () -> new AscensionCharmItem(props()));
    public static final RegistryObject<Item> METEOR_TOTEM =
            ITEMS.register(AbilityId.TOTEM.id, () -> new MeteorTotemItem(props()));
    public static final RegistryObject<Item> INSTINCT_BAND =
            ITEMS.register(AbilityId.BAND.id, () -> new InstinctBandItem(props()));
    public static final RegistryObject<Item> GOD_CORE =
            ITEMS.register(AbilityId.GODCORE.id, () -> new GodCoreItem(props()));

    private AbilityItems() { }

    /** The registry object for a given ability, in {@link AbilityId} order - used by the creative tab hook. */
    public static RegistryObject<Item> itemFor(AbilityId id) {
        return switch (id) {
            case GAUNTLET -> KRAVE_GAUNTLET;
            case LASER -> LASER_LENS;
            case CHARM -> ASCENSION_CHARM;
            case TOTEM -> METEOR_TOTEM;
            case BAND -> INSTINCT_BAND;
            case GODCORE -> GOD_CORE;
        };
    }
}
