package com.barbarajones.dimension;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Drags the player's companions through a dimension change with them.
 *
 * Vanilla leaves pets behind on any dimension change that isn't a nether
 * portal, so both the Krave Door and the Krave Tether moved the player alone.
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
        List<Entity> out = new ArrayList<>();
        ServerLevel from = player.serverLevel();

        for (CaydenCobb cayden : from.getEntitiesOfClass(CaydenCobb.class,
                player.getBoundingBox().inflate(RADIUS))) {
            if (cayden.isAlive() && cayden.isTame() && cayden.getOwner() == player) {
                out.add(cayden);
            }
        }
        for (BarbaraJones barbara : from.getEntitiesOfClass(BarbaraJones.class,
                player.getBoundingBox().inflate(RADIUS))) {
            if (barbara.isAlive() && barbara.isPet() && barbara.getPetOwner() == player) {
                out.add(barbara);
            }
        }
        return out;
    }

    /**
     * Move gathered companions to {@code dest} around {@code landing}.
     *
     * @return the companions as they exist at the destination. Cross-dimension
     *         travel discards the original entity and rebuilds a copy, so the
     *         references passed in are dead afterwards and must not be reused.
     */
    public static List<Entity> deliver(List<Entity> pets, ServerLevel dest, Vec3 landing) {
        List<Entity> arrived = new ArrayList<>();
        int index = 0;

        for (Entity pet : pets) {
            if (pet == null || !pet.isAlive()) {
                continue;
            }
            // fan them out a little so they don't land stacked inside each other
            double angle = (Math.PI * 2.0D / Math.max(1, pets.size())) * index++;
            final Vec3 spot = landing.add(Math.cos(angle) * 1.5D, 0.0D, Math.sin(angle) * 1.5D);

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
                arrived.add(moved);
            }
        }
        return arrived;
    }
}
