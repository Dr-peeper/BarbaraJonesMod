package com.barbarajones.v2.village.ai;

import com.barbarajones.v2.village.KraveVillageData;
import com.barbarajones.v2.village.KraveVillagerEntity;
import com.barbarajones.v2.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Krave Villagers go to bed.
 *
 * <p>Beds are the strongest attraction buff in the base table, and a bed nobody
 * sleeps in is a lie. At dusk a resident looks for a free bed inside its claim,
 * walks to it and calls {@code startSleeping}; at dawn, or the moment something
 * attacks it, it gets up.
 *
 * <h2>Finding a free bed</h2>
 * The primary test is vanilla's own {@code OCCUPIED} blockstate, which
 * {@code startSleeping}/{@code stopSleeping} maintain for us. That property is not
 * quite trustworthy on its own - a server killed mid-night leaves beds flagged
 * occupied with nobody in them, and villagers would then never sleep again. So an
 * occupied bed is still accepted if there is demonstrably no living thing lying in
 * it. That one extra check is the difference between "villagers sleep" and
 * "villagers slept, once, until the first crash".
 *
 * <h2>Goal flags</h2>
 * Holds MOVE, LOOK and JUMP, and sits at priority 1 - above every wander and work
 * goal - so nothing drags a sleeping villager back out of bed. It yields to being
 * attacked, because standing up and running is more important than the nap.
 */
public class VillagerSleepGoal extends Goal {

    /** Only used by the no-settlement fallback scan; kept small on purpose. */
    private static final int FALLBACK_RADIUS = 8;
    private static final int SEARCH_HEIGHT = 4;
    private static final double SLEEP_DISTANCE_SQR = 2.5D;
    /** Ticks before giving up on an unreachable bed and just standing there. */
    private static final int APPROACH_TIMEOUT = 400;

    private final KraveVillagerEntity villager;

    @Nullable
    private BlockPos bed;
    private int approachTimer;
    private int searchCooldown;

    public VillagerSleepGoal(KraveVillagerEntity villager) {
        this.villager = villager;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Level level = this.villager.level();
        if (!level.isNight() || this.villager.getTarget() != null || this.villager.isTrading()) {
            return false;
        }
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }
        BlockPos remembered = this.villager.getClaimedBed();
        if (remembered != null && isUsableBed(level, remembered)) {
            this.bed = remembered;
            return true;
        }
        this.bed = findBed(level);
        if (this.bed == null) {
            // Nothing free. Do not re-scan every tick; a village of twelve
            // villagers all searching a 40-block box every tick is a real
            // performance problem, not a theoretical one.
            this.searchCooldown = 200;
            return false;
        }
        this.villager.setClaimedBed(this.bed);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        Level level = this.villager.level();
        if (this.bed == null || this.villager.getTarget() != null) {
            return false;
        }
        if (!level.isNight()) {
            return false;
        }
        if (!(level.getBlockState(this.bed).getBlock() instanceof BedBlock)) {
            return false;
        }
        // Once actually asleep this goal holds until morning; before that it gives
        // up if the walk is taking implausibly long.
        return this.villager.isSleeping() || this.approachTimer < APPROACH_TIMEOUT;
    }

    @Override
    public void start() {
        this.approachTimer = 0;
        if (this.bed != null) {
            this.villager.getNavigation().moveTo(
                    this.bed.getX() + 0.5D, this.bed.getY(), this.bed.getZ() + 0.5D, 0.55D);
        }
    }

    @Override
    public void stop() {
        if (this.villager.isSleeping()) {
            this.villager.stopSleeping();
        }
        this.villager.getNavigation().stop();
        this.bed = null;
        this.approachTimer = 0;
    }

    @Override
    public void tick() {
        if (this.bed == null) {
            return;
        }
        if (this.villager.isSleeping()) {
            return;
        }
        this.approachTimer++;
        double distSqr = this.villager.distanceToSqr(
                this.bed.getX() + 0.5D, this.bed.getY() + 0.5D, this.bed.getZ() + 0.5D);
        if (distSqr <= SLEEP_DISTANCE_SQR) {
            this.villager.getNavigation().stop();
            this.villager.startSleeping(this.bed);
            return;
        }
        if (this.villager.getNavigation().isDone() && this.approachTimer % 40 == 0) {
            this.villager.getNavigation().moveTo(
                    this.bed.getX() + 0.5D, this.bed.getY(), this.bed.getZ() + 0.5D, 0.55D);
        }
    }

    // ---- bed search ----------------------------------------------------------

    /**
     * Finds a bed to claim.
     *
     * <p>The fast path reads the settlement's own tracked-block map rather than
     * scanning the world: beds are already the highest-value entry in that map, so
     * the village knows where every one of them is, and iterating a few dozen known
     * positions costs nothing next to the ten thousand block reads a 16-block cube
     * scan would need - per villager, per night.
     *
     * <p>The slow path is only for a villager with no settlement at all (spawn egg
     * in the wilderness), and is deliberately kept to a small box.
     */
    @Nullable
    private BlockPos findBed(Level level) {
        BlockPos centre = this.villager.hasRestriction()
                ? this.villager.getRestrictCenter()
                : this.villager.blockPosition();

        Set<BlockPos> taken = bedsClaimedByOthers(level, centre);
        List<BlockPos> candidates = new ArrayList<>();

        if (level instanceof ServerLevel server) {
            Village village = KraveVillageData.get(server).containing(this.villager.blockPosition());
            if (village != null) {
                for (BlockPos pos : village.buildingPositions()) {
                    if (server.hasChunkAt(pos) && isUsableBed(level, pos)) {
                        candidates.add(pos);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                for (int dx = -FALLBACK_RADIUS; dx <= FALLBACK_RADIUS; dx++) {
                    for (int dz = -FALLBACK_RADIUS; dz <= FALLBACK_RADIUS; dz++) {
                        cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                        if (level.hasChunkAt(cursor) && isUsableBed(level, cursor)) {
                            candidates.add(cursor.immutable());
                        }
                    }
                }
            }
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            if (taken.contains(pos)) {
                continue;
            }
            double dist = this.villager.distanceToSqr(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            if (dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }
        return best;
    }

    /**
     * Beds other residents have already reserved. Collected in one entity query
     * rather than one per candidate bed - the naive version is quadratic in the
     * number of villagers and shows up immediately in a town of twenty.
     */
    private Set<BlockPos> bedsClaimedByOthers(Level level, BlockPos centre) {
        Set<BlockPos> taken = new HashSet<>();
        AABB box = new AABB(centre).inflate(Village.CLAIM_RADIUS);
        for (KraveVillagerEntity other : level.getEntitiesOfClass(KraveVillagerEntity.class, box)) {
            if (other == this.villager) {
                continue;
            }
            BlockPos claimed = other.getClaimedBed();
            if (claimed != null) {
                taken.add(claimed);
            }
        }
        return taken;
    }

    /** Head half of a bed, and either unoccupied or occupied by nothing real. */
    private boolean isUsableBed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
            return false;
        }
        if (!state.getValue(BedBlock.OCCUPIED)) {
            return true;
        }
        // Occupied flag set. Believe it only if something is actually lying there.
        List<LivingEntity> sleepers = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(1.0D), LivingEntity::isSleeping);
        return sleepers.isEmpty();
    }

}
