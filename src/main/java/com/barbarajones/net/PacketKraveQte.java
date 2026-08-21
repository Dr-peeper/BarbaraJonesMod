package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: put the finisher prompt up, or take it down.
 *
 * <p>Carries only what the prompt needs to draw itself. The client is told to
 * show a prompt; it is never told what pressing it means, and it never decides
 * whether the press counted. See {@link PacketKraveQteInput} for the other
 * direction.
 */
public class PacketKraveQte {

    /** Ticks the prompt has left, or zero to clear it. */
    private final int ticks;
    /** Which form is being finished, for the prompt text. */
    private final int form;
    /** True on a retry, so the prompt can say so rather than silently reappearing. */
    private final boolean retry;

    public PacketKraveQte(int ticks, int form, boolean retry) {
        this.ticks = ticks;
        this.form = form;
        this.retry = retry;
    }

    public static void encode(PacketKraveQte msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ticks);
        buf.writeVarInt(msg.form);
        buf.writeBoolean(msg.retry);
    }

    public static PacketKraveQte decode(FriendlyByteBuf buf) {
        return new PacketKraveQte(buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(PacketKraveQte msg, Supplier<NetworkEvent.Context> ctx) {
        // Through DistExecutor, matching every other client-bound packet here.
        // A direct call would put a hard reference to a client-only class in a
        // method the dedicated server loads, which is the standard way to turn a
        // packet handler into a NoClassDefFoundError on a real server.
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.KraveQteClient.accept(
                        msg.ticks, msg.form, msg.retry)));
        ctx.get().setPacketHandled(true);
    }
}
