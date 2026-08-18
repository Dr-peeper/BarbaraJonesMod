package com.barbarajones.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;

/**
 * The giant Krave box that plummets out of the blood-red sky during the
 * apocalypse. Pure spectacle - it tumbles down and vanishes; the blast itself
 * is driven by KraveApocalypse. The wild tumble is client-side in the renderer.
 */
public class GiantKraveBox extends Entity {

    public GiantKraveBox(EntityType<? extends GiantKraveBox> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().add(0.0D, -0.14D, 0.0D));
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(1.0D, 0.98D, 1.0D));

        if (!level().isClientSide && (onGround() || this.tickCount > 40)) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 16384.0D;
    }
}
