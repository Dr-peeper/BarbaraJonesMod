package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> owner sync of Krave level, XP and the stat counters.
 *
 * <p>All of it lives in the player's persistent NBT server-side, which the
 * client never sees, so the HUD would otherwise have nothing to draw. Sent on
 * login, on respawn, on dimension change and whenever XP is awarded.
 *
 * <p>The stat block is length-prefixed rather than fixed-width so a client and
 * server that disagree about how many counters exist degrade to "show the ones
 * we both know about" instead of desyncing the whole buffer.
 */
public class PacketKraveLevel {

    public final int level;
    public final int xpIntoLevel;
    public final int xpForNextLevel;
    public final int totalXp;
    public final int[] stats;

    public PacketKraveLevel(int level, int xpIntoLevel, int xpForNextLevel, int totalXp, int[] stats) {
        this.level = level;
        this.xpIntoLevel = xpIntoLevel;
        this.xpForNextLevel = xpForNextLevel;
        this.totalXp = totalXp;
        this.stats = stats;
    }

    public static void encode(PacketKraveLevel msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.level);
        buf.writeInt(msg.xpIntoLevel);
        buf.writeInt(msg.xpForNextLevel);
        buf.writeInt(msg.totalXp);
        buf.writeVarInt(msg.stats.length);
        for (int value : msg.stats) {
            buf.writeVarInt(value);
        }
    }

    public static PacketKraveLevel decode(FriendlyByteBuf buf) {
        int level = buf.readInt();
        int xpIntoLevel = buf.readInt();
        int xpForNextLevel = buf.readInt();
        int totalXp = buf.readInt();
        int count = buf.readVarInt();
        int[] stats = new int[count];
        for (int i = 0; i < count; i++) {
            stats[i] = buf.readVarInt();
        }
        return new PacketKraveLevel(level, xpIntoLevel, xpForNextLevel, totalXp, stats);
    }

    public static void handle(PacketKraveLevel msg, Supplier<NetworkEvent.Context> ctx) {
        // DistExecutor keeps the client-only class off the dedicated server's classpath
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.KraveLevelClient.accept(msg)));
        ctx.get().setPacketHandled(true);
    }
}
