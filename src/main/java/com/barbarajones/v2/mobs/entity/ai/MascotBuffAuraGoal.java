package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.entity.CravelingKin;
import com.barbarajones.v2.mobs.entity.MascotEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * The Mascot never fights - instead, every few seconds it pulses Speed and
 * Strength onto every {@link CravelingKin} within range, hyping up the rest
 * of the family. Empty flag set: it never claims movement/look control, so it
 * runs quietly alongside {@code AvoidEntityGoal} (the actual fleeing).
 */
public class MascotBuffAuraGoal extends Goal {

    private static final double RADIUS = 10.0D;
    private static final int PULSE_INTERVAL = 60; // 3s

    private final MascotEntity mascot;
    private int timer;

    public MascotBuffAuraGoal(MascotEntity mascot) {
        this.mascot = mascot;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        return true; // always running in the background, like an idle buff totem
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (timer-- > 0) {
            return;
        }
        timer = PULSE_INTERVAL;

        AABB area = mascot.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> kin = mascot.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e instanceof CravelingKin && e.isAlive());

        if (kin.isEmpty()) {
            return;
        }

        for (LivingEntity entity : kin) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, PULSE_INTERVAL + 20, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, PULSE_INTERVAL + 20, 0, false, false));
        }

        if (mascot.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mascot.getX(), mascot.getY() + mascot.getBbHeight(), mascot.getZ(),
                    8, 0.5D, 0.3D, 0.5D, 0.01D);
        }
    }
}
