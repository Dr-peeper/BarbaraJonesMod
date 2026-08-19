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
        boolean dark = entity.isDark();
        boolean full = !dark && entity.isSuperSaiyan();
        boolean half = !dark && !full && entity.isDesperate();
        if (!full && !half && !dark) {
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

        if (dark) {
            // Dark Cayden reads as the inverse of the ascension: the column falls
            // INWARD and downward rather than licking up, in violet-black with a
            // red core, and the arcs are dense instead of occasional. Same
            // machinery, deliberately opposite silhouette.
            darkColumn(pose, buf, t, camYaw);
            arcs(pose, buf, entity, t);
        } else if (full && entity.getTier() >= 4) {
            // ULTRA INSTINCT. The opposite of effort: no flames at all, a still
            // silver shell and a slow drifting halo. Everything below is loud
            // because it is straining; this one is quiet because it is not.
            ultraShell(pose, buf, t, camYaw);
            hairSpikes(pose, buf, t);
        } else if (full && entity.getTier() == 3) {
            // SSJ3: the gold goes taller and wilder still, and the hair extends.
            flameColumn(pose, buf, t, camYaw, 1.75F, false);
            hairSpikes(pose, buf, t);
            arcs(pose, buf, entity, t);
            arcs(pose, buf, entity, t + 5.0F);
            arcs(pose, buf, entity, t + 11.0F);
            shockwave(pose, buf, entity, t);
        } else if (full && entity.getTier() == 2) {
            // SSJ2: same gold, but taller, denser, and the arcs never stop.
            flameColumn(pose, buf, t, camYaw, 1.35F, false);
            hairSpikes(pose, buf, t);
            arcs(pose, buf, entity, t);
            arcs(pose, buf, entity, t + 7.0F);   // a second offset pass = constant crackle
            shockwave(pose, buf, entity, t);
        } else if (full) {
            flameColumn(pose, buf, t, camYaw, 1.0F, false);
            hairSpikes(pose, buf, t);
            arcs(pose, buf, entity, t);
            shockwave(pose, buf, entity, t);
        } else {
            // Half-ascension: a low, angry fire around the legs and chest only.
            // No hair, no shockwave - he has not transformed, he is just refusing
            // to die, and it should read as clearly lesser than the real thing.
            flameColumn(pose, buf, t, camYaw, 0.45F, true);
        }

        pose.popPose();
    }

    // ---- the flame column ---------------------------------------------------

    /** Tapering tongues of gold licking upward, each on its own cycle. */
    private void flameColumn(PoseStack pose, VertexConsumer buf, float t, float camYaw,
                             float scale, boolean ember) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));   // face the camera
        Matrix4f m = pose.last().pose();

        final int TONGUES = 9;
        for (int i = 0; i < TONGUES; i++) {
            // each tongue climbs, then restarts - staggered so it never pulses as one
            float phase = ((t * 0.09F) + i / (float) TONGUES) % 1.0F;
            float life = 1.0F - phase;                       // 1 at birth, 0 at the top
            float x = ((i % 3) - 1) * 0.24F + Mth.sin(t * 0.21F + i) * 0.07F;
            float baseY = -0.05F + phase * 2.05F * scale;
            float height = (0.55F * life + 0.18F) * scale;
            float halfW = (0.30F * life + 0.04F) * (1.0F - phase * 0.45F) * (0.6F + scale * 0.4F);

            // white-hot at the base, deep amber as it burns out
            // Gold for the full transformation; a dirtier orange-red for the half.
            float r = 1.0F;
            float g = ember ? (0.34F + 0.22F * life) : (0.72F + 0.28F * life);
            float b = ember ? (0.04F + 0.10F * life * life) : (0.10F + 0.55F * life * life);
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

    /**
     * The possession aura. Tongues fall in toward him instead of rising off him,
     * which is what makes it read as something taking hold rather than power
     * escaping - the whole point of the transformation.
     */
    private void darkColumn(PoseStack pose, VertexConsumer buf, float t, float camYaw) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
        Matrix4f m = pose.last().pose();

        final int TONGUES = 12;
        for (int i = 0; i < TONGUES; i++) {
            float phase = ((t * 0.07F) + i / (float) TONGUES) % 1.0F;
            float life = phase;                       // grows as it descends
            float x = ((i % 4) - 1.5F) * 0.22F + Mth.sin(t * 0.17F + i) * 0.06F;
            float baseY = 2.4F - phase * 2.5F;        // falls toward the ground
            float height = 0.5F * (1.0F - life) + 0.2F;
            float halfW = 0.09F + life * 0.16F;

            float r = 0.42F + 0.5F * life;
            float g = 0.03F;
            float b = 0.30F - 0.18F * life;
            float a = 0.55F * (1.0F - Math.abs(phase - 0.5F) * 1.6F);
            if (a <= 0.01F) {
                continue;
            }
            tri(buf, m,
                    x - halfW, baseY, 0.0F,
                    x + halfW, baseY, 0.0F,
                    x + Mth.sin(t * 0.26F + i) * 0.10F, baseY - height, 0.0F,
                    r, g, b, a);
        }

        // a bruised halo at chest height, pulsing slowly
        float pulse = 0.7F + Mth.sin(t * 0.3F) * 0.3F;
        for (int i = 0; i < 5; i++) {
            float w = (0.30F + i * 0.05F) * pulse;
            tri(buf, m, -w, 1.05F, 0.0F, w, 1.05F, 0.0F, 0.0F, 1.05F + 0.30F * pulse, 0.0F,
                    0.55F, 0.02F, 0.38F, 0.20F);
        }
        pose.popPose();
    }

    /**
     * Ultra Instinct: a smooth silver shell and a slow halo, with none of the
     * flame the lower forms have. The read is stillness - he is not powering up
     * any more, he simply is not where the hit was going to land.
     */
    private void ultraShell(PoseStack pose, VertexConsumer buf, float t, float camYaw) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
        Matrix4f m = pose.last().pose();

        // a tall, near-still teardrop of pale silver around him
        final int PANELS = 14;
        float breathe = 1.0F + Mth.sin(t * 0.08F) * 0.03F;
        for (int i = 0; i < PANELS; i++) {
            float u = i / (float) (PANELS - 1);              // 0 at feet, 1 at crown
            float y = u * 2.3F * breathe;
            float w = Mth.sin(u * Mth.PI) * 0.62F * breathe + 0.08F;
            float a = 0.20F * (1.0F - Math.abs(u - 0.45F));
            if (a <= 0.005F) {
                continue;
            }
            tri(buf, m, -w, y, 0.0F, w, y, 0.0F, 0.0F, y + 0.16F, 0.0F,
                    0.86F, 0.92F, 1.0F, a);
        }

        // the halo: a thin ring that drifts rather than pulses
        float spin = t * 0.9F;
        final int SEG = 24;
        for (int i = 0; i < SEG; i++) {
            float a0 = (i / (float) SEG) * Mth.TWO_PI + spin * Mth.DEG_TO_RAD;
            float a1 = ((i + 1) / (float) SEG) * Mth.TWO_PI + spin * Mth.DEG_TO_RAD;
            float r0 = 0.52F, r1 = 0.60F;
            float yy = 2.32F;
            tri(buf, m,
                    Mth.cos(a0) * r0, yy, Mth.sin(a0) * r0,
                    Mth.cos(a1) * r0, yy, Mth.sin(a1) * r0,
                    Mth.cos(a1) * r1, yy, Mth.sin(a1) * r1,
                    0.95F, 0.98F, 1.0F, 0.5F);
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
