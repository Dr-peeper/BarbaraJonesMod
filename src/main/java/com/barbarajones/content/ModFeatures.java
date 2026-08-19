package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.world.AbandonedCarFeature;
import com.barbarajones.world.BarbaraLawnFeature;
import com.barbarajones.world.BurntPatchFeature;
import com.barbarajones.world.CerealShrineFeature;
import com.barbarajones.world.SewerEntranceFeature;
import com.barbarajones.worldgen.feature.KraveCavePocketFeature;
import com.barbarajones.worldgen.feature.KraveMountainFeature;
import com.barbarajones.worldgen.feature.KravePeakFeature;
import com.barbarajones.worldgen.feature.KraveRuinFeature;
import com.barbarajones.worldgen.feature.KraveTreeFeature;
import com.barbarajones.worldgen.feature.KraveValleyFeature;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Custom worldgen features - Krave Kosmos ruins and cave pockets. */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, BarbaraJonesMod.MODID);

    private ModFeatures() { }

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_RUIN =
            FEATURES.register("krave_ruin", () -> new KraveRuinFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_CAVE_POCKET =
            FEATURES.register("krave_cave_pocket", () -> new KraveCavePocketFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_MOUNTAIN =
            FEATURES.register("krave_mountain", () -> new KraveMountainFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_VALLEY =
            FEATURES.register("krave_valley", () -> new KraveValleyFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_PEAK =
            FEATURES.register("krave_peak", () -> new KravePeakFeature(NoneFeatureConfiguration.CODEC));

    // ---- overworld set dressing and the Krave tree -------------------------
    // These classes and their configured_feature JSON both shipped, but nothing
    // ever registered the features themselves. A configured_feature naming a
    // feature that does not exist fails the WHOLE registry load, which is why
    // world creation died rather than just missing a structure.
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> KRAVE_TREE =
            FEATURES.register("krave_tree", () -> new KraveTreeFeature(NoneFeatureConfiguration.CODEC));
}
