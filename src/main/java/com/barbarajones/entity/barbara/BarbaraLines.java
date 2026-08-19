package com.barbarajones.entity.barbara;

import com.barbarajones.entity.BarbaraJones;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * What Barbara says while she works. Written as literal components rather than
 * lang keys to match how the rest of her dialogue in {@code BarbaraJones} is
 * already done, so this drops in without touching the lang files.
 */
public final class BarbaraLines {

    /** How far a player can be and still hear her run her mouth. */
    private static final double EARSHOT = 24.0D;

    private static final String[] OS = {
        "Watch this. Watch. This.",
        "Look at them O's, baby.",
        "Perfect circle. Every single time."
    };
    private static final String[] CHERRY = {
        "Have a cherry, sugar.",
        "Catch this.",
        "That's hot, ain't it."
    };
    private static final String[] SCREEN = {
        "Y'all can't see me now.",
        "Fog 'em up.",
        "Breathe that in, baby."
    };
    private static final String[] ASH = {
        "Get OFF me!",
        "Ash everywhere. My bad.",
        "Back UP off me."
    };
    private static final String[] BLOWBACK = {
        "Right in your face.",
        "Say that again. I dare you.",
        "You smell that? That's you leavin'."
    };
    private static final String[] CONTACT = {
        "Here, baby, take a pull.",
        "Pass it around, we all in this.",
        "Y'all look tense. I got you."
    };
    private static final String[] BURNOUT = {
        "That's the WHOLE bag!",
        "I'm smokin' all of it. Watch me.",
        "This one's for Cayden."
    };

    private BarbaraLines() { }

    /** Null for the passive - she does not announce standing there smelling like that. */
    public static String forAbility(SmokeAbility ability, RandomSource rng) {
        String[] pool = switch (ability) {
            case THE_OS -> OS;
            case LIT_CHERRY -> CHERRY;
            case SMOKE_SCREEN -> SCREEN;
            case ASH_CLOUD -> ASH;
            case BLOWBACK -> BarbaraLines.BLOWBACK;
            case CONTACT_HIGH -> CONTACT;
            case BURNOUT -> BarbaraLines.BURNOUT;
            case SECOND_HAND -> (String[]) null;
        };
        return pool == null ? null : pool[rng.nextInt(pool.length)];
    }

    /** Speak to whoever is close enough to have heard it. */
    public static void speak(BarbaraJones barbara, String line) {
        if (line == null) {
            return;
        }
        Component msg = Component.literal(ChatFormatting.DARK_GREEN + "Barbara: "
                + ChatFormatting.GRAY + "\"" + line + "\"");
        for (Player player : barbara.level().getEntitiesOfClass(Player.class,
                barbara.getBoundingBox().inflate(EARSHOT))) {
            player.sendSystemMessage(msg);
        }
    }
}
