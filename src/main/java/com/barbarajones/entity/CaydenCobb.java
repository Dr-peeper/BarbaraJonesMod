package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModSounds;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.housing.HousingResult;
import com.barbarajones.housing.HousingValidator;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cayden Cobb - your Krave-addicted companion. He fights every hostile mob on
 * sight (and they hunt him back), gets stronger and fatter the more Krave you
 * feed him, and - Terraria-style - he will only settle in a room that actually
 * meets the housing requirements.
 *
 * Claim a home by right-clicking him while standing in the room you want him
 * to live in. If the room later stops qualifying (you break the door, steal the
 * light, tear the roof off) he complains and moves out.
 */
public class CaydenCobb extends TamableAnimal {

    private static final EntityDataAccessor<Integer> FED =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RAGE =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOUSED =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SSJ =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    /** Half-ascension: cornered and fighting for his life. Synced for the aura. */
    private static final EntityDataAccessor<Boolean> DESPERATE =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    /** Dark Cayden: what his mother brings out of him. Synced for the aura. */
    private static final EntityDataAccessor<Boolean> DARK =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    /**
     * How far up the ladder he currently is. See {@link AscensionLadder}: 0 base,
     * 1 Super Saiyan, 2 SSJ2, 3 SSJ3, 4 Super Saiyan God, 5 Super Saiyan Blue,
     * 6 Ultra Instinct. Synced so the aura can render the right form.
     */
    private static final EntityDataAccessor<Integer> TIER =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.INT);
    /**
     * Bitmask of the forms he has actually been taught, bit N == rung N.
     * Synced because the upgrade screen is drawn entirely from his entity data -
     * there is no server-to-client packet behind that GUI at all.
     */
    private static final EntityDataAccessor<Integer> UNLOCKS =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.INT);
    /** Spendable ki. Earned by feeding him and by things dying in front of him. */
    private static final EntityDataAccessor<Integer> KI =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.INT);

    public static final int RAGE_THRESHOLD = 25;
    /** How long a transformation lasts before powering down on its own. */
    private static final int SSJ_DURATION_TICKS = 6000;
    /** Below this fraction of max health he half-ascends to save himself. */
    private static final float DESPERATE_AT = 0.40F;
    /** He must climb back above this to calm down - hysteresis stops it flickering. */
    private static final float DESPERATE_OFF = 0.70F;
    /** Odds of a raw power flash on any given hit: 1 in this. */
    private static final int FLASH_ODDS = 10;
    /** Odds of a meteor answering a desperate punch: 1 in this. */
    private static final int METEOR_ODDS = 3;
    /** Radius the dark aura rots plants within. */
    private static final int CORRUPT_RADIUS = 6;
    /** Ticks between laser volleys while dark. */
    private static final int LASER_INTERVAL = 30;
    /** Ticks between flight bursts while dark. */
    private static final int BURST_INTERVAL = 55;
    /** Ticks between SSJ2 shockwaves. */
    private static final int WAVE_INTERVAL = 45;
    /** Ticks between techniques at tier 1; higher tiers cut this down. */
    private static final int ARSENAL_INTERVAL = 90;
    /** How far he will look for something worth transforming for. */
    private static final double BOSS_SCAN_RANGE = 160.0D;
    /** Heal a point this often, once out of combat. */
    private static final int REGEN_INTERVAL = 60;
    /** How long since being hit before he starts healing again. */
    private static final int OUT_OF_COMBAT_TICKS = 100;
    private static final double BASE_SPEED = 0.5D;
    private static final double BASE_DAMAGE = 3.0D;
    private static final double BASE_HEALTH = 24.0D;
    /** Ticks between "he cannot reach that form yet" reminders. */
    private static final int LOCKED_NAG_INTERVAL = 200;
    /** How far he'll wander from a claimed home before heading back. */
    private static final double HOME_LEASH = 22.0D;

    @Nullable
    /** 30s of post-respawn immunity, in ticks. */
    public static final int GRACE_TICKS = 600;

    private BlockPos home;
    /** Dimension the home was claimed in - a house in the Overworld is not a house in the Kosmos. */
    private String homeDim = "";
    private int homeCheckTimer = 100;
    private int graceTicks;
    /** Client-only: ticks since the ascension became visible, or -1. */
    private int ssjClientAge = -1;
    private int laserTimer;
    private int burstTimer;
    private int flightTicks;
    private int waveTimer;
    private int dodgeFlash;
    private int arsenalTimer = 60;
    private int lastMove = -1;
    private int tierIdle;
    private int ssjTicks;
    /** While true the transformation has no timer: it ends when the boss does. */
    private boolean ssjUntilBossDies;
    /**
     * The foe his ki is currently riding on, and what rung it demanded. Held so
     * the reward is paid when it dies even if he has already turned to swing at
     * something else - and even if the player landed the killing blow.
     */
    @Nullable
    private LivingEntity creditFoe;
    private int creditDemand;
    private int lockedNag;

    public CaydenCobb(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    // ---- being thrown by Barbara ---------------------------------------------

    /** Ticks left in a Barbara throw. Server-side timer only; noAi is what actually yields his AI. */
    private int throwTicks = 0;

    /**
     * Barbara picks him up and launches him at a target. Disabling AI (not
     * killing his goals outright) is what makes this work cleanly: with
     * {@code isNoAi()} true, {@link net.minecraft.world.entity.Mob}'s own
     * tick skips the goal selector entirely, so nothing fights the arc's
     * velocity, but gravity and collision (and this class's own tick(),
     * which isn't gated by noAi) still run normally - he really flies.
     */
    public void launchFrom(Vec3 origin, Vec3 targetPos) {
        if (level().isClientSide) {
            return;
        }
        Vec3 to = targetPos.subtract(origin);
        double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
        // Fixed launch power, not scaled to how far the target actually is -
        // this needs to read as "she just yeeted him twenty blocks," not a
        // gentle toss that happens to stop near the enemy. 1.6 blocks/tick
        // horizontal with a 1.15 lift clears roughly a 20-block arc before
        // drag and gravity bring him down.
        double speed = 1.6D;
        double lift = 1.15D;
        double dirX = horiz > 0.01D ? to.x / horiz : (this.random.nextDouble() - 0.5D);
        double dirZ = horiz > 0.01D ? to.z / horiz : (this.random.nextDouble() - 0.5D);
        setDeltaMovement(dirX * speed, lift, dirZ * speed);
        setNoAi(true);
        this.throwTicks = 60;   // 3s hard cap in case he never lands (e.g. thrown over a cliff)

        // Loud and unmistakable: a windup grunt, a launch boom, and a
        // dedicated chat callout, so nobody mistakes this for random ragdoll.
        level().playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW, getSoundSource(), 1.3F, 0.6F);
        level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(), 1.6F, 1.6F);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD
                    + "BARBARA HURLS CAYDEN LIKE A LAWN DART."));
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    20, 0.3D, 0.3D, 0.3D, 0.08D);
        }
    }

    private void tickThrow() {
        if (this.throwTicks <= 0) {
            return;
        }
        this.throwTicks--;
        // A visible trail for the whole flight - this is the part that makes
        // "he's flying twenty blocks" actually readable instead of a blink.
        if (level() instanceof ServerLevel trailLevel && this.throwTicks % 2 == 0) {
            trailLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    3, 0.15D, 0.15D, 0.15D, 0.02D);
        }
        boolean landed = onGround() && this.throwTicks < 57;   // ignore the launch-frame's own ground flag
        if (landed || this.throwTicks <= 0) {
            setNoAi(false);
            this.throwTicks = 0;
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_BIG_FALL, getSoundSource(), 1.4F, 0.8F);
                for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0D),
                        e -> e != this && e.isAlive() && e instanceof Monster)) {
                    nearby.hurt(damageSources().mobAttack(this), 6.0F + getTier() * 2.0F);
                }
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_BIG_FALL, getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    /**
     * Hitbox grows with him, more conservatively than the visual scale in
     * CaydenRenderer#scale (which goes up to 1.6x) so collision never gets
     * comically oversized - just enough that a fully ascended Cayden doesn't
     * look huge while still fitting through a 1-block gap the same as before.
     */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions base = super.getDimensions(pose);
        float scale = 1.0F + getTier() * 0.05F;
        return base.scale(scale);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, BASE_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SsjFlyAttackGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new CaydenFollowOrHomeGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // always hostile toward hostiles - except Kosmonauts (Krave Minions),
        // which ignore him and he ignores them; the player handles those, not
        // Cayden. Krave Healing Boxes no longer need excluding here at all -
        // they're a plain Entity now (End-Crystal-style), not a Monster, so
        // this Monster-typed target selector can never select one anyway.
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                e -> !(e instanceof KraveMinion)));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FED, 0);
        this.entityData.define(RAGE, false);
        this.entityData.define(HOUSED, false);
        this.entityData.define(SSJ, false);
        this.entityData.define(DESPERATE, false);
        this.entityData.define(DARK, false);
        this.entityData.define(TIER, 0);
        this.entityData.define(UNLOCKS, 0);
        this.entityData.define(KI, 0);
    }

    // ---- krave state -------------------------------------------------------

    public int getKraveFed() {
        return this.entityData.get(FED);
    }

    public boolean isRageUnlocked() {
        return this.entityData.get(RAGE);
    }

    public boolean isHoused() {
        return this.entityData.get(HOUSED);
    }

    public float getFatScale() {
        return Math.min(2.4F, 1.0F + getKraveFed() * 0.03F);
    }

    /**
     * Ascension progress parked across one death, keyed by owner.
     *
     * <p>The apocalypse does not resurrect him, it builds a brand new Cayden and
     * copies his Krave counters over. Without a place to hold the ladder, dying
     * once wiped every form the player had paid for - and dying is a thing this
     * mod is entirely built around happening.
     */
    private static final Map<UUID, int[]> ASCENSION_LEGACY = new ConcurrentHashMap<>();

    @Override
    public void die(DamageSource cause) {
        UUID owner = getOwnerUUID();
        if (!level().isClientSide && owner != null) {
            ASCENSION_LEGACY.put(owner, new int[] { getUnlockMask(), getKi() });
        }
        super.die(cause);
    }

    /** Used when he respawns from the Krave blast - keep his hard-won progress. */
    public void restoreKrave(int fed, boolean rage) {
        this.entityData.set(FED, fed);
        this.entityData.set(RAGE, rage);
        UUID owner = getOwnerUUID();
        int[] legacy = owner == null ? null : ASCENSION_LEGACY.remove(owner);
        if (legacy != null) {
            this.entityData.set(UNLOCKS, legacy[0]);
            this.entityData.set(KI, legacy[1]);
        }
        applyKraveStats();
    }

    private void applyKraveStats() {
        int fed = getKraveFed();
        double atkBase = BASE_DAMAGE + fed / 5;
        double spdBase = Math.max(0.12D, BASE_SPEED - fed * 0.012D);
        double hpBase = BASE_HEALTH;
        if (isSuperSaiyan()) {
            // Each rung is a real step up, not a recolour. One table drives the
            // numbers here and the prices in the upgrade screen, so a form can
            // never advertise something it does not deliver.
            AscensionLadder.Rung rung =
                    AscensionLadder.rung(Math.max(AscensionLadder.SSJ, getTier()));
            atkBase *= rung.attackMul();
            spdBase = BASE_SPEED * rung.speedMul();
            hpBase += rung.bonusHealth();
        }
        if (isDark()) {
            // Ruthless, not invulnerable: he hits far harder and closes faster,
            // but he can still be killed - which is what keeps the fight tense.
            atkBase *= 3.0D;
            spdBase = BASE_SPEED * 1.45D;
        }
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (atk != null) {
            atk.setBaseValue(atkBase);
        }
        if (spd != null) {
            spd.setBaseValue(spdBase);
        }
        if (hp != null && hp.getBaseValue() != hpBase) {
            hp.setBaseValue(hpBase);
            // Powering down shrinks the bar. Left alone his current health would
            // sit above the new maximum and the HUD would read as overfull.
            if (getHealth() > getMaxHealth()) {
                setHealth(getMaxHealth());
            }
        }
    }

    /**
     * Rough total damage-per-second across melee and Krave lasers combined,
     * at whatever rung/fed level he's actually at right now. Used by
     * KraveMonster's matchRival() to size the boss off what Cayden can
     * ACTUALLY do this fight, rather than a fixed formula that ignores fed
     * (which directly multiplies melee damage via applyKraveStats) and the
     * lasers, both of which used to make the old estimate wildly low and the
     * fight end in a couple of hits.
     */
    public double estimatedDps() {
        double meleeDps = getAttributeValue(Attributes.ATTACK_DAMAGE);   // ~1 swing/sec baseline
        double laserDps = 0.0D;
        if (isSuperSaiyan()) {
            int interval = Math.max(7, LASER_INTERVAL - getTier() * 4);
            laserDps = (3 * 6.0D) / (interval / 20.0D);   // 3 bolts * KraveLaser.DAMAGE per fire, per fire cadence
        }
        return meleeDps + laserDps;
    }

    // ---- the upgrade ladder --------------------------------------------------

    /** Bitmask of the forms he has been taught. See {@link AscensionLadder}. */
    public int getUnlockMask() {
        return this.entityData.get(UNLOCKS);
    }

    public boolean isFormUnlocked(int tier) {
        return AscensionLadder.unlocked(getUnlockMask(), tier);
    }

    /** The highest rung he is allowed to reach, whatever he is fighting. */
    public int highestUnlockedTier() {
        return AscensionLadder.highest(getUnlockMask());
    }

    /** Spendable ki. */
    public int getKi() {
        return this.entityData.get(KI);
    }

    /** Ki is capped well above the top of the ladder so it can never overflow. */
    public void addKi(int amount) {
        if (amount <= 0 || level().isClientSide) {
            return;
        }
        this.entityData.set(KI, Math.min(999_999, getKi() + amount));
    }

    /**
     * The rung the upgrade screen wants to buy. Every price and prerequisite is
     * re-checked here: the screen is a convenience, not the authority.
     *
     * @return true when the purchase went through
     */
    public boolean tryUnlock(int tier, @Nullable Player buyer) {
        if (level().isClientSide) {
            return false;
        }
        String blocker = AscensionLadder.blocker(tier, getUnlockMask(), getKi(), getKraveFed());
        if (blocker != null) {
            if (buyer != null) {
                buyer.sendSystemMessage(Component.literal(ChatFormatting.RED + blocker));
                playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.3F);
            }
            return false;
        }

        AscensionLadder.Rung rung = AscensionLadder.rung(tier);
        this.entityData.set(KI, getKi() - rung.kiCost());
        this.entityData.set(UNLOCKS, AscensionLadder.withUnlocked(getUnlockMask(), tier));
        applyKraveStats();

        playSound(ModSounds.KRAVE_ROAR.get(), 1.5F, 0.9F + tier * 0.08F);
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.2F, 1.0F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    90, 0.6D, 0.9D, 0.6D, 0.25D);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "CAYDEN LEARNS " + rung.name().toUpperCase(java.util.Locale.ROOT) + "."));
            p.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "  " + rung.edge()));
        }
        return true;
    }

    /** Ticks left in the current transformation spectacle; drives tickSpectacle(). */
    private int spectacleTicks = 0;
    private int spectacleTier = 0;

    /**
     * "The whole world reacts" - THIS is the moment he actually transforms
     * mid-fight (called from announceTier, not the upgrade-screen purchase),
     * and it needs to feel like an event, not a sound effect. Rather than one
     * instant particle burst, this arms a sustained multi-second sequence
     * (see tickSpectacle) of repeated lightning claps, ground-crack waves and
     * shake pulses - a real earthquake, not a flash. Every rung gets
     * something; it escalates brutally from SSJ1's one quick punch up to
     * Ultra Instinct's five-second, screen-shaking, sky-splitting event.
     */
    private void transformationSpectacle(int tier) {
        if (!(level() instanceof ServerLevel sl) || tier < AscensionLadder.SSJ) {
            return;
        }
        this.spectacleTier = tier;
        this.spectacleTicks = 20 + tier * 22;   // SSJ1 ~1s punch -> ULTRA ~5.3s earthquake

        // the very first instant hits hardest - a charge-up hum, then the
        // release burst and the first ring of the lightning wall
        playSound(ModSounds.TRANSFORM_CHARGE.get(), 1.4F, 1.0F);
        groundCrack(sl, 14 + tier * 4, 2.0D + tier * 1.2D);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        playSound(ModSounds.TRANSFORM_RELEASE.get(), 2.0F + tier * 0.15F, 0.6F);
        if (tier >= AscensionLadder.GOD) {
            playSound(ModSounds.TRANSFORM_GODPULSE.get(), 1.5F, 1.0F);
        }
        if (tier >= AscensionLadder.ULTRA) {
            playSound(ModSounds.TRANSFORM_ULTRA_HUM.get(), 1.3F, 1.0F);
        }
        lightningWall(sl, 3.0D + tier * 1.5D, 8 + tier * 2);
    }

    /** Runs the sustained ground-crack/lightning-wall sequence armed by transformationSpectacle. */
    private void tickSpectacle() {
        if (this.spectacleTicks <= 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        this.spectacleTicks--;
        int tier = this.spectacleTier;
        double radius = 3.0D + tier * 2.2D;

        // a pulse every few ticks - the "earthquake" cadence, faster and
        // heavier at higher tiers
        int period = Math.max(3, 9 - tier);
        if (this.spectacleTicks % period == 0) {
            groundCrack(sl, 6 + tier * 3, radius);
            flinchNearbyMobs(sl, radius, 0.06D + tier * 0.035D);
            if (tier >= AscensionLadder.SSJ2 && this.spectacleTicks % (period * 4) == 0) {
                // another ring of the wall, further out each time - a wall
                // that expands outward instead of players getting shoved
                lightningWall(sl, radius, 8 + tier * 2);
            }
        }
    }

    /**
     * A full ring of lightning bolts around him at a fixed radius - the
     * "huge wall" the user asked for instead of pushing anyone away.
     * Players and mobs can walk right up to him through it; it's a visual
     * boundary, not a physical one.
     */
    private void lightningWall(ServerLevel sl, double radius, int bolts) {
        for (int i = 0; i < bolts; i++) {
            double ang = (i / (double) bolts) * Math.PI * 2.0D;
            double lx = getX() + Math.cos(ang) * radius;
            double lz = getZ() + Math.sin(ang) * radius;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(lx, sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) lx, (int) lz), lz);
                bolt.setVisualOnly(true);   // dramatic, not a fire hazard
                sl.addFreshEntity(bolt);
            }
        }
        sl.playSound(null, blockPosition(), ModSounds.LIGHTNING_CRACK.get(), getSoundSource(), 1.6F, 0.9F);
    }

    /** Ground crack: block-particles pulled from what he's standing on. Cosmetic only - nothing is ever broken. */
    private void groundCrack(ServerLevel sl, int count, double radius) {
        var blockState = sl.getBlockState(blockPosition().below());
        for (int i = 0; i < count; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = this.random.nextDouble() * radius;
            sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK, blockState),
                    getX() + Math.cos(ang) * dist, getY() + 0.1D, getZ() + Math.sin(ang) * dist,
                    4, 0.15D, 0.05D, 0.15D, 0.02D);
        }
    }

    /**
     * Hostile mobs flinch away from the shockwave - players are left
     * entirely alone now. They used to get shoved back by the same pulse,
     * which fought against anyone trying to walk up and watch (or fight)
     * him mid-transformation; the lightning wall above is the "you can feel
     * this happening" cue instead.
     */
    private void flinchNearbyMobs(ServerLevel sl, double radius, double strength) {
        for (LivingEntity mob : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e != this && e.isAlive() && !(e instanceof Player))) {
            Vec3 away = mob.position().subtract(position());
            double len = Math.max(0.5D, away.length());
            mob.setDeltaMovement(mob.getDeltaMovement().add(away.scale(strength / len)).add(0.0D, 0.1D, 0.0D));
            mob.hurtMarked = true;
        }
    }

    // ---- super saiyan --------------------------------------------------------

    public boolean isSuperSaiyan() {
        return this.entityData.get(SSJ);
    }

    /** Client-side ticks since he ascended; -1 when not ascended. */
    public int ticksSinceAscension() {
        return this.ssjClientAge;
    }

    /** Arriving in the Kosmos. He ascends, shouts, and stays ascended until the boss falls. */
    public void onEnterKosmos() {
        if (level().isClientSide) {
            return;
        }
        this.ssjUntilBossDies = true;
        becomeSuperSaiyan();
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.6F, 1.0F);
    }

    /**
     * Liquid chocolate does this too - see EventHandler.onLivingTick.
     *
     * <p>Refuses outright when he has not been taught a single form. That gate is
     * the point of the whole upgrade system: the chocolate, the Kosmos and the
     * bosses can all demand an ascension, and none of them can hand him one he
     * has not paid for.
     */
    public void becomeSuperSaiyan() {
        if (isSuperSaiyan() || level().isClientSide) {
            return;
        }
        if (highestUnlockedTier() < AscensionLadder.SSJ) {
            nagLocked(AscensionLadder.SSJ);
            return;
        }
        this.entityData.set(SSJ, true);
        this.ssjTicks = SSJ_DURATION_TICKS;
        setNoGravity(true);
        applyKraveStats();
        heal(getMaxHealth());
        playSound(ModSounds.KRAVE_BOOM.get(), 1.4F, 0.7F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    80, 0.6D, 0.9D, 0.6D, 0.08D);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "CAYDEN COBB HAS ASCENDED."));
        }
    }

    /**
     * Tells the owner what he is straining against, at most once every ten
     * seconds - the alternative is a wall of chat during a boss fight.
     */
    private void nagLocked(int wanted) {
        if (this.lockedNag > 0 || level().isClientSide) {
            return;
        }
        this.lockedNag = LOCKED_NAG_INTERVAL;
        playSound(ModSounds.CAYDEN_HURT.get(), 0.8F, 1.3F);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0D))) {
            if (isOwnedBy(p)) {
                p.sendSystemMessage(Component.literal(ChatFormatting.YELLOW
                        + "Cayden reaches for " + AscensionLadder.nameOf(wanted)
                        + " and finds nothing there."));
                p.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                        + "  Open his ascension ledger and teach it to him."));
                // The stakes changed: outclassed against the Monster he can now
                // actually die. Saying so is the difference between a player who
                // pulls him out and one who watches Rule #1 break and wonders why.
                if (getTarget() instanceof KraveMonster monster) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD
                            + "  He cannot win Form " + monster.getForm()
                            + " without it. Get him out of there."));
                }
            }
        }
    }

    /** Back to being a kid who eats too much cereal. */
    public void powerDown() {
        boolean was = isSuperSaiyan();
        this.entityData.set(SSJ, false);
        // Tier drives his stats and his aura, so it must fall with the form. The
        // fight-ended path already cleared it; the boss-death path did not.
        this.entityData.set(TIER, 0);
        this.ssjTicks = 0;
        this.ssjUntilBossDies = false;
        setNoGravity(false);
        applyKraveStats();
        if (was && !level().isClientSide) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                        + "Cayden powers down. He is just a kid again."));
            }
        }
    }

    // ---- housing -----------------------------------------------------------

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** Try to claim the room the player is standing in. Reports problems if not. */
    public void tryClaimHome(Player player) {
        HousingResult result = HousingValidator.validate(level(), player.blockPosition());
        if (result.valid) {
            this.home = result.anchor;
            this.entityData.set(HOUSED, true);
            this.homeDim = level().dimension().location().toString();
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                    + "Cayden moves in. (" + result.volume + " blocks of space)"));
            playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.4F);
        } else {
            this.home = null;
            this.entityData.set(HOUSED, false);
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "Cayden refuses to live here:"));
            for (String problem : result.problems) {
                player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + " - " + problem));
            }
            playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.4F);
        }
    }

    /** True only when he is standing in the dimension his house is actually in. */
    public boolean homeIsInThisDimension() {
        return this.home != null
                && level().dimension().location().toString().equals(this.homeDim);
    }

    /** Periodically re-check the claimed home; he moves out if you ruin it. */
    private void recheckHome() {
        // His house is in the Overworld. Validating those coordinates against
        // Kosmos terrain would evict him for a room that is not even here.
        if (!homeIsInThisDimension()) {
            return;
        }
        if (this.home == null || !(level() instanceof ServerLevel)) {
            return;
        }
        HousingResult result = HousingValidator.validate(level(), this.home);
        if (!result.valid) {
            this.entityData.set(HOUSED, false);
            BlockPos lost = this.home;
            this.home = null;
            List<Player> near = level().getEntitiesOfClass(Player.class,
                    new AABB(lost).inflate(32.0D));
            for (Player p : near) {
                p.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "Cayden moved out! " + ChatFormatting.GRAY + result.summary()));
            }
        } else {
            this.home = result.anchor;
            this.entityData.set(HOUSED, true);
        }
    }

    // ---- tick --------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Client-side age of the transformation, used by SsjAuraLayer for the
            // one-second ground shockwave. Derived from the synced SSJ flag rather
            // than networked separately - there is nothing here worth a packet.
            if (isSuperSaiyan()) {
                this.ssjClientAge = this.ssjClientAge < 0 ? 0 : this.ssjClientAge + 1;
            } else {
                this.ssjClientAge = -1;
            }
            return;
        }

        tickThrow();
        tickSpectacle();
        tickUltraVortex();

        if (this.graceTicks > 0) {
            this.graceTicks--;
            // He respawns airborne. Without this he banks a lethal fall distance
            // during the grace window and dies the instant it runs out.
            this.fallDistance = 0.0F;
            this.setRemainingFireTicks(0);
            if (this.tickCount % 4 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        2, 0.35D, 0.4D, 0.35D, 0.0D);
            }
            if (this.graceTicks == 0) {
                setInvulnerable(false);
            }
        }

        if (!isSuperSaiyan() && getFluidTypeHeight(ModFluids.CHOCOLATE_TYPE.get()) > 0.0D) {
            becomeSuperSaiyan();
        }
        if (isSuperSaiyan() && this.ssjUntilBossDies && this.tickCount % 20 == 0
                && level() instanceof ServerLevel sl) {
            // Go and find him. The Kosmos is far too big to rely on follow range.
            if (!(getTarget() instanceof KraveMonster boss) || !boss.isAlive()) {
                for (KraveMonster candidate : sl.getEntitiesOfClass(KraveMonster.class,
                        getBoundingBox().inflate(512.0D, 256.0D, 512.0D))) {
                    if (candidate.isAlive()) {
                        setTarget(candidate);
                        break;
                    }
                }
            }
        }

        regenerate();
        updateDesperation();
        updateDark();
        scanForBoss();
        updateTier();
        useArsenal();
        collectKi();

        if (isSuperSaiyan()) {
            // Linked to the boss fight, the transformation has no clock on it -
            // it lasts exactly as long as the Krave Monster does.
            if (!this.ssjUntilBossDies && --this.ssjTicks <= 0) {
                powerDown();
            } else if (this.tickCount % 5 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        3, 0.35D, 0.6D, 0.35D, 0.03D);
            }
        }

        // auto-tame to whoever is nearby, however he got here
        if (!isTame()) {
            Player near = level().getNearestPlayer(this, 12.0D);
            if (near != null) {
                tame(near);
                near.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                        + "Cayden Cobb imprinted on you. Feed him Krave - and build him a house."));
            }
        }

        if (this.tickCount % 10 == 0 && isTame()
                && getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
            com.barbarajones.net.ModNetwork.sendTo(owner,
                    new com.barbarajones.net.PacketCaydenStatus(getHealth(), getMaxHealth(),
                            (int) Math.sqrt(distanceToSqr(owner)), isHoused()));
        }

        if (--this.homeCheckTimer <= 0) {
            this.homeCheckTimer = 200;
            recheckHome();
        }

        // hostiles hunt him back
        if (this.tickCount % 20 == 0) {
            for (Monster m : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(12.0D))) {
                if (m.isAlive() && m.getTarget() == null) {
                    m.setTarget(this);
                }
            }
        }
    }

    // ---- interaction -------------------------------------------------------

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        var held = player.getItemInHand(hand);

        // The ascension ledger, or the Cayden Compass as the always-available
        // fallback: both open his upgrade screen. The screen is drawn purely from
        // his synched data, so it opens client-side and only the buy button ever
        // talks to the server.
        if (isTame() && isOwnedBy(player)
                && (AscensionLadder.isLedger(held)
                    || held.is(com.barbarajones.content.ModItems.CAYDEN_COMPASS.get()))) {
            if (level().isClientSide) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.barbarajones.client.ui.CaydenUpgradeKeys.open(this));
            } else {
                playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.1F);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        // The one-in-a-hundred-thousand box is worth thirty ordinary ones. It is
        // the only way to buy the first rung without a month of hand-feeding.
        if (held.is(com.barbarajones.content.ModItems.GOLDEN_KRAVE.get())) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    tame(player);
                }
                feedGoldenKrave(player);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        // The crafted mid-tier from the Krave Crafting Economy module
        // (com.barbarajones.v2.economy.KraveEconomy.RICH_KRAVE) - better than a
        // plain box, not the one-in-a-hundred-thousand golden one.
        if (held.is(com.barbarajones.v2.economy.KraveEconomy.RICH_KRAVE.get())) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    tame(player);
                }
                feedRichKrave(player);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (held.is(com.barbarajones.content.ModItems.KRAVE_CEREAL.get())) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    tame(player);
                }
                feedKrave(player);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        // empty hand from the owner: claim the room you're standing in
        if (held.isEmpty() && isTame() && isOwnedBy(player)) {
            if (!level().isClientSide) {
                tryClaimHome(player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** The golden box: a full heal, thirty ki, and a great deal of noise. */
    private void feedGoldenKrave(Player player) {
        this.entityData.set(FED, getKraveFed() + 1);
        heal(getMaxHealth());
        addKi(AscensionLadder.KI_PER_GOLDEN_KRAVE);
        applyKraveStats();
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.8F);
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.4F, 0.9F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + getBbHeight() * 0.7D, getZ(), 30, 0.5D, 0.6D, 0.5D, 0.0D);
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                + "The golden box. +" + AscensionLadder.KI_PER_GOLDEN_KRAVE + " Ki. ("
                + getKi() + " banked)"));
    }

    /** Ki banked per Rich Krave box - between a plain box and the golden one. */
    private static final int KI_PER_RICH_KRAVE = 5;

    /** Rich Krave: better than the plain box, well short of the golden one. */
    private void feedRichKrave(Player player) {
        this.entityData.set(FED, getKraveFed() + 1);
        heal(7.0F);
        addKi(KI_PER_RICH_KRAVE);
        applyKraveStats();
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.1F, 1.0F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + getBbHeight() * 0.7D, getZ(), 10, 0.4D, 0.5D, 0.4D, 0.0D);
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                + "Rich Krave. +" + KI_PER_RICH_KRAVE + " Ki. (" + getKi() + " banked)"));
    }

    private void feedKrave(Player player) {
        this.entityData.set(FED, getKraveFed() + 1);
        int fed = getKraveFed();
        heal(4.0F);
        addKi(AscensionLadder.KI_PER_KRAVE);
        applyKraveStats();
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.2F);
        // he announces it. every single time.
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.0F, 1.0F);

        if (fed % 5 == 0) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                    + "Cayden's attack rose to " + (int) getAttributeValue(Attributes.ATTACK_DAMAGE)
                    + "! (slower, and fatter...)"));
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "  " + getKi() + " Ki banked, " + fed + " boxes eaten."));
        }
        if (fed >= RAGE_THRESHOLD && !isRageUnlocked()) {
            this.entityData.set(RAGE, true);
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + ""
                    + ChatFormatting.BOLD + "KRAVE RAGE UNLOCKED!"));
        }
    }

    // ---- misc --------------------------------------------------------------

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    /**
     * Heals like a well-fed player: a point every few seconds, but only once he
     * has been left alone for a moment. Without this he accumulated every scrape
     * from every fight until something trivial finished him off - and his death
     * is the one thing this mod is built around avoiding.
     */
    private void regenerate() {
        if (getHealth() >= getMaxHealth() || !isAlive()) {
            return;
        }
        // hurtTime is set on every hit; use the vanilla last-hurt clock so he
        // does not heal through a fight he is currently losing.
        if (getLastHurtByMob() != null
                && this.tickCount - getLastHurtByMobTimestamp() < OUT_OF_COMBAT_TICKS) {
            return;
        }
        if (this.tickCount % REGEN_INTERVAL == 0) {
            heal(1.0F);
            if (level() instanceof ServerLevel sl && getHealth() < getMaxHealth()) {
                sl.sendParticles(ParticleTypes.HEART,
                        getX(), getY() + getBbHeight() + 0.2D, getZ(),
                        1, 0.25D, 0.1D, 0.25D, 0.0D);
            }
        }
    }

    /**
     * Finds a boss worth transforming for, far outside his ordinary follow
     * range.
     *
     * <p>His FOLLOW_RANGE is 32 blocks and NearestAttackableTargetGoal will not
     * look past it, but an Ender Dragon circles hundreds of blocks out and
     * hundreds of blocks up. Without this he simply never noticed one was there,
     * which read in play as him doing nothing at all.
     */
    private void scanForBoss() {
        if (this.tickCount % 20 != 0 || !isTame()) {
            return;
        }
        LivingEntity current = getTarget();
        if (current != null && current.isAlive() && demandFor(current) > 0) {
            return;                       // already committed to something worthy
        }
        AABB far = getBoundingBox().inflate(BOSS_SCAN_RANGE, BOSS_SCAN_RANGE, BOSS_SCAN_RANGE);
        LivingEntity best = null;
        int bestTier = 0;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, far)) {
            int t = demandFor(e);
            if (t > bestTier) {
                bestTier = t;
                best = e;
            }
        }
        if (best != null) {
            setTarget(best);
        }
    }

    // ---- earning it ---------------------------------------------------------

    /**
     * Pays out ki when whatever he is fighting stops moving.
     *
     * <p>Tracked as a held reference rather than hooked off a death event because
     * the reward has to survive him turning away mid-fight, and because the
     * player is usually the one who lands the last hit on a boss - Cayden was
     * still the reason it died.
     *
     * <p>The reference is only ever kept for something in this level, so it
     * cannot pin an entity across a dimension change.
     */
    private void collectKi() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && target.level() == level()) {
            int demand = demandFor(target);
            if (this.creditFoe == null || !this.creditFoe.isAlive() || demand >= this.creditDemand) {
                this.creditFoe = target;
                this.creditDemand = demand;
            }
        }
        if (this.creditFoe == null) {
            return;
        }
        if (this.creditFoe.level() != level() || distanceToSqr(this.creditFoe) > 256.0D * 256.0D) {
            this.creditFoe = null;                 // it walked out of the story
            this.creditDemand = 0;
            return;
        }
        if (this.creditFoe.isAlive() && !this.creditFoe.isRemoved()) {
            return;
        }

        int reward = AscensionLadder.kiForKill(this.creditDemand);
        int demand = this.creditDemand;
        this.creditFoe = null;
        this.creditDemand = 0;
        addKi(reward);
        if (demand > 0) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                if (isOwnedBy(p)) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                            + "Cayden takes " + reward + " Ki off that. (" + getKi() + " banked)"));
                }
            }
        }
    }

    // ---- the ladder ---------------------------------------------------------

    public int getTier() {
        return this.entityData.get(TIER);
    }

    /** True on the far side of the Krave Door. The Kosmos is divine ground. */
    private boolean inKosmos() {
        return level().dimension().equals(KraveDimensions.KRAVE_KOSMOS);
    }

    /**
     * What the Krave Monster's six incarnations are each worth - one form
     * per rung from Super Saiyan through Ultra Instinct, so every step of
     * the ladder has a Monster form that actually demands it instead of
     * jumping straight from SSJ2 to GOD.
     */
    private static final int[] KRAVE_FORM_DEMAND = {
        AscensionLadder.SSJ, AscensionLadder.SSJ2, AscensionLadder.SSJ3,
        AscensionLadder.GOD, AscensionLadder.BLUE, AscensionLadder.ULTRA
    };

    /**
     * What he turns into is decided by what is in front of him. Fighting the
     * Wither is worth a transformation; fighting the Krave Monster is worth
     * everything he has.
     *
     * <p>Returns the tier the opponent deserves, 0 for anything ordinary. Note
     * this is what the fight <em>demands</em>, not what he can actually reach -
     * {@link #updateTier()} clamps it to what he has been taught.
     */
    private int demandFor(@Nullable LivingEntity foe) {
        if (foe == null || !foe.isAlive()) {
            return 0;
        }
        int base;
        if (foe instanceof com.barbarajones.entity.KraveMonster monster) {
            int form = Math.max(1, Math.min(KRAVE_FORM_DEMAND.length, monster.getForm()));
            base = KRAVE_FORM_DEMAND[form - 1];
        } else if (foe instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            base = AscensionLadder.SSJ3;                  // it flies, so he has to
        } else if (foe instanceof com.barbarajones.v2.internet.InternetManagerBoss) {
            // The outage boss. Its own module suggested God; Blue is what was
            // asked for, and it reads better anyway - Blue is God power held
            // perfectly still, which is the right answer to a man whose whole
            // threat is latency and throttling.
            base = AscensionLadder.BLUE;
        } else if (foe instanceof net.minecraft.world.entity.monster.warden.Warden
                || foe instanceof com.barbarajones.boss.manager.TheManager) {
            base = AscensionLadder.SSJ2;                  // above a Wither
        } else if (foe instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
            base = AscensionLadder.SSJ;
        } else {
            return 0;
        }
        if (inKosmos()) {
            // Nothing is fought in the Kosmos below Super Saiyan God, and
            // anything that already demanded God there demands Blue. This is the
            // only place either of those two forms is ever required, which is
            // what makes the dimension the back half of the ladder.
            base = Math.max(base + 1, AscensionLadder.GOD);
        }
        return Math.min(base, AscensionLadder.MAX);
    }

    /**
     * Escalates to meet the opponent, and never quietly drops back down mid
     * fight - he powers down when the fight is over, not when the boss happens
     * to step out of range for a tick.
     */
    private void updateTier() {
        if (this.lockedNag > 0) {
            this.lockedNag--;
        }
        int demand = demandFor(getTarget());
        int allowed = highestUnlockedTier();
        // A form he has not been taught is not available to him, however badly
        // the fight wants it. He fights the boss in the best thing he has.
        int want = Math.min(demand, allowed);
        if (demand > allowed) {
            nagLocked(demand);
        }
        int have = getTier();

        if (want > have) {
            this.entityData.set(TIER, want);
            if (!isSuperSaiyan()) {
                becomeSuperSaiyan();      // reuses the existing ascension beat
            }
            this.ssjUntilBossDies = true; // it lasts as long as the fight does
            applyKraveStats();
            announceTier(want);
        } else if (want == 0 && have > 0) {
            // A dead boss still lingers as getTarget() for a tick or two, and the
            // old check also demanded a null target - between them he never
            // powered down at all. Give it a moment in case the boss simply
            // blinked out of sight, then drop him all the way back.
            if (++this.tierIdle > 40) {
                this.tierIdle = 0;
                this.entityData.set(TIER, 0);
                this.ssjUntilBossDies = false;
                powerDown();
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                            + "Cayden powers down."));
                }
            }
        } else {
            this.tierIdle = 0;
        }

        if (getTier() >= AscensionLadder.SSJ2) {
            ssj2Shockwave();
        }
        if (getTier() >= AscensionLadder.SSJ) {
            // Without these he is a ground mob with a melee goal, which against
            // anything airborne means standing still and being shot.
            combatFlight();
            combatLasers();
        }
        formTrail();
        if (this.dodgeFlash > 0) {
            this.dodgeFlash--;
        }
    }

    /**
     * The idle signature of the top three forms, so you can tell across a field
     * which one he is standing in without waiting for him to swing.
     *
     * <p>The aura layer draws the shape; this draws the colour he leaves behind
     * him, which is the part that survives being twenty blocks away.
     */
    private void formTrail() {
        int tier = getTier();
        if (tier < AscensionLadder.GOD || this.tickCount % 2 != 0
                || !(level() instanceof ServerLevel sl)) {
            return;
        }
        net.minecraft.core.particles.SimpleParticleType particle = switch (tier) {
            case AscensionLadder.GOD -> ParticleTypes.FLAME;             // divine red
            case AscensionLadder.BLUE -> ParticleTypes.SOUL_FIRE_FLAME;  // cold blue
            default -> ParticleTypes.END_ROD;                            // silver, calm
        };
        sl.sendParticles(particle, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                2, 0.3D, 0.5D, 0.3D, 0.01D);
    }

    private void announceTier(int tier) {
        String line = switch (tier) {
            case AscensionLadder.SSJ -> ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "CAYDEN COBB HAS ASCENDED.";
            case AscensionLadder.SSJ2 -> ChatFormatting.YELLOW + "" + ChatFormatting.BOLD
                    + "THAT WAS NOT ALL OF IT. SUPER SAIYAN 2.";
            case AscensionLadder.SSJ3 -> ChatFormatting.AQUA + "" + ChatFormatting.BOLD
                    + "IT FLIES. SO DOES HE. SUPER SAIYAN 3.";
            case AscensionLadder.GOD -> ChatFormatting.RED + "" + ChatFormatting.BOLD
                    + "THE COLOUR GOES OUT OF HIM. SUPER SAIYAN GOD.";
            case AscensionLadder.BLUE -> ChatFormatting.BLUE + "" + ChatFormatting.BOLD
                    + "GOD POWER, HELD STILL. SUPER SAIYAN BLUE.";
            case AscensionLadder.ULTRA -> ChatFormatting.WHITE + "" + ChatFormatting.BOLD
                    + "He stops trying. ULTRA INSTINCT.";
            default -> null;
        };
        if (line == null) {
            return;
        }
        playSound(ModSounds.KRAVE_ROAR.get(), 1.6F,
                tier >= AscensionLadder.GOD ? 1.4F : 0.8F);
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(64.0D))) {
            p.sendSystemMessage(Component.literal(line));
        }
        // This is the ACTUAL transformation moment - he is standing in the
        // fight becoming this form right now, not just learning it exists on
        // an upgrade screen. Every tier gets a real spectacle; it scales up
        // savagely from here.
        transformationSpectacle(tier);
    }

    /** SSJ2: the air itself keeps detonating around him. */
    private void ssj2Shockwave() {
        if (--this.waveTimer > 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        this.waveTimer = WAVE_INTERVAL;
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0D, getZ(),
                3, 1.2D, 0.6D, 1.2D, 0.0D);
        sl.playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(), 1.1F, 1.7F);

        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(7.0D))) {
            if (victim == this || victim instanceof Player
                    || victim instanceof com.barbarajones.entity.BarbaraJones) {
                continue;
            }
            Vec3 away = victim.position().subtract(position());
            double len = Math.max(0.4D, away.length());
            victim.push(away.x / len * 0.9D, 0.45D, away.z / len * 0.9D);
            victim.hurtMarked = true;
            victim.hurt(sl.damageSources().mobAttack(this), 4.0F + getTier() * 2.0F);
        }
    }

    // ---- the arsenal --------------------------------------------------------

    /**
     * Transformed, he can reach for pieces of the apocalypse itself and throw
     * them at whatever he is fighting.
     *
     * <p>Only hardware that cannot hit his own side is in here. The meteors
     * already refuse to damage Cayden, Barbara or players; the giant box and the
     * sky actors are pure spectacle. The tornado and the mouth beam are both
     * built to grab or burn the PLAYER - they belong to the apocalypse, not to
     * him, and putting them in his kit would have him attacking you.
     */
    private void useArsenal() {
        int tier = getTier();
        LivingEntity boss = getTarget();
        if (tier < 1 || boss == null || !boss.isAlive() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (--this.arsenalTimer > 0) {
            return;
        }
        // Higher forms cycle techniques faster - that escalation is most of what
        // makes the top of the ladder feel different from the bottom in play.
        // The step is per rung, so widening the ladder widened the ramp too.
        this.arsenalTimer = Math.max(24, ARSENAL_INTERVAL - (tier - 1) * 13);

        int moves = tier >= AscensionLadder.GOD ? 6 : tier >= AscensionLadder.SSJ2 ? 4 : 2;
        int move = this.random.nextInt(moves);
        if (move == this.lastMove) {                 // never the same trick twice running
            move = (move + 1) % moves;
        }
        this.lastMove = move;

        switch (move) {
            case 0 -> meteorVolley(sl, boss, com.barbarajones.entity.KraveMeteor.TYPE_METEOR,
                    2 + tier, "The sky answers him.");
            case 1 -> meteorVolley(sl, boss, com.barbarajones.entity.KraveMeteor.TYPE_KNIFE,
                    4 + tier * 2, "Knives. Actual knives.");
            case 2 -> meteorVolley(sl, boss, com.barbarajones.entity.KraveMeteor.TYPE_PIBB,
                    6 + tier * 2, "It is raining Mr Pibb.");
            case 3 -> meteorVolley(sl, boss, com.barbarajones.entity.KraveMeteor.TYPE_GATORADE,
                    6 + tier * 2, "Blue Gatorade, from orbit.");
            case 4 -> boxDrop(sl, boss);
            default -> skyCameo(sl, boss);
        }
    }

    /** A cluster of one kind of falling object, aimed at the boss. */
    private void meteorVolley(ServerLevel sl, LivingEntity boss, byte kind, int count, String callout) {
        Vec3 at = boss.position();
        for (int i = 0; i < count; i++) {
            com.barbarajones.entity.KraveMeteor m =
                    com.barbarajones.content.ModEntities.METEOR.get().create(sl);
            if (m == null) {
                continue;
            }
            double ox = (this.random.nextDouble() - 0.5D) * 9.0D;
            double oz = (this.random.nextDouble() - 0.5D) * 9.0D;
            m.saiyanStrike(this);            // attributed to him, so bosses take it in full
            m.kind(kind);
            m.setPos(at.x + ox, at.y + 30.0D + i * 2.5D, at.z + oz);
            m.aim(-ox * 0.05D, -oz * 0.05D);
            sl.addFreshEntity(m);
        }
        playSound(ModSounds.KRAVE_ROAR.get(), 1.3F, 1.1F);
        say(callout);
    }

    /** The giant Krave box, dropped from a height onto whatever he is fighting. */
    private void boxDrop(ServerLevel sl, LivingEntity boss) {
        com.barbarajones.entity.GiantKraveBox box =
                com.barbarajones.content.ModEntities.GIANT_BOX.get().create(sl);
        if (box != null) {
            box.setPos(boss.getX(), boss.getY() + 36.0D, boss.getZ());
            sl.addFreshEntity(box);
        }
        // the box itself is cosmetic, so the hurt comes from what rides it down
        meteorVolley(sl, boss, com.barbarajones.entity.KraveMeteor.TYPE_BOX, 6, null);
        sl.playSound(null, boss.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                getSoundSource(), 2.0F, 0.6F);
        say("He drops the whole box on them.");
    }

    /** He calls one of the sky actors down for a moment. Pure theatre. */
    private void skyCameo(ServerLevel sl, LivingEntity boss) {
        byte kind = this.random.nextBoolean()
                ? com.barbarajones.entity.SkyCinematic.TORCHER
                : com.barbarajones.entity.SkyCinematic.POURER;
        com.barbarajones.entity.SkyCinematic actor =
                com.barbarajones.content.ModEntities.SKY_CINEMATIC.get().create(sl);
        if (actor != null) {
            actor.kind(kind).lifespan(90).walkTo(boss.getX(), boss.getZ());
            actor.setPos(boss.getX() - 24.0D, boss.getY() + 2.0D, boss.getZ() - 24.0D);
            sl.addFreshEntity(actor);
        }
        say(kind == com.barbarajones.entity.SkyCinematic.TORCHER
                ? "He calls her down with the blowtorch."
                : "He calls her down with the Pibb.");
    }

    private void say(@Nullable String line) {
        if (line == null) {
            return;
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + line));
        }
    }

    // ---- dark cayden --------------------------------------------------------

    public boolean isDark() {
        return this.entityData.get(DARK);
    }

    /**
     * His mother is the one thing that turns him. Fighting her drops the kid act
     * entirely: he stops flinching, starts flying at her in bursts, and the
     * ground he passes over dies.
     *
     * <p>Deliberately keyed to WHO he is fighting rather than to his health, so
     * it never overlaps with the desperation state - that one is about losing,
     * this one is about her.
     */
    private void updateDark() {
        boolean fightingMum = getTarget() instanceof com.barbarajones.boss.mom.MomCobbBoss
                && getTarget().isAlive();

        if (fightingMum && !isDark()) {
            this.entityData.set(DARK, true);
            applyKraveStats();
            playSound(ModSounds.KRAVE_ROAR.get(), 1.6F, 0.55F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0D, getZ(),
                        60, 0.6D, 1.0D, 0.6D, 0.05D);
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.0D, getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE + ""
                        + ChatFormatting.BOLD + "Cayden stops being a kid about it."));
            }
        } else if (!fightingMum && isDark()) {
            this.entityData.set(DARK, false);
            setNoGravity(false);
            this.flightTicks = 0;
            applyKraveStats();
        }

        if (!isDark()) {
            return;
        }

        corruptGround();
        // updateTier already drives these for any transformed Cayden. Calling
        // them again here would decrement the shot timer twice a tick and
        // double his rate of fire.
        if (getTier() < 1) {
            combatFlight();
            combatLasers();
        }

        if (this.tickCount % 2 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                    3, 0.4D, 0.5D, 0.4D, 0.01D);
        }
    }

    /**
     * Everything growing near him rots. Plants are destroyed and grass blocks go
     * to coarse dirt; nothing structural is touched, so this scars the field
     * without eating anyone's build.
     */
    private void corruptGround() {
        if (this.tickCount % 10 != 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        BlockPos centre = blockPosition();
        for (int i = 0; i < 8; i++) {
            BlockPos p = centre.offset(
                    this.random.nextInt(CORRUPT_RADIUS * 2 + 1) - CORRUPT_RADIUS,
                    this.random.nextInt(3) - 1,
                    this.random.nextInt(CORRUPT_RADIUS * 2 + 1) - CORRUPT_RADIUS);
            var state = sl.getBlockState(p);

            if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                    || state.is(Blocks.LARGE_FERN) || state.is(BlockTags.FLOWERS)
                    || state.is(BlockTags.SAPLINGS) || state.is(Blocks.SUGAR_CANE)) {
                sl.destroyBlock(p, false);
                sl.sendParticles(ParticleTypes.ASH, p.getX() + 0.5D, p.getY() + 0.3D, p.getZ() + 0.5D,
                        4, 0.3D, 0.2D, 0.3D, 0.0D);
            } else if (state.is(Blocks.GRASS_BLOCK)) {
                sl.setBlockAndUpdate(p, Blocks.COARSE_DIRT.defaultBlockState());
                sl.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5D, p.getY() + 1.05D, p.getZ() + 0.5D,
                        2, 0.3D, 0.0D, 0.3D, 0.0D);
            } else if (state.is(BlockTags.LEAVES) && this.random.nextInt(4) == 0) {
                sl.destroyBlock(p, false);
            }
        }
    }

    /** Short bursts of flight to close on her, not sustained hovering. */
    private void combatFlight() {
        LivingEntity target = getTarget();
        if (target == null) {
            return;
        }
        if (this.flightTicks > 0) {
            this.flightTicks--;
            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D)
                    .subtract(position());
            double len = to.length();
            if (len > 0.5D) {
                // Bias upward while he is below them, or he skims along underneath
                // a hovering boss forever without ever arriving.
                // Chase speed scales with the form. At the old flat rate an Ender
                // Dragon simply outran him forever - it moves faster than he
                // could close, so he trailed it and never arrived.
                // Capped at the old top rung: past that he overshoots the target
                // every tick and orbits it instead of landing on it.
                int chase = Math.min(getTier(), AscensionLadder.GOD);
                double pull = 0.22D + chase * 0.16D;
                double lift = to.y > 0.5D ? 0.06D + chase * 0.05D : 0.0D;
                setDeltaMovement(getDeltaMovement().scale(0.72D)
                        .add(to.scale(pull / len))
                        .add(0.0D, lift, 0.0D));
            }
            this.fallDistance = 0.0F;
            if (this.flightTicks == 0) {
                setNoGravity(false);
            }
            return;
        }
        // A target that is genuinely airborne is chased continuously rather than
        // in bursts. Bursts are fine for closing ground on something that walks;
        // against a Wither they just drop him back down to be shot again.
        // Anything airborne, or any boss at all, is pursued without pause. Bursts
        // are for closing on things that walk.
        boolean airborne = target.getY() - getY() > 3.0D || !target.onGround()
                || demandFor(target) >= AscensionLadder.SSJ3;
        if (airborne && distanceToSqr(target) > 4.0D) {
            this.flightTicks = Math.max(this.flightTicks, 12);
            setNoGravity(true);
            if (this.burstTimer <= 0) {
                this.burstTimer = BURST_INTERVAL;
                playSound(ModSounds.KRAVE_SCREECH.get(), 0.9F, 0.7F);
            } else {
                this.burstTimer--;
            }
            return;
        }

        if (--this.burstTimer <= 0) {
            this.burstTimer = BURST_INTERVAL;
            if (distanceToSqr(target) > 9.0D) {
                this.flightTicks = 22;
                setNoGravity(true);
                setDeltaMovement(getDeltaMovement().add(0.0D, 0.42D, 0.0D));
                playSound(ModSounds.KRAVE_SCREECH.get(), 0.9F, 0.7F);
            }
        }

    }

    /** Red Krave lasers, fired in threes with a little spread. */
    private void combatLasers() {
        LivingEntity target = getTarget();
        if (target == null || --this.laserTimer > 0) {
            return;
        }
        // The higher forms fire noticeably faster - it is his main answer to
        // anything he cannot punch.
        this.laserTimer = Math.max(7, LASER_INTERVAL - getTier() * 4);
        if (!hasLineOfSight(target)) {
            return;
        }
        Vec3 from = position().add(0.0D, getBbHeight() * 0.75D, 0.0D);
        for (int i = 0; i < 3; i++) {
            // Lead the shot: a Wither drifts, and bolts aimed at where it was
            // sail behind it.
            Vec3 lead = target.getDeltaMovement().scale(6.0D);
            Vec3 aim = target.position().add(lead).add(
                    (this.random.nextDouble() - 0.5D) * 0.8D,
                    target.getBbHeight() * 0.5D + (this.random.nextDouble() - 0.5D) * 0.6D,
                    (this.random.nextDouble() - 0.5D) * 0.8D);
            com.barbarajones.entity.KraveLaser bolt =
                    new com.barbarajones.entity.KraveLaser(level(), this, from, aim);
            if (getOwner() instanceof Player owner) {
                bolt.creditTo(owner);   // so the Ender Dragon does not discard it
            }
            level().addFreshEntity(bolt);
        }
        playSound(ModSounds.KRAVE_BOOM.get(), 1.0F, 1.8F);
    }

    // ---- flashes of the real thing -----------------------------------------

    public boolean isDesperate() {
        return this.entityData.get(DESPERATE);
    }

    /**
     * Cornered, he half-ascends. Not the full transformation - no flight, no
     * invulnerability - but enough to fight his way out: damage resistance,
     * fists that burn, and the occasional meteor.
     *
     * <p>Entering and leaving use different thresholds on purpose. A single
     * threshold makes him strobe in and out of the state while he trades blows
     * around that health value.
     */
    private void updateDesperation() {
        float frac = getMaxHealth() <= 0.0F ? 1.0F : getHealth() / getMaxHealth();
        boolean now = isDesperate();

        if (!now && !isSuperSaiyan() && frac <= DESPERATE_AT && getTarget() != null) {
            this.entityData.set(DESPERATE, true);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, false));
            addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
            playSound(ModSounds.KRAVE_ROAR.get(), 1.3F, 1.35F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        40, 0.5D, 0.7D, 0.5D, 0.06D);
            }
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                        + "Cayden is cornered - he is not going down like this."));
            }
        } else if (now && (frac >= DESPERATE_OFF || getTarget() == null || isSuperSaiyan())) {
            this.entityData.set(DESPERATE, false);
        }

        // top the effects up while it lasts, so a long fight does not run them out
        if (isDesperate() && this.tickCount % 100 == 0) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, false));
            addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
        }
        if (isDesperate() && this.tickCount % 3 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    2, 0.32D, 0.45D, 0.32D, 0.01D);
        }
    }

    /**
     * A damage source the target will actually accept.
     *
     * <p>{@code EnderDragon.hurt} throws damage away unless the source's causing
     * entity is a Player or the damage type is tagged to always hurt dragons.
     * Cayden is neither, so his every hit on one was silently discarded and the
     * fight could not be won. Crediting his owner is both the fix and the honest
     * attribution - he is their companion, fighting on their behalf.
     */
    private net.minecraft.world.damagesource.DamageSource sourceFor(net.minecraft.world.entity.Entity target) {
        boolean dragonish = target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || target instanceof net.minecraft.world.entity.boss.EnderDragonPart;
        if (dragonish && getOwner() instanceof Player owner) {
            return level().damageSources().indirectMagic(this, owner);
        }
        return level().damageSources().mobAttack(this);
    }

    /** True when vanilla's own melee path would have its damage thrown away. */
    private static boolean needsOwnerCredit(net.minecraft.world.entity.Entity target) {
        return target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || target instanceof net.minecraft.world.entity.boss.EnderDragonPart;
    }

    /**
     * Every punch is a chance for the power to show through: a one-in-ten
     * launch, and while desperate, burning fists and a one-in-five meteor.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit;
        if (!level().isClientSide && needsOwnerCredit(target)) {
            // Vanilla's melee would build a mobAttack source, which the dragon
            // discards outright. Apply it ourselves with the owner credited.
            float dmg = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            hit = target.hurt(sourceFor(target), dmg);
            if (hit) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        } else {
            hit = super.doHurtTarget(target);
        }
        if (!hit || level().isClientSide) {
            return hit;
        }

        playSound(ModSounds.COMBAT_PUNCH.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);

        // 1-in-10: a flash of the full thing. Ten times the shove.
        if (this.random.nextInt(FLASH_ODDS) == 0) {
            double yaw = Math.toRadians(getYRot());
            target.push(-Math.sin(yaw) * 3.4D, 0.85D, Math.cos(yaw) * 3.4D);
            target.hurtMarked = true;   // without this the client never sees the launch
            playSound(ModSounds.KRAVE_BOOM.get(), 1.1F, 1.6F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        3, 0.3D, 0.3D, 0.3D, 0.0D);
                sl.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 0.4D, target.getZ(),
                        18, 0.2D, 0.3D, 0.2D, 0.22D);
            }
        }

        if (isDesperate()) {
            target.setSecondsOnFire(4);          // fire aspect, near enough
            if (this.random.nextInt(METEOR_ODDS) == 0) {
                callMeteor(target);
            }
        }
        return hit;
    }

    /**
     * Drops one meteor on whatever he just hit. KraveMeteor already refuses to
     * damage Cayden, Barbara or players, and attributing it to him means the
     * Krave Monster's damage gate does not shrug it off - but the crater still
     * sets fires, which is why the desperate state carries fire resistance.
     */
    private void callMeteor(net.minecraft.world.entity.Entity target) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        com.barbarajones.entity.KraveMeteor m =
                com.barbarajones.content.ModEntities.METEOR.get().create(sl);
        if (m == null) {
            return;
        }
        double ox = (this.random.nextDouble() - 0.5D) * 3.0D;
        double oz = (this.random.nextDouble() - 0.5D) * 3.0D;

        // Drop from as high as there is actually room for. Spawning at a fixed
        // 34 blocks put the meteor inside the rock whenever the fight was in a
        // cave or indoors, where it collided on its first tick and "landed"
        // somewhere nobody could see. Walk up from the target instead and stop
        // under whatever ceiling is there.
        BlockPos scan = BlockPos.containing(target.getX() + ox, target.getY() + 1.0D, target.getZ() + oz);
        int clear = 0;
        for (int i = 0; i < 34; i++) {
            BlockPos above = scan.above(i + 1);
            if (!sl.getBlockState(above).isAir()) {
                break;
            }
            clear = i + 1;
        }
        if (clear < 3) {
            return;   // pinned against a low ceiling: no room for a meteor at all
        }

        double spawnY = target.getY() + clear;
        m.saiyanStrike(this);
        m.setPos(target.getX() + ox, spawnY, target.getZ() + oz);
        m.aim(-ox * 0.06D, -oz * 0.06D);
        sl.addFreshEntity(m);

        // Telegraph it, so the strike reads as his doing rather than random noise.
        playSound(ModSounds.KRAVE_SCREECH.get(), 1.2F, 1.5F);
        sl.sendParticles(ParticleTypes.SMALL_FLAME,
                target.getX(), target.getY() + target.getBbHeight() + 0.4D, target.getZ(),
                14, 0.35D, 0.2D, 0.35D, 0.02D);

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        AscensionLadder.Rung rung = AscensionLadder.rung(getTier());
        // Ultra Instinct: the body answers before he does. Most incoming damage
        // is simply not there when it lands, and he slides out of the way. Blue
        // does a lesser version of the same trick; everything below just tanks.
        if (rung.dodgePercent() > 0 && !level().isClientSide
                && this.random.nextInt(100) < rung.dodgePercent()
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel sl) {
                Vec3 side = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
                double len = Math.max(0.2D, side.length());
                setDeltaMovement(getDeltaMovement().add(side.scale(0.55D / len)).add(0.0D, 0.18D, 0.0D));
                this.hurtMarked = true;
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        10, 0.3D, 0.4D, 0.3D, 0.12D);
                sl.playSound(null, blockPosition(), ModSounds.KRAVE_SCREECH.get(), getSoundSource(), 0.7F, 2.0F);
            }
            this.dodgeFlash = 6;
            return false;
        }
        // Ascended, he is untouchable for the length of a boss fight - but ONLY
        // while he can match what he is fighting. That invulnerability used to
        // be unconditional, which is the real reason the Krave Monster could
        // never kill him whatever form it wore. It is now the reward for having
        // earned the right form: walk into Form 3 without Super Saiyan 3 and he
        // is as mortal as anybody.
        int shortfall = shortfall();
        if (isSuperSaiyan() && this.ssjUntilBossDies && !isMortallyOutmatched()) {
            return false;
        }
        // 30 seconds of total immunity after he claws his way back out. He respawns
        // mid-apocalypse - in the air, next to fire, inside the tornado - and used to
        // immediately splatter on landing and trigger the whole thing over again.
        if (this.graceTicks > 0) {
            return false;
        }
        // the apocalypse's own fire/blasts can't kill him (no death loops)
        if (isTame() && (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE))
                && com.barbarajones.apocalypse.KraveApocalypse.isActiveNear(level(), position())) {
            return false;
        }
        // The divine forms do not dodge - they simply refuse most of what lands.
        // Applied as a multiplier rather than a resistance effect so it stacks
        // predictably with the desperation resistance he may already be holding.
        float taken = (float) (amount * rung.damageTaken());
        if (shortfall > 0) {
            // Outclassed, everything lands harder. This is most of what makes an
            // unmatched form actually WIN rather than merely take longer to lose to.
            taken *= outmatchedTakenScale();
        }
        return super.hurt(source, taken);
    }

    /** Make him untouchable for a while (used on post-death respawn). */
    public void grantGrace(int ticks) {
        this.graceTicks = Math.max(this.graceTicks, ticks);
        this.invulnerableTime = 20;
        setInvulnerable(true);
    }

    public boolean hasGrace() {
        return this.graceTicks > 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CAYDEN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CAYDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CAYDEN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KraveFed", getKraveFed());
        tag.putBoolean("KraveRage", isRageUnlocked());
        tag.putBoolean("Ssj", isSuperSaiyan());
        tag.putInt("SsjTicks", this.ssjTicks);
        tag.putBoolean("SsjBossLinked", this.ssjUntilBossDies);
        tag.putInt("AscensionUnlocks", getUnlockMask());
        tag.putInt("AscensionKi", getKi());
        if (this.home != null) {
            tag.putInt("HomeX", this.home.getX());
            tag.putInt("HomeY", this.home.getY());
            tag.putInt("HomeZ", this.home.getZ());
            tag.putString("HomeDim", this.homeDim);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FED, tag.getInt("KraveFed"));
        this.entityData.set(RAGE, tag.getBoolean("KraveRage"));
        // Masked to the rungs that actually exist, so a save written by a future
        // wider ladder cannot hand this build a tier it has no stats for.
        this.entityData.set(UNLOCKS,
                tag.getInt("AscensionUnlocks") & ((1 << (AscensionLadder.MAX + 1)) - 1));
        this.entityData.set(KI, Math.max(0, tag.getInt("AscensionKi")));
        if (tag.getBoolean("Ssj")) {
            this.entityData.set(SSJ, true);
            this.ssjTicks = Math.max(1, tag.getInt("SsjTicks"));
            this.ssjUntilBossDies = tag.getBoolean("SsjBossLinked");
            setNoGravity(true);
        }
        if (tag.contains("HomeX")) {
            this.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            this.homeDim = tag.getString("HomeDim");
            if (this.homeDim.isEmpty()) {
                this.homeDim = level().dimension().location().toString();
            }
            this.entityData.set(HOUSED, true);
        }
        applyKraveStats();
    }

    /**
     * Only relevant transformed and locked onto Krave Monster: flies toward him,
     * firing Krave Lasers at range and switching to melee once close. This is
     * intentionally the mod's only way to actually hurt Krave Monster during the
     * fight (see KraveMonster.hurt()'s damage gating) - the player's job is the
     * minions and healing boxes, not this fight directly.
     */
    static class SsjFlyAttackGoal extends Goal {
        private static final double RANGE = 5.0D;
        private final CaydenCobb cayden;
        private int laserCooldown;
        private int meleeCooldown;

        SsjFlyAttackGoal(CaydenCobb cayden) {
            this.cayden = cayden;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Any target, not just the Krave Monster. This is his only goal that
            // can engage something airborne - gating it to one boss left him with
            // no behaviour whatsoever against a Wither, since MeleeAttackGoal
            // cannot path to a flying entity.
            LivingEntity target = this.cayden.getTarget();
            return this.cayden.isSuperSaiyan() && target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            if (this.cayden.getTarget() instanceof KraveMonster boss
                    && this.cayden.level() instanceof ServerLevel sl) {
                com.barbarajones.apocalypse.KraveKosmosBattle.start(sl, boss, this.cayden);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.cayden.getTarget();
            if (target == null) {
                return;
            }
            this.cayden.getLookControl().setLookAt(target, 30.0F, 30.0F);

            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(this.cayden.position().add(0.0D, this.cayden.getBbHeight() * 0.5D, 0.0D));
            double dist = to.length();

            if (dist > RANGE) {
                Vec3 dir = to.scale(1.0D / Math.max(dist, 0.01D));
                this.cayden.setDeltaMovement(dir.scale(0.5D));
                if (--this.laserCooldown <= 0) {
                    this.laserCooldown = 25;
                    fireLaser(target);
                }
            } else {
                this.cayden.setDeltaMovement(this.cayden.getDeltaMovement().scale(0.6D));
                if (--this.meleeCooldown <= 0) {
                    this.meleeCooldown = 15;
                    this.cayden.doHurtTarget(target);
                }
            }
        }

        private void fireLaser(LivingEntity target) {
            if (!(this.cayden.level() instanceof ServerLevel)) {
                return;
            }
            Vec3 from = this.cayden.position().add(0.0D, this.cayden.getBbHeight() * 0.6D, 0.0D);
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            KraveLaser laser = new KraveLaser(this.cayden.level(), this.cayden, from, aim);
            this.cayden.level().addFreshEntity(laser);
            this.cayden.level().playSound(null, this.cayden.blockPosition(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 1.6F);
        }
    }

    /** Follow the owner when unhoused; stay near home when housed. */
    static class CaydenFollowOrHomeGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final CaydenCobb cayden;

        CaydenFollowOrHomeGoal(CaydenCobb cayden) {
            this.cayden = cayden;
        }

        @Override
        public boolean canUse() {
            return this.cayden.getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos home = this.cayden.getHome();
            if (home != null && this.cayden.homeIsInThisDimension()) {
                if (this.cayden.blockPosition().distSqr(home) > HOME_LEASH * HOME_LEASH) {
                    this.cayden.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D);
                }
                return;
            }
            LivingEntity owner = this.cayden.getOwner();
            if (owner != null && this.cayden.distanceToSqr(owner) > 36.0D
                    && this.cayden.distanceToSqr(owner) < 1024.0D) {
                this.cayden.getNavigation().moveTo(owner, 1.1D);
            }
        }
    }

    // ---- being outclassed ---------------------------------------------------
    //
    // Rule #1 of this mod is that Cayden must not die, and every other system
    // here defends that. This is the deliberate exception: against the Krave
    // Monster he is only safe while he can MATCH the form in front of him. Meet
    // Form 3 without Super Saiyan 3 and the fight is still his to lose, but it
    // is genuinely losable.
    //
    // Indexed by how many tiers short he is. One tier short is a real fight he
    // narrowly drops; two is a beating; three or more is a formality. The point
    // is that the first one FEELS close - a stomp teaches nothing, whereas
    // losing at 10% health teaches you exactly which form you still need.
    private static final float[] OUTMATCHED_DEALT = { 1.00F, 0.55F, 0.30F, 0.15F };
    private static final float[] OUTMATCHED_TAKEN = { 1.00F, 1.60F, 2.20F, 3.00F };

    /**
     * The floor, as a fraction of the boss's max health, that an outclassed
     * Cayden cannot punch through. He can take Form 3 to a sliver at one tier
     * short - and then not finish it. Without this he still eventually grinds
     * any form down, and "he cannot beat it" becomes "it takes him longer".
     */
    private static final float[] OUTMATCHED_FLOOR = { 0.00F, 0.10F, 0.32F, 0.55F };

    /** How many ascension tiers short he is for the fight he is in. 0 is fair. */
    public int shortfall() {
        LivingEntity foe = getTarget();
        if (foe == null || !foe.isAlive()) {
            return 0;
        }
        return Math.max(0, demandFor(foe) - highestUnlockedTier());
    }

    /** True when the thing he is fighting is above anything he has been taught. */
    public boolean isOutmatched() {
        return shortfall() > 0;
    }

    private static float band(float[] table, int shortfall) {
        return table[Math.max(0, Math.min(table.length - 1, shortfall))];
    }

    /** Multiplier on damage he DEALS while outclassed. */
    public float outmatchedDealtScale() {
        return band(OUTMATCHED_DEALT, shortfall());
    }

    /** Multiplier on damage he TAKES while outclassed. */
    public float outmatchedTakenScale() {
        return band(OUTMATCHED_TAKEN, shortfall());
    }

    /** Fraction of a boss's max health he cannot get it below while outclassed. */
    public float outmatchedFloor() {
        return band(OUTMATCHED_FLOOR, shortfall());
    }

    /**
     * Whether being outclassed can actually get him KILLED, as opposed to merely
     * beaten up.
     *
     * <p>Deliberately narrower than {@link #isOutmatched()}. The brief was that
     * the Krave Monster should be able to kill him if he cannot match its form,
     * and that is a fight the player chooses, walks into, and can walk out of.
     * The outage boss is not: it arrives on a random timer, and now demands Super
     * Saiyan Blue, so a universal rule would mean most players lose Cayden to an
     * event they never opted into. Rule #1 gets exactly one exception, and it is
     * the one that was asked for.
     */
    public boolean isMortallyOutmatched() {
        return isOutmatched() && getTarget() instanceof KraveMonster;
    }

    /**
     * The void does not get to have him.
     *
     * <p>Rule #1 is that Cayden must not die, and the void was the one death
     * that slipped past every guard protecting it. The Kosmos is floating
     * islands, so falling off is not an edge case there, it is Tuesday - and an
     * out-of-world death is uniquely unrecoverable, because it happens hundreds
     * of blocks below the player. The respawn only fires when the owner is
     * within 48 blocks, so he would fall, die at y=-337, and simply never come
     * back, with no cutscene and no body.
     *
     * <p>So he is caught instead of killed: put back on his owner's head, at
     * half health, with his fall reset. Half health rather than full because
     * falling off the world should cost something - it just should not cost him.
     */
    @Override
    protected void onBelowWorld() {
        if (!level().isClientSide && isTame() && getOwner() instanceof Player owner
                && owner.level() == level()) {
            teleportTo(owner.getX(), owner.getY() + 1.0D, owner.getZ());
            setDeltaMovement(Vec3.ZERO);
            this.fallDistance = 0.0F;
            // Clearing this matters: without it he banks the whole fall and
            // splatters the instant he lands next to you.
            setHealth(Math.max(1.0F, getMaxHealth() * 0.5F));
            hurtMarked = true;
            owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    net.minecraft.ChatFormatting.YELLOW
                    + "Cayden went off the edge. You have him back. Watch him."));
            playSound(ModSounds.CAYDEN_HURT.get(), 1.0F, 1.2F);
            return;
        }
        super.onBelowWorld();
    }
    // ---- Ultra Instinct: the vortex ----------------------------------------

    /** How far out the vortex reaches while he is at Ultra Instinct. */
    private static final double VORTEX_RADIUS = 14.0D;

    /**
     * While he is at Ultra Instinct, everything around him is airborne.
     *
     * <p>Not a transformation burst - this runs for as long as he holds the
     * form. Mobs, items and anything else nearby get lifted off the ground and
     * swept around him, with lightning arcing HORIZONTALLY through them at his
     * own chest height rather than dropping from the sky, so the storm reads as
     * something coming out of him rather than weather happening above him.
     *
     * <p>Players are exempt, deliberately and without exception - including the
     * owner. A permanent effect that throws you around whenever your own
     * companion powers up would make him unplayable to stand near, and the
     * whole point is that you get to walk up and look at it.
     */
    private void tickUltraVortex() {
        if (getTier() < AscensionLadder.ULTRA || !(level() instanceof ServerLevel sl)) {
            return;
        }
        double cx = getX();
        double cy = getY();
        double cz = getZ();

        for (Entity caught : sl.getEntities(this,
                getBoundingBox().inflate(VORTEX_RADIUS, VORTEX_RADIUS * 0.6D, VORTEX_RADIUS))) {
            // Players are never caught. Neither is the pair of them - Barbara
            // being flung around by Cayden is a different joke and not this one.
            if (caught instanceof Player || caught instanceof CaydenCobb
                    || caught instanceof BarbaraJones) {
                continue;
            }
            double dx = caught.getX() - cx;
            double dz = caught.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > VORTEX_RADIUS) {
                continue;
            }

            // Tangent, not a push: crossing the radius with up gives the
            // sideways component that makes this orbit instead of scatter.
            double nx = dist < 0.01D ? 1.0D : dx / dist;
            double nz = dist < 0.01D ? 0.0D : dz / dist;
            double spinX = -nz;
            double spinZ = nx;

            // Pulled inward as well as around, or the orbit decays outward and
            // everything slowly leaves the tornado it is supposed to be in.
            double pull = 0.10D;
            double spin = 0.42D;
            double lift = caught.getY() < cy + 7.0D ? 0.34D : 0.02D;

            caught.setDeltaMovement(
                    spinX * spin - nx * pull,
                    Math.min(0.85D, caught.getDeltaMovement().y + lift),
                    spinZ * spin - nz * pull);
            // Without this the client never sees the launch - the server moves
            // it and the client quietly interpolates it back down.
            caught.hurtMarked = true;
            caught.fallDistance = 0.0F;

            // Horizontal lightning: an arc from his chest straight out through
            // whatever is being flung, drawn as a line of sparks rather than a
            // LightningBolt entity, because a bolt is always vertical and
            // always strikes the ground.
            if (this.tickCount % 5 == 0 && this.random.nextInt(3) == 0) {
                arcThrough(sl, caught);
            }
        }

        if (this.tickCount % 3 == 0) {
            vortexWall(sl);
        }
        if (this.tickCount % 40 == 0) {
            sl.playSound(null, blockPosition(), ModSounds.KRAVE_TORNADO.get(),
                    getSoundSource(), 0.7F, 1.4F);
        }
    }

    /** A horizontal bolt from his chest out through one caught entity. */
    private void arcThrough(ServerLevel sl, Entity target) {
        double y = getY() + getBbHeight() * 0.62D;
        Vec3 from = new Vec3(getX(), y, getZ());
        // Aimed level with his chest rather than at the target's own height, so
        // every arc in the storm sits on one plane and reads as a sheet.
        Vec3 to = new Vec3(target.getX(), y, target.getZ());
        Vec3 step = to.subtract(from);
        int points = 14;
        for (int i = 0; i <= points; i++) {
            double f = i / (double) points;
            // Jitter perpendicular to the run so it forks like lightning instead
            // of drawing a clean laser.
            double wobble = (this.random.nextDouble() - 0.5D) * 0.55D;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    from.x + step.x * f + wobble,
                    y + (this.random.nextDouble() - 0.5D) * 0.35D,
                    from.z + step.z * f + wobble,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        sl.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                3, 0.2D, 0.2D, 0.2D, 0.01D);
    }

    /** The turning wall of debris that makes the vortex visible from outside. */
    private void vortexWall(ServerLevel sl) {
        double spin = this.tickCount * 0.35D;
        for (int i = 0; i < 10; i++) {
            double a = spin + (i / 10.0D) * Math.PI * 2.0D;
            // Narrower at the bottom, wider at the top: a funnel, not a cylinder.
            double h = (i / 10.0D) * 9.0D;
            double r = 2.5D + h * 0.55D;
            sl.sendParticles(ParticleTypes.CLOUD,
                    getX() + Math.cos(a) * r, getY() + h, getZ() + Math.sin(a) * r,
                    1, 0.0D, 0.0D, 0.0D, 0.02D);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    getX() + Math.cos(a + 0.6D) * r, getY() + h, getZ() + Math.sin(a + 0.6D) * r,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
