package com.barbarajones.v2.mobs;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's OWN item registry: the five spawn eggs plus two small flavor
 * items (the shard Krispbone throws, and the Mascot's trophy drop). Everything
 * a player can hold from the Craveling family lives here, not in the shared
 * {@code content.ModItems}.
 *
 * <p>NOTE for whoever owns {@code content/ModTabs.java}: these will NOT show
 * up in the creative tab on their own - that tab only iterates
 * {@code ModItems.ITEMS.getEntries()}. See docs/modules/craveling-mobs.md for
 * the exact line to add.
 */
public final class ModMobItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    private ModMobItems() { }

    private static Item.Properties props() {
        return new Item.Properties();
    }

    // ---- spawn eggs ---------------------------------------------------------
    // Colors: primary = base body, secondary = spots/accent. Picked per mob's
    // palette so the egg reads correctly in the creative menu at a glance.

    public static final RegistryObject<Item> CRAVELING_SPAWN_EGG = ITEMS.register("craveling_spawn_egg",
            () -> new ForgeSpawnEggItem(ModMobEntities.CRAVELING, 0x8A6134, 0xD9A857, props()));

    public static final RegistryObject<Item> KRISPBONE_SPAWN_EGG = ITEMS.register("krispbone_spawn_egg",
            () -> new ForgeSpawnEggItem(ModMobEntities.KRISPBONE, 0xC9AD7F, 0x6B4A2F, props()));

    public static final RegistryObject<Item> LOOMWEAVER_SPAWN_EGG = ITEMS.register("loomweaver_spawn_egg",
            () -> new ForgeSpawnEggItem(ModMobEntities.LOOMWEAVER, 0x3A2C1E, 0xEDE6D6, props()));

    public static final RegistryObject<Item> SOGGY_SPAWN_EGG = ITEMS.register("soggy_spawn_egg",
            () -> new ForgeSpawnEggItem(ModMobEntities.SOGGY, 0x6E7A4A, 0xC7D9A0, props()));

    public static final RegistryObject<Item> MASCOT_SPAWN_EGG = ITEMS.register("the_mascot_spawn_egg",
            () -> new ForgeSpawnEggItem(ModMobEntities.MASCOT, 0xD9384A, 0xF2D33A, props()));

    // ---- flavor items ---------------------------------------------------------

    /** Krispbone's ammo - also what {@link com.barbarajones.v2.mobs.entity.projectile.KraveShardEntity} renders as. */
    public static final RegistryObject<Item> KRAVE_SHARD = ITEMS.register("krave_shard",
            () -> new Item(props().stacksTo(64)));

    /** The Mascot's rare guaranteed drop - a trophy, not equipment. */
    public static final RegistryObject<Item> CEREAL_MASCOT_HEAD = ITEMS.register("cereal_mascot_head",
            () -> new Item(props().stacksTo(1).rarity(Rarity.EPIC)));
}
