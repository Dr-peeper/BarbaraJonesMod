package com.barbarajones.world;

import com.barbarajones.content.ModItems;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Somebody's car, left where it died. Five blocks long, wheels at the corners,
 * a glass cabin with at least one window put through, one headlight still in
 * and one gone, and a barrel in the boot holding whatever they could not carry.
 *
 * <p>Duhl Wol pulls up in a working one; this is the other outcome. It is the
 * only piece of set dressing in the pack that stands taller than a player,
 * which is the point - it is the thing you spot from a distance and walk over
 * to.
 */
public class AbandonedCarFeature extends Feature<NoneFeatureConfiguration> {

    /** Length along the car's forward axis, in blocks. */
    private static final int LENGTH = 5;

    private static final Block[] PAINT = {
        Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE,
        Blocks.BLACK_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.CYAN_CONCRETE
    };

    public AbandonedCarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!WorldDressing.standable(level, origin) || !WorldDressing.flatEnough(level, origin, 4, 1)) {
            return false;
        }

        Direction forward = Direction.from2DDataValue(random.nextInt(4));
        Direction side = forward.getClockWise();
        BlockState paint = PAINT[random.nextInt(PAINT.length)].defaultBlockState();
        BlockState wheel = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();

        // the whole footprint has to be standable, otherwise half the car ends
        // up buried and the other half floating
        for (int i = 0; i < LENGTH; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!WorldDressing.standable(level, at(origin, forward, side, i, 0, j))) {
                    return false;
                }
            }
        }

        for (int i = 0; i < LENGTH; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos ground = at(origin, forward, side, i, 0, j);
                WorldDressing.clearAbove(level, ground, 4);
                WorldDressing.set(level, ground.below(), Blocks.COARSE_DIRT.defaultBlockState());
                // wheels at the four corners, open air under the middle
                boolean corner = (i == 0 || i == LENGTH - 1) && j != 0;
                WorldDressing.set(level, ground,
                        corner ? wheel : Blocks.AIR.defaultBlockState());
            }
        }

        // chassis: one solid slab of bodywork
        for (int i = 0; i < LENGTH; i++) {
            for (int j = -1; j <= 1; j++) {
                WorldDressing.set(level, at(origin, forward, side, i, 1, j), paint);
            }
        }

        // headlights at the front corners - one of the pair is always out,
        // because a car nobody has come back for does not have two good bulbs
        boolean leftLamp = random.nextBoolean();
        WorldDressing.set(level, at(origin, forward, side, LENGTH - 1, 1, -1),
                leftLamp ? Blocks.GLOWSTONE.defaultBlockState() : paint);
        WorldDressing.set(level, at(origin, forward, side, LENGTH - 1, 1, 1),
                leftLamp ? paint : Blocks.GLOWSTONE.defaultBlockState());

        // cabin: pillars at the corners, glass everywhere else, hollow inside
        for (int i = 1; i <= 3; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = at(origin, forward, side, i, 2, j);
                boolean interior = i == 2 && j == 0;
                boolean pillar = (i == 1 || i == 3) && j != 0;
                if (interior) {
                    WorldDressing.set(level, pos, Blocks.AIR.defaultBlockState());
                } else if (pillar) {
                    WorldDressing.set(level, pos, paint);
                } else {
                    // one window in three is already gone
                    WorldDressing.set(level, pos,
                            random.nextInt(3) == 0 ? Blocks.AIR.defaultBlockState() : glass);
                }
            }
        }

        // roof
        for (int i = 1; i <= 3; i++) {
            for (int j = -1; j <= 1; j++) {
                WorldDressing.set(level, at(origin, forward, side, i, 3, j), paint);
            }
        }

        // a web strung across the cabin, so it reads as long abandoned
        WorldDressing.attach(level, at(origin, forward, side, 2, 2, 0),
                Blocks.COBWEB.defaultBlockState());

        // the boot, cut into the back of the bodywork
        WorldDressing.barrel(level, at(origin, forward, side, 0, 1, 0), random,
                new ItemStack(ModItems.DOLLARS.get(), 4 + random.nextInt(20)),
                new ItemStack(ModItems.CHICKEN_NUGGETS.get(), 1 + random.nextInt(4)),
                new ItemStack(ModItems.MR_PIBB.get(), 1 + random.nextInt(3)),
                random.nextBoolean() ? new ItemStack(ModItems.DONUT.get(), 1 + random.nextInt(3)) : ItemStack.EMPTY,
                random.nextInt(4) == 0 ? new ItemStack(ModItems.CHILD_SUPPORT_PAPERS.get()) : ItemStack.EMPTY,
                random.nextInt(4) == 0 ? new ItemStack(ModItems.KRAVE_BOX.get()) : ItemStack.EMPTY);
        return true;
    }

    /** Car-local coordinates: {@code i} along the bonnet, {@code j} across it. */
    private BlockPos at(BlockPos origin, Direction forward, Direction side, int i, int up, int j) {
        return origin.relative(forward, i).relative(side, j).above(up);
    }
}
