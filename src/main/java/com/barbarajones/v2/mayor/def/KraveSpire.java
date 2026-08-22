package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 6: the Krave Spire. Sixteen courses of the same shed, three times, each
 * one smaller than the last and set back from it, with a fourth shed built on
 * the terrace that made, and a bell on top of the lot.
 *
 * <p>It is the tallest thing in the village and it is still a shanty. That is
 * the whole brief for the top of this ladder: the capital-tier building must not
 * be a reward for tidiness. There is no new material at this rung, no stone
 * facade, no lit avenue - the Spire is exactly what the Krave Shack is, stacked
 * until it is visible from the treeline.
 *
 * <p><b>Structure</b>, bottom to top:
 * <ul>
 *   <li>{@code y 0..3} - the granary. Nine by nine, four blocks of compressed
 *       Krave in it, and the only door.
 *   <li>{@code y 4} - the granary's roof, which is the first-floor terrace.
 *   <li>{@code y 5..8} - the living floor, seven by seven, set back two courses
 *       from the front - plus a rooftop shack on the terrace it left, sharing
 *       that front wall with a hole knocked through it and hanging its own front
 *       course out past the granary on three stair brackets.
 *   <li>{@code y 10..13} - the bell room, five by five.
 *   <li>{@code y 15} - the bell.
 * </ul>
 *
 * <p>One ladder shaft runs the whole height at {@code x = 4, z = 1}, hung off
 * the back wall - which is why every storey keeps its back wall on {@code z = 0}
 * and steps in from the front only. Move a storey sideways and the shaft loses
 * its wall four floors up.
 */
final class KraveSpire {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_krave_spire");

    private KraveSpire() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(240);

        // ---- the granary -----------------------------------------------------
        b.fill(0, -1, 0, 8, -1, 8, FLOOR);
        b.walls(0, 0, 0, 8, 3, 8, RUBBLE);
        b.column(0, 0, 0, 3, POST);
        b.column(8, 0, 0, 3, POST);
        b.column(0, 8, 0, 3, POST);
        b.column(8, 8, 0, 3, POST);
        SlumRoof.windows(b, 0, 0, 8, 8, 1);
        SlumRoof.windows(b, 0, 0, 8, 8, 2);
        b.door(4, 0, 8, () -> Blocks.SPRUCE_DOOR);

        b.column(2, 1, 0, 1, KRAVE_BLOCK);
        b.column(6, 1, 0, 1, KRAVE_BLOCK);
        b.set(1, 0, 1, BARREL);
        b.set(7, 0, 1, BARREL);
        b.set(1, 0, 7, CHEST);
        b.set(7, 0, 7, CHEST);
        b.set(3, 0, 6, HAY);
        b.set(5, 0, 6, HAY);
        b.set(2, 2, 1, LANTERN);
        b.set(4, 0, 6, PIPE);

        SlumRoof.deck(b, 0, 0, 8, 8, 4);
        SlumRoof.parapet(b, 0, 0, 8, 8, 5);

        // ---- the living floor, set back from the front ----------------------
        b.walls(1, 5, 0, 7, 8, 6, WALL);
        b.column(1, 0, 5, 8, POST);
        b.column(7, 0, 5, 8, POST);
        SlumRoof.windows(b, 1, 0, 7, 6, 6);
        SlumRoof.windows(b, 1, 0, 7, 6, 7);
        b.bed(2, 5, 4, () -> Blocks.BROWN_BED, Direction.NORTH);
        b.set(6, 5, 1, RECLINER);
        b.set(5, 5, 1, TELEVISION);
        b.set(6, 5, 5, BARREL);
        b.set(2, 5, 1, STASH_BOX);
        b.set(4, 4, 5, FLOOR);
        b.set(4, 5, 5, LANTERN);
        b.set(3, 4, 3, CARPET);
        SlumRoof.deck(b, 1, 0, 7, 6, 9);
        SlumRoof.parapet(b, 1, 0, 7, 6, 10);

        // ---- the shack somebody put on the terrace ---------------------------
        // Its back wall IS the living floor's front wall - one course of blocks
        // doing two jobs, because nobody was going to build a second one. A hole
        // was knocked through and that was that. Its front course hangs a block
        // past the granary below it on three stair brackets, which is the only
        // structure holding it up and is not enough.
        b.fill(2, 4, 6, 6, 4, 9, FLOOR);
        b.fill(3, 3, 9, 5, 3, 9, ROOF_S);
        b.walls(2, 5, 6, 6, 6, 9, WALL);
        // The granary's roof kerb runs straight through where this room's floor
        // now is, so the room is hollowed out after the walls go up rather than
        // being assumed empty. Building on somebody else's parapet is exactly
        // the sort of thing that happens here, and it has to be dealt with.
        b.carve(3, 5, 7, 5, 6, 8);
        SlumRoof.windows(b, 2, 6, 6, 9, 6);
        b.carve(4, 5, 6, 4, 6, 6);
        b.bed(3, 5, 8, () -> Blocks.WHITE_BED, Direction.NORTH);
        b.set(5, 5, 8, BARREL);
        // Roofed over its own courses only, so the wall it shares with the
        // storey behind it keeps its full height.
        SlumRoof.deck(b, 2, 7, 6, 9, 7);
        b.set(6, 8, 9, FENCE);

        // ---- the bell room ----------------------------------------------------
        b.walls(2, 10, 0, 6, 13, 4, WALL);
        b.column(2, 0, 10, 13, POST);
        b.column(6, 0, 10, 13, POST);
        SlumRoof.windows(b, 2, 0, 6, 4, 11);
        SlumRoof.windows(b, 2, 0, 6, 4, 12);
        b.set(3, 10, 3, BARREL);
        b.set(5, 10, 1, CHEST);
        b.set(5, 9, 3, FLOOR);
        b.set(5, 10, 3, LANTERN);
        SlumRoof.deck(b, 2, 0, 6, 4, 14);
        SlumRoof.parapet(b, 2, 0, 6, 4, 15);
        b.set(4, 15, 2, BELL);
        // Pinned to a full block, because the deck's slab scatter runs over this
        // square too and a torch on a bottom slab has nothing to stand on.
        b.set(3, 14, 3, DECK);
        b.set(3, 15, 3, TORCH);

        // ---- the shaft --------------------------------------------------------
        // Pinned last, over every scatter, because a single missing block in this
        // column is a ladder that stops working forty feet up.
        b.column(4, 0, 0, 13, WALL);
        b.column(4, 1, 0, 13, LADDER_S);
        b.carve(4, 14, 1, 4, 14, 1);

        b.marker("staff0", 5, 0, 4);
        b.marker("staff1", 4, 5, 3);

        b.core(4, -1, 8);
        return b.build();
    }
}
