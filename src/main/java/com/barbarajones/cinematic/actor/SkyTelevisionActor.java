package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Easing;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The set in the sky: a colossal wood-cabinet television hung over the death
 * site, which Barbara sits and watches through the <b>interview</b> beat.
 *
 * <p>The whole beat is the SCREEN, so everything else exists only to frame it.
 * The cabinet is deep, stepped and heavy because a thin panel floating in the
 * air reads as a poster; a picture tube with two and a half blocks of funnel
 * behind it reads as an object that was carried up there. The rabbit ears and
 * the two cream dials are there for the same reason - they are the silhouette
 * details that survive at sixty blocks and say "television" before the picture
 * has resolved at all.
 *
 * <p>The glass is faked curved twice over. Four nested plates step forward
 * toward the middle to give the faceplate its bulge when the set is dark, and
 * the raster on top of them is a grid of cells whose depth follows the same
 * dome, so the front of the screen is convex from any angle the camera orbits
 * to. A flat quad would have been cheaper and would have killed the shot.
 *
 * <p>Tracks:
 * <ul>
 *   <li>{@code power} 0..1 - screen on. It does not simply brighten: the raster
 *       opens out of a horizontal line the way a CRT does, which is the one
 *       switch-on nobody mistakes for a fade.</li>
 *   <li>{@code static} 0..1 - snow. Every cell is re-rolled off a deterministic
 *       integer hash of (cell, snow frame), so it is identical on every client,
 *       allocates nothing, and re-rolls on its own clock rather than on the
 *       frame rate - snow tied to framerate crawls on a slow machine.</li>
 *   <li>{@code buffer} 0..1 - the BUFFERING RING. This is the beat, so it is a
 *       real ring of sixteen additive segments with a lit arc chasing round it,
 *       more than half the screen across.</li>
 *   <li>{@code crack} 0..1 - a spiderweb spreading from an off-centre impact:
 *       seven jagged arms with the chords between them closing into rings, each
 *       shard held back until the shard feeding it has finished growing.</li>
 *   <li>{@code tumble} - degrees, for when the set is hurled. It drives three
 *       axes at once, because nothing thrown that heavy turns on a clean one.</li>
 * </ul>
 *
 * <p>{@link #screenLocal(float)} and {@link #screenCentre(float)} answer where
 * the middle of the glass is at a given scene time - the same service
 * {@link BluntActor#tip(float)} does for the coal, and the reason a shot can be
 * authored against the picture instead of against the actor's feet.
 */
public final class SkyTelevisionActor extends CinematicActor {

    private static final int COLS = 12;
    private static final int ROWS = 9;
    private static final int CELLS = COLS * ROWS;

    private static final int RING_SEGMENTS = 16;
    private static final int CRACK_ARMS = 7;
    private static final int CRACK_SEGMENTS = 4;

    /** Radial shards plus the chords between them plus the impact star. */
    private static final int CRACK_PARTS = CRACK_ARMS * (CRACK_SEGMENTS * 2 - 1) + 1;

    /** Segments of the buffering ring the lit head travels per tick. */
    private static final float BUFFER_SPIN = 0.55F;

    /** How many segments behind the head still carry light. */
    private static final float BUFFER_TAIL = 4.5F;

    /**
     * Snow re-rolls per tick. Deliberately not per render frame: noise keyed to
     * the frame counter runs at whatever speed the machine happens to manage,
     * so the same shot boils on one PC and crawls on another.
     */
    private static final float SNOW_RATE = 2.0F;

    /** The impact sits off-centre, in normalised screen coordinates. */
    private static final float HUB_U = -0.30F;
    private static final float HUB_V = 0.36F;

    // Rest angles for the ears, held as constants because pose() rewrites both
    // axes every frame to shake them and needs somewhere to shake them from.
    private static final float EAR_ROLL_R = -34.0F;
    private static final float EAR_ROLL_L = 39.0F;
    private static final float EAR_PITCH_R = -14.0F;
    private static final float EAR_PITCH_L = -21.0F;

    private static final int CASE = 0x6A5237;
    private static final int CASE_SIDE = 0x584430;
    private static final int CASE_BACK = 0x3E3121;
    private static final int BEZEL = 0x241F1B;
    private static final int PANEL = 0x7C5F3E;
    private static final int DIAL = 0xCBB78F;
    private static final int DIAL_RIM = 0x6E5C3E;
    private static final int GRILLE = 0x1A1714;
    private static final int METAL = 0x9BA2A8;
    private static final int METAL_DARK = 0x6E757B;
    private static final int GLASS_DEAD = 0x141A19;
    private static final int GLASS_EDGE = 0x1E2A29;
    private static final int CRACK_LINE = 0xE8EEF6;
    private static final int CRACK_WEB = 0xB9C6D4;

    /** Every dimension in the model is a multiple of this. */
    private final float unit;

    // Where the middle of the glass sits in the body's own frame, before any
    // tumble. Cached rather than recomputed: two public methods and nothing
    // else ever needs it, and neither is allowed to disagree with the geometry.
    private final float screenY;
    private final float screenZ;

    private final Part body;
    private final Part picture;
    private final Part bloom;
    private final Part flashLine;
    private final Part ringGroup;
    private final Part crackWeb;
    private final Part earR;
    private final Part earL;
    private final Part dialA;
    private final Part dialB;
    private final Part led;

    // Flat arrays rather than get("px" + i): the raster pass touches a hundred
    // boxes every frame and building that name would allocate a string on each.
    private final Part[] cell = new Part[CELLS];
    private final float[] cellV = new float[CELLS];
    private final int[] cellRow = new int[CELLS];
    private final float[] cellHub = new float[CELLS];
    private int cells;

    /** Scratch for this frame's per-row tear roll - a field so pose() allocates none. */
    private final float[] rowTear = new float[ROWS];

    private final Part[] ringSeg = new Part[RING_SEGMENTS];

    private final Part[] shard = new Part[CRACK_PARTS];
    private final float[] shardAt = new float[CRACK_PARTS];
    private int shards;

    /**
     * @param size multiplier on every dimension of the model. At 1.0 the set is
     *             2.4 units wide, matching {@link HumanoidActor}'s height, so an
     *             actor scale reads the same way it does for the cast - bind at
     *             16 and the television is about forty blocks across.
     */
    public SkyTelevisionActor(float size) {
        float k = Math.max(0.05F, size);
        this.unit = k;

        float hx = 1.20F * k;
        float hy = 0.93F * k;
        // Half the depth of the cabinet. As much again as the height, because
        // the funnel behind the tube is what stops this reading as a picture
        // frame the moment the camera comes off-axis.
        float hz = 0.86F * k;
        float w = hx * 2.0F;
        float h = hy * 2.0F;
        float d = hz * 2.0F;

        // The aperture sits high in the face so the strip of bezel underneath it
        // is deep enough to carry the controls - that asymmetry is most of what
        // makes a box read as a television rather than as a monitor.
        float shw = 0.80F * hx;
        float shh = 0.74F * hy;
        float scy = 0.07F * hy;

        float glassZ = hz - 0.04F * k;
        float cellD = 0.06F * k;
        float bulge = 0.13F * k;
        this.screenY = scy;
        this.screenZ = glassZ + cellD + bulge;

        this.body = part("body", this.root, 0.0F, 0.0F, 0.0F);

        buildCabinet(hx, hy, hz, w, h, d, k);
        buildBezel(hx, hy, hz, w, shw, shh, scy, k);

        Part glass = part("glass", this.body, 0.0F, scy, glassZ);
        buildFaceplate(glass, shw, shh, cellD, bulge);
        this.picture = part("picture", glass, 0.0F, 0.0F, 0.0F);
        buildRaster(shw, shh, cellD, bulge);

        // Over the raster and never scaled with it: the switch-on line has to
        // stay a line while the picture around it is still a slit.
        this.flashLine = part("flash", glass, 0.0F, 0.0F, cellD + bulge + 0.035F * k);
        this.flashLine.box(-shw, -0.020F * k, 0.0F, shw * 2.0F, 0.040F * k, 0.020F * k)
                .colour(1.0F, 0.98F, 0.92F).glow().alpha(0.0F);

        this.bloom = part("bloom", this.picture, 0.0F, 0.0F, cellD + bulge + 0.015F * k);
        this.bloom.box(-shw * 1.02F, -shh * 1.02F, 0.0F, shw * 2.04F, shh * 2.04F, 0.020F * k)
                .colour(0.62F, 0.76F, 1.0F).glow().alpha(0.0F);

        this.ringGroup = buildRing(shh, cellD, bulge, k);
        this.crackWeb = buildCracks(glass, shw, shh, cellD, bulge, k);

        Part mast = part("earMast", this.body, 0.0F, hy, -0.24F * hz);
        mast.box(-0.15F * k, 0.0F, -0.13F * k, 0.30F * k, 0.09F * k, 0.26F * k).colour(BEZEL);
        this.earR = antenna("R", mast, 0.10F * k, EAR_ROLL_R, EAR_PITCH_R,
                0.66F * k, 0.56F * k, -9.0F, -7.0F, false);
        // The left ear was bent years ago and never straightened, and somebody
        // wrapped foil round the end of it to chase a signal that never came.
        this.earL = antenna("L", mast, -0.10F * k, EAR_ROLL_L, EAR_PITCH_L,
                0.62F * k, 0.40F * k, 26.0F, -18.0F, true);

        float panelY = -hy + (hy + scy - shh) * 0.5F;
        float faceZ = hz - 0.06F * k + 0.13F * k;
        buildControls(hx, panelY, faceZ, k);
        this.dialA = get("dialA");
        this.dialB = get("dialB");
        this.led = get("led");
    }

    // ---- geometry ----------------------------------------------------------

    /**
     * Three slabs stepping inward toward the back. A single deep box has the
     * silhouette of a crate; a real set narrows behind the tube, and the two
     * shoulders that taper is what the eye uses to tell which way it is facing
     * while it tumbles.
     */
    private void buildCabinet(float hx, float hy, float hz, float w, float h, float d, float k) {
        part("caseFront", this.body, 0.0F, 0.0F, 0.0F)
                .box(-hx, -hy, hz - 0.34F * d, w, h, 0.34F * d).colour(CASE);
        part("caseMid", this.body, 0.0F, 0.0F, 0.0F)
                .box(-0.86F * hx, -0.86F * hy, hz - 0.71F * d,
                        1.72F * hx, 1.72F * hy, 0.38F * d).colour(CASE_SIDE);
        part("caseBack", this.body, 0.0F, 0.0F, 0.0F)
                .box(-0.60F * hx, -0.56F * hy, -hz,
                        1.20F * hx, 1.12F * hy, 0.32F * d).colour(CASE_BACK);

        // Ventilation slots along the back deck. They cost five boxes and they
        // are the difference between the back of the set being a surface and
        // being a thing that was manufactured.
        for (int i = 0; i < 5; i++) {
            part("vent" + i, this.body, 0.0F, 0.56F * hy, -hz + 0.05F * d + i * 0.055F * d)
                    .box(-0.44F * hx, 0.0F, 0.0F, 0.88F * hx, 0.012F * k, 0.026F * d)
                    .colour(GRILLE);
        }

        part("footR", this.body, 0.62F * hx, -hy, 0.28F * hz)
                .box(-0.11F * hx, -0.09F * k, -0.17F * hz, 0.22F * hx, 0.09F * k, 0.34F * hz)
                .colour(BEZEL);
        part("footL", this.body, -0.62F * hx, -hy, 0.28F * hz)
                .box(-0.11F * hx, -0.09F * k, -0.17F * hz, 0.22F * hx, 0.09F * k, 0.34F * hz)
                .colour(BEZEL);
    }

    /**
     * Four bars of surround standing proud of the cabinet face. Proud rather
     * than flush so the glass is visibly sunk into a rebate - a bezel drawn
     * level with the screen is just a border and reads as printed on.
     */
    private void buildBezel(float hx, float hy, float hz, float w,
                            float shw, float shh, float scy, float k) {
        float bz = hz - 0.06F * k;
        float bd = 0.13F * k;
        part("bezelTop", this.body, 0.0F, 0.0F, 0.0F)
                .box(-hx, scy + shh, bz, w, hy - scy - shh, bd).colour(BEZEL);
        part("bezelBottom", this.body, 0.0F, 0.0F, 0.0F)
                .box(-hx, -hy, bz, w, hy + scy - shh, bd).colour(BEZEL);
        part("bezelLeft", this.body, 0.0F, 0.0F, 0.0F)
                .box(-hx, scy - shh, bz, hx - shw, shh * 2.0F, bd).colour(BEZEL);
        part("bezelRight", this.body, 0.0F, 0.0F, 0.0F)
                .box(shw, scy - shh, bz, hx - shw, shh * 2.0F, bd).colour(BEZEL);
    }

    /**
     * The physical faceplate: four nested plates stepping forward toward the
     * middle. This is what the audience sees when the set is dark, and it has to
     * be convex on its own - if the curve lived only in the lit raster, then
     * switching the picture off would flatten the glass into a hole.
     */
    private void buildFaceplate(Part glass, float shw, float shh, float cellD, float bulge) {
        for (int i = 0; i < 4; i++) {
            float f = 1.00F - i * 0.235F;
            // Held just behind whatever the raster cells at the same radius will
            // sit at, so the picture always wins the depth test cleanly.
            float lift = bulge * 0.85F * Math.max(0.0F, 1.0F - 0.62F * f * f);
            part("plate" + i, glass, 0.0F, 0.0F, 0.0F)
                    .box(-shw * f, -shh * f, 0.0F, shw * f * 2.0F, shh * f * 2.0F,
                            cellD * 0.45F + lift)
                    .colour(i == 0 ? GLASS_EDGE : GLASS_DEAD);
        }
    }

    /**
     * The raster: a grid of cells, each pushed forward by how near the middle of
     * the dome it is, with the four corner cells dropped so the picture is a
     * rounded rectangle. Cells rather than one plate because the snow, the roll
     * bar and the shadow the fracture casts all have to vary ACROSS the screen,
     * and a single box can only ever be one colour.
     */
    private void buildRaster(float shw, float shh, float cellD, float bulge) {
        float cw = shw * 2.0F / COLS;
        float ch = shh * 2.0F / ROWS;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                float u = (c + 0.5F) / COLS * 2.0F - 1.0F;
                float v = (r + 0.5F) / ROWS * 2.0F - 1.0F;
                float du = Math.max(0.0F, Math.abs(u) - 0.60F) / 0.40F;
                float dv = Math.max(0.0F, Math.abs(v) - 0.60F) / 0.40F;
                if (du * du + dv * dv > 1.0F) {
                    continue;
                }
                float lift = bulge * Math.max(0.0F, 1.0F - 0.62F * (u * u + v * v));
                Part px = part("px" + (r * COLS + c), this.picture, u * shw, v * shh, 0.0F);
                // Cells overlap by a few percent: neighbours that meet exactly
                // leave hairlines the moment the actor is scaled up forty times.
                px.box(-cw * 0.53F, -ch * 0.53F, 0.0F, cw * 1.06F, ch * 1.06F, cellD + lift)
                        .colour(0.06F, 0.07F, 0.09F);

                float hu = u - HUB_U;
                float hv = v - HUB_V;
                this.cell[this.cells] = px;
                this.cellV[this.cells] = v;
                this.cellRow[this.cells] = r;
                this.cellHub[this.cells] =
                        Math.min(1.0F, (float) Math.sqrt(hu * hu + hv * hv) / 1.30F);
                this.cells++;
            }
        }
    }

    /**
     * The buffering ring, built the way {@link SmokerActor} builds a smoke O:
     * every segment pivoted on the circle and turned so its long axis runs
     * tangentially. The gaps between segments are load-bearing - a solid annulus
     * with a bright patch on it does not read as rotating, a dashed one does.
     */
    private Part buildRing(float shh, float cellD, float bulge, float k) {
        float rr = 0.62F * shh;
        Part ring = part("buffer", this.picture, 0.0F, 0.0F, cellD + bulge + 0.045F * k);
        ring.visible = false;
        float len = (float) (Math.PI * 2.0D * rr / RING_SEGMENTS) * 0.72F;
        float thick = rr * 0.20F;
        for (int s = 0; s < RING_SEGMENTS; s++) {
            double a = s * Math.PI * 2.0D / RING_SEGMENTS;
            Part seg = part("buf" + s, ring,
                    (float) Math.cos(a) * rr, (float) Math.sin(a) * rr, 0.0F);
            seg.rotZ = (float) Math.toDegrees(a) + 90.0F;
            seg.box(-len * 0.5F, -thick * 0.5F, 0.0F, len, thick, 0.05F * k)
                    .colour(0.30F, 0.38F, 0.50F).glow().alpha(0.0F);
            this.ringSeg[s] = seg;
        }
        return ring;
    }

    /**
     * The spiderweb. Each arm is a chain of shards, each parented to the end of
     * the one before it, so hiding a shard hides everything downstream of it and
     * the fracture propagates outward for free. The chords all turn the same way
     * onto the next arm, which closes them into concentric rings - that ring is
     * the difference between a starburst and cracked glass.
     */
    private Part buildCracks(Part glass, float shw, float shh,
                             float cellD, float bulge, float k) {
        Part web = part("cracks", glass, HUB_U * shw, HUB_V * shh,
                cellD + bulge + 0.060F * k);
        web.visible = false;

        Part star = part("crackStar", web, 0.0F, 0.0F, 0.0F);
        star.box(-0.048F * k, -0.048F * k, 0.0F, 0.096F * k, 0.096F * k, 0.020F * k)
                .colour(CRACK_LINE).alpha(0.0F);
        remember(star, 0.0F);

        float step = 1.0F / (CRACK_SEGMENTS + 1);
        // Chord from a point on a ring to the same ring on the next arm: the
        // angle off the radial is a right angle plus half the arm spacing.
        float chordTurn = 90.0F + 180.0F / CRACK_ARMS;
        float hubX = HUB_U * shw;
        float hubY = HUB_V * shh;

        for (int a = 0; a < CRACK_ARMS; a++) {
            // Never evenly spaced and never all the same speed: seven arms at
            // exact sevenths arriving together is a snowflake, not a fracture.
            float base = a * 360.0F / CRACK_ARMS + ((a * 11) % 7) * 2.6F;
            float lag = ((a * 7) % 5) / 5.0F * 0.55F;
            // Every arm is cut to where its own ray leaves the aperture, so the
            // web fills the glass at any size and no fracture is left drawn
            // across the wooden surround, which would read as a scratch on the
            // cabinet rather than as broken glass.
            float baseRad = base * Mth.DEG_TO_RAD;
            float edge = 0.96F * toEdge(baseRad, hubX, hubY, shw, shh);
            float weight = 0.0F;
            for (int i = 0; i < CRACK_SEGMENTS; i++) {
                weight += segWeight(a, i);
            }
            float unitLen = edge / weight;

            Part parent = web;
            float radius = 0.0F;
            float prev = 0.0F;

            for (int i = 0; i < CRACK_SEGMENTS; i++) {
                float len = unitLen * segWeight(a, i);
                Part seg = part("crk" + a + "_" + i, parent, prev, 0.0F, 0.0F);
                seg.rotZ = i == 0 ? base
                        : ((a + i) % 2 == 0 ? 9.0F : -11.0F) + ((i * 5) % 3) * 3.0F;
                float t = 0.024F * k * (1.0F - 0.45F * i / (float) CRACK_SEGMENTS);
                seg.box(0.0F, -t, 0.0F, len, t * 2.0F, 0.020F * k)
                        .colour(CRACK_LINE).alpha(0.0F);
                remember(seg, (i + lag) * step);
                radius += len;

                if (i < CRACK_SEGMENTS - 1) {
                    Part chord = part("web" + a + "_" + i, seg, len, 0.0F, 0.0F);
                    chord.rotZ = chordTurn;
                    float ct = 0.016F * k;
                    // Reach for the same ring on the next arm, but stop at the
                    // glass: the arms are each cut to their own edge, so on the
                    // side where the impact sits close to the frame a full-length
                    // chord would sail off the aperture and onto the woodwork.
                    float chordLen = Math.min(
                            2.0F * radius * (float) Math.sin(Math.PI / CRACK_ARMS),
                            0.98F * toEdge(baseRad + chordTurn * Mth.DEG_TO_RAD,
                                    hubX + Mth.cos(baseRad) * radius,
                                    hubY + Mth.sin(baseRad) * radius, shw, shh));
                    chord.box(0.0F, -ct, 0.0F, chordLen, ct * 2.0F, 0.018F * k)
                            .colour(CRACK_WEB).alpha(0.0F);
                    // One full step behind its own arm segment, so a chord never
                    // appears hanging off a shard that is still growing.
                    remember(chord, (i + 1 + lag) * step);
                }

                prev = len;
                parent = seg;
            }
        }
        return web;
    }

    /**
     * How far a ray from a point inside the aperture travels before it crosses
     * the edge of it. A slab test rather than a radius, because the glass is a
     * rectangle: a circular web on a 4:3 screen leaves the left and right
     * thirds of it untouched.
     */
    private static float toEdge(float angle, float ox, float oy, float shw, float shh) {
        float dx = Mth.cos(angle);
        float dy = Mth.sin(angle);
        float tx = Math.abs(dx) < 0.0001F ? Float.MAX_VALUE
                : ((dx > 0.0F ? shw : -shw) - ox) / dx;
        float ty = Math.abs(dy) < 0.0001F ? Float.MAX_VALUE
                : ((dy > 0.0F ? shh : -shh) - oy) / dy;
        return Math.min(tx, ty);
    }

    /** Relative length of one arm segment - the same jitter every time it is asked. */
    private static float segWeight(int arm, int seg) {
        return 0.72F + 0.42F * (((arm * 5 + seg * 3) % 7) / 6.0F);
    }

    /** One telescoping ear: two tapering rods and a knob, optionally foiled. */
    private Part antenna(String side, Part mast, float x, float roll, float pitch,
                         float len1, float len2, float kinkRoll, float kinkPitch,
                         boolean foil) {
        float k = this.unit;
        Part rod = part("ear" + side, mast, x, 0.08F * k, 0.0F);
        rod.rot(pitch, 0.0F, roll);
        rod.box(-0.026F * k, 0.0F, -0.026F * k, 0.052F * k, len1, 0.052F * k).colour(METAL);

        Part upper = part("ear" + side + "2", rod, 0.0F, len1, 0.0F);
        upper.rot(kinkPitch, 0.0F, kinkRoll);
        upper.box(-0.018F * k, 0.0F, -0.018F * k, 0.036F * k, len2, 0.036F * k)
                .colour(METAL_DARK);

        Part tip = part("ear" + side + "Tip", upper, 0.0F, len2, 0.0F);
        tip.box(-0.032F * k, 0.0F, -0.032F * k, 0.064F * k, 0.064F * k, 0.064F * k)
                .colour(0x3A3F44);
        if (foil) {
            part("ear" + side + "Foil", tip, 0.0F, 0.03F * k, 0.0F)
                    .rot(12.0F, 24.0F, 9.0F)
                    .box(-0.075F * k, 0.0F, -0.068F * k, 0.150F * k, 0.130F * k, 0.136F * k)
                    .colour(0xBFC6CB);
        }
        return rod;
    }

    /** Speaker grille, two dials and the power lamp, along the bottom bezel. */
    private void buildControls(float hx, float panelY, float faceZ, float k) {
        Part panel = part("panel", this.body, 0.55F * hx, panelY, faceZ);
        panel.box(-0.40F * hx, -0.115F * k, 0.0F, 0.80F * hx, 0.230F * k, 0.05F * k)
                .colour(PANEL);

        for (int i = 0; i < 4; i++) {
            part("grille" + i, this.body, -0.46F * hx, panelY - 0.075F * k + i * 0.05F * k,
                    faceZ).box(-0.30F * hx, -0.012F * k, 0.0F, 0.60F * hx, 0.024F * k,
                            0.02F * k).colour(GRILLE);
        }

        dial("dialA", 0.42F * hx, panelY, faceZ + 0.05F * k, 0.085F * k);
        dial("dialB", 0.74F * hx, panelY, faceZ + 0.05F * k, 0.068F * k);

        part("led", this.body, -0.82F * hx, panelY, faceZ)
                .box(-0.028F * k, -0.028F * k, 0.0F, 0.056F * k, 0.056F * k, 0.030F * k)
                .colour(0xFF3A24).glow().alpha(0.0F);
    }

    /** A knob with a pointer on it, so that turning it is visible from below. */
    private void dial(String name, float x, float y, float z, float r) {
        float k = this.unit;
        Part hub = part(name, this.body, x, y, z);
        hub.box(-r, -r, 0.0F, r * 2.0F, r * 2.0F, 0.055F * k).colour(DIAL);
        part(name + "Rim", hub, 0.0F, 0.0F, 0.0F)
                .box(-r * 1.24F, -r * 1.24F, -0.012F * k,
                        r * 2.48F, r * 2.48F, 0.020F * k).colour(DIAL_RIM);
        part(name + "Mark", hub, 0.0F, 0.0F, 0.0F)
                .box(-r * 0.14F, 0.0F, 0.055F * k, r * 0.28F, r * 1.05F, 0.012F * k)
                .colour(0x1A1512);
    }

    private void remember(Part p, float at) {
        if (this.shards >= CRACK_PARTS) {
            return;
        }
        this.shard[this.shards] = p;
        this.shardAt[this.shards] = at;
        this.shards++;
    }

    // ---- animation ---------------------------------------------------------

    @Override
    protected void pose(float time) {
        this.body.rotX = bodyPitch(time);
        this.body.rotY = bodyYaw(time);
        this.body.rotZ = bodyRoll(time);

        float power = Mth.clamp(track("power", time, 0.0F), 0.0F, 1.0F);
        float snow = Mth.clamp(track("static", time, 0.0F), 0.0F, 1.0F);
        float buffer = Mth.clamp(track("buffer", time, 0.0F), 0.0F, 1.0F);
        float crack = Mth.clamp(track("crack", time, 0.0F), 0.0F, 1.0F);

        raster(time, power, snow, buffer, crack);
        buffering(time, power, buffer);
        fractures(crack);
        antennae(time, snow);
        controls(time, power, snow);
    }

    /**
     * The picture. Three things are happening at once and they have to stay
     * separable: the raster opening on power, the snow riding on top of it, and
     * the buffering beat pulling the whole thing down toward black so the ring
     * has something to be bright against.
     */
    private void raster(float time, float power, float snow, float buffer, float crack) {
        // A CRT does not fade up, it unfolds: the beam is a line first and the
        // vertical deflection opens it into a picture over a fraction of a second.
        float open = Easing.CUBIC_OUT.apply(Mth.clamp(power / 0.55F, 0.0F, 1.0F));
        this.picture.scaleY = Math.max(0.006F, open);
        // All of the tube's energy is in that band while it is still a slit,
        // which is why the line is blinding and the opened picture is not.
        float squeeze = 1.0F + (1.0F - open) * 2.4F;
        this.flashLine.alpha =
                Mth.clamp((1.0F - open) * Math.min(1.0F, power * 6.0F), 0.0F, 0.90F);

        int frame = Mth.floor(time * SNOW_RATE);
        // Mains hum, and the vertical hold slipping: a bright band crawling down
        // the picture. Both are cheap and both say "this set is failing".
        float hum = 1.0F - 0.05F * Mth.sin(time * 1.87F);
        float bar = time * 0.011F;
        bar -= Mth.floor(bar);
        bar = 1.2F - bar * 2.4F;
        float damp = 1.0F - 0.72F * buffer;

        // One roll shared by a whole row, so the snow tears into horizontal
        // bands now and then instead of being uniform fizz. Rolled per row
        // rather than per cell because a band has to be a band.
        for (int r = 0; r < ROWS; r++) {
            this.rowTear[r] = hash01(r * 37 + 11, frame) > 0.86F ? 0.34F * snow : 0.0F;
        }

        for (int i = 0; i < this.cells; i++) {
            float n = hash01(i, frame);
            float band = Math.max(0.0F, 1.0F - Math.abs(this.cellV[i] - bar) * 3.4F);
            float lit = 0.09F + 0.17F * band;
            lit += (0.13F + 0.87F * n - lit) * snow;
            lit += this.rowTear[this.cellRow[i]];
            // The picture dies around the impact first - a fracture that leaves
            // the image behind it perfect reads as a decal on the glass.
            lit *= 1.0F - 0.55F * crack * (1.0F - this.cellHub[i]);
            lit *= damp * squeeze * hum;

            // Powered off is dark glass, not nothing: the tube is still there.
            float lum = Mth.clamp(0.05F + (lit - 0.05F) * power, 0.0F, 1.0F);
            this.cell[i].colour(lum * 0.88F, lum * 0.94F, Math.min(1.0F, lum * 1.06F));
        }

        this.bloom.alpha =
                Mth.clamp(power * (0.05F + 0.14F * snow + 0.26F * buffer), 0.0F, 0.34F);
    }

    /**
     * The lit arc chasing round the ring. The head runs backwards through the
     * segment indices because the screen faces +Z, so ascending angle sweeps
     * anticlockwise for anyone looking at it - and a buffering spinner that
     * turns the wrong way reads as broken rather than as waiting.
     */
    private void buffering(float time, float power, float buffer) {
        if (buffer <= 0.004F || power <= 0.02F) {
            this.ringGroup.visible = false;
            return;
        }
        this.ringGroup.visible = true;
        float grow = 0.62F + 0.38F * buffer;
        this.ringGroup.scale(grow, grow, 1.0F);

        float head = -time * BUFFER_SPIN;
        for (int s = 0; s < RING_SEGMENTS; s++) {
            float back = s - head;
            back -= RING_SEGMENTS * Mth.floor(back / RING_SEGMENTS);
            float lit = Math.max(0.0F, 1.0F - back / BUFFER_TAIL);
            // Squared, so the head is a hot point with a tail rather than an
            // evenly bright quarter of the ring.
            lit *= lit;

            Part seg = this.ringSeg[s];
            seg.colour(0.24F + 0.62F * lit, 0.32F + 0.64F * lit, 0.44F + 0.56F * lit);
            seg.alpha = Mth.clamp(buffer * (0.16F + 0.84F * lit), 0.0F, 1.0F);
            // The lit segments also swell and stand proud, so the arc survives
            // even when the additive colour is washed out by a bright sky.
            float bump = 1.0F + lit * 0.42F;
            seg.scale(1.0F, bump, bump);
            seg.offZ = lit * 0.05F * this.unit;
        }
    }

    /** Grow the web outward from the impact, one ring of shards at a time. */
    private void fractures(float crack) {
        if (crack <= 0.002F) {
            this.crackWeb.visible = false;
            return;
        }
        this.crackWeb.visible = true;
        float step = 1.0F / (CRACK_SEGMENTS + 1);
        for (int i = 0; i < this.shards; i++) {
            Part s = this.shard[i];
            float head = (crack - this.shardAt[i]) / step;
            if (head <= 0.0F) {
                s.visible = false;
                continue;
            }
            s.visible = true;
            // Shards run along their own +X, so one scale splits them open, and
            // whatever hangs off the far end is still hidden while it does.
            s.scaleX = Math.min(1.0F, head);
            s.alpha = Math.min(0.95F, 0.34F + 0.61F * head);
        }
    }

    /** The ears shake with the snow - the bad picture has to have a cause. */
    private void antennae(float time, float snow) {
        float quiver = snow * 3.2F;
        this.earR.rotZ = EAR_ROLL_R + Mth.sin(time * 1.29F) * quiver;
        this.earR.rotX = EAR_PITCH_R + Mth.cos(time * 0.97F) * quiver * 0.6F;
        this.earL.rotZ = EAR_ROLL_L + Mth.sin(time * 1.11F + 2.2F) * quiver;
        this.earL.rotX = EAR_PITCH_L + Mth.cos(time * 0.83F + 1.1F) * quiver * 0.6F;
    }

    /** Somebody off-screen keeps working the tuning dial, and it keeps failing. */
    private void controls(float time, float power, float snow) {
        this.dialA.rotZ = Mth.sin(time * 0.37F) * 26.0F * snow;
        this.dialB.rotZ = -18.0F + Mth.sin(time * 0.13F + 1.4F) * 7.0F * snow;
        this.led.alpha = Mth.clamp(power * (0.72F + 0.28F * Mth.sin(time * 0.60F)),
                0.0F, 1.0F);
    }

    // The tumble, split across three axes with an idle sway underneath it. Kept
    // as functions rather than as fields because screenCentre() has to be able
    // to ask where the glass was pointing at a time that is not this frame.

    private float bodyPitch(float time) {
        return track("tumble", time, 0.0F) + Mth.sin(time * 0.041F) * 1.3F;
    }

    private float bodyYaw(float time) {
        return track("tumble", time, 0.0F) * 0.17F + Mth.sin(time * 0.029F + 1.7F) * 2.1F;
    }

    private float bodyRoll(float time) {
        return track("tumble", time, 0.0F) * 0.31F + Mth.sin(time * 0.034F + 0.6F) * 1.6F;
    }

    // ---- where the picture actually is -------------------------------------

    /**
     * The middle of the glass as an offset from the actor's own origin, in model
     * units, at this scene time - what a scene aims a camera at, hangs sparks
     * off, or throws a reflection from.
     *
     * <p>Takes a time because the set tumbles: the screen centre swings a long
     * way round the cabinet once it has been hurled, and an answer from the
     * model's rest pose would put the shot on the back of the box.
     *
     * <p>The three rotations are applied in the same order
     * {@link CinematicActor#render} applies them to a part - Z, then Y, then X -
     * so this cannot drift away from where the geometry ends up.
     */
    public Vec3 screenLocal(float time) {
        float rx = bodyPitch(time) * Mth.DEG_TO_RAD;
        float ry = bodyYaw(time) * Mth.DEG_TO_RAD;
        float rz = bodyRoll(time) * Mth.DEG_TO_RAD;

        float c = Mth.cos(rx);
        float s = Mth.sin(rx);
        float y1 = this.screenY * c - this.screenZ * s;
        float z1 = this.screenY * s + this.screenZ * c;

        c = Mth.cos(ry);
        s = Mth.sin(ry);
        float x2 = z1 * s;
        float z2 = z1 * c;

        c = Mth.cos(rz);
        s = Mth.sin(rz);
        return new Vec3(x2 * c - y1 * s, x2 * s + y1 * c, z2);
    }

    /**
     * World point of the screen centre at this scene time.
     *
     * <p>Rebuilt from the tracks the same way {@link CinematicActor#render}
     * rebuilds the actor's transform, rather than read off whatever the last
     * frame happened to leave behind, so a cue firing on a whole tick gets the
     * answer for that tick and not for the frame before it.
     */
    public Vec3 screenCentre(float time) {
        Vec3 local = screenLocal(time);
        float yaw = track("yaw", time, this.baseYaw) * Mth.DEG_TO_RAD;
        float sc = Math.max(0.001F, track("scale", time, this.baseScale));
        double x = local.x * sc;
        double z = local.z * sc;
        float sy = Mth.sin(yaw);
        float cy = Mth.cos(yaw);
        return this.basePos.add(
                track("dx", time, 0.0F) + x * cy + z * sy,
                track("dy", time, 0.0F) + local.y * sc,
                track("dz", time, 0.0F) - x * sy + z * cy);
    }

    /**
     * Deterministic 0..1 from two integers. Every client watching this beat has
     * to see the SAME snow - it is a broadcast, and two players stood together
     * seeing different noise breaks the illusion instantly - and it has to cost
     * nothing, because this runs a hundred and eight times a frame.
     */
    private static float hash01(int a, int b) {
        int h = a * 0x27D4EB2D + b * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return (h >>> 8) * (1.0F / 16777216.0F);
    }
}
