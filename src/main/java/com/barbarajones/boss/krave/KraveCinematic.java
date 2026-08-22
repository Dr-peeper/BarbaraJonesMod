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
}
