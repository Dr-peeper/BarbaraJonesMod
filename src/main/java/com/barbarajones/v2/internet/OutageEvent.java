package com.barbarajones.v2.internet;

import com.barbarajones.content.extra.TelevisionBlock;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * THE OUTAGE: village-wide, Terraria-blood-moon-shaped event, scoped to one
 * {@link ServerLevel} and persisted as {@link SavedData} exactly the way
 * vanilla's {@code Raids} tracks a raid, or {@link com.barbarajones.dimension.
 * KraveKosmosData} tracks the Kosmos boss - one file, one source of truth, so
 * two chunks can never disagree about whether the internet is currently out.
 *
 * <p>State machine, one field ({@link #state}) driving everything:
 * <pre>
 *   INACTIVE --(roll a future time)--&gt; SCHEDULED
 *   SCHEDULED --(time due AND a housed Cayden exists)--&gt; ANNOUNCED
 *   ANNOUNCED --(countdown elapses)--&gt; ACTIVE (boss spawns here)
 *   ACTIVE --(boss dead/gone, OR safety timeout)--&gt; ENDING
 *   ENDING --(countdown elapses)--&gt; INACTIVE (next time rolled)
 * </pre>
 * A manual call ({@link #tryManualCall}) skips straight from INACTIVE/
 * SCHEDULED to ANNOUNCED, on its own cooldown, so spamming the call box can't
 * chain-summon him.
 *
 * <p><b>The village gate.</b> This module does not invent a settlement system
 * - {@code CaydenCobb.isHoused()} is already the mod's one working signal for
 * "the player has built somewhere Cayden calls home" (backed by {@code
 * HousingValidator}), so that is what "the village exists" means here: at
 * least one tamed, housed Cayden loaded somewhere in this level. No village,
 * no outage, exactly as required - {@link #villageExists} is checked before
 * every SCHEDULED-&gt;ANNOUNCED transition and again inside every manual call.
 */
public class OutageEvent extends SavedData {

    private static final String KEY = "barbarajones_internet_outage";

    public static final int STATE_INACTIVE = 0;
    public static final int STATE_SCHEDULED = 1;
    public static final int STATE_ANNOUNCED = 2;
    public static final int STATE_ACTIVE = 3;
    public static final int STATE_ENDING = 4;

    /** Random schedule window: somewhere between four and ten in-game days. */
    private static final long MIN_INTERVAL = 24000L * 4L;
    private static final long MAX_INTERVAL = 24000L * 10L;
    /** If the gate fails at the scheduled moment, try again a day later rather than checking every tick. */
    private static final long RECHECK_DELAY = 24000L;
    /** How long the siren/dim-lights buildup runs before he actually spawns. */
    private static final int ANNOUNCE_TICKS = 200;
    /** Nobody engaging him at all cannot hang the event forever - a mercy timeout. */
    private static final int ACTIVE_SAFETY_TIMEOUT = 24000;
    private static final int ENDING_TICKS = 100;
    /** A manual call cannot be repeated for ten minutes after the last one resolves. */
    private static final long MANUAL_COOLDOWN = 12000L;
    private static final int ATMOSPHERE_INTERVAL = 30;
    /**
     * Ticks {@link #tick} tolerates the boss being unfindable before treating
     * the fight as over. Not just a nicety: right after a server restart the
     * boss's own chunk may not be loaded yet on the very first ticks, so
     * {@code level.getEntity(bossId)} returning null for one tick does NOT
     * mean he died - it can just as easily mean his chunk hasn't come back
     * yet. Ending the event on that alone would orphan a perfectly alive
     * boss (still saved, still going to spawn back in) with no
     * {@link OutageEvent} left tracking him.
     */
    private static final int BOSS_MISSING_GRACE = 100;

    private static final AABB WORLD_BOUNDS =
            new AABB(-3.0E7, -64.0D, -3.0E7, 3.0E7, 320.0D, 3.0E7);

    private int state = STATE_INACTIVE;
    private long nextEventTime;
    private int ticksInState;
    private long manualCooldownUntil;
    @Nullable private BlockPos epicenter;
    @Nullable private UUID bossId;
    /** Transient - see {@link #BOSS_MISSING_GRACE}. Deliberately not persisted: a fresh reload gets a fresh grace window, which is the safe default. */
    private int bossMissingTicks;

    /** Pre-fight-only bar: "INCOMING CALL...". Handed off to the boss's own bar the instant he spawns. Transient - see {@code barStyled} in TheManager for why this class of state does not need to survive a reload. */
    private final ServerBossEvent preFightBar = new ServerBossEvent(
            Component.literal("Incoming Call..."), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    public static OutageEvent get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OutageEvent::load, OutageEvent::new, KEY);
    }

    // ---- public surface -------------------------------------------------------

    public int getState() {
        return this.state;
    }

    public boolean isActive() {
        return this.state == STATE_ANNOUNCED || this.state == STATE_ACTIVE || this.state == STATE_ENDING;
    }

    public static boolean isActiveIn(ServerLevel level) {
        return get(level).isActive();
    }

    /**
     * The village block and the phone item both call this. Returns {@code null}
     * on success, or a reason to show the caller when the call cannot go
     * through.
     */
    public static String tryManualCall(ServerLevel level, BlockPos callerPos, Player caller) {
        return get(level).manualCallInternal(level, callerPos);
    }

    /**
     * Forcible, clean cancellation - the "cancellable" half of the brief. Ends
     * whatever is currently happening (announcement, active fight, or just a
     * pending schedule) and rolls a fresh future time. Safe to call from
     * anywhere; does nothing if nothing is running.
     */
    public static void cancel(ServerLevel level) {
        OutageEvent data = get(level);
        if (data.bossId != null && level.getEntity(data.bossId) instanceof InternetManagerBoss boss) {
            boss.discard();
        }
        data.preFightBar.removeAllPlayers();
        LatencyTracker.clearAll();
        data.state = STATE_INACTIVE;
        data.bossId = null;
        data.epicenter = null;
        data.nextEventTime = 0L;
        data.setDirty();
    }

    /** Driven once per level per server tick by {@link InternetOutageEvents}. */
    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        switch (this.state) {
            case STATE_INACTIVE -> {
                if (this.nextEventTime <= 0L) {
                    this.nextEventTime = now + rollInterval(level);
                }
                this.state = STATE_SCHEDULED;
                setDirty();
            }
            case STATE_SCHEDULED -> {
                if (now < this.nextEventTime) {
                    return;
                }
                BlockPos village = villageExists(level);
                if (village == null) {
                    this.nextEventTime = now + RECHECK_DELAY;
                    setDirty();
                    return;
                }
                beginAnnounce(level, village);
            }
            case STATE_ANNOUNCED -> {
                tickAtmosphere(level);
                this.preFightBar.setProgress(1.0F - this.ticksInState / (float) ANNOUNCE_TICKS);
                if (--this.ticksInState <= 0) {
                    beginActive(level);
                }
                setDirty();
            }
            case STATE_ACTIVE -> {
                tickAtmosphere(level);
                Entity found = this.bossId != null ? level.getEntity(this.bossId) : null;
                if (found instanceof InternetManagerBoss boss && boss.isAlive()) {
                    this.bossMissingTicks = 0;
                    if (--this.ticksInState <= 0) {
                        dropCall(level, boss);
                        beginEnding(level, false);
                    }
                } else if (this.bossId == null || ++this.bossMissingTicks > BOSS_MISSING_GRACE) {
                    // either he never actually spawned (creation failed - nothing to
                    // wait for) or he's been unfindable for the whole grace window,
                    // which past that point really does mean gone, not just unloaded
                    beginEnding(level, true);
                }
                setDirty();
            }
            case STATE_ENDING -> {
                if (--this.ticksInState <= 0) {
                    finish(level);
                }
                setDirty();
            }
            default -> { }
        }
    }

    private long rollInterval(ServerLevel level) {
        long span = MAX_INTERVAL - MIN_INTERVAL;
        return MIN_INTERVAL + (span > 0L ? (long) (level.random.nextDouble() * span) : 0L);
    }

    // ---- the village gate -----------------------------------------------------

    /**
     * A loaded, tamed Cayden with a valid home - see the class javadoc. Returns
     * his position (used as the outage's epicenter) or {@code null}.
     */
    @Nullable
    private static BlockPos villageExists(ServerLevel level) {
        for (CaydenCobb cayden : level.getEntitiesOfClass(CaydenCobb.class, WORLD_BOUNDS)) {
            if (cayden.isHoused()) {
                return cayden.blockPosition();
            }
        }
        return null;
    }

    private String manualCallInternal(ServerLevel level, BlockPos callerPos) {
        if (this.state != STATE_INACTIVE && this.state != STATE_SCHEDULED) {
            return "The line's already busy.";
        }
        long now = level.getGameTime();
        if (now < this.manualCooldownUntil) {
            long remain = (this.manualCooldownUntil - now) / 20L;
            return "Still on hold from last time. Try again in " + remain + "s.";
        }
        if (villageExists(level) == null) {
            return "There's nothing here he'd call a village yet.";
        }
        this.manualCooldownUntil = now + MANUAL_COOLDOWN;
        beginAnnounce(level, callerPos);
        return null;
    }

    // ---- state transitions ------------------------------------------------

    private void beginAnnounce(ServerLevel level, BlockPos epicenter) {
        this.state = STATE_ANNOUNCED;
        this.ticksInState = ANNOUNCE_TICKS;
        this.epicenter = epicenter.immutable();
        this.preFightBar.setProgress(0.0F);
        this.preFightBar.setDarkenScreen(true);
        for (ServerPlayer player : level.players()) {
            this.preFightBar.addPlayer(player);
            player.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED + "" + ChatFormatting.BOLD
                    + "⚠ THE INTERNET IS OUT."));
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "Somebody call the manager. The INTERNET manager."));
            level.playSound(null, player.blockPosition(), com.barbarajones.content.ModSounds.KRAVE_SIREN.get(),
                    SoundSource.AMBIENT, 2.0F, 0.6F);
        }
        setDirty();
    }

    private void beginActive(ServerLevel level) {
        this.state = STATE_ACTIVE;
        this.ticksInState = ACTIVE_SAFETY_TIMEOUT;
        this.preFightBar.removeAllPlayers();

        BlockPos ground = findGround(level, this.epicenter != null ? this.epicenter : level.getSharedSpawnPos());
        InternetManagerBoss boss = InternetContent.INTERNET_MANAGER.get().create(level);
        if (boss != null) {
            boss.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(boss);
            this.bossId = boss.getUUID();
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD
                        + "THE INTERNET MANAGER HAS ARRIVED."));
            }
            level.playSound(null, ground, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.6F, 0.6F);
        }
        setDirty();
    }

    private void dropCall(ServerLevel level, InternetManagerBoss boss) {
        for (Player player : level.getEntitiesOfClass(Player.class, boss.getBoundingBox().inflate(64.0D))) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "The call drops. He'll be back."));
        }
        boss.discard();
    }

    private void beginEnding(ServerLevel level, boolean resolved) {
        this.state = STATE_ENDING;
        this.ticksInState = ENDING_TICKS;
        this.bossId = null;
        this.bossMissingTicks = 0;
        this.preFightBar.removeAllPlayers();
        if (resolved) {
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                        + "The connection is restored."));
            }
            level.playSound(null, this.epicenter != null ? this.epicenter : level.getSharedSpawnPos(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.4F, 1.2F);
        }
        setDirty();
    }

    private void finish(ServerLevel level) {
        this.state = STATE_INACTIVE;
        this.epicenter = null;
        this.nextEventTime = 0L;   // the INACTIVE branch above rolls a fresh one next tick
        LatencyTracker.clearAll();
        setDirty();
    }

    // ---- atmosphere ---------------------------------------------------------

    /**
     * "Village production halts, a dread hum, static particles" - all of it
     * anchored on {@link #epicenter} rather than swept over the whole level,
     * both so it stays affordable and so it reads as a local blackout rather
     * than a global weather effect.
     */
    private void tickAtmosphere(ServerLevel level) {
        if (this.epicenter == null || level.getGameTime() % ATMOSPHERE_INTERVAL != 0L) {
            return;
        }
        BlockPos ep = this.epicenter;

        // the dread hum
        level.playSound(null, ep, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.AMBIENT, 3.0F, 0.55F);

        // static hanging in the air near the epicenter
        level.sendParticles(ParticleTypes.SMOKE,
                ep.getX() + 0.5D, ep.getY() + 1.5D, ep.getZ() + 0.5D, 6,
                6.0D, 3.0D, 6.0D, 0.01D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                ep.getX() + 0.5D, ep.getY() + 1.5D, ep.getZ() + 0.5D, 4,
                6.0D, 3.0D, 6.0D, 0.02D);

        // village production halts: villagers nearby stall out and go dark
        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(ep).inflate(40.0D, 12.0D, 40.0D))) {
            villager.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
        }

        // screens in the village go to static: every lit TV nearby gets a burst
        int hit = 0;
        int radius = 20;
        for (int dx = -radius; dx <= radius && hit < 6; dx += 2) {
            for (int dz = -radius; dz <= radius && hit < 6; dz += 2) {
                for (int dy = -6; dy <= 6 && hit < 6; dy++) {
                    BlockPos p = ep.offset(dx, dy, dz);
                    if (!level.isLoaded(p)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.getBlock() instanceof TelevisionBlock && state.getValue(TelevisionBlock.LIT)) {
                        hit++;
                        level.sendParticles(ParticleTypes.SMOKE,
                                p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 8, 0.2D, 0.2D, 0.2D, 0.02D);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 6, 0.2D, 0.2D, 0.2D, 0.03D);
                        level.playSound(null, p, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 1.7F);
                    }
                }
            }
        }
    }

    // ---- helpers --------------------------------------------------------------

    private static BlockPos findGround(ServerLevel level, BlockPos near) {
        BlockPos.MutableBlockPos m = near.mutable();
        // climb out if the epicenter turned out to be embedded in solid ground
        int guard = 0;
        while (!level.getBlockState(m).isAir() && guard++ < 32) {
            m.move(0, 1, 0);
        }
        // then settle onto the first solid floor beneath, so he doesn't spawn hovering
        guard = 0;
        while (level.getBlockState(m.below()).isAir()
                && m.getY() > level.getMinBuildHeight() + 1 && guard++ < 32) {
            m.move(0, -1, 0);
        }
        return m.immutable();
    }

    // ---- persistence ------------------------------------------------------

    private static OutageEvent load(CompoundTag tag) {
        OutageEvent data = new OutageEvent();
        data.state = tag.getInt("State");
        data.nextEventTime = tag.getLong("NextEventTime");
        data.ticksInState = tag.getInt("TicksInState");
        data.manualCooldownUntil = tag.getLong("ManualCooldownUntil");
        if (tag.contains("EpicenterX")) {
            data.epicenter = new BlockPos(tag.getInt("EpicenterX"), tag.getInt("EpicenterY"), tag.getInt("EpicenterZ"));
        }
        if (tag.hasUUID("BossId")) {
            data.bossId = tag.getUUID("BossId");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("State", this.state);
        tag.putLong("NextEventTime", this.nextEventTime);
        tag.putInt("TicksInState", this.ticksInState);
        tag.putLong("ManualCooldownUntil", this.manualCooldownUntil);
        if (this.epicenter != null) {
            tag.putInt("EpicenterX", this.epicenter.getX());
            tag.putInt("EpicenterY", this.epicenter.getY());
            tag.putInt("EpicenterZ", this.epicenter.getZ());
        }
        if (this.bossId != null) {
            tag.putUUID("BossId", this.bossId);
        }
        return tag;
    }
}
