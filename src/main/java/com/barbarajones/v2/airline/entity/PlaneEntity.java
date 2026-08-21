package com.barbarajones.v2.airline.entity;

import com.barbarajones.v2.airline.FlightData;
import com.barbarajones.v2.airline.FlightScheduler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The aircraft itself.
 *
 * <p>A plain {@link Entity}, not a vehicle and not a {@code LivingEntity}. Players
 * are not vanilla passengers of it - a real seat rig would cap the cabin at one
 * rider and fight the 12-block hitbox - they are tracked by
 * {@link com.barbarajones.v2.airline.PassengerManager} and carried by
 * {@link com.barbarajones.v2.airline.event.PassengerFlightHandler}. This entity is
 * the thing you see out the window, and the authority on where "inside the plane"
 * currently is.
 *
 * <p>Position is derived from the flight, never integrated: the scheduler owns the
 * clock, so a plane that is reloaded mid-cruise snaps to exactly where it should be
 * rather than restarting its journey.
 */
public class PlaneEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_FLIGHT_ID =
            SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_PASSENGER_COUNT =
            SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_AIRCRAFT_TYPE =
            SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_GEAR_DOWN =
            SynchedEntityData.defineId(PlaneEntity.class, EntityDataSerializers.BOOLEAN);

    /** Cruise height above the departure field, in blocks. */
    private static final float CRUISE_CLIMB = 100.0F;
    /** Fraction of the flight spent climbing, and the point descent begins. */
    private static final double CLIMB_END = 0.20D;
    private static final double DESCENT_START = 0.85D;

    private final List<UUID> cabin = new ArrayList<>();
    private float propellerRotation;

    public PlaneEntity(EntityType<?> type, Level level) {
        super(type, level);
        // The flight drives the position outright; vanilla movement would fight it.
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FLIGHT_ID, "");
        this.entityData.define(DATA_PASSENGER_COUNT, 0);
        this.entityData.define(DATA_AIRCRAFT_TYPE, "BOEING747");
        this.entityData.define(DATA_PITCH, 0.0F);
        this.entityData.define(DATA_GEAR_DOWN, true);
    }

    @Override
    public void tick() {
        super.tick();

        // Spinning fans are a client-side flourish; the server has no use for them.
        if (this.level().isClientSide) {
            this.propellerRotation = (this.propellerRotation + 24.0F) % 360.0F;
            return;
        }

        String flightId = this.getFlightId();
        if (flightId.isEmpty()) {
            return;
        }

        FlightData flight = FlightScheduler.getInstance().getFlight(flightId);
        if (flight == null) {
            // The scheduler has retired this flight - the airframe goes with it.
            this.discard();
            return;
        }

        this.applyFlightPosition(flight);
    }

    private void applyFlightPosition(FlightData flight) {
        double x;
        double z;
        double y;
        float pitch = 0.0F;
        boolean gearDown = true;

        switch (flight.state) {
            case FLYING -> {
                double progress = flight.getFlightProgress(this.level().getGameTime());
                x = Mth.lerp(progress, flight.departureX, flight.arrivalX);
                z = Mth.lerp(progress, flight.departureZ, flight.arrivalZ);

                double fieldY = Mth.lerp(progress, flight.departureY, flight.arrivalY);
                double cruiseY = fieldY + CRUISE_CLIMB;
                if (progress < CLIMB_END) {
                    y = Mth.lerp(progress / CLIMB_END, fieldY, cruiseY);
                    pitch = 15.0F;
                    gearDown = progress < 0.05D;
                } else if (progress > DESCENT_START) {
                    y = Mth.lerp((progress - DESCENT_START) / (1.0D - DESCENT_START), cruiseY, fieldY);
                    pitch = -15.0F;
                    gearDown = progress > 0.93D;
                } else {
                    y = cruiseY;
                    gearDown = false;
                }
            }
            case TAXIING -> {
                // Rolling out towards the threshold over the five seconds before rotation.
                double t = Mth.clamp((this.level().getGameTime() - (flight.departureTime - 100)) / 100.0D, 0.0D, 1.0D);
                x = Mth.lerp(t, flight.departureX, flight.departureX + 50.0D);
                z = flight.departureZ;
                y = flight.departureY + 2.0D;
            }
            case LANDING, ARRIVED -> {
                x = flight.arrivalX;
                z = flight.arrivalZ;
                y = flight.arrivalY + 2.0D;
            }
            default -> {
                x = flight.departureX;
                z = flight.departureZ;
                y = flight.departureY + 2.0D;
            }
        }

        this.setPos(x + 0.5D, y, z + 0.5D);
        this.entityData.set(DATA_PITCH, pitch);
        this.entityData.set(DATA_GEAR_DOWN, gearDown);

        // Face the destination so the model does not fly sideways.
        double dx = flight.arrivalX - flight.departureX;
        double dz = flight.arrivalZ - flight.departureZ;
        if (dx != 0.0D || dz != 0.0D) {
            this.setYRot((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
            this.yRotO = this.getYRot();
        }
    }

    public void setFlight(String flightId, String aircraftType) {
        this.entityData.set(DATA_FLIGHT_ID, flightId == null ? "" : flightId);
        this.entityData.set(DATA_AIRCRAFT_TYPE, aircraftType == null ? "BOEING747" : aircraftType);
    }

    public String getFlightId() {
        return this.entityData.get(DATA_FLIGHT_ID);
    }

    public String getAircraftType() {
        return this.entityData.get(DATA_AIRCRAFT_TYPE);
    }

    public float getPlanePitch() {
        return this.entityData.get(DATA_PITCH);
    }

    public boolean isLandingGearDown() {
        return this.entityData.get(DATA_GEAR_DOWN);
    }

    public float getPropellerRotation() {
        return this.propellerRotation;
    }

    public int getCabinCount() {
        return this.entityData.get(DATA_PASSENGER_COUNT);
    }

    /**
     * Named for the cabin rather than {@code getPassengers} - {@link Entity} declares
     * that one final for its own ridden-entity list, and these are not that.
     */
    public List<UUID> getCabinOccupants() {
        return List.copyOf(this.cabin);
    }

    public void addCabinOccupant(UUID playerId) {
        if (!this.cabin.contains(playerId)) {
            this.cabin.add(playerId);
            this.entityData.set(DATA_PASSENGER_COUNT, this.cabin.size());
        }
    }

    public void removeCabinOccupant(UUID playerId) {
        if (this.cabin.remove(playerId)) {
            this.entityData.set(DATA_PASSENGER_COUNT, this.cabin.size());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("FlightId", this.getFlightId());
        tag.putString("AircraftType", this.getAircraftType());
        ListTag occupants = new ListTag();
        for (UUID id : this.cabin) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", id);
            occupants.add(entry);
        }
        tag.put("Cabin", occupants);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setFlight(tag.getString("FlightId"), tag.getString("AircraftType"));
        this.cabin.clear();
        ListTag occupants = tag.getList("Cabin", Tag.TAG_COMPOUND);
        for (int i = 0; i < occupants.size(); i++) {
            this.cabin.add(occupants.getCompound(i).getUUID("UUID"));
        }
        this.entityData.set(DATA_PASSENGER_COUNT, this.cabin.size());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
