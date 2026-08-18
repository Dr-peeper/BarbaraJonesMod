package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A straight-line energy bolt fired by Super Saiyan Cayden at Krave Monster.
 * Modeled on KraveMeteor's entity pattern, but with constant velocity (no
 * gravity) and a specific-target hit check instead of an on-ground impact.
 */
public class KraveLaser extends Entity {

    private static final double SPEED = 1.8D;
    private static final float DAMAGE = 6.0F;

    @Nullable
    private LivingEntity owner;

    public KraveLaser(EntityType<? extends KraveLaser> type, Level level) {
        super(type, level);
    }

    public KraveLaser(Level level, LivingEntity owner, Vec3 from, Vec3 target) {
        super(ModEntities.KRAVE_LASER.get(), level);
        this.owner = owner;
        setPos(from.x, from.y, from.z);
        Vec3 dir = target.subtract(from);
        double len = dir.length();
        if (len > 0.01D) {
            setDeltaMovement(dir.scale(SPEED / len));
        }
    }

    @Override
    protected void defineSynchedData() {
        // no synced state - a fixed-speed bolt needs nothing beyond position/motion
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);

        if (!level().isClientSide) {
            EntityHitResult hit = findHit(start, end);
            if (hit != null) {
                onHit(hit.getEntity());
                return;
            }
        }

        move(MoverType.SELF, motion);

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (this.horizontalCollision || this.verticalCollision || this.tickCount > 100) {
            discard();
        }
    }

    @Nullable
    private EntityHitResult findHit(Vec3 start, Vec3 end) {
        var box = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.6D);
        Entity found = null;
        double closest = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, box,
                e -> e instanceof KraveMonster && e.isAlive())) {
            double d = candidate.getBoundingBox().inflate(0.4D).distanceToSqr(start);
            if (d < closest) {
                closest = d;
                found = candidate;
            }
        }
        return found == null ? null : new EntityHitResult(found);
    }

    private void onHit(Entity target) {
        if (target instanceof KraveMonster monster && this.owner != null) {
            monster.hurt(level().damageSources().mobAttack(this.owner), DAMAGE);
        }
        level().playSound(null, blockPosition(), ModSounds.KRAVE_HURT.get(), getSoundSource(), 1.0F, 1.6F);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 65536.0D;
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { }
}
