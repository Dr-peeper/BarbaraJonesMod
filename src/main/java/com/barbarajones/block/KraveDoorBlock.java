package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.dimension.KraveLanding;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * The Krave Kosmos portal: a 3-wide x 3-tall frame of {@link ModBlocks#KRAVE_BLOCK},
 * bottom-middle two cells replaced by this door. Until a complete frame
 * surrounds it, it is just a door - opens, closes, shows the room behind it,
 * same as any other. Once the chocolate threshold is complete, it stops
 * being a physical door at all: "opening" it plays the door-open sound and
 * immediately travels the player instead of ever swinging open. It never
 * shows what's on the other side, on either end of the trip - that's
 * deliberate, not a missing animation. Bidirectional like a Nether portal:
 * from anywhere else, a complete frame sends you into the Kosmos and builds
 * a matching frame+door right behind where you land; from inside the
 * Kosmos, a complete frame sends you back to wherever you stepped in from.
 * No consumable item required either direction - the door you arrive next
 * to IS the way back, not a Krave Tether (that item still exists for quest/
 * loot reasons elsewhere in the mod, it's just no longer load-bearing here).
 */
public class KraveDoorBlock extends DoorBlock {

    public KraveDoorBlock(BlockBehaviour.Properties props, BlockSetType setType) {
        super(props, setType);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);

        if (!isFrameComplete(level, lowerPos, lowerState, player)) {
            // No threshold yet - an ordinary door, vanilla behavior untouched.
            return super.use(state, level, pos, player, hand, hit);
        }

        // Threshold complete: never call super.use() here, so OPEN never
        // flips and the door never visually swings - only the sound plays,
        // then the player travels directly.
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            level.playSound(null, lowerPos, type().doorOpen(), SoundSource.BLOCKS,
                    1.0F, level.random.nextFloat() * 0.1F + 0.9F);
            if (level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
                returnHome(serverPlayer);
            } else {
                enterKosmos(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * The Kosmos side keeps the old flat 3x3 frame - it has to, since that's
     * all {@link #buildReturnPortal} ever puts down. The overworld side now
     * needs a real enclosed chocolate room around the door, not just a
     * frame someone could build in open air with nothing behind it.
     */
    private boolean isFrameComplete(Level level, BlockPos lowerPos, BlockState lowerState, Player player) {
        Direction facing = lowerState.getValue(FACING);
        if (level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
            return isFlatFrameComplete(level, lowerPos, facing);
        }
        return isChocolateRoomComplete(level, lowerPos, facing, player);
    }

    private boolean isFlatFrameComplete(Level level, BlockPos lowerPos, Direction facing) {
        Direction side = facing.getClockWise();

        BlockPos left = lowerPos.relative(side.getOpposite());
        BlockPos right = lowerPos.relative(side);

        return isKraveBlock(level, lowerPos.above(2))
                && isKraveBlock(level, left) && isKraveBlock(level, left.above()) && isKraveBlock(level, left.above(2))
                && isKraveBlock(level, right) && isKraveBlock(level, right.above()) && isKraveBlock(level, right.above(2));
    }

    /**
     * A fully enclosed 3-wide x 3-deep x 3-tall chocolate room, door set into
     * the middle of one wall - floor and roof included, only the one column
     * directly behind the door left hollow to actually stand in. Tried on
     * both sides of the door plane, since a player building this has no way
     * to know which way {@code FACING} happens to point.
     */
    private boolean isChocolateRoomComplete(Level level, BlockPos lowerPos, Direction doorFacing, Player player) {
        String failA = isRoomExtending(level, lowerPos, doorFacing);
        if (failA == null) {
            return true;
        }
        String failB = isRoomExtending(level, lowerPos, doorFacing.getOpposite());
        if (failB == null) {
            return true;
        }
        // TEMPORARY diagnostic (see the "the door only makes a sound" report) -
        // reports the first thing wrong on whichever of the two attempts got
        // further, so we can see exactly what a real, in-game "complete" room
        // is missing instead of guessing blind. Strip once confirmed fixed.
        if (!level.isClientSide && player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "[Krave Door] " + failA + " | " + failB), false);
        }
        return false;
    }

    /** Returns null if the room is complete extending {@code into}, or a description of the first problem found. */
    private String isRoomExtending(Level level, BlockPos lowerPos, Direction into) {
        Direction side = into.getClockWise();
        for (int depth = 0; depth <= 2; depth++) {
            for (int across = -1; across <= 1; across++) {
                for (int row = -1; row <= 3; row++) {
                    BlockPos cell = lowerPos.relative(into, depth).relative(side, across).above(row);
                    boolean isFloorOrRoof = row == -1 || row == 3;
                    boolean isDoorCell = !isFloorOrRoof && depth == 0 && across == 0 && row <= 1;
                    boolean isInterior = !isFloorOrRoof && !isDoorCell && depth == 1 && across == 0;

                    if (isDoorCell) {
                        if (!level.getBlockState(cell).is(ModBlocks.KRAVE_DOOR.get())) {
                            return "expected door at " + cell.toShortString() + " (into=" + into
                                    + "), found " + level.getBlockState(cell).getBlock();
                        }
                    } else if (isInterior) {
                        if (level.getBlockState(cell).blocksMotion()) {
                            return "expected open interior at " + cell.toShortString() + " (into=" + into
                                    + "), found " + level.getBlockState(cell).getBlock();
                        }
                    } else if (!isKraveBlock(level, cell)) {
                        return "expected chocolate block at " + cell.toShortString() + " (into=" + into
                                + ", depth=" + depth + ", across=" + across + ", row=" + row
                                + "), found " + level.getBlockState(cell).getBlock();
                    }
                }
            }
        }
        return null;
    }

    private boolean isKraveBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.KRAVE_BLOCK.get());
    }

    private void enterKosmos(ServerPlayer player) {
        ServerLevel dest = player.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (dest == null) {
            return;
        }
        ensureBossExists(dest);

        // Remember where (and in which dimension) the player stepped in, so
        // opening the door we're about to build sends them straight back.
        CompoundTag persist = persisted(player);
        persist.putString("KraveReturnDim", player.level().dimension().location().toString());
        persist.putDouble("KraveReturnX", player.getX());
        persist.putDouble("KraveReturnY", player.getY());
        persist.putDouble("KraveReturnZ", player.getZ());

        // radius 3 (covers the return portal's full reach: 2 blocks behind the
        // landing spot, plus 1 either side for the frame), height variance 2
        // (tolerates gentle unevenness, rejects a mountain peak/slope or a
        // crevice floor), clearance 4 (room for the player plus the 3-tall
        // frame above). Falls back to the plain search, then the fixed point,
        // rather than ever leaving a player stuck mid-interaction.
        Vec3 landing = KraveLanding.findClearLanding(dest, KraveDimensions.PORTAL_LANDING, 10, 3, 2, 4)
                .or(() -> KraveLanding.findLanding(dest, KraveDimensions.PORTAL_LANDING, 6))
                .orElse(KraveDimensions.PORTAL_LANDING);
        ensureLandingBoxesExist(dest, landing);
        buildReturnPortal(dest, landing, player.getYRot());

        // Gather the companions BEFORE the player leaves - afterwards they are
        // no longer "near the player" in any level we can search.
        List<Entity> escort = com.barbarajones.dimension.PetEscort.gather(player);

        player.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                            java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(landing, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }
        });
        // Defense-in-depth: if the bounded search above somehow failed (should
        // be effectively unreachable given the guaranteed-landmass assumption
        // near the dimension origin), Slow Falling still keeps a fallback
        // fixed-point landing non-fatal instead of a hard crash-into-void.
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));

        // Cayden comes too. He is the reason to be here at all.
        for (Entity arrived : com.barbarajones.dimension.PetEscort.deliver(escort, dest, landing)) {
            if (arrived instanceof com.barbarajones.entity.CaydenCobb cayden) {
                cayden.onEnterKosmos();
            }
        }
    }

    /**
     * Mirrors what KraveTetherItem does, minus consuming an item: read the
     * player's persisted arrival point and step them back to it. Only
     * reachable by opening a COMPLETE frame (see use() above), so it can't
     * be triggered by a lone door someone dropped somewhere.
     *
     * <p>Goes through {@code changeDimension}/{@code ITeleporter}, the same
     * mechanism {@link #enterKosmos} uses, rather than the simpler {@code
     * Player#teleportTo(ServerLevel, ...)} this used to call - that more
     * primitive path was cutting off the door-open sound queued a moment
     * earlier in {@code use()}: it does a harder, more immediate level swap
     * than the portal-teleporter path does, and apparently doesn't give the
     * sound packet time to actually reach the client before the dimension
     * changes out from under it. Entering never had this problem because it
     * already went through {@code changeDimension}.
     */
    private void returnHome(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            return;
        }
        CompoundTag persist = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persist.contains("KraveReturnDim")) {
            return;
        }

        ResourceKey<Level> returnDim = ResourceKey.create(Registries.DIMENSION,
                new ResourceLocation(persist.getString("KraveReturnDim")));
        ServerLevel dest = player.getServer().getLevel(returnDim);
        if (dest == null) {
            return;
        }

        double x = persist.getDouble("KraveReturnX");
        double y = persist.getDouble("KraveReturnY");
        double z = persist.getDouble("KraveReturnZ");
        Vec3 target = new Vec3(x, y, z);
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        List<Entity> escort = com.barbarajones.dimension.PetEscort.gather(player);
        player.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                            java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(target, Vec3.ZERO, yRot, xRot);
            }
        });
        com.barbarajones.dimension.PetEscort.deliver(escort, dest, target);
    }

    /**
     * A complete Krave Block frame + open-able Krave Door, built once right
     * behind wherever the player is about to land - facing back toward the
     * landing spot so turning around after arriving puts it in view. Reuses
     * the exact same frame shape isFrameComplete() checks, so it's a real,
     * independently valid portal the moment it's placed, not a decoration.
     */
    private void buildReturnPortal(ServerLevel dest, Vec3 landing, float playerYRot) {
        Direction facing = Direction.fromYRot(playerYRot).getOpposite();
        BlockPos landingPos = BlockPos.containing(landing.x, landing.y, landing.z);
        BlockPos doorLower = landingPos.relative(facing.getOpposite(), 2);

        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        Direction side = facing.getClockWise();
        BlockPos left = doorLower.relative(side.getOpposite());
        BlockPos right = doorLower.relative(side);

        dest.setBlock(doorLower, Blocks.AIR.defaultBlockState(), 3);
        dest.setBlock(doorLower.above(), Blocks.AIR.defaultBlockState(), 3);
        dest.setBlock(doorLower.above(2), frame, 3);
        dest.setBlock(left, frame, 3);
        dest.setBlock(left.above(), frame, 3);
        dest.setBlock(left.above(2), frame, 3);
        dest.setBlock(right, frame, 3);
        dest.setBlock(right.above(), frame, 3);
        dest.setBlock(right.above(2), frame, 3);

        BlockState doorState = ModBlocks.KRAVE_DOOR.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HINGE, DoorHingeSide.LEFT)
                .setValue(OPEN, Boolean.FALSE);
        dest.setBlock(doorLower, doorState.setValue(HALF, DoubleBlockHalf.LOWER), 3);
        dest.setBlock(doorLower.above(), doorState.setValue(HALF, DoubleBlockHalf.UPPER), 3);
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
     * The four ordinary healing boxes ring the landing island - the larger
     * island players actually arrive on and explore - rather than the den,
     * which now has just its one elite guardian (see KraveDenBuilder). Same
     * one-time-authoring pattern as ensureBossExists: guarded by a flag in
     * KraveKosmosData so re-entering the Kosmos never duplicates them.
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
