package com.barbarajones.boss.manager;

import com.barbarajones.BarbaraJonesMod;

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
 * CLIENT ONLY - reached solely from ClientSetup, which is {@code Dist.CLIENT}
 * guarded.
 *
 * <p>Draws The Manager plus the floor telegraph for whatever he is winding up.
 * The ring is procedural geometry on {@link RenderType#lightning()} - POSITION_COLOR,
 * additive, no depth write - so it glows over the floor instead of z-fighting
 * with it, and it needs no texture and no UVs. It draws QUADS, which is exactly
 * what an annulus segment is.
 */
public class ManagerRenderer extends MobRenderer<TheManager, ManagerModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/manager_boss.png");

    private static final int RING_SEGMENTS = 40;
    private static final int WARNING_BARS = 6;

    public ManagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ManagerModel(ctx.bakeLayer(ManagerModel.LAYER_LOCATION)), 0.55F);
    }

    /** Taller than a player without being a giant - he is management, not a titan. */
    @Override
    protected void scale(TheManager entity, PoseStack pose, float partialTicks) {
        pose.scale(1.15F, 1.25F, 1.15F);
    }

    @Override
    public void render(TheManager entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        if (entity.isWindingUp()) {
            renderTelegraph(entity, pose, buffers);
        }
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private void renderTelegraph(TheManager entity, PoseStack pose, MultiBufferSource buffers) {
        float progress = entity.getWindupProgress();
        float[] tint = colourFor(entity.getWindupKind());

        // the ring races outward and fades as it goes, so "nearly done" reads
        // from across the arena without looking at the boss himself
        float radius = 1.0F + progress * 4.2F;
        float thickness = 0.30F + (1.0F - progress) * 0.25F;
        float alpha = 0.20F + 0.65F * (1.0F - Math.abs(progress - 0.6F));

        VertexConsumer buf = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();

        float inner = Math.max(0.05F, radius - thickness);
        for (int i = 0; i < RING_SEGMENTS; i++) {
            float a0 = (float) (Math.PI * 2.0D * i / RING_SEGMENTS);
            float a1 = (float) (Math.PI * 2.0D * (i + 1) / RING_SEGMENTS);
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            quad(buf, matrix, tint, alpha,
                    c0 * inner, 0.03F, s0 * inner,
                    c1 * inner, 0.03F, s1 * inner,
                    c1 * radius, 0.03F, s1 * radius,
                    c0 * radius, 0.03F, s0 * radius);
        }

        // vertical bars climbing the ring as the timer fills - the "3, 2, 1"
        float barHeight = 0.2F + progress * 2.4F;
        float barAlpha = 0.5F * progress;
        for (int i = 0; i < WARNING_BARS; i++) {
            float angle = (float) (Math.PI * 2.0D * i / WARNING_BARS) + progress * 1.5F;
            float cx = Mth.cos(angle) * radius;
            float cz = Mth.sin(angle) * radius;
            float wx = -Mth.sin(angle) * 0.12F;
            float wz = Mth.cos(angle) * 0.12F;
            quad(buf, matrix, tint, barAlpha,
                    cx - wx, 0.03F, cz - wz,
                    cx + wx, 0.03F, cz + wz,
                    cx + wx, barHeight, cz + wz,
                    cx - wx, barHeight, cz - wz);
        }
    }

    private static float[] colourFor(int windupKind) {
        return switch (windupKind) {
            case TheManager.WINDUP_SUMMON -> new float[] { 0.55F, 0.70F, 1.00F };
            case TheManager.WINDUP_REVIEW -> new float[] { 1.00F, 0.82F, 0.18F };
            case TheManager.WINDUP_CABINETS -> new float[] { 0.75F, 0.78F, 0.82F };
            default -> new float[] { 1.00F, 0.14F, 0.10F };
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
    public ResourceLocation getTextureLocation(TheManager entity) {
        return TEXTURE;
    }
}
