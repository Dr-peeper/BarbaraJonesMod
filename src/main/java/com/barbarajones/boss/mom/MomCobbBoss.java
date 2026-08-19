package com.barbarajones.boss.mom;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * MOM COBB, the boss. Not the wandering {@code entity/MomCobb} NPC - this is the
 * three-act fight version, the woman who takes the Krave away.
 *
 * <p>The fight runs on {@link MomPhase}, driven by her health and only ever
 * moving forward:
 * <ol>
 *   <li><b>"Where have you been?"</b> - she lobs household objects
 *       ({@link ThrownHousehold}) at you on a slow arc.</li>
 *   <li><b>"GET OFF THAT GAME"</b> - she kills the lights in an area (blindness
 *       plus heavy slowness for as long as you stand in it) and stops caring
 *       about you entirely: she goes for Cayden. Standing physically between the
 *       two of them makes her hit you instead, and staggers her.</li>
 *   <li><b>"I'M TAKING THE KRAVE"</b> - she surrounds herself with confiscated
 *       {@link MomKraveStash} boxes and eats them for large heals. Smash them.
 *       Smash one while she is mid-feed and she comes up empty and reels.</li>
 * </ol>
 *
 * <p><b>Every</b> attack she has is telegraphed: a wind-up with its own length,
 * its own sound, its own particle cue and its own pose in
 * {@code MomCobbBossModel}. She is rooted in place for the whole wind-up, so
 * each one is a real window rather than a dice roll. That is also why she has no
 * {@code MeleeAttackGoal} - vanilla's melee has no tell, so her close-range
 * attack is a short telegraphed swipe instead.
 *
 * <p><b>Cayden's death is not hers to swallow.</b> Every point of damage she
 * deals goes through the ordinary {@code hurt()} path with this entity as the
 * damage source's owner, and nothing in this class cancels, delays or
 * special-cases his death. If she kills him, {@code EventHandler.onDeath} sees
 * it exactly as it sees any other kill and the Krave Apocalypse fires as normal.
 */
public class MomCobbBoss extends Monster {

    // ---- wind-up kinds (also read by the client model) ----------------------

    public static final int WINDUP_NONE = 0;
    /** Short close-range tell: hand up, then a cone swipe. */
    public static final int WINDUP_SWIPE = 1;
    /** Long tell: object cocked back over the shoulder, then a lob. */
    public static final int WINDUP_THROW = 2;
    /** Longest tell: both arms overhead, then the lights go out. */
    public static final int WINDUP_BLACKOUT = 3;
    /** Hunched over a Krave box, then a big heal. */
    public static final int WINDUP_DEVOUR = 4;

    private static final int SWIPE_WINDUP = 15;
    private static final int THROW_WINDUP = 26;
    private static final int BLACKOUT_WINDUP = 38;
    private static final int DEVOUR_WINDUP = 32;

    /** Client-safe: the model needs the same numbers to normalise its poses. */
    public static int windupLength(int kind) {
        return switch (kind) {
            case WINDUP_SWIPE -> SWIPE_WINDUP;
            case WINDUP_THROW -> THROW_WINDUP;
            case WINDUP_BLACKOUT -> BLACKOUT_WINDUP;
            case WINDUP_DEVOUR -> DEVOUR_WINDUP;
            default -> 1;
        };
    }

    // ---- tuning -------------------------------------------------------------

    private static final float MAX_HP = 220.0F;
    private static final float SWIPE_REACH = 3.4F;
    private static final float THROW_MIN_RANGE = 5.0F;
    private static final float THROW_MAX_RANGE = 26.0F;
    private static final double BLACKOUT_RADIUS = 9.0D;
    private static final int BLACKOUT_DURATION = 240;
    private static final int BLACKOUT_COOLDOWN = 340;
    private static final float DEVOUR_HEAL = 26.0F;
    private static final double DEVOUR_REACH = 3.2D;
    /** How many boxes she can ever put down across one fight. Hard cap, so it ends. */
    private static final int STASH_BUDGET = 7;
    private static final int STASH_TOPUP_INTERVAL = 320;
    private static final int MAX_LIVE_STASHES = 2;
    /** Half a metre of slack either side of the Mom-to-Cayden line counts as blocking. */
    private static final double BODY_BLOCK_RADIUS = 1.7D;

