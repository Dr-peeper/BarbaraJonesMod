package com.barbarajones.v2.airline;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

public class FlightScheduler {
    private static FlightScheduler instance;
    private Map<String, FlightData> activeFlights = new HashMap<>();
    private Set<String> completedFlights = new HashSet<>();
    private long lastFlightCheckTick = 0;

    public static FlightScheduler getInstance() {
        if (instance == null) {
            instance = new FlightScheduler();
        }
        return instance;
    }

    public void addFlight(FlightData flight) {
        activeFlights.put(flight.flightId, flight);
    }

    public void removeFlight(String flightId) {
        activeFlights.remove(flightId);
        completedFlights.add(flightId);
    }

    public FlightData getFlight(String flightId) {
        return activeFlights.get(flightId);
    }

    public Collection<FlightData> getAllFlights() {
        return activeFlights.values();
    }

    public List<FlightData> getFlightsByDeparture(String departureCity) {
        List<FlightData> flights = new ArrayList<>();
        for (FlightData flight : activeFlights.values()) {
            if (flight.departureCity.equals(departureCity)) {
                flights.add(flight);
            }
        }
        return flights;
    }

    public List<FlightData> getFlightsByArrival(String arrivalCity) {
        List<FlightData> flights = new ArrayList<>();
        for (FlightData flight : activeFlights.values()) {
            if (flight.arrivalCity.equals(arrivalCity)) {
                flights.add(flight);
            }
        }
        return flights;
    }

    public void updateFlights(Level level, long currentTick) {
        List<String> toRemove = new ArrayList<>();
        boolean isServerLevel = level instanceof net.minecraft.server.level.ServerLevel;
        net.minecraft.server.level.ServerLevel serverLevel = isServerLevel ? (net.minecraft.server.level.ServerLevel)level : null;

        for (FlightData flight : activeFlights.values()) {
            switch (flight.state) {
                case SCHEDULED:
                    // Check if it's time to board
                    if (currentTick >= flight.departureTime - 300) { // 15 seconds before
                        flight.state = FlightData.FlightState.BOARDING;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceBoarding(flight, serverLevel);
                        }
                    }
                    break;

                case BOARDING:
                    // Boarding complete 5 seconds before departure
                    if (currentTick >= flight.departureTime - 100) {
                        flight.state = FlightData.FlightState.TAXIING;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceDeparture(flight, serverLevel);
                        }
                    }
                    break;

                case TAXIING:
                    // Takeoff
                    if (currentTick >= flight.departureTime) {
                        flight.state = FlightData.FlightState.FLYING;
                        flight.flightStartTick = currentTick;
                    }
                    break;

                case FLYING:
                    // Check if flight is complete
                    long flightElapsed = currentTick - flight.flightStartTick;

                    // Both of these are edge-triggered on a latch rather than on a bare
                    // threshold. A `>=` test alone re-fires every tick it stays true, which
                    // is how the descent call once spammed chat for the last fifteen
                    // percent of every flight.
                    if (!flight.announcedCruise && flightElapsed >= 200) {
                        flight.announcedCruise = true;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceInFlight(flight, serverLevel);
                        }
                    }

                    if (!flight.announcedDescent && flightElapsed >= flight.scheduledDuration * 0.85) {
                        flight.announcedDescent = true;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceDescent(flight, serverLevel);
                        }
                    }

                    if (flightElapsed >= flight.scheduledDuration) {
                        flight.state = FlightData.FlightState.LANDING;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceLanding(flight, serverLevel);
                        }
                    }
                    break;

                case LANDING:
                    // After landing, wait a bit before cleanup
                    long landingElapsed = currentTick - flight.flightStartTick - flight.scheduledDuration;
                    if (landingElapsed >= 200) { // 10 seconds
                        flight.state = FlightData.FlightState.ARRIVED;
                        if (serverLevel != null) {
                            FlightAnnouncements.announceArrival(flight, serverLevel);
                        }
                        toRemove.add(flight.flightId);
                    }
                    break;

                case CANCELLED:
                    toRemove.add(flight.flightId);
                    break;

                default:
                    break;
            }
        }

        // Remove completed flights
        for (String flightId : toRemove) {
            removeFlight(flightId);
        }
    }

    public void save(CompoundTag tag) {
        ListTag flightsList = new ListTag();

        for (FlightData flight : activeFlights.values()) {
            CompoundTag flightTag = new CompoundTag();
            flight.save(flightTag);
            flightsList.add(flightTag);
        }

        tag.put("Flights", flightsList);

        ListTag completedList = new ListTag();
        for (String completed : completedFlights) {
            CompoundTag tag1 = new CompoundTag();
            tag1.putString("FlightId", completed);
            completedList.add(tag1);
        }
        tag.put("CompletedFlights", completedList);
    }

    public void load(CompoundTag tag) {
        activeFlights.clear();
        completedFlights.clear();

        ListTag flightsList = tag.getList("Flights", Tag.TAG_COMPOUND);
        for (int i = 0; i < flightsList.size(); i++) {
            CompoundTag flightTag = flightsList.getCompound(i);
            FlightData flight = FlightData.load(flightTag);
            activeFlights.put(flight.flightId, flight);
        }

        ListTag completedList = tag.getList("CompletedFlights", Tag.TAG_COMPOUND);
        for (int i = 0; i < completedList.size(); i++) {
            CompoundTag tag1 = completedList.getCompound(i);
            completedFlights.add(tag1.getString("FlightId"));
        }
    }

    public boolean isFlightCompleted(String flightId) {
        return completedFlights.contains(flightId);
    }

    public void cancelFlight(String flightId) {
        FlightData flight = activeFlights.get(flightId);
        if (flight != null) {
            flight.state = FlightData.FlightState.CANCELLED;
        }
    }

    public int getTotalPassengers() {
        int total = 0;
        for (FlightData flight : activeFlights.values()) {
            total += flight.currentPassengers;
        }
        return total;
    }
}
