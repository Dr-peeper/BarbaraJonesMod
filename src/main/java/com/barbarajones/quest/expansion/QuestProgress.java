package com.barbarajones.quest.expansion;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * The counters behind the expansion's event quests - joints smoked, lighters
 * extorted out of Daniel, minutes spent floating over an ocean of chocolate.
 *
 * <p>Everything lives in one compound nested inside the player's
 * {@link Player#PERSISTED_NBT_TAG} bag, which is the one part of player data that
 * survives death and dimension changes. Keeping it under a single {@code KraveExp}
 * key means none of these counters can ever collide with the flags EventHandler
 * already stores next to them (KraveDeathStage, DuhlLastDay, KraveEndless...).
 */
public final class QuestProgress {

    private QuestProgress() { }

    private static final String ROOT = "KraveExp";

    // ---- counter keys -------------------------------------------------------

    public static final String JOINTS_SMOKED   = "JointsSmoked";
    public static final String BARBARA_FED     = "BarbaraFed";
    public static final String CAYDEN_MEDIC    = "CaydenMedic";
    public static final String CAYDEN_DAYS     = "CaydenDays";
    public static final String CAYDEN_DAY_MARK = "CaydenDayMark";
    public static final String KOSMOS_TICKS    = "KosmosTicks";
    public static final String KOSMOS_DIST     = "KosmosDist";
    public static final String KOSMOS_X        = "KosmosX";
    public static final String KOSMOS_Z        = "KosmosZ";
    public static final String SEWER_TICKS     = "SewerTicks";
    public static final String FAKES_BURNED    = "FakesBurned";
    public static final String PLUGS_KILLED    = "PlugsKilled";
    public static final String PLUG_DEALS      = "PlugDeals";
    public static final String LIGHTERS        = "Lighters";
    public static final String NUGGET_TREATS   = "NuggetTreats";
    public static final String DUHL_PAYMENTS   = "DuhlPayments";
    public static final String DUHL_DAYS       = "DuhlDays";
    public static final String DUHL_DAY_MARK   = "DuhlDayMark";
    public static final String CEREAL_EATEN    = "CerealEaten";
    public static final String PIBB_DRUNK      = "PibbDrunk";
    public static final String NUGGETS_EATEN   = "NuggetsEaten";

    // ---- cast-call flags (each of these spawns a character exactly once) ----

    public static final String CAST_BARBARA = "CastBarbara";
    public static final String CAST_DANIEL  = "CastDaniel";
    public static final String CAST_NUGGET  = "CastNugget";
    public static final String CAST_MOM     = "CastMom";

    /**
     * The mod's own corner of the player's persistent data, created on demand.
     * {@code getCompound} hands back the live instance once it exists, so writes
     * through the returned tag stick without having to put it back.
     */
    public static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    public static int get(Player player, String key) {
        return data(player).getInt(key);
    }

    public static void set(Player player, String key, int value) {
        data(player).putInt(key, value);
    }

    /** Adds to a counter and returns the new total. */
    public static int bump(Player player, String key, int by) {
        CompoundTag tag = data(player);
        int now = tag.getInt(key) + by;
        tag.putInt(key, now);
        return now;
    }

    public static boolean flag(Player player, String key) {
        return data(player).getBoolean(key);
    }

    public static void setFlag(Player player, String key) {
        data(player).putBoolean(key, true);
    }

    /**
     * True the first time this is called on a given in-game day, false for the
     * rest of it. Used for "on N different days" quests, where two payments in
     * one afternoon must only ever count once.
     */
    public static boolean newDay(Player player, String markKey, long day) {
        CompoundTag tag = data(player);
        if (tag.contains(markKey) && tag.getLong(markKey) == day) {
            return false;
        }
        tag.putLong(markKey, day);
        return true;
    }
}