    // ---- synched state ------------------------------------------------------

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(MomCobbBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP =
            SynchedEntityData.defineId(MomCobbBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_LEFT =
            SynchedEntityData.defineId(MomCobbBoss.class, EntityDataSerializers.INT);
    /** Synced because the reeling pose is the client's cue that the whiff worked. */
    private static final EntityDataAccessor<Boolean> DATA_STAGGERED =
            SynchedEntityData.defineId(MomCobbBoss.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            MomPhase.QUESTIONS.barTitle(), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS);

    // ---- server-side fight state -------------------------------------------

    private MomPhase phase = MomPhase.QUESTIONS;
    private int windupKind = WINDUP_NONE;
    private int windupLeft;
    @Nullable
    private LivingEntity windupFocus;
    @Nullable
    private MomKraveStash windupStash;

    private int actionCooldown = 60;
    private int blackoutCooldown = 120;
    private int staggerTicks;
    private int tauntTimer = 200;
    private int tauntIndex;

    @Nullable
    private Vec3 blackoutCenter;
    private int blackoutTicks;

    private int stashBudget = STASH_BUDGET;
    private int stashTopupTimer = STASH_TOPUP_INTERVAL;

    /** The Cayden she is currently hunting, so she can react when he goes down. */
    @Nullable
    private CaydenCobb quarry;

    public MomCobbBoss(EntityType<? extends MomCobbBoss> type, Level level) {
        super(type, level);
        this.xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HP)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                // doHurtTarget() reads this; a melee mob without it crashes the server
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, MomPhase.QUESTIONS.ordinal());
        this.entityData.define(DATA_WINDUP, WINDUP_NONE);
        this.entityData.define(DATA_WINDUP_LEFT, 0);
        this.entityData.define(DATA_STAGGERED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Eating beats hunting beats advancing: all three claim MOVE, so the
        // ordering here IS the priority list for what she cares about right now.
        this.goalSelector.addGoal(1, new SeekStashGoal(this));
        this.goalSelector.addGoal(2, new HuntCaydenGoal(this));
        this.goalSelector.addGoal(3, new ScoldingAdvanceGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- client-readable accessors -----------------------------------------

    public MomPhase getPhase() {
        return MomPhase.byOrdinal(this.entityData.get(DATA_PHASE));
    }

    public int getWindupKind() {
        return this.entityData.get(DATA_WINDUP);
    }

    /** 0 at the start of the tell, 1 the instant it lands. Safe on either side. */
    public float getWindupProgress() {
        int kind = getWindupKind();
        if (kind == WINDUP_NONE) {
            return 0.0F;
        }
        float length = windupLength(kind);
        return Mth.clamp(1.0F - this.entityData.get(DATA_WINDUP_LEFT) / length, 0.0F, 1.0F);
    }

    public boolean isWindingUp() {
        return getWindupKind() != WINDUP_NONE;
    }

    /** Drives the reeling pose after she whiffs a feed or gets body-blocked. */
    public boolean isStaggered() {
        return this.entityData.get(DATA_STAGGERED);
    }

    // ---- boss bar -----------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ---- tick ---------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        this.bossEvent.setProgress(getHealth() / getMaxHealth());
        updatePhase();
        tickBlackout();
        tickTaunts();
        tickQuarryWatch();

        if (this.blackoutCooldown > 0) {
            this.blackoutCooldown--;
        }
        if (this.phase == MomPhase.KRAVE && --this.stashTopupTimer <= 0) {
            this.stashTopupTimer = STASH_TOPUP_INTERVAL;
            topUpStash();
        }

        if (this.staggerTicks > 0) {
            tickStagger();
            return;
        }
        if (this.windupLeft > 0) {
            tickWindup();
            return;
        }
        if (this.actionCooldown > 0) {
            this.actionCooldown--;
            return;
        }
        chooseAction();
    }

    private void updatePhase() {
        MomPhase want = MomPhase.forHealth(getHealth() / getMaxHealth());
        if (want.ordinal() <= this.phase.ordinal()) {
            return;   // forward only - see MomPhase's javadoc
        }
        this.phase = want;
        this.entityData.set(DATA_PHASE, want.ordinal());
        this.bossEvent.setName(want.barTitle());
        this.bossEvent.setColor(want.barColor());
        clearWindup();
        this.staggerTicks = 0;
        this.entityData.set(DATA_STAGGERED, false);
        this.actionCooldown = 45;   // a beat to read the new bar before she swings again

        announce(want.entryLine());
        playSound(ModSounds.BARBARA_RAGE.get(), 1.5F, want == MomPhase.KRAVE ? 0.85F : 1.0F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.2D, getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 0.6D, getZ(), 40, 1.2D, 0.8D, 1.2D, 0.03D);
        }

        if (want == MomPhase.GAME) {
            this.bossEvent.setDarkenScreen(true);
            this.blackoutCooldown = 40;
        } else if (want == MomPhase.KRAVE) {
            this.bossEvent.setCreateWorldFog(true);
            announce(ChatFormatting.GOLD + "Smash her boxes. She heals off every one she gets down.");
            for (int i = 0; i < 3; i++) {
                spawnStash();
            }
            this.stashTopupTimer = STASH_TOPUP_INTERVAL;
        }
    }

    private void tickTaunts() {
        if (--this.tauntTimer > 0) {
            return;
        }
        this.tauntTimer = 220 + this.random.nextInt(160);
        announce(this.phase.taunt(this.tauntIndex++));
        playSound(ModSounds.BARBARA_IDLE.get(), 1.1F, 0.95F);
    }

    /**
     * She reacts to Cayden going down - and does nothing else about it. The kill
     * already went through the vanilla death path, so the apocalypse is already
     * on its way; this is a line, not an intervention.
     */
    private void tickQuarryWatch() {
        if (this.quarry == null) {
            return;
        }
        if (!this.quarry.isAlive()) {
            announce(ChatFormatting.DARK_RED + "Mom Cobb: \"...Cayden? Cayden, get UP.\"");
            this.quarry = null;
        } else if (distanceToSqr(this.quarry) > 48.0D * 48.0D) {
            this.quarry = null;
        }
    }

    private void tickStagger() {
        if (--this.staggerTicks <= 0) {
            this.entityData.set(DATA_STAGGERED, false);
        }
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (level() instanceof ServerLevel sl && this.staggerTicks % 3 == 0) {
            sl.sendParticles(ParticleTypes.CRIT,
                    getX(), getY() + getBbHeight() * 0.9D, getZ(), 3, 0.3D, 0.2D, 0.3D, 0.02D);
        }
    }

    // ---- wind-ups -----------------------------------------------------------

    private boolean isBusy() {
        return this.windupLeft > 0 || this.staggerTicks > 0;
    }

    /** @return true if the tell actually started. */
    private boolean beginWindup(int kind, @Nullable LivingEntity focus) {
        if (isBusy()) {
            return false;
        }
        this.windupKind = kind;
        this.windupLeft = windupLength(kind);
        this.windupFocus = focus;
        this.entityData.set(DATA_WINDUP, kind);
        this.entityData.set(DATA_WINDUP_LEFT, this.windupLeft);
        windupStartCue(kind);
        return true;
    }

    private void clearWindup() {
        this.windupKind = WINDUP_NONE;
        this.windupLeft = 0;
        this.windupFocus = null;
        this.windupStash = null;
        this.entityData.set(DATA_WINDUP, WINDUP_NONE);
        this.entityData.set(DATA_WINDUP_LEFT, 0);
    }

    private void windupStartCue(int kind) {
        switch (kind) {
            case WINDUP_SWIPE -> playSound(SoundEvents.VILLAGER_NO, 1.3F, 0.75F);
            case WINDUP_THROW -> {
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.1F, 1.35F);
                announce(ChatFormatting.RED + "Mom Cobb picks something up.");
            }
            case WINDUP_BLACKOUT -> {
                playSound(SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.55F);
                announce(ChatFormatting.DARK_AQUA + "Mom Cobb reaches for the breaker.");
            }
            case WINDUP_DEVOUR -> {
                playSound(SoundEvents.BREWING_STAND_BREW, 1.3F, 0.65F);
                announce(ChatFormatting.GOLD + "Mom Cobb is getting into a box. BREAK IT.");
            }
            default -> { }
        }
    }

