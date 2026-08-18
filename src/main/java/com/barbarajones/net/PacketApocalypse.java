package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client trigger for the Krave Apocalypse cinematic. The sequence
 * lives server-side; this tells each client which beat is happening, at which
 * stage, and where - so the client can run its own rendering.
 */
public class PacketApocalypse {

    public static final int PHASE_END     = 0;
    public static final int PHASE_ONSET   = 1;
    public static final int PHASE_BLAST   = 2;
    public static final int PHASE_WRATH   = 3;
    public static final int PHASE_MISSILE = 4;

    public final int phase;
    public final int stage;
    public final int target;
    public final double x, y, z;

    public PacketApocalypse(int phase, int stage, double x, double y, double z) {
        this(phase, stage, 0, x, y, z);
    }

    public PacketApocalypse(int phase, int stage, int target, double x, double y, double z) {
        this.phase = phase; this.stage = stage; this.target = target;
        this.x = x; this.y = y; this.z = z;
    }

    public static void encode(PacketApocalypse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase);
        buf.writeInt(msg.stage);
        buf.writeInt(msg.target);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static PacketApocalypse decode(FriendlyByteBuf buf) {
        return new PacketApocalypse(buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(PacketApocalypse msg, Supplier<NetworkEvent.Context> ctx) {
        // DistExecutor keeps the client-only class off the dedicated server's classpath
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.ClientPacketHandler.handleApocalypse(msg)));
        ctx.get().setPacketHandled(true);
    }
}
