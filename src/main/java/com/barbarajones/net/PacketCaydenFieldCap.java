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
 * Sets how far Cayden may go against ordinary mobs.
 *
 * <p>Client to server, like the ascension purchase - the screen can only ask,
 * and the server decides. It re-checks ownership, range and the unlock on this
 * side rather than trusting the screen, because a packet is not a promise: an
 * open screen survives a teleport, and nothing stops a modified client sending
 * whatever it likes.
 */
public class PacketCaydenFieldCap {

    /** How close the owner must be, matching the ascension packet. */
    private static final double REACH = 24.0D;

    public final int entityId;
    public final int cap;

    public PacketCaydenFieldCap(int entityId, int cap) {
        this.entityId = entityId;
        this.cap = cap;
    }

    public static void encode(PacketCaydenFieldCap msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.cap);
    }

    public static PacketCaydenFieldCap decode(FriendlyByteBuf buf) {
        return new PacketCaydenFieldCap(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PacketCaydenFieldCap msg, Supplier<NetworkEvent.Context> ctx) {
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
                        + "He is not yours to give orders to."));
                return;
            }
            if (cayden.distanceToSqr(sender) > REACH * REACH) {
                sender.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "You have to be standing with him."));
                return;
            }
            if (!cayden.isFieldCapUnlocked()) {
                sender.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "Put the Krave Monster down first. All of him."));
                return;
            }

            cayden.setFieldCap(msg.cap);
            int now = cayden.getFieldCap();
            sender.sendSystemMessage(Component.literal(now <= 0
                    ? ChatFormatting.GRAY + "Cayden will fight ordinary mobs as himself. Watch him."
                    : ChatFormatting.GOLD + "Against ordinary mobs, Cayden goes "
                            + com.barbarajones.progression.AscensionLadder.nameOf(now) + "."));
        });
        context.setPacketHandled(true);
    }

    /** Client-side convenience so the screen never touches the channel itself. */
    public static void set(int entityId, int cap) {
        ModNetwork.CHANNEL.sendToServer(new PacketCaydenFieldCap(entityId, cap));
    }
}
