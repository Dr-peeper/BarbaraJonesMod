package com.barbarajones.v2.village.ai;

import com.barbarajones.v2.village.KraveVillagerEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Strolling, but homeward.
 *
 * <p>Vanilla's {@code WaterAvoidingRandomStrollGoal} picks a point near the mob,
 * which over an hour walks a villager clean out of town. This one picks its point
 * near the settlement's origin instead, so residents drift around the village
 * rather than away from it, and a villager that has been dragged off by a fight
 * walks home on its own.
 *
 * <p>The home position comes from the mob's pathfinding restriction, which
 * {@code KraveVillagerEntity} sets to the village origin every few seconds. If it
 * has no village yet it falls back to strolling around wherever it is standing, so
 * a spawn-egg villager in the middle of nowhere still behaves like a creature.
 */
public class VillagerWanderHomeGoal extends Goal {

    private static final int SPREAD = 14;
    private static final int VERTICAL_TOLERANCE = 8;

    private final KraveVillagerEntity villager;
    private final double speed;

    @Nullable
    private BlockPos destination;

    public VillagerWanderHomeGoal(KraveVillagerEntity villager, double speed) {
        this.villager = villager;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.villager.isSleeping() || this.villager.isTrading() || this.villager.getTarget() != null) {
            return false;
        }
        if (!this.villager.getNavigation().isDone()) {
            return false;
        }
        // One roll in two seconds keeps a village from looking like a fire drill.
        if (this.villager.getRandom().nextInt(40) != 0) {
            return false;
        }
        this.destination = pickDestination();
        return this.destination != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.villager.getNavigation().isDone()
                && !this.villager.isSleeping()
                && !this.villager.isTrading()
                && this.villager.getTarget() == null;
    }

    @Override
    public void start() {
        if (this.destination != null) {
            this.villager.getNavigation().moveTo(
                    this.destination.getX() + 0.5D,
                    this.destination.getY(),
                    this.destination.getZ() + 0.5D,
                    this.speed);
        }
    }

    @Override
    public void stop() {
        this.destination = null;
        this.villager.getNavigation().stop();
    }

    @Nullable
    private BlockPos pickDestination() {
        Level level = this.villager.level();
        BlockPos home = this.villager.hasRestriction()
                ? this.villager.getRestrictCenter()
                : this.villager.blockPosition();

        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = this.villager.getRandom().nextInt(SPREAD * 2 + 1) - SPREAD;
            int dz = this.villager.getRandom().nextInt(SPREAD * 2 + 1) - SPREAD;
            BlockPos probe = home.offset(dx, 0, dz);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            // Refuse anything that would send the villager up a cliff or down a
            // ravine - a stroll goal has no business scaling a mountain.
            if (Math.abs(ground.getY() - this.villager.getBlockY()) > VERTICAL_TOLERANCE) {
                continue;
            }
            return ground;
        }
        return null;
    }
}
