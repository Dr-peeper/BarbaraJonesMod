package com.barbarajones.v2.mobs;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's OWN sound registry. Every clip is synthesized by
 * {@code tools/make_craveling_mobs.ps1} - see docs/modules/craveling-mobs.md.
 */
public final class ModMobSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BarbaraJonesMod.MODID);

    private ModMobSounds() { }

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(BarbaraJonesMod.MODID, name)));
    }

    // Craveling
    public static final RegistryObject<SoundEvent> CRAVELING_AMBIENT = reg("craveling_ambient");
    public static final RegistryObject<SoundEvent> CRAVELING_HURT    = reg("craveling_hurt");
    public static final RegistryObject<SoundEvent> CRAVELING_DEATH   = reg("craveling_death");
    public static final RegistryObject<SoundEvent> CRAVELING_STEP    = reg("craveling_step");

    // Krispbone
    public static final RegistryObject<SoundEvent> KRISPBONE_AMBIENT = reg("krispbone_ambient");
    public static final RegistryObject<SoundEvent> KRISPBONE_HURT    = reg("krispbone_hurt");
    public static final RegistryObject<SoundEvent> KRISPBONE_DEATH   = reg("krispbone_death");
    public static final RegistryObject<SoundEvent> KRISPBONE_SHOOT   = reg("krispbone_shoot");

    // Loomweaver
    public static final RegistryObject<SoundEvent> LOOMWEAVER_AMBIENT = reg("loomweaver_ambient");
    public static final RegistryObject<SoundEvent> LOOMWEAVER_HURT    = reg("loomweaver_hurt");
    public static final RegistryObject<SoundEvent> LOOMWEAVER_DEATH   = reg("loomweaver_death");
    public static final RegistryObject<SoundEvent> LOOMWEAVER_WEB     = reg("loomweaver_web");

    // Soggy
    public static final RegistryObject<SoundEvent> SOGGY_AMBIENT = reg("soggy_ambient");
    public static final RegistryObject<SoundEvent> SOGGY_HURT    = reg("soggy_hurt");
    public static final RegistryObject<SoundEvent> SOGGY_DEATH   = reg("soggy_death");
    public static final RegistryObject<SoundEvent> SOGGY_SPLASH  = reg("soggy_splash");

    // The Mascot
    public static final RegistryObject<SoundEvent> MASCOT_AMBIENT = reg("mascot_ambient");
    public static final RegistryObject<SoundEvent> MASCOT_HURT    = reg("mascot_hurt");
    public static final RegistryObject<SoundEvent> MASCOT_DEATH   = reg("mascot_death");
    public static final RegistryObject<SoundEvent> MASCOT_FLEE    = reg("mascot_flee");
}
