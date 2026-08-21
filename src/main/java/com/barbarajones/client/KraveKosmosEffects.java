package com.barbarajones.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * The Krave Kosmos's own sky. Was {@code SkyType.END} at first, reusing the
 * End's own minimal void-and-stars rendering - but that sky type barely
 * shows a real sky color at all (mostly void-black with the barest tint),
 * which is why an earlier attempt at "red sky" here read as "still looks
 * weird" rather than actually red. {@code SkyType.NORMAL} is what actually
 * paints a full gradient sky dome from the biome's {@code sky_color} (see
 * krave_void.json) the way the Overworld's own sky does - the real fix for
 * "make the sky red" is this switch, not the color value, which was already
 * correct. Trade-off: NORMAL also draws the sun/moon sprites, which END's
 * sky type doesn't - with {@code fixed_time} locking the clock in the
 * dimension_type, they just sit still rather than crossing the sky, a minor
 * cosmetic side effect rather than the day/night cycle actually running.
 *
 * <p>Cloud LEVEL is a separate, independent constructor argument from the
 * sky type - already enabled here regardless of which SkyType is picked.
 * The cloud TEXTURE/color itself is a different matter entirely: it comes
 * from {@code ClientLevel.getCloudColor()}, a concrete vanilla method with
 * no Forge hook and no per-dimension override point at all - not something
 * reachable without a client-side rendering patch (a mixin), which is
 * beyond what this mod does anywhere else. Clouds here are real, but they
 * are vanilla's white cloud texture, not a chocolate-colored one - that
 * part of the original ask genuinely cannot be delivered through data or
 * any exposed Forge API.
 */
public class KraveKosmosEffects extends DimensionSpecialEffects {

    public KraveKosmosEffects() {
        super(160.0F, false, SkyType.NORMAL, false, true);
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
