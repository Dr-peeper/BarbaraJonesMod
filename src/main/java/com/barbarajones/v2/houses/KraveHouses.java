package com.barbarajones.v2.houses;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.item.KraveSchematicItem;
import com.barbarajones.v2.houses.def.KraveHouseDefs;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The village housing module: ten buildings, ascending from a Lean-To to the
 * Krave Mansion, and the one entry point that wires them in.
 *
 * <p>Wiring, for whoever is holding {@code BarbaraJonesMod.java}: add exactly
 * one line to the constructor, after {@code KraveBuild.init(bus)} (this
 * module's schematics need {@code KraveBuild}'s core block and item already
 * registered on the same bus, though Forge's deferred registration means the
 * actual order those two calls run in does not matter, only that both run):
 * <pre>{@code
 * com.barbarajones.v2.houses.KraveHouses.init(bus);
 * }</pre>
 * Nothing else in this module needs central wiring. Each building's schematic
 * appears in the creative tab automatically - {@code KraveBuild.Tabs} already
 * iterates every registered {@code StructureDef} and drops one schematic in
 * per building, this module does not add its own tab code.
 *
 * <h2>What actually lives here</h2>
 * <ul>
 *   <li>{@code def.KraveHouseDefs} builds and registers the ten {@code
 *       StructureDef}s against {@code StructureRegistry} - read that class
 *       first, it is the index of the other ten.</li>
 *   <li>This class registers one dedicated {@link KraveSchematicItem} per
 *       building, so each gets its own item id, its own icon, and (per
 *       {@code KraveSchematicItem}'s own doc) is what a refund hands back -
 *       rather than sharing {@code KraveBuild}'s generic NBT-tagged item.</li>
 *   <li>Recipes for all ten live as ordinary data-driven crafting recipes
 *       under {@code data/barbarajones/recipes/schematic_*.json} - nothing to
 *       register in code for those.</li>
 * </ul>
 *
 * @see com.barbarajones.v2.houses.def.KraveHouseDefs
 */
public final class KraveHouses {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    public static final RegistryObject<Item> SCHEMATIC_LEAN_TO = schematic("schematic_lean_to", KraveHouseDefs.LEAN_TO);
    public static final RegistryObject<Item> SCHEMATIC_SMALL_HOUSE = schematic("schematic_small_house", KraveHouseDefs.SMALL_HOUSE);
    public static final RegistryObject<Item> SCHEMATIC_COTTAGE = schematic("schematic_cottage", KraveHouseDefs.COTTAGE);
    public static final RegistryObject<Item> SCHEMATIC_TWO_STOREY_HOUSE = schematic("schematic_two_storey_house", KraveHouseDefs.TWO_STOREY_HOUSE);
    public static final RegistryObject<Item> SCHEMATIC_RANCH = schematic("schematic_ranch", KraveHouseDefs.RANCH);
    public static final RegistryObject<Item> SCHEMATIC_LONGHOUSE = schematic("schematic_longhouse", KraveHouseDefs.LONGHOUSE);
    public static final RegistryObject<Item> SCHEMATIC_MANOR = schematic("schematic_manor", KraveHouseDefs.MANOR);
    public static final RegistryObject<Item> SCHEMATIC_TOWER_HOUSE = schematic("schematic_tower_house", KraveHouseDefs.TOWER_HOUSE);
    public static final RegistryObject<Item> SCHEMATIC_GREAT_HALL = schematic("schematic_great_hall", KraveHouseDefs.GREAT_HALL);
    public static final RegistryObject<Item> SCHEMATIC_KRAVE_MANSION = schematic("schematic_krave_mansion", KraveHouseDefs.KRAVE_MANSION);

    private KraveHouses() {
    }

    private static RegistryObject<Item> schematic(String id, net.minecraft.resources.ResourceLocation structure) {
        return ITEMS.register(id, () -> new KraveSchematicItem(new Item.Properties().stacksTo(16), structure));
    }

    /**
     * The module's single entry point. Call from the mod constructor with the
     * mod event bus, alongside {@code KraveBuild.init(bus)}.
     */
    public static void init(IEventBus bus) {
        KraveHouseDefs.registerAll();
        ITEMS.register(bus);
    }
}
