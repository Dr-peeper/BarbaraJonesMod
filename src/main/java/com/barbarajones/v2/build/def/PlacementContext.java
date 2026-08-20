package com.barbarajones.v2.build.def;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Everything a {@link CompletionHook} needs to know about the building that
 * just went up: where it is, which way it faces, who put it there, and how to
 * translate the local coordinates from the definition into real world ones.
 */
public final class PlacementContext {

    private final ServerLevel level;
    private final StructureDef def;
    private final Rotation rotation;
    /** World position of the rotated footprint's minimum X/Z corner, at Y = the build plane. */
    private final BlockPos origin;
    private final BlockPos anchor;
    private final UUID placer;
    private final List<UUID> spawned = new ArrayList<>();

    public PlacementContext(ServerLevel level, StructureDef def, Rotation rotation,
                            BlockPos origin, BlockPos anchor, UUID placer) {
        this.level = level;
        this.def = def;
        this.rotation = rotation;
        this.origin = origin;
        this.anchor = anchor;
        this.placer = placer;
    }

    public ServerLevel level() {
        return level;
    }

    public StructureDef def() {
        return def;
    }

    public Rotation rotation() {
        return rotation;
    }

    /** Minimum X/Z corner of the rotated footprint. Its Y is the build plane (local y = 0). */
    public BlockPos origin() {
        return origin;
    }

    /** The block the player was pointing at. */
    public BlockPos anchor() {
        return anchor;
    }

    /** UUID of the player who placed it, or null if something else did (a command, worldgen). */
    public UUID placer() {
        return placer;
    }

    /** Which way the building's front faces in the world. */
    public Direction front() {
        return StructureGeometry.front(rotation);
    }

    /** Translates a local position from the definition into a world position. */
    public BlockPos world(int localX, int localY, int localZ) {
        return world(def, rotation, origin, localX, localY, localZ);
    }

    public BlockPos world(BlockPos local) {
        return world(local.getX(), local.getY(), local.getZ());
    }

    /** World position of a {@link StructureDef.Builder#marker} , or null if that name was never set. */
    public BlockPos marker(String name) {
        BlockPos local = def.markers().get(name);
        return local == null ? null : world(local);
    }

    /** World position of the building's core block. */
    public BlockPos core() {
        return world(def.core());
    }

    /** Called by the spawn hooks so an undo can clean the entity up again. */
    public void trackSpawned(Entity entity) {
        spawned.add(entity.getUUID());
    }

    public List<UUID> spawnedEntities() {
        return spawned;
    }

    /**
     * The one true local-to-world transform. Everything - the placer, the ghost
     * preview, the undo pass, the hooks - goes through here.
     */
    public static BlockPos world(StructureDef def, Rotation rotation, BlockPos origin,
                                 int localX, int localY, int localZ) {
        int spanX = def.spanX();
        int spanZ = def.spanZ();
        int nx = localX - def.localBounds().minX();
        int nz = localZ - def.localBounds().minZ();
        int rx = StructureGeometry.rotateX(nx, nz, spanX, spanZ, rotation);
        int rz = StructureGeometry.rotateZ(nx, nz, spanX, spanZ, rotation);
        return new BlockPos(origin.getX() + rx, origin.getY() + localY, origin.getZ() + rz);
    }
}
