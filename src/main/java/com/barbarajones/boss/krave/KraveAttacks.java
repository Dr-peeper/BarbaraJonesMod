package com.barbarajones.boss.krave;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The building blocks every Krave Monster attack is assembled from.
 *
 * <p>Thirty named moves across seven forms are not thirty unrelated pieces of
 * code - they are a dozen primitives in different combinations, at different
 * sizes and speeds. Keeping the primitives here is what makes the movesets
 * readable as a list of intentions rather than a wall of particle calls, and it
 * means tuning "how hard does a shockwave hit" happens in exactly one place.
 *
 * <p>Two rules hold throughout. Cayden is never hit by area attacks he is not
 * the target of - he is in melee range constantly by design and would otherwise
 * be deleted by every sweep the boss throws. And nothing here damages through
 * walls: every area effect checks line of sight, because a boss that hits you
 * through the arena floor reads as broken rather than difficult.
 */
public final class KraveAttacks {

    private KraveAttacks() { }

    // ---- how big an attack actually is -------------------------------------

    /** Form one's collision width, the baseline every radius was tuned against. */
    private static final double BASE_WIDTH = 4.2D;

    /**
     * The gap between "a boss move" and "a catastrophe".
     *
     * <p>The movesets were written at a sane, readable scale - a five-block
     * sweep, a seven-block wave. At that size the attacks are perfectly correct
     * and completely unremarkable: they land inside the patch of ground you are
     * already standing on, and the only way to tell one from another is the
     * particle colour. This multiplier is what turns them into weather.
     */
    private static final double SPECTACLE = 2.4D;

    /**
     * Nothing he throws covers less than this.
     *
     * <p>A floor rather than a bigger multiplier, because the small moves are
     * the ones that need it most: scaling a two-block minefield tick by anything
     * still leaves a two-block minefield tick. Every attack now sweeps ground
     * you have to actually run out of.
     */
    private static final double MIN_REACH = 10.0D;

    /**
     * And a ceiling, because the top-form finisher multiplied out past a hundred
     * blocks - which is not more impressive than twenty-four, it just means the
     * attack has no edge and there is nowhere to stand that is not already
     * inside it. A move you cannot escape is not a move.
     */
    private static final double MAX_REACH = 24.0D;

    /**
     * How much bigger he is than the form the numbers were written for.
     *
     * <p>Every radius in the movesets was picked against a monster about four
     * blocks wide. By the Krave God he is eleven wide and twenty-two tall, so a
     * ring at radius four was being drawn INSIDE him and a shockwave finished
     * before it cleared his own feet.
     */
    public static double sizeFactor(KraveMonster boss) {
        return Math.max(1.0D, boss.getBbWidth() / BASE_WIDTH);
    }

    /** A tuned radius, grown to what he is now and to what the fight should look like. */
    public static double reach(KraveMonster boss, double base) {
        return Mth.clamp(base * sizeFactor(boss) * SPECTACLE, MIN_REACH, MAX_REACH);
    }

    // ---- targeting ---------------------------------------------------------

