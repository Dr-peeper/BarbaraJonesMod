package com.barbarajones.cinematic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The invisible thing the camera rides during a cinematic.
 *
 * <p>Minecraft will not let a mod set the camera's position directly - Camera's
 * setters are protected and Forge does not open them up - but it will happily
 * follow whatever entity {@code Minecraft.setCameraEntity} is pointed at. So the
 * rig drives this, and the camera follows it. That is the whole reason a real
 * dolly is possible at all instead of another look-at snap.
 *
 * <p>It is never registered and never added to the level. It borrows the armour
 * stand's {@link EntityType} purely because Entity's constructor needs some type
 * to take its dimensions from; nothing ever asks this entity what it is.
 *
 * <p>{@link #place} keeps the previous position in {@code xo/yo/zo}, which is
 * what vanilla's camera interpolates against - that is what turns twenty
 * position updates a second into a smooth move at any frame rate.
 */
public final class CameraAnchor extends Entity {

    public CameraAnchor(Level level) {
        super(EntityType.ARMOR_STAND, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Advance to the next authored pose, leaving the old one to interpolate from. */
    public void place(double x, double y, double z, float yaw, float pitch) {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        setPos(x, y, z);
        setYRot(yaw);
        setXRot(pitch);
    }

    /** Jump with no interpolation - only ever for the first frame of a scene. */
    public void snap(double x, double y, double z, float yaw, float pitch) {
        setPos(x, y, z);
        setYRot(yaw);
        setXRot(pitch);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    @Override
    protected void defineSynchedData() {
        // Nothing is ever synced: this entity exists on one client and no server
        // has ever heard of it.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Never saved.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Never saved.
    }
}
