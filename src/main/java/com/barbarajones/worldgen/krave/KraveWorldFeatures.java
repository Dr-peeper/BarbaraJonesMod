package com.barbarajones.worldgen.krave;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registry for the features that only the Krave world type uses.
 *
 * <p>Registers straight off {@link RegisterEvent}, the same way
 * {@code com.barbarajones.world.WorldFeatures} does, so the Krave world type is
 * self-contained: turning it on or off touches no shared registry class and
 * cannot collide with the Krave Kosmos features in {@code content.ModFeatures}.
 * Every id here carries the {@code krave_world_} prefix for that reason -
 * registering the same feature id twice is a hard crash on startup.
 *
 * <p>The matching data files are
 * {@code data/barbarajones/worldgen/configured_feature/krave_world_*.json} and
 * the placements next to them; the biomes that pull them in are
 * {@code data/barbarajones/worldgen/biome/krave_world_*.json}.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KraveWorldFeatures {

    public static final Feature<NoneFeatureConfiguration> MONOLITH =
            new KraveMonolithFeature(NoneFeatureConfiguration.CODEC);

    private KraveWorldFeatures() { }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.FEATURES, helper ->
                helper.register(new ResourceLocation(BarbaraJonesMod.MODID, "krave_world_monolith"),
                        MONOLITH));
    }
}
