package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.ModMobBlocks;
import com.barbarajones.v2.mobs.ModMobSounds;
import com.barbarajones.v2.mobs.entity.LoomweaverEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * The "signature move" from the design brief: every few seconds, if
 * Loomweaver has a target in web range, it drops a patch of sticky milk
 * webbing ({@link ModMobBlocks#MILK_WEBBING}) near the target's feet rather
 * than attacking directly. Runs with NO {@link Goal.Flag#MOVE} - it never
 * fights for movement control, so it layers cleanly alongside a normal
 * {@code MeleeAttackGoal} for the bite.
 */
public class LoomweaverWebTrapGoal extends Goal {

    private static final double MIN_RANGE_SQR = 2.0D * 2.0D;
    private static final double MAX_RANGE_SQR = 9.0D * 9.0D;
    private static final int COOLDOWN_TICKS = 100; // 5s

    private final LoomweaverEntity loomweaver;
    private int cooldown;

    public LoomweaverWebTrapGoal(LoomweaverEntity loomweaver) {
        this.loomweaver = loomweaver;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = loomweaver.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSqr = loomweaver.distanceToSqr(target);
        return distSqr >= MIN_RANGE_SQR && distSqr <= MAX_RANGE_SQR
                && loomweaver.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one-shot action per activation
    }

    @Override
    public void start() {
        LivingEntity target = loomweaver.getTarget();
        if (target == null) {
            cooldown = COOLDOWN_TICKS;
            return;
        }

        Level level = loomweaver.level();
        BlockPos feet = BlockPos.containing(target.getX(), target.getY(), target.getZ());

        // Try the target's own feet first, then a couple of nearby offsets -
        // never overwrite anything, and never place mid-air.
        BlockPos[] candidates = {
                feet, feet.below(),
                feet.offset(1, 0, 0), feet.offset(-1, 0, 0),
                feet.offset(0, 0, 1), feet.offset(0, 0, -1)
        };

        for (BlockPos pos : candidates) {
            if (tryWeb(level, pos)) {
                level.playSound(null, pos, ModMobSounds.LOOMWEAVER_WEB.get(),
                        SoundSource.HOSTILE, 0.8F, 1.0F);
                break;
            }
        }

        cooldown = COOLDOWN_TICKS;
    }

    private boolean tryWeb(Level level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        BlockState under = level.getBlockState(pos.below());
        if (here.isAir() && !under.isAir() && under.isSolidRender(level, pos.below())) {
            return level.setBlockAndUpdate(pos, ModMobBlocks.MILK_WEBBING.get().defaultBlockState());
        }
        return false;
    }
}
