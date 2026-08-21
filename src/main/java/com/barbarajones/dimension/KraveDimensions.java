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
     * same convention vanilla uses for the End's main island). Every portal
     * room KraveDoorBlock auto-builds in the Kosmos lands hundreds of blocks
     * from here, in a random direction, specifically so the den reads as a
     * real, distant destination instead of something standing right next to
     * wherever you first arrive - see KraveDoorBlock.buildKosmosRoomCopy().
     */
    public static final Vec3 BOSS_ISLAND = new Vec3(0.0D, 90.0D, 0.0D);

    private KraveDimensions() { }
}
