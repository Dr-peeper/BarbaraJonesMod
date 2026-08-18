package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Krave Monster's own mouth-fired beam - a blue "kamehameha"-style bolt aimed
 * at players. Deliberately a sibling of KraveLaser rather than a shared
 * generalization of it: KraveLaser is Cayden's already-shipped boss weapon,
 * and duplicating this small entity keeps zero regression risk on that fight.
 */
public class KraveMouthBeam extends Entity {

    private static final double SPEED = 1.4D;
    private static final float DAMAGE = 8.0F;

    @Nullable
    private LivingEntity owner;

    public KraveMouthBeam(EntityType<? extends KraveMouthBeam> type, Level level) {
        super(type, level);
    }

    public KraveMouthBeam(Level level, LivingEntity owner, Vec3 from, Vec3 target) {
        super(ModEntities.KRAVE_MOUTH_BEAM.get(), level);
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
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
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
                e -> e instanceof Player p && p.isAlive() && !p.getAbilities().invulnerable)) {
            double d = candidate.getBoundingBox().inflate(0.4D).distanceToSqr(start);
            if (d < closest) {
                closest = d;
                found = candidate;
            }
        }
        return found == null ? null : new EntityHitResult(found);
    }

    private void onHit(Entity target) {
        if (target instanceof Player player && this.owner != null) {
            player.hurt(level().damageSources().mobAttack(this.owner), DAMAGE);
        }
        level().playSound(null, blockPosition(), ModSounds.KRAVE_BEAM_HIT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
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
