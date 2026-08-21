package com.barbarajones.client.render;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.AscensionLadder;

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
 * The ascension effect: a towering flame column, a beam of light punched into
 * the sky, upswept hair, constant crackling arcs, orbiting sparks, and
 * repeating ground shockwaves - the whole point is that he should not read as
 * "a kid with a glow effect," he should read as something that just tore a
 * hole in the sky. Everything scales with distance from the ground/head so it
 * keeps a sense of scale even when he's standing still.
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

        VertexConsumer rawBuf = buffers.getBuffer(RenderType.lightning());
        float t = ageInTicks;
        // billboard the flat effects toward the camera
        float camYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();

        // Per-rung tint. RenderType.lightning() is POSITION_COLOR with no
        // texture and no ColorModulator uniform in its shader, so
        // RenderSystem.setShaderColor() is silently a no-op on it (that was
        // tried first and does nothing here - it only works on textured
        // entity render types, which is why KraveMonster's tint works but
        // this one didn't). The colors are baked straight into the vertices
        // by every draw call below, so tinting means actually multiplying
        // those vertex colors - hence the wrapper.
        float tr = 1.0F, tg = 1.0F, tb = 1.0F;
        int tier = full ? entity.getTier() : 0;
        switch (tier) {
            case 2 -> { tr = 1.0F; tg = 1.0F; tb = 0.85F; }               // SSJ2: white-hot gold
            case 3 -> { tr = 1.0F; tg = 0.7F; tb = 0.35F; }               // SSJ3: deep orange
            case 4 -> { tr = 1.0F; tg = 0.15F; tb = 0.18F; }              // GOD: red
            case 5 -> { tr = 0.25F; tg = 0.45F; tb = 1.0F; }              // BLUE: blue
            case 6 -> {                                                  // ULTRA: white
                // Handled by the whitewash below, not by tinting. The aura's
                // vertex colours are baked gold, and multiplying gold by
                // anything is still gold - white has to REPLACE the colour.
                // It used to be a shifting prismatic wash, which read as one
                // more colour in the ladder; the point of Ultra Instinct is
                // that it is not another colour, it is the absence of one.
            }
            default -> { }                                               // SSJ1 / half / dark: unchanged gold-red
        }
        VertexConsumer buf;
        if (full && tier == AscensionLadder.ULTRA) {
            buf = new WhitewashVertexConsumer(rawBuf);
        } else if (tr == 1.0F && tg == 1.0F && tb == 1.0F) {
            buf = rawBuf;
        } else {
            buf = new TintedVertexConsumer(rawBuf, tr, tg, tb);
        }

        // Each rung reads as its own escalation, not a recolor of the same
        // effect stack:
        //  SSJ1        - aura only, no crackle at all (the "first time" should be quiet)
        //  SSJ2        - aura + lightning starts
        //  SSJ3        - huge aura, denser/crazier lightning, long swept-back hair
        //  GOD / BLUE  - a different silhouette entirely: pulsating rings instead of hair spikes
        //  ULTRA       - white, sparse, fast - reads as "stopped trying," not "biggest fire"
        boolean isSsj1 = full && tier == AscensionLadder.SSJ;
        boolean isSsj3Plus = full && tier >= AscensionLadder.SSJ3 && tier < AscensionLadder.GOD;
        boolean isDivine = full && (tier == AscensionLadder.GOD || tier == AscensionLadder.BLUE);
        boolean isUltra = full && tier == AscensionLadder.ULTRA;

        flameColumn(pose, buf, t, camYaw);
        skyBeam(pose, buf, t, camYaw);

        if (!isDivine) {
            hairSpikes(pose, buf, t, isSsj3Plus || isUltra);
        }
        if (!isSsj1) {
            int bolts = isSsj3Plus ? 14 : 7;
            arcs(pose, buf, entity, t, bolts);
        }
        if (!isUltra) {
            orbitingSparks(pose, buf, t);
        }
        shockwave(pose, buf, entity, t);
        groundPulse(pose, buf, t);

        if (isDivine) {
            // "More pulsating energy" - the GOD/BLUE signature: the whole
            // silhouette breathes instead of just flaring outward once.
            pulsingCore(pose, buf, t);
        }

        // Ultra Instinct gets a second, wider shockwave ring on top of the
        // normal one - the standout "this is the final form" flourish.
        if (isUltra) {
            shockwave(pose, buf, entity, t + 40.0F);
        }

        pose.popPose();
    }

    /**
     * Multiplies every vertex color that flows through it by a fixed tint,
     * then forwards to the real buffer. RenderType.lightning() has no
     * texture and no shader-color uniform, so this is the only way to
     * actually recolor it per rung - see the note in render() above.
     */
    /**
     * Forces every vertex white, keeping only its alpha.
     *
     * <p>Ultra Instinct is the one rung that is not a colour. Every other tier
     * multiplies the aura's baked gold by something, which is why they all read
     * as the same fire in a different shade - but you cannot multiply gold into
     * white, so this discards the incoming rgb entirely and keeps the alpha, so
     * the shape, falloff and flicker all survive untouched.
     */
    private static final class WhitewashVertexConsumer implements VertexConsumer {

        private final VertexConsumer delegate;

        WhitewashVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            this.delegate.color(255, 255, 255, a);   // alpha survives; hue does not
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            this.delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            this.delegate.defaultColor(255, 255, 255, a);
        }

        @Override
        public void unsetDefaultColor() {
            this.delegate.unsetDefaultColor();
        }
    }

    private static final class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float tr, tg, tb;

        TintedVertexConsumer(VertexConsumer delegate, float tr, float tg, float tb) {
            this.delegate = delegate;
            this.tr = tr;
            this.tg = tg;
            this.tb = tb;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(Mth.clamp((int) (r * tr), 0, 255), Mth.clamp((int) (g * tg), 0, 255),
                    Mth.clamp((int) (b * tb), 0, 255), a);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(Mth.clamp((int) (r * tr), 0, 255), Mth.clamp((int) (g * tg), 0, 255),
                    Mth.clamp((int) (b * tb), 0, 255), a);
        }
    }

    /** A slow breathing glow around the torso - GOD/BLUE's "pulsating energy" instead of hair spikes. */
    private void pulsingCore(PoseStack pose, VertexConsumer buf, float t) {
        pose.pushPose();
        pose.translate(0.0D, 0.9D, 0.0D);
        Matrix4f m = pose.last().pose();
        for (int ring = 0; ring < 3; ring++) {
            float phase = ((t * 0.045F) + ring / 3.0F) % 1.0F;
            float r = 0.3F + phase * 1.1F;
            float a = (1.0F - phase) * 0.5F;
            if (a <= 0.02F) {
                continue;
            }
            final int SEGS = 16;
            for (int i = 0; i < SEGS; i++) {
                float a0 = (i / (float) SEGS) * Mth.TWO_PI;
                float a1 = ((i + 1) / (float) SEGS) * Mth.TWO_PI;
                tri(buf, m, Mth.cos(a0) * r, Mth.sin(a0) * r * 0.5F, 0.0F,
                        Mth.cos(a1) * r, Mth.sin(a1) * r * 0.5F, 0.0F,
                        0.0F, 0.0F, 0.0F,
                        1.0F, 0.95F, 0.9F, a);
            }
        }
        pose.popPose();
    }

    // ---- the flame column ---------------------------------------------------

    /** Tapering tongues of gold licking upward, each on its own cycle - now a full-height inferno. */
    private void flameColumn(PoseStack pose, VertexConsumer buf, float t, float camYaw) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));   // face the camera
        Matrix4f m = pose.last().pose();

        final int TONGUES = 18;
        for (int i = 0; i < TONGUES; i++) {
            // each tongue climbs, then restarts - staggered so it never pulses as one
            float phase = ((t * 0.10F) + i / (float) TONGUES) % 1.0F;
            float life = 1.0F - phase;                       // 1 at birth, 0 at the top
            float x = ((i % 3) - 1) * 0.34F + Mth.sin(t * 0.21F + i) * 0.10F;
            float baseY = -0.05F + phase * 3.1F;
            float height = 0.85F * life + 0.24F;
            float halfW = (0.42F * life + 0.06F) * (1.0F - phase * 0.4F);

            // white-hot at the base, deep amber as it burns out
            // Gold for the full transformation; a dirtier orange-red for the half.
            float r = 1.0F;
            float g = 0.75F + 0.25F * life;
            float b = 0.15F + 0.6F * life * life;
            float a = 0.65F * life * life;
            if (a <= 0.01F) {
                continue;
            }

            // a triangle: wide at the bottom, pinched to a point at the tip
            tri(buf, m,
                    x - halfW, baseY, 0.0F,
                    x + halfW, baseY, 0.0F,
                    x + Mth.sin(t * 0.3F + i) * 0.16F, baseY + height, 0.0F,
                    r, g, b, a);
        }

        // a wide, very bright core at his feet - the "engine" of the whole column
        for (int i = 0; i < 5; i++) {
            float pulse = 0.8F + Mth.sin(t * 0.5F + i * 1.7F) * 0.2F;
            float w = 0.55F * pulse;
            tri(buf, m, -w, 0.0F, 0.0F, w, 0.0F, 0.0F, 0.0F, 1.3F * pulse, 0.0F,
                    1.0F, 0.97F, 0.75F, 0.5F);
        }
        pose.popPose();
    }

    // ---- the sky beam ---------------------------------------------------------

    /**
     * A column of light punched straight up out of the top of his head, far
     * higher than anything else in the effect - the single biggest signal
     * that something enormous is happening, visible from well outside the
     * arena, not just up close.
     */
    private void skyBeam(PoseStack pose, VertexConsumer buf, float t, float camYaw) {
        pose.pushPose();
        pose.translate(0.0D, 1.9D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(-camYaw));
        Matrix4f m = pose.last().pose();

        float pulse = 0.75F + 0.25F * Mth.sin(t * 0.12F);
        float topY = 40.0F;
        float outerW = 0.32F * pulse;
        float innerW = 0.11F * pulse;
        float baseA = 0.4F * pulse;

        quad(buf, m, -outerW, 0.0F, outerW, 0.0F, outerW, topY, -outerW, topY,
                1.0F, 0.92F, 0.55F, baseA);
        quad(buf, m, -innerW, 0.0F, innerW, 0.0F, innerW, topY, -innerW, topY,
                1.0F, 1.0F, 0.92F, baseA * 1.4F);
        pose.popPose();
    }

    // ---- upswept hair -------------------------------------------------------

    /** Golden spikes fanning up and back off the crown - bigger, brighter, denser. SSJ3+ gets them longer, per the "long hair" ask. */
    private void hairSpikes(PoseStack pose, VertexConsumer buf, float t, boolean longHair) {
        pose.pushPose();
        pose.translate(0.0D, 1.42D, 0.0D);       // crown of the head
        Matrix4f m = pose.last().pose();
        float lenMul = longHair ? 1.7F : 1.0F;

        final int SPIKES = 11;
        for (int i = 0; i < SPIKES; i++) {
            float spread = (i / (float) (SPIKES - 1)) - 0.5F;      // -0.5 .. 0.5
            float flick = Mth.sin(t * 0.6F + i * 1.7F) * 0.05F;
            float baseX = spread * 0.48F;
            float baseZ = -0.06F + Math.abs(spread) * 0.12F;
            float len = (0.62F - Math.abs(spread) * 0.2F) * lenMul;

            float tipX = baseX + spread * 0.4F + flick;
            float tipY = 0.12F + len;
            float tipZ = baseZ - 0.3F - Math.abs(spread) * 0.12F;   // swept backward

            tri(buf, m,
                    baseX - 0.095F, 0.0F, baseZ,
                    baseX + 0.095F, 0.0F, baseZ,
                    tipX, tipY, tipZ,
                    1.0F, 0.9F, 0.4F, 0.9F);
            // a brighter inner sliver so the spike reads as solid, not a flat card
            tri(buf, m,
                    baseX - 0.045F, 0.02F, baseZ - 0.02F,
                    baseX + 0.045F, 0.02F, baseZ - 0.02F,
                    tipX, tipY - 0.04F, tipZ,
                    1.0F, 1.0F, 0.88F, 0.95F);
        }
        pose.popPose();
    }

    // ---- crackling arcs -----------------------------------------------------

    /** Jagged bolts that snap around him constantly, not occasionally. boltCount lets SSJ3+ get "crazier lightning." */
    private void arcs(PoseStack pose, VertexConsumer buf, CaydenCobb entity, float t, int boltCount) {
        Matrix4f m = pose.last().pose();
        // A deterministic pseudo-random keyed on the tick so every client draws
        // the same crackle without any of it needing to be networked.
        long tick = (long) t;
        for (int bolt = 0; bolt < boltCount; bolt++) {
            long seed = tick / 2L * 31L + bolt * 977L + entity.getId() * 7919L;
            if (Math.floorMod(seed, 3L) == 0L) {
                continue;                        // still flickers, but most bolts are now present most frames
            }
            float ang = Math.floorMod(seed * 13L, 360L) * Mth.DEG_TO_RAD;
            float radius = 0.4F + Math.floorMod(seed * 7L, 26L) / 100.0F;
            float y = 0.2F + Math.floorMod(seed * 3L, 190L) / 100.0F;

            float px = Mth.cos(ang) * radius;
            float pz = Mth.sin(ang) * radius;
            for (int seg = 0; seg < 6; seg++) {
                long s2 = seed + seg * 131L;
                float nx = px + (Math.floorMod(s2 * 17L, 100L) / 100.0F - 0.5F) * 0.5F;
                float ny = y + (Math.floorMod(s2 * 23L, 100L) / 100.0F - 0.5F) * 0.42F;
                float nz = pz + (Math.floorMod(s2 * 29L, 100L) / 100.0F - 0.5F) * 0.5F;
                // a hairline triangle stands in for a line segment
                tri(buf, m, px, y, pz, px + 0.025F, y + 0.025F, pz, nx, ny, nz,
                        0.8F, 0.97F, 1.0F, 0.95F);
                px = nx; y = ny; pz = nz;
            }
        }
    }

    // ---- orbiting sparks ------------------------------------------------------

    /** Small bright motes swirling around him in two counter-rotating rings - a constant sense of contained power. */
    private void orbitingSparks(PoseStack pose, VertexConsumer buf, float t) {
        Matrix4f m = pose.last().pose();
        final int SPARKS = 10;
        for (int i = 0; i < SPARKS; i++) {
            boolean outer = i % 2 == 0;
            float dir = outer ? 1.0F : -1.0F;
            float speed = outer ? 0.045F : -0.07F;
            float ang = (i / (float) SPARKS) * Mth.TWO_PI + t * speed * dir;
            float radius = outer ? 0.9F : 0.6F;
            float y = 0.6F + Mth.sin(t * 0.08F + i * 1.3F) * 0.55F + (outer ? 0.5F : 0.0F);

            float cx = Mth.cos(ang) * radius;
            float cz = Mth.sin(ang) * radius;
            float s = 0.05F + 0.02F * Mth.sin(t * 0.3F + i);

            tri(buf, m, cx - s, y - s, cz, cx + s, y - s, cz, cx, y + s, cz,
                    1.0F, 0.95F, 0.7F, 0.85F);
        }
    }

    // ---- ground shockwave -----------------------------------------------------

    /** The one big ring on the floor for the first second after ascending. */
    private void shockwave(PoseStack pose, VertexConsumer buf, CaydenCobb entity, float t) {
        int since = entity.ticksSinceAscension();
        if (since < 0 || since > 20) {
            return;
        }
        float p = since / 20.0F;                 // 0 -> 1
        float radius = 0.5F + p * 4.6F;
        float alpha = (1.0F - p) * 0.85F;
        ring(pose, buf, radius, radius - 0.28F, 0.02F, alpha, 1.0F, 0.9F, 0.5F);
    }

    /** Continuous, smaller pulses so the aura never goes quiet between fights or hits. */
    private void groundPulse(PoseStack pose, VertexConsumer buf, float t) {
        final float PERIOD = 45.0F;
        float phase = (t % PERIOD) / PERIOD;
        float radius = 0.6F + phase * 2.4F;
        float alpha = (1.0F - phase) * 0.4F;
        ring(pose, buf, radius, radius - 0.16F, 0.015F, alpha, 1.0F, 0.85F, 0.45F);
    }

    private void ring(PoseStack pose, VertexConsumer buf, float radius, float inner, float y,
                      float alpha, float r, float g, float b) {
        Matrix4f m = pose.last().pose();
        final int SEGMENTS = 32;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (i / (float) SEGMENTS) * Mth.TWO_PI;
            float a1 = ((i + 1) / (float) SEGMENTS) * Mth.TWO_PI;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            tri(buf, m, c0 * inner, y, s0 * inner, c1 * inner, y, s1 * inner,
                    c1 * radius, y, s1 * radius, r, g, b, alpha);
            tri(buf, m, c0 * inner, y, s0 * inner, c1 * radius, y, s1 * radius,
                    c0 * radius, y, s0 * radius, r, g, b, alpha);
        }
    }

    // ---- helpers --------------------------------------------------------------

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

    /** A genuine 4-corner quad, for the sky beam's rectangular column. */
    private void quad(VertexConsumer buf, Matrix4f m,
                      float x0, float y0, float x1, float y1,
                      float x2, float y2, float x3, float y3,
                      float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, 0.0F).color(r, g, b, a).endVertex();
        buf.vertex(m, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        buf.vertex(m, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        buf.vertex(m, x3, y3, 0.0F).color(r, g, b, a).endVertex();
    }
}
