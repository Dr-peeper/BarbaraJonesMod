package com.barbarajones.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * The counters worth bragging about (and the ones worth being ashamed of).
 *
 * <p>Every counter is a plain int in the mod's persistent NBT sub-tag, so it
 * rides through death and respawn exactly like the Krave level does. The enum
 * is the single source of truth for the NBT key AND the label the GUI prints,
 * which means a stat can never end up displayed under one name and saved under
 * another.
 *
 * <p>Counters only ever go up. Nothing in the mod subtracts from them, so a
 * client-side mirror can be trusted between syncs.
 */
public final class PlayerStats {

    /** One tracked counter. Declaration order is the packet's wire order - append only. */
    public enum Stat {
        KRAVE_FED("KraveStatFed", "Bowls of Krave fed to Cayden"),
        CAYDEN_DEATHS("KraveStatCaydenDeaths", "Times you broke Rule #1"),
        APOCALYPSES_SURVIVED("KraveStatApocalypse", "Krave Apocalypses survived"),
        BOSSES_KILLED("KraveStatBosses", "Bosses put in the ground"),
        GRASS_SMOKED("KraveStatSmoked", "Grass smoked"),
        DOLLARS_SCAMMED("KraveStatScammed", "Dollars handed to The Plug"),
        QUESTS_DONE("KraveStatQuests", "Quests completed"),
        KOSMOS_TRIPS("KraveStatKosmos", "Trips to the Krave Kosmos"),
        PLUG_DEALS("KraveStatPlugDeals", "Deals done on the street");

        /** NBT key inside the mod's persistent sub-tag. */
        public final String key;
        /** Player-facing label. Plain English; the GUI prints it verbatim. */
        public final String label;

        Stat(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    private PlayerStats() { }

    public static int get(Player player, Stat stat) {
        return KraveLevel.data(player).getInt(stat.key);
    }

    /** Bump a counter. Non-positive deltas are ignored - these only ever climb. */
    public static void add(Player player, Stat stat, int delta) {
        if (player.level().isClientSide || delta <= 0) {
            return;
        }
        CompoundTag tag = KraveLevel.data(player);
        tag.putInt(stat.key, tag.getInt(stat.key) + delta);
    }

    /** True the first time a counter is about to leave zero - handy for "first ever" rewards. */
    public static boolean isFirst(Player player, Stat stat) {
        return get(player, stat) == 0;
    }

    /** Every counter in {@link Stat} order, for the sync packet and the GUI. */
    public static int[] snapshot(Player player) {
        Stat[] all = Stat.values();
        int[] values = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            values[i] = get(player, all[i]);
        }
        return values;
    }

    /**
     * A one-line summary for chat / tooltips. Deliberately blunt about the
     * Cayden death count, because that is the number that matters.
     */
    public static String summary(Player player) {
        return "Level " + KraveLevel.getLevel(player) + " (" + KraveLevel.titleFor(KraveLevel.getLevel(player))
                + ") - " + get(player, Stat.KRAVE_FED) + " Krave fed, "
                + get(player, Stat.APOCALYPSES_SURVIVED) + " apocalypses survived, "
                + get(player, Stat.CAYDEN_DEATHS) + " Cayden deaths.";
    }
}
