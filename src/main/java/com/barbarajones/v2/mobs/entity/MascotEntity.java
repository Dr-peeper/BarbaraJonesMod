package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.MascotBuffAuraGoal;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * THE MASCOT - a rare, unsettlingly cheerful cereal-box-headed thing. It has
 * NO attack goal and NO target selector at all: it can never hurt the player.
 * Instead {@link MascotBuffAuraGoal} pulses buffs onto every nearby
 * {@link CravelingKin}, and {@link AvoidEntityGoal} makes it bolt the instant
 * a player gets close - "flees when approached" from the brief, using the
 * same vanilla fleeing utility Endermen/foxes use, just aimed permanently at
 * {@link Player}.
 *
 * <p>Deliberately fragile ({@link #createAttributes()}) so that catching one
 * (worth real drops - see the loot table) is a real, if brief, chase.
 */
public class MascotEntity extends PathfinderMob {

    public MascotEntity(EntityType<? extends MascotEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.4D, 1.8D));
        this.goalSelector.addGoal(2, new MascotBuffAuraGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        // deliberately no targetSelector goals at all - The Mascot never attacks anything
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.MASCOT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.MASCOT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.MASCOT_DEATH.get();
    }
}
