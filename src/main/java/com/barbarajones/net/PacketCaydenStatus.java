package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> owner heartbeat carrying Cayden's vitals.
 *
 * This exists rather than reading his health straight off the client world
 * because the client only knows about entities inside its tracking range. He
 * can be housed hundreds of blocks away and quietly losing a fight, which is
 * exactly the moment the alarm needs to reach you.
 */
public class PacketCaydenStatus {

    public final float health;
    public final float maxHealth;
    public final int distance;
    public final boolean housed;

    public PacketCaydenStatus(float health, float maxHealth, int distance, boolean housed) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.distance = distance;
        this.housed = housed;
    }

    public static void encode(PacketCaydenStatus msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.health);
        buf.writeFloat(msg.maxHealth);
        buf.writeInt(msg.distance);
        buf.writeBoolean(msg.housed);
    }

    public static PacketCaydenStatus decode(FriendlyByteBuf buf) {
        return new PacketCaydenStatus(buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(PacketCaydenStatus msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.ClientPacketHandler.handleCaydenStatus(msg)));
        ctx.get().setPacketHandled(true);
    }
}
