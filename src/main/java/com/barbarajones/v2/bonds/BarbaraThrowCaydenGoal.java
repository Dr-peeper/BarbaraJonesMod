package com.barbarajones.v2.bonds;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * THE MOVE THAT NEVER WORKED: Barbara picks Cayden up and throws him at
 * whoever she is fighting. A real grab (he freezes mid-whatever-he-was-doing),
 * a real carry (repositioned overhead each tick, not teleported once), a real
 * arc and real impact damage (both already exist in {@code CaydenCobb.launchFrom}
 * / {@code tickThrow} - reused rather than duplicated), and a real landing
 * beat once he is back on the ground.
 *
 * <p>Lives in this package as a plain {@code Goal} plus the entity helper it
 * needs ({@code CaydenCobb.launchFrom}, already public) rather than inside
 * {@code BarbaraJones.java}, which is off limits to edit. Attaching it is a
 * one-line swap - see the module doc - for the existing inner-class
 * {@code BarbaraJones.BarbaraThrowCaydenGoal}, which already does the "throw"
 * half of this (it calls the same {@code launchFrom}) but skips straight from
 * "sees a target" to "already thrown": no grab, no carry, no landing beat, and
 * no cooldown longer than a random 5-10s roll. This version adds all four.
 *
 * <p>State machine, ticked in {@link #tick()}: GRAB (she reaches, he freezes)
 * &rarr; CARRY (he rides overhead, lined up on the target) &rarr; THROWN
 * (delegates to {@code launchFrom} for the actual flight and impact, then
 * watches {@code isNoAi()} flip back to {@code false} - the exact instant
 * {@code CaydenCobb.tickThrow()} detects he has landed - as the landing
 * signal, with no need to reach into any of its private timer state to find
 * it) &rarr; done.
 */
public class BarbaraThrowCaydenGoal extends Goal {

    private enum Phase { GRAB, CARRY, THROWN, DONE }

    private static final int GRAB_TICKS = 8;
    private static final int CARRY_TICKS = 16;
    /** Longest we wait for a landing before letting go anyway - covers him getting stuck mid-flight. */
    private static final int WATCH_TIMEOUT = 140;
    private static final double PICKUP_RANGE = 6.0D;
    private static final double THROW_RANGE = 40.0D;
    private static final int COOLDOWN_MIN = 100;
    private static final int COOLDOWN_RANGE = 140;

    private final BarbaraJones barbara;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int cooldown;
    @Nullable private CaydenCobb carried;

    public BarbaraThrowCaydenGoal(BarbaraJones barbara) {
        this.barbara = barbara;
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.barbara.getTarget();
        return target != null && target.isAlive()
                && this.barbara.distanceToSqr(target) < THROW_RANGE * THROW_RANGE
                && findCayden() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.phase != Phase.DONE;
    }

    @Override
    public void start() {
        this.carried = findCayden();
        this.phaseTicks = 0;
        if (this.carried == null) {
            this.phase = Phase.DONE;
            return;
        }
        this.phase = Phase.GRAB;
        this.barbara.playThrowAnimation();
        this.barbara.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
        this.barbara.lookAt(this.carried, 90.0F, 90.0F);
        this.barbara.getNavigation().stop();
    }

    @Override
    public void tick() {
        CaydenCobb cayden = this.carried;
        if (cayden == null || !cayden.isAlive()) {
            this.phase = Phase.DONE;
            return;
        }
        LivingEntity target = this.barbara.getTarget();

        switch (this.phase) {
            case GRAB -> tickGrab(cayden);
            case CARRY -> tickCarry(cayden, target);
            case THROWN -> tickThrown(cayden);
            default -> this.phase = Phase.DONE;
        }
    }

    /** The windup. He is frozen the instant she reaches for him - the joke is that he never sees it coming. */
    private void tickGrab(CaydenCobb cayden) {
        this.barbara.getNavigation().stop();
        this.barbara.lookAt(cayden, 90.0F, 90.0F);
        cayden.setNoAi(true);
        cayden.setDeltaMovement(Vec3.ZERO);

        if (++this.phaseTicks >= GRAB_TICKS) {
            this.phase = Phase.CARRY;
            this.phaseTicks = 0;
            this.barbara.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F, 1.3F);
            if (this.barbara.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, cayden.getX(), cayden.getY() + 0.3D, cayden.getZ(),
                        10, 0.2D, 0.1D, 0.2D, 0.02D);
            }
        }
    }

    /** Carried overhead, repositioned every tick so it reads as a carry, not a teleport. */
    private void tickCarry(CaydenCobb cayden, @Nullable LivingEntity target) {
        Vec3 look = this.barbara.getLookAngle();
        Vec3 carryPos = this.barbara.position().add(look.x * 0.9D, this.barbara.getBbHeight() * 0.95D, look.z * 0.9D);
        cayden.moveTo(carryPos.x, carryPos.y, carryPos.z, this.barbara.getYRot(), 0.0F);
        cayden.setDeltaMovement(Vec3.ZERO);
        cayden.setNoAi(true);

        if (target != null && target.isAlive()) {
            this.barbara.lookAt(target, 180.0F, 180.0F);
        }

        if (++this.phaseTicks >= CARRY_TICKS) {
            Vec3 aimAt = (target != null && target.isAlive()) ? target.position()
                    : this.barbara.position().add(this.barbara.getLookAngle().scale(20.0D));
            cayden.launchFrom(this.barbara.position().add(0.0D, this.barbara.getBbHeight(), 0.0D), aimAt);
            this.barbara.swing(InteractionHand.MAIN_HAND);
            this.phase = Phase.THROWN;
            this.phaseTicks = 0;
        }
    }

    /** In flight; {@code launchFrom} owns the arc, the trail and the impact damage from here. */
    private void tickThrown(CaydenCobb cayden) {
        boolean landed = !cayden.isNoAi();
        if (landed || ++this.phaseTicks > WATCH_TIMEOUT) {
            if (!landed) {
                cayden.setNoAi(false);   // gave up waiting (he's stuck somewhere) - free him regardless
            }
            landingBeat(cayden);
            this.phase = Phase.DONE;
        }
    }

    /** He lands on his feet. He is annoyed about it. Every single time. */
    private void landingBeat(CaydenCobb cayden) {
        cayden.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.7F);
        if (cayden.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, cayden.getX(), cayden.getY() + 0.2D, cayden.getZ(),
                    8, 0.3D, 0.05D, 0.3D, 0.01D);
        }
        for (Player p : cayden.level().getEntitiesOfClass(Player.class, cayden.getBoundingBox().inflate(32.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "Cayden lands on his feet. He is, predictably, annoyed about it."));
        }
        this.cooldown = COOLDOWN_MIN + this.barbara.getRandom().nextInt(COOLDOWN_RANGE);
    }

    @Override
    public void stop() {
        // Interrupted mid-grab/carry (target died, she got stunned, etc. - the
        // THROWN phase's own timeout already frees him, so only these two need
        // it here) - give him his AI back rather than leaving him frozen.
        if (this.carried != null && (this.phase == Phase.GRAB || this.phase == Phase.CARRY)) {
            this.carried.setNoAi(false);
        }
        this.carried = null;
        this.phase = Phase.DONE;
        this.cooldown = Math.max(this.cooldown, COOLDOWN_MIN / 2);
    }

    @Nullable
    private CaydenCobb findCayden() {
        return this.barbara.level().getEntitiesOfClass(CaydenCobb.class,
                        this.barbara.getBoundingBox().inflate(PICKUP_RANGE),
                        c -> c.isAlive() && !c.isNoAi())
                .stream().findFirst().orElse(null);
    }
}
