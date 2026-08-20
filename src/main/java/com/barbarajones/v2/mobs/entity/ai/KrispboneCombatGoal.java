package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.entity.KrispboneEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Krispbone does not have a melee-attack goal at all - this single goal both
 * positions it (kites backward when the target closes in, advances when the
 * target is too far to hit) and fires the shard bursts. That is the
 * "signature move: flicks hardened Krave shards in a short spread" behaviour;
 * it is deliberately a skirmisher's kit, not a skeleton clone with a reskinned
 * arrow.
 */
public class KrispboneCombatGoal extends Goal {

    private static final double MIN_DISTANCE_SQR = 5.0D * 5.0D;   // start backing off inside this
    private static final double MAX_DISTANCE_SQR = 14.0D * 14.0D; // stop chasing beyond this
    private static final double IDEAL_DISTANCE_SQR = 8.0D * 8.0D;

    private final KrispboneEntity krispbone;
    private final double speedModifier;
    private int burstCooldown;
    private int seeTime;

    public KrispboneCombatGoal(KrispboneEntity krispbone, double speedModifier) {
        this.krispbone = krispbone;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = krispbone.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        krispbone.getNavigation().stop();
        seeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = krispbone.getTarget();
        if (target == null) {
            return;
        }

        double distSqr = krispbone.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = krispbone.getSensing().hasLineOfSight(target);
        seeTime = canSee ? seeTime + 1 : Math.max(0, seeTime - 1);

        krispbone.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (distSqr < MIN_DISTANCE_SQR) {
            // too close - back away rather than trading melee hits
            krispbone.getNavigation().moveTo(
                    krispbone.getX() + (krispbone.getX() - target.getX()),
                    krispbone.getY(),
                    krispbone.getZ() + (krispbone.getZ() - target.getZ()),
                    speedModifier);
        } else if (distSqr > MAX_DISTANCE_SQR) {
            krispbone.getNavigation().moveTo(target, speedModifier);
        } else {
            krispbone.getNavigation().stop();
        }

        if (burstCooldown > 0) {
            burstCooldown--;
        } else if (distSqr <= MAX_DISTANCE_SQR && distSqr >= MIN_DISTANCE_SQR * 0.4D
                && seeTime >= 5) {
            krispbone.fireShardBurst(target);
            // shorter interval the closer the ideal skirmish range is
            burstCooldown = distSqr < IDEAL_DISTANCE_SQR ? 35 : 25;
        }
    }
}
