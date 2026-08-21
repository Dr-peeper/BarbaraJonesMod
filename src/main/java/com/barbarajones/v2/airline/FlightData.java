package com.barbarajones.v2.airline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class FlightData {
    public String flightId;
    public String departureCity;
    public String arrivalCity;
    public int departureX, departureY, departureZ;
    public int arrivalX, arrivalY, arrivalZ;
    public int departureTime; // in world ticks
    public int scheduledDuration; // in ticks
    public int currentPassengers;
    public int maxCapacity;
    public String aircraftType; // "BOEING747", "A380", "B787", etc.
    public FlightState state;
    public int gateNumber; // which gate at departure airport
    public int arrivalGate; // which gate at arrival airport
    public long flightStartTick;

    /**
     * Latches so the cruise and descent calls are made once per flight rather than
     * once per tick for as long as their condition holds. Saved with the flight so a
     * reload mid-cruise does not replay announcements the cabin already heard.
     */
    public boolean announcedCruise;
    public boolean announcedDescent;

    public enum FlightState {
        SCHEDULED, BOARDING, TAXIING, FLYING, LANDING, ARRIVED, CANCELLED
    }

    public FlightData() {
        this.state = FlightState.SCHEDULED;
        this.maxCapacity = 300;
        this.currentPassengers = 0;
    }

    public FlightData(String flightId, String depCity, String arrCity, int depX, int depY, int depZ,
                      int arrX, int arrY, int arrZ, int depTime, String aircraftType) {
        this.flightId = flightId;
        this.departureCity = depCity;
        this.arrivalCity = arrCity;
        this.departureX = depX;
        this.departureY = depY;
        this.departureZ = depZ;
        this.arrivalX = arrX;
        this.arrivalY = arrY;
        this.arrivalZ = arrZ;
        this.departureTime = depTime;
        this.aircraftType = aircraftType;
        this.state = FlightState.SCHEDULED;

        // Calculate flight duration based on distance
        double distanceBlocks = Math.sqrt(
            Math.pow(arrX - depX, 2) +
            Math.pow(arrZ - depZ, 2)
        );
        // Cap at 5 minutes (6000 ticks), minimum 30 seconds (600 ticks)
        this.scheduledDuration = Math.min(6000, Math.max(600, (int)(distanceBlocks / 2.0)));

        // Aircraft capacity
        if (aircraftType.equals("BOEING747")) this.maxCapacity = 416;
        else if (aircraftType.equals("A380")) this.maxCapacity = 555;
        else if (aircraftType.equals("B787")) this.maxCapacity = 242;
        else if (aircraftType.equals("A320")) this.maxCapacity = 180;
        else if (aircraftType.equals("CESSNA")) this.maxCapacity = 4;
        else this.maxCapacity = 300;

        this.currentPassengers = 0;
    }

    public void save(CompoundTag tag) {
        tag.putString("FlightId", flightId);
        tag.putString("DepartureCity", departureCity);
        tag.putString("ArrivalCity", arrivalCity);
        tag.putInt("DepX", departureX);
        tag.putInt("DepY", departureY);
        tag.putInt("DepZ", departureZ);
        tag.putInt("ArrX", arrivalX);
        tag.putInt("ArrY", arrivalY);
        tag.putInt("ArrZ", arrivalZ);
        tag.putInt("DepartureTime", departureTime);
        tag.putInt("ScheduledDuration", scheduledDuration);
        tag.putInt("CurrentPassengers", currentPassengers);
        tag.putInt("MaxCapacity", maxCapacity);
        tag.putString("AircraftType", aircraftType);
        tag.putString("State", state.name());
        tag.putInt("GateNumber", gateNumber);
        tag.putInt("ArrivalGate", arrivalGate);
        tag.putLong("FlightStartTick", flightStartTick);
        tag.putBoolean("AnnouncedCruise", announcedCruise);
        tag.putBoolean("AnnouncedDescent", announcedDescent);
    }

    public static FlightData load(CompoundTag tag) {
        FlightData data = new FlightData();
        data.flightId = tag.getString("FlightId");
        data.departureCity = tag.getString("DepartureCity");
        data.arrivalCity = tag.getString("ArrivalCity");
        data.departureX = tag.getInt("DepX");
        data.departureY = tag.getInt("DepY");
        data.departureZ = tag.getInt("DepZ");
        data.arrivalX = tag.getInt("ArrX");
        data.arrivalY = tag.getInt("ArrY");
        data.arrivalZ = tag.getInt("ArrZ");
        data.departureTime = tag.getInt("DepartureTime");
        data.scheduledDuration = tag.getInt("ScheduledDuration");
        data.currentPassengers = tag.getInt("CurrentPassengers");
        data.maxCapacity = tag.getInt("MaxCapacity");
        data.aircraftType = tag.getString("AircraftType");
        data.state = FlightState.valueOf(tag.getString("State"));
        data.gateNumber = tag.getInt("GateNumber");
        data.arrivalGate = tag.getInt("ArrivalGate");
        data.flightStartTick = tag.getLong("FlightStartTick");
        data.announcedCruise = tag.getBoolean("AnnouncedCruise");
        data.announcedDescent = tag.getBoolean("AnnouncedDescent");
        return data;
    }

    public double getFlightProgress(long currentTick) {
        if (state != FlightState.FLYING) return 0;
        long elapsed = currentTick - flightStartTick;
        return Math.min(1.0, (double)elapsed / scheduledDuration);
    }

    public void updatePassengerPosition(double progress) {
        // Linear interpolation between departure and arrival
        // This will be used by the plane entity to update its position
    }
}
