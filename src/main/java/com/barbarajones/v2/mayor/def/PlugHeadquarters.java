package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 4: the Plug Headquarters. A grille-fronted block with two lookout stumps
 * on the street corners, a bell nobody rings for good reasons, and a back room
 * with more compressed Krave in it than the rest of the village put together.
 *
 * <p>This is the building that turns the village into an income. While one of
 * these stands in a settlement, the mayor accrues a cut every mayor tick and
 * hands it over in Dollars the next time the player asks her for the numbers -
 * see {@code MayorSettlement#accruePayout}. The building does not do that by
 * itself; the mayor counts completed Plug Headquarters and pays on the count, so
 * knocking one down stops the money.
 *
 * <p><b>Footprint</b> 11 x 11. <b>Walls</b> five courses, with the two front
 * corners carried up to eight as squat lookouts - the only vertical emphasis in
 * the module, and it is a pair of concrete stumps rather than towers.
 *
 * <p><b>Village contribution</b>: heavy fortification (iron bars are worth 1
 * defence each and there are a lot of them), a bell at 1 building / 2 defence /
 * 4 attraction / 3 happiness, four Krave blocks at 12 production apiece, and
 * three residents of whom two are Guards.
 */
final class PlugHeadquarters {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_plug_headquarters");

    private PlugHeadquarters() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(150);

        final int LAST_X = 10;
        final int FRONT = 10;
        final int EAVE = 4;
        final int ROOF = EAVE + 1;
        final int LOOKOUT = 8;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);

        // Masonry to head height, patchwork above it: the bottom of this
        // building has been kicked, and the top of it has not.
        b.walls(0, 0, 0, LAST_X, 1, FRONT, RUBBLE);
        b.walls(0, 2, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);

        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 2);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 3);
        SlumRoof.grille(b, 0, 0, LAST_X, FRONT, 2, 0.45F);
        SlumRoof.grille(b, 0, 0, LAST_X, FRONT, 3, 0.30F);

        b.column(9, 0, 0, EAVE, WALL);
        // A wooden door, not iron. An iron door has no handle and this building
        // has no redstone in it, so an iron one would seal the headquarters shut
        // against the player and against its own staff.
        b.door(5, 0, FRONT, () -> Blocks.DARK_OAK_DOOR);

        // ---- the yard, which is inside ---------------------------------------
        // Shag carpet goes in the floor course, not on top of it: it is a full
        // block in this mod rather than a thin carpet, so laid at standing height
        // it would be a knee-high plinth in the middle of the room.
        b.fill(4, -1, 4, 6, -1, 6, CARPET);
        b.set(5, 0, 5, CAMPFIRE);
        b.set(3, 0, 5, RECLINER);
        b.set(7, 0, 5, RECLINER);
        b.set(5, 0, 3, TELEVISION);
        b.set(2, 0, 8, BELL);
        b.set(8, 0, 8, PIPE);

        // ---- the back room ----------------------------------------------------
        b.fill(1, 0, 2, 1, 2, 2, RUBBLE);
        b.fill(9, 0, 2, 9, 2, 2, RUBBLE);
        b.fill(2, 0, 2, 8, 2, 2, BARS);
        b.carve(5, 0, 2, 5, 1, 2);
        b.column(3, 1, 0, 1, KRAVE_BLOCK);
        b.column(4, 1, 0, 1, KRAVE_BLOCK);
        b.set(6, 0, 1, CHEST);
        b.set(7, 0, 1, CHEST);
        b.set(8, 0, 1, BARREL);
        b.set(2, 0, 1, BARREL);
        b.set(5, 0, 1, STASH_BOX);
        // On top of the Krave stack, which is a full block. A lantern on a chest
        // fails canSupportCenter - a chest is fourteen sixteenths tall.
        b.set(4, 2, 1, LANTERN);

        // ---- roofline ---------------------------------------------------------
        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.clutter(b, 2, 2, ROOF + 1);
        SlumRoof.awning(b, 4, 6, FRONT + 1, ROOF - 1, ROOF_S);

        b.column(9, 1, 0, EAVE, LADDER_S);
        b.carve(9, ROOF, 1, 9, ROOF, 1);

        // The lookouts. Deliberately stumpy - three courses above the parapet,
        // open at the top, with a torch in each. A watchtower here would read as
        // a castle; this reads as somebody standing on a box.
        lookout(b, 0, FRONT, ROOF, LOOKOUT);
        lookout(b, LAST_X, FRONT, ROOF, LOOKOUT);

        b.marker("staff0", 5, 0, 7);
        b.marker("staff1", 3, 0, 7);
        b.marker("staff2", 7, 0, 7);

        b.core(5, -1, FRONT);
        return b.build();
    }

    /**
     * One corner stump: two columns of masonry carried up past the parapet, a
     * rail on the inner one and a torch on the top of the outer. Solid, not
     * hollow - there is nothing inside it and nobody stands in it. Somebody
     * stands ON it, which is the difference between this and a watchtower.
     */
    private static void lookout(StructureDef.Builder b, int x, int z, int from, int to) {
        b.column(x, z, from, to - 1, RUBBLE);
        b.column(x, z - 1, from, to - 2, RUBBLE);
        b.set(x, to, z, TORCH);
        b.set(x, to - 1, z - 1, FENCE);
    }
}
