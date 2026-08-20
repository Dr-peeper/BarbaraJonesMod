package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.LoomweaverWebTrapGoal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * LOOMWEAVER - a low, many-legged thing built out of clumped cereal and
 * hardened milk-strands (see {@link com.barbarajones.v2.mobs.client.LoomweaverModel},
 * a bespoke multi-leg rig, not a reused biped). Its signature move is
 * {@link LoomweaverWebTrapGoal}, dropping sticky milk webbing under a target
 * instead of just closing to bite. Like vanilla's spider it can climb sheer
 * walls the instant it bumps into one (see {@link #tick()}) - that mechanic
 * is copied deliberately from {@code Spider}, since it is exactly the "many
 * legged thing that isn't afraid of walls" behaviour the brief wants.
 */
public class LoomweaverEntity extends Monster implements CravelingKin {

    private static final EntityDataAccessor<Byte> DATA_FLAGS =
            SynchedEntityData.defineId(LoomweaverEntity.class, EntityDataSerializers.BYTE);

    public LoomweaverEntity(EntityType<? extends LoomweaverEntity> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLAGS, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new LoomweaverWebTrapGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            setClimbing(this.horizontalCollision);
        }
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte flags = this.entityData.get(DATA_FLAGS);
        flags = climbing ? (byte) (flags | 1) : (byte) (flags & ~1);
        this.entityData.set(DATA_FLAGS, flags);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.LOOMWEAVER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.LOOMWEAVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.LOOMWEAVER_DEATH.get();
    }
}
