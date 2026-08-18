package com.barbarajones.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Krave tornado - clamps onto the tornado'd player and rises with them.
 * The funnel itself is drawn entirely in KraveTornadoRenderer.
 */
public class KraveTornado extends Entity {

    public KraveTornado(EntityType<? extends KraveTornado> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();
        Player p = level().getNearestPlayer(this, 10.0D);
        if (p != null) {
            setPos(p.getX(), p.getY() - 1.5D, p.getZ());
        }
        if (!level().isClientSide && this.tickCount > 28) {
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
