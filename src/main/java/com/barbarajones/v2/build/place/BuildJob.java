package com.barbarajones.v2.build.place;

import com.barbarajones.v2.build.KraveBuild;
import com.barbarajones.v2.build.block.KraveCoreBlockEntity;
import com.barbarajones.v2.build.def.CompletionHook;
import com.barbarajones.v2.build.def.PlacementContext;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One building going up (or coming back down), a few blocks per tick.
 *
 * <p>Everything is decided in the constructor. By the time the first block is
 * written the entire ordered write list exists, the terrain verdict has already
 * passed, and the undo snapshot array is allocated. Nothing during the animation
 * can decide to stop - which is what makes "never half-place a building" true
 * rather than aspirational. The only thing that can interrupt a job is the level
 * unloading, and {@link BuildScheduler} responds to that by calling
 * {@link #finishNow()}.
 *
 * <p>Write order, which is what the animation actually looks like:
 * <ol>
 *   <li>terrain above the build plane is scraped away, top down;</li>
 *   <li>hollows below the build plane are packed up, bottom up;</li>
 *   <li>the building itself grows bottom up, each layer spreading outwards from
 *       the middle.</li>
 * </ol>
 *
 * <p>Blocks are written with {@link Block#UPDATE_CLIENTS} only. That matters:
 * with neighbour updates on, a door's lower half spends a tick alone and
 * {@code DoorBlock.updateShape} deletes it before the upper half arrives. The
 * shape and neighbour updates are all run once, at the end, in
 * {@link #settle()} - the same order vanilla's own structure placer uses.
 */
public final class BuildJob {

    private static final Logger LOG = LoggerFactory.getLogger("BarbaraJones/BuildJob");

    /** Never write more than this in a single tick, however large the building. */
    public static final int MAX_BLOCKS_PER_TICK = 512;

    private final ServerLevel level;
    @Nullable
    private final StructureDef def;
    @Nullable
    private final PlacementContext ctx;
    @Nullable
    private final UUID placer;
    private final boolean undo;

    private final BoundingBox box;
    private final long[] positions;
    private final BlockState[] states;
    /** Previous block state ids over the whole box; -1 means "we never touched this one". */
    @Nullable
    private final int[] snapshot;

    private final int blocksPerTick;
    private final RandomSource rng;

    private int cursor;
    private int ticks;
    private boolean complete;

    // =====================================================================

    BuildJob(ServerLevel level, StructureDef def, PlacementCheck check,
             PlacementContext ctx, @Nullable ServerPlayer placer) {
        this.level = level;
        this.def = def;
        this.ctx = ctx;
        this.placer = placer == null ? null : placer.getUUID();
        this.undo = false;
        this.box = check.worldBounds();
        this.rng = level.getRandom();

        List<Entry> entries = plan(level, def, check, ctx);
        this.positions = new long[entries.size()];
        this.states = new BlockState[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            this.positions[i] = e.pos.asLong();
            this.states[i] = e.state;
        }
        this.snapshot = new int[volumeOf(box)];
        java.util.Arrays.fill(this.snapshot, -1);
        this.blocksPerTick = rate(entries.size(), def.buildTicks());

        clearOccupants();
        level.playSound(null, check.origin(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS, 0.35F, 1.6F);
    }

    /** The reverse job: puts a snapshot back, top down. */
    private BuildJob(ServerLevel level, BoundingBox box, List<Entry> entries) {
        this.level = level;
        this.def = null;
        this.ctx = null;
        this.placer = null;
        this.undo = true;
        this.box = box;
        this.rng = level.getRandom();
        this.positions = new long[entries.size()];
        this.states = new BlockState[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            this.positions[i] = e.pos.asLong();
            this.states[i] = e.state;
        }
        this.snapshot = null;
        this.blocksPerTick = rate(entries.size(), 20);
    }

    /**
     * Builds the reverse job used by the refund. Entries are already ordered
     * top down by the caller.
     */
    static BuildJob reverse(ServerLevel level, BoundingBox box, Map<BlockPos, BlockState> restore) {
        List<Entry> entries = new ArrayList<>(restore.size());
        for (Map.Entry<BlockPos, BlockState> e : restore.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue(), 0, e.getKey().getY(), 0));
        }
        entries.sort(Comparator.comparingInt((Entry e) -> -e.y()));
        return new BuildJob(level, box, entries);
    }

    // =====================================================================

    private static int rate(int total, int ticks) {
        return Math.max(1, Math.min(MAX_BLOCKS_PER_TICK, (total + ticks - 1) / Math.max(1, ticks)));
    }

    private static int volumeOf(BoundingBox box) {
        return box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    private record Entry(BlockPos pos, BlockState state, int phase, int y, int radius) { }

    /** Turns a validated check into the full ordered write list. */
    private static List<Entry> plan(ServerLevel level, StructureDef def,
                                    PlacementCheck check, PlacementContext ctx) {
        final int baseY = check.baseY();
        final BoundingBox box = check.worldBounds();
        final RandomSource rng = level.getRandom();

        // Resolve the definition into world blocks first, so the clear pass can
        // skip anything that is about to be overwritten anyway.
        Map<BlockPos, BlockState> planned = new HashMap<>();
        for (Map.Entry<BlockPos, Character> e : def.plan().entrySet()) {
            BlockPos local = e.getKey();
            BlockState state = def.palette().resolve(e.getValue(), rng, check.rotation());
            if (state == null) {
                LOG.warn("Structure {} has no palette entry for '{}' - skipping that block",
                        def.id(), e.getValue());
                continue;
            }
            planned.put(ctx.world(local), state);
        }

        List<Entry> entries = new ArrayList<>(planned.size() + 64);
        final int centreX = box.minX() + box.getXSpan() / 2;
        final int centreZ = box.minZ() + box.getZSpan() / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final boolean levelling = def.groundMode() != StructureDef.GroundMode.FLOAT;

        for (int dz = 0; dz < check.spanZ(); dz++) {
            for (int dx = 0; dx < check.spanX(); dx++) {
                int wx = check.origin().getX() + dx;
                int wz = check.origin().getZ() + dz;
                int surface = check.surfaceY(dx, dz);

                // Phase 0: scrape everything above the build plane that the
                // building is not going to occupy. Trees, hilltops, snow.
                // FLOAT means "do not touch the terrain", so it skips this entirely.
                for (int y = baseY; levelling && y <= box.maxY(); y++) {
                    cursor.set(wx, y, wz);
                    if (planned.containsKey(cursor)) {
                        continue;
                    }
                    if (level.getBlockState(cursor).isAir()) {
                        continue;
                    }
                    entries.add(new Entry(cursor.immutable(), Blocks.AIR.defaultBlockState(),
                            0, y, 0));
                }

                // Phase 1: pack the hollow under a low column up to the plane.
                if (surface != PlacementCheck.NO_SURFACE && surface + 1 < baseY) {
                    BlockState packing = foundationFor(level, def, wx, surface, wz);
                    for (int y = surface + 1; y < baseY; y++) {
                        cursor.set(wx, y, wz);
                        if (planned.containsKey(cursor)) {
                            continue;
                        }
                        entries.add(new Entry(cursor.immutable(), packing, 1, y, 0));
                    }
                }
            }
        }

        // Phase 2: the building.
        for (Map.Entry<BlockPos, BlockState> e : planned.entrySet()) {
            BlockPos pos = e.getKey();
            int radius = Math.abs(pos.getX() - centreX) + Math.abs(pos.getZ() - centreZ);
            entries.add(new Entry(pos, e.getValue(), 2, pos.getY(), radius));
        }

        entries.sort(Comparator
                .comparingInt((Entry e) -> e.phase())
                // scrape from the top down, everything else from the ground up
                .thenComparingInt(e -> e.phase() == 0 ? -e.y() : e.y())
                .thenComparingInt(Entry::radius));
        return entries;
    }

    private static BlockState foundationFor(ServerLevel level, StructureDef def, int x, int surfaceY, int z) {
        if (def.foundation() != null) {
            BlockState explicit = def.foundation().get();
            if (explicit != null) {
                return explicit;
            }
        }
        // Match the ground so a packed slope does not read as a concrete plinth.
        BlockState ground = level.getBlockState(new BlockPos(x, surfaceY, z));
        if (ground.isAir() || ground.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return Blocks.DIRT.defaultBlockState();
        }
        return ground;
    }

    /** Moves anything living out of the footprint before walls appear around it. Cayden included. */
    private void clearOccupants() {
        AABB volume = new AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1.0, box.maxY() + 1.0, box.maxZ() + 1.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, volume)) {
            double westGap = entity.getX() - box.minX();
            double eastGap = box.maxX() + 1.0 - entity.getX();
            double northGap = entity.getZ() - box.minZ();
            double southGap = box.maxZ() + 1.0 - entity.getZ();
            double best = Math.min(Math.min(westGap, eastGap), Math.min(northGap, southGap));
            double x = entity.getX();
            double z = entity.getZ();
            if (best == westGap) {
                x = box.minX() - 1.5;
            } else if (best == eastGap) {
                x = box.maxX() + 2.5;
            } else if (best == northGap) {
                z = box.minZ() - 1.5;
            } else {
                z = box.maxZ() + 2.5;
            }
            double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(x), (int) Math.floor(z));
            entity.teleportTo(x, y, z);
        }
    }

    // =====================================================================

    public boolean isComplete() {
        return complete;
    }

    public BoundingBox bounds() {
        return box;
    }

    public ServerLevel level() {
        return level;
    }

    /** Called once per server tick by {@link BuildScheduler}. */
    void tick() {
        if (complete) {
            return;
        }
        int end = Math.min(positions.length, cursor + blocksPerTick);
        int placedThisTick = 0;
        BlockState lastAudible = null;
        BlockPos lastAudiblePos = null;

        while (cursor < end) {
            BlockPos pos = BlockPos.of(positions[cursor]);
            BlockState state = states[cursor];
            cursor++;
            if (writeBlock(pos, state)) {
                placedThisTick++;
                if (!state.isAir()) {
                    lastAudible = state;
                    lastAudiblePos = pos;
                    if (placedThisTick <= 4) {
                        spawnDust(pos, state);
                    }
                }
            }
        }

        if (lastAudible != null && (ticks & 1) == 0) {
            SoundType sound = lastAudible.getSoundType();
            level.playSound(null, lastAudiblePos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    Math.min(1.0F, sound.getVolume() * 0.7F),
                    sound.getPitch() * (0.85F + rng.nextFloat() * 0.3F));
        }
        ticks++;

        if (cursor >= positions.length) {
            finish();
        }
    }

    /** Writes the rest of the building immediately. Used on shutdown and by instant placement. */
    public void finishNow() {
        if (complete) {
            return;
        }
        while (cursor < positions.length) {
            writeBlock(BlockPos.of(positions[cursor]), states[cursor]);
            cursor++;
        }
        finish();
    }

    private boolean writeBlock(BlockPos pos, BlockState state) {
        BlockState before = level.getBlockState(pos);
        if (before == state) {
            return false;
        }
        recordSnapshot(pos, before);
        return level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private void recordSnapshot(BlockPos pos, BlockState before) {
        if (snapshot == null || !box.isInside(pos)) {
            return;
        }
        int index = snapshotIndex(pos);
        if (snapshot[index] < 0) {
            snapshot[index] = Block.getId(before);
        }
    }

    private int snapshotIndex(BlockPos pos) {
        int dx = pos.getX() - box.minX();
        int dy = pos.getY() - box.minY();
        int dz = pos.getZ() - box.minZ();
        return (dy * box.getZSpan() + dz) * box.getXSpan() + dx;
    }

    private void spawnDust(BlockPos pos, BlockState state) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.0);
    }

    // =====================================================================

    private void finish() {
        if (complete) {
            return;
        }
        complete = true;
        settle();
        if (undo) {
            return;
        }
        placeCore();
        runHooks();
        BlockPos centre = new BlockPos(box.minX() + box.getXSpan() / 2,
                box.minY() + box.getYSpan() / 2,
                box.minZ() + box.getZSpan() / 2);
        level.playSound(null, centre, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.4F);
        level.playSound(null, centre, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.3F);
    }

    /**
     * One pass of shape and neighbour updates over everything we wrote. Fences
     * connect, stairs join up, redstone notices. Deferred to here so nothing
     * self-destructs mid-build.
     */
    private void settle() {
        for (long packed : positions) {
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
            level.blockUpdated(pos, state.getBlock());
        }
    }

    private void placeCore() {
        if (def == null || ctx == null) {
            return;
        }
        BlockPos corePos = ctx.core();
        BlockState previous = level.getBlockState(corePos);
        recordSnapshot(corePos, previous);
        level.setBlock(corePos, KraveBuild.CORE_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity be = level.getBlockEntity(corePos);
        if (be instanceof KraveCoreBlockEntity core) {
            core.initialise(def, ctx.rotation(), ctx.anchor(), placer, box, snapshot,
                    ctx.spawnedEntities(), level.getGameTime());
        } else {
            LOG.warn("Core block for {} at {} did not produce a block entity - no refund will be possible",
                    def.id(), corePos);
        }
    }

    private void runHooks() {
        if (def == null || ctx == null) {
            return;
        }
        for (CompletionHook hook : def.hooks()) {
            try {
                hook.onComplete(level, ctx);
            } catch (Exception ex) {
                LOG.error("Completion hook for structure {} threw; the building is finished but that hook did not run",
                        def.id(), ex);
            }
        }
        // The hooks may have spawned entities after the core was written, so the
        // core's record of them is refreshed here rather than earlier.
        if (ctx != null) {
            BlockEntity be = level.getBlockEntity(ctx.core());
            if (be instanceof KraveCoreBlockEntity core) {
                core.setSpawned(ctx.spawnedEntities());
            }
        }
    }
}
