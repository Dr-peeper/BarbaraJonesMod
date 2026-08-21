package com.barbarajones.boss.krave;

import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the Krave Monster's moveset: pick, telegraph, strike, recover.
 *
 * <p>Written as one goal driving a table rather than a goal per attack. Thirty
 * attacks as thirty goals would mean thirty priority decisions, and the priority
 * list is the wrong tool for "which of these is interesting right now" - goal
 * priority answers which behaviour WINS, not which one is worth choosing.
 *
 * <p>The wind-up is not decoration. He is rooted for the whole tell, because an
 * attack you cannot see coming is not difficulty, it is damage you stand in. The
 * same reasoning as Mom Cobb's fight, which is the one boss here that already
 * reads clearly.
 */
public class KraveMovesetGoal extends Goal {

    private final KraveMonster boss;

    /** Per-move cooldowns, by name, so an inherited move shares its timer. */
    private final Map<String, Integer> cooldowns = new HashMap<>();

    private KraveMove pending;
    private int windupLeft;
    private int globalCooldown;

    public KraveMovesetGoal(KraveMonster boss) {
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.boss.getTarget();
        return target != null && target.isAlive() && this.boss.distanceToSqr(target) < 40.0D * 40.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        this.pending = null;
        this.windupLeft = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = this.boss.getTarget();
        if (target == null || !(this.boss.level() instanceof ServerLevel level)) {
            return;
        }
        this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Tick every cooldown down, including moves not currently chosen.
        this.cooldowns.replaceAll((name, ticks) -> Math.max(0, ticks - 1));
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }

        if (this.pending != null) {
            tickWindup(level, target);
            return;
        }
        if (this.globalCooldown > 0) {
            return;
        }
        choose(target);
    }

    /** Rooted, glowing, and about to do something. */
    private void tickWindup(ServerLevel level, LivingEntity target) {
        this.boss.getNavigation().stop();
        // Held in place for the tell. Without this the wind-up happens while he
        // is still walking at you, which makes the telegraph unreadable.
        this.boss.setDeltaMovement(0.0D, this.boss.getDeltaMovement().y, 0.0D);

        level.sendParticles(ParticleTypes.CRIT,
                this.boss.getX(), this.boss.getY() + this.boss.getBbHeight() * 0.8D, this.boss.getZ(),
                3, 0.6D, 0.4D, 0.6D, 0.02D);

        if (--this.windupLeft > 0) {
            return;
        }
        KraveMove move = this.pending;
        this.pending = null;
        try {
            move.strike(level, this.boss, target);
        } catch (Exception failure) {
            // One broken attack must not take the fight - or the server - with it.
            com.mojang.logging.LogUtils.getLogger().error(
                    "Krave Monster move '{}' threw at form {}", move.name, this.boss.getForm(), failure);
        }
        this.cooldowns.put(move.name, move.cooldown);
        this.globalCooldown = 12 + this.boss.getRandom().nextInt(10);
    }

    /** Weighted pick from whatever is off cooldown and in range right now. */
    private void choose(LivingEntity target) {
        double distance = this.boss.distanceTo(target);
        List<KraveMove> options = new ArrayList<>();
        int total = 0;

        for (KraveMove move : KraveMovesets.forForm(this.boss.getForm())) {
            if (this.cooldowns.getOrDefault(move.name, 0) > 0) {
                continue;
            }
            if (!move.canUse(this.boss, target, distance)) {
                continue;
            }
            options.add(move);
            total += move.weight;
        }
        if (options.isEmpty() || total <= 0) {
            return;
        }

        int roll = this.boss.getRandom().nextInt(total);
        for (KraveMove move : options) {
            roll -= move.weight;
            if (roll < 0) {
                this.pending = move;
                this.windupLeft = move.windup;
                return;
            }
        }
    }
}
