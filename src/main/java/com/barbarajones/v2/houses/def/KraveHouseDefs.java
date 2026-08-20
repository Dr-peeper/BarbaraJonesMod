package com.barbarajones.v2.houses.def;

import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureRegistry;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The ten village buildings, ascending from a shack to a mansion, and the one
 * place that knows all ten ids.
 *
 * <p>Every other class outside this package - {@code KraveHouses} for item
 * registration, anything that wants to ask "how many people does this
 * building sleep" - goes through here rather than importing the individual
 * {@code LeanTo}/{@code Cottage}/... classes directly, which is why those
 * classes stay package-private. One seam, not ten.
 *
 * <h2>Villager capacity</h2>
 * The numbers in {@link #capacities()} are a direct count of the beds each
 * definition places - see each class's own javadoc for the placement and the
 * reasoning. They are NOT how the village module scores these buildings
 * toward its tier: that happens automatically and separately, per placed
 * block, via {@code VillageBuffs} (a bed there is worth building score,
 * attraction and happiness the instant it exists in a claim, with nothing
 * required from this module). This table exists purely so "how much housing
 * is this" has an honest, single-call answer.
 *
 * <p>Capacity is deliberately NOT monotonic rung to rung. The tower house (a
 * garrison keep) and the great hall (civic infrastructure with a small guest
 * wing) both sleep fewer people than the buildings just below them in the
 * ladder - they are still unambiguously bigger, costlier and more advanced
 * builds, they are just not primarily housing. The mansion at the top sleeps
 * more than everything else combined.
 */
public final class KraveHouseDefs {

    public static final ResourceLocation LEAN_TO = LeanTo.ID;
    public static final ResourceLocation SMALL_HOUSE = SmallHouse.ID;
    public static final ResourceLocation COTTAGE = Cottage.ID;
    public static final ResourceLocation TWO_STOREY_HOUSE = TwoStoreyHouse.ID;
    public static final ResourceLocation RANCH = Ranch.ID;
    public static final ResourceLocation LONGHOUSE = Longhouse.ID;
    public static final ResourceLocation MANOR = Manor.ID;
    public static final ResourceLocation TOWER_HOUSE = TowerHouse.ID;
    public static final ResourceLocation GREAT_HALL = GreatHall.ID;
    public static final ResourceLocation KRAVE_MANSION = KraveMansion.ID;

    /** Registration order - also the ladder order, shack to mansion. */
    public static final ResourceLocation[] ORDER = {
            LEAN_TO, SMALL_HOUSE, COTTAGE, TWO_STOREY_HOUSE, RANCH,
            LONGHOUSE, MANOR, TOWER_HOUSE, GREAT_HALL, KRAVE_MANSION
    };

    private static final Map<ResourceLocation, Integer> CAPACITY = new LinkedHashMap<>();

    static {
        CAPACITY.put(LEAN_TO, 1);
        CAPACITY.put(SMALL_HOUSE, 1);
        CAPACITY.put(COTTAGE, 2);
        CAPACITY.put(TWO_STOREY_HOUSE, 3);
        CAPACITY.put(RANCH, 4);
        CAPACITY.put(LONGHOUSE, 6);
        CAPACITY.put(MANOR, 5);
        CAPACITY.put(TOWER_HOUSE, 3);
        CAPACITY.put(GREAT_HALL, 4);
        CAPACITY.put(KRAVE_MANSION, 8);
    }

    private KraveHouseDefs() {
    }

    /** Builds and registers all ten definitions with {@link StructureRegistry}. Call once, from {@code init(bus)}. */
    public static void registerAll() {
        StructureRegistry.register(LeanTo.build());
        StructureRegistry.register(SmallHouse.build());
        StructureRegistry.register(Cottage.build());
        StructureRegistry.register(TwoStoreyHouse.build());
        StructureRegistry.register(Ranch.build());
        StructureRegistry.register(Longhouse.build());
        StructureRegistry.register(Manor.build());
        StructureRegistry.register(TowerHouse.build());
        StructureRegistry.register(GreatHall.build());
        StructureRegistry.register(KraveMansion.build());
    }

    /** Declared villager capacity (bed count) for a house id, or 0 if unknown. */
    public static int capacity(ResourceLocation id) {
        return CAPACITY.getOrDefault(id, 0);
    }

    public static Map<ResourceLocation, Integer> capacities() {
        return Map.copyOf(CAPACITY);
    }

    /** Convenience: looks the definition back up in {@link StructureRegistry} after {@link #registerAll()}. */
    public static StructureDef get(ResourceLocation id) {
        return StructureRegistry.get(id);
    }
}
