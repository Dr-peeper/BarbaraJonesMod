package com.barbarajones.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Duhl Wol's car - a simple animated vehicle that arrives, parks, and departs.
 * Purely visual/audio; no collision or gameplay interaction.
 */
public class DuhlWolCar extends Entity {

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(
            DuhlWolCar.class, EntityDataSerializers.INT);
    // state: 0=arriving (moving toward target), 1=parked (idle), 2=leaving (moving away)

    // Minecraft's SynchedEntityData has no double serializer - float is plenty
    // of precision for a car's target coordinates.
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(
            DuhlWolCar.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(
            DuhlWolCar.class, EntityDataSerializers.FLOAT);

    public DuhlWolCar(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STATE, 0);
        this.entityData.define(TARGET_X, 0.0F);
        this.entityData.define(TARGET_Z, 0.0F);
    }

    public void setState(int s) {
        this.entityData.set(STATE, s);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    public void setTarget(double x, double z) {
        this.entityData.set(TARGET_X, (float) x);
        this.entityData.set(TARGET_Z, (float) z);
    }

    public double getTargetX() {
        return this.entityData.get(TARGET_X);
    }

    public double getTargetZ() {
        return this.entityData.get(TARGET_Z);
    }

    @Override
    public void tick() {
        super.tick();

        int state = getState();
        double tx = getTargetX(), tz = getTargetZ();

        if (state == 0) {  // arriving
            double dx = tx - this.getX();
            double dz = tz - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 1.0D) {
                setState(1);   // reached, now parked
                if (!this.level().isClientSide) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 0.9F, 0.6F, false);
                }
            } else {
                double speed = 0.25D;
                this.setPos(this.getX() + dx / dist * speed, this.getY(), this.getZ() + dz / dist * speed);
            }
        } else if (state == 1) {  // parked - idle
            // do nothing, just sit here
        } else if (state == 2) {  // leaving
            double dx = tx - this.getX();
            double dz = tz - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.5D) {
                this.discard();  // gone
            } else {
                double speed = 0.35D;
                this.setPos(this.getX() + dx / dist * speed, this.getY(), this.getZ() + dz / dist * speed);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("CarState", getState());
        tag.putDouble("CarTargetX", getTargetX());
        tag.putDouble("CarTargetZ", getTargetZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setState(tag.getInt("CarState"));
        setTarget(tag.getDouble("CarTargetX"), tag.getDouble("CarTargetZ"));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 4096.0D;
    }
}
