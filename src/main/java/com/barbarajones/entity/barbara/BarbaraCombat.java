package com.barbarajones.entity.barbara;

import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.EmberCherry;
import com.barbarajones.entity.SmokeRing;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Barbara's whole fighting kit, kept out of the entity file.
 *
 * <p>One of these hangs off each {@link BarbaraJones} and is ticked server-side
 * every tick. It owns the cooldowns, the lingering smoke screens and the
 * decision of what she does next, and it is the only thing that spends her
 * stash on combat - so the bag the player keeps filling is literally her
 * ammunition. When the bag is empty nothing here fires and she is left with the
 * fists and the temper the base entity already gives her.
 *
 * <p>Her role next to Cayden is deliberate: she does almost no damage and an
 * enormous amount of crowd control. Half the kit exists to drag aggro off him.
 */
public class BarbaraCombat {

    /** More than this many clouds at once is a framerate problem, not a tactic. */
    private static final int MAX_CLOUDS = 3;
    /** Burnout is off the table below this much stash - it has to be a real sacrifice. */
    private static final int BURNOUT_FLOOR = 2000;
    private static final double AURA_RADIUS = 7.0D;
    private static final double AURA_RADIUS_HIGH = 10.0D;
    private static final double REACH = 18.0D;
    private static final int CHATTER_GAP = 140;
    /** How long after being hit she still counts it as worth answering. */
    private static final int BLOWBACK_WINDOW = 30;

    private final BarbaraJones barbara;
    private final int[] cooldowns = new int[SmokeAbility.values().length];
    private final List<SmokeScreenCloud> clouds = new ArrayList<>();

    private int auraTimer;
    private int chatterGap;
    private int burnoutTicks;
    private int blowbackWindow;
    /** Free-running counter used to stagger the abilities that scan for crowds. */
    private int pulse;

    @Nullable
    private LivingEntity blowbackVictim;

    public BarbaraCombat(BarbaraJones barbara) {
        this.barbara = barbara;
    }

    // ---- driving --------------------------------------------------------------

