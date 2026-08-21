package com.barbarajones.v2.airline;

import net.minecraft.world.entity.player.Player;
import java.util.*;

public class PassengerManager {
    private static PassengerManager instance;
    private Map<String, Set<UUID>> flightPassengers = new HashMap<>();
    private Map<UUID, String> playerFlightAssignment = new HashMap<>();
    private Map<UUID, String> playerSeatAssignment = new HashMap<>();

    public static PassengerManager getInstance() {
        if (instance == null) {
            instance = new PassengerManager();
        }
        return instance;
    }

    public void boardPassenger(Player player, String flightId, String seatNumber) {
        UUID playerUUID = player.getUUID();

        // Remove from previous flight if any
        if (playerFlightAssignment.containsKey(playerUUID)) {
            String previousFlight = playerFlightAssignment.get(playerUUID);
            Set<UUID> passengers = flightPassengers.get(previousFlight);
            if (passengers != null) {
                passengers.remove(playerUUID);
            }
        }

        // Add to new flight
        flightPassengers.computeIfAbsent(flightId, k -> new HashSet<>()).add(playerUUID);
        playerFlightAssignment.put(playerUUID, flightId);
        playerSeatAssignment.put(playerUUID, seatNumber);

        // Update flight passenger count
        FlightData flight = FlightScheduler.getInstance().getFlight(flightId);
        if (flight != null) {
            flight.currentPassengers++;
        }
    }

    public void deboardPassenger(Player player) {
        UUID playerUUID = player.getUUID();

        if (playerFlightAssignment.containsKey(playerUUID)) {
            String flightId = playerFlightAssignment.get(playerUUID);
            Set<UUID> passengers = flightPassengers.get(flightId);
            if (passengers != null) {
                passengers.remove(playerUUID);
            }

            // Update flight passenger count
            FlightData flight = FlightScheduler.getInstance().getFlight(flightId);
            if (flight != null && flight.currentPassengers > 0) {
                flight.currentPassengers--;
            }

            playerFlightAssignment.remove(playerUUID);
            playerSeatAssignment.remove(playerUUID);
        }
    }

    public String getPlayerFlight(Player player) {
        return playerFlightAssignment.getOrDefault(player.getUUID(), "");
    }

    public String getPlayerSeat(Player player) {
        return playerSeatAssignment.getOrDefault(player.getUUID(), "");
    }

    public boolean isPlayerBoarded(Player player) {
        return playerFlightAssignment.containsKey(player.getUUID());
    }

    public Set<UUID> getFlightPassengers(String flightId) {
        return flightPassengers.getOrDefault(flightId, new HashSet<>());
    }

    public int getFlightPassengerCount(String flightId) {
        return flightPassengers.getOrDefault(flightId, new HashSet<>()).size();
    }

    public void clearFlight(String flightId) {
        Set<UUID> passengers = flightPassengers.get(flightId);
        if (passengers != null) {
            passengers.forEach(uuid -> {
                playerFlightAssignment.remove(uuid);
                playerSeatAssignment.remove(uuid);
            });
            flightPassengers.remove(flightId);
        }
    }

    public String generateSeatNumber(int capacity, int passengerIndex) {
        int rows = (capacity + 5) / 6; // 6 seats per row (3-3 configuration)
        int row = (passengerIndex / 6) + 1;
        int seatInRow = (passengerIndex % 6) + 1;

        char column = switch (seatInRow) {
            case 1 -> 'A';
            case 2 -> 'B';
            case 3 -> 'C';
            case 4 -> 'D';
            case 5 -> 'E';
            case 6 -> 'F';
            default -> '?';
        };

        return row + "" + column;
    }
}
