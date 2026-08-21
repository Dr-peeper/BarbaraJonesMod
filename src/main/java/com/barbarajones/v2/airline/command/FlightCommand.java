package com.barbarajones.v2.airline.command;

import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.FlightScheduler;
import com.barbarajones.v2.airline.WorldLocationData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class FlightCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("flight")
            .then(Commands.literal("create")
                .then(Commands.argument("flightId", StringArgumentType.word())
                    .then(Commands.argument("departure", StringArgumentType.word())
                        .then(Commands.argument("arrival", StringArgumentType.word())
                            .then(Commands.argument("time", StringArgumentType.word())
                                .executes(ctx -> createFlight(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "flightId"),
                                    StringArgumentType.getString(ctx, "departure"),
                                    StringArgumentType.getString(ctx, "arrival"),
                                    StringArgumentType.getString(ctx, "time")
                                ))
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> listFlights(ctx.getSource()))
            )
            .then(Commands.literal("cancel")
                .then(Commands.argument("flightId", StringArgumentType.word())
                    .executes(ctx -> cancelFlight(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "flightId")
                    ))
                )
            )
        );
    }

    private static int createFlight(CommandSourceStack source, String flightId, String departureCode,
                                    String arrivalCode, String timeStr) {
        try {
            WorldLocationData.Location depLoc = WorldLocationData.getLocation(departureCode);
            WorldLocationData.Location arrLoc = WorldLocationData.getLocation(arrivalCode);

            if (depLoc == null) {
                source.sendFailure(Component.literal("Unknown departure airport: " + departureCode));
                return 0;
            }
            if (arrLoc == null) {
                source.sendFailure(Component.literal("Unknown arrival airport: " + arrivalCode));
                return 0;
            }

            int time = Integer.parseInt(timeStr);
            FlightData flight = new FlightData(
                flightId,
                depLoc.cityName,
                arrLoc.cityName,
                depLoc.x, depLoc.y, depLoc.z,
                arrLoc.x, arrLoc.y, arrLoc.z,
                time,
                "BOEING747"
            );

            FlightScheduler.getInstance().addFlight(flight);
            source.sendSuccess(
                () -> Component.literal("§aFlight " + flightId + " created: " +
                    departureCode + " -> " + arrivalCode + " at tick " + time),
                true
            );
            return 1;
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("Invalid time format (must be integer)"));
            return 0;
        }
    }

    private static int listFlights(CommandSourceStack source) {
        FlightScheduler scheduler = FlightScheduler.getInstance();
        var flights = scheduler.getAllFlights();

        if (flights.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active flights"), false);
            return 0;
        }

        for (FlightData flight : flights) {
            String info = String.format(
                "§6%s§r: %s -> %s (State: %s, Passengers: %d/%d)",
                flight.flightId,
                flight.departureCity,
                flight.arrivalCity,
                flight.state.name(),
                flight.currentPassengers,
                flight.maxCapacity
            );
            source.sendSuccess(() -> Component.literal(info), false);
        }

        return flights.size();
    }

    private static int cancelFlight(CommandSourceStack source, String flightId) {
        FlightScheduler scheduler = FlightScheduler.getInstance();
        scheduler.cancelFlight(flightId);
        source.sendSuccess(
            () -> Component.literal("§aFlight " + flightId + " cancelled"),
            true
        );
        return 1;
    }
}
