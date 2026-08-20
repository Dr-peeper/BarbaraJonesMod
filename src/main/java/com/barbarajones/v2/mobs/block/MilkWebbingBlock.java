package com.barbarajones.v2.mobs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The "sticky milk webbing" Loomweaver leaves behind (see
 * {@link com.barbarajones.v2.mobs.entity.ai.LoomweaverWebTrapGoal}). Built on
 * vanilla's {@link WebBlock} for the free, well-tested "slows anything that
 * walks into it" physics (entityInside -> makeStuckInBlock) - only the
 * texture and drops are new.
 *
 * <p>No BlockItem is registered for this block on purpose: it is placed by
 * the mob, never by a player, so it needs no item form and no creative-tab
 * entry.
 */
public class MilkWebbingBlock extends WebBlock {

    public MilkWebbingBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.SNOW)
                .noCollission()
                .strength(4.0F)
                .sound(SoundType.WOOL)
                .noOcclusion());
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Slightly stickier than vanilla cobweb (0.25/0.05/0.25) - this is
        // supposed to read as clinging milk-strands, not a spider's dry silk.
        entity.makeStuckInBlock(state, new Vec3(0.20D, 0.10D, 0.20D));
    }
}
