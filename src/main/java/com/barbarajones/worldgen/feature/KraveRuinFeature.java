package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModItems;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A small alien ruin scattered across the Krave Kosmos islands - pillars
 * around a barrel of upgraded loot. Blocks/block-entities only, no entities -
 * deliberately kept out of the entity-spawning-from-worldgen risk zone (see
 * KraveKosmosAmbience for the ambient Kosmonaut spawner that populates the
 * area instead).
 */
public class KraveRuinFeature extends Feature<NoneFeatureConfiguration> {

    public KraveRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!level.getBlockState(origin.below()).isSolid()) {
            return false;
        }

        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();

        // four short pillars around the loot barrel
        int[][] offsets = { {3, 3}, {-3, 3}, {3, -3}, {-3, -3} };
        for (int[] off : offsets) {
            BlockPos base = origin.offset(off[0], 0, off[1]);
            if (!level.getBlockState(base.below()).isSolid()) {
                continue;
            }
            int height = 3 + random.nextInt(2);
            for (int y = 0; y < height; y++) {
                level.setBlock(base.above(y), frame, 3);
            }
        }
        // a bit of deterministic-count (not deterministic-position) rubble
        for (int i = 0; i < 6; i++) {
            BlockPos base = origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (level.getBlockState(base.below()).isSolid() && level.getBlockState(base).isAir()) {
                level.setBlock(base, random.nextBoolean() ? frame : dirt, 3);
            }
        }

        level.setBlock(origin, Blocks.BARREL.defaultBlockState(), 3);
        if (level.getBlockEntity(origin) instanceof BarrelBlockEntity barrel) {
            fillLoot(barrel, random);
        }
        return true;
    }

    private void fillLoot(BarrelBlockEntity barrel, RandomSource random) {
        barrel.setItem(4, new ItemStack(ModItems.KRAVE_CEREAL.get(), 4 + random.nextInt(5)));
        barrel.setItem(11, new ItemStack(ModItems.KRAVE_TETHER.get(), 1 + random.nextInt(2)));
        if (random.nextBoolean()) {
            barrel.setItem(13, new ItemStack(ModItems.KRAVE_SWORD.get()));
        }
    }
}
