package com.barbarajones.apocalypse;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveLanding;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMinion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

/**
 * Ambient Kosmonaut (Krave Minion) presence on the Krave Kosmos islands - a
 * second, always-on instance of KraveKosmosBattle's proven tick-spawner idea,
 * scoped to ordinary exploration instead of the scripted Super Saiyan fight,
 * so there's actually something to run into out on the islands.
 */
public final class KraveKosmosAmbience {

    private static final int TICK_INTERVAL = 100;
    private static final int PER_PLAYER_CAP = 3;
    private static final double SCAN_RADIUS = 64.0D;
    /** Cube radius around each player to check for KraveCavePocketFeature's BARRIER markers. */
    private static final int MARKER_SCAN_RADIUS = 6;
    /** Ambient healing-box scatter: independent of worldgen features, so it
     *  reaches terrain generated before those features existed too. */
    private static final int HEALING_BOX_CAP = 2;
    private static final double HEALING_BOX_SCAN_RADIUS = 80.0D;
    private static final int HEALING_BOX_SPAWN_CHANCE = 15;

    private static int timer;

    private KraveKosmosAmbience() { }

    public static void tick() {
        if (--timer > 0) {
            return;
        }
        timer = TICK_INTERVAL;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerLevel kosmos = server.getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null) {
            return;
        }

        for (ServerPlayer player : kosmos.players()) {
            int nearby = kosmos.getEntitiesOfClass(KraveMinion.class,
                    player.getBoundingBox().inflate(SCAN_RADIUS)).size();
            if (nearby < PER_PLAYER_CAP) {
                spawnNear(kosmos, player);
            }
            scanForCaveMarkers(kosmos, player);
            maybeSpawnHealingBox(kosmos, player);
        }
    }

    /**
     * Terrain-independent healing box scatter. The worldgen-placed ones (den
     * ring, mountain chambers, valley floors) only ever reach chunks
     * generated after those features were added - a save that already
     * explored the Kosmos would never see them. This runs at ordinary tick
     * time near wherever the player is actually standing, so it reaches the
     * whole dimension, old terrain included, the same way spawnNear already
     * does for Kosmonauts.
     */
    private static void maybeSpawnHealingBox(ServerLevel kosmos, ServerPlayer player) {
        int nearby = kosmos.getEntitiesOfClass(KraveHealingBox.class,
                player.getBoundingBox().inflate(HEALING_BOX_SCAN_RADIUS)).size();
        if (nearby >= HEALING_BOX_CAP || kosmos.random.nextInt(HEALING_BOX_SPAWN_CHANCE) != 0) {
            return;
        }
        double angle = kosmos.random.nextDouble() * Math.PI * 2.0D;
        double dist = 24.0D + kosmos.random.nextDouble() * 40.0D;
        Vec3 seed = player.position().add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);

        Optional<Vec3> landing = KraveLanding.findLanding(kosmos, seed, 2);
        if (landing.isEmpty()) {
            return;
        }
        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
        if (box == null) {
            return;
        }
        Vec3 pos = landing.get();
        box.moveTo(pos.x, pos.y, pos.z, kosmos.random.nextFloat() * 360.0F, 0.0F);
        kosmos.addFreshEntity(box);
    }

    /**
     * KraveCavePocketFeature carves its pocket and leaves a lone BARRIER block
     * as a marker rather than spawning the healing box itself (worldgen code
     * shouldn't spawn entities) - this finds those markers at ordinary tick
     * time and does the actual spawn.
     */
    private static void scanForCaveMarkers(ServerLevel kosmos, ServerPlayer player) {
        BlockPos center = player.blockPosition();
        for (int dx = -MARKER_SCAN_RADIUS; dx <= MARKER_SCAN_RADIUS; dx++) {
            for (int dy = -MARKER_SCAN_RADIUS; dy <= MARKER_SCAN_RADIUS; dy++) {
                for (int dz = -MARKER_SCAN_RADIUS; dz <= MARKER_SCAN_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (kosmos.getBlockState(pos).is(Blocks.BARRIER)) {
                        activateCaveMarker(kosmos, pos);
                    }
                }
            }
        }
    }

    private static void activateCaveMarker(ServerLevel kosmos, BlockPos pos) {
        kosmos.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
        if (box != null) {
            box.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            kosmos.addFreshEntity(box);
        }
        kosmos.addFreshEntity(new ItemEntity(kosmos, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                new ItemStack(ModItems.KRAVE_CEREAL.get(), 2 + kosmos.random.nextInt(3))));
    }

    private static void spawnNear(ServerLevel kosmos, ServerPlayer player) {
        double angle = kosmos.random.nextDouble() * Math.PI * 2.0D;
        double dist = 20.0D + kosmos.random.nextDouble() * 20.0D;
        Vec3 seed = player.position().add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);

        Optional<Vec3> landing = KraveLanding.findLanding(kosmos, seed, 2);
        if (landing.isEmpty()) {
            return;   // no solid ground found nearby this pass - try again next tick cycle
        }
        KraveMinion minion = ModEntities.KRAVE_MINION.get().create(kosmos);
        if (minion == null) {
            return;
        }
        Vec3 pos = landing.get();
        minion.moveTo(pos.x, pos.y, pos.z, kosmos.random.nextFloat() * 360.0F, 0.0F);
        kosmos.addFreshEntity(minion);
    }
}
