package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.barbara.SmokeTargets;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The lit cherry, flicked off the end of the blunt. Barbara's cheap poke: it
 * arcs, it burns whatever it touches, and it costs almost nothing out of the
 * stash so she can throw them all day if she is kept supplied.
 *
 * <p>It sets its victim alight but never sets a block on fire. Cayden lives in
 * a house the player built and Barbara throws these into melee - a projectile
 * that lit the floor would eventually burn him down, and rule one is that he
 * does not die.
 */
public class EmberCherry extends Entity {

    /** Flicked while she was high: bigger cherry, longer burn, worse aim upstream. */
    private static final EntityDataAccessor<Boolean> HOT =
            SynchedEntityData.defineId(EmberCherry.class, EntityDataSerializers.BOOLEAN);

    private static final double SPEED = 1.05D;
    private static final double GRAVITY = 0.035D;
    private static final int MAX_LIFETIME = 90;

    @Nullable
    private BarbaraJones shooter;

    public EmberCherry(EntityType<? extends EmberCherry> type, Level level) {
        super(type, level);
    }

    public EmberCherry(Level level, BarbaraJones shooter, Vec3 from, Vec3 direction, boolean hot) {
        super(ModEntities.EMBER_CHERRY.get(), level);
        this.shooter = shooter;
        setPos(from.x, from.y, from.z);
        setDeltaMovement(direction.normalize().scale(hot ? SPEED * 1.15D : SPEED));
        this.entityData.set(HOT, hot);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HOT, false);
    }

    public boolean isHot() {
        return this.entityData.get(HOT);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide) {
            LivingEntity struck = findVictim(motion);
            if (struck != null) {
                burn(struck);
                return;
            }
        }

        move(MoverType.SELF, motion);
        setDeltaMovement(motion.add(0.0D, -GRAVITY, 0.0D).multiply(0.99D, 1.0D, 0.99D));

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            if (this.tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (this.horizontalCollision || this.verticalCollision || this.tickCount > MAX_LIFETIME) {
            snuff();
            discard();
        }
    }

    @Nullable
    private LivingEntity findVictim(Vec3 motion) {
        AABB sweep = getBoundingBox().expandTowards(motion).inflate(0.35D);
        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, sweep,
                e -> e != this.shooter && SmokeTargets.isFoe(this.shooter, e))) {
            double d = candidate.distanceToSqr(position());
            if (d < closest) {
                closest = d;
                best = (LivingEntity) candidate;
            }
        }
        return best;
    }

    private void burn(LivingEntity victim) {
        boolean hot = isHot();
        victim.hurt(this.shooter != null
                ? level().damageSources().indirectMagic(this, this.shooter)
                : level().damageSources().magic(), hot ? 4.0F : 2.5F);
        victim.setSecondsOnFire(hot ? 6 : 4);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 5, 0.15D, 0.15D, 0.15D, 0.0D);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 8, 0.2D, 0.2D, 0.2D, 0.02D);
        }
        level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH, getSoundSource(), 0.8F, 1.9F);
        discard();
    }

    /** Landed short. All it leaves is a scorch mark and a smell. */
    private void snuff() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.1D, getZ(),
                    8, 0.25D, 0.1D, 0.25D, 0.01D);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.1D, getZ(),
                    4, 0.2D, 0.1D, 0.2D, 0.01D);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(HOT, tag.getBoolean("Hot"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Hot", isHot());
    }
}
