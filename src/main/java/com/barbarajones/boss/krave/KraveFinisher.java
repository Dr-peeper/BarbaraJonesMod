package com.barbarajones.boss.krave;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The geometry of the sky finisher: where Cayden launches from, and whether he
 * is anywhere he has any business being.
 *
 * <p>The finisher used to start from wherever the fight happened to have left
 * him. That is why it never read as anything - the prompt appeared the instant
 * the boss hit the threshold, and answering it produced a throw from across the
 * arena, or from inside the castle, or from under the island. The throw has a
 * shape now: Cayden climbs to a specific point in open air above the boss, and
 * nothing is offered to the player until he is standing in it.
 *
 * <p>The launch point is searched for rather than assumed. A fixed offset above
 * the boss is inside the castle roof about half the time, and a fixed Y is
 * meaningless in a dimension of floating islands.
 */
public final class KraveFinisher {

    private KraveFinisher() { }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** Preferred height above the boss's head. */
    private static final int WANT_ABOVE = 22;

    /** Never closer than this, or the throw has no room to read as a fall. */
    private static final int MIN_ABOVE = 14;

    /** Nor further, or the player is a dot and the boss is out of sight. */
    private static final int MAX_ABOVE = 34;

    /** Clear air needed around the launch point for the two of them plus the toss. */
    private static final int CLEAR_RADIUS = 2;
    private static final int CLEAR_HEIGHT = 4;

    /**
     * A point in open sky above the boss to throw from.
     *
     * <p>Searched downward from the generous end so the highest workable point
     * wins: under a castle roof the only clear air may be a narrow band, and
     * starting from the preferred height and giving up would put the launch
     * inside the stonework.
     *
     * @return the launch position, or null if there is genuinely nowhere - which
     *         means the boss is buried, and the caller should recover rather
     *         than throw into a ceiling
     */
    @Nullable
    public static Vec3 launchPoint(ServerLevel level, KraveMonster boss) {
        double cx = boss.getX();
        double cz = boss.getZ();
        int headY = net.minecraft.util.Mth.floor(boss.getY() + boss.getBbHeight());

        // High to low: the roomiest spot that still frames the boss underneath.
        for (int up = MAX_ABOVE; up >= MIN_ABOVE; up--) {
            int y = headY + up;
            if (y >= level.getMaxBuildHeight() - CLEAR_HEIGHT) {
                continue;
            }
            if (!isClear(level, cx, y, cz)) {
                continue;
            }
            // And the boss has to be visible from it, or the player is thrown at
            // something they cannot see through a roof they cannot see either.
            if (!hasSightDown(level, new Vec3(cx, y, cz), boss)) {
                continue;
            }
            return new Vec3(cx, y, cz);
        }

        // Nothing directly overhead. Step out a little and try again - beside the
        // boss and above him still reads as a dive, and a roof rarely extends in
        // every direction.
        for (int[] offset : new int[][] { {6, 0}, {-6, 0}, {0, 6}, {0, -6}, {5, 5}, {-5, -5} }) {
            for (int up = MAX_ABOVE; up >= MIN_ABOVE; up--) {
                int y = headY + up;
                double ox = cx + offset[0];
                double oz = cz + offset[1];
                if (y < level.getMaxBuildHeight() - CLEAR_HEIGHT
                        && isClear(level, ox, y, oz)
                        && hasSightDown(level, new Vec3(ox, y, oz), boss)) {
                    return new Vec3(ox, y, oz);
                }
            }
        }
        LOGGER.warn("[CraveBoss] No open launch point above the boss at {} {} {}.",
                (int) cx, headY, (int) cz);
        return null;
    }

    /** Whether a box of air big enough for the throw exists here. */
    private static boolean isClear(ServerLevel level, double x, int y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        for (int dx = -CLEAR_RADIUS; dx <= CLEAR_RADIUS; dx++) {
            for (int dz = -CLEAR_RADIUS; dz <= CLEAR_RADIUS; dz++) {
                for (int dy = 0; dy < CLEAR_HEIGHT; dy++) {
                    pos.set(bx + dx, y + dy, bz + dz);
                    if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** An unobstructed line from the launch point down to the boss. */
    private static boolean hasSightDown(ServerLevel level, Vec3 from, KraveMonster boss) {
        Vec3 to = boss.position().add(0.0D, boss.getBbHeight() * 0.75D, 0.0D);
        return level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss))
                .getType() == HitResult.Type.MISS;
    }

    // ---- keeping Cayden out of the ground -----------------------------------

    /** How far below the boss Cayden may drift before he is considered lost. */
    private static final double BELOW_TOLERANCE = 12.0D;

    /** And how far away laterally before he is no longer in this fight. */
    private static final double AWAY_TOLERANCE = 120.0D;

    /**
     * Whether Cayden is somewhere the encounter can actually continue from.
     *
     * <p>He was ending up under the island. His flight steering takes the direct
     * vector to its target and looks for obstacles along it, which is fine in
     * open air and says nothing about which side of the terrain he is on - so a
     * boss standing on a floating island, seen from below, is a perfectly clear
     * straight line downward through open sky into the underside of the world.
     * Once under it he would keep trying, because from down there the line is
     * still clear.
     */
    public static boolean isValidPosition(ServerLevel level, CaydenCobb cayden, KraveMonster boss) {
        if (cayden.getY() < boss.getY() - BELOW_TOLERANCE) {
            return false;                                  // under the arena
        }
        if (cayden.distanceToSqr(boss) > AWAY_TOLERANCE * AWAY_TOLERANCE) {
            return false;                                  // not in this fight any more
        }
        if (cayden.getY() < level.getMinBuildHeight() + 2) {
            return false;                                  // falling out of the world
        }
        return !suffocating(level, cayden);
    }

    /** Inside solid blocks - stuck rather than merely badly placed. */
    private static boolean suffocating(ServerLevel level, Entity entity) {
        BlockPos pos = BlockPos.containing(entity.getEyePosition());
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Puts Cayden somewhere the fight can carry on from.
     *
     * <p>A teleport, and deliberately only ever this: recovery from a state his
     * navigation cannot get out of, never his normal way of travelling. Logged
     * every time, because each occurrence is evidence about why the steering let
     * him get there.
     */
    public static void recover(ServerLevel level, CaydenCobb cayden, KraveMonster boss) {
        Vec3 from = cayden.position();
        Vec3 to = launchPoint(level, boss);
        if (to == null) {
            // No sky above the boss either. Directly overhead at a safe height is
            // still better than wherever he is now, which is inside something.
            to = boss.position().add(0.0D, boss.getBbHeight() + MIN_ABOVE, 0.0D);
        }
        cayden.teleportTo(to.x, to.y, to.z);
        cayden.setDeltaMovement(Vec3.ZERO);
        cayden.fallDistance = 0.0F;
        cayden.hurtMarked = true;
        LOGGER.info("[CraveBoss] Kaiden invalid position detected at {}/{}/{}, recovering to safe aerial position {}/{}/{}",
                (int) from.x, (int) from.y, (int) from.z, (int) to.x, (int) to.y, (int) to.z);
    }
}
