package com.barbarajones.entity;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cayden's Mom. Wanders around radiating disappointment and periodically
 * scolds whoever is nearby - the guilt lands as brief Weakness.
 */
public class MomCobb extends PathfinderMob {

    private static final String[] SCOLDS = {
        "Mom: \"Are those stupid irrelevant puns?! I KNEW I should have put you up for adoption.\"",
        "Mom: \"You bought the OFF BRAND, you dumb idiot!\"",
        "Mom: \"That computer had ONE job. And so did you.\"",
        "Mom: \"$500. FIVE. HUNDRED. And you came back with THAT?\"",
        "Mom: \"I'm not mad. I'm just... no, I'm mad.\""
    };

    private int scoldTimer = 200;

    public MomCobb(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.45D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (--this.scoldTimer <= 0) {
            this.scoldTimer = 300 + this.random.nextInt(300);
            scold();
        }
    }

    private void scold() {
        List<Player> near = level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(14.0D, 6.0D, 14.0D));
        if (near.isEmpty()) {
            return;
        }
        String line = SCOLDS[this.random.nextInt(SCOLDS.length)];
        for (Player p : near) {
            p.sendSystemMessage(Component.literal(ChatFormatting.RED + line));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));   // the guilt
        }
        playSound(ModSounds.BARBARA_HURT.get(), 1.2F, 1.15F);
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;   // she saves her voice for the scolding
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BARBARA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BARBARA_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
