package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 0: the Krave Shack. One room, one bed, one barrel, and a ladder to the
 * roof, because the roof is flat and there is nowhere else to put anything.
 *
 * <p><b>Footprint</b> 5 x 6 - the extra course of depth is the salvage awning
 * over the door. <b>Walls</b> three courses. <b>Roof</b> a flat sheeted deck
 * with a ragged kerb, reachable through a hatch. That hatch is not decoration:
 * every later rung in this module is something built on top of something else,
 * and this is the first roof it can be built on.
 *
 * <p><b>Why the floor is at y = -1.</b> The engine's local {@code y = 0} is the
 * first course above levelled ground, so a floor drawn there leaves the doorway
 * a block below the room and you step up into your own house. Drawing the floor
 * at {@code y = -1} replaces the ground surface instead and the threshold comes
 * out flush. Every building in this module does the same - do not "correct" one
 * of them to match the housing module, which uses the other convention
 * throughout and has the step to prove it.
 *
 * <p><b>Village contribution</b> arrives automatically through {@code
 * VillageBuffs}, from the blocks themselves rather than from anything this class
 * declares: bed 2 building / 6 attraction, Krave door 1 / 2, barrel and chest 1
 * each, lantern 1 defence. Houses one resident.
 */
final class KraveShack {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_krave_shack");

    private KraveShack() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(35);

        final int LAST_X = 4;
        final int FRONT = 4;        // last z of the room; z = 5 is the awning
        final int EAVE = 2;         // top wall course
        final int ROOF = EAVE + 1;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 1);

        // Everything below goes in after the window scatter, because the scatter
        // is allowed to resolve to air and would otherwise be free to knock a
        // hole in the wall the ladder hangs on or in the top half of the door.
        b.column(2, 0, 0, EAVE, WALL);
        b.door(2, 0, FRONT, ModBlocks.KRAVE_DOOR);

        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);

        // Salvage nailed over the door, one course proud of the wall.
        SlumRoof.awning(b, 1, 3, FRONT + 1, ROOF, ROOF_S);

        // Roof access. A ladder's FACING names the way you look while climbing,
        // so the wall holding it up is the course on the OPPOSITE side - south
        // here means the back wall at z = 0.
        b.column(2, 1, 0, EAVE, LADDER_S);
        b.carve(2, ROOF, 1, 2, ROOF, 1);

        b.bed(1, 0, 3, () -> Blocks.RED_BED, Direction.NORTH);
        b.set(3, 0, 1, BARREL);
        b.set(3, 0, 2, CHEST);
        b.set(1, 0, 1, STASH_BOX);
        b.set(3, 0, 3, LANTERN);

        b.marker("staff0", 2, 0, 3);

        b.core(2, -1, FRONT);
        return b.build();
    }
}
