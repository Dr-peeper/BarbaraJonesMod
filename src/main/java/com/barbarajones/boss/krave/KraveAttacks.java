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

    // ---- primitives --------------------------------------------------------

    /** A ring of particles on the ground - the readable footprint of an attack. */
    public static void ring(ServerLevel level, Vec3 centre, double radius,
                            ParticleOptions type, int points) {
        for (int i = 0; i < points; i++) {
            double a = (i / (double) points) * Math.PI * 2.0D;
            level.sendParticles(type,
                    centre.x + Math.cos(a) * radius, centre.y + 0.2D, centre.z + Math.sin(a) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Damage plus knockback away from a point. The shape of every slam and burst. */
    public static void blast(ServerLevel level, KraveMonster boss, LivingEntity target,
                             Vec3 centre, double radius, float damage, double knock) {
        for (LivingEntity victim : victims(level, boss, centre, radius, target)) {
            victim.hurt(victim.damageSources().mobAttack(boss), damage);
            Vec3 away = victim.position().subtract(centre).normalize();
            victim.knockback(knock, -away.x, -away.z);
            victim.hurtMarked = true;
        }
    }

    /** An expanding shockwave, drawn and applied over several ticks. */
    public static void shockwave(ServerLevel level, KraveMonster boss, LivingEntity target,
                                 Vec3 centre, double maxRadius, float damage, int rings) {
        for (int r = 1; r <= rings; r++) {
            double radius = maxRadius * (r / (double) rings);
            ring(level, centre, radius, ParticleTypes.CRIT, 16 + r * 6);
        }
        blast(level, boss, target, centre, maxRadius, damage, 0.55D);
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
     * blocks in an arena means permanently rearranging it every fight, and this
     * boss already has one attack whose whole point is that it destroys terrain -
     * the hazards should not be quietly doing it too.
     */
    public static void puddle(ServerLevel level, KraveMonster boss, Vec3 at,
                              float radius, int seconds, MobEffectInstance effect) {
        net.minecraft.world.entity.AreaEffectCloud cloud =
                new net.minecraft.world.entity.AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(boss);
        cloud.setRadius(radius);
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

    /** A telegraphed circle that erupts after a delay - geysers and minefields. */
    public static void delayedEruption(ServerLevel level, KraveMonster boss, LivingEntity target,
                                       Vec3 at, double radius, float damage, int delayTicks,
                                       ParticleOptions warning, ParticleOptions blast) {
        // The warning is drawn immediately and the hit lands later; the gap is
        // the entire fairness of the attack.
        ring(level, at, radius, warning, 20);
        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, delayTicks, () -> {
            if (!boss.isAlive()) {
                return;
            }
            for (int i = 0; i < 3; i++) {
                ring(level, at.add(0.0D, i * 0.6D, 0.0D), radius * (1.0D - i * 0.15D), blast, 24);
            }
            blast(level, boss, target, at, radius, damage, 0.8D);
            level.playSound(null, BlockPos.containing(at), ModSounds.KRAVE_BOOM.get(),
                    SoundSource.HOSTILE, 1.3F, 1.1F);
        });
    }

    /** Drags everything toward a point - whirlpool and singularity. */
    public static void pullToward(ServerLevel level, KraveMonster boss, LivingEntity target,
                                  Vec3 centre, double radius, double strength) {
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
