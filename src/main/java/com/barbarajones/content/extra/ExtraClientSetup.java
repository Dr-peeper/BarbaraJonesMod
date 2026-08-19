package com.barbarajones.content.extra;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client wiring for the extra-content pack. Only the recliner seat needs a
 * renderer, and it needs one that draws nothing - an entity with no registered
 * renderer crashes the dispatcher the first time it comes into view.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ExtraClientSetup {

    private ExtraClientSetup() { }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExtraRegistry.RECLINER_SEAT, SeatRenderer::new);
    }

    /**
     * Draws nothing at all. The player riding the seat is still drawn - passengers
     * are separate entries in the render loop, so culling the seat away costs
     * nothing visually and skips a texture bind per frame per occupied chair.
     */
    public static class SeatRenderer extends EntityRenderer<SeatEntity> {

        private static final ResourceLocation TEXTURE =
                new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/recliner_seat.png");

        public SeatRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public ResourceLocation getTextureLocation(SeatEntity entity) {
            return TEXTURE;
        }

        @Override
        public void render(SeatEntity entity, float yaw, float partial, PoseStack pose,
                           MultiBufferSource buffers, int light) {
            // intentionally empty - the seat is furniture-shaped nothingness
        }

        @Override
        public boolean shouldRender(SeatEntity entity, Frustum frustum, double camX, double camY, double camZ) {
            return false;
        }
    }
}
