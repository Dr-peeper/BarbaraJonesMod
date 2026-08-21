package com.barbarajones.v2.airline.network;

import com.barbarajones.v2.airline.PassengerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeboardFlightPacket {

    public DeboardFlightPacket() {
    }

    public DeboardFlightPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(DeboardFlightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                PassengerManager.getInstance().deboardPassenger(player);
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§aYou have deboarded the flight"),
                    false
                );
            }
        });
    }
}
