package com.barbarajones.v2.internet;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * THE INTERNET MANAGER - the contractor from {@link com.barbarajones.cinematic
 * .actor.InternetManagerActor}, given a fight instead of a cutscene.
 *
 * <p>Same read, same equipment: hard hat, hi-vis, headset, a belt of tools he
 * is entirely willing to use on you, and a run of cable that is never just
 * scenery. He is not {@code TheManager} - that one is a faceless suit who
 * relocates you with paperwork. This one has a face, a job, and a genuine
 * commitment to making your connection worse before it gets better.
 *
 * <ol>
 *   <li><b>CONNECTING...</b> - cable-whip melee plus PACKET LOSS windows,
 *       where a chunk of your hits on him simply do not register.</li>
 *   <li><b>BUFFERING...</b> - adds BUFFERING itself: he goes invulnerable
 *       behind a loading ring (reusing the windup telegraph as the ring's
 *       fill) and LATENCY, which rubber-bands whoever it lands on. Damage him
 *       hard enough while the ring is filling and it shatters early; let it
 *       finish and he unloads a THROTTLE pulse the instant it completes.</li>
 *   <li><b>503 SERVICE UNAVAILABLE</b> - every ability in rotation, shorter
 *       cooldowns, and THROTTLE zones on a much shorter fuse.</li>
 * </ol>
 *
 * <p>Every special move runs through {@link #beginWindup} exactly like
 * {@code TheManager}: rooted, telegraphed, and - except for BUFFERING, which
 * is telegraph and ability at once - cancellable with one big enough hit.
 */
public class InternetManagerBoss extends Monster {

    public static final int PHASE_CONNECTING = 1;
    public static final int PHASE_BUFFERING_PHASE = 2;
    public static final int PHASE_503 = 3;

    public static final int WINDUP_NONE = 0;
    public static final int WINDUP_WHIP = 1;
    public static final int WINDUP_LATENCY = 2;
    public static final int WINDUP_PACKET_LOSS = 3;
    public static final int WINDUP_BUFFER = 4;
    public static final int WINDUP_THROTTLE = 5;

    /** Damage in a single hit that shatters an ordinary telegraph. Buffering has its own, much larger, threshold below. */
    private static final float INTERRUPT_DAMAGE = 10.0F;
    /** Cumulative damage landed on the loading ring before it breaks early. */
    private static final float BUFFER_BREAK_THRESHOLD = 42.0F;

    private static final double ENGAGE_RANGE = 40.0D;
    private static final double WHIP_REACH = 6.5D;
    private static final float WHIP_DAMAGE = 9.0F;
    private static final double THROTTLE_RADIUS = 9.0D;
    private static final int THROTTLE_DURATION = 140;
    private static final int LATENCY_DURATION = 140;
    private static final int PACKET_LOSS_WINDOW = 100;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(InternetManagerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP =
            SynchedEntityData.defineId(InternetManagerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_TOTAL =
            SynchedEntityData.defineId(InternetManagerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_KIND =
            SynchedEntityData.defineId(InternetManagerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PACKET_LOSS =
            SynchedEntityData.defineId(InternetManagerBoss.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("The Internet Manager"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

    private int announcedPhase;
    private int attackCooldown = 60;
    private int packetLossTicks;
    private int stunTicks;
    private float bufferAccum;

    /** Transient: restyled from scratch on every world load, same reasoning as {@code TheManager.barStyled}. */
    private boolean barStyled;

    public InternetManagerBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 160;
    }

    // ---- registration -------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.9D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_CONNECTING);
        this.entityData.define(DATA_WINDUP, 0);
        this.entityData.define(DATA_WINDUP_TOTAL, 0);
        this.entityData.define(DATA_WINDUP_KIND, WINDUP_NONE);
        this.entityData.define(DATA_PACKET_LOSS, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- client-visible state ------------------------------------------------

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public int getWindupKind() {
        return this.entityData.get(DATA_WINDUP_KIND);
    }

    public boolean isWindingUp() {
        return this.entityData.get(DATA_WINDUP) > 0;
    }

    public boolean isBuffering() {
        return isWindingUp() && getWindupKind() == WINDUP_BUFFER;
    }

    public boolean isPacketLossActive() {
        return this.entityData.get(DATA_PACKET_LOSS);
    }

    /** 0 at the start of a telegraph, 1 the instant before it fires - and, for BUFFERING, how full the ring is. */
    public float getWindupProgress() {
        int total = this.entityData.get(DATA_WINDUP_TOTAL);
        if (total <= 0) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - this.entityData.get(DATA_WINDUP) / (float) total, 0.0F, 1.0F);
    }

    // ---- boss bar ------------------------------------------------------------

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

    // ---- main loop -----------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        this.bossEvent.setProgress(getHealth() / getMaxHealth());
        updatePhase();

        if (this.packetLossTicks > 0) {
            this.packetLossTicks--;
            if (this.packetLossTicks == 0) {
                this.entityData.set(DATA_PACKET_LOSS, false);
            }
        }

        if (this.stunTicks > 0) {
            this.stunTicks--;
            getNavigation().stop();
            return;
        }

        int windup = this.entityData.get(DATA_WINDUP);
        if (windup > 0) {
            tickWindup(windup);
            return;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && distanceToSqr(target) < ENGAGE_RANGE * ENGAGE_RANGE) {
            beginWindup();
        }
    }

    private void updatePhase() {
        float fraction = getHealth() / getMaxHealth();
        int phase = fraction > 0.65F ? PHASE_CONNECTING
                : fraction > 0.30F ? PHASE_BUFFERING_PHASE : PHASE_503;
        if (phase != getPhase()) {
            this.entityData.set(DATA_PHASE, phase);
        }
        if (!this.barStyled) {
            this.barStyled = true;
            styleBar(phase);
        }
        if (phase == this.announcedPhase) {
            return;
        }
        this.announcedPhase = phase;
        cancelWindup();
        this.attackCooldown = 40;
        styleBar(phase);
        announcePhase(phase);
    }

    private void styleBar(int phase) {
        switch (phase) {
            case PHASE_BUFFERING_PHASE -> {
                this.bossEvent.setName(Component.literal("The Internet Manager - BUFFERING...")
                        .withStyle(ChatFormatting.GOLD));
                this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            }
            case PHASE_503 -> {
                this.bossEvent.setName(Component.literal("The Internet Manager - 503 SERVICE UNAVAILABLE")
                        .withStyle(ChatFormatting.DARK_RED));
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
                this.bossEvent.setDarkenScreen(true);
                this.bossEvent.setCreateWorldFog(true);
            }
            default -> {
                this.bossEvent.setName(Component.literal("The Internet Manager - CONNECTING...")
                        .withStyle(ChatFormatting.GREEN));
                this.bossEvent.setColor(BossEvent.BossBarColor.GREEN);
            }
        }
    }

    private void announcePhase(int phase) {
        switch (phase) {
            case PHASE_BUFFERING_PHASE -> {
                say("Let's get you rebooted. This might take a moment.", ChatFormatting.GOLD);
                say("Please do not unplug anything.", ChatFormatting.YELLOW);
                level().playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE,
                        SoundSource.HOSTILE, 1.4F, 0.7F);
            }
            case PHASE_503 -> {
                say("Yeah. I'm gonna have to escalate this.", ChatFormatting.DARK_RED);
                say("Nothing personal. It's a capacity issue.", ChatFormatting.GRAY);
                level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN,
                        SoundSource.HOSTILE, 0.7F, 1.4F);
            }
            default -> say("Hi there. I hear you're having some issues with your connection.",
                    ChatFormatting.GREEN);
        }
    }

    // ---- telegraphed attacks --------------------------------------------------

    private void beginWindup() {
        int kind;
        int ticks;
        int phase = getPhase();
        switch (choose(phase)) {
            case WINDUP_LATENCY -> {
                kind = WINDUP_LATENCY;
                ticks = 30;
                say("One second, let me just check something.", ChatFormatting.AQUA);
            }
            case WINDUP_PACKET_LOSS -> {
                kind = WINDUP_PACKET_LOSS;
                ticks = 25;
                say("Huh. That's weird. Try it again.", ChatFormatting.YELLOW);
            }
            case WINDUP_BUFFER -> {
                kind = WINDUP_BUFFER;
                ticks = 110;
                this.bufferAccum = 0.0F;
                say("BUFFERING. Please hold.", ChatFormatting.GOLD);
                level().playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT,
                        SoundSource.HOSTILE, 1.6F, 0.5F);
            }
            case WINDUP_THROTTLE -> {
                kind = WINDUP_THROTTLE;
                ticks = 35;
                say("I'm going to have to throttle this connection.", ChatFormatting.RED);
            }
            default -> {
                kind = WINDUP_WHIP;
                ticks = 20;
                say("Hold still, this'll just take a second.", ChatFormatting.WHITE);
            }
        }
        this.entityData.set(DATA_WINDUP_KIND, kind);
        this.entityData.set(DATA_WINDUP_TOTAL, ticks);
        this.entityData.set(DATA_WINDUP, ticks);
        getNavigation().stop();
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 0.8F, 1.7F);
    }

    /** Weighted pick, gated by phase - matches {@code TheManager.beginWindup}'s per-phase switch, just data-driven. */
    private int choose(int phase) {
        int roll = this.random.nextInt(10);
        if (phase == PHASE_CONNECTING) {
            return roll < 6 ? WINDUP_WHIP : WINDUP_PACKET_LOSS;
        }
        if (phase == PHASE_BUFFERING_PHASE) {
            if (roll < 3) return WINDUP_WHIP;
            if (roll < 5) return WINDUP_PACKET_LOSS;
            if (roll < 7) return WINDUP_LATENCY;
            return WINDUP_BUFFER;
        }
        // PHASE_503: everything, faster, and throttle joins the rotation
        if (roll < 2) return WINDUP_WHIP;
        if (roll < 4) return WINDUP_PACKET_LOSS;
        if (roll < 6) return WINDUP_LATENCY;
        if (roll < 8) return WINDUP_THROTTLE;
        return WINDUP_BUFFER;
    }

    private void tickWindup(int remaining) {
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        int kind = getWindupKind();
        drawTelegraph(getWindupProgress(), kind);

        if (kind == WINDUP_BUFFER) {
            if (remaining % 10 == 0) {
                level().playSound(null, blockPosition(), SoundEvents.BEACON_AMBIENT,
                        SoundSource.HOSTILE, 0.9F, 0.6F + getWindupProgress() * 0.9F);
            }
            if (this.bufferAccum >= BUFFER_BREAK_THRESHOLD) {
                breakBuffering();
                return;
            }
        } else if (remaining % 8 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.HOSTILE, 0.7F, 0.6F + getWindupProgress() * 0.8F);
        }

        int next = remaining - 1;
        this.entityData.set(DATA_WINDUP, next);
        if (next > 0) {
            return;
        }
        int firedKind = kind;
        this.entityData.set(DATA_WINDUP_KIND, WINDUP_NONE);
        this.entityData.set(DATA_WINDUP_TOTAL, 0);
        fireAttack(firedKind);
    }

    private void cancelWindup() {
        this.entityData.set(DATA_WINDUP, 0);
        this.entityData.set(DATA_WINDUP_TOTAL, 0);
        this.entityData.set(DATA_WINDUP_KIND, WINDUP_NONE);
    }

    /** The ring on the floor, colour-coded per ability - the same shape as {@code TheManager.drawTelegraph}. */
    private void drawTelegraph(float progress, int kind) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        var particle = switch (kind) {
            case WINDUP_LATENCY -> ParticleTypes.ELECTRIC_SPARK;
            case WINDUP_PACKET_LOSS -> ParticleTypes.SMOKE;
            case WINDUP_BUFFER -> ParticleTypes.END_ROD;
            case WINDUP_THROTTLE -> ParticleTypes.SQUID_INK;
            default -> ParticleTypes.CRIT;
        };
        double radius = kind == WINDUP_BUFFER ? 1.6D : 1.0D + progress * 4.0D;
        int points = kind == WINDUP_BUFFER ? 24 : 14;
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2.0D
                    + progress * (kind == WINDUP_BUFFER ? 8.0D : 2.0D);
            double ringHeight = kind == WINDUP_BUFFER
                    ? getBbHeight() * 0.55D + Math.sin(progress * Math.PI * 2.0D) * 0.15D
                    : 0.15D;
            if (kind == WINDUP_BUFFER && (i / (double) points) > progress) {
                continue;   // only the FILLED arc of the ring is drawn - that arc IS the fill meter
            }
            server.sendParticles(particle,
                    getX() + Math.cos(angle) * radius, getY() + ringHeight,
                    getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + progress * getBbHeight(), getZ(), 2, 0.2D, 0.1D, 0.2D, 0.01D);
    }

    private void fireAttack(int kind) {
        switch (kind) {
            case WINDUP_WHIP -> {
                cableWhip();
                this.attackCooldown = 30 + this.random.nextInt(20);
            }
            case WINDUP_LATENCY -> {
                applyLatency();
                this.attackCooldown = 100 + this.random.nextInt(40);
            }
            case WINDUP_PACKET_LOSS -> {
                openPacketLossWindow();
                this.attackCooldown = 90 + this.random.nextInt(40);
            }
            case WINDUP_BUFFER -> {
                completeBuffering();
                this.attackCooldown = 60;
            }
            case WINDUP_THROTTLE -> {
                throttleZone();
                this.attackCooldown = 110 + this.random.nextInt(30);
            }
            default -> this.attackCooldown = 40;
        }
    }

    // ---- cable-whip melee -------------------------------------------------

    /**
     * The signature melee: a wider, longer reach than his fists, thrown in a
     * forward cone rather than at a single point, and it reels whoever it
     * catches in toward him - a cable doesn't just hit you, it's still
     * attached to something.
     */
    private void cableWhip() {
        Vec3 look = getViewVector(1.0F).normalize();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(WHIP_REACH), e -> e != this && e.isAlive() && e instanceof Player)) {
            Vec3 toTarget = target.position().subtract(position());
            double dist = toTarget.length();
            if (dist < 0.1D || dist > WHIP_REACH) {
                continue;
            }
            double dot = toTarget.normalize().dot(look);
            if (dot < 0.5D) {
                continue;   // outside the forward cone
            }
            target.hurt(level().damageSources().mobAttack(this), WHIP_DAMAGE);
            Vec3 pull = position().subtract(target.position()).normalize().scale(0.85D);
            target.setDeltaMovement(pull.x, 0.30D, pull.z);
            target.hurtMarked = true;
        }
        if (level() instanceof ServerLevel server) {
            Vec3 origin = position().add(0.0D, getBbHeight() * 0.55D, 0.0D).add(look.scale(0.6D));
            Vec3 tip = origin.add(look.scale(WHIP_REACH));
            for (int i = 0; i <= 12; i++) {
                double t = i / 12.0D;
                Vec3 p = origin.lerp(tip, t);
                server.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
        level().playSound(null, blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1,
                SoundSource.HOSTILE, 1.3F, 0.7F);
    }

    // ---- LATENCY ------------------------------------------------------------

    private void applyLatency() {
        int freeze = getPhase() >= PHASE_503 ? 5 : 3;
        boolean any = false;
        for (ServerPlayer player : nearbyPlayers()) {
            LatencyTracker.apply(player, LATENCY_DURATION, freeze);
            any = true;
        }
        if (any) {
            say("There. Should be smoother now.", ChatFormatting.AQUA);
        }
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    // ---- PACKET LOSS ----------------------------------------------------------

    private void openPacketLossWindow() {
        this.packetLossTicks = PACKET_LOSS_WINDOW;
        this.entityData.set(DATA_PACKET_LOSS, true);
        say("Packet loss on this line. Might drop a few things.", ChatFormatting.YELLOW);
        level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK,
                SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    private float packetLossChance() {
        return getPhase() >= PHASE_503 ? 0.5F : 0.32F;
    }

    private void spawnPacketLostFeedback(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY() + getBbHeight() * 0.6D, getZ(), 6, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK,
                SoundSource.HOSTILE, 0.6F, 1.6F);
        if (source.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal(ChatFormatting.GRAY
                    + "Packet lost."), true);
        }
    }

    // ---- BUFFERING ------------------------------------------------------------

    /** The ring finished filling with nobody breaking it - the payoff is a THROTTLE pulse the instant it completes. */
    private void completeBuffering() {
        say("Thanks for your patience.", ChatFormatting.GOLD);
        this.bufferAccum = 0.0F;
        throttleZone();
    }

    /** Broken early by damage. He is briefly stunned - a deliberate, big punish window, not a cosmetic pause. */
    private void breakBuffering() {
        cancelWindup();
        this.bufferAccum = 0.0F;
        this.stunTicks = 25;
        this.attackCooldown = 45;
        say("...oh, come on.", ChatFormatting.WHITE);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + getBbHeight() * 0.55D, getZ(), 30, 0.6D, 0.6D, 0.6D, 0.08D);
        }
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 1.4F, 0.6F);
    }

    // ---- THROTTLE ------------------------------------------------------------

    private void throttleZone() {
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(THROTTLE_RADIUS), le -> le != this && le.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, THROTTLE_DURATION, 2));
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, THROTTLE_DURATION, 2));
        }
        say("Throttling this connection. Everyone slows down, not just you.", ChatFormatting.RED);
        if (level() instanceof ServerLevel server) {
            for (int ring = 0; ring < 3; ring++) {
                double r = 2.0D + ring * (THROTTLE_RADIUS - 2.0D) / 2.0D;
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0D) * Math.PI * 2.0D;
                    server.sendParticles(ParticleTypes.SQUID_INK,
                            getX() + Math.cos(angle) * r, getY() + 0.2D, getZ() + Math.sin(angle) * r,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
        level().playSound(null, blockPosition(), SoundEvents.CONDUIT_ATTACK_TARGET,
                SoundSource.HOSTILE, 1.6F, 0.5F);
    }

    // ---- damage, death, chatter --------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (isBuffering()) {
                this.bufferAccum += amount;
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CRIT,
                            getX(), getY() + getBbHeight() * 0.5D, getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
                }
                return false;   // invulnerable while the ring is filling
            }
            if (this.packetLossTicks > 0 && source.getEntity() instanceof Player
                    && this.random.nextFloat() < packetLossChance()) {
                spawnPacketLostFeedback(source);
                return false;
            }
        }
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide && amount >= INTERRUPT_DAMAGE && isWindingUp()
                && getWindupKind() != WINDUP_BUFFER) {
            cancelWindup();
            this.attackCooldown = 30;
            say("Rude.", ChatFormatting.YELLOW);
            level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK,
                    SoundSource.HOSTILE, 1.2F, 1.4F);
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            say("This is... this is going to reflect very poorly on my metrics.", ChatFormatting.DARK_GRAY);
            for (ServerPlayer player : nearbyPlayers()) {
                LatencyTracker.clear(player.getUUID());
            }
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(InternetContent.STATIC_IP.get()));
        spawnAtLocation(new ItemStack(InternetContent.FIBER_OPTIC_COIL.get(), 3 + this.random.nextInt(4 + looting)));
        spawnAtLocation(new ItemStack(InternetContent.MANAGERS_HEADSET.get()));
        spawnAtLocation(new ItemStack(Items.REDSTONE, 6 + this.random.nextInt(6 + looting)));
        spawnAtLocation(new ItemStack(Items.COPPER_INGOT, 4 + this.random.nextInt(4 + looting)));
    }

    private List<ServerPlayer> nearbyPlayers() {
        List<ServerPlayer> found = new ArrayList<>();
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(ENGAGE_RANGE))) {
            if (player instanceof ServerPlayer server && server.isAlive() && !server.isSpectator()) {
                found.add(server);
            }
        }
        return found;
    }

    private void say(String line, ChatFormatting colour) {
        Component message = Component.literal("The Internet Manager: \"" + line + "\"").withStyle(colour);
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(64.0D))) {
            player.sendSystemMessage(message);
        }
    }

    // ---- sound / housekeeping ------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RESPAWN_ANCHOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("InternetManagerPhase", this.announcedPhase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.announcedPhase = tag.getInt("InternetManagerPhase");
    }
}
