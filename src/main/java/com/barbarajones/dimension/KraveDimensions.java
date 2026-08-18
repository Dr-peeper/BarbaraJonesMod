package com.barbarajones.dimension;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shared constants for the Krave Kosmos dimension. */
public final class KraveDimensions {

    public static final ResourceKey<Level> KRAVE_KOSMOS = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation(BarbaraJonesMod.MODID, "krave_kosmos"));

    /**
     * Roughly where Krave Monster's home island generates (dimension origin,
     * same convention vanilla uses for the End's main island). Players land
     * well away from here so they have to actually traverse the dimension.
     */
    public static final Vec3 BOSS_ISLAND = new Vec3(0.0D, 80.0D, 0.0D);

    /** Where a portal drops a player - offset from the boss island, not on it. */
    public static final Vec3 PORTAL_LANDING = new Vec3(300.0D, 150.0D, 0.0D);

    private KraveDimensions() { }
}
