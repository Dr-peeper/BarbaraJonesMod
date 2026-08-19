package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Timeline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Base class for a cinematic actor: a tree of articulated box parts, driven off
 * a {@link Timeline}, drawn in world space.
 *
 * <p>The sky actors used to be camera-facing quads with a texture on them, which
 * is why they read as cardboard - a billboard has no silhouette change when the
 * camera moves, so the brain never accepts it as an object with volume. Every
 * actor here is real geometry with real joints, so orbiting it reveals its
 * shape, and a limb can lead or lag the body.
 *
 * <p>Two rendering paths, both of which must be fed correctly or the game dies
 * at draw time:
 * <ul>
 *   <li>solid and textured parts go through the NEW_ENTITY vertex format, where
 *       every single vertex needs position, colour, uv, overlay, light AND
 *       normal - miss one and BufferBuilder throws "Not filled all elements of
 *       the vertex";</li>
 *   <li>glowing parts go through {@link RenderType#lightning()}, which is
 *       POSITION_COLOR - position and colour ONLY, additive, no depth write.</li>
 * </ul>
 *
 * <p>Faces are shaded into their vertex colours the way Minecraft shades block
 * faces (top brightest, bottom darkest, sides between). Without it a box lit at
 * full brightness is a flat silhouette with no readable form.
 */
public abstract class CinematicActor {

    /** Vanilla's plain white texture - all colour comes from the vertices. */
    public static final ResourceLocation WHITE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    public static final RenderType SOLID = RenderType.entitySolid(WHITE);
    public static final RenderType TRANSLUCENT = RenderType.entityTranslucent(WHITE);
    public static final RenderType GLOW = RenderType.lightning();

    /** Sky actors are above the world, so they are lit as if in full daylight. */
    protected static final int SKY_LIGHT = LightTexture.FULL_BRIGHT;

    // Face shading matching vanilla's block diffuse, baked into vertex colour.
    private static final float SHADE_TOP = 1.0F;
    private static final float SHADE_BOTTOM = 0.5F;
    private static final float SHADE_NS = 0.8F;
    private static final float SHADE_EW = 0.62F;

    /**
     * Every render type any actor touched this frame, in the order it was first
     * touched. The director flushes these explicitly so the additive glow is
     * always drawn last, over finished solids.
     */
    private static final LinkedHashSet<RenderType> USED = new LinkedHashSet<>();

    /** One articulated joint, optionally carrying a box, plus its children. */
    public static final class Part {

        public final String name;
        public final List<Part> children = new ArrayList<>();

        /** Joint position relative to the parent's origin. */
        public float pivotX;
        public float pivotY;
        public float pivotZ;

        /** Rotation about the joint, in degrees, applied Z then Y then X. */
        public float rotX;
        public float rotY;
        public float rotZ;

        /** Translation applied after rotation - for slides that are not swings. */
        public float offX;
        public float offY;
        public float offZ;

        public float scaleX = 1.0F;
        public float scaleY = 1.0F;
        public float scaleZ = 1.0F;

        /** Box minimum corner relative to the joint, and its size. */
        public float boxX;
        public float boxY;
        public float boxZ;
        public float sizeX;
        public float sizeY;
        public float sizeZ;

        public float red = 1.0F;
        public float green = 1.0F;
        public float blue = 1.0F;
        public float alpha = 1.0F;

        public boolean glowing;
        public boolean visible = true;
        public ResourceLocation skin;

        Part(String name) {
            this.name = name;
        }

        public Part box(float x, float y, float z, float w, float h, float d) {
            this.boxX = x;
            this.boxY = y;
            this.boxZ = z;
            this.sizeX = w;
            this.sizeY = h;
            this.sizeZ = d;
            return this;
        }

        public Part colour(float r, float g, float b) {
            this.red = r;
            this.green = g;
            this.blue = b;
            return this;
        }

        public Part colour(int rgb) {
            return colour(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F);
        }

        public Part alpha(float a) {
            this.alpha = a;
            return this;
        }

        public Part glow() {
            this.glowing = true;
            return this;
        }

        public Part skin(ResourceLocation texture) {
            this.skin = texture;
            return this;
        }

        public Part rot(float x, float y, float z) {
            this.rotX = x;
            this.rotY = y;
            this.rotZ = z;
            return this;
        }

        public Part offset(float x, float y, float z) {
            this.offX = x;
            this.offY = y;
            this.offZ = z;
            return this;
        }

        public Part scale(float x, float y, float z) {
            this.scaleX = x;
            this.scaleY = y;
            this.scaleZ = z;
            return this;
        }

        public Part show(boolean v) {
            this.visible = v;
            return this;
        }
    }

    protected final Part root = new Part("root");
    private final Map<String, Part> index = new HashMap<>();

    /** Timeline this actor reads its animation from, and its track name prefix. */
    protected Timeline timeline;
    protected String id = "actor";

    protected Vec3 basePos = Vec3.ZERO;
    protected float baseYaw;
    protected float baseScale = 1.0F;

    public Vec3 pos = Vec3.ZERO;
    public float yaw;
    public float scale = 1.0F;
    public boolean visible = true;

    /**
     * Multiplied into every part's alpha. The director winds it down over the
     * hand-back so the giants dissolve behind the departing camera - a forty
     * block carton that simply blinks out of existence undoes the whole beat.
     */
    public float opacity = 1.0F;

    protected CinematicActor() {
        this.index.put("root", this.root);
    }

    /** Bind this actor to the timeline that drives it. Tracks are "id.name". */
    public CinematicActor bind(Timeline timeline, String id, Vec3 at, float yaw, float scale) {
        this.timeline = timeline;
        this.id = id;
        this.basePos = at;
        this.baseYaw = yaw;
        this.baseScale = scale;
        this.pos = at;
        this.yaw = yaw;
        this.scale = scale;
        return this;
    }

    // ---- rig building ------------------------------------------------------

    protected Part part(String name, Part parent, float pivotX, float pivotY, float pivotZ) {
        Part p = new Part(name);
        p.pivotX = pivotX;
        p.pivotY = pivotY;
        p.pivotZ = pivotZ;
        parent.children.add(p);
        this.index.put(name, p);
        return p;
    }

    /** Never returns null: a typo in a part name must not crash a cutscene. */
    public Part get(String name) {
        Part p = this.index.get(name);
        return p == null ? this.root : p;
    }

    // ---- animation ---------------------------------------------------------

    /** Sample one of this actor's own tracks. */
    protected float track(String name, float time, float fallback) {
        return this.timeline == null ? fallback : this.timeline.value(this.id + "." + name, time, fallback);
    }

    /**
     * Pose the rig for this instant. Subclasses map timeline values onto joints
     * here and add whatever secondary motion the character owns.
     */
    protected abstract void pose(float time);

    /** Point the actor at a world position, using the model's +Z as its front. */
    public void faceTowards(Vec3 target) {
        double dx = target.x - this.pos.x;
        double dz = target.z - this.pos.z;
        if (dx * dx + dz * dz > 0.0001D) {
            this.yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        }
    }

    // ---- rendering ---------------------------------------------------------

    /**
     * Position tracks are OFFSETS from the bind position, never world coordinates:
     * tracks are float, and out at a few million blocks a float has lost enough
     * precision to make a giant visibly stutter in place.
     */
    public void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camPos, float time) {
        this.pos = this.basePos.add(
                track("dx", time, 0.0F),
                track("dy", time, 0.0F),
                track("dz", time, 0.0F));
        this.yaw = track("yaw", time, this.baseYaw);
        this.scale = Math.max(0.001F, track("scale", time, this.baseScale));
        this.visible = track("show", time, 1.0F) > 0.5F;
        if (!this.visible) {
            return;
        }

        pose(time);

        poseStack.pushPose();
        poseStack.translate(this.pos.x - camPos.x, this.pos.y - camPos.y, this.pos.z - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.yaw));
        poseStack.scale(this.scale, this.scale, this.scale);
        renderPart(this.root, poseStack, buffers);
        poseStack.popPose();
    }

    private void renderPart(Part p, PoseStack poseStack, MultiBufferSource buffers) {
        if (!p.visible) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(p.pivotX, p.pivotY, p.pivotZ);
        if (p.rotZ != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(p.rotZ));
        }
        if (p.rotY != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(p.rotY));
        }
        if (p.rotX != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(p.rotX));
        }
        poseStack.scale(Math.max(0.001F, p.scaleX), Math.max(0.001F, p.scaleY),
                Math.max(0.001F, p.scaleZ));
        poseStack.translate(p.offX, p.offY, p.offZ);

        float alpha = p.alpha * this.opacity;
        if (p.sizeX > 0.0F && p.sizeY > 0.0F && p.sizeZ > 0.0F && alpha > 0.004F) {
            if (p.glowing) {
                glowBox(poseStack, buffers.getBuffer(use(GLOW)), p, alpha);
            } else {
                // A cutout type discards below its alpha threshold and is fully
                // opaque above it, so anything mid-fade has to move to a blended
                // type or it would stay solid and then vanish in one frame.
                RenderType type;
                if (p.skin != null) {
                    type = alpha >= 0.999F
                            ? RenderType.entityCutoutNoCull(p.skin)
                            : RenderType.entityTranslucent(p.skin);
                } else {
                    type = alpha >= 0.999F ? SOLID : TRANSLUCENT;
                }
                litBox(poseStack, buffers.getBuffer(use(type)), p, alpha);
            }
        }
        for (int i = 0; i < p.children.size(); i++) {
            renderPart(p.children.get(i), poseStack, buffers);
        }
        poseStack.popPose();
    }

    /** A textured/solid box. Every vertex carries the full NEW_ENTITY attribute set. */
    private static void litBox(PoseStack poseStack, VertexConsumer buf, Part p, float alpha) {
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();
        float x0 = p.boxX;
        float x1 = p.boxX + p.sizeX;
        float y0 = p.boxY;
        float y1 = p.boxY + p.sizeY;
        float z0 = p.boxZ;
        float z1 = p.boxZ + p.sizeZ;

        litFace(buf, m, n, p, alpha, SHADE_NS, 0.0F, 0.0F, -1.0F,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        litFace(buf, m, n, p, alpha, SHADE_NS, 0.0F, 0.0F, 1.0F,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        litFace(buf, m, n, p, alpha, SHADE_EW, -1.0F, 0.0F, 0.0F,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        litFace(buf, m, n, p, alpha, SHADE_EW, 1.0F, 0.0F, 0.0F,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        litFace(buf, m, n, p, alpha, SHADE_TOP, 0.0F, 1.0F, 0.0F,
                x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        litFace(buf, m, n, p, alpha, SHADE_BOTTOM, 0.0F, -1.0F, 0.0F,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private static void litFace(VertexConsumer buf, Matrix4f m, Matrix3f n, Part p, float alpha,
                                float shade, float nx, float ny, float nz,
                                float ax, float ay, float az, float bx, float by, float bz,
                                float cx, float cy, float cz, float dx, float dy, float dz) {
        float r = p.red * shade;
        float g = p.green * shade;
        float b = p.blue * shade;
        litVertex(buf, m, n, r, g, b, alpha, ax, ay, az, 0.0F, 0.0F, nx, ny, nz);
        litVertex(buf, m, n, r, g, b, alpha, bx, by, bz, 1.0F, 0.0F, nx, ny, nz);
        litVertex(buf, m, n, r, g, b, alpha, cx, cy, cz, 1.0F, 1.0F, nx, ny, nz);
        litVertex(buf, m, n, r, g, b, alpha, dx, dy, dz, 0.0F, 1.0F, nx, ny, nz);
    }

    private static void litVertex(VertexConsumer buf, Matrix4f m, Matrix3f n,
                                  float r, float g, float b, float a,
                                  float x, float y, float z, float u, float v,
                                  float nx, float ny, float nz) {
        buf.vertex(m, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(SKY_LIGHT)
                .normal(n, nx, ny, nz)
                .endVertex();
    }

    /**
     * An additive glowing box. RenderType.lightning() is POSITION_COLOR and draws
     * QUADS, so these vertices get position and colour and nothing else - adding
     * uv or normal here would corrupt the buffer just as surely as omitting them
     * does on the entity format.
     */
    private static void glowBox(PoseStack poseStack, VertexConsumer buf, Part p, float alpha) {
        Matrix4f m = poseStack.last().pose();
        float x0 = p.boxX;
        float x1 = p.boxX + p.sizeX;
        float y0 = p.boxY;
        float y1 = p.boxY + p.sizeY;
        float z0 = p.boxZ;
        float z1 = p.boxZ + p.sizeZ;

        glowFace(buf, m, p, alpha, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        glowFace(buf, m, p, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        glowFace(buf, m, p, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        glowFace(buf, m, p, alpha, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        glowFace(buf, m, p, alpha, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        glowFace(buf, m, p, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
    }

    private static void glowFace(VertexConsumer buf, Matrix4f m, Part p, float alpha,
                                 float ax, float ay, float az, float bx, float by, float bz,
                                 float cx, float cy, float cz, float dx, float dy, float dz) {
        buf.vertex(m, ax, ay, az).color(p.red, p.green, p.blue, alpha).endVertex();
        buf.vertex(m, bx, by, bz).color(p.red, p.green, p.blue, alpha).endVertex();
        buf.vertex(m, cx, cy, cz).color(p.red, p.green, p.blue, alpha).endVertex();
        buf.vertex(m, dx, dy, dz).color(p.red, p.green, p.blue, alpha).endVertex();
    }

    private static RenderType use(RenderType type) {
        USED.add(type);
        return type;
    }

    /**
     * Flush everything the actors drew. Solids first so they own the depth
     * buffer, additive glow last so it lays over finished geometry.
     */
    public static void flush(MultiBufferSource.BufferSource source) {
        for (RenderType type : USED) {
            if (type != GLOW) {
                source.endBatch(type);
            }
        }
        source.endBatch(GLOW);
        USED.clear();
    }
}
