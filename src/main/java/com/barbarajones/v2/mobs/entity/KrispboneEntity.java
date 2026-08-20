package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.KrispboneCombatGoal;
import com.barbarajones.v2.mobs.entity.projectile.KraveShardEntity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * KRISPBONE - the dry, hollow, cracked-cereal skeleton. Its signature move is
 * flicking three hardened Krave shards in a short spread instead of firing
 * single arrows, and it kites rather than closing to melee - see
 * {@link KrispboneCombatGoal}, its only combat goal.
 */
public class KrispboneEntity extends Monster implements CravelingKin {

    public KrispboneEntity(EntityType<? extends KrispboneEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)   // only ever used if truly cornered
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KrispboneCombatGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Fires three shards in a shallow spread toward {@code target}. Called only by {@link KrispboneCombatGoal}. */
    public void fireShardBurst(LivingEntity target) {
        if (level().isClientSide) {
            return;
        }

        Vec3 from = this.position().add(0.0D, this.getEyeHeight() * 0.8D, 0.0D);
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 dir = aim.subtract(from).normalize();

        // three shards: dead-center, and two offset by a small yaw either side
        float[] spreadDeg = { -8.0F, 0.0F, 8.0F };
        for (float degrees : spreadDeg) {
            Vec3 spread = rotateY(dir, degrees);
            KraveShardEntity shard = new KraveShardEntity(level(), this);
            shard.setPos(from.x, from.y, from.z);
            shard.setDeltaMovement(spread.scale(1.2D));
            level().addFreshEntity(shard);
        }

        level().playSound(null, blockPosition(), ModMobSounds.KRISPBONE_SHOOT.get(),
                SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private static Vec3 rotateY(Vec3 vec, float degrees) {
        float rad = degrees * Mth.DEG_TO_RAD;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.KRISPBONE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.KRISPBONE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.KRISPBONE_DEATH.get();
    }
}
