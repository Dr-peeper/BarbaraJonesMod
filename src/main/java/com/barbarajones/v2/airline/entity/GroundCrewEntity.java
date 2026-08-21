package com.barbarajones.v2.airline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Turns the aircraft round on stand: bags, fuel, catering, cleaning. */
public class GroundCrewEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Integer> DATA_TASK =
            SynchedEntityData.defineId(GroundCrewEntity.class, EntityDataSerializers.INT);

    public enum GroundCrewTask {
        WAITING, FUELING, CLEANING, CATERING, LOADING, UNLOADING;

        private static final GroundCrewTask[] VALUES = values();

        static GroundCrewTask byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : WAITING;
        }
    }

    public GroundCrewEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.55D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TASK, GroundCrewTask.WAITING.ordinal());
    }

    public void setTask(GroundCrewTask task) {
        this.entityData.set(DATA_TASK, task.ordinal());
    }

    public GroundCrewTask getTask() {
        return GroundCrewTask.byOrdinal(this.entityData.get(DATA_TASK));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Task", this.entityData.get(DATA_TASK));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_TASK, tag.getInt("Task"));
    }
}
