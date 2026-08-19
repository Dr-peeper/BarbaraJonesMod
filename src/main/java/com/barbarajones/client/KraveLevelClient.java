package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.net.PacketKraveLevel;
import com.barbarajones.progression.KraveLevel;
import com.barbarajones.progression.PlayerStats;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side mirror of the local player's Krave progression.
 *
 * <p>Deliberately draws nothing. It is a data holder the HUD and any menu can
 * read statically, because the numbers themselves only exist in server-side
 * persistent NBT - see {@link PacketKraveLevel}. Anything that wants to render
 * a level bar or a stats page calls the getters here.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveLevelClient {

    /** How long the "you just levelled" flash stays hot, in client ticks. */
    public static final int FLASH_TICKS = 60;

    private static int level = 1;
    private static int xpIntoLevel;
    private static int xpForNextLevel;
    private static int totalXp;
    private static int[] stats = new int[0];

    /** Ticks since the last level increase, or -1 if we have not seen one. */
    private static int sinceLevelUp = -1;
    /** False until the first packet lands, so the HUD can stay hidden pre-login. */
    private static boolean received;

    private KraveLevelClient() { }

    public static void accept(PacketKraveLevel msg) {
        if (msg.level > level && received) {
            sinceLevelUp = 0;
        }
        level = msg.level;
        xpIntoLevel = msg.xpIntoLevel;
        xpForNextLevel = msg.xpForNextLevel;
        totalXp = msg.totalXp;
        stats = msg.stats;
        received = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && sinceLevelUp >= 0 && sinceLevelUp <= FLASH_TICKS) {
            sinceLevelUp++;
        }
    }

    // ---- queries for the HUD / menus ---------------------------------------

    /** Whether the client has ever heard from the server about this player. */
    public static boolean hasData() {
        return received;
    }

    public static int getLevel() {
        return level;
    }

    public static int getXpIntoLevel() {
        return xpIntoLevel;
    }

    public static int getXpForNextLevel() {
        return xpForNextLevel;
    }

    public static int getTotalXp() {
        return totalXp;
    }

    public static boolean isMaxed() {
        return level >= KraveLevel.MAX_LEVEL;
    }

    /** 0..1 through the current level; 1 at the cap so bars render full, not empty. */
    public static float getProgress() {
        if (xpForNextLevel <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, xpIntoLevel / (float) xpForNextLevel));
    }

    public static String getTitle() {
        return KraveLevel.titleFor(level);
    }

    /**
     * A tracked counter, or 0 if this client's build knows about a stat the
     * server has not sent (see the length-prefixed stat block in the packet).
     */
    public static int getStat(PlayerStats.Stat stat) {
        int index = stat.ordinal();
        return index < stats.length ? stats[index] : 0;
    }

    /** True while the level-up flash should be drawn. */
    public static boolean isFlashing() {
        return sinceLevelUp >= 0 && sinceLevelUp <= FLASH_TICKS;
    }

    /** 1 -> 0 over the flash window; 0 when not flashing. For fading effects. */
    public static float flashStrength() {
        if (!isFlashing()) {
            return 0.0F;
        }
        return 1.0F - (sinceLevelUp / (float) FLASH_TICKS);
    }
}
