package com.barbarajones.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The Krave Kosmos's own sky: red instead of the plain void-black the
 * dimension_type's old borrowed "minecraft:the_end" effects gave it, and
 * with an actual cloud layer, which the End's effects never render at all
 * (their own cloud height is set specifically to suppress them). Still no
 * ground plane and no bright lightmap forcing - same void-with-floating-
 * islands feel, just red instead of black and with clouds now overhead.
 *
 * <p>Cloud LEVEL and the sky-dome TYPE are separate, independent settings
 * on the constructor - swapping this in doesn't require moving off
 * {@link DimensionSpecialEffects.SkyType#END}'s minimal dome rendering
 * (stars, no sun/moon sprite, no day/night gradient) just to get clouds; a
 * real finite cloud height is enough on its own.
 *
 * <p>The actual sky colour comes from the biome's own {@code sky_color}
 * (see krave_void.json), not from here - this class only controls the
 * cloud layer, fog behaviour, and sunrise band. Registered under
 * {@code barbarajones:krave_kosmos}, referenced from the dimension_type's
 * {@code effects} field.
 */
public class KraveKosmosEffects extends DimensionSpecialEffects {

    public KraveKosmosEffects() {
        super(160.0F, false, SkyType.END, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;   // fixed_time in the dimension_type means there is no sunrise to color
    }
}