    /**
     * Everything the boss is willing to hurt inside a radius.
     *
     * <p>Cayden is excluded unless he IS the target: he stands in melee range for
     * the entire fight, so letting every shockwave catch him means he dies to
     * splash damage he had no way to avoid.
     */
    public static List<LivingEntity> victims(ServerLevel level, KraveMonster boss,
                                             Vec3 centre, double radius, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : level.getEntities(boss,
                boss.getBoundingBox().inflate(radius + 8.0D))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (living instanceof com.barbarajones.entity.KraveMinion) {
                continue;   // his own kids
            }
            if (living instanceof com.barbarajones.entity.CaydenCobb && living != target) {
                continue;   // splash does not get to delete the boy
            }
            if (living.position().distanceTo(centre) > radius) {
                continue;
            }
            // Nothing lands through the arena floor.
            if (!boss.hasLineOfSight(living)) {
                continue;
            }
            out.add(living);
        }
        return out;
    }

    // ---- drawing -----------------------------------------------------------

    /**
     * A ring of particles on the ground - the readable footprint of an attack.
     *
     * <p>Takes an exact radius. Callers working from a moveset number want the
     * boss-aware overload below, so the circle they draw is the circle that hits.
     */
    public static void ring(ServerLevel level, Vec3 centre, double radius,
                            ParticleOptions type, int points) {
        // Density scales with circumference. A fixed point count draws a solid
        // ring at radius two and a dotted line of six specks at radius twenty,
        // which is exactly backwards - the big attacks are the ones that need
        // to read.
        // One position is one packet, however many particles it carries, so a
        // ring at radius twenty-four costs five hundred packets at a point per
        // seven degrees. Fewer positions each carrying more particles, spread a
        // little wider, draws the same solid ring for a quarter of the traffic.
        int drawn = Math.max(points, (int) Math.ceil(radius * 4.0D));
        for (int i = 0; i < drawn; i++) {
            double a = (i / (double) drawn) * Math.PI * 2.0D;
            double x = centre.x + Math.cos(a) * radius;
            double z = centre.z + Math.sin(a) * radius;
            // Lifted clear of the ground: down at floor level most of it is
            // swallowed by the terrain mesh and by his own feet.
            level.sendParticles(type, x, centre.y + 0.9D, z, 5, 0.22D, 0.45D, 0.22D, 0.01D);
        }
    }

    /** The same ring, sized the way the attack that draws it is sized. */
    public static void ring(ServerLevel level, KraveMonster boss, Vec3 centre,
                            double baseRadius, ParticleOptions type, int points) {
        ring(level, centre, reach(boss, baseRadius), type, points);
    }

    /**
     * A wall of vertical columns standing on the ring.
     *
     * <p>A flat circle on the floor is invisible from anywhere except directly
     * above it, and the fight is not fought from above. Columns are what make a
     * twenty-block attack legible while you are standing inside it, and what
     * gives the edge of the move a shape you can watch yourself crossing.
     */
    public static void pillars(ServerLevel level, Vec3 centre, double radius,
                               int count, double height, ParticleOptions type) {
        for (int i = 0; i < count; i++) {
            double a = (i / (double) count) * Math.PI * 2.0D;
            double x = centre.x + Math.cos(a) * radius;
            double z = centre.z + Math.sin(a) * radius;
            for (double y = 0.0D; y < height; y += 0.7D) {
                level.sendParticles(type, x, centre.y + y, z, 2, 0.12D, 0.12D, 0.12D, 0.02D);
            }
        }
    }

    /**
     * A hemisphere shell, for the bursts meant to swallow the arena rather than
     * sweep across it.
     */
    public static void dome(ServerLevel level, Vec3 centre, double radius, ParticleOptions type) {
        int rings = Math.max(4, (int) (radius / 2.5D));
        for (int r = 0; r < rings; r++) {
            double lat = (r / (double) rings) * (Math.PI / 2.0D);
            double y = Math.sin(lat) * radius;
            double band = Math.cos(lat) * radius;
            int points = Math.max(10, (int) (band * 4.0D));
            for (int i = 0; i < points; i++) {
                double a = (i / (double) points) * Math.PI * 2.0D;
                level.sendParticles(type,
                        centre.x + Math.cos(a) * band, centre.y + y, centre.z + Math.sin(a) * band,
                        1, 0.05D, 0.05D, 0.05D, 0.01D);
            }
        }
    }

    // ---- primitives --------------------------------------------------------

    /**
     * Damage plus knockback away from a point, and the ground where it landed.
     *
     * <p>The shape of every slam and burst. Takes a moveset radius and grows it;
     * see {@link #blastExact} for the callers that have already done that.
     */
    public static void blast(ServerLevel level, KraveMonster boss, LivingEntity target,
                             Vec3 centre, double baseRadius, float damage, double knock) {
        blastExact(level, boss, target, centre, reach(boss, baseRadius), damage, knock);
    }

    /**
     * The same hit, at a radius that is already final.
     *
     * <p>This exists because scaling twice is a real bug with an unfair outcome:
     * a move that draws its warning circle at one size and then hits at another
     * reads as the boss cheating. Any caller that has drawn the circle itself
     * must land the hit through here.
     */
    public static void blastExact(ServerLevel level, KraveMonster boss, LivingEntity target,
                                  Vec3 centre, double radius, float damage, double knock) {
        for (LivingEntity victim : victims(level, boss, centre, radius, target)) {
            victim.hurt(victim.damageSources().mobAttack(boss), damage);
            Vec3 away = victim.position().subtract(centre).normalize();
            victim.knockback(knock, -away.x, -away.z);
            victim.hurtMarked = true;
        }
        // And it takes the scenery with it. Shallow and on the small budget: a
        // passing hit scours off what is standing rather than digging, because
        // the craters belong to the moves that earn them - and because this one
        // fires several times a second during a claw combo.
        KraveDemolition.carve(level, boss, centre, radius, 5, 1,
                KraveDemolition.BUDGET_LIGHT);
    }

    /**
     * An expanding shockwave: drawn, applied, and carved as it travels.
     *
     * <p>The one move that should never be mistaken for a particle effect. The
     * ring is walled in columns so it reads from ground level, and the terrain
     * it crosses does not survive it.
     */
    public static void shockwave(ServerLevel level, KraveMonster boss, LivingEntity target,
                                 Vec3 centre, double baseRadius, float damage, int rings) {
        double maxRadius = reach(boss, baseRadius);
        // Drawn over time rather than all at once. Every ring appearing on the
        // same tick is one flash you can miss by blinking; expanding outward is
        // what makes it read as a wave travelling toward you.
        for (int r = 1; r <= rings; r++) {
            final double radius = maxRadius * (r / (double) rings);
            final boolean front = r == rings;
            com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, r * 3, () -> {
                ring(level, centre, radius, ParticleTypes.CRIT, 24);
                ring(level, centre, radius * 0.94D, ParticleTypes.LARGE_SMOKE, 20);
                // Only the leading edge gets the wall, so it reads as a front
                // moving outward rather than a solid cylinder of particles.
                if (front) {
                    pillars(level, centre, radius, 28, 4.5D, ParticleTypes.SOUL_FIRE_FLAME);
                }
            });
        }
        blastExact(level, boss, target, centre, maxRadius, damage, 0.55D);
        KraveDemolition.carveWave(level, boss, centre, maxRadius, rings, 6, 2);
        sound(level, boss, ModSounds.KRAVE_BOOM.get(), 1.6F, 0.7F);
    }

    /**
     * A cereal projectile, thrown as a real entity so it arcs and can be dodged.
     *
     * <p>Reuses the mouth beam rather than adding another projectile type: it
     * already handles owner attribution and impact, and a second nearly-identical
     * entity would be two things to keep in step forever.
     */
    public static void spit(ServerLevel level, KraveMonster boss, Vec3 aim, double spread) {
        Vec3 from = boss.position()
                .add(0.0D, boss.getBbHeight() * 0.75D, 0.0D)
                .add(boss.getViewVector(1.0F).scale(boss.getBbWidth() * 0.6D));
        Vec3 scattered = aim.add(
                (level.random.nextDouble() - 0.5D) * spread,
                (level.random.nextDouble() - 0.5D) * spread * 0.4D,
                (level.random.nextDouble() - 0.5D) * spread);
        level.addFreshEntity(new com.barbarajones.entity.KraveMouthBeam(level, boss, from, scattered));
    }

    /** A burst of projectiles at once - scattershot, barrage, gatling. */
    public static void volley(ServerLevel level, KraveMonster boss, LivingEntity target,
                              int shots, double spread) {
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < shots; i++) {
            spit(level, boss, aim, spread);
        }
        sound(level, boss, ModSounds.KRAVE_BEAM_FIRE.get(), 1.1F, 1.3F);
    }

    /**
     * A patch of ground that hurts to stand on, for chocolate trails and milk.
     *
     * <p>Built as an area-effect cloud rather than by placing blocks. Placing
     * blocks means a lingering hazard permanently rearranging the arena, and this
     * boss already tears it up quite enough on the beats that mean to.
     */
    public static void puddle(ServerLevel level, KraveMonster boss, Vec3 at,
                              float radius, int seconds, MobEffectInstance effect) {
        net.minecraft.world.entity.AreaEffectCloud cloud =
                new net.minecraft.world.entity.AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(boss);
        // Grown, but NOT through reach() - that has a ten-block floor, which is
        // right for a hit that lands once and wrong for a hazard that sits on
        // the arena for twenty seconds. The Last Bowl drops a dozen of these,
        // and at ten blocks apiece they merge into one unbroken carpet with no
        // gaps to stand in, which is the move's entire mechanic gone. Scaled to
        // his size only, and capped.
        cloud.setRadius((float) Math.min(radius * sizeFactor(boss), 9.0D));
        cloud.setDuration(seconds * 20);
        cloud.setRadiusPerTick(0.0F);
        cloud.setParticle(ParticleTypes.FALLING_HONEY);
        cloud.addEffect(effect);
        level.addFreshEntity(cloud);
    }

    /** Krave minions, for the swarm forms. */
    public static void summonMinions(ServerLevel level, KraveMonster boss, int count) {
        for (int i = 0; i < count; i++) {
            com.barbarajones.entity.KraveMinion minion =
                    ModEntities.KRAVE_MINION.get().create(level);
            if (minion == null) {
                continue;
            }
            double a = level.random.nextDouble() * Math.PI * 2.0D;
            double r = 2.5D + level.random.nextDouble() * 4.0D;
            minion.setPos(boss.getX() + Math.cos(a) * r, boss.getY() + 1.0D,
                    boss.getZ() + Math.sin(a) * r);
            minion.setTarget(boss.getTarget());
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.POOF,
                    minion.getX(), minion.getY() + 0.5D, minion.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    /** Eats his own minions to heal - Devour, and the Krave God's hunger. */
    public static int devourMinions(ServerLevel level, KraveMonster boss, float healEach) {
        int eaten = 0;
        for (Entity e : level.getEntities(boss, boss.getBoundingBox().inflate(16.0D))) {
            if (!(e instanceof com.barbarajones.entity.KraveMinion minion) || !minion.isAlive()) {
                continue;
            }
            minion.discard();
            boss.heal(healEach);
            eaten++;
            level.sendParticles(ParticleTypes.HEART,
                    boss.getX(), boss.getY() + boss.getBbHeight() * 0.7D, boss.getZ(),
                    3, 0.5D, 0.5D, 0.5D, 0.0D);
        }
        return eaten;
    }

    /**
     * A telegraphed circle that erupts after a delay - geysers and minefields.
     *
     * <p>The warning and the hit are the same size on purpose. The radius is
     * grown once, here, and both the circle and the damage use that one number.
     */
    public static void delayedEruption(ServerLevel level, KraveMonster boss, LivingEntity target,
                                       Vec3 at, double baseRadius, float damage, int delayTicks,
                                       ParticleOptions warning, ParticleOptions blast) {
        double radius = reach(boss, baseRadius);
        // The warning is drawn now and the hit lands later; that gap is the
        // entire fairness of the attack, and at this size the circle needs a wall
        // on it or you will not notice you are standing inside one.
        ring(level, at, radius, warning, 24);
        pillars(level, at, radius, 20, 2.5D, warning);

        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, delayTicks, () -> {
            if (!boss.isAlive()) {
                return;
            }
            // A column going up, not a disc lying down: this one is supposed to
            // erupt, and it should be visible from the far side of the arena.
            for (int i = 0; i < 6; i++) {
                ring(level, at.add(0.0D, i * 1.2D, 0.0D), radius * (1.0D - i * 0.10D), blast, 26);
            }
            pillars(level, at, radius * 0.5D, 16, 9.0D, blast);
            blastExact(level, boss, target, at, radius, damage, 0.8D);
            KraveDemolition.crater(level, boss, at, radius * 0.8D, 5);
            level.playSound(null, BlockPos.containing(at), ModSounds.KRAVE_BOOM.get(),
                    SoundSource.HOSTILE, 1.5F, 1.1F);
        });
    }

    /**
     * The arena-ending hit: a dome, a full-radius wave, and a crater under it.
     *
     * <p>For the finishers, where the intended outcome is that the ground stops
     * being there.
     */
    public static void cataclysm(ServerLevel level, KraveMonster boss, LivingEntity target,
                                 Vec3 centre, double baseRadius, float damage) {
        double radius = reach(boss, baseRadius);
        dome(level, centre, radius, ParticleTypes.SOUL_FIRE_FLAME);
        pillars(level, centre, radius, 40, 12.0D, ParticleTypes.FLAME);
        blastExact(level, boss, target, centre, radius, damage, 2.0D);
        KraveDemolition.carveWave(level, boss, centre, radius, 6, 14, 3);
        KraveDemolition.crater(level, boss, centre, radius * 0.45D, 7);
        sound(level, boss, ModSounds.KRAVE_BOOM.get(), 2.0F, 0.5F);
    }

    /** Drags everything toward a point - whirlpool and singularity. */
    public static void pullToward(ServerLevel level, KraveMonster boss, LivingEntity target,
                                  Vec3 centre, double baseRadius, double strength) {
        double radius = reach(boss, baseRadius);
        for (LivingEntity victim : victims(level, boss, centre, radius, target)) {
            Vec3 toward = centre.subtract(victim.position()).normalize().scale(strength);
            victim.setDeltaMovement(victim.getDeltaMovement().add(toward.x, toward.y * 0.3D, toward.z));
            victim.hurtMarked = true;
        }
    }

    public static void sound(ServerLevel level, KraveMonster boss, SoundEvent event,
                             float volume, float pitch) {
        level.playSound(null, boss.blockPosition(), event, SoundSource.HOSTILE, volume, pitch);
    }

    public static void announce(ServerLevel level, KraveMonster boss, String text) {
        for (Player p : level.getEntitiesOfClass(Player.class,
                boss.getBoundingBox().inflate(64.0D))) {
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    net.minecraft.ChatFormatting.DARK_RED + "" + net.minecraft.ChatFormatting.BOLD
                    + text));
        }
    }

    /** Slowness in milk, used by several of the fluid attacks. */
    public static MobEffectInstance milkSlow(int seconds, int amplifier) {
        return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, seconds * 20, amplifier);
    }
}
