package com.barbarajones.quest;

import com.barbarajones.content.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The Krave questline. Progress lives on the Quest Book ItemStack's own NBT,
 * which rides normal inventory sync to the client - no custom packets needed.
 */
public final class Quests {

    public static final int START        = 0;
    public static final int CAYDEN       = 1;
    public static final int BARBARA      = 2;
    public static final int SUMMON       = 3;
    public static final int FIGHT        = 4;
    // ACT II - the lore videos
    public static final int ACT2_HAT     = 5;
    public static final int ACT2_MOUSE   = 6;
    public static final int ACT2_PLUG    = 7;
    public static final int ACT2_SEWER   = 8;
    public static final int ACT2_REVENGE = 9;
    public static final int DONE         = 10;

    public static final String[] TITLE = {
        "The Craving", "Runaway", "Therapy", "The Summoning", "The Fight",
        "I KRAVE THE KRAVE", "The Prequel", "Off-Brand", "The Sewer",
        "Got That Mothafucker", "Peace at Last"
    };

    public static final String[] OBJECTIVE = {
        "Craft KRAVE CEREAL: 2 Wheat + 1 Sugar + 1 Cocoa Beans (shapeless, makes 2). Your green "
            + "COOKBOOK lists every recipe - right-click it. Completes when cereal is in your inventory.",
        "Cayden Cobb fights any hostile on sight, and they hunt him back. FEED HIM KRAVE "
            + "(right-click holding cereal): +1 attack per 5 boxes, but fatter and slower. At 25 he "
            + "unlocks KRAVE RAGE. Right-click him EMPTY-HANDED inside a room to make him move in - "
            + "he needs real housing. If he dies near you, a DEATH STAGE fires.",
        "Find BARBARA JONES and RIGHT-CLICK HER with an EMPTY HAND to recruit her. Grass in hand "
            + "feeds her instead (and taunting her with it makes her PSYCHO). A Rolled Joint gets "
            + "her high - she blows the O's she invented.",
        "Craft a KRAVE BOX (2 Paper + 1 Krave Cereal) and RIGHT-CLICK it to summon THE KRAVE "
            + "MONSTER. Clear ground recommended: boss bar, huge jumps, constant teleporting.",
        "Kill the Krave Monster - Barbara and Cayden fight at your side. Hit him between blinks. "
            + "Brew a CEREAL BOWL (bowl + Krave + Krave Milk) first for big combat buffs.",
        "ACT II. Craft the BACKWARDS RED HAT (red wool + string + red dye) and RIGHT-CLICK it "
            + "to do the Krave dance-walk. \"I KRAVE THE KRAVE!\" If his Mom hears it... oh no.",
        "The prequel. Craft a COMPUTER MOUSE (iron + redstone + string). The download had a "
            + "VIRUS - RIGHT-CLICK to hurl the mouse out the front door and SMASH it.",
        "Craft MOM'S $500 (2 gold + paper), find THE PLUG (black ski mask) and RIGHT-CLICK him "
            + "holding the money. See what your $500 buys.",
        "You got scammed. Go UNDERGROUND (below y=45) and SMOKE A ROLLED JOINT... and realize it "
            + "too is just grass. Someone is about to have a very bad day.",
        "The Plug is HERE with the sniper that got Cayden. KILL HIM. Watch for the tracer - he "
            + "hits hard from very far away.",
        "The Plug is dead, the scam avenged, the Krave conquered. It is finally over. "
            + "(The death stages, of course, remain. Forever.)"
    };

    private Quests() { }

    public static int getStage(ItemStack book) {
        if (book == null || book.isEmpty() || book.getTag() == null) {
            return START;
        }
        return book.getTag().getInt("QuestStage");
    }

    public static void setStage(ItemStack book, int stage) {
        if (book == null || book.isEmpty()) {
            return;
        }
        CompoundTag tag = book.getOrCreateTag();
        tag.putInt("QuestStage", stage);
    }

    @Nullable
    public static ItemStack findBook(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.QUEST_BOOK.get())) {
                return s;
            }
        }
        return null;
    }

    public static int getStage(Player player) {
        ItemStack book = findBook(player);
        return book == null ? START : getStage(book);
    }

    /** Advance only if currently at exactly stage-1; announces the new stage. */
    public static boolean advanceTo(Player player, int stage) {
        ItemStack book = findBook(player);
        if (book == null || getStage(book) != stage - 1) {
            return false;
        }
        setStage(book, stage);
        announce(player, stage);
        return true;
    }

    public static void announce(Player player, int stage) {
        if (player.level().isClientSide) {
            return;
        }
        String head = ChatFormatting.LIGHT_PURPLE + "[Krave Quest] " + ChatFormatting.WHITE;
        if (stage >= DONE) {
            player.sendSystemMessage(Component.literal(
                    head + ChatFormatting.GOLD + "Quest complete: " + TITLE[DONE] + "!"));
            return;
        }
        player.sendSystemMessage(Component.literal(
                head + "Stage " + stage + " - " + ChatFormatting.YELLOW + TITLE[stage]));
        player.sendSystemMessage(Component.literal(
                ChatFormatting.GRAY + "  " + OBJECTIVE[stage]));
    }
}
