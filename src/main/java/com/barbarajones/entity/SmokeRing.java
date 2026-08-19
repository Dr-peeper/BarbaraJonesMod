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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * One of the O's. A ring of smoke that sails in a straight line, widening as it
 * goes, and - unlike every other projectile in this mod - does not stop at the
 * first thing it hits. It punches through a whole line of mobs, staggering each
 * one exactly once, which is what makes it Barbara's answer to a crowd.
 *
 * <p>Built on the plain-{@link Entity} projectile pattern used by
 * {@code KraveLaser}, with {@code setPos} stepping rather than {@code move} so
 * a ring never snags on terrain it should be gliding over.
 */
public class SmokeRing extends Entity {

    /** Blown while she was high: faster, fatter, and it hits noticeably harder. */
    private static final EntityDataAccessor<Boolean> LACED =
            SynchedEntityData.defineId(SmokeRing.class, EntityDataSerializers.BOOLEAN);

    private static final double SPEED = 0.85D;
    private static final double RISE = 0.012D;
    private static final int LIFETIME = 55;
    /** Past this many victims the ring has given up its shape and dissipates. */
    private static final int MAX_VICTIMS = 5;

    private final Set<Integer> struck = new HashSet<>();

    @Nullable
    private BarbaraJones shooter;

    public SmokeRing(EntityType<? extends SmokeRing> type, Level level) {
        super(type, level);
    }

    public SmokeRing(Level level, BarbaraJones shooter, Vec3 from, Vec3 direction, boolean laced) {
        super(ModEntities.SMOKE_RING.get(), level);
        this.shooter = shooter;
        setPos(from.x, from.y, from.z);
        setDeltaMovement(direction.normalize().scale(laced ? SPEED * 1.25D : SPEED));
        this.entityData.set(LACED, laced);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LACED, false);
    }

    public boolean isLaced() {
        return this.entityData.get(LACED);
    }

    /** How wide the ring has spread by now - the renderer draws to this radius. */
    public float ringRadius(float partial) {
        float age = this.tickCount + partial;
        return (isLaced() ? 0.6F : 0.45F) + age * 0.022F;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y + RISE, getZ() + motion.z);

        if (level().isClientSide) {
            if (this.tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (this.tickCount > LIFETIME || level().getBlockState(blockPosition()).blocksMotion()) {
            dissipate();
            return;
        }
        for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(ringRadius(0.0F) + 0.35D))) {
            if (victim == this.shooter || !SmokeTargets.isFoe(this.shooter, victim)) {
                continue;
            }
            if (!this.struck.add(victim.getId())) {
                continue;                       // a ring only staggers you once
            }
            stagger(victim, motion);
            if (this.struck.size() >= MAX_VICTIMS) {
                dissipate();
                return;
            }
        }
    }

    private void stagger(LivingEntity victim, Vec3 motion) {
        boolean laced = isLaced();
        victim.hurt(this.shooter != null
                ? level().damageSources().indirectMagic(this, this.shooter)
                : level().damageSources().magic(), laced ? 4.0F : 2.5F);
        // Shoved along the ring's travel rather than away from its centre, so a
        // line of mobs gets pushed back down the line it came from.
        double push = laced ? 0.9D : 0.6D;
        victim.push(motion.x * push, 0.24D, motion.z * push);
        victim.hurtMarked = true;
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false));
        victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        if (laced) {
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false));
        }
        // Same job as the rest of her kit: get it off Cayden.
        if (victim instanceof Mob mob && mob.getTarget() instanceof CaydenCobb
                && this.shooter != null) {
            mob.setTarget(this.shooter);
        }
        level().playSound(null, blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                getSoundSource(), 0.7F, 1.6F);
    }

    private void dissipate() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(),
                    10, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(LACED, tag.getBoolean("Laced"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Laced", isLaced());
    }
}
