package com.barbarajones.apocalypse;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveLanding;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.v2.mobs.ModMobEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ambient wildlife on the Krave Kosmos islands - a second, always-on
 * instance of KraveKosmosBattle's proven tick-spawner idea, scoped to
 * ordinary exploration instead of the scripted Super Saiyan fight, so
 * there's actually something to run into out there.
 *
 * <p>Spawns from the whole non-human Krave-creature roster (Krave Minion
 * plus the Kraveling family and Kravajo), not just Krave Minion alone - one
 * shared rate and cap across all of them, the same "ambient wildlife
 * density" a player would get from any one of them individually. Krave
 * Monster, Barbara/Cayden/Daniel/the Plug and the rest of the human cast are
 * deliberately not in this pool - they're characters, not wildlife.
 */
public final class KraveKosmosAmbience {

    private static final int TICK_INTERVAL = 27;
    private static final int PER_PLAYER_CAP = 15;
    /** How many new creatures one pass is allowed to add per player, so filling up to the cap isn't one giant burst. */
    private static final int SPAWNS_PER_PASS = 5;
    private static final double SCAN_RADIUS = 64.0D;
    /** Cube radius around each player to check for KraveCavePocketFeature's BARRIER markers. */
    private static final int MARKER_SCAN_RADIUS = 6;

    // Kravajo appears three times over - it's meant to be the "there will be
    // a lot of them" pest (see its own class javadoc and getSoundVolume()
    // comment), so it's weighted well above the rest of the pool rather than
    // getting an equal one-in-N shot at each spawn roll.
    private static final List<Supplier<EntityType<? extends Mob>>> CREATURE_TYPES = List.of(
            ModEntities.KRAVE_MINION::get,
            ModMobEntities.KRAVELING::get,
            ModMobEntities.KRISPBONE::get,
            ModMobEntities.LOOMWEAVER::get,
            ModMobEntities.SOGGY::get,
            ModMobEntities.KRAVAJO::get,
            ModMobEntities.KRAVAJO::get,
            ModMobEntities.KRAVAJO::get,
            ModMobEntities.MASCOT::get
    );

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
            int nearby = kosmos.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SCAN_RADIUS),
                    KraveKosmosAmbience::isKosmosCreature).size();
            int toSpawn = Math.min(SPAWNS_PER_PASS, PER_PLAYER_CAP - nearby);
            for (int i = 0; i < toSpawn; i++) {
                spawnNear(kosmos, player);
            }
            scanForCaveMarkers(kosmos, player);
        }
    }

    private static boolean isKosmosCreature(Mob mob) {
        for (Supplier<EntityType<? extends Mob>> type : CREATURE_TYPES) {
            if (mob.getType() == type.get()) {
                return true;
            }
        }
        return false;
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
        EntityType<? extends Mob> type = CREATURE_TYPES.get(kosmos.random.nextInt(CREATURE_TYPES.size())).get();
        Mob mob = type.create(kosmos);
        if (mob == null) {
            return;
        }
        Vec3 pos = landing.get();
        mob.moveTo(pos.x, pos.y, pos.z, kosmos.random.nextFloat() * 360.0F, 0.0F);
        kosmos.addFreshEntity(mob);
    }
}
