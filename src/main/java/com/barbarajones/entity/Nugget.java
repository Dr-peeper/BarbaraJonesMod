package com.barbarajones.entity;

import com.barbarajones.content.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Nugget - Barbara's ginger cat, the one she is forever hollering for.
 *
 * Extends the vanilla Cat so the cat rig and model apply directly (and so she
 * is tameable, which suits her), but with her own simplified goal set: she
 * follows nuggets and donuts rather than raw fish.
 */
public class Nugget extends Cat {

    private static final Ingredient TREATS = Ingredient.of(
            ModItems.CHICKEN_NUGGETS.get(), ModItems.DONUT.get());

    public Nugget(EntityType<? extends Cat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // deliberately NOT calling super: vanilla cat AI (ocelot fear, sitting on
        // beds, hunting chickens) is replaced with her own simple behaviour.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, TREATS, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return TREATS.test(stack);
    }

    @Override
    @Nullable
    public Cat getBreedOffspring(ServerLevel level, AgeableMob other) {
        return com.barbarajones.content.ModEntities.NUGGET.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAT_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
