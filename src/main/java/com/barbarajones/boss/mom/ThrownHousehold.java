package com.barbarajones.boss.mom;

import com.barbarajones.content.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Whatever was on the counter when Mom Cobb lost her temper: the TV remote, a
 * slipper, the frying pan, a phone charger, the laundry basket. Act one of her
 * boss fight is her throwing these, on a lob arc, after a long visible wind-up.
 *
 * <p>Built on {@code KraveLaser}'s plain-{@link Entity} projectile pattern
 * rather than vanilla's Projectile hierarchy, for consistency with the rest of
 * the mod - but unlike the laser this one falls, so it can be side-stepped and
 * it can miss.
 */
public class ThrownHousehold extends Entity {

    /** How many distinct objects exist. The renderer indexes its textures by this. */
    public static final int KINDS = 5;

    /** Player-facing names, indexed by kind. */
    private static final String[] NAMES = {
        "the TV remote", "a slipper", "the frying pan", "a phone charger", "the laundry basket"
    };

    private static final float DAMAGE = 6.0F;
    private static final double GRAVITY = 0.045D;
    private static final int MAX_LIFETIME = 140;

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(ThrownHousehold.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerId;
    @Nullable
    private LivingEntity ownerCache;

    public ThrownHousehold(EntityType<? extends ThrownHousehold> type, Level level) {
        super(type, level);
    }

    public ThrownHousehold(Level level, LivingEntity owner, Vec3 from, Vec3 aim, int kind) {
        super(ModEntities.MOM_THROWN.get(), level);
        this.ownerCache = owner;
        this.ownerId = owner.getUUID();
        setPos(from.x, from.y, from.z);
        this.entityData.set(DATA_KIND, Mth.clamp(kind, 0, KINDS - 1));

        double dx = aim.x - from.x;
        double dy = aim.y - from.y;
        double dz = aim.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // Lift the aim point by a slice of the horizontal distance so gravity
        // drops the object ONTO the target instead of into the floor in front of
        // it - the same trick vanilla's ranged mobs use.
        lob(dx, dy + horizontal * 0.22D, dz, 1.1F, 1.6F);
    }

    private void lob(double x, double y, double z, float velocity, float spread) {
        Vec3 dir = new Vec3(x, y, z).normalize()
                .add(this.random.nextGaussian() * 0.0075D * spread,
                     this.random.nextGaussian() * 0.0075D * spread,
                     this.random.nextGaussian() * 0.0075D * spread)
                .scale(velocity);
        setDeltaMovement(dir);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_KIND, 0);
    }

    public int getKind() {
        return Mth.clamp(this.entityData.get(DATA_KIND), 0, KINDS - 1);
    }

    public String getObjectName() {
        return NAMES[getKind()];
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            LivingEntity struck = findVictim(motion);
            if (struck != null) {
                onHit(struck);
                return;
            }
        }

        move(MoverType.SELF, motion);
        setDeltaMovement(motion.add(0.0D, -GRAVITY, 0.0D).multiply(0.99D, 1.0D, 0.99D));

        if (level().isClientSide && this.tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (this.horizontalCollision || this.verticalCollision || this.tickCount > MAX_LIFETIME) {
            burst();
            discard();
        }
    }

    @Nullable
    private LivingEntity findVictim(Vec3 motion) {
        AABB sweep = getBoundingBox().expandTowards(motion).inflate(0.45D);
        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (Entity candidate : level().getEntities(this, sweep, this::canHit)) {
            double d = candidate.distanceToSqr(position());
            if (d < closest) {
                closest = d;
                best = (LivingEntity) candidate;
            }
        }
        return best;
    }

    /** Everything alive except Mom's own side of the fight - she does not smash her own stash. */
    private boolean canHit(Entity candidate) {
        return candidate instanceof LivingEntity
                && candidate.isAlive()
                && !(candidate instanceof MomCobbBoss)
                && !(candidate instanceof MomKraveStash)
                && !candidate.isSpectator();
    }

    private void onHit(LivingEntity victim) {
        LivingEntity owner = resolveOwner();
        // Damage is always attributed to Mom through the ordinary hurt() path.
        // If the thing she just brained happens to be Cayden, his death has to
        // run the normal LivingDeathEvent route so the Krave Apocalypse fires -
        // nothing here may shortcut that.
        victim.hurt(owner != null
                ? level().damageSources().mobAttack(owner)
                : level().damageSources().generic(), DAMAGE);
        victim.knockback(0.45D, getX() - victim.getX(), getZ() - victim.getZ());
        burst();
        discard();
    }

    private void burst() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 8, 0.2D, 0.2D, 0.2D, 0.12D);
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 5, 0.2D, 0.2D, 0.2D, 0.02D);
        sl.playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, getSoundSource(), 1.1F, 0.9F);
    }

    @Nullable
    private LivingEntity resolveOwner() {
        if (this.ownerCache != null && this.ownerCache.isAlive()) {
            return this.ownerCache;
        }
        if (this.ownerId != null && level() instanceof ServerLevel sl
                && sl.getEntity(this.ownerId) instanceof LivingEntity owner) {
            this.ownerCache = owner;
            return owner;
        }
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Thrower")) {
            this.ownerId = tag.getUUID("Thrower");
        }
        this.entityData.set(DATA_KIND, Mth.clamp(tag.getInt("Kind"), 0, KINDS - 1));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Thrower", this.ownerId);
        }
        tag.putInt("Kind", getKind());
    }
}
