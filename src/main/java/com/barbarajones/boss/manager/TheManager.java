package com.barbarajones.boss.manager;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * THE MANAGER - the necktie. A three-phase corporate boss who never throws a
 * punch without first making a production of it.
 *
 * <ol>
 *   <li><b>"Do you have a reservation?"</b> - he checks a list you are not on
 *       and summons Associates in ties to escort you out.</li>
 *   <li><b>"PERFORMANCE REVIEW"</b> - he starts a file on you. Every write-up
 *       stacks slowness, weakness and mining fatigue; the only way to clear one
 *       is to obey the pointless task he announces in chat before the clock
 *       runs out. Five write-ups and he terminates you with cause.</li>
 *   <li><b>"YOU'RE FIRED"</b> - homing termination paperwork, and a room full
 *       of filing cabinets that block it if you have the sense to duck.</li>
 * </ol>
 *
 * <p>Every attack runs through {@link #beginWindup} first: he roots himself,
 * raises his arms, and burns a visible particle ring into the floor for the
 * whole telegraph. Hit him hard enough during that window and the wind-up is
 * cancelled outright - interrupting the meeting is a real mechanic, not a
 * cosmetic one.
 */
public class TheManager extends Monster {

    public static final int PHASE_RESERVATION = 1;
    public static final int PHASE_REVIEW = 2;
    public static final int PHASE_FIRED = 3;

    public static final int WINDUP_NONE = 0;
    public static final int WINDUP_SUMMON = 1;
    public static final int WINDUP_REVIEW = 2;
    public static final int WINDUP_TERMINATE = 3;
    public static final int WINDUP_CABINETS = 4;

    /** Damage in a single hit that shatters a telegraph. */
    private static final float INTERRUPT_DAMAGE = 9.0F;

    private static final double ENGAGE_RANGE = 40.0D;
    private static final int MAX_ASSOCIATES = 6;
    private static final int CABINET_TARGET_COUNT = 5;

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(TheManager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP =
            SynchedEntityData.defineId(TheManager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_TOTAL =
            SynchedEntityData.defineId(TheManager.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WINDUP_KIND =
            SynchedEntityData.defineId(TheManager.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("The Manager"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);

    /** One personnel file per player who has been anywhere near this fight. */
    private final Map<UUID, PerformanceReview> files = new HashMap<>();

    private int announcedPhase;
    private int attackCooldown = 60;
    private int cabinetRestockTimer;

    /** Transient: the boss bar is rebuilt from scratch on every world load. */
    private boolean barStyled;

    public TheManager(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 120;
    }

    // ---- registration -------------------------------------------------------

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                // the corporate handshake: he does not kill you, he relocates you
                .add(Attributes.ATTACK_KNOCKBACK, 1.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, PHASE_RESERVATION);
        this.entityData.define(DATA_WINDUP, 0);
        this.entityData.define(DATA_WINDUP_TOTAL, 0);
        this.entityData.define(DATA_WINDUP_KIND, WINDUP_NONE);
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

    /** 0 at the start of a telegraph, 1 the instant before the attack lands. */
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

        // Reviews stay live once phase 2 opens: your file does not close just
        // because he has moved on to firing you.
        if (getPhase() >= PHASE_REVIEW) {
            tickReviews();
        }
        if (getPhase() == PHASE_FIRED) {
            maintainCabinets();
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
        int phase = fraction > 0.666F ? PHASE_RESERVATION
                : fraction > 0.333F ? PHASE_REVIEW : PHASE_FIRED;
        if (phase != getPhase()) {
            this.entityData.set(DATA_PHASE, phase);
        }
        // The bar's title and colour live only in memory, so a world reload has
        // to restyle it even though the phase itself has not changed - otherwise
        // a saved-mid-fight Manager comes back wearing the default red bar.
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
            case PHASE_REVIEW -> {
                this.bossEvent.setName(Component.literal("The Manager - PERFORMANCE REVIEW")
                        .withStyle(ChatFormatting.GOLD));
                this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            }
            case PHASE_FIRED -> {
                this.bossEvent.setName(Component.literal("The Manager - YOU'RE FIRED")
                        .withStyle(ChatFormatting.DARK_RED));
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
                this.bossEvent.setDarkenScreen(true);
                this.bossEvent.setCreateWorldFog(true);
            }
            default -> {
                this.bossEvent.setName(Component.literal("The Manager - \"Do you have a reservation?\"")
                        .withStyle(ChatFormatting.RED));
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            }
        }
    }

    private void announcePhase(int phase) {
        switch (phase) {
            case PHASE_REVIEW -> {
                say("Right. Since we are all here anyway - PERFORMANCE REVIEW. Sit down.",
                        ChatFormatting.GOLD);
                say("I am opening a file on you. Every write-up stays in it until you "
                        + "do exactly what I say, when I say it.", ChatFormatting.YELLOW);
                level().playSound(null, blockPosition(), ModSounds.EVT_MANAGER.get(),
                        SoundSource.HOSTILE, 1.4F, 1.0F);
                // everyone in the room starts the review already one write-up down
                for (ServerPlayer player : nearbyPlayers()) {
                    fileOf(player).addWriteUp();
                    fileOf(player).setNextReviewDelay(60);
                }
            }
            case PHASE_FIRED -> {
                say("YOU'RE FIRED. Effective immediately. Security will not be escorting you.",
                        ChatFormatting.DARK_RED);
                say("Hide behind the cabinets if you want. Paper finds everybody eventually.",
                        ChatFormatting.GRAY);
                level().playSound(null, blockPosition(), ModSounds.KRAVE_SIREN.get(),
                        SoundSource.HOSTILE, 1.6F, 0.8F);
                spawnCabinets(CABINET_TARGET_COUNT);
            }
            default -> say("Do you have a reservation? Because I am not seeing you on the list.",
                    ChatFormatting.RED);
        }
    }

    // ---- telegraphed attacks --------------------------------------------------

    private void beginWindup() {
        int kind;
        int ticks;
        switch (getPhase()) {
            case PHASE_RESERVATION -> {
                kind = WINDUP_SUMMON;
                ticks = 45;
                say("One moment. Let me just check the list again.", ChatFormatting.RED);
            }
            case PHASE_REVIEW -> {
                kind = WINDUP_REVIEW;
                ticks = 35;
                say("Pen. Where is my pen. There it is.", ChatFormatting.GOLD);
            }
            default -> {
                // alternate paperwork with a fresh delivery of cover, so phase 3
                // never degrades into standing in the open getting papercut
                boolean cabinets = countCabinets() < 2 || this.random.nextInt(4) == 0;
                kind = cabinets ? WINDUP_CABINETS : WINDUP_TERMINATE;
                ticks = cabinets ? 30 : 40;
                say(cabinets ? "Facilities. Bring the cabinets back in."
                        : "Signing your termination notice. Do NOT move.", ChatFormatting.DARK_RED);
            }
        }
        this.entityData.set(DATA_WINDUP_KIND, kind);
        this.entityData.set(DATA_WINDUP_TOTAL, ticks);
        this.entityData.set(DATA_WINDUP, ticks);
        getNavigation().stop();
        level().playSound(null, blockPosition(), SoundEvents.GRASS_BREAK,
                SoundSource.HOSTILE, 1.6F, 0.7F);
    }

    private void tickWindup(int remaining) {
        // rooted for the whole telegraph - the wind-up is the window to punish him
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        LivingEntity target = getTarget();
        if (target != null) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        drawTelegraph(getWindupProgress());
        if (remaining % 8 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.HOSTILE, 0.7F, 0.6F + getWindupProgress() * 0.8F);
        }

        int next = remaining - 1;
        this.entityData.set(DATA_WINDUP, next);
        if (next > 0) {
            return;
        }
        int kind = getWindupKind();
        this.entityData.set(DATA_WINDUP_KIND, WINDUP_NONE);
        this.entityData.set(DATA_WINDUP_TOTAL, 0);
        fireAttack(kind);
    }

    private void cancelWindup() {
        this.entityData.set(DATA_WINDUP, 0);
        this.entityData.set(DATA_WINDUP_TOTAL, 0);
        this.entityData.set(DATA_WINDUP_KIND, WINDUP_NONE);
    }

    /** A growing ring on the floor, colour-coded to whatever is coming. */
    private void drawTelegraph(float progress) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        ParticleOptions particle = switch (getWindupKind()) {
            case WINDUP_SUMMON -> ParticleTypes.ANGRY_VILLAGER;
            case WINDUP_REVIEW -> ParticleTypes.CRIT;
            case WINDUP_CABINETS -> ParticleTypes.CLOUD;
            default -> ParticleTypes.FLAME;
        };
        double radius = 1.0D + progress * 4.0D;
        int points = 14;
        for (int i = 0; i < points; i++) {
            double angle = (i / (double) points) * Math.PI * 2.0D + progress * 2.0D;
            server.sendParticles(particle,
                    getX() + Math.cos(angle) * radius, getY() + 0.15D,
                    getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // a column of sparks climbing him as the meter fills
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                getX(), getY() + progress * getBbHeight(), getZ(), 2, 0.2D, 0.1D, 0.2D, 0.01D);
    }

    private void fireAttack(int kind) {
        switch (kind) {
            case WINDUP_SUMMON -> {
                summonAssociates();
                this.attackCooldown = 140 + this.random.nextInt(60);
            }
            case WINDUP_REVIEW -> {
                issueReviews();
                this.attackCooldown = 100 + this.random.nextInt(40);
            }
            case WINDUP_CABINETS -> {
                spawnCabinets(3);
                this.attackCooldown = 90;
            }
            default -> {
                sendTerminationPaperwork();
                this.attackCooldown = 80 + this.random.nextInt(40);
            }
        }
    }

    // ---- phase 1: the reservation ---------------------------------------------

    private void summonAssociates() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        int existing = level().getEntitiesOfClass(ManagerMinion.class,
                getBoundingBox().inflate(32.0D)).size();
        int wanted = Math.min(3, MAX_ASSOCIATES - existing);
        if (wanted <= 0) {
            say("You are lucky. I am out of staff.", ChatFormatting.RED);
            return;
        }
        say("No, you don't. Gentlemen - see this one out.", ChatFormatting.RED);
        for (int i = 0; i < wanted; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 3.0D + this.random.nextDouble() * 3.0D;
            ManagerMinion minion = ModEntities.MANAGER_MINION.get().create(level());
            if (minion == null) {
                continue;
            }
            minion.moveTo(getX() + Math.cos(angle) * dist, getY(),
                    getZ() + Math.sin(angle) * dist,
                    this.random.nextFloat() * 360.0F, 0.0F);
            minion.setBoss(this);
            minion.setTarget(getTarget());
            level().addFreshEntity(minion);
            server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    minion.getX(), minion.getY() + 1.0D, minion.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.02D);
        }
        level().playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    // ---- phase 2: the performance review ---------------------------------------

    private void issueReviews() {
        List<ServerPlayer> players = nearbyPlayers();
        if (players.isEmpty()) {
            return;
        }
        for (ServerPlayer player : players) {
            PerformanceReview file = fileOf(player);
            if (file.hasTask() || file.getNextReviewDelay() > 0) {
                continue;
            }
            if (file.getWriteUps() == 0) {
                file.addWriteUp();
                tell(player, "You were late. That's a write-up.", ChatFormatting.RED);
            }
            ReviewTask task = ReviewTask.values()[this.random.nextInt(ReviewTask.values().length)];
            file.assign(task);
            tell(player, task.order(), ChatFormatting.YELLOW);
            player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    }

    private void tickReviews() {
        for (ServerPlayer player : nearbyPlayers()) {
            PerformanceReview file = fileOf(player);
            PerformanceReview.Outcome outcome = file.tick(player, this);
            switch (outcome) {
                case PASSED -> {
                    file.clearWriteUp();
                    file.setNextReviewDelay(60 + this.random.nextInt(60));
                    tell(player, "Fine. That one comes out of your file. Don't get comfortable.",
                            ChatFormatting.GREEN);
                    player.playNotifySound(SoundEvents.VILLAGER_YES, SoundSource.HOSTILE, 1.0F, 1.0F);
                    if (level() instanceof ServerLevel server) {
                        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                player.getX(), player.getY() + 1.6D, player.getZ(),
                                10, 0.4D, 0.4D, 0.4D, 0.0D);
                    }
                }
                case FAILED -> {
                    file.addWriteUp();
                    file.setNextReviewDelay(40);
                    tell(player, "Time. That's another one for the file.", ChatFormatting.RED);
                    player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.HOSTILE, 1.0F, 0.8F);
                    if (file.getWriteUps() >= PerformanceReview.TERMINATION_THRESHOLD) {
                        terminateWithCause(player, file);
                    }
                }
                default -> { }
            }
        }
    }

    /** Five write-ups. The file closes, loudly. */
    private void terminateWithCause(ServerPlayer player, PerformanceReview file) {
        tell(player, "That is five. TERMINATED WITH CAUSE. Clear your desk.", ChatFormatting.DARK_RED);
        player.hurt(level().damageSources().mobAttack(this), 12.0F);
        Vec3 away = player.position().subtract(position());
        if (away.horizontalDistanceSqr() > 1.0E-4D) {
            Vec3 push = away.normalize().scale(1.4D);
            player.push(push.x, 0.7D, push.z);
            player.hurtMarked = true;
        }
        file.clearAll();
        level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 1.4F, 0.6F);
    }

    /** Called by termination paperwork that lands - a hit is also a write-up. */
    public void writeUp(ServerPlayer player, String reason) {
        PerformanceReview file = fileOf(player);
        file.addWriteUp();
        tell(player, reason, ChatFormatting.RED);
        if (file.getWriteUps() >= PerformanceReview.TERMINATION_THRESHOLD) {
            terminateWithCause(player, file);
        }
    }

    private PerformanceReview fileOf(ServerPlayer player) {
        return this.files.computeIfAbsent(player.getUUID(), id -> new PerformanceReview());
    }

    // ---- phase 3: you're fired --------------------------------------------------

    private void sendTerminationPaperwork() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (isBehindCover(target)) {
            say("Hiding behind the furniture. Classic.", ChatFormatting.GRAY);
        }
        Vec3 origin = position().add(0.0D, getBbHeight() * 0.8D, 0.0D);
        for (int i = 0; i < 3; i++) {
            double spread = (i - 1) * 0.6D;
            TerminationNotice notice = new TerminationNotice(level(), this, target,
                    origin.add(spread, i * 0.25D, spread));
            level().addFreshEntity(notice);
        }
        level().playSound(null, blockPosition(), SoundEvents.ARROW_SHOOT,
                SoundSource.HOSTILE, 1.4F, 0.5F);
    }

    private void maintainCabinets() {
        if (--this.cabinetRestockTimer > 0) {
            return;
        }
        this.cabinetRestockTimer = 200;
        int missing = CABINET_TARGET_COUNT - countCabinets();
        if (missing > 0) {
            spawnCabinets(missing);
        }
    }

    private int countCabinets() {
        return level().getEntitiesOfClass(FilingCabinet.class, getBoundingBox().inflate(24.0D)).size();
    }

    private void spawnCabinets(int count) {
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 5.0D + this.random.nextDouble() * 7.0D;
            FilingCabinet cabinet = new FilingCabinet(level(),
                    getX() + Math.cos(angle) * dist,
                    getY() + 1.0D,
                    getZ() + Math.sin(angle) * dist);
            cabinet.setYRot(this.random.nextFloat() * 360.0F);
            level().addFreshEntity(cabinet);
        }
        level().playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                SoundSource.HOSTILE, 1.2F, 0.7F);
    }

    /**
     * True when a filing cabinet stands on the straight line between his eyes
     * and the target's - the whole point of the cabinets existing.
     */
    public boolean isBehindCover(LivingEntity target) {
        return isBehindCover(getEyePosition(), target.getEyePosition());
    }

    /** Shared with {@link TerminationNotice}, which re-checks cover every tick. */
    public static boolean isBehindCover(Level level, Vec3 from, Vec3 to) {
        AABB span = new AABB(from, to).inflate(1.0D);
        for (FilingCabinet cabinet : level.getEntitiesOfClass(FilingCabinet.class, span)) {
            if (cabinet.getBoundingBox().clip(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private boolean isBehindCover(Vec3 from, Vec3 to) {
        return isBehindCover(level(), from, to);
    }

    // ---- damage, death, chatter --------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hit = super.hurt(source, amount);
        if (hit && !level().isClientSide && amount >= INTERRUPT_DAMAGE && isWindingUp()) {
            cancelWindup();
            this.attackCooldown = 40;
            say("Do you MIND? I was in the middle of something.", ChatFormatting.YELLOW);
            level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK,
                    SoundSource.HOSTILE, 1.2F, 1.4F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CLOUD,
                        getX(), getY() + getBbHeight() * 0.6D, getZ(), 20, 0.5D, 0.5D, 0.5D, 0.05D);
            }
        }
        return hit;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            say("This is... this is going in MY file...", ChatFormatting.DARK_GRAY);
            // the fight is over: every open file is closed and every debuff lapses
            for (PerformanceReview file : this.files.values()) {
                file.clearAll();
            }
            for (FilingCabinet cabinet : level().getEntitiesOfClass(
                    FilingCabinet.class, getBoundingBox().inflate(32.0D))) {
                cabinet.retire();
            }
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.PINK_SLIP.get(), 1 + this.random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.SEVERANCE_CHECK.get()));
        spawnAtLocation(new ItemStack(ModItems.EMPLOYEE_OF_THE_MONTH.get()));
        spawnAtLocation(new ItemStack(ModItems.MANAGERS_TIE.get()));
        spawnAtLocation(new ItemStack(ModItems.FIVE_HUNDRED_DOLLARS.get()));
        spawnAtLocation(new ItemStack(Items.GOLD_INGOT, 3 + this.random.nextInt(4 + looting)));
    }

    private List<ServerPlayer> nearbyPlayers() {
        List<ServerPlayer> found = new ArrayList<>();
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(ENGAGE_RANGE))) {
            if (player instanceof ServerPlayer server && server.isAlive() && !server.isSpectator()) {
                found.add(server);
            }
        }
        return found;
    }

    private void say(String line, ChatFormatting colour) {
        Component message = Component.literal("The Manager: \"" + line + "\"").withStyle(colour);
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(64.0D))) {
            player.sendSystemMessage(message);
        }
    }

    private void tell(ServerPlayer player, String line, ChatFormatting colour) {
        player.sendSystemMessage(Component.literal("The Manager: \"" + line + "\"").withStyle(colour));
    }

    // ---- sound / housekeeping ------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ManagerPhase", this.announcedPhase);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PerformanceReview> entry : this.files.entrySet()) {
            if (entry.getValue().getWriteUps() <= 0) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("Player", entry.getKey());
            row.putInt("WriteUps", entry.getValue().getWriteUps());
            list.add(row);
        }
        tag.put("ManagerFiles", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.announcedPhase = tag.getInt("ManagerPhase");
        this.files.clear();
        ListTag list = tag.getList("ManagerFiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!row.hasUUID("Player")) {
                continue;
            }
            // The in-flight task is intentionally not saved: shouting an order at
            // someone who just logged in and cannot see the chat backlog would be
            // an unwinnable timer. Only the write-up count survives a reload.
            PerformanceReview file = new PerformanceReview();
            file.setWriteUps(row.getInt("WriteUps"));
            file.setNextReviewDelay(100);
            this.files.put(row.getUUID("Player"), file);
        }
    }
}
