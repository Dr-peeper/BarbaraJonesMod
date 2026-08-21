package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.dimension.KraveLanding;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayList;
import java.util.List;

/**
 * The Krave Kosmos portal: an enclosed 3-wide x 3-deep x 3-tall chocolate
 * room with this door set into one wall. It is an entirely ordinary door in
 * every way that matters visually - opens, closes, plays the normal sound,
 * shows the room behind it - right up until the specific moment it gets
 * CLOSED with a player standing inside a complete room. That close is the
 * trigger, not the open: walk in, shut the door behind you, and you (and
 * anything riding with you) are moved into an identical room built on the
 * other side, the Kosmos if you started outside it or back home if you
 * started inside it. Opening the door on the far side afterward is just...
 * opening a door - by the time you can do that, you're already through.
 *
 * <p>Each physical door/room the player builds gets its own permanent,
 * independent partner room in the Kosmos, created the first time it's used
 * and remembered from then on ({@link KraveKosmosData}'s portal-link map) -
 * closing the same door later always returns to the same partner, never a
 * fresh one.
 */
public class KraveDoorBlock extends DoorBlock {

    /** The fixed orientation every auto-built room copy uses, Kosmos-side or overworld-side - arbitrary, just has to be consistent with itself. */
    private static final Direction AUTO_ROOM_INTO = Direction.NORTH;

    /** Pause between the door actually closing and the trip firing, so the close animation/sound have a moment to land first. */
    private static final int DOOR_CLOSE_DELAY_TICKS = 15;
    /** How long a companion who didn't make it through with the player gets to catch up before being forced along. */
    private static final int STRAGGLER_DELAY_TICKS = 140;
    /** How close a companion has to actually be, at the moment the door shuts, to count as "ran in with you." */
    private static final double COMPANION_MUST_BE_CLOSE_RADIUS = 3.0D;

    public KraveDoorBlock(BlockBehaviour.Properties props, BlockSetType setType) {
        super(props, setType);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        boolean wasOpen = level.getBlockState(lowerPos).getValue(OPEN);

        InteractionResult result = super.use(state, level, pos, player, hand, hit);

        if (wasOpen && !level.isClientSide && level instanceof ServerLevel serverLevel
                && player instanceof ServerPlayer serverPlayer) {
            BlockState lowerState = level.getBlockState(lowerPos);
            if (!lowerState.getValue(OPEN)) {
                // super.use() just closed it - this is the trigger moment.
                tryTravel(serverLevel, lowerPos, lowerState.getValue(FACING), serverPlayer);
            }
        }
        return result;
    }

    private void tryTravel(ServerLevel level, BlockPos lowerPos, Direction facing, ServerPlayer player) {
        Direction into = validRoomDirection(level, lowerPos, facing);
        if (into == null || !playerInInterior(player, lowerPos, into)) {
            return;
        }
        // A short pause before the trip actually fires - triggering it the
        // instant the door shuts (the same tick, same method call) never
        // gave the close animation or its sound any time to actually land
        // before the world swapped out from under the player.
        KraveDoorScheduler.schedule(DOOR_CLOSE_DELAY_TICKS, () -> {
            if (!player.isAlive() || player.level() != level) {
                return;
            }
            BlockState recheck = level.getBlockState(lowerPos);
            if (!(recheck.getBlock() instanceof KraveDoorBlock) || recheck.getValue(OPEN)) {
                return;   // reopened, broken, replaced - something changed in the meantime, don't force it
            }
            if (level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
                travelFromKosmos(level, lowerPos, player);
            } else {
                travelToKosmos(level, lowerPos, into, player);
            }
        });
    }

    // ---- room geometry: validate + locate --------------------------------

