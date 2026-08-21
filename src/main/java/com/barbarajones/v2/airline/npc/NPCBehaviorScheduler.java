package com.barbarajones.v2.airline.npc;

import com.barbarajones.content.ModEntities;
import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.entity.AirportStaffEntity;
import com.barbarajones.v2.airline.entity.FlightAttendantEntity;
import com.barbarajones.v2.airline.entity.GateAgentEntity;
import com.barbarajones.v2.airline.entity.GroundCrewEntity;
import com.barbarajones.v2.airline.entity.PilotEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Crews a flight and keeps that crew in step with it.
 *
 * <p>Staff exist only for as long as their flight does. They are spawned once when
 * boarding opens, re-tasked as the flight moves through its phases, and discarded
 * on arrival - nothing here is persisted, because a crew without a flight has no
 * job to do and would just accumulate in the world.
 */
public final class NPCBehaviorScheduler {

    private static NPCBehaviorScheduler instance;

    /** flight id -> the staff working it. Absence of a key means "not yet crewed". */
    private final Map<String, List<AirportStaffEntity>> crews = new HashMap<>();

    private NPCBehaviorScheduler() { }

    public static NPCBehaviorScheduler getInstance() {
        if (instance == null) {
            instance = new NPCBehaviorScheduler();
        }
        return instance;
    }

    /**
     * Crews a flight, once.
     *
     * <p>The guard on the first line is the whole point of the method. The scheduler
     * calls this every tick a flight is boarding; without it, a full crew spawned
     * twenty times a second and a single flight buried the terminal in villagers.
     */
    public void spawnFlightNPCs(ServerLevel level, FlightData flight) {
        if (this.crews.containsKey(flight.flightId)) {
            return;
        }

        List<AirportStaffEntity> crew = new ArrayList<>();
        double x = flight.departureX + 0.5D;
        double y = flight.departureY + 1.0D;
        double z = flight.departureZ + 0.5D;

        PilotEntity pilot = ModEntities.PILOT.get().create(level);
        if (pilot != null) {
            pilot.moveTo(x, y, z, 0.0F, 0.0F);
            pilot.setAssignedFlight(flight.flightId);
            level.addFreshEntity(pilot);
            crew.add(pilot);
        }

        // One attendant per hundred seats, floor two, cap four - a 747 should not be
        // worked by the same single attendant as a Cessna.
        int attendants = Math.max(2, Math.min(4, flight.maxCapacity / 100));
        for (int i = 0; i < attendants; i++) {
            FlightAttendantEntity attendant = ModEntities.FLIGHT_ATTENDANT.get().create(level);
            if (attendant != null) {
                attendant.moveTo(x + i * 1.5D, y, z + 2.0D, 0.0F, 0.0F);
                attendant.setAssignedFlight(flight.flightId);
                attendant.setCurrentPhase(FlightAttendantEntity.AttendantPhase.BOARDING);
                level.addFreshEntity(attendant);
                crew.add(attendant);
            }
        }

        GateAgentEntity agent = ModEntities.GATE_AGENT.get().create(level);
        if (agent != null) {
            agent.moveTo(x - 5.0D, y, z, 0.0F, 0.0F);
            agent.setAssignedFlight(flight.flightId);
            agent.setAssignedGate(flight.gateNumber);
            agent.setBoardingActive(true);
            level.addFreshEntity(agent);
            crew.add(agent);
        }

        GroundCrewEntity ground = ModEntities.GROUND_CREW.get().create(level);
        if (ground != null) {
            ground.moveTo(x + 10.0D, y, z, 0.0F, 0.0F);
            ground.setAssignedFlight(flight.flightId);
            ground.setTask(GroundCrewEntity.GroundCrewTask.LOADING);
            level.addFreshEntity(ground);
            crew.add(ground);
        }

        this.crews.put(flight.flightId, crew);
    }

    /** Re-tasks a flight's crew for the phase it is now in. */
    public void updateFlightNPCs(ServerLevel level, FlightData flight) {
        List<AirportStaffEntity> crew = this.crews.get(flight.flightId);
        if (crew == null) {
            return;
        }

        Iterator<AirportStaffEntity> it = crew.iterator();
        while (it.hasNext()) {
            AirportStaffEntity staff = it.next();
            if (!staff.isAlive()) {
                it.remove();
                continue;
            }

            if (staff instanceof PilotEntity pilot) {
                pilot.setInCockpit(switch (flight.state) {
                    case TAXIING, FLYING, LANDING -> true;
                    default -> false;
                });
            } else if (staff instanceof FlightAttendantEntity attendant) {
                attendant.setCurrentPhase(switch (flight.state) {
                    case FLYING -> FlightAttendantEntity.AttendantPhase.IN_FLIGHT;
                    case LANDING, ARRIVED -> FlightAttendantEntity.AttendantPhase.DEPLANING;
                    case BOARDING, TAXIING -> FlightAttendantEntity.AttendantPhase.BOARDING;
                    default -> FlightAttendantEntity.AttendantPhase.WAITING;
                });
            } else if (staff instanceof GateAgentEntity agent) {
                agent.setBoardingActive(flight.state == FlightData.FlightState.BOARDING);
            } else if (staff instanceof GroundCrewEntity ground) {
                ground.setTask(switch (flight.state) {
                    case BOARDING -> GroundCrewEntity.GroundCrewTask.LOADING;
                    case LANDING, ARRIVED -> GroundCrewEntity.GroundCrewTask.UNLOADING;
                    default -> GroundCrewEntity.GroundCrewTask.WAITING;
                });
            }
        }
    }

    /** Sends a finished flight's crew home. Safe to call more than once. */
    public void cleanupFlightNPCs(String flightId) {
        List<AirportStaffEntity> crew = this.crews.remove(flightId);
        if (crew == null) {
            return;
        }
        for (AirportStaffEntity staff : crew) {
            // discard, not remove(): remove() needs an explicit RemovalReason and this
            // is a despawn, not a death - no drops, no death sound.
            staff.discard();
        }
    }

    /** Whether a flight already has a crew on the ground. */
    public boolean isCrewed(String flightId) {
        return this.crews.containsKey(flightId);
    }
}
