package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.entity.SoggyEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

/**
 * Soggy doesn't throw a normal punch - it belly-flops. A connecting hit
 * shoves the target hard (well past ordinary zombie knockback) and, being
 * waterlogged and top-heavy, Soggy itself stumbles for a moment afterward
 * (a short self-inflicted Slowness). Slow to approach, but landing a hit on
 * it is genuinely costly to the target's positioning.
 */
public class SoggySlamAttackGoal extends MeleeAttackGoal {

    private final SoggyEntity soggy;

    public SoggySlamAttackGoal(SoggyEntity soggy, double speedModifier, boolean followEvenIfNotSeen) {
        super(soggy, speedModifier, followEvenIfNotSeen);
        this.soggy = soggy;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTargetSqr) {
        double reach = this.getAttackReachSqr(target);
        boolean willAttack = distToTargetSqr <= reach && this.isTimeToAttack();
        super.checkAndPerformAttack(target, distToTargetSqr);

        if (willAttack) {
            Vec3 push = target.position().subtract(soggy.position()).normalize().scale(1.4D);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.45D, push.z));
            target.hurtMarked = true;

            soggy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));

            if (soggy.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH,
                        target.getX(), target.getY() + target.getBbHeight() * 0.4D, target.getZ(),
                        12, 0.4D, 0.3D, 0.4D, 0.05D);
            }
        }
    }
}
