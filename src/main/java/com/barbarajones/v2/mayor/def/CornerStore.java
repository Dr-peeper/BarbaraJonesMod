package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 2: the Corner Store. A shop with a counter, a back room full of stock,
 * two people behind it and a security grille that goes down at closing time and
 * never quite comes back up.
 *
 * <p><b>Footprint</b> 11 x 8. The shop is 9 x 7 of that; the outer column on
 * each side is a lean-to hanging over the alley, which is what makes two of
 * these built next to each other read as a terrace with a gap rather than as two
 * separate sheds. <b>Shopfront</b> the whole front wall, and the one wall in
 * this module that gets three courses of window scatter rather than one - so it
 * comes out as a plate front that has been boarded up a little more every year,
 * from the top down.
 *
 * <p><b>What it gives the village</b>: two Grocers on the payroll, six barrels
 * and two chests of storage the player can use, and a two-block stack of
 * compressed Krave in the back which is the single largest production block in
 * {@code VillageBuffs}'s table. This is the first building Barbara puts up that
 * pays for itself.
 */
final class CornerStore {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_corner_store");

    private CornerStore() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(75);

        final int LAST_X = 8;
        final int FRONT = 6;
        final int EAVE = 3;
        final int ROOF = EAVE + 1;
        final int COUNTER_Z = 4;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);

        // The sides and back get the usual treatment; the shopfront gets three
        // courses of it, which is what turns a plate-glass front into a plate
        // front that has been boarded up a bit more every year.
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 1);
        SlumRoof.windowRun(b, 1, FRONT, LAST_X - 1, FRONT, 2);
        SlumRoof.windowRun(b, 1, FRONT, LAST_X - 1, FRONT, 3);
        b.fill(1, 1, FRONT, 3, 1, FRONT, BARS);
        b.fill(5, 1, FRONT, LAST_X - 1, 1, FRONT, BARS);

        // The ladder shaft's wall, restored after the scatter that is allowed to
        // knock holes in it, and the door, which must not be boarded over.
        b.column(7, 0, 0, EAVE, WALL);
        b.door(4, 0, FRONT, () -> Blocks.SPRUCE_DOOR);

        // The counter, with the gap the staff get behind it through.
        b.fill(1, 0, COUNTER_Z, LAST_X - 1, 0, COUNTER_Z, SLAB);
        b.carve(6, 0, COUNTER_Z, 6, 0, COUNTER_Z);

        // Back room: shelving, the till, and the Krave.
        b.set(1, 0, 1, BARREL);
        b.set(2, 0, 1, BARREL);
        b.set(3, 0, 1, CHEST);
        b.set(5, 0, 1, CHEST);
        b.set(6, 0, 1, BARREL);
        b.column(4, 1, 0, 1, KRAVE_BLOCK);
        b.set(1, 0, 3, BARREL);
        b.set(7, 0, 3, BARREL);
        b.set(2, 0, 3, SMOKER);
        b.set(6, 0, 3, COMPOSTER);
        b.set(3, 0, COUNTER_Z - 1, TORCH);
        b.set(5, 0, COUNTER_Z - 1, TORCH);
        b.set(2, -1, 5, CARPET);
        b.set(3, -1, 5, CARPET);
        b.set(6, 0, 5, HAY);

        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.clutter(b, 1, 1, ROOF + 1);
        SlumRoof.awning(b, 1, LAST_X - 1, FRONT + 1, ROOF - 1, ROOF_S);

        // Lean-tos down both side alleys, each sloping away from the building
        // it is nailed to. They are what makes two of these built next to each
        // other read as a terrace with a gap rather than two separate sheds.
        b.fill(LAST_X + 1, ROOF - 1, 1, LAST_X + 1, ROOF - 1, FRONT - 1, ROOF_E);
        b.fill(-1, ROOF - 1, 1, -1, ROOF - 1, FRONT - 1, ROOF_W);

        b.column(7, 1, 0, EAVE, LADDER_S);
        b.carve(7, ROOF, 1, 7, ROOF, 1);

        b.marker("staff0", 4, 0, 3);
        b.marker("staff1", 2, 0, 2);

        b.core(4, -1, FRONT);
        return b.build();
    }
}
