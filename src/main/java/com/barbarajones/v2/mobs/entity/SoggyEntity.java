package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.SoggySlamAttackGoal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
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

/**
 * SOGGY - a bloated, waterlogged Kraveling. Slow and tanky rather than fast:
 * heavy knockback resistance, a lumbering belly-flop attack
 * ({@link SoggySlamAttackGoal}) instead of a normal swing, and on death it
 * bursts into a milk splash (a vanilla {@link AreaEffectCloud} with
 * Slowness) that punishes anyone who finished it off in melee range.
 */
public class SoggyEntity extends Monster implements KravelingKin {

    private static final float SPLASH_RADIUS = 3.0F;
    private static final int SPLASH_DURATION = 100; // 5s

    public SoggyEntity(EntityType<? extends SoggyEntity> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.ARMOR, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SoggySlamAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            AreaEffectCloud splash = new AreaEffectCloud(level(), getX(), getY(), getZ());
            splash.setRadius(SPLASH_RADIUS);
            splash.setDuration(SPLASH_DURATION);
            splash.setRadiusPerTick(-splash.getRadius() / (float) SPLASH_DURATION);
            splash.setParticle(ParticleTypes.SPLASH);
            splash.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            level().addFreshEntity(splash);
            level().playSound(null, blockPosition(), ModMobSounds.SOGGY_SPLASH.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.SOGGY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.SOGGY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.SOGGY_DEATH.get();
    }
}
