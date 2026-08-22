package com.barbarajones.boss.krave;

import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * One running finisher attack, and the vocabulary the six of them are written
 * in.
 *
 * <p>The moves differ in choreography, not in machinery. All of them move
 * somebody somewhere, hold the boss still, hit the ground hard and shake the
 * arena; writing each as its own self-contained routine would be six copies of
 * that with six chances to forget the safety rules. So the shared verbs live
 * here - carry, launch, slam, shake, stagger - and each move is a short script
 * that calls them on a timeline.
 *
 * <p>{@link #t} is ticks since the move began. Scripts branch on it, which
 * makes them read as a storyboard: at tick 0 do this, at tick 20 do that.
 */
public final class KraveCinematic {

    /**
     * What a missed prompt costs, as a share of the player's health bar.
     *
     * <p>A share rather than a count of hearts. A flat figure quietly means
     * different things to different players - health boost, absorption, or
     * anything else that moves MAX_HEALTH - so one constant would be a scratch
     * to one of them and most of the bar to the next. A quarter is a quarter
     * for everybody; and being a fraction below one it cannot empty a full bar,
     * however unarmoured the player standing there happens to be.
     */
    private static final float FAIL_SHARE = 0.25F;

    /** How hard a miss throws the player. Around ten blocks, on flat ground. */
    private static final double FAIL_KNOCKBACK = 1.2D;

    /**
     * Lift added on top of vanilla's, which caps itself at 0.4 and only grants
     * it to somebody already standing on the ground. Deliberately small:
     * launched any higher the landing becomes a second hit that
     * {@link #FAIL_SHARE} never accounted for.
     */
    private static final double FAIL_LIFT = 0.55D;

    /** Radius of the failure dome, in blocks. */
    private static final double FAIL_RADIUS = 7.0D;

    public final ServerLevel level;
    public final KraveMonster boss;
    public final CaydenCobb cayden;
    public final ServerPlayer player;

    /** Ticks since this move started. */
    public int t;

    /**
     * The ground under the boss when the move began.
     *
     * <p>The GROUND, not his position. Every sky move aims at origin plus some
     * height, and a retry builds a fresh cinematic from wherever the failed
     * attempt left him - so anchoring to his current Y meant each attempt
     * started higher than the last and added its offset again. Twenty-six
     * blocks a go, looping every few seconds, put him a thousand blocks above a
     * world that is two hundred and fifty-six tall; and once he was above it no
     * launch point could be found, so the loop had no way to end.
     *
     * <p>A retry now aims at exactly the same place as the first attempt.
     */
    public final Vec3 origin;

    /** Nothing in a cinematic may fly higher than this. */
    public final double ceiling;

    public KraveCinematic(ServerLevel level, KraveMonster boss, CaydenCobb cayden, ServerPlayer player) {
        this.level = level;
        this.boss = boss;
        this.cayden = cayden;
        this.player = player;
        // Snapped to the surface beneath him. getHeight returns the first free
        // space above the ground, which is where a slam should land.
        int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                net.minecraft.util.Mth.floor(boss.getX()), net.minecraft.util.Mth.floor(boss.getZ()));
        // Never above where he actually is: if he is mid-air over a hole, the
        // heightmap answer could be far below and the move would aim into it.
        this.origin = new Vec3(boss.getX(), Math.min(groundY, boss.getY()), boss.getZ());
        // A hard lid, well under the build limit. Belt and braces against any
        // future move that adds height without thinking about where it ends up.
        this.ceiling = Math.min(this.origin.y + 70.0D, level.getMaxBuildHeight() - 10.0D);
    }

    // ---- moving people ------------------------------------------------------

    /**
     * Flies an entity toward a point under its own power.
     *
     * <p>Steered rather than set, so it reads as travel. The one exception is
     * the grab hand-off, which has to be exact.
     */
    public void fly(net.minecraft.world.entity.Entity who, Vec3 target, double speed) {
        who.setNoGravity(true);
        who.fallDistance = 0.0F;
        // Clamped here rather than at each call site: one lid that every script
        // passes through cannot be forgotten by the seventh move somebody adds.
        if (target.y > this.ceiling) {
            target = new Vec3(target.x, this.ceiling, target.z);
        }
        Vec3 to = target.subtract(who.position());
        double d = to.length();
        if (d < 0.25D) {
            who.setDeltaMovement(who.getDeltaMovement().scale(0.4D));
            return;
        }
        who.setDeltaMovement(who.getDeltaMovement().scale(0.5D).add(to.scale(speed / d)));
        who.hurtMarked = true;
    }

    /** Points somebody at the boss, so the camera is looking at the thing happening. */
    public void faceBoss(net.minecraft.world.entity.Entity who) {
        Vec3 to = this.boss.position().add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D)
                .subtract(who.position());
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        who.setYRot((float) (Math.toDegrees(Math.atan2(-to.x, to.z))));
        who.setXRot((float) (-Math.toDegrees(Math.atan2(to.y, flat))));
        // Pushed to the client only every few ticks, not every one. A forced
        // rotation sync is a full position packet, and sending one twenty times
        // a second fights the mouse the whole way through a cinematic - the
        // camera judders and the player cannot follow what they are looking at,
        // which defeats the point of aiming it for them.
        if (who instanceof ServerPlayer sp && sp.tickCount % 5 == 0) {
            sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), sp.getYRot(), sp.getXRot());
        }
    }

    /** Somewhere in clear air above the boss, for the launches. */
    public Vec3 skyAbove(double height) {
        Vec3 p = KraveFinisher.launchPoint(this.level, this.boss);
        if (p != null) {
            return p;
        }
        // No searched point available - straight up is still better than into a
        // roof, and the caller only needs somewhere to aim.
        return this.boss.position().add(0.0D, this.boss.getBbHeight() + height, 0.0D);
    }

    // ---- hitting things -----------------------------------------------------

    /**
     * The boss driven into the ground: crater, shockwave, sound, hit-stop.
     *
     * @param power 1 for the first finisher, rising with the step - every
     *              number that controls how big this looks is derived from it,
     *              so a later move cannot accidentally land smaller than an
     *              earlier one
     */
    public void slam(Vec3 at, int power) {
        double radius = 6.0D + power * 2.0D;
        this.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                at.x, at.y + 1.0D, at.z, 2 + power, 2.0D, 1.0D, 2.0D, 0.0D);
        this.level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1.0D, at.z, 2, 1.0D, 1.0D, 1.0D, 0.0D);
        KraveAttacks.dome(this.level, at, radius, ParticleTypes.SOUL_FIRE_FLAME);
        KraveAttacks.ring(this.level, at, radius, ParticleTypes.LARGE_SMOKE, 40);
        KraveAttacks.shockwave(this.level, this.boss, null, at, radius * 0.6D, 0.0F, 4 + power);
        KraveDemolition.crater(this.level, this.boss, at, radius * 0.7D, 3 + power);
        this.level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                ModSounds.KRAVE_BOOM.get(), SoundSource.HOSTILE,
                (float) Math.min(2.0D, 1.2D + power * 0.15D), 0.45F);
        shake(power);
        // Hit-stop. He stops dead for a beat instead of sliding away from the
        // blow that just landed.
        this.boss.setDeltaMovement(Vec3.ZERO);
        this.boss.hurtTime = 20;
        this.boss.hurtMarked = true;
    }

    /**
     * Screen shake, done with what 1.20.1 actually gives a server.
     *
     * <p>There is no shake API, so this is the trick vanilla itself uses for
     * the warden's sonic boom feeling: a burst of camera-local particles and a
     * low, loud sound. Honest about what it is - the alternative was a client
     * mixin for one effect.
     */
    public void shake(int power) {
        for (Player p : this.level.getEntitiesOfClass(Player.class,
                this.boss.getBoundingBox().inflate(48.0D))) {
            this.level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER, Math.min(1.0F, 0.35F + power * 0.1F), 0.5F);
            this.level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    p.getX(), p.getY() + 1.0D, p.getZ(), 6 + power * 2, 1.5D, 1.0D, 1.5D, 0.02D);
        }
    }

    /** A trail behind something travelling fast, so the direction is readable. */
    public void trail(net.minecraft.world.entity.Entity who, ParticleOptions type, int count) {
        this.level.sendParticles(type, who.getX(), who.getY() + who.getBbHeight() * 0.5D, who.getZ(),
                count, 0.4D, 0.4D, 0.4D, 0.04D);
    }

    /**
     * He gets back up, and it costs him more each time.
     *
     * <p>The whole point of a multi-step finisher is that the previous hit
     * plainly hurt and plainly was not enough, so this escalates with the step.
     */
    public void stagger(int power) {
        this.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                this.boss.getX(), this.boss.getY() + 1.0D, this.boss.getZ(),
                40 + power * 20, 2.0D, 1.5D, 2.0D, 0.02D);
        this.level.playSound(null, this.boss.blockPosition(),
                power >= 4 ? ModSounds.MONSTER_ROAR_2.get() : ModSounds.KRAVE_ROAR.get(),
                SoundSource.HOSTILE, 2.0F, Math.max(0.4F, 0.9F - power * 0.08F));
    }

    public void say(String text, ChatFormatting colour) {
        for (Player p : this.level.getEntitiesOfClass(Player.class,
                this.boss.getBoundingBox().inflate(96.0D))) {
            p.sendSystemMessage(Component.literal(colour + "" + ChatFormatting.BOLD + text));
        }
    }

    /** Nobody in a cinematic is killed by it. */
    public void protect(int ticks) {
        this.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, ticks, 5, false, false));
        this.player.fallDistance = 0.0F;
    }

    // ---- failing ------------------------------------------------------------

    /**
     * The price of missing a finisher prompt.
     *
     * <p>Missing used to cost a line of grey text and a three second wait,
     * which is not a consequence - it is a pause, and a pause is what the
     * player gets for succeeding too. This is the other half of the bargain: he
     * breaks the hold, goes off in their face, and they spend the retry
     * half-blind and staggering.
     *
     * <p>Survivable by design. The same step is offered again a moment later,
     * so a punishment that killed - or that reliably left somebody on one heart
     * with six forms still ahead of them - would turn one fumbled key into the
     * end of the encounter.
     *
     * <p>Nothing here touches a block. There is no {@code Level.explode} and no
     * {@link KraveDemolition} call, because the arena floor and the Kraved
     * Castle are protected from the boss' own moves elsewhere, and a failure
     * blast that dug would hand the player a way through that protection by
     * losing on purpose.
     *
     * @param victim the player who failed, or null if they have left since the
     *               prompt went up - in which case the blast still goes off,
     *               with nobody in it. Taken as an argument rather than read
     *               off {@link #player} because a cinematic can legitimately
     *               carry a null player (the recovery stagger builds one that
     *               way), and one authority for who is being hit cannot
     *               disagree with itself.
     */
    public void punish(ServerPlayer victim) {
        // Centred on the player where there is one: the point of this is that
        // the thing they failed to stop went off where they were standing, not
        // somewhere across the arena.
        Vec3 at = victim != null
                ? victim.position()
                : this.boss.position().add(0.0D, this.boss.getBbHeight() * 0.3D, 0.0D);

        this.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                at.x, at.y + 1.0D, at.z, 4, 1.5D, 1.0D, 1.5D, 0.0D);
        this.level.sendParticles(ParticleTypes.FLASH,
                at.x, at.y + 1.5D, at.z, 3, 1.0D, 1.0D, 1.0D, 0.0D);
        KraveAttacks.dome(this.level, at, FAIL_RADIUS, ParticleTypes.SOUL_FIRE_FLAME);
        KraveAttacks.ring(this.level, at, FAIL_RADIUS * 1.3D, ParticleTypes.LARGE_SMOKE, 48);
        shake(3);

        this.level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                ModSounds.KRAVE_BOOM.get(), SoundSource.HOSTILE, 2.0F, 0.4F);
        // And he enjoys it. The boom says a move landed; the laugh says whose
        // fault it was, and that is the half meant to sting.
        this.level.playSound(null, this.boss.blockPosition(),
                ModSounds.KRAVE_LAUGH.get(), SoundSource.HOSTILE, 2.0F, 0.8F);

        if (victim == null) {
            return;
        }

        // Cleared first, or the hit is simply swallowed any time a minion got a
        // scratch in during the last half second. A punishment that silently
        // does nothing on a crowded arena is worse than no punishment at all,
        // because it reads as the mechanic being broken rather than merciful.
        victim.invulnerableTime = 0;
        victim.hurt(victim.damageSources().mobAttack(this.boss),
                victim.getMaxHealth() * FAIL_SHARE);

        // Away from him and off their feet. The direction comes from the boss,
        // so it reads as being swatted by the thing they failed to finish.
        Vec3 push = victim.position().subtract(this.boss.position());
        double flat = Math.sqrt(push.x * push.x + push.z * push.z);
        if (flat < 0.1D) {
            // Standing inside his hitbox, which is exactly where a finisher
            // tends to leave somebody. "Away from the boss" is then a zero
            // vector, and vanilla knockback handed one does nothing whatsoever,
            // so fall back to the way they are facing. Any consistent direction
            // beats a shove that is quietly not there.
            float yaw = victim.getYRot() * ((float) Math.PI / 180.0F);
            push = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
            flat = 1.0D;
        }
        // knockback() travels opposite the vector it is handed, hence the sign
        // - the same convention KraveAttacks.blastExact works to.
        victim.knockback(FAIL_KNOCKBACK, -push.x / flat, -push.z / flat);
        victim.setDeltaMovement(victim.getDeltaMovement().add(0.0D, FAIL_LIFT, 0.0D));
        victim.fallDistance = 0.0F;
        victim.hurtMarked = true;
        // Being thrown off the edge of a floating island is already covered -
        // KraveKosmosBattle.catchTheFallen puts anyone who drops below the boss
        // back into the arena for the length of the fight - so this is free to
        // be a real shove rather than a polite nudge.

        // Two to ten seconds apiece, and no longer. The step is offered again
        // after the controller's retry delay, so anything that outlasted the
        // fight by much would compound across misses into a player who cannot
        // answer the prompt at all, and a punishment nobody can climb out of is
        // only a slower death.
        debuff(victim, MobEffects.MOVEMENT_SLOWDOWN, 100, 1);
        debuff(victim, MobEffects.CONFUSION, 140, 0);
        debuff(victim, MobEffects.WEAKNESS, 120, 0);
        debuff(victim, MobEffects.DIG_SLOWDOWN, 120, 1);
        // The shortest of the lot, and it has to be. The retry prompt is a
        // letter drawn on the screen: a blindness that outlived the retry delay
        // would make the next window unanswerable rather than hard, which is
        // how a punishment quietly becomes a soft lock.
        debuff(victim, MobEffects.BLINDNESS, 45, 0);
        // Flavour more than teeth - ten seconds of Hunger II is about two
        // exhaustion points. Draining the food bar for real would push a player
        // toward starving in the middle of a seven form fight, which is a
        // different and far nastier punishment than the one intended here.
        debuff(victim, MobEffects.HUNGER, 200, 1);
    }

    /**
     * One debuff, with the swirls left visible.
     *
     * <p>Hiding the particles would be the wrong call: somebody who has just
     * been thrown across the arena needs to be able to see that the reason
     * everything is suddenly sluggish is the miss, and not the boss cheating.
     */
    private static void debuff(ServerPlayer who, MobEffect what, int ticks, int amplifier) {
        who.addEffect(new MobEffectInstance(what, ticks, amplifier));
    }
}
