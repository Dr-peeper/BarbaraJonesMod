package com.barbarajones.v2.airline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/** Screens passengers at the checkpoint. Patrols; goes alert when someone walks through without a pass. */
public class SecurityOfficerEntity extends AirportStaffEntity {

    private static final EntityDataAccessor<Boolean> DATA_ALERT =
            SynchedEntityData.defineId(SecurityOfficerEntity.class, EntityDataSerializers.BOOLEAN);

    public SecurityOfficerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AirportStaffEntity.createStaffAttributes();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ALERT, false);
    }

    public void setAlert(boolean alert) {
        this.entityData.set(DATA_ALERT, alert);
    }

    public boolean isAlert() {
        return this.entityData.get(DATA_ALERT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Alert", this.isAlert());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAlert(tag.getBoolean("Alert"));
    }
}
