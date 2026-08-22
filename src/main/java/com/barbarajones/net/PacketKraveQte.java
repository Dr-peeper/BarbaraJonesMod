package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: put a finisher prompt up, or take it down.
 *
 * <p>Carries only what the prompt needs to draw itself and to answer with. The
 * client is told which key to show and which step it belongs to; it is never
 * told what pressing it means, and it never decides whether the press counted.
 * See {@link PacketKraveQteInput} for the other direction.
 */
public class PacketKraveQte {

    /** Ticks the prompt has left, or zero to clear it. */
    private final int ticks;
    /** Which form is being finished. */
    private final int form;
    /** True on a retry, so the prompt can say so rather than silently reappearing. */
    private final boolean retry;
    /** Zero-based attack within this form's sequence. Echoed back on the answer. */
    private final int step;
    /** How many attacks this form needs, so the prompt can read "2 of 4". */
    private final int totalSteps;
    /** The letter to press, chosen server-side from the move table. */
    private final String key;
    /** The move's name, so each attack is announced rather than anonymous. */
    private final String caption;

    public PacketKraveQte(int ticks, int form, boolean retry,
                          int step, int totalSteps, String key, String caption) {
        this.ticks = ticks;
        this.form = form;
        this.retry = retry;
        this.step = step;
        this.totalSteps = totalSteps;
        this.key = key;
        this.caption = caption;
    }

    public static void encode(PacketKraveQte msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.ticks);
        buf.writeVarInt(msg.form);
        buf.writeBoolean(msg.retry);
        buf.writeVarInt(msg.step);
        buf.writeVarInt(msg.totalSteps);
        buf.writeUtf(msg.key, 8);
        buf.writeUtf(msg.caption, 64);
    }

    public static PacketKraveQte decode(FriendlyByteBuf buf) {
        return new PacketKraveQte(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readUtf(8), buf.readUtf(64));
    }

    public static void handle(PacketKraveQte msg, Supplier<NetworkEvent.Context> ctx) {
        // Through DistExecutor, matching every other client-bound packet here.
        // A direct call would put a hard reference to a client-only class in a
        // method the dedicated server loads, which is the standard way to turn a
        // packet handler into a NoClassDefFoundError on a real server.
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.KraveQteClient.accept(
                        msg.ticks, msg.form, msg.retry, msg.step, msg.totalSteps,
                        msg.key, msg.caption)));
        ctx.get().setPacketHandled(true);
    }
}
