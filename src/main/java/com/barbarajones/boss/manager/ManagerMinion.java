package com.barbarajones.boss.manager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * An Associate: one of the interchangeable men in ties The Manager summons
 * during his "Do you have a reservation?" phase to walk you out of the
 * building. Fast, fragile, and completely without initiative - when the boss
 * who hired them stops existing, so do they.
 */
public class ManagerMinion extends Monster {

    /** How long an Associate survives without a boss before clocking out. */
    private static final int ORPHAN_GRACE = 60;

    @Nullable
    private UUID bossId;
    private int orphanTicks;

    public ManagerMinion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setBoss(TheManager manager) {
        this.bossId = manager.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || this.bossId == null) {
            return;
        }
        // Checked on a slow cadence: resolving a UUID walks the level's entity
        // index, and there can be six of these in the room at once.
        if (this.tickCount % 20 != 0) {
            return;
        }
        boolean bossAlive = level() instanceof ServerLevel server
                && server.getEntity(this.bossId) instanceof TheManager manager
                && manager.isAlive();
        if (bossAlive) {
            this.orphanTicks = 0;
            return;
        }
        this.orphanTicks += 20;
        if (this.orphanTicks < ORPHAN_GRACE) {
            return;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 1.0D, getZ(), 15, 0.3D, 0.6D, 0.3D, 0.02D);
        }
        playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.3F);
        discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.bossId != null) {
            tag.putUUID("ManagerBoss", this.bossId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("ManagerBoss")) {
            this.bossId = tag.getUUID("ManagerBoss");
        }
    }
}
