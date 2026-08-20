package com.barbarajones.worldgen.feature;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registry for the chocolate waterfall feature and its one ambient sound.
 *
 * <p>Registers straight off {@link RegisterEvent}, the same self-contained
 * pattern as {@code com.barbarajones.worldgen.krave.KraveWorldFeatures} and
 * {@code com.barbarajones.world.WorldFeatures}: turning this file on or off
 * touches nothing else, and it cannot collide with the feature ids already
 * claimed in {@code content.ModFeatures} or the sound ids in
 * {@code content.ModSounds}. One handler, two registries - {@link
 * RegisterEvent#register} is a no-op for whichever registry key isn't the
 * one currently firing, so both can live in the same method safely.
 *
 * <p>The matching data files are
 * {@code data/barbarajones/worldgen/configured_feature/krave_waterfall.json},
 * its placement, and the {@code assets/barbarajones/sounds/chocolate_flow.ogg}
 * clip (synthesized by {@code tools/make_krave_waterfalls.ps1}) wired up in
 * {@code assets/barbarajones/sounds.json}.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KraveWaterfallContent {

    public static final Feature<NoneFeatureConfiguration> WATERFALL =
            new KraveWaterfallFeature(NoneFeatureConfiguration.CODEC);

    /** The soft, continuous pour-and-drip ambience played near a falling chocolate face. */
    public static final SoundEvent CHOCOLATE_FLOW =
            SoundEvent.createVariableRangeEvent(id("chocolate_flow"));

    private KraveWaterfallContent() { }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.FEATURES, helper ->
                helper.register(id("krave_waterfall"), WATERFALL));
        event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper ->
                helper.register(id("chocolate_flow"), CHOCOLATE_FLOW));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(BarbaraJonesMod.MODID, name);
    }
}
