package com.barbarajones.v2.airline;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModEntities;
import com.barbarajones.v2.airline.entity.FlightAttendantEntity;
import com.barbarajones.v2.airline.entity.GateAgentEntity;
import com.barbarajones.v2.airline.entity.PilotEntity;
import com.barbarajones.v2.airline.entity.SecurityOfficerEntity;
import com.barbarajones.v2.airline.entity.GroundCrewEntity;
import com.barbarajones.v2.airline.entity.AirTrafficControllerEntity;
import com.barbarajones.v2.airline.npc.NPCBehaviorScheduler;
import com.barbarajones.v2.airline.FlightData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AirlineEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PILOT.get(), PilotEntity.createAttributes().build());
        event.put(ModEntities.FLIGHT_ATTENDANT.get(), FlightAttendantEntity.createAttributes().build());
        event.put(ModEntities.GATE_AGENT.get(), GateAgentEntity.createAttributes().build());
        event.put(ModEntities.SECURITY_OFFICER.get(), SecurityOfficerEntity.createAttributes().build());
        event.put(ModEntities.GROUND_CREW.get(), GroundCrewEntity.createAttributes().build());
        event.put(ModEntities.AIR_TRAFFIC_CONTROLLER.get(), AirTrafficControllerEntity.createAttributes().build());
    }
}

@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
class AirlineServerEvents {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.getServer() != null && !event.getServer().isRunning()) {
            return;
        }

        net.minecraft.server.level.ServerLevel serverLevel =
                event.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (serverLevel != null) {
            FlightScheduler scheduler = FlightScheduler.getInstance();
            scheduler.updateFlights(serverLevel, serverLevel.getGameTime());

            NPCBehaviorScheduler npcScheduler = NPCBehaviorScheduler.getInstance();
            AircraftManager aircraft = AircraftManager.getInstance();

            // Iterate a copy: retiring a flight mutates the scheduler's own map.
            for (FlightData flight : new java.util.ArrayList<>(scheduler.getAllFlights())) {
                if (flight.state == FlightData.FlightState.BOARDING) {
                    // Both of these are idempotent - they are called every boarding tick.
                    aircraft.ensureAircraft(serverLevel, flight);
                    npcScheduler.spawnFlightNPCs(serverLevel, flight);
                }
                npcScheduler.updateFlightNPCs(serverLevel, flight);

                if (flight.state == FlightData.FlightState.ARRIVED) {
                    npcScheduler.cleanupFlightNPCs(flight.flightId);
                    aircraft.release(flight.flightId);
                }
            }
        }
    }
}
