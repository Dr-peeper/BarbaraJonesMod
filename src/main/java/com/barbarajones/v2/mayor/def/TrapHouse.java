package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 2: the Trap House. Two storeys, every ground-floor window barred and
 * boarded, one door, and a television on in a room with nothing else in it.
 *
 * <p>This is the first building in the module with a second floor, and the way
 * it gets one is the way every taller building here gets one: the walls simply
 * carry on past the first ceiling, and the ceiling is a floor slab punched
 * through by a ladder. No extra structure, no separate roof - which is exactly
 * why the silhouette stays a box and never starts reading as architecture.
 *
 * <p><b>Footprint</b> 9 x 9 including the awning course. <b>Walls</b> seven
 * courses, split by a floor at y = 3. <b>Sleeps</b> three, all upstairs.
 *
 * <p><b>Village contribution</b>: three beds, iron bars over most of the ground
 * floor (fortification, 1 defence each), two stash boxes - which are worth 5
 * happiness and MINUS 1 defence apiece, and that is not a mistake in the buff
 * table.
 */
final class TrapHouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_trap_house");

    private TrapHouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(110);

        final int LAST_X = 8;
        final int FRONT = 7;
        final int MID = 3;          // the first-floor slab
        final int EAVE = 6;         // top wall course
        final int ROOF = EAVE + 1;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);

        // Four courses of window scatter over two storeys. The ground floor
        // then gets a grille laid over the top of whatever the scatter left,
        // which is what makes it read as barred rather than merely derelict.
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 1);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 2);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 5);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 6);
        SlumRoof.grille(b, 0, 0, LAST_X, FRONT, 1, 0.32F);

        b.column(7, 0, 0, EAVE, WALL);
        b.door(3, 0, FRONT, () -> Blocks.SPRUCE_DOOR);

        // ---- the first floor ------------------------------------------------
        b.fill(1, MID, 1, LAST_X - 1, MID, FRONT - 1, FLOOR);
        b.scatter(1, MID, 1, LAST_X - 1, MID, FRONT - 1, SLAB, 0.10F);

        // One shaft, ground to roof, hung off the back wall. It overwrites the
        // floor slab where it passes through it, which IS the stairwell - there
        // is no separate hole to carve except the one in the roof.
        b.column(7, 1, 0, EAVE, LADDER_S);
        b.carve(7, ROOF, 1, 7, ROOF, 1);

        // ---- ground floor ---------------------------------------------------
        b.set(2, 0, 1, STASH_BOX);
        b.set(3, 0, 1, STASH_BOX);
        b.set(5, 0, 1, CHEST);
        b.set(6, 0, 1, BARREL);
        b.set(1, 0, 4, RECLINER);
        b.set(2, 0, 4, RECLINER);
        b.set(1, 0, 2, TELEVISION);
        b.set(4, -1, 5, CARPET);
        b.set(5, -1, 5, CARPET);
        b.set(3, 0, 3, TORCH);
        b.set(6, 0, 4, PIPE);
        b.set(4, 0, 1, KRAVE_BLOCK);

        // ---- first floor ----------------------------------------------------
        b.bed(1, MID + 1, 3, () -> Blocks.GRAY_BED, Direction.NORTH);
        b.bed(3, MID + 1, 3, () -> Blocks.ORANGE_BED, Direction.NORTH);
        b.bed(5, MID + 1, 3, () -> Blocks.BROWN_BED, Direction.NORTH);
        b.set(1, MID + 1, 5, CHEST);
        b.set(6, MID + 1, 5, BARREL);
        // The floor under the lantern is pinned to a full block first: the slab
        // scatter above runs over this square too, and a lantern on a bottom
        // slab fails canSupportCenter and pops off on the settle pass.
        b.set(3, MID, 5, FLOOR);
        b.set(3, MID + 1, 5, LANTERN);
        b.set(5, MID, 1, CARPET);

        // ---- roofline -------------------------------------------------------
        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.clutter(b, 2, 2, ROOF + 1);
        SlumRoof.awning(b, 2, 4, FRONT + 1, ROOF - 3, ROOF_S);

        b.marker("staff0", 4, 0, 4);
        b.marker("staff1", 4, MID + 1, 4);
        b.marker("staff2", 2, MID + 1, 2);

        b.core(3, -1, FRONT);
        return b.build();
    }
}
