package com.barbarajones.v2.mobs.entity;

import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.ai.KravajoDiveGoal;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * THE KRAVAJO: a cereal-flake bat that lives in the sky and dives at your head.
 *
 * <p>It is not a threat and is not supposed to become one. The design brief was
 * "annoying fleas", so everything here is tuned for irritation rather than
 * danger:
 *
 * <ul>
 *   <li><b>Four out of five dives do nothing at all.</b> They connect, they
 *       knock you about, they make a noise - and they deal zero damage. Only the
 *       fifth takes half a heart. The near-misses are the joke; a mob that
 *       reliably chips you is a threat you have to answer, whereas one that
 *       mostly just BOTHERS you is something you swat at while doing something
 *       else. That distinction is the entire mob.</li>
 *   <li><b>One hit from a bare fist kills it.</b> Two health, no armour. You
 *       should never have to stop what you are doing and equip something.</li>
 *   <li><b>It flies, so it cannot be ignored the way a ground pest can.</b> It
 *       comes from above, retreats upward after a pass, and comes back.</li>
 * </ul>
 *
 * <p>The knockback on a zero-damage hit is deliberate and is applied directly:
 * a hit that deals no damage also delivers no knockback through the normal path,
 * and a dive that passes straight through you reads as a bug rather than a miss.
 */
public class KravajoEntity extends PathfinderMob implements Enemy, KravelingKin {

    /** One dive in this many actually hurts. The rest are pure nuisance. */
    public static final int PAINFUL_DIVE_ODDS = 5;

    /** Half a heart, on the one dive in five that lands properly. */
    public static final float BITE_DAMAGE = 1.0F;

    public KravajoEntity(EntityType<? extends KravajoEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 12, true);
        // It should not path around water like a walker; it is above it.
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // createMobAttributes, never createLivingAttributes - the latter omits
        // FOLLOW_RANGE, which the navigation reads in the constructor.
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)          // one punch
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FLYING_SPEED, 0.62D)       // faster than you, on purpose
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, BITE_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KravajoDiveGoal(this));
        this.goalSelector.addGoal(4, new WaitingAloftGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;   // it flies; falling is not a thing that happens to it
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
                                   net.minecraft.core.BlockPos pos) {
        // no-op, same reason
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;   // it is a flea in spirit; let Bane of Arthropods work
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean painful = this.random.nextInt(PAINFUL_DIVE_ODDS) == 0;
        if (!painful) {
            // A whiff. It still connects visibly - noise, particles, a shove -
            // it simply does not cost you anything. Knockback is applied by hand
            // because a zero-damage hit delivers none through the usual path,
            // and a dive that passes straight through reads as a bug, not a miss.
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                double dx = living.getX() - getX();
                double dz = living.getZ() - getZ();
                living.knockback(0.22D, -dx, -dz);
                living.hurtMarked = true;
            }
            playSound(ModMobSounds.KRAVAJO_MISS.get(), 0.7F, 1.4F + this.random.nextFloat() * 0.3F);
            if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(),
                        target.getY() + target.getBbHeight() * 0.8D, target.getZ(),
                        3, 0.2D, 0.2D, 0.2D, 0.0D);
            }
            peelAway();
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            peelAway();
        }
        return hit;
    }

    /** After a pass it climbs away, so the next dive reads as a new one. */
    private void peelAway() {
        setDeltaMovement(getDeltaMovement().add(
                (this.random.nextDouble() - 0.5D) * 0.3D, 0.42D, (this.random.nextDouble() - 0.5D) * 0.3D));
        this.hurtMarked = true;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data,
            net.minecraft.nbt.CompoundTag tag) {
        // They arrive already airborne rather than standing on the ground looking
        // confused, which is what a ground-spawned flyer does for its first second.
        setDeltaMovement(getDeltaMovement().add(0.0D, 0.25D, 0.0D));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMobSounds.KRAVAJO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMobSounds.KRAVAJO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMobSounds.KRAVAJO_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;   // there will be a lot of them; keep each one quiet
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    /**
     * Hangs above its target between dives instead of orbiting at head height.
     * Without this they cluster around your face and the dive stops reading as a
     * dive, because there is nowhere for it to come DOWN from.
     */
    private static final class WaitingAloftGoal extends net.minecraft.world.entity.ai.goal.Goal {

        private final KravajoEntity mob;

        WaitingAloftGoal(KravajoEntity mob) {
            this.mob = mob;
            setFlags(java.util.EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
        }

        @Override
        public void tick() {
            var target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            double perchY = target.getY() + 4.5D;
            this.mob.getMoveControl().setWantedPosition(target.getX(), perchY, target.getZ(), 1.0D);
        }
    }
}
