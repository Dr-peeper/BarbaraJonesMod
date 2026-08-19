package com.barbarajones.world;

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
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A scorch mark where somebody put a lighter to a whole patch of grass and
 * walked off. Coarse dirt and podzol under a burnt-out campfire, a couple of
 * dead bushes, and - if you are lucky - the barrel of ruined product they
 * were trying to dry out.
 *
 * <p>The fire is deliberately <em>out</em>. A lit campfire reads as a camp
 * somebody is using; a cold one reads as something that happened here a while
 * ago and nobody came back for, which is the note the whole cast lives on.
 */
public class BurntPatchFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MAX_RADIUS = 4;

    public BurntPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!WorldDressing.standable(level, origin)) {
            return false;
        }
        // a burn scar can happily lie over a gentle slope, so this is far more
        // forgiving about flatness than the built structures are
        if (!WorldDressing.flatEnough(level, origin, 3, 3)) {
            return false;
        }

        int radius = 2 + random.nextInt(MAX_RADIUS - 1);
        int scorched = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSqr = dx * dx + dz * dz;
                if (distSqr > radius * radius) {
                    continue;
                }
                BlockPos pos = origin.offset(dx, 0, dz);
                if (!WorldDressing.standable(level, pos)) {
                    continue;
                }
                WorldDressing.clearAbove(level, pos, 2);

                // ragged edge: the further out you go the likelier the fire
                // simply did not reach
                float edge = (float) distSqr / (radius * radius);
                if (random.nextFloat() < edge * 0.7F) {
                    continue;
                }
                WorldDressing.set(level, pos.below(), charred(random));
                scorched++;

                if (random.nextInt(9) == 0) {
                    WorldDressing.attach(level, pos, Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
        if (scorched == 0) {
            return false;
        }

        // the cold campfire at the middle of it
        WorldDressing.set(level, origin, Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.LIT, Boolean.FALSE)
                .setValue(BlockStateProperties.SIGNAL_FIRE, Boolean.FALSE));

        if (random.nextInt(3) == 0) {
            BlockPos stash = origin.offset(radius - 1, 0, 0);
            if (WorldDressing.standable(level, stash)) {
                WorldDressing.barrel(level, stash, random,
                        new ItemStack(ModItems.BURNT_GRASS.get(), 4 + random.nextInt(8)),
                        new ItemStack(ModItems.DICED_GRASS.get(), 1 + random.nextInt(4)),
                        new ItemStack(ModItems.LIGHTER.get()),
                        random.nextBoolean() ? new ItemStack(ModItems.ROLLING_PAPER.get(), 2) : ItemStack.EMPTY);
            }
        }
        return true;
    }

    private BlockState charred(RandomSource random) {
        int roll = random.nextInt(10);
        if (roll < 6) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (roll < 9) {
            return Blocks.PODZOL.defaultBlockState();
        }
        return Blocks.GRAVEL.defaultBlockState();
    }
}