    /**
     * A fully enclosed 3-wide x 3-deep x 3-tall chocolate room, door set into
     * the middle of one wall - floor and roof included, only the one column
     * directly behind the door left hollow to actually stand in. Tried on
     * both sides of the door plane, since a player building this has no way
     * to know which way {@code FACING} happens to point; returns whichever
     * direction the room actually extends in, or null if neither works.
     */
    private Direction validRoomDirection(Level level, BlockPos lowerPos, Direction doorFacing) {
        if (isRoomComplete(level, lowerPos, doorFacing)) {
            return doorFacing;
        }
        Direction opposite = doorFacing.getOpposite();
        return isRoomComplete(level, lowerPos, opposite) ? opposite : null;
    }

    private boolean isRoomComplete(Level level, BlockPos lowerPos, Direction into) {
        Direction side = into.getClockWise();
        for (int depth = 0; depth <= 2; depth++) {
            for (int across = -1; across <= 1; across++) {
                // 4 rows total, floor to roof: -1 (floor), 0-1 (door height,
                // 2 tall), 2 (roof, sitting directly on the door/interior -
                // no separate lintel row buffering it).
                for (int row = -1; row <= 2; row++) {
                    // Everywhere else in the shell sits under a wall column
                    // that's often buried anyway - only the two cells someone
                    // actually stands or walks on (under the door, under the
                    // interior) need to visibly be chocolate block. The rest
                    // of the floor is unchecked, whatever the terrain already
                    // has there.
                    if (row == -1 && !(across == 0 && depth <= 1)) {
                        continue;
                    }
                    BlockPos cell = lowerPos.relative(into, depth).relative(side, across).above(row);
                    boolean isRoof = row == 2;
                    boolean isDoorCell = row != -1 && !isRoof && depth == 0 && across == 0;
                    boolean isInterior = row != -1 && !isRoof && !isDoorCell && depth == 1 && across == 0;

                    if (isDoorCell) {
                        if (!level.getBlockState(cell).is(ModBlocks.KRAVE_DOOR.get())) {
                            return false;
                        }
                    } else if (isInterior) {
                        if (level.getBlockState(cell).blocksMotion()) {
                            return false;
                        }
                    } else if (!isKraveBlock(level, cell)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isKraveBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.KRAVE_BLOCK.get());
    }

    /** The one interior cell (the column directly behind the door) is 1 wide, 1 deep, 2 tall - the player's feet must be in it. */
    private boolean playerInInterior(Player player, BlockPos lowerPos, Direction into) {
        BlockPos interior = lowerPos.relative(into);
        BlockPos feet = player.blockPosition();
        return feet.getX() == interior.getX() && feet.getZ() == interior.getZ()
                && feet.getY() >= interior.getY() && feet.getY() <= interior.getY() + 1;
    }

    /** Places the exact shape {@link #isRoomComplete} validates - the door and the interior column are left for the caller. */
    private void placeRoomShell(ServerLevel level, BlockPos lowerPos, Direction into) {
        Direction side = into.getClockWise();
        BlockState kraveBlock = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        for (int depth = 0; depth <= 2; depth++) {
            for (int across = -1; across <= 1; across++) {
                for (int row = -1; row <= 2; row++) {
                    boolean isFloorOrRoof = row == -1 || row == 2;
                    boolean isDoorCell = !isFloorOrRoof && depth == 0 && across == 0;
                    boolean isInterior = !isFloorOrRoof && !isDoorCell && depth == 1 && across == 0;
                    if (isDoorCell || isInterior) {
                        continue;
                    }
                    BlockPos cell = lowerPos.relative(into, depth).relative(side, across).above(row);
                    level.setBlock(cell, kraveBlock, 3);
                }
            }
        }
        for (int row = 0; row <= 1; row++) {
            level.setBlock(lowerPos.relative(into).above(row), Blocks.AIR.defaultBlockState(), 3);
        }
        BlockState doorLower = ModBlocks.KRAVE_DOOR.get().defaultBlockState()
                .setValue(FACING, into)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, Boolean.FALSE)
                .setValue(POWERED, Boolean.FALSE)
                .setValue(HALF, DoubleBlockHalf.LOWER);
        level.setBlock(lowerPos, doorLower, 3);
        level.setBlock(lowerPos.above(), doorLower.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    // ---- travel -------------------------------------------------------------

    private void travelToKosmos(ServerLevel overworld, BlockPos doorLowerPos, Direction into, ServerPlayer player) {
        ServerLevel kosmos = player.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null) {
            return;
        }
        ensureBossExists(kosmos);

        GlobalPos external = GlobalPos.of(overworld.dimension(), doorLowerPos);
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        BlockPos kosmosDoorPos = data.kosmosDoorFor(external);
        if (kosmosDoorPos == null) {
            kosmosDoorPos = buildKosmosRoomCopy(kosmos);
            if (kosmosDoorPos == null) {
                return;   // couldn't find anywhere to put it - leave the player where they are rather than half-teleport them
            }
            data.link(external, kosmosDoorPos);
        }

        // Still writes the old per-player return spot too - KraveTetherItem
        // reads it independently of this door's own portal-link system, and
        // stays fully functional as long as this keeps being set on arrival.
        CompoundTag persist = persisted(player);
        persist.putString("KraveReturnDim", overworld.dimension().location().toString());
        persist.putDouble("KraveReturnX", player.getX());
        persist.putDouble("KraveReturnY", player.getY());
        persist.putDouble("KraveReturnZ", player.getZ());

        teleportInto(player, kosmos, kosmosDoorPos, AUTO_ROOM_INTO, true);
    }

    private void travelFromKosmos(ServerLevel kosmos, BlockPos kosmosDoorPos, ServerPlayer player) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        GlobalPos external = data.externalDoorFor(kosmosDoorPos);
        ServerLevel dest;
        BlockPos destDoorPos;

        if (external == null) {
            // A room with no link at all: a player built this one themselves,
            // from scratch, inside the Kosmos - reverses the usual formula.
            // Instead of connecting to an existing overworld door, spawn a
            // brand new one somewhere random on the overworld surface and
            // link the two, turning the Kosmos into a shortcut for reaching
            // genuinely distant, unexplored territory.
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            BlockPos newDoorPos = buildRandomOverworldRoomCopy(overworld);
            if (newDoorPos == null) {
                return;   // couldn't find anywhere suitable after several tries - leave the player where they are
            }
            data.link(GlobalPos.of(overworld.dimension(), newDoorPos), kosmosDoorPos);
            dest = overworld;
            destDoorPos = newDoorPos;
        } else {
            dest = player.getServer().getLevel(external.dimension());
            if (dest == null) {
                return;
            }
            destDoorPos = external.pos();
        }

        BlockState doorState = dest.getBlockState(destDoorPos);
        Direction into = doorState.getBlock() instanceof KraveDoorBlock
                ? validRoomDirection(dest, destDoorPos, doorState.getValue(FACING))
                : null;
        if (into == null) {
            return;   // the far room was torn down or altered since - nothing sane to teleport into
        }

        teleportInto(player, dest, destDoorPos, into, false);
    }

    /**
     * Picks a random point 2000-6000 blocks out from the origin, in a random
     * direction, and looks for flat, clear overworld surface near it - the
     * same flatness/clearance check {@link #buildKosmosRoomCopy} uses for
     * the Kosmos, just aimed somewhere genuinely far from wherever the
     * player has already been rather than a fixed spot. Retries a handful of
     * times with fresh random points before giving up, since a single roll
     * can easily land in open ocean or a cliff face.
     */
    private BlockPos buildRandomOverworldRoomCopy(ServerLevel overworld) {
        var random = java.util.concurrent.ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 2000.0D + random.nextDouble() * 4000.0D;
            Vec3 center = new Vec3(Math.cos(angle) * distance, 64.0D, Math.sin(angle) * distance);

            Vec3 spot = KraveLanding.findClearLanding(overworld, center, 16, 3, 3, 5)
                    .or(() -> KraveLanding.findLanding(overworld, center, 10))
                    .orElse(null);
            if (spot == null) {
                continue;
            }
            BlockPos doorLowerPos = BlockPos.containing(spot.x, spot.y, spot.z);
            placeRoomShell(overworld, doorLowerPos, AUTO_ROOM_INTO);
            return doorLowerPos;
        }
        return null;
    }

    private void teleportInto(ServerPlayer player, ServerLevel dest, BlockPos doorLowerPos,
                              Direction into, boolean enteringKosmos) {
        BlockPos interior = doorLowerPos.relative(into);
        Vec3 target = new Vec3(interior.getX() + 0.5D, interior.getY(), interior.getZ() + 0.5D);
        float yRot = into.getOpposite().toYRot();   // face back toward the door you just walked through

        // Force-loads the arrival chunks server-side, before the client ever
        // asks for them - the part of "loading terrain" that's actually
        // within a mod's reach without deep client-side changes.
        preloadChunks(dest, doorLowerPos);

        // Only companions actually within a few blocks at the moment the
        // door shut count as having "run in with you" and travel instantly.
        // Anyone farther off gets a real grace period below instead of being
        // yanked along regardless of distance, which never read as them
        // running anywhere at all.
        List<Entity> withPlayer = com.barbarajones.dimension.PetEscort.gatherWithin(player, COMPANION_MUST_BE_CLOSE_RADIUS);
        List<Entity> stragglers = com.barbarajones.dimension.PetEscort.gather(player);
        stragglers.removeIf(withPlayer::contains);

        player.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                            java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(target, Vec3.ZERO, yRot, entity.getXRot());
            }
        });
        player.fallDistance = 0.0F;

        for (Entity arrived : com.barbarajones.dimension.PetEscort.deliver(withPlayer, dest, target)) {
            if (enteringKosmos && arrived instanceof CaydenCobb cayden) {
                cayden.onEnterKosmos();
            }
        }

        for (Entity straggler : stragglers) {
            KraveDoorScheduler.schedule(STRAGGLER_DELAY_TICKS, () -> {
                if (straggler == null || !straggler.isAlive() || straggler.level() == dest) {
                    return;   // already gone, or already got there some other way
                }
                List<Entity> late = com.barbarajones.dimension.PetEscort.deliver(List.of(straggler), dest, target);
                if (late.isEmpty()) {
                    return;
                }
                for (Entity arrived : late) {
                    if (enteringKosmos && arrived instanceof CaydenCobb cayden) {
                        cayden.onEnterKosmos();
                    }
                }
                dest.playSound(null, doorLowerPos, ModSounds.KRAVE_ROAR.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            });
        }
    }

    /** Loads (and generates, if needed) the 3x3 chunk block around the arrival point ahead of the player actually landing there. */
    private void preloadChunks(ServerLevel level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }

    /**
     * Builds a fresh partner room somewhere clear in the Kosmos and hands
     * back its door's lower position - the caller links it to whichever
     * overworld door asked for it. Reuses the same mountain/crevice-avoiding
     * search the old landing system used, since the requirements are
     * identical: a flat, open pocket big enough for the room to actually fit
     * without ending up half-buried.
     */
    private BlockPos buildKosmosRoomCopy(ServerLevel kosmos) {
        Vec3 spot = KraveLanding.findClearLanding(kosmos, KraveDimensions.PORTAL_LANDING, 12, 3, 2, 5)
                .or(() -> KraveLanding.findLanding(kosmos, KraveDimensions.PORTAL_LANDING, 6))
                .orElse(null);
        if (spot == null) {
            return null;
        }
        BlockPos doorLowerPos = BlockPos.containing(spot.x, spot.y, spot.z);
        placeRoomShell(kosmos, doorLowerPos, AUTO_ROOM_INTO);
        ensureLandingBoxesExist(kosmos, spot);
        return doorLowerPos;
    }

    /**
     * The Kosmos always has exactly one Krave Monster on door-entry - spawn
     * him near the boss island the first time, never again after that as
     * long as he's alive.
     *
     * <p>{@code Level#getEntity(UUID)} only finds entities in currently
     * LOADED chunks - if nobody has been near the den for a while, its
     * chunks unload and he'd read as "not found" even though he's still
     * alive out there, which meant a fresh one got spawned on every single
     * entry instead of just the first. Force-loading his home chunk first
     * (the same one-line trick {@code KraveLanding} already uses for
     * terrain) fixes the common case - him just standing near where he
     * spawned, not actively chasing a player somewhere else entirely.
     */
    private void ensureBossExists(ServerLevel kosmos) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        var id = data.getBossId();
        if (id != null) {
            Vec3 den = KraveDimensions.BOSS_ISLAND;
            kosmos.getChunkAt(BlockPos.containing(den.x, den.y, den.z));
            var existing = kosmos.getEntity(id);
            if (existing instanceof KraveMonster monster && monster.isAlive()) {
                return;
            }
        }
        Vec3 pos = KraveDimensions.BOSS_ISLAND;
        BlockPos denCenter = BlockPos.containing(pos.x, pos.y, pos.z);
        // Build the den (and its guaranteed-solid platform) before the boss
        // spawns, so he lands on authored ground rather than whatever the
        // procedural terrain happened to generate at the origin.
        com.barbarajones.dimension.KraveDenBuilder.buildDen(kosmos, denCenter);

        KraveMonster monster = ModEntities.KRAVE_MONSTER.get().create(kosmos);
        if (monster == null) {
            return;
        }
        monster.setPos(pos.x, pos.y, pos.z);
        kosmos.addFreshEntity(monster);
        data.setBossId(monster.getUUID());
        // The den's healing boxes were built before the boss existed, so they
        // had no target to latch onto yet - KraveHealingBox.resolveTarget()'s
        // KraveKosmosData fallback picks him up automatically on their next
        // heal tick now that setBossId() above has run, no extra wiring needed.
    }

    /**
     * The four ordinary healing boxes ring the landing area - guarded by a
     * flag in KraveKosmosData so they're only ever placed once ever, no
     * matter how many separate portal rooms later get built.
     *
     * <p>Each spot uses {@link KraveLanding#findOpenLanding}, not the plain
     * search: it rejects candidates too close to a box already placed this
     * pass (so they read as spread out instead of clustering wherever the
     * spiral search happens to land first) and candidates boxed in against a
     * wall or cliff (so nothing ends up half-buried in a mountainside). The
     * cardinal offsets are just four different starting points to search
     * outward from - the actual final spot is whatever passes both checks
     * nearest that direction, not that exact point.
     */
    private void ensureLandingBoxesExist(ServerLevel kosmos, Vec3 landing) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        if (data.isLandingBoxesSpawned()) {
            return;
        }
        data.setLandingBoxesSpawned(true);

        var bossId = data.getBossId();
        KraveMonster boss = bossId != null && kosmos.getEntity(bossId) instanceof KraveMonster m ? m : null;

        int[][] offsets = { {18, 0}, {-18, 0}, {0, 18}, {0, -18} };
        List<Vec3> placed = new ArrayList<>();
        for (int[] off : offsets) {
            Vec3 seed = landing.add(off[0], 0.0D, off[1]);
            var spot = KraveLanding.findOpenLanding(kosmos, seed, 4, placed, 14.0D);
            if (spot.isEmpty()) {
                continue;
            }
            placed.add(spot.get());

            KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
            if (box == null) {
                continue;
            }
            Vec3 pos = spot.get();
            box.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            if (boss != null) {
                box.setHealTarget(boss);
            }
            kosmos.addFreshEntity(box);
        }
    }

    private CompoundTag persisted(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
