package com.barbarajones.v2.village.ai;

import com.barbarajones.v2.village.KraveProfession;
import com.barbarajones.v2.village.KraveVillageData;
import com.barbarajones.v2.village.KraveVillagerEntity;
import com.barbarajones.v2.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Residents actually use the buildings they attracted.
 *
 * <p>The village already tracks every buffed block in its claim (beds, doors,
 * workstations, Krave blocks, the television). This goal picks one of those, walks
 * to it, and works there for a while: arm swings, profession-coloured particles, a
 * work sound. It is the difference between a settlement that has buildings and a
 * settlement that looks inhabited.
 *
 * <p>It is also honest about the tracking: it reads the same map the tier maths
 * reads, so if a villager is standing at a workstation it is because that
 * workstation is counting toward the tier, and if it stands around doing nothing
 * the village is genuinely empty of anything to do.
 */
public class VillagerUseBuildingGoal extends Goal {

    private static final int WORK_TICKS = 120;
    private static final int COOLDOWN_TICKS = 200;
    private static final double ARRIVE_DISTANCE_SQR = 6.0D;

    private final KraveVillagerEntity villager;
    private final double speed;

    @Nullable
    private BlockPos site;
    private int workTimer;
    private int cooldown;

    public VillagerUseBuildingGoal(KraveVillagerEntity villager, double speed) {
        this.villager = villager;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (this.villager.isSleeping() || this.villager.isTrading()
                || this.villager.getTarget() != null) {
            return false;
        }
        if (!(this.villager.level() instanceof ServerLevel server)) {
            return false;
        }
        if (this.villager.getRandom().nextInt(30) != 0) {
            return false;
        }
        this.site = pickSite(server);
        return this.site != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.site != null
                && this.workTimer > 0
                && !this.villager.isSleeping()
                && !this.villager.isTrading()
                && this.villager.getTarget() == null;
    }

    @Override
    public void start() {
        this.workTimer = WORK_TICKS;
        if (this.site != null) {
            this.villager.getNavigation().moveTo(
                    this.site.getX() + 0.5D, this.site.getY(), this.site.getZ() + 0.5D, this.speed);
        }
    }

    @Override
    public void stop() {
        this.site = null;
        this.workTimer = 0;
        this.cooldown = COOLDOWN_TICKS;
        this.villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.site == null) {
            return;
        }
        this.workTimer--;
        this.villager.getLookControl().setLookAt(
                this.site.getX() + 0.5D, this.site.getY() + 0.5D, this.site.getZ() + 0.5D);

        double distSqr = this.villager.distanceToSqr(
                this.site.getX() + 0.5D, this.site.getY() + 0.5D, this.site.getZ() + 0.5D);
        if (distSqr > ARRIVE_DISTANCE_SQR) {
            // Still walking. Re-issue the path occasionally; a door closing or a
            // villager blocking the way otherwise strands this goal for its whole
            // duration doing nothing.
            if (this.workTimer % 40 == 0 && this.villager.getNavigation().isDone()) {
                this.villager.getNavigation().moveTo(
                        this.site.getX() + 0.5D, this.site.getY(), this.site.getZ() + 0.5D, this.speed);
            }
            return;
        }

        this.villager.getNavigation().stop();
        if (this.workTimer % 20 == 0) {
            this.villager.swing(InteractionHand.MAIN_HAND);
            this.villager.playSound(workSound(), 0.5F, 0.9F + this.villager.getRandom().nextFloat() * 0.2F);
        }
        if (this.workTimer % 6 == 0 && this.villager.level() instanceof ServerLevel server) {
            server.sendParticles(workParticle(),
                    this.site.getX() + 0.5D, this.site.getY() + 1.1D, this.site.getZ() + 0.5D,
                    2, 0.2D, 0.15D, 0.2D, 0.01D);
        }
    }

    @Nullable
    private BlockPos pickSite(ServerLevel server) {
        Village village = KraveVillageData.get(server).containing(this.villager.blockPosition());
        if (village == null) {
            return null;
        }
        List<BlockPos> sites = village.buildingPositions();
        if (sites.isEmpty()) {
            return null;
        }
        // Sample a few rather than sorting the whole list; picking the truly nearest
        // would make every villager in town converge on the same block.
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos candidate = sites.get(this.villager.getRandom().nextInt(sites.size()));
            if (!server.hasChunkAt(candidate)) {
                continue;
            }
            double dist = this.villager.distanceToSqr(
                    candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            if (dist > 4096.0D) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private ParticleOptions workParticle() {
        KraveProfession job = this.villager.getProfession();
        return switch (job) {
            case CEREALOGIST -> ParticleTypes.WITCH;
            case BUILDER -> ParticleTypes.CRIT;
            case GUARD -> ParticleTypes.ANGRY_VILLAGER;
            case COURIER -> ParticleTypes.CLOUD;
            default -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    private net.minecraft.sounds.SoundEvent workSound() {
        return switch (this.villager.getProfession()) {
            case CEREALOGIST -> SoundEvents.BREWING_STAND_BREW;
            case BUILDER -> SoundEvents.WOOD_PLACE;
            case GUARD -> SoundEvents.SHIELD_BLOCK;
            case COURIER -> SoundEvents.ITEM_PICKUP;
            default -> SoundEvents.VILLAGER_WORK_FARMER;
        };
    }
}
