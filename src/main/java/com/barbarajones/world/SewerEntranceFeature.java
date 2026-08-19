package com.barbarajones.world;

import com.barbarajones.content.ModItems;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A manhole in the middle of nowhere: a paved apron, an iron cover you can
 * flip open, a brick-lined shaft with a ladder, and at the bottom a stash room
 * with a running gutter down one side.
 *
 * <p>This is where The Plug keeps his product, so the barrels down there hold
 * the fake stuff and the cash he took for it. The room is deliberately tiny -
 * five by three - because the joke is that a grown man is running a business
 * out of a hole.
 */
public class SewerEntranceFeature extends Feature<NoneFeatureConfiguration> {

    /** Interior spans x in [-2, 2], z in [0, 2]; the shell sits one block outside that. */
    private static final int INTERIOR_HALF_X = 2;
    private static final int INTERIOR_MAX_Z = 2;
    private static final int INTERIOR_HEIGHT = 3;
    private static final int MIN_DROP = 9;

    public SewerEntranceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!WorldDressing.standable(level, origin) || !WorldDressing.flatEnough(level, origin, 2, 1)) {
            return false;
        }

        int floorY = origin.getY() - (MIN_DROP + random.nextInt(5));
        // the gutter sits one layer under the floor, so we need two spare
        // layers of rock beneath it - bail rather than punch through into the
        // void on a world with a shallow surface
        if (floorY - 2 <= level.getMinBuildHeight() + 4) {
            return false;
        }

        // Order matters: the room has to be hollowed out before the shaft is
        // dug, because the ladder runs all the way down into the room and the
        // room's interior fill would otherwise wipe the bottom rungs.
        pave(level, origin, random);
        buildRoom(level, origin, floorY, random);
        digShaft(level, origin, floorY, random);
        furnish(level, origin, floorY, random);
        return true;
    }

    /** The street-level apron and the cover over the hole. */
    private void pave(WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                WorldDressing.clearAbove(level, pos, 3);
                if (dx == 0 && dz == 0) {
                    continue;   // the hole itself
                }
                WorldDressing.set(level, pos.below(), WorldDressing.oldBrick(random));
            }
        }
        // A closed bottom-half iron trapdoor is a flat grid plate sitting flush
        // with the paving - the closest vanilla gets to a manhole cover, and it
        // still opens when a player wants down.
        WorldDressing.attach(level, origin, Blocks.IRON_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, Boolean.FALSE));
    }

    /**
     * The vertical run from just under the cover down to the room ceiling,
     * lined on all four sides so a cave never opens into it, with a ladder on
     * the north wall.
     */
    private void digShaft(WorldGenLevel level, BlockPos origin, int floorY, RandomSource random) {
        int ceilingY = floorY + INTERIOR_HEIGHT;
        for (int y = origin.getY() - 1; y >= ceilingY; y--) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            WorldDressing.set(level, pos, Blocks.CAVE_AIR.defaultBlockState());
            for (Direction side : Direction.Plane.HORIZONTAL) {
                WorldDressing.set(level, pos.relative(side), WorldDressing.oldBrick(random));
            }
        }
        // Ladders survive on the block at FACING.getOpposite(), so SOUTH-facing
        // rungs hang off the north wall the shaft lining just built.
        BlockState rung = Blocks.LADDER.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        for (int y = origin.getY() - 1; y >= floorY; y--) {
            WorldDressing.attach(level, new BlockPos(origin.getX(), y, origin.getZ()), rung);
        }
    }

    /** Hollow shell: floor, four walls, ceiling with a hole for the shaft. */
    private void buildRoom(WorldGenLevel level, BlockPos origin, int floorY, RandomSource random) {
        for (int dx = -INTERIOR_HALF_X - 1; dx <= INTERIOR_HALF_X + 1; dx++) {
            for (int dz = -1; dz <= INTERIOR_MAX_Z + 1; dz++) {
                for (int dy = -1; dy <= INTERIOR_HEIGHT; dy++) {
                    boolean shell = Math.abs(dx) > INTERIOR_HALF_X
                            || dz < 0 || dz > INTERIOR_MAX_Z
                            || dy < 0 || dy >= INTERIOR_HEIGHT;
                    BlockPos pos = new BlockPos(origin.getX() + dx, floorY + dy, origin.getZ() + dz);
                    if (shell) {
                        if (dy == INTERIOR_HEIGHT && dx == 0 && dz == 0) {
                            continue;   // leave the shaft mouth open
                        }
                        WorldDressing.set(level, pos, WorldDressing.oldBrick(random));
                    } else {
                        WorldDressing.set(level, pos, Blocks.CAVE_AIR.defaultBlockState());
                    }
                }
            }
        }
        // The gutter is water placed *in* the floor layer, not on top of it:
        // every neighbour at that height is brick, so it can never spread and
        // flood the room, and it still reads as ankle-deep sewer water.
        for (int dx = -INTERIOR_HALF_X; dx <= INTERIOR_HALF_X; dx++) {
            BlockPos gutter = new BlockPos(origin.getX() + dx, floorY - 1,
                    origin.getZ() + INTERIOR_MAX_Z);
            WorldDressing.set(level, gutter.below(), Blocks.STONE_BRICKS.defaultBlockState());
            WorldDressing.set(level, gutter, Blocks.WATER.defaultBlockState());
        }
    }

    /** The stash itself, plus the details that sell the room. */
    private void furnish(WorldGenLevel level, BlockPos origin, int floorY, RandomSource random) {
        BlockPos product = new BlockPos(origin.getX() - INTERIOR_HALF_X, floorY, origin.getZ() + 1);
        WorldDressing.barrel(level, product, random,
                new ItemStack(ModItems.FAKE_COCAINE.get(), 2 + random.nextInt(4)),
                new ItemStack(ModItems.FAKE_WEED.get(), 2 + random.nextInt(4)),
                new ItemStack(ModItems.FIVE_HUNDRED_DOLLARS.get()),
                new ItemStack(ModItems.DOLLARS.get(), 8 + random.nextInt(24)),
                random.nextInt(3) == 0 ? new ItemStack(ModItems.SKI_MASK.get()) : ItemStack.EMPTY);

        BlockPos leftovers = new BlockPos(origin.getX() + INTERIOR_HALF_X, floorY, origin.getZ() + 1);
        WorldDressing.barrel(level, leftovers, random,
                new ItemStack(ModItems.SEWER_GRATE.get()),
                new ItemStack(ModItems.KRAVE_CEREAL.get(), 2 + random.nextInt(4)),
                new ItemStack(ModItems.CHICKEN_NUGGETS.get(), 1 + random.nextInt(3)),
                random.nextBoolean() ? new ItemStack(ModItems.MR_PIBB.get()) : ItemStack.EMPTY);

        // one hanging lantern, off to the side of the ladder so it does not
        // block the climb
        WorldDressing.attach(level,
                new BlockPos(origin.getX(), floorY + INTERIOR_HEIGHT - 1, origin.getZ() + 1),
                Blocks.LANTERN.defaultBlockState()
                        .setValue(BlockStateProperties.HANGING, Boolean.TRUE));

        for (int i = 0; i < 4; i++) {
            BlockPos web = new BlockPos(
                    origin.getX() + random.nextInt(INTERIOR_HALF_X * 2 + 1) - INTERIOR_HALF_X,
                    floorY + random.nextInt(INTERIOR_HEIGHT),
                    origin.getZ() + random.nextInt(INTERIOR_MAX_Z + 1));
            if (level.getBlockState(web).isAir()) {
                WorldDressing.attach(level, web, Blocks.COBWEB.defaultBlockState());
            }
        }
    }
}
