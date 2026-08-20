package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.KravelingCrumbleAttackGoal;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.Blocks;

/**
 * KRAVELING - the baseline of the mod's new overworld hostile family: a
 * walking person built out of chunky Krave squares, shedding crumbs as it
 * moves. Meant to be exactly as common as a zombie (see the biome modifier
 * and {@code MobSpawnPlacements}), but it does NOT fight like one:
 * {@link KravelingCrumbleAttackGoal} sprays crumbs and can gum up a target's
 * feet on hit, and unlike Zombie/Skeleton it never overrides sun-sensitivity,
 * so it happily walks around in daylight - cereal doesn't fear the sun.
 *
 * <p>Only ever targets {@link Player}, never any tamed/NPC entity - Cayden
 * Cobb ({@code TamableAnimal}) is never eligible no matter how close he gets.
 */
public class KravelingEntity extends Monster implements KravelingKin {

    public KravelingEntity(EntityType<? extends KravelingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.276D)   // 1.2x a zombie (0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KravelingCrumbleAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Crumb trail: cheap client-only cosmetic, no gameplay effect. Only
        // while actually moving on the ground, so a standing Kraveling isn't
        // constantly shedding.
        if (level().isClientSide && this.onGround()
                && this.getDeltaMovement().horizontalDistanceSqr() > 0.003D
                && this.random.nextInt(3) == 0) {
            level().addParticle(
                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BROWN_CONCRETE_POWDER.defaultBlockState()),
                    getX() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    getY() + 0.05D,
                    getZ() + (random.nextDouble() - 0.5D) * getBbWidth(),
                    0.0D, 0.02D, 0.0D);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.KRAVELING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.KRAVELING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.KRAVELING_DEATH.get();
    }
}
