package com.barbarajones.client.render;

import com.barbarajones.entity.CaydenCobb;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The ascension effect: a golden flame column, crackling arcs, upswept hair and
 * a ground shockwave.
 *
 * Everything is procedural geometry on {@link RenderType#lightning()} - that
 * type is POSITION_COLOR with additive transparency, no texture and no depth
 * write, which is exactly what glow wants: it stacks brighter where it overlaps
 * and never punches a hole in whatever is behind it.
 */
public class SsjAuraLayer extends RenderLayer<CaydenCobb, CaydenModel> {

    public SsjAuraLayer(RenderLayerParent<CaydenCobb, CaydenModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, CaydenCobb entity,
                       float limbSwing, float limbSwingAmount, float partial,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isSuperSaiyan()) {
            return;
        }
        // The aura is drawn in world space around the entity, so undo the model's
        // 180-degree flip and its shift down to the feet.
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.translate(0.0D, -1.5D, 0.0D);

        VertexConsumer buf = buffers.getBuffer(RenderType.lightning());
        float t = ageInTicks;
        // billboard the flat effects toward the camera
        float camYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();

        flameColumn(pose, buf, t, camYaw);
        hairSpikes(pose, buf, t);
        arcs(pose, buf, entity, t);
        shockwave(pose, buf, entity, t);

        pose.popPose();
    }

    // ---- the flame column ---------------------------------------------------

    /** Tapering tongues of gold licking upward, each on its own cycle. */
    private void flameColumn(PoseStack pose, VertexConsumer buf, float t, float camYaw) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));   // face the camera
        Matrix4f m = pose.last().pose();

        final int TONGUES = 9;
        for (int i = 0; i < TONGUES; i++) {
            // each tongue climbs, then restarts - staggered so it never pulses as one
            float phase = ((t * 0.09F) + i / (float) TONGUES) % 1.0F;
            float life = 1.0F - phase;                       // 1 at birth, 0 at the top
            float x = ((i % 3) - 1) * 0.24F + Mth.sin(t * 0.21F + i) * 0.07F;
            float baseY = -0.05F + phase * 2.05F;
            float height = 0.55F * life + 0.18F;
            float halfW = (0.30F * life + 0.04F) * (1.0F - phase * 0.45F);

            // white-hot at the base, deep amber as it burns out
            float r = 1.0F;
            float g = 0.72F + 0.28F * life;
            float b = 0.10F + 0.55F * life * life;
            float a = 0.55F * life * life;
            if (a <= 0.01F) {
                continue;
            }

            // a triangle: wide at the bottom, pinched to a point at the tip
            tri(buf, m,
                    x - halfW, baseY, 0.0F,
                    x + halfW, baseY, 0.0F,
                    x + Mth.sin(t * 0.3F + i) * 0.12F, baseY + height, 0.0F,
                    r, g, b, a);
        }

        // a squat, very bright core at his feet
        for (int i = 0; i < 3; i++) {
            float pulse = 0.75F + Mth.sin(t * 0.55F + i * 2.1F) * 0.25F;
            float w = 0.34F * pulse;
            tri(buf, m, -w, 0.0F, 0.0F, w, 0.0F, 0.0F, 0.0F, 0.95F * pulse, 0.0F,
                    1.0F, 0.95F, 0.65F, 0.42F);
        }
        pose.popPose();
    }

    // ---- upswept hair -------------------------------------------------------

    /** Golden spikes fanning up and back off the crown. */
    private void hairSpikes(PoseStack pose, VertexConsumer buf, float t) {
        pose.pushPose();
        pose.translate(0.0D, 1.42D, 0.0D);       // crown of the head
        Matrix4f m = pose.last().pose();

        final int SPIKES = 7;
        for (int i = 0; i < SPIKES; i++) {
            float spread = (i / (float) (SPIKES - 1)) - 0.5F;      // -0.5 .. 0.5
            float flick = Mth.sin(t * 0.6F + i * 1.7F) * 0.035F;
            float baseX = spread * 0.42F;
            float baseZ = -0.06F + Math.abs(spread) * 0.12F;
            float len = 0.42F - Math.abs(spread) * 0.16F;

            float tipX = baseX + spread * 0.30F + flick;
            float tipY = 0.12F + len;
            float tipZ = baseZ - 0.24F - Math.abs(spread) * 0.10F;   // swept backward

            tri(buf, m,
                    baseX - 0.075F, 0.0F, baseZ,
                    baseX + 0.075F, 0.0F, baseZ,
                    tipX, tipY, tipZ,
                    1.0F, 0.88F, 0.35F, 0.85F);
            // a brighter inner sliver so the spike reads as solid, not a flat card
            tri(buf, m,
                    baseX - 0.035F, 0.02F, baseZ - 0.02F,
                    baseX + 0.035F, 0.02F, baseZ - 0.02F,
                    tipX, tipY - 0.03F, tipZ,
                    1.0F, 1.0F, 0.85F, 0.9F);
        }
        pose.popPose();
    }

    // ---- crackling arcs -----------------------------------------------------

    /** Short jagged bolts that snap around him at random. */
    private void arcs(PoseStack pose, VertexConsumer buf, CaydenCobb entity, float t) {
        Matrix4f m = pose.last().pose();
        // A deterministic pseudo-random keyed on the tick so every client draws
        // the same crackle without any of it needing to be networked.
        long tick = (long) t;
        for (int bolt = 0; bolt < 3; bolt++) {
            long seed = tick / 2L * 31L + bolt * 977L + entity.getId() * 7919L;
            if (Math.floorMod(seed, 5L) != 0L) {
                continue;                        // most frames, most bolts are absent
            }
            float ang = Math.floorMod(seed * 13L, 360L) * Mth.DEG_TO_RAD;
            float radius = 0.34F + Math.floorMod(seed * 7L, 20L) / 100.0F;
            float y = 0.25F + Math.floorMod(seed * 3L, 130L) / 100.0F;

            float px = Mth.cos(ang) * radius;
            float pz = Mth.sin(ang) * radius;
            for (int seg = 0; seg < 4; seg++) {
                long s2 = seed + seg * 131L;
                float nx = px + (Math.floorMod(s2 * 17L, 100L) / 100.0F - 0.5F) * 0.42F;
                float ny = y + (Math.floorMod(s2 * 23L, 100L) / 100.0F - 0.5F) * 0.38F;
                float nz = pz + (Math.floorMod(s2 * 29L, 100L) / 100.0F - 0.5F) * 0.42F;
                // a hairline triangle stands in for a line segment
                tri(buf, m, px, y, pz, px + 0.02F, y + 0.02F, pz, nx, ny, nz,
                        0.75F, 0.95F, 1.0F, 0.9F);
                px = nx; y = ny; pz = nz;
            }
        }
    }

    // ---- ground shockwave ---------------------------------------------------

    /** An expanding ring on the floor for the first second after ascending. */
    private void shockwave(PoseStack pose, VertexConsumer buf, CaydenCobb entity, float t) {
        int since = entity.ticksSinceAscension();
        if (since < 0 || since > 20) {
            return;
        }
        float p = since / 20.0F;                 // 0 -> 1
        float radius = 0.4F + p * 3.4F;
        float alpha = (1.0F - p) * 0.75F;
        float y = 0.02F;

        Matrix4f m = pose.last().pose();
        final int SEGMENTS = 32;
        float inner = radius - 0.22F;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (i / (float) SEGMENTS) * Mth.TWO_PI;
            float a1 = ((i + 1) / (float) SEGMENTS) * Mth.TWO_PI;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            // ring as a strip of two triangles per segment
            tri(buf, m, c0 * inner, y, s0 * inner, c1 * inner, y, s1 * inner,
                    c1 * radius, y, s1 * radius, 1.0F, 0.9F, 0.5F, alpha);
            tri(buf, m, c0 * inner, y, s0 * inner, c1 * radius, y, s1 * radius,
                    c0 * radius, y, s0 * radius, 1.0F, 0.9F, 0.5F, alpha);
        }
    }

    // ---- helper -------------------------------------------------------------

    /**
     * RenderType.lightning() draws QUADS, so a triangle is emitted as a quad
     * with its last two corners collapsed onto the tip.
     */
    private void tri(VertexConsumer buf, Matrix4f m,
                     float x0, float y0, float z0,
                     float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, z0).color(r, g, b, a).endVertex();
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
