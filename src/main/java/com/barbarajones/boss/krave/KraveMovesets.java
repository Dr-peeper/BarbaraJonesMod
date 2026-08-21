package com.barbarajones.boss.krave;

import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.barbarajones.boss.krave.KraveAttacks.announce;
import static com.barbarajones.boss.krave.KraveAttacks.blast;
import static com.barbarajones.boss.krave.KraveAttacks.cataclysm;
import static com.barbarajones.boss.krave.KraveAttacks.delayedEruption;
import static com.barbarajones.boss.krave.KraveAttacks.devourMinions;
import static com.barbarajones.boss.krave.KraveAttacks.milkSlow;
import static com.barbarajones.boss.krave.KraveAttacks.pillars;
import static com.barbarajones.boss.krave.KraveAttacks.puddle;
import static com.barbarajones.boss.krave.KraveAttacks.pullToward;
import static com.barbarajones.boss.krave.KraveAttacks.ring;
import static com.barbarajones.boss.krave.KraveAttacks.shockwave;
import static com.barbarajones.boss.krave.KraveAttacks.sound;
import static com.barbarajones.boss.krave.KraveAttacks.summonMinions;
import static com.barbarajones.boss.krave.KraveAttacks.victims;
import static com.barbarajones.boss.krave.KraveAttacks.volley;

/**
 * What the Krave Monster can do, per form.
 *
 * <p>Each form KEEPS the one before it and adds to it, so form seven genuinely
 * plays as the accumulation of the whole fight rather than a separate boss
 * wearing the same name. That is why every list starts by including the previous
 * one instead of restating it: a change to Bowl Slam is a change to Bowl Slam
 * everywhere it appears, which is the only way thirty-odd moves stay tunable.
 *
 * <p>Weights decide how often a move comes up once it is off cooldown and in
 * range - the signature attack of a form is weighted heavily so the form reads
 * as being ABOUT that attack, and the inherited ones fill the gaps between.
 */
public final class KraveMovesets {

    private KraveMovesets() { }

    /** The whole book for a given form. */
    public static List<KraveMove> forForm(int form) {
        return switch (Math.max(1, Math.min(7, form))) {
            case 1 -> form1();
            case 2 -> form2();
            case 3 -> form3();
            case 4 -> form4();
            case 5 -> form5();
            case 6 -> form6();
            default -> form7();
        };
    }

    // ---- FORM 1: Krave Awakening -------------------------------------------
    // Deliberately small. This form exists to teach his rhythm - wind up, strike,
    // recover - so everything later reads as a variation you already understand.

    private static List<KraveMove> form1() {
        return List.of(
                KraveMove.close("Krave Claw", 10, 30, 40, 5.0D, (level, boss, target) -> {
                    // Three swipes, not one hit: the combo is the tell that melee
                    // range is a commitment rather than a place to stand.
                    for (int i = 0; i < 3; i++) {
                        final int swipe = i;
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 5, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            Vec3 reach = boss.position()
                                    .add(boss.getViewVector(1.0F).scale(boss.getBbWidth() * 0.7D));
                            blast(level, boss, target, reach, 4.5D, 6.0F, 0.3D);
                            ring(level, boss, reach, 4.5D, ParticleTypes.CRIT, 12);
                            sound(level, boss, ModSounds.KRAVE_SCREECH.get(), 0.9F, 1.4F + swipe * 0.15F);
                        });
                    }
                }),

                KraveMove.ranged("Cereal Spit", 14, 40, 30, 6.0D, (level, boss, target) ->
                        volley(level, boss, target, 1, 0.0D)),

