package com.barbarajones;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common config. Currently just the escape hatch for endless death-stage mode
 * (armed permanently on a player's 11th pet death - see EventHandler.nextDeathStage
 * and KraveApocalypse). The user was offered a config toggle or a craftable cure
 * and never answered either way, so this defaults OFF: endless mode stays exactly
 * as permanent as originally shipped unless someone deliberately flips it on.
 */
public final class Config {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_KRAVE_CLEANSE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DREAD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("horror");
        ENABLE_DREAD = builder
                .comment(
                    "The ambient Dread director: creeping vignette, distant whispers behind you,",
                    "a rising heartbeat, subliminal face flashes and rare light-outs when you're",
                    "alone in the dark. Purely client-side atmosphere - no gameplay effect.",
                    "Set false for a calmer experience.")
                .define("enableDread", true);
        builder.pop();

        builder.push("endlessMode");
        ALLOW_KRAVE_CLEANSE = builder
                .comment(
                    "Whether a crafted Krave Cleanse can actually break endless death-stage mode",
                    "(the 'it does not stop now' state armed on a player's 11th pet death).",
                    "Default false: endless mode stays exactly as permanent as originally shipped -",
                    "the item can still be crafted and used, it just won't do anything while this is off.",
                    "Set true to let a player craft their way out of it.")
                .define("allowKraveCleanse", false);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() { }
}
