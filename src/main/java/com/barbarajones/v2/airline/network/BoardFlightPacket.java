package com.barbarajones.v2.airline.network;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.FlightScheduler;
import com.barbarajones.v2.airline.PassengerManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class BoardFlightPacket {
    private String flightId;
    private String seatNumber;

    public BoardFlightPacket(String flightId, String seatNumber) {
        this.flightId = flightId;
        this.seatNumber = seatNumber;
    }

    public BoardFlightPacket(FriendlyByteBuf buf) {
        this.flightId = buf.readUtf();
        this.seatNumber = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(flightId);
        buf.writeUtf(seatNumber);
    }

    public static void handle(BoardFlightPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                // Verify boarding pass
                var boardingPass = player.containerMenu;
                // Simple validation - just check flight exists
                if (FlightScheduler.getInstance().getFlight(msg.flightId) != null) {
                    PassengerManager.getInstance().boardPassenger(player, msg.flightId, msg.seatNumber);
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§aBoarded flight " + msg.flightId + " | Seat " + msg.seatNumber),
                        false
                    );
                }
            }
        });
    }
}
