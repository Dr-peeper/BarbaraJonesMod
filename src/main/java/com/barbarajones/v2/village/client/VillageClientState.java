package com.barbarajones.v2.village.client;

import com.barbarajones.v2.village.menu.KraveTradeMenu;
import com.barbarajones.v2.village.net.PacketVillageOffers;
import com.barbarajones.v2.village.net.PacketVillageStatus;

import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

/**
 * The client's copy of village state.
 *
 * <p>Settlement data is server-only; the client has no {@code SavedData} and no
 * villages. Everything the HUD and the atlas screen draw arrives here through the
 * two server-to-client messages and is held until it is replaced. Nothing on this
 * class is authoritative - it exists to be <em>drawn</em>, and no gameplay decision
 * should ever be made from it.
 *
 * <p>Client-only by construction: it is only ever reached through
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}, so the class is never
 * loaded on a dedicated server.
 */
public final class VillageClientState {

    /** Last status the server sent. Null until the first packet arrives. */
    @Nullable
    private static PacketVillageStatus status;

    /** Game time of the last status, so a stale HUD can fade rather than lie. */
    private static long lastStatusTick;

    private VillageClientState() { }

    // ---- status --------------------------------------------------------------

    public static void acceptStatus(PacketVillageStatus msg) {
        status = msg;
        Minecraft mc = Minecraft.getInstance();
        lastStatusTick = mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** True when the player is standing inside a claim and the data is fresh. */
    public static boolean inVillage() {
        return status != null && status.inVillage && !isStale();
    }

    @Nullable
    public static PacketVillageStatus status() {
        return isStale() ? null : status;
    }

    /**
     * The server sends every two seconds while inside a claim. Anything older than
     * six seconds means we have stopped hearing from it - a dimension change, a
     * reconnect - and the HUD should stand down rather than keep showing numbers
     * that may be from a different world.
     */
    private static boolean isStale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || status == null) {
            return true;
        }
        return mc.level.getGameTime() - lastStatusTick > 120L;
    }

    /** Called on disconnect so a new world does not inherit the last one's HUD. */
    public static void clear() {
        status = null;
        lastStatusTick = 0L;
    }

    // ---- trade offers --------------------------------------------------------

    /**
     * Routes an offers packet into whichever trade menu is open.
     *
     * <p>Keyed by container id: a packet that arrives one tick after the player
     * closed the screen, or for a menu that has already been replaced, is dropped.
     * Without that check a late packet would repopulate a menu the player is no
     * longer looking at.
     */
    public static void acceptOffers(PacketVillageOffers msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!(mc.player.containerMenu instanceof KraveTradeMenu menu)) {
            return;
        }
        if (menu.containerId != msg.containerId) {
            return;
        }
        menu.setClientOffers(msg.offers, msg.tradeLevel, msg.tradeXp, msg.kraveFed, msg.profession);
    }
}
