package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * A massive, harmless shape that drifts around the far outer void of the
 * Krave Kosmos - never over the islands, never close enough to reach. Pure
 * distant spectacle: something huge glimpsed on the horizon, gliding, never
 * a threat and never a destination.
 *
 * <p>Deliberately a plain {@link Entity}, not a {@code PathfinderMob} - it
 * has no AI, no target, no combat, and cannot be hurt or picked (see
 * {@link #isPickable()}/{@link #hurt}). Its whole life is a fixed circular
 * orbit around the dimension origin, computed directly from tick count each
 * server tick rather than driven by velocity/physics, the same way
 * {@link KraveMeteor}/{@link GiantKraveBox} hand-roll their own motion
 * instead of relying on vanilla movement.
 *
 * <p>The orbit radius (see {@link #ORBIT_MIN}/{@link #ORBIT_MAX}) is chosen
 * to sit well past where anything else in this dimension normally reaches -
 * portal rooms the player actually visits land within 200-500 blocks of the
 * boss island (see KraveDoorBlock#buildKosmosRoomCopy) - so a player would
 * have to deliberately fly for minutes in a straight line away from
 * everything to ever get close, which is the point.
 *
 * <p>An entity only ticks (and only gets sent to any client, tracking range
 * or not) while its own chunk is loaded, and normal chunk loading follows
 * players - it never reaches 300+ blocks out where this thing spends its
 * whole life. Without forcing something to keep that chunk loaded, this
 * simply never runs and never renders no matter how correct everything
 * else about it is. {@link #tick()} force-loads a small bubble around its
 * own current position each tick and releases the previous one as it moves,
 * so it keeps animating (and can be sent to anyone within tracking range)
 * regardless of where any player actually is.
 */
public class KraveLeviathan extends Entity {

    // Kept inside the entity's own clientTrackingRange (see ModEntities) -
    // the first version orbited at 700-1100, comfortably past a normal
    // player's actual position AND past the entity's own tracking range, so
    // the server essentially never sent it to anyone. Nothing showing up
    // was that bug, not bad luck.
    private static final double ORBIT_MIN = 300.0D;
    private static final double ORBIT_MAX = 480.0D;
    private static final double ALTITUDE_MIN = 130.0D;
    private static final double ALTITUDE_MAX = 190.0D;
    private static final double BOB_AMPLITUDE = 6.0D;
    private static final double ANGULAR_SPEED = 0.00035D;   // radians/tick - a slow, wide circuit
    private static final double BOB_SPEED = 0.006D;

    private static final int CALL_MIN_INTERVAL = 600;    // 30s
    private static final int CALL_MAX_INTERVAL = 1800;   // 90s

    private double orbitRadius;
    private double altitude;
    private double phase;
    private double direction;   // +1 or -1, which way around the circle
    private int nextCallTick;
    private long forcedChunkKey = Long.MIN_VALUE;   // ChunkPos.asLong() of whatever's currently forced, or the sentinel for "none yet"

    public KraveLeviathan(EntityType<? extends KraveLeviathan> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.orbitRadius = ORBIT_MIN + this.random.nextDouble() * (ORBIT_MAX - ORBIT_MIN);
        this.altitude = ALTITUDE_MIN + this.random.nextDouble() * (ALTITUDE_MAX - ALTITUDE_MIN);
        this.phase = this.random.nextDouble() * Math.PI * 2.0D;
        this.direction = this.random.nextBoolean() ? 1.0D : -1.0D;
        this.nextCallTick = randomCallDelay();
    }

    private int randomCallDelay() {
        return CALL_MIN_INTERVAL + this.random.nextInt(CALL_MAX_INTERVAL - CALL_MIN_INTERVAL);
    }

    /**
     * Three of them, spawned once, ever - gated by their own one-time flag
     * in KraveKosmosData, so calling this from more than one place is
     * always safe. Called both from KraveDoorBlock (the first time a door
     * leads into the Kosmos) AND every tick from KraveKosmosAmbience, as a
     * redundant safety net.
     *
     * <p>On any world that already ran the very first version of this
     * method (orbit 700-1100, past both the old AND new tracking range),
     * that one-time flag was already permanently consumed - three
     * leviathans exist somewhere, forever stuck at the old orbit, and no
     * amount of fixing the spawn-call site was ever going to produce new,
     * correctly-configured ones once the flag was already spent. The
     * isLeviathanOrbitFixApplied migration below forces exactly one more
     * spawn pass, independent of the original flag's state, the first time
     * this runs on a world that hasn't already gotten it.
     */
    public static void ensureSpawned(ServerLevel kosmos) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        if (!data.isLeviathanOrbitFixApplied()) {
            data.setLeviathanOrbitFixApplied(true);
            data.setLeviathansEverSpawned(false);
        }
        if (data.isLeviathansEverSpawned()) {
            return;
        }
        data.setLeviathansEverSpawned(true);

        for (int i = 0; i < 3; i++) {
            KraveLeviathan leviathan = ModEntities.KRAVE_LEVIATHAN.get().create(kosmos);
            if (leviathan != null) {
                kosmos.addFreshEntity(leviathan);
            }
        }
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();

        double originX = KraveDimensions.BOSS_ISLAND.x;
        double originZ = KraveDimensions.BOSS_ISLAND.z;
        double angle = this.phase + this.direction * ANGULAR_SPEED * this.tickCount;
        double x = originX + Math.cos(angle) * this.orbitRadius;
        double z = originZ + Math.sin(angle) * this.orbitRadius;
        double y = this.altitude + Math.sin(this.tickCount * BOB_SPEED) * BOB_AMPLITUDE;

        // Facing tangent to the circle, so it visibly noses along its own
        // flight path instead of always pointing at the horizon.
        float yaw = (float) (Mth.atan2(
                this.direction * Math.cos(angle), -this.direction * Math.sin(angle)) * (180.0D / Math.PI));
        setYRot(yaw);
        setYHeadRot(yaw);

        setPos(x, y, z);
        move(MoverType.SELF, net.minecraft.world.phys.Vec3.ZERO);

        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            keepChunkLoaded(serverLevel, x, z);

            if (--this.nextCallTick <= 0) {
                call();
                this.nextCallTick = randomCallDelay();
            }
        }
    }

    /**
     * Forces a 3x3 bubble of chunks around wherever it currently is, and
     * releases the previous bubble once it's actually moved into a new
     * chunk - not every tick, since the orbit is slow enough (one chunk
     * roughly every 5-6 seconds) that re-forcing the same chunks each tick
     * would just be wasted ticket churn.
     */
    private void keepChunkLoaded(ServerLevel serverLevel, double x, double z) {
        long chunkKey = ChunkPos.asLong(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
        if (chunkKey == this.forcedChunkKey) {
            return;
        }
        if (this.forcedChunkKey != Long.MIN_VALUE) {
            setBubbleForced(serverLevel, this.forcedChunkKey, false);
        }
        setBubbleForced(serverLevel, chunkKey, true);
        this.forcedChunkKey = chunkKey;
    }

    private static void setBubbleForced(ServerLevel serverLevel, long chunkKey, boolean forced) {
        int cx = ChunkPos.getX(chunkKey);
        int cz = ChunkPos.getZ(chunkKey);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                serverLevel.setChunkForced(cx + dx, cz + dz, forced);
            }
        }
    }

    // No cleanup hook on removal: these are permanent ambient decorations
    // with no despawn path in normal play, so the only way one goes away is
    // an admin command - a genuinely rare edge case not worth chasing a
    // working override point for right now. Worst case then, its last
    // forced chunk bubble sits loaded until server restart.

    private void call() {
        SoundEvent[] calls = {
                ModSounds.LEVIATHAN_CALL_1.get(),
                ModSounds.LEVIATHAN_CALL_2.get(),
                ModSounds.LEVIATHAN_CALL_3.get(),
        };
        SoundEvent call = calls[this.random.nextInt(calls.length)];
        level().playSound(null, blockPosition(), call, SoundSource.AMBIENT,
                8.0F, 0.75F + this.random.nextFloat() * 0.2F);
    }

    /** Nothing can target, click, or attack it - it is never close enough to matter, and this makes sure. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.orbitRadius = tag.getDouble("OrbitRadius");
        this.altitude = tag.getDouble("Altitude");
        this.phase = tag.getDouble("Phase");
        this.direction = tag.getDouble("Direction");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("OrbitRadius", this.orbitRadius);
        tag.putDouble("Altitude", this.altitude);
        tag.putDouble("Phase", this.phase);
        tag.putDouble("Direction", this.direction);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4_000_000.0D;   // it's supposed to be visible from the far side of its own orbit
    }
}
