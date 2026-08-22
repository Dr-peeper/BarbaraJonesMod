package com.barbarajones.dimension;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Drags the player's companions through a dimension change with them.
 *
 * Vanilla leaves pets behind on any dimension change that isn't a nether
 * portal, so the Krave Door alone would move the player without them.
 * Cayden is the entire point of the Kosmos trip, so he has to come.
 *
 * Gather BEFORE moving the player: once they are gone, "near the player" no
 * longer resolves in the level we need to search.
 */
public final class PetEscort {

    /** Companions within this many blocks of the player travel too. */
    private static final double RADIUS = 24.0D;

    private PetEscort() { }

    /** Snapshot the companions that should travel with this player. */
    public static List<Entity> gather(ServerPlayer player) {
        return gatherWithin(player, RADIUS);
    }

    /** Same as {@link #gather}, but with a caller-chosen radius instead of the default 24 blocks. */
    public static List<Entity> gatherWithin(ServerPlayer player, double radius) {
        List<Entity> out = new ArrayList<>();
        ServerLevel from = player.serverLevel();

        for (CaydenCobb cayden : from.getEntitiesOfClass(CaydenCobb.class,
                player.getBoundingBox().inflate(radius))) {
            if (cayden.isAlive() && cayden.isTame() && cayden.getOwner() == player) {
                out.add(cayden);
            }
        }
        for (BarbaraJones barbara : from.getEntitiesOfClass(BarbaraJones.class,
                player.getBoundingBox().inflate(radius))) {
            if (barbara.isAlive() && barbara.isPet() && barbara.getPetOwner() == player) {
                out.add(barbara);
            }
        }
        return out;
    }

    /**
     * Everything actually inside a bounded region - the Krave Door's portal
     * room, specifically - not filtered by ownership or type the way
     * {@link #gather}/{@link #gatherWithin} are. A real portal takes whoever
     * is standing in it, not just the one person who used it and their own
     * pets; {@code exclude} is the player themselves, since they travel
     * through {@code changeDimension} directly rather than through this list.
     */
    public static List<Entity> gatherRoomOccupants(ServerLevel level, AABB box, Entity exclude) {
        List<Entity> out = new ArrayList<>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (entity != exclude && entity.isAlive()) {
                out.add(entity);
            }
        }
        return out;
    }

    /**
     * Move gathered companions to {@code dest}, all landing at the exact same
     * spot with no fan-out - the Krave Door's portal room is a single 1-wide
     * column, so any spread-out landing offset would put someone inside a
     * wall. Multiple entities briefly sharing a block is harmless; they path
     * apart on their own the moment they start moving.
     *
     * @return the companions as they exist at the destination. Cross-dimension
     *         travel discards the original entity and rebuilds a copy, so the
     *         references passed in are dead afterwards and must not be reused.
     */
    public static List<Entity> deliverTogether(List<Entity> pets, ServerLevel dest, Vec3 landing) {
        List<Entity> arrived = new ArrayList<>();
        for (Entity pet : pets) {
            if (pet == null || !pet.isAlive()) {
                continue;
            }
            Entity moved = moveOne(pet, dest, landing);
            if (moved != null) {
                arrived.add(moved);
            }
        }
        return arrived;
    }

    private static Entity moveOne(Entity pet, ServerLevel dest, Vec3 spot) {
        Entity moved;
        if (pet.level() == dest) {
            pet.teleportTo(spot.x, spot.y, spot.z);
            moved = pet;
        } else {
            moved = pet.changeDimension(dest, new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                                Function<ServerLevel, PortalInfo> defaultInfo) {
                    return new PortalInfo(spot, Vec3.ZERO, entity.getYRot(), entity.getXRot());
                }
            });
        }
        if (moved != null) {
            moved.fallDistance = 0.0F;
        }
        return moved;
    }
}
