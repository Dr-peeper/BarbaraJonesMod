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

    /**
     * The model's paw geometry (KraveMonsterModel: hips.y + thigh/shin/foot
     * offsets) reaches noticeably lower than the entity's actual feet
     * position, so the feet were sinking into the ground - most visible now
     * that the model renders at 1.8x instead of the original 1.15x. Nudges
     * the whole solid body up to compensate; the ghost trail already has its
     * own separate, independently-tuned offset (see renderGhosts) so this
     * only applies to the main render pass.
     */
    private static final double GROUND_CORRECTION = 0.3D;

    public KraveMonsterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KraveMonsterModel(ctx.bakeLayer(KraveMonsterModel.LAYER_LOCATION)), 1.6F);
    }

    // No new model or texture exists per form - that needs real art. Instead
    // the escalation between forms reads through size and body-color tint:
    // form 1 is barely bigger than a player, form 4 is gargantuan.
    private static final float[] FORM_SCALE = { 1.2F, 1.8F, 2.4F, 3.4F };

    @Override
    protected void scale(KraveMonster entity, PoseStack pose, float partialTicks) {
        float s = FORM_SCALE[Mth.clamp(entity.getForm() - 1, 0, FORM_SCALE.length - 1)];
        pose.scale(s, s, s);
    }

    @Override
    public void render(KraveMonster entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        renderGhosts(entity, partialTicks, pose, buffers, light);

        // Form 3 reads red, form 4 goes near-black with a red core - "the
        // scariest thing you've fought" through tint and size since there's
        // no dedicated model per form.
        int form = entity.getForm();
        if (form == 3) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 0.5F, 0.45F, 1.0F);
        } else if (form == 4) {
            float pulse = 0.55F + 0.15F * Mth.sin((entity.tickCount + partialTicks) * 0.15F);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(pulse, 0.12F, 0.12F, 1.0F);
        }

        pose.pushPose();
        pose.translate(0.0D, GROUND_CORRECTION, 0.0D);
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        pose.popPose();

        if (form == 3 || form == 4) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
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