    public void tick() {
        if (!(this.barbara.level() instanceof ServerLevel level)) {
            return;
        }
        this.pulse++;
        for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
                this.cooldowns[i]--;
            }
        }
        if (this.chatterGap > 0) {
            this.chatterGap--;
        }

        tickClouds(level);
        if (this.burnoutTicks > 0) {
            tickBurnout(level);
        }
        tickSecondHand(level);

        if (this.blowbackWindow > 0) {
            this.blowbackWindow--;
            if (tryBlowback(level)) {
                return;
            }
        }

        LivingEntity target = this.barbara.getTarget();
        if (target == null || !target.isAlive() || !SmokeTargets.isFoe(this.barbara, target)) {
            return;
        }
        // Ordered worst-situation-first, so the expensive plays only come out
        // when the cheap ones would not have solved it. These four each sweep
        // the area for a crowd, so they are polled on a cadence rather than
        // every tick - four AABB queries per Barbara per tick adds up fast.
        if (this.pulse % 5 == 0 && (tryBurnout(level, target) || tryAshCloud(level)
                || trySmokeScreen(level, target) || tryContactHigh(level))) {
            return;
        }
        double dist = this.barbara.distanceTo(target);
        if (dist < 3.0D || dist > REACH || !this.barbara.hasLineOfSight(target)) {
            return;
        }
        if (!tryTheOs(level, target)) {
            tryLitCherry(level, target);
        }
    }

    /** Called from the entity's hurt() so Blowback can answer whoever swung. */
    public void onHurtBy(LivingEntity attacker) {
        if (attacker == this.barbara || SmokeTargets.isCrew(attacker)) {
            return;
        }
        this.blowbackVictim = attacker;
        this.blowbackWindow = BLOWBACK_WINDOW;
    }

    public boolean isBurningOut() {
        return this.burnoutTicks > 0;
    }

    public int cooldownOf(SmokeAbility ability) {
        return this.cooldowns[ability.ordinal()];
    }

    // ---- the abilities --------------------------------------------------------

    /**
     * The O's. Rings that keep going after the first thing they hit, which is
     * what makes them her answer to a line of mobs rather than a single one.
     */
    private boolean tryTheOs(ServerLevel level, LivingEntity target) {
        boolean high = this.barbara.isHigh();
        Vec3 from = mouth();
        Vec3 aim = target.getEyePosition().subtract(from);
        if (aim.lengthSqr() < 1.0E-4D || !spend(SmokeAbility.THE_OS)) {
            return false;
        }
        aim = aim.normalize();
        RandomSource rng = this.barbara.getRandom();
        // High she blows three at once and not one of them goes where she meant.
        int rings = high ? 3 : 1;
        double spread = high ? 0.14D : 0.02D;
        for (int i = 0; i < rings; i++) {
            Vec3 dir = aim.add(rng.nextGaussian() * spread, rng.nextGaussian() * spread * 0.5D,
                    rng.nextGaussian() * spread).normalize();
            level.addFreshEntity(new SmokeRing(level, this.barbara, from, dir, high));
        }
        sound(level, ModSounds.EVT_OG.get(), 1.0F, high ? 1.15F : 1.0F);
        exhale(level, aim, 6);
        return true;
    }

    /** A flicked cherry. Cheap, arcs, and whatever it lands on catches. */
    private boolean tryLitCherry(ServerLevel level, LivingEntity target) {
        boolean high = this.barbara.isHigh();
        Vec3 from = mouth();
        Vec3 aim = target.getEyePosition().subtract(from);
        if (aim.lengthSqr() < 1.0E-4D || !spend(SmokeAbility.LIT_CHERRY)) {
            return false;
        }
        RandomSource rng = this.barbara.getRandom();
        // lift the aim by a slice of the horizontal run so the arc lands on them
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        Vec3 lofted = new Vec3(aim.x, aim.y + horizontal * 0.20D, aim.z).normalize();
        int flicks = high ? 3 : 1;
        double spread = high ? 0.20D : 0.04D;
        for (int i = 0; i < flicks; i++) {
            Vec3 dir = lofted.add(rng.nextGaussian() * spread, rng.nextGaussian() * spread * 0.5D,
                    rng.nextGaussian() * spread).normalize();
            level.addFreshEntity(new EmberCherry(level, this.barbara, from, dir, high));
        }
        sound(level, SoundEvents.FLINTANDSTEEL_USE, 0.9F, 1.4F);
        return true;
    }

    /**
     * Smoke Screen. Expensive, so it only comes out for a crowd or for something
     * that has singled Cayden out - in which case the cloud lands on that thing
     * specifically and takes his name out of its head.
     */
    private boolean trySmokeScreen(ServerLevel level, LivingEntity target) {
        if (this.cooldowns[SmokeAbility.SMOKE_SCREEN.ordinal()] > 0) {
            return false;
        }
        List<LivingEntity> around = SmokeTargets.foesWithin(this.barbara, this.barbara.position(), 14.0D);
        LivingEntity focus = target;
        boolean huntingCayden = false;
        for (LivingEntity foe : around) {
            if (foe instanceof Mob mob && mob.getTarget() instanceof CaydenCobb) {
                focus = foe;
                huntingCayden = true;
                break;
            }
        }
        if (!huntingCayden && around.size() < 2) {
            return false;
        }
        if (!spend(SmokeAbility.SMOKE_SCREEN)) {
            return false;
        }
        boolean high = this.barbara.isHigh();
        if (this.clouds.size() >= MAX_CLOUDS) {
            this.clouds.remove(0);
        }
        this.clouds.add(new SmokeScreenCloud(focus.position(), high ? 6.0F : 4.5F,
                high ? 240 : 170, high));
        sound(level, SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.55F);
        exhale(level, focus.position().subtract(this.barbara.position()).normalize(), 12);
        return true;
    }

    /**
     * Ash Cloud. She taps the cherry off hard and the burning ash rings out of
     * her - the panic button for when she is being swarmed or worn down.
     */
    private boolean tryAshCloud(ServerLevel level) {
        if (this.cooldowns[SmokeAbility.ASH_CLOUD.ordinal()] > 0) {
            return false;
        }
        List<LivingEntity> near = SmokeTargets.foesWithin(this.barbara, this.barbara.position(), 5.0D);
        boolean swarmed = near.size() >= 3;
        boolean cornered = !near.isEmpty()
                && this.barbara.getHealth() < this.barbara.getMaxHealth() * 0.45F;
        if ((!swarmed && !cornered) || !spend(SmokeAbility.ASH_CLOUD)) {
            return false;
        }
        boolean high = this.barbara.isHigh();
        for (LivingEntity foe : near) {
            foe.hurt(level.damageSources().mobAttack(this.barbara), high ? 6.0F : 4.0F);
            foe.setSecondsOnFire(high ? 5 : 3);
            foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0, false, false));
            foe.knockback(high ? 1.4D : 1.0D,
                    this.barbara.getX() - foe.getX(), this.barbara.getZ() - foe.getZ());
            foe.hurtMarked = true;
        }
        double x = this.barbara.getX();
        double y = this.barbara.getY() + 0.4D;
        double z = this.barbara.getZ();
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0D;
            level.sendParticles(ParticleTypes.ASH, x + Math.cos(a) * 3.2D, y,
                    z + Math.sin(a) * 3.2D, 4, 0.35D, 0.35D, 0.35D, 0.02D);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.6D, z, 30, 1.6D, 0.5D, 1.6D, 0.06D);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 8, 0.8D, 0.2D, 0.8D, 0.0D);
        sound(level, SoundEvents.FIRE_EXTINGUISH, 1.6F, 0.7F);
        sound(level, SoundEvents.CAMPFIRE_CRACKLE, 1.5F, 0.6F);
        return true;
    }

    /**
     * Blowback. Whoever just hit her gets the lungful straight in the face, and
     * anything in that cone that was chasing Cayden is now chasing her instead.
     */
    private boolean tryBlowback(ServerLevel level) {
        LivingEntity who = this.blowbackVictim;
        if (who == null || !who.isAlive() || !SmokeTargets.isFoe(this.barbara, who)
                || this.barbara.distanceToSqr(who) > 20.0D) {
            return false;
        }
        if (!spend(SmokeAbility.BLOWBACK)) {
            return false;
        }
        this.blowbackVictim = null;
        this.blowbackWindow = 0;
        this.barbara.lookAt(who, 60.0F, 60.0F);

        boolean high = this.barbara.isHigh();
        Vec3 facing = who.position().subtract(this.barbara.position());
        facing = facing.lengthSqr() < 1.0E-4D ? this.barbara.getLookAngle() : facing.normalize();
        for (LivingEntity foe : SmokeTargets.foesWithin(this.barbara, this.barbara.position(), 4.0D)) {
            Vec3 delta = foe.position().subtract(this.barbara.position());
            if (delta.lengthSqr() > 1.0E-4D && delta.normalize().dot(facing) < 0.1D) {
                continue;                       // only what is genuinely in front of her
            }
            foe.hurt(level.damageSources().mobAttack(this.barbara), high ? 5.0F : 3.0F);
            foe.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            foe.knockback(high ? 1.8D : 1.3D,
                    this.barbara.getX() - foe.getX(), this.barbara.getZ() - foe.getZ());
            foe.hurtMarked = true;
            if (foe instanceof Mob mob && mob.getTarget() instanceof CaydenCobb) {
                mob.setTarget(this.barbara);
            }
        }
        exhale(level, facing, 20);
        sound(level, SoundEvents.FIRE_EXTINGUISH, 1.3F, 1.3F);
        return true;
    }

    /**
     * Contact High. She passes it round the crew: faster, tougher, healing - and
     * dizzy, because it is still second-hand smoke and it is still not free.
     */
    private boolean tryContactHigh(ServerLevel level) {
        if (this.cooldowns[SmokeAbility.CONTACT_HIGH.ordinal()] > 0) {
            return false;
        }
        List<LivingEntity> crew = SmokeTargets.crewWithin(this.barbara, 8.0D);
        boolean needed = false;
        for (LivingEntity mate : crew) {
            if (mate.getHealth() < mate.getMaxHealth() * 0.6F) {
                needed = true;
                break;
            }
        }
        if (!needed || !spend(SmokeAbility.CONTACT_HIGH)) {
            return false;
        }
        boolean high = this.barbara.isHigh();
        int dur = high ? 400 : 260;
        for (LivingEntity mate : crew) {
            mate.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0, false, true));
            mate.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur / 2, 0, false, true));
            mate.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, true));
            // High Barbara does not warn anybody how much they are taking.
            mate.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                    high ? 180 : 70, 0, false, false));
        }
        this.barbara.makeHigh(200);              // she is not passing it without taking one
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.barbara.getX(),
                this.barbara.getY() + 1.2D, this.barbara.getZ(), 16, 2.5D, 1.0D, 2.5D, 0.0D);
        sound(level, ModSounds.EVT_ROLL.get(), 1.1F, 1.0F);
        return true;
    }

    /**
     * Burnout. The whole bag in one pull, which leaves her at zero - and the
     * entity's existing rule is that zero stash means PSYCHO. That is the cost,
     * and it is meant to be felt.
     */
    private boolean tryBurnout(ServerLevel level, LivingEntity target) {
        if (this.cooldowns[SmokeAbility.BURNOUT.ordinal()] > 0 || this.burnoutTicks > 0
                || this.barbara.getStash() < BURNOUT_FLOOR) {
            return false;
        }
        boolean desperate = this.barbara.getHealth() < this.barbara.getMaxHealth() * 0.35F;
        boolean bigFight = target.getMaxHealth() >= 80.0F
                || SmokeTargets.foesWithin(this.barbara, this.barbara.position(), 12.0D).size() >= 4;
        if (!desperate && !bigFight) {
            return false;
        }
        this.cooldowns[SmokeAbility.BURNOUT.ordinal()] = SmokeAbility.BURNOUT.cooldown(false);
        this.barbara.spendStash(this.barbara.getStash());
        this.barbara.makeHigh(160);
        this.burnoutTicks = 140;

        this.barbara.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, true));
        this.barbara.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true));
        this.barbara.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true));
        this.barbara.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false));
        this.barbara.swing(InteractionHand.MAIN_HAND);

        this.clouds.clear();
        this.clouds.add(new SmokeScreenCloud(this.barbara.position(), 8.0F, 140, true));
        BarbaraLines.speak(this.barbara,
                BarbaraLines.forAbility(SmokeAbility.BURNOUT, this.barbara.getRandom()));
        this.chatterGap = CHATTER_GAP;
        sound(level, ModSounds.BARBARA_RAGE.get(), 1.6F, 0.85F);
        sound(level, SoundEvents.FIRE_EXTINGUISH, 1.8F, 0.5F);
        return true;
    }

    // ---- passives and upkeep --------------------------------------------------

    /**
     * Second-Hand. Standing near her is bad for you: it costs her a trickle of
     * stash per second and only while something hostile is actually in it.
     */
    private void tickSecondHand(ServerLevel level) {
        if (++this.auraTimer < 20) {
            return;
        }
        this.auraTimer = 0;
        boolean high = this.barbara.isHigh();
        double radius = high ? AURA_RADIUS_HIGH : AURA_RADIUS;
        List<LivingEntity> foes = SmokeTargets.foesWithin(this.barbara, this.barbara.position(), radius);
        if (foes.isEmpty() || !this.barbara.spendStash(SmokeAbility.SECOND_HAND.cost(high))) {
            return;
        }
        int amp = high ? 1 : 0;
        for (LivingEntity foe : foes) {
            foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, amp, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amp, false, false));
        }
        level.sendParticles(ParticleTypes.SMOKE, this.barbara.getX(), this.barbara.getY() + 0.5D,
                this.barbara.getZ(), 8, radius * 0.35D, 0.35D, radius * 0.35D, 0.005D);
    }

    private void tickClouds(ServerLevel level) {
        Iterator<SmokeScreenCloud> it = this.clouds.iterator();
        while (it.hasNext()) {
            SmokeScreenCloud cloud = it.next();
            cloud.tick(level, this.barbara);
            if (cloud.isDone()) {
                it.remove();
            }
        }
    }

    private void tickBurnout(ServerLevel level) {
        this.burnoutTicks--;
        double x = this.barbara.getX();
        double y = this.barbara.getY();
        double z = this.barbara.getZ();
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.3D, z,
                6, 0.3D, 1.2D, 0.3D, 0.07D);
        if (this.burnoutTicks % 20 != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 2.6D, z, 30, 1.3D, 2.0D, 1.3D, 0.05D);
        for (LivingEntity foe : SmokeTargets.foesWithin(this.barbara, this.barbara.position(), 12.0D)) {
            foe.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, false));
            foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, false));
            foe.hurt(level.damageSources().mobAttack(this.barbara), 2.0F);
        }
    }

    // ---- plumbing -------------------------------------------------------------

    private boolean spend(SmokeAbility ability) {
        boolean high = this.barbara.isHigh();
        if (this.cooldowns[ability.ordinal()] > 0
                || !this.barbara.spendStash(ability.cost(high))) {
            return false;
        }
        this.cooldowns[ability.ordinal()] = ability.cooldown(high);
        this.barbara.swing(InteractionHand.MAIN_HAND);
        say(ability);
        return true;
    }

    private void say(SmokeAbility ability) {
        if (this.chatterGap > 0 || this.barbara.getRandom().nextInt(3) != 0) {
            return;
        }
        this.chatterGap = CHATTER_GAP;
        BarbaraLines.speak(this.barbara, BarbaraLines.forAbility(ability, this.barbara.getRandom()));
    }

    /** Where the smoke actually leaves her - the tip of whatever is lit. */
    private Vec3 mouth() {
        Vec3 look = this.barbara.getLookAngle();
        return this.barbara.getEyePosition().add(look.x * 0.45D, -0.05D, look.z * 0.45D);
    }

    private void exhale(ServerLevel level, Vec3 dir, int count) {
        Vec3 m = mouth();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, m.x + dir.x * 0.4D, m.y + dir.y * 0.4D,
                m.z + dir.z * 0.4D, count, 0.14D, 0.14D, 0.14D, 0.04D);
    }

    private void sound(ServerLevel level, SoundEvent event, float volume, float pitch) {
        level.playSound(null, this.barbara.getX(), this.barbara.getY(), this.barbara.getZ(),
                event, this.barbara.getSoundSource(), volume, pitch);
    }

    // ---- persistence ----------------------------------------------------------

    public void save(CompoundTag tag) {
        tag.putIntArray("SmokeCooldowns", this.cooldowns.clone());
        tag.putInt("SmokeBurnout", this.burnoutTicks);
    }

    public void load(CompoundTag tag) {
        int[] saved = tag.getIntArray("SmokeCooldowns");
        for (int i = 0; i < this.cooldowns.length && i < saved.length; i++) {
            this.cooldowns[i] = saved[i];
        }
        this.burnoutTicks = tag.getInt("SmokeBurnout");
    }
}
