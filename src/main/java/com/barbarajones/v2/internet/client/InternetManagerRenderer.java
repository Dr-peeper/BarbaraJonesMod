package com.barbarajones.v2.internet.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.internet.InternetManagerBoss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * CLIENT ONLY - reached solely from {@link InternetClientSetup}, which is
 * {@code Dist.CLIENT} guarded.
 *
 * <p>Two distinct telegraphs, both procedural geometry on
 * {@link RenderType#lightning()} exactly the way {@code ManagerRenderer} draws
 * its floor ring - POSITION_COLOR, additive, no texture, no depth write:
 * <ul>
 *   <li>a ground ring for WHIP / LATENCY / PACKET LOSS / THROTTLE, colour-coded
 *       per ability, racing outward as the windup closes in;</li>
 *   <li>a floating halo ring over his head for BUFFERING, which does not race
 *       outward at all - it fills clockwise, because that ring <em>is</em> the
 *       loading meter {@link InternetManagerBoss#getWindupProgress()} reports,
 *       not a countdown to something else.</li>
 * </ul>
 */
public class InternetManagerRenderer extends MobRenderer<InternetManagerBoss, InternetManagerModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/internet_manager.png");

    private static final int RING_SEGMENTS = 40;

    public InternetManagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new InternetManagerModel(ctx.bakeLayer(InternetManagerModel.LAYER_LOCATION)), 0.6F);
    }

    /** A head taller than a player - present in an arena without being a titan. */
    @Override
    protected void scale(InternetManagerBoss entity, PoseStack pose, float partialTicks) {
        pose.scale(1.12F, 1.2F, 1.12F);
    }

    @Override
    public void render(InternetManagerBoss entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (entity.isBuffering()) {
            renderBufferHalo(entity, pose, buffers);
        } else if (entity.isWindingUp()) {
            renderGroundRing(entity, pose, buffers);
        }
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private void renderGroundRing(InternetManagerBoss entity, PoseStack pose, MultiBufferSource buffers) {
        float progress = entity.getWindupProgress();
        float[] tint = colourFor(entity.getWindupKind());

        float radius = 1.0F + progress * 4.2F;
        float thickness = 0.30F + (1.0F - progress) * 0.25F;
        float alpha = 0.20F + 0.65F * (1.0F - Math.abs(progress - 0.6F));

        VertexConsumer buf = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();

        float inner = Math.max(0.05F, radius - thickness);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / RING_SEGMENTS);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / RING_SEGMENTS);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(buf, matrix, tint, alpha,
                    c0 * inner, 0.03F, s0 * inner, c1 * inner, 0.03F, s1 * inner,
                    c1 * radius, 0.03F, s1 * radius, c0 * radius, 0.03F, s0 * radius);
        }
    }

    /** A ring hanging over his head that fills like a loading spinner instead of expanding outward. */
    private void renderBufferHalo(InternetManagerBoss entity, PoseStack pose, MultiBufferSource buffers) {
        float fill = entity.getWindupProgress();
        VertexConsumer buf = buffers.getBuffer(RenderType.lightning());

        pose.pushPose();
        pose.translate(0.0D, entity.getBbHeight() + 0.55D, 0.0D);
        Matrix4f matrix = pose.last().pose();

        float radius = 0.85F;
        float thickness = 0.11F;
        float inner = radius - thickness;
        int filledSegments = Math.round(RING_SEGMENTS * fill);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            boolean lit = i < filledSegments;
            float[] tint = lit ? new float[] { 0.55F, 0.95F, 1.0F } : new float[] { 0.15F, 0.2F, 0.25F };
            float alpha = lit ? 0.85F : 0.30F;
            float a0 = (float) (Math.PI * 2.0D * i / RING_SEGMENTS) - Mth.PI * 0.5F;
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / RING_SEGMENTS) - Mth.PI * 0.5F;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(buf, matrix, tint, alpha,
                    c0 * inner, s0 * inner, 0.0F, c1 * inner, s1 * inner, 0.0F,
                    c1 * radius, s1 * radius, 0.0F, c0 * radius, s0 * radius, 0.0F);
        }
        pose.popPose();
    }

    private static float[] colourFor(int windupKind) {
        return switch (windupKind) {
            case InternetManagerBoss.WINDUP_LATENCY -> new float[] { 0.55F, 0.85F, 1.00F };
            case InternetManagerBoss.WINDUP_PACKET_LOSS -> new float[] { 0.75F, 0.75F, 0.78F };
            case InternetManagerBoss.WINDUP_THROTTLE -> new float[] { 0.55F, 0.15F, 0.65F };
            default -> new float[] { 1.00F, 0.45F, 0.06F };   // WHIP: cable orange
        };
    }

    /** lightning() is POSITION_COLOR: position and colour, nothing else. */
    private static void quad(VertexConsumer buf, Matrix4f m, float[] tint, float alpha,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buf.vertex(m, ax, ay, az).color(tint[0], tint[1], tint[2], alpha).endVertex();
        buf.vertex(m, bx, by, bz).color(tint[0], tint[1], tint[2], alpha).endVertex();
        buf.vertex(m, cx, cy, cz).color(tint[0], tint[1], tint[2], alpha).endVertex();
        buf.vertex(m, dx, dy, dz).color(tint[0], tint[1], tint[2], alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(InternetManagerBoss entity) {
        return TEXTURE;
    }
}
