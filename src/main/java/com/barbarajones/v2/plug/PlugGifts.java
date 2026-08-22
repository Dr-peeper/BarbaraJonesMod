package com.barbarajones.v2.plug;

import com.barbarajones.content.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Things you can hand The Plug that are not payment for anything.
 *
 * <p>Reputation from gifts is capped per day in {@link PlugReputation}, which is
 * what stops a stack of sixty-four cigarettes buying the whole progression in
 * one sitting. The cap lives on the reputation record rather than here because
 * it has to be persisted; this file is only the table.
 *
 * <p>Negative entries are deliberate. Handing him back the fake weed he sold
 * you is not a gift, it is a comment, and he takes it as one.
 *
 * <p>Nothing he accepts as money appears in this table. {@link PlugBusiness}
 * checks currency first, so a $500 note in here would be an entry no player
 * could ever reach.
 */
final class PlugGifts {

    /** One giftable item: what it is worth to him, and what he says about it. */
    record Gift(Supplier<Item> item, int reputation, String line) { }

    private PlugGifts() { }

    private static Gift g(Supplier<Item> item, int reputation, String line) {
        return new Gift(item, reputation, line);
    }

    private static final Gift[] TABLE = {
            g(ModItems.GOLDEN_KRAVE, 9,
                    "the GOLD one? for me? nah you serious? nah you SERIOUS?"),
            g(ModItems.GOLDEN_JOINT, 8,
                    "man who ROLLED this. I need a name. I need a phone number."),
            g(ModItems.SNIPER_SCOPE, 6,
                    "where you get this. no - don't tell me. don't ever tell me."),
            g(ModItems.BURNER_PHONE, 5,
                    "now THIS is thoughtful. this is a thoughtful gift. two contacts already in it."),
            g(ModItems.ROLLED_JOINT, 5,
                    "aight. aight. you know what, you aight."),
            g(ModItems.CHEPINA, 4,
                    "CHEPINA?? aw hell yeah. hell yeah. we not talkin for the next ten minutes."),
            g(ModItems.SKI_MASK, 4,
                    "a spare. everybody need a spare. mine got a hole in it from a incident."),
            g(ModItems.CIGAR, 3,
                    "a cigar. what we celebratin. don't tell me, let me guess. actually tell me."),
            g(ModItems.NUGGET_BOX, 3,
                    "the whole box. you didn't even ask for none back. that's character."),
            g(ModItems.BARBARA_PLUSH, 3,
                    "why they made this. why they MADE this. I love it. it's mine now."),
            g(ModItems.CIGARETTE, 2,
                    "good lookin. I was finna ask somebody and it was gon be awkward."),
            g(ModItems.MR_PIBB, 2,
                    "cold too. you been thinkin about me."),
            g(ModItems.GATORADE, 2,
                    "electrolytes. I be out there workin, people forget that."),
            g(ModItems.KRAVE_CEREAL, 2,
                    "you know I gotta hide this from that kid, right. this is a liability you handin me."),
            g(ModItems.RED_HAT, 2,
                    "turn it around. bet. watch how different I look."),

            // ---- the ones that are not really gifts ------------------------
            g(ModItems.FAKE_WEED, -1,
                    "you tryna sell ME this? MY product? back to ME? that's disrespectful and I'm keepin it."),
            g(ModItems.FAKE_COCAINE, -1,
                    "oh so you got jokes. it's cold outside, that's all that is. that's all that ever was."),
            g(ModItems.HANDFUL_OF_GRASS, -1,
                    "grass. you brought a man grass. I SELL grass. this is my whole business you handin back."),
            g(ModItems.SEWER_WATER, -1,
                    "put it down. put it DOWN. why is it warm."),
    };

    /** The gift entry for this stack, or null if it is just an item to him. */
    @Nullable
    static Gift find(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Gift gift : TABLE) {
            if (stack.is(gift.item().get())) {
                return gift;
            }
        }
        return null;
    }
}
