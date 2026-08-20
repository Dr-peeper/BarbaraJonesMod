package com.barbarajones.v2.build.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.ui.KraveKeys;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

/**
 * The rotate key. R by default, because that is what every other building mod
 * uses and muscle memory is a feature.
 *
 * <p>Left-clicking does the same thing, so the key is the alternative rather
 * than the only way in - but it is the one that works while you are standing
 * far enough back to actually see the building you are placing.
 *
 * <p>Shares the mod's existing key category rather than inventing a second one.
 */
public final class BuildKeys {

    public static final KeyMapping ROTATE = new KeyMapping(
            "key." + BarbaraJonesMod.MODID + ".rotate_schematic",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            KraveKeys.CATEGORY);

    private BuildKeys() { }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(ROTATE);
    }
}