                KraveMove.ranged("Chocolate Bite", 16, 70, 20, 4.0D, (level, boss, target) -> {
                    // A lunge that heals if it connects, so whiffing it is a real
                    // window rather than a free reposition.
                    Vec3 at = target.position().subtract(boss.position()).normalize();
                    boss.setDeltaMovement(at.x * 1.2D, 0.35D, at.z * 1.2D);
                    boss.hurtMarked = true;
                    com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 8, () -> {
                        if (!boss.isAlive()) {
                            return;
                        }
                        var hit = victims(level, boss, boss.position(), 3.5D, target);
                        if (!hit.isEmpty()) {
                            blast(level, boss, target, boss.position(), 3.5D, 8.0F, 0.5D);
                            boss.heal(boss.getMaxHealth() * 0.02F);
                            sound(level, boss, ModSounds.KRAVE_LAUGH.get(), 1.2F, 0.8F);
                        }
                    });
                }),

                KraveMove.any("Bowl Slam", 20, 80, 25, (level, boss, target) -> {
                    shockwave(level, boss, target, boss.position(), 7.0D, 7.0F, 3);
                    sound(level, boss, ModSounds.KRAVE_BOOM.get(), 1.4F, 0.9F);
                })
        );
    }

    // ---- FORM 2: Chocolate-Filled ------------------------------------------
    // He cracks. Everything he does now leaves chocolate behind, so the arena
    // starts accumulating consequences instead of resetting between attacks.

    private static List<KraveMove> form2() {
        return join(form1(), List.of(
                KraveMove.close("Chocolate Burst", 12, 45, 35, 5.5D, (level, boss, target) -> {
                    Vec3 at = boss.position().add(boss.getViewVector(1.0F).scale(2.0D));
                    blast(level, boss, target, at, 5.0D, 7.0F, 0.6D);
                    ring(level, boss, at, 5.0D, ParticleTypes.FALLING_HONEY, 26);
                    puddle(level, boss, at, 3.0F, 8, milkSlow(3, 1));
                    sound(level, boss, ModSounds.KRAVE_BOOM.get(), 1.1F, 1.3F);
                }),

                KraveMove.ranged("Krave Scattershot", 18, 60, 35, 5.0D, (level, boss, target) ->
                        volley(level, boss, target, 7, 0.55D)),

                KraveMove.any("Chocolate Trail", 10, 90, 20, (level, boss, target) -> {
                    // Laid along the path between them, so it denies the approach
                    // rather than just decorating where he is standing.
                    Vec3 from = boss.position();
                    Vec3 to = target.position();
                    for (int i = 1; i <= 6; i++) {
                        Vec3 at = from.add(to.subtract(from).scale(i / 6.0D));
                        puddle(level, boss, at, 2.2F, 10, milkSlow(4, 1));
                    }
                    announce(level, boss, "He is leaking.");
                })
        ));
    }

    // ---- FORM 3: Double Chocolate ------------------------------------------
    // The core lights up. This is where he stops reacting and starts controlling
    // space - a beam that sweeps, mines that deny ground, and a speed window.

    private static List<KraveMove> form3() {
        return join(form2(), List.of(
                KraveMove.ranged("Krave Barrage", 20, 70, 40, 4.0D, (level, boss, target) -> {
                    // Tracking, not a single burst: fired over time so it follows
                    // you and strafing has to be committed to.
                    for (int i = 0; i < 8; i++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 4, () -> {
                            if (boss.isAlive() && target.isAlive()) {
                                volley(level, boss, target, 1, 0.12D);
                            }
                        });
                    }
                }),

                KraveMove.ranged("Chocolate Beam", 26, 110, 30, 5.0D, (level, boss, target) -> {
                    announce(level, boss, "The core opens.");
                    // Swept across the arena rather than aimed once, so it is
                    // dodged by moving through it, not by standing still.
                    for (int i = 0; i < 10; i++) {
                        final double t = i / 10.0D;
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 3, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            double sweep = (t - 0.5D) * 2.2D;
                            Vec3 aim = target.position()
                                    .add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                                    .add(Math.cos(sweep) * 4.0D, 0.0D, Math.sin(sweep) * 4.0D);
                            KraveAttacks.spit(level, boss, aim, 0.0D);
                        });
                    }
                    sound(level, boss, ModSounds.KRAVE_BEAM_FIRE.get(), 1.6F, 0.7F);
                }),

                KraveMove.any("Cereal Minefield", 22, 130, 25, (level, boss, target) -> {
                    announce(level, boss, "Watch the floor.");
                    for (int i = 0; i < 6; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double r = level.random.nextDouble() * 6.0D;
                        Vec3 at = target.position().add(Math.cos(a) * r, 0.0D, Math.sin(a) * r);
                        delayedEruption(level, boss, target, at, 2.6D, 9.0F, 35 + i * 4,
                                ParticleTypes.SMOKE, ParticleTypes.EXPLOSION);
                    }
                }),

                KraveMove.any("Sugar Rush", 16, 220, 15, (level, boss, target) -> {
                    boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                    boss.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 2));
                    announce(level, boss, "SUGAR RUSH.");
                    sound(level, boss, ModSounds.KRAVE_ROAR.get(), 1.8F, 1.4F);
                })
        ));
    }

    // ---- FORM 4: Krave Swarm ------------------------------------------------
    // He stops being one thing. The fight goes from a duel to crowd control.

    private static List<KraveMove> form4() {
        return join(form3(), List.of(
                KraveMove.any("Release the Krave", 24, 160, 40, (level, boss, target) -> {
                    announce(level, boss, "RELEASE THE KRAVE.");
                    summonMinions(level, boss, 5);
                    sound(level, boss, ModSounds.KRAVE_SPAWN.get(), 1.6F, 1.0F);
                }),

                KraveMove.any("Breakfast Storm", 20, 140, 30, (level, boss, target) -> {
                    // A rotating shell around HIM, so closing the distance is the
                    // dangerous part rather than staying out.
                    for (int t = 0; t < 40; t++) {
                        final int step = t;
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, t * 2, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            double a = step * 0.5D;
                            Vec3 at = boss.position().add(Math.cos(a) * 5.0D, 1.0D, Math.sin(a) * 5.0D);
                            ring(level, boss, at, 2.0D, ParticleTypes.CRIT, 6);
                            blast(level, boss, target, at, 2.0D, 4.0F, 0.4D);
                        });
                    }
                }),

                KraveMove.any("Cereal Portal", 26, 200, 25, (level, boss, target) -> {
                    announce(level, boss, "Bowls open around the arena.");
                    for (int p = 0; p < 4; p++) {
                        double a = (p / 4.0D) * Math.PI * 2.0D;
                        Vec3 portal = boss.position().add(Math.cos(a) * 9.0D, 2.0D, Math.sin(a) * 9.0D);
                        for (int shot = 0; shot < 6; shot++) {
                            com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, shot * 12, () -> {
                                if (!boss.isAlive() || !target.isAlive()) {
                                    return;
                                }
                                ring(level, boss, portal, 2.5D, ParticleTypes.PORTAL, 14);
                                blast(level, boss, target, portal, 2.5D, 3.5F, 0.2D);
                            });
                        }
                    }
                }),

                KraveMove.wounded("Devour", 18, 150, 35, 0.6F, (level, boss, target) -> {
                    int eaten = devourMinions(level, boss, boss.getMaxHealth() * 0.03F);
                    if (eaten > 0) {
                        boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                        announce(level, boss, "He ate his own. He looks better for it.");
                        sound(level, boss, ModSounds.KRAVE_LAUGH.get(), 1.5F, 0.7F);
                    }
                })
        ));
    }

    // ---- FORM 5: Milk & Chocolate Abomination -------------------------------
    // The arena itself becomes hostile. Every move here is about where you can
    // stand, not about what is pointed at you.

    private static List<KraveMove> form5() {
        return join(form4(), List.of(
                KraveMove.any("Milk Flood", 30, 190, 35, (level, boss, target) -> {
                    announce(level, boss, "MILK FLOOD.");
                    for (int i = 0; i < 10; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double r = 3.0D + level.random.nextDouble() * 9.0D;
                        Vec3 at = boss.position().add(Math.cos(a) * r, 0.0D, Math.sin(a) * r);
                        puddle(level, boss, at, 4.0F, 14, milkSlow(5, 2));
                    }
                    // The push is what makes it a flood rather than a carpet.
                    pullToward(level, boss, target, boss.position(), 14.0D, -0.35D);
                }),

                KraveMove.ranged("Soggy Krave", 24, 120, 30, 4.0D, (level, boss, target) -> {
                    Vec3 at = target.position();
                    delayedEruption(level, boss, target, at, 3.4D, 14.0F, 45,
                            ParticleTypes.FALLING_HONEY, ParticleTypes.EXPLOSION_EMITTER);
                    sound(level, boss, ModSounds.KRAVE_BOOM.get(), 1.2F, 0.7F);
                }),

                KraveMove.any("Milk Geysers", 28, 160, 30, (level, boss, target) -> {
                    announce(level, boss, "Move.");
                    for (int i = 0; i < 5; i++) {
                        final int index = i;
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 10, () -> {
                            if (!boss.isAlive() || !target.isAlive()) {
                                return;
                            }
                            // Telegraphed UNDER the player, then erupts - so it
                            // punishes standing still and nothing else.
                            delayedEruption(level, boss, target, target.position(), 3.0D, 11.0F, 25,
                                    ParticleTypes.SPLASH, ParticleTypes.CLOUD);
                        });
                    }
                }),

                KraveMove.any("Krave Whirlpool", 34, 240, 25, (level, boss, target) -> {
                    announce(level, boss, "THE BOWL TURNS.");
                    for (int t = 0; t < 50; t++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, t * 2, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            pullToward(level, boss, target, boss.position(), 16.0D, 0.14D);
                            ring(level, boss, boss.position(), 16.0D, ParticleTypes.FALLING_WATER, 22);
                        });
                    }
                })
        ));
    }

    // ---- FORM 6: Krave Overload ---------------------------------------------
    // Everything, faster, together. The first form that combines attacks rather
    // than picking one.

    private static List<KraveMove> form6() {
        return join(form5(), List.of(
                KraveMove.any("Chocolate Meteor", 30, 170, 35, (level, boss, target) -> {
                    announce(level, boss, "LOOK UP.");
                    for (int i = 0; i < 8; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double r = level.random.nextDouble() * 10.0D;
                        Vec3 at = target.position().add(Math.cos(a) * r, 0.0D, Math.sin(a) * r);
                        delayedEruption(level, boss, target, at, 3.2D, 13.0F, 30 + i * 6,
                                ParticleTypes.FLAME, ParticleTypes.EXPLOSION_EMITTER);
                    }
                }),

                KraveMove.ranged("Krave Gatling", 22, 130, 40, 3.0D, (level, boss, target) -> {
                    for (int i = 0; i < 30; i++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 2, () -> {
                            if (boss.isAlive() && target.isAlive()) {
                                volley(level, boss, target, 1, 0.22D);
                            }
                        });
                    }
                }),

                KraveMove.any("Breakfast Annihilation", 40, 320, 25, (level, boss, target) -> {
                    announce(level, boss, "BREAKFAST ANNIHILATION.");
                    // Three systems at once - this is the form's thesis.
                    for (int i = 0; i < 4; i++) {
                        delayedEruption(level, boss, target, target.position(), 3.0D, 10.0F, 20 + i * 12,
                                ParticleTypes.SPLASH, ParticleTypes.CLOUD);
                    }
                    summonMinions(level, boss, 4);
                    for (int i = 0; i < 10; i++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, i * 5, () -> {
                            if (boss.isAlive() && target.isAlive()) {
                                volley(level, boss, target, 2, 0.4D);
                            }
                        });
                    }
                }),

                KraveMove.any("Chocolate Core Detonation", 36, 260, 30, (level, boss, target) -> {
                    announce(level, boss, "The core is charging.");
                    for (int r = 1; r <= 5; r++) {
                        final double radius = r * 5.0D;
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 30 + r * 6, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            Vec3 from = boss.position();
                            ring(level, from, radius, ParticleTypes.SOUL_FIRE_FLAME, 30);
                            ring(level, from, radius * 0.93D, ParticleTypes.LARGE_SMOKE, 24);
                            pillars(level, from, radius, 26, 5.0D, ParticleTypes.FLAME);
                            KraveDemolition.carve(level, boss, from, radius, 6, 2,
                                    KraveDemolition.BUDGET);
                            // Only the expanding ring hurts, not the whole disc -
                            // so it is escaped by moving THROUGH it, which is the
                            // only reason an expanding ring is interesting.
                            for (var victim : victims(level, boss, boss.position(), radius + 1.5D, target)) {
                                if (victim.position().distanceTo(boss.position()) >= radius - 1.5D) {
                                    victim.hurt(victim.damageSources().mobAttack(boss), 12.0F);
                                }
                            }
                        });
                    }
                }),

                KraveMove.close("Insatiable Hunger", 30, 300, 25, 4.0D, (level, boss, target) -> {
                    announce(level, boss, "HE HAS YOU.");
                    // Held, then bitten - the gap is the escape window.
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 5));
                    com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 40, () -> {
                        if (!boss.isAlive() || !target.isAlive()) {
                            return;
                        }
                        if (target.position().distanceTo(boss.position()) < 5.0D) {
                            target.hurt(target.damageSources().mobAttack(boss), 22.0F);
                            boss.heal(boss.getMaxHealth() * 0.05F);
                            announce(level, boss, "He swallowed.");
                        } else {
                            announce(level, boss, "You got loose.");
                        }
                    });
                }),

                KraveMove.wounded("Sugar Rush EX", 20, 400, 45, 0.3F, (level, boss, target) -> {
                    boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 4));
                    boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 2));
                    announce(level, boss, "SUGAR RUSH EX.");
                    sound(level, boss, ModSounds.KRAVE_ROAR.get(), 2.0F, 1.6F);
                })
        ));
    }

    // ---- FORM 7: THE KRAVE GOD ----------------------------------------------
    // The arena is the boss now. Everything returns, larger, and two of these
    // permanently change the space you are fighting in.

    private static List<KraveMove> form7() {
        return join(form6(), List.of(
                KraveMove.any("Kravepocalypse", 40, 280, 40, (level, boss, target) -> {
                    announce(level, boss, "KRAVEPOCALYPSE.");
                    for (int i = 0; i < 24; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double r = level.random.nextDouble() * 16.0D;
                        Vec3 at = boss.position().add(Math.cos(a) * r, 0.0D, Math.sin(a) * r);
                        delayedEruption(level, boss, target, at, 2.8D, 10.0F, 10 + i * 3,
                                ParticleTypes.FLAME, ParticleTypes.EXPLOSION);
                    }
                }),

                KraveMove.any("Infinite Breakfast", 34, 340, 30, (level, boss, target) -> {
                    announce(level, boss, "INFINITE BREAKFAST.");
                    for (int wave = 0; wave < 5; wave++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, wave * 40, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            summonMinions(level, boss, 3);
                            double a = level.random.nextDouble() * Math.PI * 2.0D;
                            Vec3 portal = boss.position().add(Math.cos(a) * 8.0D, 2.0D, Math.sin(a) * 8.0D);
                            ring(level, portal, 1.6D, ParticleTypes.PORTAL, 18);
                            puddle(level, boss, portal, 3.0F, 10, milkSlow(4, 1));
                        });
                    }
                }),

                KraveMove.any("Chocolate Singularity", 44, 360, 30, (level, boss, target) -> {
                    announce(level, boss, "It is pulling everything in.");
                    Vec3 core = boss.position().add(0.0D, 6.0D, 0.0D);
                    for (int t = 0; t < 60; t++) {
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, t, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            ring(level, core, 2.0D, ParticleTypes.PORTAL, 12);
                            pullToward(level, boss, target, core, 20.0D, 0.09D);
                        });
                    }
                    com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 62, () -> {
                        if (!boss.isAlive()) {
                            return;
                        }
                        cataclysm(level, boss, target, core, 10.0D, 20.0F);
                        announce(level, boss, "It burst.");
                    });
                }),

                KraveMove.any("The Last Bowl", 46, 420, 25, (level, boss, target) -> {
                    announce(level, boss, "THE LAST BOWL.");
                    // Almost everything becomes milk. The gaps are the platforms.
                    for (int i = 0; i < 26; i++) {
                        double a = (i / 26.0D) * Math.PI * 2.0D;
                        for (double r = 4.0D; r <= 16.0D; r += 4.0D) {
                            Vec3 at = boss.position().add(Math.cos(a) * r, 0.0D, Math.sin(a) * r);
                            puddle(level, boss, at, 3.4F, 18, milkSlow(6, 2));
                        }
                    }
                }),

                KraveMove.any("DEVOUR EVERYTHING", 50, 500, 20, (level, boss, target) -> {
                    announce(level, boss, "HE IS EATING THE ARENA.");
                    // He genuinely removes ground. Kept to a shallow bite at the
                    // outer edge so it shrinks the arena without dropping anyone
                    // into the void the moment it fires - this is the Kosmos, and
                    // the floor is the only thing between you and a long fall.
                    Vec3 c = boss.position();
                    for (int i = 0; i < 40; i++) {
                        double a = level.random.nextDouble() * Math.PI * 2.0D;
                        double r = 12.0D + level.random.nextDouble() * 5.0D;
                        var pos = net.minecraft.core.BlockPos.containing(
                                c.x + Math.cos(a) * r, c.y - 1.0D, c.z + Math.sin(a) * r);
                        if (!level.getBlockState(pos).isAir()) {
                            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            level.sendParticles(ParticleTypes.SOUL,
                                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                                    2, 0.2D, 0.2D, 0.2D, 0.01D);
                        }
                    }
                    sound(level, boss, ModSounds.KRAVE_ROAR.get(), 2.4F, 0.4F);
                }),

                KraveMove.wounded("ONE LAST BITE", 60, 9999, 100, 0.05F, (level, boss, target) -> {
                    // Everything stops. This is the window, and it is the only
                    // attack in the fight that can simply end it.
                    announce(level, boss, "...");
                    com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 20, () ->
                            announce(level, boss, "THE MOUTH OPENS AROUND THE ARENA."));
                    for (int t = 0; t < 60; t++) {
                        final double closing = 26.0D - (t * 0.3D);
                        com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 20 + t, () -> {
                            if (!boss.isAlive()) {
                                return;
                            }
                            // The ring tightening IS the timer - it is the only
                            // thing telling you how long the window still is.
                            ring(level, boss.position(), closing, ParticleTypes.SOUL_FIRE_FLAME, 40);
                            pillars(level, boss.position(), closing, 32, 6.0D,
                                    ParticleTypes.SOUL_FIRE_FLAME);
                        });
                    }
                    com.barbarajones.behavior.DelayedEffects.scheduleWorld(level, 100, () -> {
                        if (!boss.isAlive()) {
                            return;   // you finished him inside the window
                        }
                        announce(level, boss, "HE SWALLOWS THE BATTLEFIELD.");
                        cataclysm(level, boss, target, boss.position(), 22.0D, 40.0F);
                        boss.heal(boss.getMaxHealth() * 0.25F);
                    });
                })
        ));
    }

    /** Previous form plus new moves, so each form inherits the whole book. */
    private static List<KraveMove> join(List<KraveMove> previous, List<KraveMove> added) {
        List<KraveMove> all = new java.util.ArrayList<>(previous);
        all.addAll(added);
        return java.util.List.copyOf(all);
    }
}
