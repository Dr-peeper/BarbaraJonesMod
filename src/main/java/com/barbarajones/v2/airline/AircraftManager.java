package com.barbarajones.v2.airline;

import com.barbarajones.content.ModEntities;
import com.barbarajones.v2.airline.entity.PlaneEntity;

import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Puts an actual aircraft in the world for each flight.
 *
 * <p>This exists because the first pass shipped a {@link PlaneEntity}, a model and
 * a renderer, and then never constructed one - flights ran to completion with
 * nothing in the sky. The scheduler owns the timetable; this owns the airframe.
 *
 * <p>One plane per flight, created when boarding opens and left to retire itself:
 * {@link PlaneEntity#tick()} discards any aircraft whose flight the scheduler has
 * dropped, so there is no cleanup path to forget to call.
 */
public final class AircraftManager {

    private static AircraftManager instance;

    /** flight id -> the aircraft flying it. */
    private final Map<String, UUID> aircraft = new HashMap<>();

    private AircraftManager() { }

    public static AircraftManager getInstance() {
        if (instance == null) {
            instance = new AircraftManager();
        }
        return instance;
    }

    /**
     * Ensures a flight has exactly one aircraft.
     *
     * <p>Called every tick a flight is boarding, so the membership check is the load
     * bearing part - and it re-checks the world, not just the map, so an aircraft
     * lost to a chunk unload or a /kill is replaced rather than leaving the flight
     * permanently invisible.
     */
    public void ensureAircraft(ServerLevel level, FlightData flight) {
        UUID existing = this.aircraft.get(flight.flightId);
        if (existing != null && level.getEntity(existing) instanceof PlaneEntity plane && plane.isAlive()) {
            return;
        }

        PlaneEntity plane = ModEntities.PLANE.get().create(level);
        if (plane == null) {
            return;
        }
        plane.setFlight(flight.flightId, flight.aircraftType);
        plane.moveTo(flight.departureX + 0.5D, flight.departureY + 2.0D, flight.departureZ + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(plane);
        this.aircraft.put(flight.flightId, plane.getUUID());
    }

    /** Forgets a finished flight. The entity retires itself; this just drops the handle. */
    public void release(String flightId) {
        this.aircraft.remove(flightId);
    }

    /** The aircraft flying a given flight, if it is currently loaded. */
    public PlaneEntity getAircraft(ServerLevel level, String flightId) {
        UUID id = this.aircraft.get(flightId);
        if (id == null) {
            return null;
        }
        return level.getEntity(id) instanceof PlaneEntity plane ? plane : null;
    }
}
