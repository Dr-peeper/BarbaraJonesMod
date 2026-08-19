package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Custom damage types, data-driven via data/barbarajones/damage_type/*.json
 * (required since 1.19.4 - DamageType is a registry, not a Java enum
 * anymore). {@link DamageSources} only exposes vanilla types as named
 * methods, so a custom one is built by hand from its registry Holder.
 */
public final class ModDamageTypes {

    public static final ResourceKey<DamageType> CHOCOLATE = key("chocolate");
    public static final ResourceKey<DamageType> KRAVE_BOX = key("krave_box");

    private ModDamageTypes() { }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(BarbaraJonesMod.MODID, name));
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> type) {
        Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        return new DamageSource(holder);
    }
}
