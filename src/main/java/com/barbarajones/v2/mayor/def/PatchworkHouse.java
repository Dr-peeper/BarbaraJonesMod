package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 1: the Patchwork House. A shack that has been lived in long enough to
 * have been repaired several times by several people who did not consult one
 * another, and to have had a lean-to stuck on the side of it.
 *
 * <p><b>Footprint</b> 11 x 8. Only the left 7 x 7 of that is the house; three
 * columns to the right are the annexe, whose roof sits a course lower than the
 * main deck so that it reads as having arrived later, and the last column is a
 * lean-to hanging off the annexe. That offset is the entire trick, and it is
 * used again at every rung above this one: nothing in this village is ever built
 * all at once, so nothing in it should ever line up.
 *
 * <p><b>Sleeps</b> two, in two beds that do not match. <b>Village
 * contribution</b>: two beds, a door, a crafting table, a chest, a barrel and
 * two lights, all scored automatically by {@code VillageBuffs}.
 */
final class PatchworkHouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_patchwork_house");

    private PatchworkHouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(60);

        final int LAST_X = 6;       // the house proper; 7 and 8 are the annexe
        final int FRONT = 6;
        final int EAVE = 3;
        final int ROOF = EAVE + 1;

        // ---- the house ------------------------------------------------------
        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 1);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 2);

        // Restored after the scatter: the ladder needs a wall behind it for its
        // whole run, and the doorway needs a lintel rather than a hole.
        b.column(1, 0, 0, EAVE, WALL);
        b.door(3, 0, FRONT, () -> Blocks.OAK_DOOR);

        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.clutter(b, 1, 1, ROOF + 1);
        SlumRoof.awning(b, 2, 4, FRONT + 1, ROOF - 1, ROOF_S);

        b.column(1, 1, 0, EAVE, LADDER_S);
        b.carve(1, ROOF, 1, 1, ROOF, 1);

        // ---- furnishing -----------------------------------------------------
        b.bed(1, 0, 3, () -> Blocks.BROWN_BED, Direction.NORTH);
        b.bed(5, 0, 3, () -> Blocks.LIGHT_GRAY_BED, Direction.NORTH);
        b.set(3, 0, 1, CRAFTING);
        b.set(4, 0, 1, CHEST);
        b.set(2, 0, 1, BARREL);
        b.set(5, 0, 5, RECLINER);
        b.set(3, 0, 5, TELEVISION);
        b.set(1, 0, 5, LANTERN);
        b.set(5, 0, 1, TORCH);
        b.set(2, -1, 4, CARPET);
        b.set(3, -1, 4, CARPET);

        // ---- the annexe somebody stuck on the side --------------------------
        // A course shorter than the house and hard up against it. Note that it
        // has only three walls of its own: the house's east wall is the annexe's
        // west wall. Nobody rebuilt it, they leaned on it. Drawn as three
        // explicit runs rather than with walls(...), because a shell over a
        // three-wide box would put a wall down the middle of a two-wide room.
        final int ANNEX_LO = LAST_X + 1;
        final int ANNEX_HI = LAST_X + 3;
        b.fill(ANNEX_LO, -1, 2, ANNEX_HI, -1, 5, FLOOR);
        b.fill(ANNEX_HI, 0, 2, ANNEX_HI, 2, 5, WALL);
        b.fill(ANNEX_LO, 0, 2, ANNEX_HI, 2, 2, WALL);
        b.fill(ANNEX_LO, 0, 5, ANNEX_HI, 2, 5, WALL);
        b.column(ANNEX_HI, 2, 0, 2, POST);
        b.column(ANNEX_HI, 5, 0, 2, POST);
        SlumRoof.windowRun(b, ANNEX_HI, 2, ANNEX_HI, 5, 1);
        // Its opening onto the street. There has never been a door in it.
        b.carve(ANNEX_LO, 0, 5, ANNEX_LO + 1, 1, 5);
        SlumRoof.deck(b, ANNEX_LO, 2, ANNEX_HI, 5, 3);
        // A lean-to course off the annexe's own outer wall, sloping the other
        // way again. Three roof pitches on one building, none of them agreeing.
        b.fill(ANNEX_HI + 1, 3, 2, ANNEX_HI + 1, 3, 5, ROOF_E);
        b.set(ANNEX_HI, 4, 3, FENCE);
        b.set(ANNEX_LO, 0, 3, BARREL);
        b.set(ANNEX_LO, 0, 4, HAY);
        b.set(ANNEX_LO + 1, 0, 4, COMPOSTER);

        b.marker("staff0", 3, 0, 3);
        b.marker("staff1", 4, 0, 4);

        b.core(3, -1, FRONT);
        return b.build();
    }
}
