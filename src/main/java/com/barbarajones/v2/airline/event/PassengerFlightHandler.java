package com.barbarajones.v2.airline.event;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.FlightScheduler;
import com.barbarajones.v2.airline.PassengerManager;
import com.barbarajones.v2.airline.PlayerFlightStats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Carries booked players along with their flight.
 *
 * <p>Players ride as tracked cabin occupants rather than as vanilla passengers of
 * the plane entity: vanilla seating would cap the cabin at one rider and drag the
 * player through the 12-block hitbox's collision. Instead their position is
 * rewritten each tick from the same flight maths the aircraft uses, so the two can
 * never drift apart.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PassengerFlightHandler {

    /** Matches PlaneEntity: cruise sits this far above the departure field. */
    private static final double CRUISE_CLIMB = 100.0D;
    private static final double CLIMB_END = 0.20D;
    private static final double DESCENT_START = 0.85D;
    /** Where a seat sits relative to the aircraft's rendered origin. */
    private static final double CABIN_OFFSET = 1.0D;

    private PassengerFlightHandler() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            return;
        }

        PassengerManager passengers = PassengerManager.getInstance();
        FlightScheduler scheduler = FlightScheduler.getInstance();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String flightId = passengers.getPlayerFlight(player);
            if (flightId.isEmpty()) {
                continue;
            }

            FlightData flight = scheduler.getFlight(flightId);
            if (flight == null) {
                // The flight was cancelled or retired out from under them. Put them down
                // safely rather than leaving them stranded at cruise altitude.
                passengers.deboardPassenger(player);
                setDownAt(player, level, flight, player.getX(), player.getZ());
                player.displayClientMessage(
                        Component.literal("§cYour flight was cancelled."), false);
                continue;
            }

            if (flight.state == FlightData.FlightState.ARRIVED) {
                PlayerFlightStats.getOrCreate(player).recordFlight(flight);
                passengers.deboardPassenger(player);
                setDownAt(player, level, flight, flight.arrivalX + 3.0D, flight.arrivalZ);
                player.displayClientMessage(
                        Component.literal("§aWelcome to " + flight.arrivalCity + "."), false);
                continue;
            }

            double[] seat = seatPosition(flight, level.getGameTime());
            player.connection.teleport(seat[0], seat[1], seat[2], player.getYRot(), player.getXRot());
            // Nothing about riding a plane should read as falling.
            player.fallDistance = 0.0F;
        }
    }

    /** Where the cabin is right now, in world coordinates. */
    private static double[] seatPosition(FlightData flight, long gameTime) {
        double x;
        double y;
        double z;

        if (flight.state == FlightData.FlightState.FLYING) {
            double progress = flight.getFlightProgress(gameTime);
            x = Mth.lerp(progress, flight.departureX, flight.arrivalX);
            z = Mth.lerp(progress, flight.departureZ, flight.arrivalZ);

            double fieldY = Mth.lerp(progress, flight.departureY, flight.arrivalY);
            double cruiseY = fieldY + CRUISE_CLIMB;
            if (progress < CLIMB_END) {
                y = Mth.lerp(progress / CLIMB_END, fieldY, cruiseY);
            } else if (progress > DESCENT_START) {
                y = Mth.lerp((progress - DESCENT_START) / (1.0D - DESCENT_START), cruiseY, fieldY);
            } else {
                y = cruiseY;
            }
        } else if (flight.state == FlightData.FlightState.LANDING) {
            x = flight.arrivalX;
            z = flight.arrivalZ;
            y = flight.arrivalY + 2.0D;
        } else {
            x = flight.departureX;
            z = flight.departureZ;
            y = flight.departureY + 2.0D;
        }

        return new double[]{x + 0.5D, y + CABIN_OFFSET, z + 0.5D};
    }

    /** Drop a player onto solid ground rather than into the void or inside terrain. */
    private static void setDownAt(ServerPlayer player, ServerLevel level, FlightData flight,
                                  double x, double z) {
        int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mth.floor(x), Mth.floor(z));
        double y = flight == null ? surface : Math.max(surface, flight.arrivalY);
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0F;
    }
}
