package com.barbarajones.boss.mom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

/**
 * The three acts of the Mom Cobb fight, and the boss-bar dressing that goes
 * with each one.
 *
 * <p>Which act she is in comes purely from her health fraction, but the boss
 * only ever moves FORWARD through this enum. She heals herself in the last act
 * (see {@link MomCobbBoss} and {@link MomKraveStash}), and a phase that slid
 * back a step every time she got a box down would re-announce itself, re-colour
 * the bar and re-seed her stash on a loop.
 */
public enum MomPhase {

    /** Act one: the interrogation. She throws whatever is within reach. */
    QUESTIONS("Mom Cobb - \"Where have you been?\"",
            BossEvent.BossBarColor.YELLOW,
            "Where have you been? Do you know what TIME it is?",
            new String[] {
                "Mom Cobb: \"I called you six times. SIX.\"",
                "Mom Cobb: \"Don't you walk away from me.\"",
                "Mom Cobb: \"Whose house do you think this is?\"",
                "Mom Cobb: \"I am not asking again.\""
            }),

    /** Act two: the power goes off and she goes looking for Cayden. */
    GAME("Mom Cobb - \"GET OFF THAT GAME\"",
            BossEvent.BossBarColor.BLUE,
            "GET OFF THAT GAME. I am TALKING to you.",
            new String[] {
                "Mom Cobb: \"CAYDEN. Come here. NOW.\"",
                "Mom Cobb: \"I will unplug the whole house.\"",
                "Mom Cobb: \"You can't hide behind somebody forever.\"",
                "Mom Cobb: \"Lights out. Every damn one of them.\""
            }),

    /** Act three: she goes for the stash, and eats it to stay standing. */
    KRAVE("Mom Cobb - \"I'M TAKING THE KRAVE\"",
            BossEvent.BossBarColor.RED,
            "Fine. FINE. I'M TAKING THE KRAVE.",
            new String[] {
                "Mom Cobb: \"This garbage is going in the trash.\"",
                "Mom Cobb: \"Nobody in this house needs this.\"",
                "Mom Cobb: \"You want it? Come and TAKE it.\"",
                "Mom Cobb: \"I bought it. It's mine.\""
            });

    private final String barTitle;
    private final BossEvent.BossBarColor color;
    private final String entryLine;
    private final String[] taunts;

    MomPhase(String barTitle, BossEvent.BossBarColor color, String entryLine, String[] taunts) {
        this.barTitle = barTitle;
        this.color = color;
        this.entryLine = entryLine;
        this.taunts = taunts;
    }

    public Component barTitle() {
        return Component.literal(this.barTitle);
    }

    public BossEvent.BossBarColor barColor() {
        return this.color;
    }

    /** Shouted the moment she crosses into this act. */
    public String entryLine() {
        return ChatFormatting.RED + "" + ChatFormatting.BOLD + "Mom Cobb: \"" + this.entryLine + "\"";
    }

    public String taunt(int index) {
        return ChatFormatting.RED + this.taunts[Math.floorMod(index, this.taunts.length)];
    }

    /** Reads a health fraction (0..1). Callers are responsible for never going backwards. */
    public static MomPhase forHealth(float fraction) {
        if (fraction > 0.66F) {
            return QUESTIONS;
        }
        return fraction > 0.33F ? GAME : KRAVE;
    }

    public static MomPhase byOrdinal(int ordinal) {
        MomPhase[] all = values();
        return all[Math.floorMod(ordinal, all.length)];
    }
}
