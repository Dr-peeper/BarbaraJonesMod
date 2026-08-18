package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.worldgen.feature.KraveCavePocketFeature;
import com.barbarajones.worldgen.feature.KraveRuinFeature;

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
}