    private void tickWindup() {
        // Rooted for the whole tell. A telegraph you can't act on is decoration.
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        if (this.windupFocus != null && this.windupFocus.isAlive()) {
            getLookControl().setLookAt(this.windupFocus, 30.0F, 30.0F);
        } else if (this.windupStash != null && this.windupStash.isAlive()) {
            getLookControl().setLookAt(this.windupStash, 30.0F, 30.0F);
        }
        windupParticles();

        this.windupLeft--;
        this.entityData.set(DATA_WINDUP_LEFT, this.windupLeft);
        if (this.windupLeft > 0) {
            return;
        }
        int kind = this.windupKind;
        LivingEntity focus = this.windupFocus;
        MomKraveStash stash = this.windupStash;
        clearWindup();
        switch (kind) {
            case WINDUP_SWIPE -> releaseSwipe();
            case WINDUP_THROW -> releaseThrow(focus);
            case WINDUP_BLACKOUT -> releaseBlackout(focus);
            case WINDUP_DEVOUR -> releaseDevour(stash);
            default -> { }
        }
    }

    private void windupParticles() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        double handY = getY() + getBbHeight() * 1.05D;
        Vec3 ahead = position().add(getLookAngle().scale(0.6D));
        switch (this.windupKind) {
            case WINDUP_SWIPE -> sl.sendParticles(ParticleTypes.CRIT,
                    ahead.x, handY, ahead.z, 2, 0.2D, 0.15D, 0.2D, 0.02D);
            case WINDUP_THROW -> {
                sl.sendParticles(ParticleTypes.CRIT, getX(), handY, getZ(), 2, 0.3D, 0.2D, 0.3D, 0.03D);
                sl.sendParticles(ParticleTypes.SMOKE, getX(), handY, getZ(), 1, 0.25D, 0.1D, 0.25D, 0.0D);
            }
            case WINDUP_BLACKOUT -> {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                        getX(), handY, getZ(), 3, 0.45D, 0.4D, 0.45D, 0.02D);
                sl.sendParticles(ParticleTypes.PORTAL,
                        getX(), getY() + 0.2D, getZ(), 4, 0.6D, 0.1D, 0.6D, 0.35D);
            }
            case WINDUP_DEVOUR -> {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        ahead.x, getY() + 1.1D, ahead.z, 2, 0.3D, 0.3D, 0.3D, 0.0D);
                sl.sendParticles(ParticleTypes.SMOKE,
                        ahead.x, getY() + 1.3D, ahead.z, 1, 0.2D, 0.2D, 0.2D, 0.01D);
            }
            default -> { }
        }
    }

    // ---- action selection ---------------------------------------------------

    private void chooseAction() {
        LivingEntity target = getTarget();
        if (this.phase == MomPhase.GAME && this.blackoutTicks <= 0 && this.blackoutCooldown <= 0) {
            if (beginWindup(WINDUP_BLACKOUT, target)) {
                return;
            }
        }
        if (canLobAt(target)) {
            beginWindup(WINDUP_THROW, target);   // releaseThrow() sets the real cooldown
        }
    }

    private boolean canLobAt(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        double d = distanceTo(target);
        return d > THROW_MIN_RANGE && d < THROW_MAX_RANGE && getSensing().hasLineOfSight(target);
    }

    // ---- attack releases ----------------------------------------------------

    private void releaseSwipe() {
        this.actionCooldown = 25;
        Vec3 look = getLookAngle();
        List<LivingEntity> hits = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(SWIPE_REACH, 1.6D, SWIPE_REACH),
                e -> e != this && e.isAlive() && !(e instanceof MomKraveStash));
        boolean landed = false;
        for (LivingEntity victim : hits) {
            Vec3 to = victim.position().subtract(position()).normalize();
            if (look.x * to.x + look.z * to.z < 0.15D) {
                continue;   // behind her; the swipe is a cone, not a nova
            }
            // doHurtTarget applies ATTACK_DAMAGE and ATTACK_KNOCKBACK and uses
            // damageSources().mobAttack(this) - the ordinary path, deliberately.
            landed |= doHurtTarget(victim);
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        if (level() instanceof ServerLevel sl) {
            Vec3 at = position().add(look.scale(1.4D));
            sl.sendParticles(landed ? ParticleTypes.CRIT : ParticleTypes.SMOKE,
                    at.x, getY() + 1.1D, at.z, 12, 0.7D, 0.35D, 0.7D, 0.08D);
        }
    }

    private void releaseThrow(@Nullable LivingEntity focus) {
        this.actionCooldown = 55 + this.random.nextInt(35);
        if (focus == null || !focus.isAlive() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 from = position()
                .add(0.0D, getBbHeight() * 0.9D, 0.0D)
                .add(getLookAngle().scale(0.45D));
        // Lead a moving target so walking sideways isn't a free dodge; the lead is
        // deliberately short so sprinting still beats it.
        Vec3 aim = focus.position()
                .add(focus.getDeltaMovement().scale(4.0D))
                .add(0.0D, focus.getBbHeight() * 0.5D, 0.0D);

        ThrownHousehold object = new ThrownHousehold(
                level(), this, from, aim, this.random.nextInt(ThrownHousehold.KINDS));
        level().addFreshEntity(object);
        announce(ChatFormatting.RED + "Mom Cobb throws " + object.getObjectName() + ".");
        sl.playSound(null, blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.3F, 0.6F);
        sl.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0, 0, 0, 0);
    }

    private void releaseBlackout(@Nullable LivingEntity focus) {
        this.actionCooldown = 50;
        this.blackoutCooldown = BLACKOUT_COOLDOWN;
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        this.blackoutCenter = focus != null && focus.isAlive() ? focus.position() : position();
        this.blackoutTicks = BLACKOUT_DURATION;
        BlockPos at = BlockPos.containing(this.blackoutCenter);
        sl.playSound(null, at, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.42F);
        sl.playSound(null, blockPosition(), ModSounds.KRAVE_SIREN.get(), SoundSource.HOSTILE, 0.9F, 1.1F);
        sl.sendParticles(ParticleTypes.FLASH,
                this.blackoutCenter.x, this.blackoutCenter.y + 1.0D, this.blackoutCenter.z, 1, 0, 0, 0, 0);
        announce(ChatFormatting.DARK_AQUA + "The lights go out. Get out of the dark.");
    }

    private void releaseDevour(@Nullable MomKraveStash stash) {
        this.actionCooldown = 40;
        if (stash != null && stash.isAlive() && distanceToSqr(stash) < 16.0D) {
            heal(DEVOUR_HEAL);
            stash.discard();
            playSound(SoundEvents.GENERIC_EAT, 1.4F, 0.85F);
            playSound(ModSounds.KRAVE_LAUGH.get(), 0.9F, 0.8F);
            announce(ChatFormatting.RED + "Mom Cobb: \"Mmh. That's MINE now.\"");
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getX(), getY() + 1.4D, getZ(), 24, 0.6D, 0.7D, 0.6D, 0.0D);
            }
            return;
        }
        // The box died mid-feed. She comes up with a handful of cardboard, and
        // this is the fight's real damage window - see hurt()'s stagger bonus.
        stagger(60);
        announce(ChatFormatting.GREEN + "Mom Cobb: \"WHERE IS IT? WHO TOOK IT?\"");
        playSound(ModSounds.BARBARA_HURT.get(), 1.4F, 0.7F);
    }

    /** Roots her and leaves her open. Used by the whiffed feed and the body-block. */
    void stagger(int ticks) {
        clearWindup();
        this.staggerTicks = Math.max(this.staggerTicks, ticks);
        this.actionCooldown = Math.max(this.actionCooldown, ticks);
        this.entityData.set(DATA_STAGGERED, true);
    }

    // ---- phase two: hunting Cayden -----------------------------------------

    void noteQuarry(CaydenCobb cayden) {
        this.quarry = cayden;
    }

    /**
     * Her phase-two attack: she goes for Cayden, not for you. If a player is
     * physically standing on the line between them, that player eats it instead
     * and she loses her footing - that is the entire body-block mechanic, so the
     * test is a plain distance-to-segment check rather than raycasting.
     *
     * <p>When nobody blocks, Cayden takes the hit through the ordinary
     * {@code hurt()} call below. Nothing here can save him and nothing here tries
     * to: if that is the blow that kills him, the Krave Apocalypse fires exactly
     * as it would from any other source.
     */
    void grabAt(CaydenCobb cayden) {
        Player blocker = findBlocker(cayden);
        if (blocker != null) {
            blocker.hurt(damageSources().mobAttack(this), 6.0F);
            blocker.knockback(0.9D, getX() - blocker.getX(), getZ() - blocker.getZ());
            announce(ChatFormatting.YELLOW + "Mom Cobb: \"MOVE. This is between me and him.\"");
            playSound(SoundEvents.VILLAGER_NO, 1.4F, 0.8F);
            stagger(35);
            return;
        }
        cayden.hurt(damageSources().mobAttack(this), 5.0F);
        cayden.knockback(0.6D, getX() - cayden.getX(), getZ() - cayden.getZ());
        playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.9F);
        announce(ChatFormatting.RED + "Mom Cobb has hold of Cayden. GET BETWEEN THEM.");
    }

    @Nullable
    private Player findBlocker(LivingEntity cayden) {
        Vec3 from = position();
        Vec3 span = cayden.position().subtract(from);
        double lenSq = span.horizontalDistanceSqr();
        if (lenSq < 1.0E-4D) {
            return null;
        }
        Player best = null;
        double bestAlong = Double.MAX_VALUE;
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(14.0D, 5.0D, 14.0D))) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            Vec3 rel = player.position().subtract(from);
            double along = (rel.x * span.x + rel.z * span.z) / lenSq;
            if (along < 0.0D || along > 1.0D) {
                continue;   // beside or behind one of us, not between
            }
            double offX = rel.x - span.x * along;
            double offZ = rel.z - span.z * along;
            if (offX * offX + offZ * offZ > BODY_BLOCK_RADIUS * BODY_BLOCK_RADIUS) {
                continue;
            }
            if (Math.abs(player.getY() - from.y) > 3.0D) {
                continue;   // standing on a different floor is not standing in the way
            }
            if (along < bestAlong) {
                bestAlong = along;
                best = player;
            }
        }
        return best;
    }

    // ---- phase two: the blackout zone --------------------------------------

    private void tickBlackout() {
        if (this.blackoutTicks <= 0 || this.blackoutCenter == null
                || !(level() instanceof ServerLevel sl)) {
            return;
        }
        this.blackoutTicks--;
        Vec3 c = this.blackoutCenter;

        // A smoke wall on the boundary, so the dead zone is readable from OUTSIDE
        // it - which is the only place you can still see anything.
        if (this.tickCount % 3 == 0) {
            for (int i = 0; i < 12; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0D;
                double r = BLACKOUT_RADIUS * (0.85D + this.random.nextDouble() * 0.15D);
                sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                        c.x + Math.cos(a) * r, c.y + this.random.nextDouble() * 3.5D,
                        c.z + Math.sin(a) * r, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }
        if (this.blackoutTicks % 20 == 0) {
            AABB zone = new AABB(c, c).inflate(BLACKOUT_RADIUS, 5.0D, BLACKOUT_RADIUS);
            for (Player player : sl.getEntitiesOfClass(Player.class, zone)) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                // Re-applied on a short timer instead of one long dose, so walking
                // out of the zone actually gets you out of it within a second.
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 1, false, true));
            }
            sl.playSound(null, BlockPos.containing(c), ModSounds.KRAVE_RUMBLE.get(),
                    SoundSource.HOSTILE, 0.5F, 0.7F);
        }
        if (this.blackoutTicks == 0) {
            this.blackoutCenter = null;
            announce(ChatFormatting.GRAY + "The lights flicker back on.");
        }
    }

    // ---- phase three: the stash --------------------------------------------

    private void topUpStash() {
        if (this.stashBudget <= 0 || countLiveStashes() >= MAX_LIVE_STASHES) {
            return;
        }
        spawnStash();
    }

    private int countLiveStashes() {
        return level().getEntitiesOfClass(MomKraveStash.class,
                getBoundingBox().inflate(32.0D, 12.0D, 32.0D), MomKraveStash::isAlive).size();
    }

    private void spawnStash() {
        if (this.stashBudget <= 0 || !(level() instanceof ServerLevel sl)) {
            return;
        }
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double radius = 5.0D + this.random.nextDouble() * 4.0D;
        double x = getX() + Math.cos(angle) * radius;
        double z = getZ() + Math.sin(angle) * radius;

        // Drop it onto whatever floor is actually under that spot. The Kosmos is
        // floating islands; a fixed Y offset leaves boxes hanging in mid-air.
        BlockPos ground = BlockPos.containing(x, getY() + 2.0D, z);
        for (int i = 0; i < 12 && sl.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }

        MomKraveStash stash = ModEntities.MOM_STASH.get().create(sl);
        if (stash == null) {
            return;
        }
        stash.setPos(x, ground.getY(), z);
        stash.setOwnerBoss(this);
        sl.addFreshEntity(stash);
        this.stashBudget--;

        sl.sendParticles(ParticleTypes.FLASH, x, ground.getY() + 0.5D, z, 1, 0, 0, 0, 0);
        sl.playSound(null, ground, SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    /** Called by a box as it dies, so she can react and you get told it worked. */
    void onStashDestroyed(MomKraveStash stash) {
        if (level().isClientSide) {
            return;
        }
        if (this.windupStash == stash && this.windupLeft > 0) {
            // Interrupted mid-feed: releaseDevour() would have staggered her when
            // the timer ran out, but taking it away NOW is the responsive read.
            stagger(60);
            announce(ChatFormatting.GREEN + "Mom Cobb: \"WHERE IS IT? WHO TOOK IT?\"");
            playSound(ModSounds.BARBARA_HURT.get(), 1.4F, 0.7F);
            return;
        }
        announce(ChatFormatting.GREEN + "Mom Cobb: \"Don't you DARE.\"");
    }

    // ---- damage / death -----------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Punching a staggered boss is supposed to be worth it.
        float applied = this.staggerTicks > 0 ? amount * 1.5F : amount;
        return super.hurt(source, applied);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.bossEvent.removeAllPlayers();
        if (level().isClientSide) {
            return;
        }
        // The boxes were hers. With her gone they are just litter.
        for (MomKraveStash stash : level().getEntitiesOfClass(MomKraveStash.class,
                getBoundingBox().inflate(48.0D, 24.0D, 48.0D))) {
            if (stash.resolveBoss() == this || stash.resolveBoss() == null) {
                stash.discard();
            }
        }
        announce(ChatFormatting.GREEN + "" + ChatFormatting.BOLD + "Mom Cobb: \"...fine. Keep it.\"");
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.MOMS_TV_REMOTE.get()));
        spawnAtLocation(new ItemStack(ModItems.CONFISCATED_KRAVE.get(),
                2 + this.random.nextInt(3) + looting));
        spawnAtLocation(new ItemStack(ModItems.KRAVE_BOX.get(), 1 + this.random.nextInt(2)));
        spawnAtLocation(new ItemStack(ModItems.CHILD_SUPPORT_PAPERS.get()));
        if (this.random.nextInt(4) == 0) {
            spawnAtLocation(new ItemStack(ModItems.ADOPTION_PAPERS.get()));   // she was not joking
        }
    }

    // ---- misc ---------------------------------------------------------------

    private void announce(String message) {
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(48.0D, 24.0D, 48.0D))) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;   // her ambience is the taunt timer, which is not on a 4-second loop
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BARBARA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BARBARA_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MomPhase", this.phase.ordinal());
        tag.putInt("StashBudget", this.stashBudget);
        tag.putInt("BlackoutCooldown", this.blackoutCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phase = MomPhase.byOrdinal(tag.getInt("MomPhase"));
        this.entityData.set(DATA_PHASE, this.phase.ordinal());
        this.bossEvent.setName(this.phase.barTitle());
        this.bossEvent.setColor(this.phase.barColor());
        if (tag.contains("StashBudget")) {
            this.stashBudget = tag.getInt("StashBudget");
        }
        this.blackoutCooldown = tag.getInt("BlackoutCooldown");
        clearWindup();
    }

    // =====================================================================
    // Goals
    // =====================================================================

    /**
     * Closes on whoever she is currently angry at and opens the short swipe tell
     * once she is in reach. Replaces MeleeAttackGoal, which swings with no
     * warning at all.
     */
    static class ScoldingAdvanceGoal extends Goal {

        private final MomCobbBoss mom;
        private int repathTimer;
        private int swipeCooldown;

        ScoldingAdvanceGoal(MomCobbBoss mom) {
            this.mom = mom;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mom.getTarget();
            return target != null && target.isAlive() && !this.mom.isBusy();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mom.getTarget();
            return target != null && target.isAlive() && !this.mom.isBusy();
        }

        @Override
        public void stop() {
            this.mom.getNavigation().stop();
            this.repathTimer = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mom.getTarget();
            if (target == null) {
                return;
            }
            this.mom.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.repathTimer <= 0) {
                this.repathTimer = 10;
                this.mom.getNavigation().moveTo(target, 1.0D);
            }
            if (this.swipeCooldown > 0) {
                this.swipeCooldown--;
                return;
            }
            double reach = SWIPE_REACH + this.mom.getBbWidth() * 0.5D;
            if (this.mom.distanceToSqr(target) <= reach * reach
                    && this.mom.beginWindup(WINDUP_SWIPE, target)) {
                this.swipeCooldown = 40;
            }
        }
    }

    /**
     * Phase two onward: she loses interest in whoever is hitting her and walks at
     * Cayden. The player's only answer is to physically get in the way - see
     * {@link MomCobbBoss#grabAt}.
     */
    static class HuntCaydenGoal extends Goal {

        private final MomCobbBoss mom;
        @Nullable
        private CaydenCobb quarry;
        private int repathTimer;
        private int grabCooldown;

        HuntCaydenGoal(MomCobbBoss mom) {
            this.mom = mom;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mom.getPhase().ordinal() < MomPhase.GAME.ordinal() || this.mom.isBusy()) {
                return false;
            }
            this.quarry = nearestCayden();
            if (this.quarry != null) {
                this.mom.noteQuarry(this.quarry);
            }
            return this.quarry != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.quarry != null && this.quarry.isAlive() && !this.mom.isBusy()
                    && this.mom.getPhase().ordinal() >= MomPhase.GAME.ordinal()
                    && this.mom.distanceToSqr(this.quarry) < 40.0D * 40.0D;
        }

        @Override
        public void stop() {
            this.quarry = null;
            this.mom.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.quarry == null) {
                return;
            }
            this.mom.getLookControl().setLookAt(this.quarry, 30.0F, 30.0F);
            if (--this.repathTimer <= 0) {
                this.repathTimer = 8;
                this.mom.getNavigation().moveTo(this.quarry, 1.15D);
            }
            if (this.grabCooldown > 0) {
                this.grabCooldown--;
                return;
            }
            double reach = 3.0D + this.mom.getBbWidth();
            if (this.mom.distanceToSqr(this.quarry) <= reach * reach) {
                this.grabCooldown = 45;
                this.mom.grabAt(this.quarry);
            }
        }

        @Nullable
        private CaydenCobb nearestCayden() {
            CaydenCobb best = null;
            double closest = Double.MAX_VALUE;
            for (CaydenCobb candidate : this.mom.level().getEntitiesOfClass(CaydenCobb.class,
                    this.mom.getBoundingBox().inflate(34.0D, 14.0D, 34.0D), Entity::isAlive)) {
                double d = this.mom.distanceToSqr(candidate);
                if (d < closest) {
                    closest = d;
                    best = candidate;
                }
            }
            return best;
        }
    }

    /** Phase three: walk to the nearest surviving box and start the feeding tell. */
    static class SeekStashGoal extends Goal {

        private final MomCobbBoss mom;
        @Nullable
        private MomKraveStash box;
        private int repathTimer;

        SeekStashGoal(MomCobbBoss mom) {
            this.mom = mom;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mom.getPhase() != MomPhase.KRAVE || this.mom.isBusy()) {
                return false;
            }
            // No point burning a box at full health - she saves them for when
            // she's actually losing, which is when you least want her to have one.
            if (this.mom.getHealth() > this.mom.getMaxHealth() * 0.95F) {
                return false;
            }
            this.box = nearestBox();
            return this.box != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.box != null && this.box.isAlive() && !this.mom.isBusy()
                    && this.mom.getPhase() == MomPhase.KRAVE;
        }

        @Override
        public void stop() {
            this.box = null;
            this.mom.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.box == null) {
                return;
            }
            this.mom.getLookControl().setLookAt(this.box, 30.0F, 30.0F);
            if (--this.repathTimer <= 0) {
                this.repathTimer = 10;
                this.mom.getNavigation().moveTo(this.box, 1.1D);
            }
            if (this.mom.distanceToSqr(this.box) <= DEVOUR_REACH * DEVOUR_REACH) {
                this.mom.windupStash = this.box;
                if (!this.mom.beginWindup(WINDUP_DEVOUR, null)) {
                    this.mom.windupStash = null;
                }
            }
        }

        @Nullable
        private MomKraveStash nearestBox() {
            MomKraveStash best = null;
            double closest = Double.MAX_VALUE;
            for (MomKraveStash candidate : this.mom.level().getEntitiesOfClass(MomKraveStash.class,
                    this.mom.getBoundingBox().inflate(40.0D, 16.0D, 40.0D), MomKraveStash::isAlive)) {
                double d = this.mom.distanceToSqr(candidate);
                if (d < closest) {
                    closest = d;
                    best = candidate;
                }
            }
            return best;
        }
    }
}
