package com.barbarajones.boss.manager;

import com.barbarajones.content.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A steel filing cabinet The Manager's facilities team drops into the arena
 * during the YOU'RE FIRED phase. It is solid - you can stand behind it - and
 * {@link TerminationNotice} dies against it, which is the entire point: the
 * counter to homing paperwork is remembering that paper does not go through
 * furniture.
 *
 * <p>Deliberately an entity rather than a block. A block would need a registry
 * entry, a blockstate, a model and a loot table, and it would still be sitting
 * in the world long after the fight; an entity can be dropped mid-air, settle
 * under its own gravity and expire on its own.
 */
public class FilingCabinet extends Entity {

    private static final float MAX_INTEGRITY = 30.0F;
    private static final int LIFETIME = 3600;
    private static final double GRAVITY = 0.06D;

    /** Synced so the renderer can dent the drawers as it takes damage. */
    private static final EntityDataAccessor<Float> DATA_INTEGRITY =
            SynchedEntityData.defineId(FilingCabinet.class, EntityDataSerializers.FLOAT);

    public FilingCabinet(EntityType<? extends FilingCabinet> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public FilingCabinet(Level level, double x, double y, double z) {
        this(ModEntities.FILING_CABINET.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_INTEGRITY, MAX_INTEGRITY);
    }

    /** 1.0 fresh out of the truck, 0.0 the moment before it folds. */
    public float getIntegrity() {
        return this.entityData.get(DATA_INTEGRITY) / MAX_INTEGRITY;
    }

    // ---- solidity ------------------------------------------------------------

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        // a loaded filing cabinet does not slide because someone walked into it
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().subtract(0.0D, GRAVITY, 0.0D));
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().multiply(0.9D, 0.98D, 0.9D));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (!level().isClientSide && this.tickCount > LIFETIME) {
            retire();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            collapse();
            return true;
        }
        markHurt();
        float left = this.entityData.get(DATA_INTEGRITY) - amount;
        this.entityData.set(DATA_INTEGRITY, Math.max(0.0F, left));
        level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, getSoundSource(), 0.8F, 0.6F);
        if (left <= 0.0F) {
            collapse();
        }
        return true;
    }

    /** Torn apart - a cloud of loose paperwork and it is gone. */
    private void collapse() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY() + 0.7D, getZ(), 25, 0.4D, 0.5D, 0.4D, 0.05D);
        }
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, getSoundSource(), 1.1F, 0.5F);
        discard();
    }

    /** Quiet removal when the fight ends - no shrapnel, no noise. */
    public void retire() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY() + 0.7D, getZ(), 10, 0.3D, 0.4D, 0.3D, 0.01D);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Integrity")) {
            this.entityData.set(DATA_INTEGRITY, tag.getFloat("Integrity"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Integrity", this.entityData.get(DATA_INTEGRITY));
    }
}
