package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.entity.CravelingEntity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.block.Blocks;

/**
 * Craveling's bite: an ordinary melee swing, but connecting sprays a burst of
 * dry cereal crumbs and has a chance to leave the crumbs literally underfoot -
 * a short Slowness, as if the target is now standing in gritty spilled cereal.
 * This is the one bit of "genuinely distinct" combat Craveling gets over a
 * plain zombie; everything else about its kit is the overworld baseline.
 */
public class CravelingCrumbleAttackGoal extends MeleeAttackGoal {

    private final CravelingEntity craveling;

    public CravelingCrumbleAttackGoal(CravelingEntity craveling, double speedModifier, boolean followEvenIfNotSeen) {
        super(craveling, speedModifier, followEvenIfNotSeen);
        this.craveling = craveling;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distToTargetSqr) {
        double reach = this.getAttackReachSqr(target);
        boolean couldAttack = distToTargetSqr <= reach && this.isTimeToAttack();
        super.checkAndPerformAttack(target, distToTargetSqr);

        if (couldAttack && !craveling.level().isClientSide && craveling.level() instanceof ServerLevel server) {
            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BROWN_CONCRETE_POWDER.defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    10, 0.3D, 0.3D, 0.3D, 0.02D);

            if (craveling.getRandom().nextFloat() < 0.35F) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
        }
    }
}
