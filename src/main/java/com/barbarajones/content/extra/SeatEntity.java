package com.barbarajones.content.extra;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The invisible anchor a player rides while sitting in a {@link ReclinerBlock}.
 *
 * <p>Minecraft has no "sit on a block" concept - the only way to pin a player to
 * a spot and get the sitting pose on their model is to make them a passenger of
 * something. This is that something: no hitbox worth speaking of, no gravity, no
 * saving, and it deletes itself the instant nobody is on it, so a world can never
 * accumulate orphaned seats.
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
    }

    @Override
    public void tick() {
        super.tick();
        // Nothing else can clean these up: the recliner does not tick and the
        // rider may dismount from anywhere (including by dying or logging out).
        // The two-tick grace covers the gap between spawning the seat and the
        // startRiding() that follows it.
        if (!this.level().isClientSide && this.tickCount > 2 && this.getPassengers().isEmpty()) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** The rider sits exactly where the seat is; the block places it at chair height. */
    @Override
    public double getPassengersRidingOffset() {
        return 0.0D;
    }

    /** Stand up on top of the chair rather than being shoved into whatever is behind it. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getY() + 0.75D, this.getZ());
    }
}
