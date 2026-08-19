package com.barbarajones.entity.barbara;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * One lungful, left hanging in the air where she blew it.
 *
 * <p>Deliberately not an entity. A cloud has no model, no collision and no
 * saved state worth keeping across a reload, so it lives as a plain object on
 * {@link BarbaraCombat} and paints itself with server-sent particles. That
 * keeps the entity registry for things that actually need to be rendered.
 */
public class SmokeScreenCloud {

    /** Effects reapply on this cadence rather than every tick - a haze, not a stun-lock. */
    private static final int PULSE = 20;

    private final Vec3 centre;
    private final float radius;
    private final boolean laced;
    private int life;

    public SmokeScreenCloud(Vec3 centre, float radius, int life, boolean laced) {
        this.centre = centre;
        this.radius = radius;
        this.life = life;
        this.laced = laced;
    }

    public boolean isDone() {
        return this.life <= 0;
    }

    public Vec3 centre() {
        return this.centre;
    }

    public void tick(ServerLevel level, BarbaraJones barbara) {
        this.life--;
        if (this.life % 3 == 0) {
            paint(level);
        }
        if (this.life % PULSE != 0) {
            return;
        }
        int amp = this.laced ? 2 : 1;
        for (LivingEntity foe : SmokeTargets.foesWithin(barbara, this.centre, this.radius)) {
            foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 90, 0, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, amp, false, false));
            if (this.laced) {
                foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0, false, false));
            }
            // The whole point of the screen: anything hunting Cayden loses him in
            // it and comes for the woman who made the smoke instead.
            if (foe instanceof Mob mob && mob.getTarget() instanceof CaydenCobb) {
                mob.setTarget(barbara);
            }
        }
    }

    private void paint(ServerLevel level) {
        double spread = this.radius * 0.45D;
        int puffs = this.laced ? 14 : 9;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.centre.x, this.centre.y + 0.7D,
                this.centre.z, puffs, spread, 0.7D, spread, 0.01D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.centre.x, this.centre.y + 0.2D,
                this.centre.z, 3, spread, 0.3D, spread, 0.008D);
        if (this.laced && this.life % 12 == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.centre.x, this.centre.y + 1.0D,
                    this.centre.z, 4, spread, 0.6D, spread, 0.0D);
        }
    }
}
