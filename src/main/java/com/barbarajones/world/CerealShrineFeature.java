package com.barbarajones.world;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModItems;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * A cereal-box shrine: a black stone platform, three Krave blocks stacked into
 * a column at the middle of it, a lantern on top and purple candles burning at
 * the corners, with offerings left in barrels at the sides.
 *
 * <p>Somebody built this. That is the whole idea - the player is meant to find
 * it, work out that other people out here worship the box, and feel slightly
 * worse about how much Krave is already in their own inventory.
 */
public class CerealShrineFeature extends Feature<NoneFeatureConfiguration> {

    /** Platform is (2 * PLATFORM_HALF + 1) square. */
    private static final int PLATFORM_HALF = 2;

    public CerealShrineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!WorldDressing.standable(level, origin)
                || !WorldDressing.flatEnough(level, origin, PLATFORM_HALF, 1)) {
            return false;
        }

        BlockState plinth = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState corner = Blocks.PURPLE_CONCRETE.defaultBlockState();
        BlockState box = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();

        // validate the whole footprint before touching anything, so a bad site
        // leaves no half-built platform behind
        for (int dx = -PLATFORM_HALF; dx <= PLATFORM_HALF; dx++) {
            for (int dz = -PLATFORM_HALF; dz <= PLATFORM_HALF; dz++) {
                if (!WorldDressing.standable(level, origin.offset(dx, 0, dz))) {
                    return false;
                }
            }
        }

        for (int dx = -PLATFORM_HALF; dx <= PLATFORM_HALF; dx++) {
            for (int dz = -PLATFORM_HALF; dz <= PLATFORM_HALF; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                WorldDressing.clearAbove(level, pos, 5);
                boolean atCorner = Math.abs(dx) == PLATFORM_HALF && Math.abs(dz) == PLATFORM_HALF;
                WorldDressing.set(level, pos.below(), atCorner ? corner : plinth);
                WorldDressing.set(level, pos, Blocks.AIR.defaultBlockState());
            }
        }

        // three boxes stacked, lantern on top - the whole reason for the pile
        for (int dy = 0; dy < 3; dy++) {
            WorldDressing.set(level, origin.above(dy), box);
        }
        WorldDressing.attach(level, origin.above(3), Blocks.LANTERN.defaultBlockState()
                .setValue(BlockStateProperties.HANGING, Boolean.FALSE));

        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                BlockPos candle = origin.offset(sx * PLATFORM_HALF, 0, sz * PLATFORM_HALF);
                WorldDressing.attach(level, candle, Blocks.PURPLE_CANDLE.defaultBlockState()
                        .setValue(BlockStateProperties.LIT, Boolean.TRUE)
                        .setValue(BlockStateProperties.CANDLES, 1 + random.nextInt(3)));
            }
        }

        // a strip of carpet either side of the column, worn by whoever kneels here
        WorldDressing.attach(level, origin.offset(0, 0, PLATFORM_HALF),
                Blocks.PURPLE_CARPET.defaultBlockState());
        WorldDressing.attach(level, origin.offset(0, 0, -PLATFORM_HALF),
                Blocks.PURPLE_CARPET.defaultBlockState());

        offerings(level, origin.offset(PLATFORM_HALF, 0, 0), random);
        offerings(level, origin.offset(-PLATFORM_HALF, 0, 0), random);
        return true;
    }

    private void offerings(WorldGenLevel level, BlockPos pos, RandomSource random) {
        WorldDressing.barrel(level, pos, random,
                new ItemStack(ModItems.KRAVE_CEREAL.get(), 3 + random.nextInt(6)),
                new ItemStack(ModItems.KRAVE_MILK.get(), 1 + random.nextInt(2)),
                new ItemStack(ModItems.CEREAL_BOWL.get()),
                random.nextBoolean() ? new ItemStack(ModItems.KRAVE_BOX.get()) : ItemStack.EMPTY,
                random.nextInt(4) == 0 ? new ItemStack(ModItems.KRAVE_DUST.get(), 4 + random.nextInt(5)) : ItemStack.EMPTY,
                random.nextInt(6) == 0 ? new ItemStack(ModItems.RED_HAT.get()) : ItemStack.EMPTY);
    }
}
