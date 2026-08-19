package com.barbarajones.block.krave;

import javax.annotation.Nullable;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * Turns a planted Krave Sapling into a Krave tree.
 *
 * <p>The grower only names a configured feature; it never touches the registry
 * itself, so the key can be resolved lazily at growth time from
 * {@code data/barbarajones/worldgen/configured_feature/krave_tree_single.json}.
 */
public class KraveTreeGrower extends AbstractTreeGrower {

    /** Must match the configured-feature JSON filename exactly. */
    public static final ResourceKey<ConfiguredFeature<?, ?>> KRAVE_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation(BarbaraJonesMod.MODID, "krave_tree_single"));

    @Override
    @Nullable
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random,
                                                                       boolean hasFlowers) {
        return KRAVE_TREE;
    }
}
