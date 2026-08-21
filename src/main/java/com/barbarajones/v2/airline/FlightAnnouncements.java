package com.barbarajones.v2.airline;

import com.barbarajones.v2.airline.WorldLocationData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class FlightAnnouncements {

    private static final String[] BOARDING_ANNOUNCEMENTS = {
        "Now boarding all passengers for flight %s to %s",
        "Boarding begins for flight %s with service to %s",
        "We are ready to begin boarding flight %s",
        "Flight %s is now ready for boarding"
    };

    private static final String[] DEPARTURE_ANNOUNCEMENTS = {
        "Flight %s is now pushing back from the gate",
        "We are ready for immediate pushback of flight %s",
        "Flight %s, you are cleared for pushback",
        "Preparing flight %s for departure to %s"
    };

    private static final String[] INFLIGHT_ANNOUNCEMENTS = {
        "Ladies and gentlemen, flight %s is now at cruising altitude",
        "Flight %s has reached cruise altitude. Enjoy the flight to %s",
        "We are maintaining a cruising altitude for flight %s to %s",
        "Thank you for flying. Flight %s is currently en route to %s"
    };

    private static final String[] DESCENT_ANNOUNCEMENTS = {
        "Ladies and gentlemen, we are now beginning our descent into %s",
        "Flight %s is now descending for arrival at %s",
        "We will be landing at %s in approximately 10 minutes",
        "Prepare for descent into %s"
    };

    private static final String[] LANDING_ANNOUNCEMENTS = {
        "Flight %s, you are cleared to land at %s",
        "Ladies and gentlemen, welcome to %s. Flight %s is now landing",
        "We are on final approach to %s",
        "Prepare for landing at %s"
    };

    private static final String[] ARRIVAL_ANNOUNCEMENTS = {
        "Ladies and gentlemen, we have arrived at %s. Thank you for flying",
        "Flight %s has safely arrived at %s",
        "Welcome to %s. Local time is %s",
        "Thank you for choosing our airline. Flight %s is now at the gate in %s"
    };

    private static final Random RANDOM = new Random();

    public static void announceBoarding(FlightData flight, ServerLevel level) {
        String announcement = BOARDING_ANNOUNCEMENTS[RANDOM.nextInt(BOARDING_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.flightId, flight.arrivalCity), level);
    }

    public static void announceDeparture(FlightData flight, ServerLevel level) {
        String announcement = DEPARTURE_ANNOUNCEMENTS[RANDOM.nextInt(DEPARTURE_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.flightId, flight.arrivalCity), level);
    }

    public static void announceInFlight(FlightData flight, ServerLevel level) {
        String announcement = INFLIGHT_ANNOUNCEMENTS[RANDOM.nextInt(INFLIGHT_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.flightId, flight.arrivalCity), level);
    }

    public static void announceDescent(FlightData flight, ServerLevel level) {
        String announcement = DESCENT_ANNOUNCEMENTS[RANDOM.nextInt(DESCENT_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.arrivalCity), level);
    }

    public static void announceLanding(FlightData flight, ServerLevel level) {
        String announcement = LANDING_ANNOUNCEMENTS[RANDOM.nextInt(LANDING_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.flightId, flight.arrivalCity), level);
    }

    public static void announceArrival(FlightData flight, ServerLevel level) {
        String announcement = ARRIVAL_ANNOUNCEMENTS[RANDOM.nextInt(ARRIVAL_ANNOUNCEMENTS.length)];
        broadcastToPassengers(flight, String.format(announcement, flight.arrivalCity), level);
    }

    private static void broadcastToPassengers(FlightData flight, String message, ServerLevel level) {
        PassengerManager passengers = PassengerManager.getInstance();
        for (java.util.UUID passengerId : passengers.getFlightPassengers(flight.flightId)) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(passengerId);
            if (player != null) {
                player.displayClientMessage(
                    Component.literal("§e[Flight " + flight.flightId + "]§r " + message),
                    false
                );
            }
        }
    }

    public static String[] getRandomSafetyMessage() {
        String[] messages = {
            "This is a non-smoking flight. Smoking is prohibited throughout the aircraft",
            "In the unlikely event of decompression, oxygen masks will automatically drop",
            "Please keep your seatbelt fastened during the flight",
            "Emergency exits are located at the front and rear of the aircraft",
            "Flight attendants may move about the cabin during flight"
        };
        return new String[]{messages[RANDOM.nextInt(messages.length)]};
    }

    public static void announceWeather(FlightData flight, ServerLevel level) {
        String[] weatherConditions = {
            "We're expecting some light turbulence",
            "Current weather at our destination looks clear",
            "We're cruising at optimal altitude to avoid weather",
            "The flight today is expected to be smooth"
        };
        String message = weatherConditions[RANDOM.nextInt(weatherConditions.length)];
        broadcastToPassengers(flight, message, level);
    }
}
