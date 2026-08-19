package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Every custom sound: the real voice clips plus the synthesized horror set. */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BarbaraJonesMod.MODID);

    private ModSounds() { }

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(BarbaraJonesMod.MODID, name)));
    }

    // character voices (real clips from the video)
    public static final RegistryObject<SoundEvent> BARBARA_IDLE  = reg("barbara_idle");
    public static final RegistryObject<SoundEvent> BARBARA_HURT  = reg("barbara_hurt");
    public static final RegistryObject<SoundEvent> BARBARA_DEATH = reg("barbara_death");
    public static final RegistryObject<SoundEvent> BARBARA_RAGE  = reg("barbara_rage");
    public static final RegistryObject<SoundEvent> CAYDEN_ALARM  = reg("cayden_alarm");
    public static final RegistryObject<SoundEvent> CAYDEN_IDLE   = reg("cayden_idle");
    public static final RegistryObject<SoundEvent> CAYDEN_SHOUT  = reg("cayden_krave_shout");
    public static final RegistryObject<SoundEvent> CAYDEN_HURT   = reg("cayden_hurt");
    public static final RegistryObject<SoundEvent> CAYDEN_DEATH  = reg("cayden_death");

    // the horror set
    public static final RegistryObject<SoundEvent> KRAVE_LAUGH   = reg("krave_laugh");
    public static final RegistryObject<SoundEvent> KRAVE_SCREECH = reg("krave_screech");
    public static final RegistryObject<SoundEvent> KRAVE_BOOM    = reg("krave_boom");
    public static final RegistryObject<SoundEvent> KRAVE_TORNADO = reg("krave_tornado");
    public static final RegistryObject<SoundEvent> KRAVE_SPAWN   = reg("krave_spawn");
    public static final RegistryObject<SoundEvent> KRAVE_VOICE   = reg("krave_voice");
    public static final RegistryObject<SoundEvent> KRAVE_SIREN   = reg("krave_siren");
    public static final RegistryObject<SoundEvent> KRAVE_RUMBLE  = reg("krave_hell_rumble");
    public static final RegistryObject<SoundEvent> KRAVE_ROAR    = reg("krave_roar");
    public static final RegistryObject<SoundEvent> KRAVE_HURT    = reg("krave_hurt");
    public static final RegistryObject<SoundEvent> KRAVE_DEATH   = reg("krave_death");

    // original synthesized cues, not part of the reused horror set above -
    // see tools/make_krave_audio.ps1
    public static final RegistryObject<SoundEvent> KRAVE_BEAM_FIRE = reg("krave_beam_fire");
    public static final RegistryObject<SoundEvent> KRAVE_BEAM_HIT  = reg("krave_beam_hit");

    // per-stage canon lines
    public static final RegistryObject<SoundEvent> EVT_OG       = reg("evt_og");
    public static final RegistryObject<SoundEvent> EVT_CHEPINA  = reg("evt_chepina");
    public static final RegistryObject<SoundEvent> EVT_LIGHTER  = reg("evt_lighter");
    public static final RegistryObject<SoundEvent> EVT_MUSIC    = reg("evt_music");
    public static final RegistryObject<SoundEvent> EVT_MANAGER  = reg("evt_manager");
    public static final RegistryObject<SoundEvent> EVT_DEMOCRAT = reg("evt_democrat");
    public static final RegistryObject<SoundEvent> EVT_HOUSE    = reg("evt_house");
    public static final RegistryObject<SoundEvent> EVT_MCD      = reg("evt_mcd");
    public static final RegistryObject<SoundEvent> EVT_ROLL     = reg("evt_roll");
    public static final RegistryObject<SoundEvent> EVT_NOTREADY = reg("evt_notready");

    // raw unlabeled speech clips extracted from the source video (see
    // tools; nobody has matched these to specific lines yet, they're a
    // generic voice-line pool used for Duhl Wol's arrival and Cayden's
    // higher-tier transformation shout until someone identifies them)
    public static final RegistryObject<SoundEvent>[] DIALOGUE = new RegistryObject[24];
    static {
        for (int i = 0; i < 24; i++) {
            DIALOGUE[i] = reg(String.format("dialogue_%02d", i + 1));
        }
    }
}
