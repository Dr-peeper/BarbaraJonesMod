package com.barbarajones.v2.airline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Cabin crew. Works the aisle through the flight and reads the safety brief on boarding. */
public class FlightAttendantEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(FlightAttendantEntity.class, EntityDataSerializers.INT);

    public enum AttendantPhase {
        WAITING, BOARDING, IN_FLIGHT, DEPLANING;

        private static final AttendantPhase[] VALUES = values();

        static AttendantPhase byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WAITING;
        }
    }

    public FlightAttendantEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, AttendantPhase.WAITING.ordinal());
    }

    public void setCurrentPhase(AttendantPhase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
    }

    public AttendantPhase getCurrentPhase() {
        return AttendantPhase.byOrdinal(this.entityData.get(DATA_PHASE));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", this.entityData.get(DATA_PHASE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PHASE, tag.getInt("Phase"));
    }
}
