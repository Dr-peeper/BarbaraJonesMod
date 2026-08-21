package com.barbarajones.v2.airline.entity;

import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.FlightScheduler;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Sits in the tower and watches the board. Unlike the rest of the staff it gets no
 * stroll goal - a controller that wanders off the visual circuit is not a
 * controller - so the goal set is overridden rather than inherited.
 */
public class AirTrafficControllerEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Integer> DATA_ACTIVE_FLIGHTS =
            SynchedEntityData.defineId(AirTrafficControllerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TRANSMITTING =
            SynchedEntityData.defineId(AirTrafficControllerEntity.class, EntityDataSerializers.BOOLEAN);

    public AirTrafficControllerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE_FLIGHTS, 0);
        this.entityData.define(DATA_TRANSMITTING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // Recounting the board every tick is pointless work for a display value.
        if (this.tickCount % 40 != 0) {
            return;
        }
        int airborne = 0;
        for (FlightData flight : FlightScheduler.getInstance().getAllFlights()) {
            if (flight.state == FlightData.FlightState.FLYING) {
                airborne++;
            }
        }
        this.entityData.set(DATA_ACTIVE_FLIGHTS, airborne);
        this.entityData.set(DATA_TRANSMITTING, airborne > 0 && this.random.nextFloat() > 0.7F);
    }

    public int getActiveFlightCount() {
        return this.entityData.get(DATA_ACTIVE_FLIGHTS);
    }

    public boolean isTransmitting() {
        return this.entityData.get(DATA_TRANSMITTING);
    }
}
