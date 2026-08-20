package com.barbarajones.v2.machines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.barbarajones.v2.machines.block.KraveConveyorBlock;
import com.barbarajones.v2.machines.blockentity.KraveConveyorBlockEntity;

/**
 * Draws the items riding a belt.
 *
 * <p>This is the payoff for storing transit progress instead of teleporting
 * stacks between inventories: a Krave line visibly moves. Position comes from the
 * lane's 0..1 progress fed the partial tick, so the item slides continuously at
 * any framerate rather than stepping twenty times a second, and the four lanes
 * are offset sideways so a busy belt looks loaded rather than like one flickering
 * item.
 */
public class KraveConveyorRenderer implements BlockEntityRenderer<KraveConveyorBlockEntity> {

    /** Height of the belt surface plus a hair, so items sit on it rather than in it. */
    private static final float RIDE_HEIGHT = 0.21F;
    /** Sideways spacing between the four lanes, in blocks. */
    private static final double LANE_SPACING = 0.17D;
    private static final double LANE_ORIGIN = -0.255D;

    private final ItemRenderer itemRenderer;

    public KraveConveyorRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(KraveConveyorBlockEntity belt, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Direction facing = belt.getBlockState().getValue(KraveConveyorBlock.FACING);
        int forwardX = facing.getStepX();
        int forwardZ = facing.getStepZ();
        // Perpendicular in the horizontal plane, for lane offsets.
        int sideX = facing.getClockWise().getStepX();
        int sideZ = facing.getClockWise().getStepZ();

        for (int lane = 0; lane < KraveConveyorBlockEntity.LANES; lane++) {
            ItemStack stack = belt.cargo(lane);
            if (stack.isEmpty()) {
                continue;
            }
            float travel = belt.laneProgress(lane, partialTick) - 0.5F;
            double offset = LANE_ORIGIN + lane * LANE_SPACING;

            double x = 0.5D + forwardX * travel + sideX * offset;
            double z = 0.5D + forwardZ * travel + sideZ * offset;

            pose.pushPose();
            pose.translate(x, RIDE_HEIGHT, z);
            pose.scale(0.5F, 0.5F, 0.5F);
            // Lay the item flat and turn it to face along the belt, so a stack of
            // cocoa beans reads as cargo on a conveyor and not as a dropped item.
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-facing.toYRot()));

            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                    belt.getLevel(), (int) belt.getBlockPos().asLong() + lane);
            pose.popPose();
        }
    }

    /** Belts are small; do not bother drawing their cargo from across the base. */
    @Override
    public int getViewDistance() {
        return 32;
    }

    @Override
    public boolean shouldRenderOffScreen(KraveConveyorBlockEntity belt) {
        return false;
    }
}
