package com.barbarajones.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Shared placement helpers for the overworld set-dressing features in this
 * package (Barbara's lawn, the sewer entrance, the burnt patch, the abandoned
 * car, the cereal shrine).
 *
 * <p>Every one of those features is "a small tableau dropped onto flat ground",
 * so they all need the same three things: a site test that refuses cliffs,
 * water and overhangs; a block setter that knows the difference between
 * structure and decoration; and a loot barrel. Keeping them here means a fix to
 * the site test fixes all five at once.
 */
public final class WorldDressing {

    /**
     * Flag set for load-bearing blocks - matches what vanilla features use.
     */
    public static final int STRUCTURE_FLAGS = 3;

    /**
     * Flag set for anything that hangs off another block (ladders, trapdoors,
     * flower pots, tall grass). Placed without the neighbour notification so a
     * half-built structure can never knock its own decoration back off.
     */
    public static final int ATTACHMENT_FLAGS = 2;

    private WorldDressing() { }

    /**
     * True when {@code pos} is an open block sitting directly on dry solid
     * ground - i.e. somewhere you could stand. Rejects oceans, rivers and
     * lava lakes, since these features are placed from a WORLD_SURFACE_WG
     * heightmap that happily lands on top of water.
     */
    public static boolean standable(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty()
                && level.getFluidState(pos.below()).isEmpty()
                && level.getBlockState(pos.below()).isSolid()
                && !level.getBlockState(pos).isSolid();
    }

    /**
     * True when the whole square footprint of radius {@code radius} around
     * {@code origin} sits within {@code slack} blocks of the origin's height.
     * Without this a lawn chair ends up embedded in a hillside, or a car half
     * floats off a ledge.
     */
    public static boolean flatEnough(WorldGenLevel level, BlockPos origin, int radius, int slack) {
        int base = origin.getY();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int here = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
                        origin.getX() + dx, origin.getZ() + dz);
                if (Math.abs(here - base) > slack) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Place a load-bearing block. */
    public static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, STRUCTURE_FLAGS);
    }

    /** Place a block that leans on a neighbour. */
    public static void attach(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, ATTACHMENT_FLAGS);
    }

    /**
     * Strip whatever is growing in the {@code height} blocks above {@code pos}
     * so a structure does not generate with flowers and grass stabbing through
     * its roof. Stops at the first solid block: if there is real terrain
     * overhead this is not a site we should be flattening.
     */
    public static void clearAbove(WorldGenLevel level, BlockPos pos, int height) {
        for (int dy = 0; dy < height; dy++) {
            BlockPos above = pos.above(dy);
            BlockState state = level.getBlockState(above);
            if (state.isSolid()) {
                return;
            }
            if (!state.isAir()) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), STRUCTURE_FLAGS);
            }
        }
    }

    /**
     * Plant a two-block-tall grass clump, but only where there is head room -
     * a lower half with no upper half is an invalid state that pops the moment
     * anything updates it.
     */
    public static void tallGrass(WorldGenLevel level, BlockPos pos) {
        if (!level.getBlockState(pos.above()).isAir() || !level.getBlockState(pos).isAir()) {
            return;
        }
        BlockState grass = Blocks.TALL_GRASS.defaultBlockState();
        attach(level, pos, grass.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        attach(level, pos.above(), grass.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /**
     * Drop a barrel and rummage the given stacks into random slots. Scattered
     * rather than packed from slot 0 so it reads as somebody's abandoned stash
     * instead of a neatly generated chest. Empty stacks are skipped, which lets
     * callers write {@code random.nextBoolean() ? stack : ItemStack.EMPTY}
     * inline for "sometimes" loot.
     */
    public static void barrel(WorldGenLevel level, BlockPos pos, RandomSource random, ItemStack... loot) {
        set(level, pos, Blocks.BARREL.defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return;
        }
        for (ItemStack stack : loot) {
            if (stack.isEmpty()) {
                continue;
            }
            for (int attempt = 0; attempt < 8; attempt++) {
                int slot = random.nextInt(barrel.getContainerSize());
                if (barrel.getItem(slot).isEmpty()) {
                    barrel.setItem(slot, stack);
                    break;
                }
            }
        }
        barrel.setChanged();
    }

    /**
     * A weathered stone-brick variant. Mixing these three by hand is the
     * cheapest way to stop a hand-built structure looking machine-stamped.
     */
    public static BlockState oldBrick(RandomSource random) {
        int roll = random.nextInt(10);
        if (roll < 4) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        if (roll < 7) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }
}
