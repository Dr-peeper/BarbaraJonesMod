package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveMonster;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Krave Monster: a custom four-legged/rearing beast (see KraveMonsterModel),
 * not the shared humanoid rig. Before the solid model we draw the after-image
 * trail - a translucent copy at each of his last positions, so a teleport
 * smears ten ghosts across the room.
 */
public class KraveMonsterRenderer extends MobRenderer<KraveMonster, KraveMonsterModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_monster.png");

    public KraveMonsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KraveMonsterModel(ctx.bakeLayer(KraveMonsterModel.LAYER_LOCATION)), 1.6F);
    }

    // Bumped from 1.15 to read as clearly bigger than the player (roughly
    // 3-3.5 blocks reared / 2.2-2.6 at the shoulder on all fours) - a first
    // draft to hand-tune once actually visible in-game, not a final number.
    @Override
    protected void scale(KraveMonster entity, PoseStack pose, float partialTicks) {
        pose.scale(1.8F, 1.8F, 1.8F);
    }

    @Override
    public void render(KraveMonster entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        renderGhosts(entity, partialTicks, pose, buffers, light);
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private void renderGhosts(KraveMonster e, float partial, PoseStack pose,
                              MultiBufferSource buffers, int light) {
        int count = e.ghostFilled ? KraveMonster.GHOSTS : e.ghostHead;
        if (count <= 0) {
            return;
        }
        Vec3 now = new Vec3(
                Mth.lerp(partial, e.xo, e.getX()),
                Mth.lerp(partial, e.yo, e.getY()),
                Mth.lerp(partial, e.zo, e.getZ()));

        var buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (int i = 0; i < count; i++) {
            int idx = (e.ghostHead - 1 - i + KraveMonster.GHOSTS * 2) % KraveMonster.GHOSTS;
            Vec3 g = e.ghostPos[idx];
            if (g.distanceToSqr(now) < 0.02D) {
                continue;   // no smear, no point
            }
            float alpha = 0.45F * (1.0F - (float) i / count);

            pose.pushPose();
            pose.translate(g.x - now.x, g.y - now.y, g.z - now.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - e.ghostYaw[idx]));
            pose.scale(-1.15F, -1.15F, 1.15F);
            pose.translate(0.0D, -1.501D, 0.0D);

            this.model.setupAnim(e, 0.0F, 0.0F, e.tickCount + partial, 0.0F, 0.0F);
            this.model.renderToBuffer(pose, buffer, light,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    0.65F, 0.35F, 1.0F, alpha);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(KraveMonster entity) {
        return TEXTURE;
    }
}
