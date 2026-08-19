package com.barbarajones.entity;

import com.barbarajones.content.ModSounds;
import com.barbarajones.dimension.KraveKosmosData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A Krave Box that heals the boss every few seconds while it survives - the
 * player's job is finding and destroying these (and the minions), not
 * fighting Krave Monster directly. Built on the same lightweight pattern as
 * vanilla's End Crystal (plain Entity, no AI, a hand-rolled hurt/destroy
 * path) instead of Monster/LivingEntity: it was always motionless anyway
 * (empty registerGoals(), zero movement speed), and this way it's genuinely
 * fixed in place - no incidental gravity/knockback from LivingEntity's
 * physics, since base Entity.tick() doesn't apply any unless a subclass
 * explicitly adds it (this one never touches deltaMovement).
 *
 * <p>Protected by a small regenerating shield (see MAX_SHIELD) - it takes
 * several hits to bring down, not one, and left alone for a while it
 * recovers. Destroying one triggers a real (block-safe) explosion - a
 * dramatic launch for whoever's standing close, not just a sound cue. One
 * elite instance (see {@link #setElite}) - the boss's own
 * protector at the center of his den - is bigger, has double the shield
 * capacity, and never needs a distinct texture since size alone reads as
 * "the strong one" next to the four ordinary boxes on the landing island.
 */
public class KraveHealingBox extends Entity {

    private static final int HEAL_INTERVAL = 60;
    private static final float HEAL_AMOUNT = 6.0F;
    private static final int MAX_SHIELD = 3;
    private static final int ELITE_MAX_SHIELD = 6;
    private static final int SHIELD_REGEN_TICKS = 100;
    private static final float ELITE_SCALE = 1.5F;
    /**
     * TNT's own power (4.0) still hit too hard up close - explosion damage
     * scales roughly with the square of power, so even with armor it was a
     * serious chunk of a player's health. Dropped to 2.0 (a quarter of
     * TNT's damage at the same distance) - still a real shove, not a
     * near-guaranteed hit.
     */
    private static final float EXPLOSION_POWER = 2.0F;

    private static final EntityDataAccessor<Integer> DATA_SHIELD =
            SynchedEntityData.defineId(KraveHealingBox.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ELITE =
            SynchedEntityData.defineId(KraveHealingBox.class, EntityDataSerializers.BOOLEAN);

    private UUID healTargetId;
    private KraveMonster healTargetCache;
    private int regenCooldown;

    public KraveHealingBox(EntityType<? extends KraveHealingBox> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SHIELD, MAX_SHIELD);
        this.entityData.define(DATA_ELITE, false);
    }

    /** Client-safe: used by the renderer to compute the shield overlay's fill fraction. */
    public int maxShield() {
        return isElite() ? ELITE_MAX_SHIELD : MAX_SHIELD;
    }

    /** Client-safe: driven by synced data, used by the renderer's shield overlay. */
    public int getShield() {
        return this.entityData.get(DATA_SHIELD);
    }

    private void setShield(int value) {
        this.entityData.set(DATA_SHIELD, Math.max(0, Math.min(maxShield(), value)));
    }

    /** Client-safe: driven by synced data, used by the renderer to pick the bigger model. */
    public boolean isElite() {
        return this.entityData.get(DATA_ELITE);
    }

    /**
     * Marks this box as the boss's bigger, stronger protector - bigger hitbox
     * (see getDimensions below), a larger shield capacity, and a bigger model
     * on the client. Call before addFreshEntity so the client never sees the
     * normal size, and refreshDimensions() afterward so the server-side
     * hitbox actually updates to match.
     */
    public void setElite(boolean elite) {
        this.entityData.set(DATA_ELITE, elite);
        setShield(maxShield());
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isElite() ? super.getDimensions(pose).scale(ELITE_SCALE) : super.getDimensions(pose);
    }

    /** Set once at spawn time by whatever placed this box (battle controller, den, or worldgen marker). */
    public void setHealTarget(KraveMonster monster) {
        this.healTargetCache = monster;
        this.healTargetId = monster.getUUID();
    }

    /** Required for melee/attack targeting to consider this entity at all - see EndCrystal. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        this.regenCooldown = SHIELD_REGEN_TICKS;
        int shield = getShield();
        if (shield > 0) {
            setShield(shield - 1);
            level().playSound(null, blockPosition(), ModSounds.KRAVE_HURT.get(), SoundSource.HOSTILE, 0.7F, 1.3F);
            return true;
        }
        level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
        // NONE block interaction: real explosion knockback/damage and the
        // vanilla explosion sound+particle, without cratering the terrain -
        // this is a destroyed protector box, not a bomb.
        level().explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
        discard();
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (this.regenCooldown > 0) {
            this.regenCooldown--;
        } else if (getShield() < maxShield() && this.tickCount % SHIELD_REGEN_TICKS == 0) {
            setShield(getShield() + 1);
        }

        if (this.tickCount % HEAL_INTERVAL != 0) {
            return;
        }
        KraveMonster target = resolveTarget();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        target.heal(HEAL_AMOUNT);
        level().playSound(null, blockPosition(), ModSounds.KRAVE_LAUGH.get(), getSoundSource(), 0.6F, 1.4F);
    }

    private KraveMonster resolveTarget() {
        if (this.healTargetCache != null && this.healTargetCache.isAlive()) {
            return this.healTargetCache;
        }
        if (!(level() instanceof ServerLevel sl)) {
            return null;
        }
        if (this.healTargetId != null) {
            var entity = sl.getEntity(this.healTargetId);
            if (entity instanceof KraveMonster monster) {
                this.healTargetCache = monster;
                return monster;
            }
        }
        // No explicit target was ever assigned (e.g. this box was placed by
        // worldgen/structure code rather than a battle controller) - fall
        // back to whichever boss the dimension's singleton tracker knows about.
        var bossId = KraveKosmosData.get(sl).getBossId();
        if (bossId != null && sl.getEntity(bossId) instanceof KraveMonster monster && monster.isAlive()) {
            this.healTargetCache = monster;
            this.healTargetId = bossId;
            return monster;
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.healTargetId != null) {
            tag.putUUID("HealTarget", this.healTargetId);
        }
        tag.putInt("Shield", getShield());
        tag.putBoolean("Elite", isElite());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("HealTarget")) {
            this.healTargetId = tag.getUUID("HealTarget");
        }
        if (tag.contains("Elite")) {
            this.entityData.set(DATA_ELITE, tag.getBoolean("Elite"));
            refreshDimensions();
        }
        if (tag.contains("Shield")) {
            setShield(tag.getInt("Shield"));
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4096.0D;
    }
}
