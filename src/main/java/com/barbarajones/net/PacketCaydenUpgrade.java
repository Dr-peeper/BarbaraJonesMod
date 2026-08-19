package com.barbarajones.net;

import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: "buy this rung of Cayden's ladder."
 *
 * <p>The upgrade screen already knows his ki, his unlock mask and every price -
 * all of that rides on his synched entity data - so the only thing that has to
 * cross the wire is which rung the player pressed. Everything is re-checked
 * server side; the packet is a request, never an instruction.
 *
 * <p>Follows {@link PacketCaydenStatus} exactly, only in the other direction,
 * which is why the handler pulls a sender out of the context instead of hopping
 * onto the client through DistExecutor.
 */
public class PacketCaydenUpgrade {

    /** How close the buyer has to be. A screen left open across a teleport must not pay out. */
    private static final double REACH = 24.0D;

    public final int entityId;
    public final int tier;

    public PacketCaydenUpgrade(int entityId, int tier) {
        this.entityId = entityId;
        this.tier = tier;
    }

    public static void encode(PacketCaydenUpgrade msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.tier);
    }

    public static PacketCaydenUpgrade decode(FriendlyByteBuf buf) {
        return new PacketCaydenUpgrade(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PacketCaydenUpgrade msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity found = sender.level().getEntity(msg.entityId);
            if (!(found instanceof CaydenCobb cayden) || !cayden.isAlive()) {
                return;
            }
            if (!cayden.isOwnedBy(sender)) {
                sender.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "He is not yours to train."));
                return;
            }
            if (cayden.distanceToSqr(sender) > REACH * REACH) {
                sender.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "You have to be standing with him."));
                return;
            }
            cayden.tryUnlock(msg.tier, sender);
        });
        context.setPacketHandled(true);
    }

    /** Client-side convenience so the screen never has to touch the channel itself. */
    public static void buy(int entityId, int tier) {
        ModNetwork.CHANNEL.sendToServer(new PacketCaydenUpgrade(entityId, tier));
    }
}
