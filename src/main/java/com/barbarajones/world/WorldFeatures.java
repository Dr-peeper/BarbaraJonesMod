package com.barbarajones.world;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registry for the overworld set dressing - the five small hand-built scenes
 * that make the surface look like this cast lives on it.
 *
 * <p>These register themselves straight off {@link RegisterEvent} rather than
 * through a {@code DeferredRegister}, so the whole world package is
 * self-contained: no other file has to be edited to turn it on, and nothing
 * here can collide with the Krave Kosmos features that already live in
 * {@code content.ModFeatures}. Do not also list these there - registering the
 * same id twice is a hard crash on startup.
 *
 * <p>The matching data files are
 * {@code data/barbarajones/worldgen/configured_feature}, the placements next
 * to them, and the two {@code forge/biome_modifier} files that inject them
 * into every overworld biome.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WorldFeatures {

    public static final Feature<NoneFeatureConfiguration> BARBARA_LAWN =
            new BarbaraLawnFeature(NoneFeatureConfiguration.CODEC);

    public static final Feature<NoneFeatureConfiguration> SEWER_ENTRANCE =
            new SewerEntranceFeature(NoneFeatureConfiguration.CODEC);

    public static final Feature<NoneFeatureConfiguration> BURNT_PATCH =
            new BurntPatchFeature(NoneFeatureConfiguration.CODEC);

    public static final Feature<NoneFeatureConfiguration> ABANDONED_CAR =
            new AbandonedCarFeature(NoneFeatureConfiguration.CODEC);

    public static final Feature<NoneFeatureConfiguration> CEREAL_SHRINE =
            new CerealShrineFeature(NoneFeatureConfiguration.CODEC);

    private WorldFeatures() { }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.FEATURES, helper -> {
            helper.register(id("barbara_lawn"), BARBARA_LAWN);
            helper.register(id("sewer_entrance"), SEWER_ENTRANCE);
            helper.register(id("burnt_patch"), BURNT_PATCH);
            helper.register(id("abandoned_car"), ABANDONED_CAR);
            helper.register(id("cereal_shrine"), CEREAL_SHRINE);
        });
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(BarbaraJonesMod.MODID, name);
    }
}
