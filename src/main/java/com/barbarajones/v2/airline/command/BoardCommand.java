package com.barbarajones.v2.airline.command;

import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.FlightScheduler;
import com.barbarajones.v2.airline.PassengerManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class BoardCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("board")
            .then(Commands.argument("flightId", StringArgumentType.word())
                .executes(ctx -> boardFlight(
                    ctx.getSource(),
                    StringArgumentType.getString(ctx, "flightId")
                ))
            )
        );
    }

    private static int boardFlight(CommandSourceStack source, String flightId) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only players can board flights"));
            return 0;
        }

        FlightData flight = FlightScheduler.getInstance().getFlight(flightId);
        if (flight == null) {
            source.sendFailure(Component.literal("Flight not found: " + flightId));
            return 0;
        }

        if (flight.state != FlightData.FlightState.BOARDING) {
            source.sendFailure(Component.literal("Flight is not currently boarding"));
            return 0;
        }

        if (flight.currentPassengers >= flight.maxCapacity) {
            source.sendFailure(Component.literal("Flight is full"));
            return 0;
        }

        // Assign seat
        String seatNumber = PassengerManager.getInstance().generateSeatNumber(
            flight.maxCapacity,
            flight.currentPassengers
        );

        PassengerManager.getInstance().boardPassenger(player, flightId, seatNumber);

        source.sendSuccess(
            () -> Component.literal("§aSuccessfully boarded flight " + flightId + " | Seat: " + seatNumber),
            true
        );

        return 1;
    }
}
