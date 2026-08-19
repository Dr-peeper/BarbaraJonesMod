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
 * Barbara's lawn: a knee-deep patch of tall grass nobody has cut in months,
 * a folding chair facing out over it, an ashtray she keeps kicking over, and
 * a barrel of supplies stuffed behind the chair.
 *
 * <p>This is the harvesting spot as much as the set piece - the grass is the
 * whole reason she is out here, and the barrel gives a player who has not
 * met her yet a first handful of it plus the papers to roll it.
 */
public class BarbaraLawnFeature extends Feature<NoneFeatureConfiguration> {

    private static final int LAWN_RADIUS = 4;

    public BarbaraLawnFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!WorldDressing.standable(level, origin)
                || !WorldDressing.flatEnough(level, origin, LAWN_RADIUS, 2)) {
            return false;
        }

        // The direction the sitter looks. Stairs put their low step on the
        // FACING side and the tall back opposite, which is exactly a chair
        // back behind whoever is sitting in it.
        Direction facing = Direction.from2DDataValue(random.nextInt(4));
        Direction left = facing.getClockWise();
        Direction right = facing.getCounterClockWise();

        growLawn(level, origin, random);
        placeChair(level, origin, facing, left, right);

        // the ashtray, sitting in the trodden dirt just off the chair's front corner
        BlockPos ashtray = origin.relative(facing).relative(left);
        if (WorldDressing.standable(level, ashtray)) {
            WorldDressing.clearAbove(level, ashtray, 2);
            WorldDressing.set(level, ashtray.below(), Blocks.COARSE_DIRT.defaultBlockState());
            WorldDressing.attach(level, ashtray, Blocks.FLOWER_POT.defaultBlockState());
        }

        BlockPos stash = origin.relative(facing.getOpposite());
        if (WorldDressing.standable(level, stash)) {
            WorldDressing.clearAbove(level, stash, 2);
            stockStash(level, stash, random);
        }
        return true;
    }

    /** The overgrown part: a rough circle of tall grass with bald patches. */
    private void growLawn(WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (int dx = -LAWN_RADIUS; dx <= LAWN_RADIUS; dx++) {
            for (int dz = -LAWN_RADIUS; dz <= LAWN_RADIUS; dz++) {
                // the 3x3 around the origin is the chair, its armrests and the
                // stash - leave it walkable, she has to be able to sit down
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue;
                }
                if (dx * dx + dz * dz > LAWN_RADIUS * LAWN_RADIUS) {
                    continue;
                }
                BlockPos pos = origin.offset(dx, 0, dz);
                if (!WorldDressing.standable(level, pos)) {
                    continue;
                }
                float roll = random.nextFloat();
                if (roll < 0.42F) {
                    WorldDressing.tallGrass(level, pos);
                } else if (roll < 0.68F) {
                    WorldDressing.attach(level, pos, Blocks.GRASS.defaultBlockState());
                } else if (roll < 0.72F) {
                    WorldDressing.attach(level, pos, Blocks.DANDELION.defaultBlockState());
                } else if (roll < 0.76F) {
                    // a worn-out bald spot where the chair has been dragged around
                    WorldDressing.set(level, pos.below(), Blocks.COARSE_DIRT.defaultBlockState());
                }
            }
        }
    }

    private void placeChair(WorldGenLevel level, BlockPos seat, Direction facing,
                            Direction left, Direction right) {
        WorldDressing.clearAbove(level, seat, 3);
        WorldDressing.set(level, seat.below(), Blocks.COARSE_DIRT.defaultBlockState());
        WorldDressing.set(level, seat, Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM));
        armrest(level, seat.relative(left), left.getOpposite());
        armrest(level, seat.relative(right), right.getOpposite());
    }

    /**
     * An open trapdoor's panel stands against the block face named by FACING,
     * so aiming it back at the seat puts the plank right where an elbow goes.
     */
    private void armrest(WorldGenLevel level, BlockPos pos, Direction towardSeat) {
        if (!WorldDressing.standable(level, pos)) {
            return;
        }
        WorldDressing.clearAbove(level, pos, 2);
        WorldDressing.attach(level, pos, Blocks.SPRUCE_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, towardSeat)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, Boolean.TRUE));
    }

    private void stockStash(WorldGenLevel level, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.isSolid()) {
            return;
        }
        WorldDressing.barrel(level, pos, random,
                new ItemStack(ModItems.ASHTRAY.get()),
                new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), 3 + random.nextInt(5)),
                new ItemStack(ModItems.ROLLING_PAPER.get(), 2 + random.nextInt(4)),
                new ItemStack(ModItems.GRASS_SEEDS.get(), 2 + random.nextInt(6)),
                new ItemStack(ModItems.MR_PIBB.get(), 1 + random.nextInt(2)),
                random.nextBoolean()
                        ? new ItemStack(ModItems.ROLLED_JOINT.get(), 1 + random.nextInt(3))
                        : ItemStack.EMPTY,
                random.nextInt(3) == 0 ? new ItemStack(ModItems.LIGHTER.get()) : ItemStack.EMPTY);
    }
}
