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

    /**
     * Ticks left on the track currently coming out of the car.
     *
     * <p>Counted down rather than watched for, because a server has no way to
     * ask whether a client-side sound has finished. The length is known, so the
     * honest thing is to time it and start it again - which is also what a car
     * stereo on repeat actually does.
     */
    private int musicTicks;

    /** 2:47 at twenty ticks a second, plus a beat of silence between plays. */
    private static final int TRACK_TICKS = 167 * 20 + 20;

    /**
     * Keeps the song coming out of the car while it is here.
     *
     * <p>Played through playSound rather than playLocalSound so it reaches every
     * client in range rather than only the one that happens to be nearest, and
     * at a volume above one so it carries - Minecraft scales audible range with
     * volume, so a value of 4 is not four times louder, it is four times further
     * away that you can hear it from. Which is the point of a car stereo.
     */
    private void tickMusic() {
        if (this.level().isClientSide || getState() == 2) {
            return;   // silent once it is pulling away
        }
        if (--this.musicTicks > 0) {
            return;
        }
        this.musicTicks = TRACK_TICKS;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                com.barbarajones.content.ModSounds.MUSIC_BET_CAR.get(),
                net.minecraft.sounds.SoundSource.RECORDS, 4.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        tickMusic();

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
