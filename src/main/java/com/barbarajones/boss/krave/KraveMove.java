package com.barbarajones.boss.krave;

import com.barbarajones.entity.KraveMonster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * One named attack in the Krave Monster's book.
 *
 * <p>Every move is telegraph, strike, recover. That shape is not decoration: an
 * attack with no wind-up cannot be dodged, so it stops being a fight and becomes
 * a damage tick you stand in. Mom Cobb's fight already proved the pattern here -
 * hers is the one boss in the mod that reads as readable - so this is the same
 * idea generalised, because seven forms of hand-rolled attacks would be
 * unmaintainable within a week.
 *
 * <p>The split also gives the renderer something to hang a tell on: {@code windup}
 * ticks are exactly the window where a pose or a glow should be building.
 */
public final class KraveMove {

    /** What actually happens when the move lands, after the telegraph. */
    @FunctionalInterface
    public interface Strike {
        void run(ServerLevel level, KraveMonster boss, LivingEntity target);
    }

    /** Whether the move is even worth choosing right now. */
    @FunctionalInterface
    public interface Usable {
        boolean test(KraveMonster boss, LivingEntity target, double distance);
    }

    public final String name;
    public final int windup;
    public final int cooldown;
    public final int weight;
    private final Usable usable;
    private final Strike strike;

    public KraveMove(String name, int windup, int cooldown, int weight, Usable usable, Strike strike) {
        this.name = name;
        this.windup = windup;
        this.cooldown = cooldown;
        this.weight = weight;
        this.usable = usable;
        this.strike = strike;
    }

    /** A move with no range condition - always a candidate. */
    public static KraveMove any(String name, int windup, int cooldown, int weight, Strike strike) {
        return new KraveMove(name, windup, cooldown, weight, (b, t, d) -> true, strike);
    }

    /** A move that only makes sense within a given reach. */
    public static KraveMove close(String name, int windup, int cooldown, int weight,
                                  double maxDistance, Strike strike) {
        return new KraveMove(name, windup, cooldown, weight,
                (b, t, d) -> d <= maxDistance, strike);
    }

    /** A move that only makes sense from further out than melee. */
    public static KraveMove ranged(String name, int windup, int cooldown, int weight,
                                   double minDistance, Strike strike) {
        return new KraveMove(name, windup, cooldown, weight,
                (b, t, d) -> d >= minDistance, strike);
    }

    /** A move gated on how hurt he is - the desperation attacks. */
    public static KraveMove wounded(String name, int windup, int cooldown, int weight,
                                    float healthFraction, Strike strike) {
        return new KraveMove(name, windup, cooldown, weight,
                (b, t, d) -> b.getHealth() / b.getMaxHealth() <= healthFraction, strike);
    }

    public boolean canUse(KraveMonster boss, LivingEntity target, double distance) {
        return this.usable.test(boss, target, distance);
    }

    public void strike(ServerLevel level, KraveMonster boss, LivingEntity target) {
        this.strike.run(level, boss, target);
    }
}
