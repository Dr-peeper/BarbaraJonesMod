package com.barbarajones.v2.abilities.net;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.v2.abilities.client.AbilityClientState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> owner: the full unlock mask plus every ability's cooldown-end and
 * active-end tick. Sent on login, respawn, dimension change and every
 * activation - see {@code AbilityEvents} and {@code AbilityData#activate}.
 *
 * <p>Fixed-width ({@link AbilityId#COUNT} longs each way) rather than
 * length-prefixed like other modules' stat blocks: this module owns both
 * ends of the wire format, so there is no risk of a client and server
 * disagreeing about how many abilities exist.
 *
 * <p>The handler touches only {@link AbilityClientState}, which has no
 * {@code net.minecraft.client} imports at all, so this can run straight off
 * the network thread's {@code enqueueWork} with no {@code DistExecutor} - a
 * server never receives this packet in the first place (it only ever sends
 * it), so there is nothing to gate.
 */
public class PacketAbilitySync {

    public final long now;
    public final int mask;
    public final long[] cooldownEnd;
    public final long[] activeEnd;

    public PacketAbilitySync(long now, int mask, long[] cooldownEnd, long[] activeEnd) {
        this.now = now;
        this.mask = mask;
        this.cooldownEnd = cooldownEnd;
        this.activeEnd = activeEnd;
    }

    public static void encode(PacketAbilitySync msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.now);
        buf.writeInt(msg.mask);
        for (int i = 0; i < AbilityId.COUNT; i++) {
            buf.writeLong(msg.cooldownEnd[i]);
        }
        for (int i = 0; i < AbilityId.COUNT; i++) {
            buf.writeLong(msg.activeEnd[i]);
        }
    }

    public static PacketAbilitySync decode(FriendlyByteBuf buf) {
        long now = buf.readLong();
        int mask = buf.readInt();
        long[] cd = new long[AbilityId.COUNT];
        for (int i = 0; i < AbilityId.COUNT; i++) {
            cd[i] = buf.readLong();
        }
        long[] act = new long[AbilityId.COUNT];
        for (int i = 0; i < AbilityId.COUNT; i++) {
            act[i] = buf.readLong();
        }
        return new PacketAbilitySync(now, mask, cd, act);
    }

    public static void handle(PacketAbilitySync msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> AbilityClientState.accept(msg.mask, msg.now, msg.cooldownEnd, msg.activeEnd));
        context.setPacketHandled(true);
    }
}
