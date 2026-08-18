package com.barbarajones.entity;

import com.barbarajones.content.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

/**
 * A pure-spectacle cinematic actor for the death stages:
 *   O_BLOWER - giant sky-Barbara visibly blowing giant smoke O's
 *   POURER   - giant sky-Barbara tilting a colossal Pibb cup
 *   TORCHER  - a COLOSSAL blowtorch roaring flame at the ground
 *   MANAGER  - THE INTERNET MANAGER, a faceless giant walking in
 *   CLEAVER  - a colossal cleaver that hangs, quivers, then plunges
 */
public class SkyCinematic extends Entity {

    public static final byte O_BLOWER = 0;
    public static final byte POURER   = 1;
    public static final byte TORCHER  = 2;
    public static final byte MANAGER  = 3;
    public static final byte CLEAVER  = 4;

    private static final EntityDataAccessor<Byte> KIND =
            SynchedEntityData.defineId(SkyCinematic.class, EntityDataSerializers.BYTE);

    private double targetX, targetZ;
    private int life = 300;

    public SkyCinematic(EntityType<? extends SkyCinematic> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public byte getKind() {
        return this.entityData.get(KIND);
    }

    public SkyCinematic kind(byte k) {
        this.entityData.set(KIND, k);
        return this;
    }

    public SkyCinematic lifespan(int ticks) {
        this.life = ticks;
        return this;
    }

    public SkyCinematic walkTo(double x, double z) {
        this.targetX = x;
        this.targetZ = z;
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, O_BLOWER);
    }

    @Override
    public void tick() {
        super.tick();
        byte k = getKind();

        if (k == MANAGER) {
            double dx = this.targetX - getX();
            double dz = this.targetZ - getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 4.0D) {
                setPos(getX() + dx / len * 0.07D, getY(), getZ() + dz / len * 0.07D);
                setYRot((float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F);
            }
            if (!level().isClientSide && this.tickCount % 24 == 0) {
                level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(),
                        getSoundSource(), 0.7F, 0.35F);   // booming footsteps
            }
        } else if (k == CLEAVER) {
            if (this.tickCount > 40) {
                setPos(getX(), getY() - 1.4D, getZ());
                if (!level().isClientSide) {
                    BlockPos below = blockPosition().below();
                    if (level().getBlockState(below).blocksMotion() || this.tickCount > 140) {
                        level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(),
                                getSoundSource(), 4.0F, 0.5F);
                        level().explode(this, getX(), getY(), getZ(), 7.0F, ExplosionInteraction.MOB);
                        discard();
                        return;
                    }
                }
            }
        } else {
            setPos(getX(), getY() + Math.sin(this.tickCount * 0.06D) * 0.02D, getZ());
        }

        if (!level().isClientSide && this.tickCount > this.life) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        kind(tag.getByte("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Kind", getKind());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 65536.0D;
    }
}
