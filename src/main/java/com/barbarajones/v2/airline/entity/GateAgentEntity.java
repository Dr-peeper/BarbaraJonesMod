package com.barbarajones.v2.airline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Works a gate: opens boarding, checks passes, closes the door at pushback. */
public class GateAgentEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Integer> DATA_GATE =
            SynchedEntityData.defineId(GateAgentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOARDING =
            SynchedEntityData.defineId(GateAgentEntity.class, EntityDataSerializers.BOOLEAN);

    public GateAgentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GATE, 0);
        this.entityData.define(DATA_BOARDING, false);
    }

    public void setAssignedGate(int gateNumber) {
        this.entityData.set(DATA_GATE, gateNumber);
    }

    public int getAssignedGate() {
        return this.entityData.get(DATA_GATE);
    }

    public void setBoardingActive(boolean active) {
        this.entityData.set(DATA_BOARDING, active);
    }

    public boolean isBoardingActive() {
        return this.entityData.get(DATA_BOARDING);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Gate", this.getAssignedGate());
        tag.putBoolean("Boarding", this.isBoardingActive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAssignedGate(tag.getInt("Gate"));
        this.setBoardingActive(tag.getBoolean("Boarding"));
    }
}
