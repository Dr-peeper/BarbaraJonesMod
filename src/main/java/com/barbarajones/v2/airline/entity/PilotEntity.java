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

/** Flies the aircraft. Walks to the plane during boarding, then sits the flight out in the cockpit. */
public class PilotEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Boolean> DATA_IN_COCKPIT =
            SynchedEntityData.defineId(PilotEntity.class, EntityDataSerializers.BOOLEAN);

    public PilotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IN_COCKPIT, false);
    }

    /**
     * While seated up front the pilot stops running its walk goals - otherwise it
     * strolls out of the flight deck mid-cruise and the cockpit renders empty.
     */
    public void setInCockpit(boolean inCockpit) {
        this.entityData.set(DATA_IN_COCKPIT, inCockpit);
        this.setNoAi(inCockpit);
    }

    public boolean isInCockpit() {
        return this.entityData.get(DATA_IN_COCKPIT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("InCockpit", this.isInCockpit());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setInCockpit(tag.getBoolean("InCockpit"));
    }
}
