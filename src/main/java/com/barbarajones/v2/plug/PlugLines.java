package com.barbarajones.v2.plug;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Everything he says while doing business, and the two helpers that put it on
 * screen.
 *
 * <p>Formatted exactly like the lines already in {@code ThePlug.mobInteract} -
 * dark grey, {@code The Plug: "..."} - so the job board reads as the same man
 * talking rather than a second system bolted onto him. Mechanical asides (a
 * price, a timer, a reputation nudge) use {@link #note} instead, in plain grey
 * and without the quotation marks, because he does not narrate his own stats.
 */
public final class PlugLines {

    private PlugLines() { }

    static final String[] TAKING_JOB_LOW = {
            "bet. say no more. actually say a little more, where you want me to look",
            "aight I'm on it. I KNOW a guy. well I know OF a guy. I know he exists",
            "consider it handled. consider it. keep considerin it",
            "you came to the right man. there was other men. you came to me though",
    };

    static final String[] TAKING_JOB_HIGH = {
            "say less. I already know where it's at",
            "aight. I'll be back before you finish whatever you finna do",
            "this the easy one. this ain't even work for me no more",
            "you good. go sit down somewhere",
    };

    static final String[] LEAVING = {
            "don't call my phone. I'll call YOUR phone",
            "if anybody come lookin, you ain't seen me today",
            "gimme a minute. a real minute, not a girl minute",
            "I'm walkin. watch this. watch how fast I walk",
    };

    static final String[] BUSY = {
            "I'm ALREADY out there for you. you can't hire a man twice, that's not how a man work",
            "one job at a time. I got one body",
            "your boy still out there handlin it. relax",
    };

    static final String[] BROKE = {
            "with WHAT. with what money. show me the money and I'll show you the hustle",
            "you countin wrong. count it again. count it in front of me",
            "I don't do payment plans. I did one time. it went bad",
    };

    static final String[] WAITING = {
            "you got a bag waitin on you. pick that up first, I'm not a storage unit",
            "nah collect what you already paid for. empty hand, click me, take yo stuff",
            "I'm holdin your last one right here. this is startin to feel like my problem",
    };

    static final String[] RETURNED = {
            "aye. I'm back. come see me",
            "handled. come get yo stuff before I get comfortable with it",
            "I'm outside. I been outside. where you at",
            "job's done. I'm not holdin this all day",
    };

    static final String[] STAND_IN = {
            "yeah your boy ain't make it back. I got his bag though. we all one family out here",
            "he sent me. don't worry about where he at. worry about what's in the bag",
    };

    static final String[] RIP_OFF_FOLLOW_UP = {
            "why you lookin at me like that",
            "next one on me. probably. don't hold me to that",
            "that's the game bro. sometimes the game games YOU",
            "you want a receipt? I'll write you a receipt",
    };

    static final String[] IDLE = {
            "you just gon stand there or you gon put me on somethin",
            "sneak and click me. I'll tell you what I do. I do a lot",
            "I'm workin. this IS workin. standin here IS the work",
            "everything I sell is real. everything. every single thing",
    };

    static final String[] REFUSE_ITEM = {
            "what am I supposed to do with that",
            "nah. put that away before somebody see it",
            "I take emeralds, dollars, or a five hundred. that's it. that's the whole menu",
    };

    static final String[] HIT = {
            "AYE. AYE. we was DOIN business",
            "you just cost yourself a discount, on God",
            "that's how you lose a connect right there. that right there",
    };

    static String pick(RandomSource random, String[] bank) {
        return bank[random.nextInt(bank.length)];
    }

    /** Him talking. Same shape as the lines already in {@code ThePlug}. */
    public static void say(Player player, String line) {
        player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                + "The Plug: \"" + line + "\""));
    }

    /** A mechanical aside - price, timer, reputation. Not in his voice, so not in quotes. */
    public static void note(Player player, String text) {
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + text));
    }
}
