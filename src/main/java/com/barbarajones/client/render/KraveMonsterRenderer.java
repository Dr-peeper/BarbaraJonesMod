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
    // the escalation between all six forms (one per Cayden rung, SSJ through
    // Ultra Instinct) reads through size and body-color tint: form 1 is
    // barely bigger than a player, form 6 is a nightmare.
    /**
     * How big he is per form. Form ONE now opens at what used to be Ultra's
     * size, and climbs from there - so the fight starts with something already
     * enormous walking at you and finishes with something absurd.
     *
     * <p>Deliberately outpaces the hitbox in KraveMonster.getDimensions. A
     * collision box that literally matched the final form would be about
     * twenty-six blocks tall, which suffocates inside his own den and shoves
     * terrain around. A boss that READS bigger than it collides is the normal
     * answer to that, and the Ender Dragon does exactly the same thing.
     */
    private static final float[] FORM_SCALE = { 5.8F, 7.0F, 8.4F, 10.0F, 11.8F, 14.0F };

    @Override
    protected void scale(KraveMonster entity, PoseStack pose, float partialTicks) {
        float s = FORM_SCALE[Mth.clamp(entity.getForm() - 1, 0, FORM_SCALE.length - 1)];
        pose.scale(s, s, s);
    }

    @Override
    public void render(KraveMonster entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        renderGhosts(entity, partialTicks, pose, buffers, light);

        // Forms 3-6 escalate through tint since there's no dedicated model
        // per form: red (SSJ3-equivalent), pulsating near-black-red (God),
        // a corrupted cold blue (Blue), then a stark near-white flicker for
        // Ultra - the palette gets colder and stranger instead of just
        // darker, so it doesn't read as "form 4 but bigger."
        int form = entity.getForm();
        float t = entity.tickCount + partialTicks;
        // Every form now tints, not just 3+ - forms 1-2 previously rendered
        // completely plain no matter how far the transformation ladder had
        // climbed, which read as "he just got bigger" instead of an actual
        // power escalation.
        switch (form) {
            case 1 -> {
                // First transformation: a faint warm gold, barely-there -
                // the same family as Cayden's own SSJ, subtle on purpose
                // so form 3+ still reads as the real escalation.
                float pulse = 0.9F + 0.1F * Mth.sin(t * 0.08F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, pulse * 0.96F, 0.75F, 1.0F);
            }
            case 2 -> {
                float pulse = 0.85F + 0.15F * Mth.sin(t * 0.1F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, pulse * 0.88F, 0.55F, 1.0F);
            }
            case 3 -> com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 0.5F, 0.45F, 1.0F);
            case 4 -> {
                float pulse = 0.55F + 0.15F * Mth.sin(t * 0.15F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(pulse, 0.12F, 0.12F, 1.0F);
            }
            case 5 -> {
                float pulse = 0.5F + 0.15F * Mth.sin(t * 0.1F);
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0.25F, pulse * 0.7F, pulse * 1.3F, 1.0F);
            }
            default -> {   // 6: Ultra - flickers toward near-white at random moments
                float flicker = (Mth.sin(t * 0.6F) > 0.85F) ? 1.6F : 0.6F;
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(
                        0.85F * flicker, 0.9F * flicker, 1.0F * flicker, 1.0F);
            }
        }

        pose.pushPose();
        pose.translate(0.0D, GROUND_CORRECTION, 0.0D);
        super.render(entity, yaw, partialTicks, pose, buffers, light);
        pose.popPose();

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
