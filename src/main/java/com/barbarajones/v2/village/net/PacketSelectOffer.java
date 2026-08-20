package com.barbarajones.v2.village.net;

import com.barbarajones.v2.village.menu.KraveTradeMenu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server: "I clicked trade number N".
 *
 * <p>Selection has to be authoritative on the server - the result slot is filled
 * server-side from the selected offer, and a client that could pick an offer the
 * server does not think exists is a duplication bug. So the client sends its index
 * and nothing else, and the server validates it against its own list.
 *
 * <p>Guarded three ways: the sender must have the matching menu open, the menu must
 * belong to the same container id, and the index must be inside the server's offer
 * list. A packet failing any of those is dropped silently rather than throwing -
 * a stale click arriving one tick after the screen closed is normal, not an attack.
 */
public class PacketSelectOffer {

    public final int containerId;
    public final int index;

    public PacketSelectOffer(int containerId, int index) {
        this.containerId = containerId;
        this.index = index;
    }

    public static void encode(PacketSelectOffer msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.index);
    }

    public static PacketSelectOffer decode(FriendlyByteBuf buf) {
        return new PacketSelectOffer(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PacketSelectOffer msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (!(sender.containerMenu instanceof KraveTradeMenu menu)) {
                return;
            }
            if (menu.containerId != msg.containerId) {
                return;
            }
            menu.selectOffer(msg.index);
        });
        context.setPacketHandled(true);
    }
}
