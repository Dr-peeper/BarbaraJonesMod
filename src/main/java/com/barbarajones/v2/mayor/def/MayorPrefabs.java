package com.barbarajones.v2.mayor.def;

import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureRegistry;

import net.minecraft.resources.ResourceLocation;

/**
 * The nineteen things Barbara can put up, and the one place that knows all of
 * their ids.
 *
 * <p>Nine buildings and ten road segments (five rungs of decay, two lettered
 * variants each). Everything outside this package goes through the constants
 * here rather than importing {@code TrapHouse} or {@code RoadKit} directly,
 * which is why those classes stay package-private: one seam, not nineteen.
 *
 * <h2>Staff markers</h2>
 * Every building that comes with residents names their spawn positions with
 * {@code StructureDef.Builder#marker} under the keys {@code staff0},
 * {@code staff1}, and so on. The mayor resolves those to world positions after
 * the build finishes and spawns exactly that many Krave Villagers - so the
 * number of residents a building houses is a property of the building, declared
 * once, in the building. Add a bed to a definition and you must add a marker, or
 * the bed is decoration.
 *
 * <p>Roads carry no markers: nobody lives in a road, however much this village
 * looks like they might.
 *
 * <h2>Registration</h2>
 * {@link #registerAll()} must run during mod construction, from the module's
 * {@code init(bus)}. Definitions bake their whole plan at that moment, which is
 * why every palette entry in {@link SlumPalette} is a supplier - the blocks they
 * name do not exist yet.
 */
public final class MayorPrefabs {

    public static final ResourceLocation KRAVE_SHACK = KraveShack.ID;
    public static final ResourceLocation PATCHWORK_HOUSE = PatchworkHouse.ID;
    public static final ResourceLocation MARKET_STALL = MarketStall.ID;
    public static final ResourceLocation CORNER_STORE = CornerStore.ID;
    public static final ResourceLocation TRAP_HOUSE = TrapHouse.ID;
    public static final ResourceLocation WORKSHOP = Workshop.ID;
    public static final ResourceLocation STACKED_TENEMENT = StackedTenement.ID;
    public static final ResourceLocation PLUG_HEADQUARTERS = PlugHeadquarters.ID;
    public static final ResourceLocation KRAVE_SPIRE = KraveSpire.ID;

    /** How many rungs of road decay exist. The last one is the end state. */
    public static final int ROAD_STAGES = RoadKit.STAGES;
    /** How far one Road Expansion project carries a spur, in blocks. */
    public static final int ROAD_SEGMENT_LENGTH = RoadKit.SPAN_Z;

    private static boolean registered;

    private MayorPrefabs() { }

    /** The id of one road segment. Clamps rather than throwing; see {@link RoadKit#id}. */
    public static ResourceLocation road(int stage, int variant) {
        return RoadKit.id(stage, variant);
    }

    /**
     * Bakes and registers every definition.
     *
     * <p>Idempotent, because an integrated server and a dedicated one reach mod
     * construction by slightly different routes and {@code StructureRegistry}
     * throws on a duplicate id rather than quietly replacing - which is the
     * right behaviour for it and the wrong crash to hand a player.
     */
    public static synchronized void registerAll() {
        if (registered) {
            return;
        }
        registered = true;

        StructureRegistry.register(KraveShack.build());
        StructureRegistry.register(PatchworkHouse.build());
        StructureRegistry.register(MarketStall.build());
        StructureRegistry.register(CornerStore.build());
        StructureRegistry.register(TrapHouse.build());
        StructureRegistry.register(Workshop.build());
        StructureRegistry.register(StackedTenement.build());
        StructureRegistry.register(PlugHeadquarters.build());
        StructureRegistry.register(KraveSpire.build());

        for (int stage = 0; stage < RoadKit.STAGES; stage++) {
            for (int variant = 0; variant < RoadKit.VARIANTS.length; variant++) {
                StructureRegistry.register(RoadKit.build(stage, variant));
            }
        }
    }

    /** Looks a definition back up after {@link #registerAll()}. Null before it. */
    public static StructureDef get(ResourceLocation id) {
        return StructureRegistry.get(id);
    }
}
