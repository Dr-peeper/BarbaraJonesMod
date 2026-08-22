package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 4: the Stacked Tenement. Three floors, six beds, one ladder, and washing
 * hung out over the parapet because there is nowhere else to hang it.
 *
 * <p>This is the building that does the most work for the "huge and still
 * run-down" requirement. It is twelve courses tall - taller than anything in the
 * housing module - and it achieves that with no new idea at all: the same walls,
 * the same flat deck, the same scatter, just three times over. A tall building
 * made of one repeated cheap idea is what a tenement <em>is</em>, and it is why
 * a street of these reads as a slum rather than as a skyline.
 *
 * <p><b>Footprint</b> 9 x 10 including the porch canopy. <b>Floors</b> at
 * y = 3 and y = 7, ceiling deck at y = 11. <b>Sleeps</b> six.
 *
 * <p><b>Village contribution</b>: six beds is the single largest attraction
 * block in the module - 36 attraction and 12 building score from the bedding
 * alone - which is what pushes a settlement over the VILLAGE and TOWN rungs of
 * {@code VillageTier}. The mayor will not put one up unless the population cap
 * has room for all six residents; see {@code KraveMayor}'s housing gate.
 */
final class StackedTenement {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_stacked_tenement");

    private StackedTenement() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(180);

        final int LAST_X = 8;
        final int FRONT = 8;
        final int FLOOR_1 = 3;
        final int FLOOR_2 = 7;
        final int EAVE = 10;
        final int ROOF = EAVE + 1;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);

        // Every habitable course gets its own scatter, so the three floors do
        // not come out as three copies of one elevation.
        for (int y : new int[]{1, 2, 5, 6, 9, 10}) {
            SlumRoof.windows(b, 0, 0, LAST_X, FRONT, y);
        }
        SlumRoof.grille(b, 0, 0, LAST_X, FRONT, 1, 0.26F);

        b.column(7, 0, 0, EAVE, WALL);
        b.door(4, 0, FRONT, () -> Blocks.OAK_DOOR);

        // ---- floors ----------------------------------------------------------
        floorSlab(b, FLOOR_1, LAST_X, FRONT);
        floorSlab(b, FLOOR_2, LAST_X, FRONT);

        // One shaft the full height of the building, hung off the back wall. It
        // punches its own holes through both floors on the way up, so there is
        // nothing to carve except the roof hatch.
        b.column(7, 1, 0, EAVE, LADDER_S);
        b.carve(7, ROOF, 1, 7, ROOF, 1);

        // ---- ground: the bit everyone shares ---------------------------------
        b.set(1, 0, 1, RECLINER);
        b.set(2, 0, 1, RECLINER);
        b.set(4, 0, 1, TELEVISION);
        b.set(5, 0, 1, STASH_BOX);
        b.set(1, 0, 6, CHEST);
        b.set(2, 0, 6, BARREL);
        b.set(3, -1, 3, CARPET);
        b.set(4, -1, 3, CARPET);
        b.set(4, -1, 4, CARPET);
        b.set(5, 0, 6, TORCH);
        b.set(3, 0, 6, PIPE);
        b.column(6, 3, 0, 1, KRAVE_BLOCK);

        // ---- two identical flats, which is the point -------------------------
        flat(b, FLOOR_1 + 1, () -> Blocks.BROWN_BED, () -> Blocks.GRAY_BED);
        flat(b, FLOOR_2 + 1, () -> Blocks.LIGHT_GRAY_BED, () -> Blocks.RED_BED);
        b.bed(3, FLOOR_1 + 1, 6, () -> Blocks.ORANGE_BED, Direction.NORTH);
        b.bed(3, FLOOR_2 + 1, 6, () -> Blocks.WHITE_BED, Direction.NORTH);

        // ---- roofline ---------------------------------------------------------
        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.clutter(b, 3, 3, ROOF + 1);
        SlumRoof.awning(b, 3, 5, FRONT + 1, ROOF - 7, ROOF_S);

        // Washing, strung between two posts on the roof. Wool blocks rather than
        // anything cleverer, because the only string-like block in the game
        // needs a redstone hook at each end and would tick forever.
        b.column(1, 1, ROOF + 1, ROOF + 2, FENCE);
        b.column(5, 1, ROOF + 1, ROOF + 2, FENCE);
        b.fill(2, ROOF + 2, 1, 4, ROOF + 2, 1, TARP);

        b.marker("staff0", 4, 0, 5);
        b.marker("staff1", 4, FLOOR_1 + 1, 4);
        b.marker("staff2", 2, FLOOR_1 + 1, 4);
        b.marker("staff3", 4, FLOOR_2 + 1, 4);
        b.marker("staff4", 2, FLOOR_2 + 1, 4);
        b.marker("staff5", 5, FLOOR_2 + 1, 5);

        b.core(4, -1, FRONT);
        return b.build();
    }

    /** One structural floor slab, with a scatter of half-laid boards in it. */
    private static void floorSlab(StructureDef.Builder b, int y, int lastX, int front) {
        b.fill(1, y, 1, lastX - 1, y, front - 1, FLOOR);
        b.scatter(1, y, 1, lastX - 1, y, front - 1, SLAB, 0.09F);
    }

    /**
     * One flat's worth of fittings, laid out identically on both upper floors -
     * two beds against the back wall, storage under the window, a light on a
     * pinned full block so it cannot land on a slab and pop off.
     */
    private static void flat(StructureDef.Builder b, int y,
                             java.util.function.Supplier<net.minecraft.world.level.block.Block> bedA,
                             java.util.function.Supplier<net.minecraft.world.level.block.Block> bedB) {
        b.bed(1, y, 3, bedA, Direction.NORTH);
        b.bed(6, y, 3, bedB, Direction.NORTH);
        b.set(1, y, 6, CHEST);
        b.set(6, y, 6, BARREL);
        b.set(4, y - 1, 6, FLOOR);
        b.set(4, y, 6, LANTERN);
        b.set(3, y - 1, 4, CARPET);
        b.set(5, y, 1, STASH_BOX);
    }
}
