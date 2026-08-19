package com.barbarajones.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Per-player Krave XP and level - the spine of the mod's progression.
 *
 * <p>Only ONE number is actually stored: the player's lifetime XP total. The
 * level, the XP into the current level and the XP to the next one are all
 * derived from it. Storing the level separately is how progression systems end
 * up desynced from their own XP bar, so this one simply cannot.
 *
 * <p>The total lives in the player's <em>persistent</em> NBT
 * ({@link Player#PERSISTED_NBT_TAG}) - the same sub-tag EventHandler uses for
 * the death-stage counter - because Forge copies that tag across death and
 * respawn. Krave levels are meant to be permanent; dying to the apocalypse you
 * caused should not reset them.
 *
 * <p>Everything here is queryable statically so the HUD/GUI and other
 * subsystems can read a player's standing without touching storage.
 */
public final class KraveLevel {

    /** The ceiling. Level 30 is "I KRAVE THE KRAVE" and there is nothing beyond it. */
    public static final int MAX_LEVEL = 30;

    /** XP needed for level 1 -> 2. */
    private static final int FIRST_STEP = 50;
    /** Each level costs this much more than the one before it (linear ramp, quadratic total). */
    private static final int STEP_GROWTH = 18;

    private static final String KEY_XP = "KraveXpTotal";
    /** Highest level we have already congratulated this player for. */
    private static final String KEY_ANNOUNCED = "KraveLevelAnnounced";

    /** Below this an award is a quiet action-bar blip; at or above it, it earns a chat line. */
    private static final int CHAT_THRESHOLD = 100;

    private KraveLevel() { }

    // ---- storage ------------------------------------------------------------

    /**
     * The mod's persistent scratch tag on this player. Shared with EventHandler
     * (KraveDeathStage / KraveEndless) and {@link PlayerStats} - one sub-tag for
     * the whole mod keeps the player's NBT tidy and survives respawn.
     */
    public static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // ---- the curve ----------------------------------------------------------

    /** XP required to climb from {@code level} to {@code level + 1}; 0 at the cap. */
    public static int xpToAdvance(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return FIRST_STEP + Math.max(0, level - 1) * STEP_GROWTH;
    }

    /** Lifetime XP a player must have banked to sit at exactly {@code level}. */
    public static int totalXpForLevel(int level) {
        int steps = Mth.clamp(level, 1, MAX_LEVEL) - 1;
        // arithmetic series: `steps` terms from FIRST_STEP, each STEP_GROWTH bigger
        return steps * FIRST_STEP + STEP_GROWTH * (steps * (steps - 1)) / 2;
    }

    /** The level a given lifetime total buys. Clamped to {@link #MAX_LEVEL}. */
    public static int levelForXp(int totalXp) {
        int level = 1;
        while (level < MAX_LEVEL && totalXp >= totalXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    // ---- queries (safe on both sides; the GUI uses the client mirror) --------

    public static int getTotalXp(Player player) {
        return data(player).getInt(KEY_XP);
    }

    public static int getLevel(Player player) {
        return levelForXp(getTotalXp(player));
    }

    /** XP banked since the current level began. 0 once maxed. */
    public static int getXpIntoLevel(Player player) {
        int level = getLevel(player);
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return getTotalXp(player) - totalXpForLevel(level);
    }

    /** XP still owed for the next level. 0 once maxed. */
    public static int getXpForNextLevel(Player player) {
        return xpToAdvance(getLevel(player));
    }

    /** 0..1 progress through the current level; 1 once maxed, so bars render full. */
    public static float getProgress(Player player) {
        int need = getXpForNextLevel(player);
        if (need <= 0) {
            return 1.0F;
        }
        return Mth.clamp(getXpIntoLevel(player) / (float) need, 0.0F, 1.0F);
    }

    public static boolean isMaxed(Player player) {
        return getLevel(player) >= MAX_LEVEL;
    }

    // ---- awarding -----------------------------------------------------------

    /**
     * Bank XP and handle every level crossed by it. Server-side only; silently
     * ignores non-positive awards so callers can hand it a computed bonus
     * without guarding first.
     *
     * @param reason short lowercase phrase shown to the player, e.g. "Cayden ate"
     */
    public static void award(Player player, int amount, String reason) {
        if (player.level().isClientSide || amount <= 0 || !(player instanceof ServerPlayer server)) {
            return;
        }
        CompoundTag tag = data(player);
        int before = tag.getInt(KEY_XP);
        int after = before + amount;
        tag.putInt(KEY_XP, after);

        Component blip = Component.literal(ChatFormatting.LIGHT_PURPLE + "+" + amount
                + " Krave XP " + ChatFormatting.GRAY + "(" + reason + ")");
        if (amount >= CHAT_THRESHOLD) {
            server.sendSystemMessage(blip);
        } else {
            // small drips (a bowl of Krave, one joint) would bury the chat log
            server.displayClientMessage(blip, true);
        }

        int oldLevel = levelForXp(before);
        int newLevel = levelForXp(after);
        if (newLevel > oldLevel) {
            announceLevels(server, oldLevel, newLevel);
        }
        sync(server);
    }

    /**
     * Walk every level the player just crossed so a single fat award (finishing
     * the questline, say) still announces each perk it unlocked instead of only
     * the last one.
     */
    private static void announceLevels(ServerPlayer player, int oldLevel, int newLevel) {
        CompoundTag tag = data(player);
        int announced = Math.max(tag.getInt(KEY_ANNOUNCED), oldLevel);

        for (int level = announced + 1; level <= newLevel; level++) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "KRAVE LEVEL " + level + ChatFormatting.RESET + ChatFormatting.YELLOW
                    + " - " + titleFor(level)));
            Perks.Perk unlocked = Perks.unlockedAt(level);
            if (unlocked != null) {
                player.sendSystemMessage(Component.literal(ChatFormatting.AQUA + "  Perk unlocked: "
                        + ChatFormatting.WHITE + unlocked.title
                        + ChatFormatting.GRAY + " - " + unlocked.blurb));
            }
        }
        tag.putInt(KEY_ANNOUNCED, newLevel);

        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.9F, 1.0F);

        for (int milestone : new int[] { 5, 10, 20, 30 }) {
            if (oldLevel < milestone && newLevel >= milestone) {
                KraveAdvancements.grant(player, "level_" + milestone);
            }
        }
    }

    /** Flavour rank shown next to the number. Pure cosmetics; the GUI can reuse it. */
    public static String titleFor(int level) {
        if (level >= 30) {
            return "I KRAVE THE KRAVE";
        } else if (level >= 26) {
            return "Kosmonaut";
        } else if (level >= 21) {
            return "Krave Monster Slayer";
        } else if (level >= 16) {
            return "Cayden's Keeper";
        } else if (level >= 11) {
            return "Certified Krave Head";
        } else if (level >= 6) {
            return "Lawn Chemist";
        } else if (level >= 3) {
            return "Grass Toucher";
        }
        return "Fresh Off The Couch";
    }

    // ---- networking ---------------------------------------------------------

    /** Push this player's level, XP and stats to their own client for the HUD. */
    public static void sync(ServerPlayer player) {
        com.barbarajones.net.ModNetwork.sendTo(player,
                new com.barbarajones.net.PacketKraveLevel(
                        getLevel(player),
                        getXpIntoLevel(player),
                        getXpForNextLevel(player),
                        getTotalXp(player),
                        PlayerStats.snapshot(player)));
    }
}
