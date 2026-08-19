package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Easing;

import net.minecraft.util.Mth;

/**
 * The gates of hell, lowered into the sky over the death site and ground open.
 *
 * <p>This replaces the flat textured sheet the client used to hang overhead. That
 * sheet was one horizontal quad about a hundred blocks wide with no thickness, no
 * edges and nothing below it, so every camera move left its silhouette identical -
 * which is exactly what "the gates move with the camera" means. The transform was
 * never wrong. A plane simply has no parallax to give: nothing on it is nearer to
 * you than anything else on it, so the brain has nothing to solve and files the
 * whole thing as an overlay stuck to the lens.
 *
 * <p>So this is a building. Everything here exists to put geometry at DIFFERENT
 * DISTANCES from a viewer standing underneath:
 * <ul>
 *   <li>the frame is a solid ring a good eighth of the span thick, and the doors
 *       are recessed up inside it, so from any angle off the vertical the near
 *       jamb visibly slides across the far one;</li>
 *   <li>ten pillars hang below the opening, half a span long, at four different
 *       lengths and ten different ground positions - they are the main parallax
 *       engine, because they are the part of the structure closest to the player
 *       and they move fastest across the sky when the camera does;</li>
 *   <li>an arch of real voussoirs at each end, hung clear below the frame;</li>
 *   <li>and detail at three scales - pillars and rails, then drums, panels,
 *       corbels and knuckles, then studs and bosses - because a single scale of
 *       detail gives the eye one cue and then stops paying out.</li>
 * </ul>
 *
 * <p>Authored with <b>the plane of the doors at y = 0</b>, the frame occupying
 * y up to {@code thick} above that, and the whole colonnade hanging below in -Y.
 * So the actor's bind position (and its {@code dy} track) is the height of the
 * threshold - the height a thing would be at the instant it came through.
 *
 * <p>Tracks: {@code open} 0..1 (0 shut, 1 fully open), {@code glow} multiplier on
 * every molten surface, {@code shudder} 0..1 for the judder as the leaves grind,
 * {@code descend} 0..1 for the whole structure lowering onto its mark. Defaults
 * are a shut, cold, still gate already in place, so an undriven one still renders
 * as a building rather than as nothing.
 *
 * <p>Serves the wrath beat: the sky splits, the gates come down, and they open.
 */
public final class HellGateActor extends CinematicActor {

    private static final int PILLARS = 10;
    private static final int DRUMS = 3;
    private static final int COLLAR_STUDS = 6;
    private static final int BRACES = 3;
    private static final int VOUSSOIRS = 11;
    private static final int RAIL_CORBELS = 7;
    private static final int END_CORBELS = 4;
    private static final int CORBEL_COURSES = 3;
    private static final int PANEL_COLS = 3;
    private static final int PANEL_ROWS = 2;
    private static final int RING_SEGMENTS = 10;
    private static final int KNUCKLE_STAVES = 8;
    private static final int DOOR_KNUCKLES = 3;
    private static final int FRAME_KNUCKLES = 2;
    private static final int SEAM_LAYERS = 6;
    private static final int SPILL_LAYERS = 3;

    // Where each pillar stands and how far it hangs, as fractions of the span.
    // The lengths are deliberately all different: a colonnade whose feet all
    // finish on one plane reads as a comb, and a comb is flat again.
    private static final float[] PILLAR_X = {
            -0.440F, 0.440F, -0.440F, 0.440F,
            -0.150F, 0.150F, -0.150F, 0.150F,
            -0.400F, 0.400F };
    private static final float[] PILLAR_Z = {
            -0.255F, -0.255F, 0.255F, 0.255F,
            -0.255F, -0.255F, 0.255F, 0.255F,
            0.0F, 0.0F };
    private static final float[] PILLAR_LEN = {
            0.82F, 0.78F, 0.78F, 0.82F,
            0.44F, 0.51F, 0.51F, 0.44F,
            0.66F, 0.66F };
    private static final float[] PILLAR_R = {
            0.070F, 0.070F, 0.070F, 0.070F,
            0.046F, 0.046F, 0.046F, 0.046F,
            0.058F, 0.058F };

    // Basalt courses, cold iron and rusted fixings. Near-black on purpose: the
    // whole point of the beat is the light coming OUT, and stone with any value
    // left in it competes with the one thing that must not be competed with.
    private static final int STONE_DARK = 0x241E1B;
    private static final int STONE_MID = 0x342C27;
    private static final int STONE_LIGHT = 0x453A32;
    private static final int IRON = 0x181513;
    private static final int IRON_LIGHT = 0x2B2521;
    private static final int RUST = 0x532913;

    // Where the swing changes character. The windows never overlap, so chaining
    // the three curves end to end is exact - same construction as the throw in
    // ThrowerActor.
    private static final float SEAL_END = 0.20F;
    private static final float RUN_END = 0.84F;

    /** Degrees the leaves manage while they are still breaking their own seal. */
    private static final float SEAL_ANGLE = 3.0F;

    /** Degrees they sail past their stop before the mass drags them back. */
    private static final float OVER_ANGLE = 95.0F;

    /** Where they finish. Short of ninety, so the leaves hang slightly proud. */
    private static final float FULL_ANGLE = 88.0F;

    private final float span;
    private final float openX;
    private final float openZ;
    private final float frameX;
    private final float frameZ;
    private final float thick;
    private final float leafT;
    private final float leafZ;
    private final float hingeX;
    private final float hingeY;
    private final float leafLen;
    private final float dropHeight;

    // Everything pose() touches, cached at build time. pose() runs per frame, so
    // it may not build a single string or allocate a single object - which rules
    // out get("seam" + i) as surely as it rules out new Vec3().
    private Part descent;
    private Part rig;
    private Part doorR;
    private Part doorL;
    private Part maw;
    private final Part[] seam = new Part[SEAM_LAYERS];
    private final float[] seamSpread = new float[SEAM_LAYERS];
    private final Part[] spill = new Part[SPILL_LAYERS];
    private final Part[] rim = new Part[4];
    private final Part[] wash = new Part[PILLARS];
    private final Part[] edge = new Part[2];

    /**
     * @param span the structure's full width in blocks when bound at scale 1, so
     *             {@code new HellGateActor(60.0F)} is a sixty block gate. The
     *             actor's {@code scale} track multiplies on top of it, but bind
     *             the real size here - the proportions inside are tuned against
     *             the span, and blowing up a small gate loses the small detail
     *             that gives the eye its nearest parallax cue.
     */
    public HellGateActor(float span) {
        float s = Math.max(4.0F, span);
        this.span = s;
        this.openX = s * 0.300F;
        this.openZ = s * 0.190F;
        this.frameX = s * 0.500F;
        this.frameZ = s * 0.300F;
        this.thick = s * 0.130F;
        this.leafT = s * 0.030F;
        this.leafZ = this.openZ * 0.980F;
        // The hinge sits a little inside the jamb so a leaf hanging straight down
        // at full open passes the stone instead of through it. The gap that
        // leaves when the gate is shut is covered by a rebate lip on the leaf.
        this.hingeX = this.openX - this.leafT * 0.60F;
        this.hingeY = s * 0.055F;
        this.leafLen = this.hingeX;
        this.dropHeight = s * 1.60F;

        // Two nodes rather than one: the descent is a pure translation and the
        // judder is a rotation, and rotating a node that also carries a ninety
        // block drop offset would swing the whole structure sideways with it.
        this.descent = part("descent", this.root, 0.0F, 0.0F, 0.0F);
        this.rig = part("rig", this.descent, 0.0F, 0.0F, 0.0F);

        buildFrame();
        buildCorbels();
        buildArches();
        buildPillars();
        buildDoors();
        buildMolten();
    }

    // ---- the frame ---------------------------------------------------------

    /**
     * Four courses of stone plus a corbelled throat. Stacking courses of slightly
     * different footprints is what gives the frame a readable profile from
     * underneath: a single slab of the same thickness would present one unbroken
     * underside and be a plane again, just a thicker one.
     */
    private void buildFrame() {
        // The eave. It overhangs the frame all the way round, so from below the
        // structure's outline is wider than its opening - the first thing that
        // says "this is a solid object seen from underneath" rather than "hole".
        ring("cornice", this.rig, -this.span * 0.030F, this.span * 0.030F,
                this.openX * 1.02F, this.openZ * 1.02F,
                this.frameX * 1.08F, this.frameZ * 1.10F, STONE_LIGHT);
        // A thinner drip course below it, projecting further still.
        ring("drip", this.rig, -this.span * 0.046F, this.span * 0.016F,
                this.openX * 1.08F, this.openZ * 1.10F,
                this.frameX * 1.14F, this.frameZ * 1.18F, STONE_DARK);

        // The mass of the thing.
        ring("frame", this.rig, 0.0F, this.thick,
                this.openX, this.openZ, this.frameX, this.frameZ, STONE_MID);

        // The throat: the opening keeps going up through the frame and narrows as
        // it does. Looking up an angled shaft is one of the strongest depth cues
        // there is, because the near lip crosses the far wall the moment you move.
        float y = this.hingeY + this.leafT * 0.5F;
        float step = (this.thick - y) / 3.0F;
        for (int i = 0; i < 3; i++) {
            float k = (i + 1) / 3.0F;
            ring("throat" + i, this.rig, y + i * step, step,
                    this.openX * (1.0F - 0.12F * k), this.openZ * (1.0F - 0.16F * k),
                    this.openX * 1.06F, this.openZ * 1.08F,
                    i % 2 == 0 ? STONE_DARK : IRON_LIGHT);
        }

        // The top course, stepped in, so the far edge of the structure is a
        // stepped profile against the sky rather than a single hard line.
        ring("coping", this.rig, this.thick, this.span * 0.028F,
                this.openX * 0.86F, this.openZ * 0.82F,
                this.frameX * 0.96F, this.frameZ * 0.94F, STONE_DARK);

        // Buttress plinths at the corners, tying the frame down onto the pillars.
        for (int i = 0; i < 4; i++) {
            float px = (i % 2 == 0 ? -1.0F : 1.0F) * this.frameX * 0.90F;
            float pz = (i < 2 ? -1.0F : 1.0F) * this.frameZ * 0.86F;
            part("plinth" + i, this.rig, px, -this.span * 0.046F, pz)
                    .box(-this.span * 0.085F, -this.span * 0.034F, -this.span * 0.085F,
                            this.span * 0.170F, this.span * 0.034F, this.span * 0.170F)
                    .colour(STONE_LIGHT);
        }
    }

    /**
     * One rectangular course of stone, as four boxes round a hole. The ends only
     * span the inner depth so they butt against the sides instead of doubling up
     * in the corners.
     */
    private Part ring(String name, Part parent, float y0, float h,
                      float inX, float inZ, float outX, float outZ, int rgb) {
        Part g = part(name, parent, 0.0F, 0.0F, 0.0F);
        float sideD = Math.max(0.001F, outZ - inZ);
        float endW = Math.max(0.001F, outX - inX);
        part(name + "N", g, 0.0F, 0.0F, (inZ + outZ) * 0.5F)
                .box(-outX, y0, -sideD * 0.5F, outX * 2.0F, h, sideD).colour(rgb);
        part(name + "S", g, 0.0F, 0.0F, -(inZ + outZ) * 0.5F)
                .box(-outX, y0, -sideD * 0.5F, outX * 2.0F, h, sideD).colour(rgb);
        part(name + "E", g, (inX + outX) * 0.5F, 0.0F, 0.0F)
                .box(-endW * 0.5F, y0, -inZ, endW, h, inZ * 2.0F).colour(rgb);
        part(name + "W", g, -(inX + outX) * 0.5F, 0.0F, 0.0F)
                .box(-endW * 0.5F, y0, -inZ, endW, h, inZ * 2.0F).colour(rgb);
        return g;
    }

    /**
     * Brackets hanging below the frame's rim, three stepped courses each, with
     * their drop following an arc along the run so the row reads as the springing
     * of a vault rather than as a row of pegs.
     *
     * <p>All of them sit OUTSIDE the opening's footprint. Anything projecting
     * inward under the doors would be swept straight through by a leaf on its way
     * down, and a corbel that a door passes through undoes every bit of solidity
     * the rest of this file is buying.
     */
    private void buildCorbels() {
        float unit = this.span * 0.026F;
        for (int side = 0; side < 2; side++) {
            float sz = side == 0 ? -1.0F : 1.0F;
            for (int i = 0; i < RAIL_CORBELS; i++) {
                float u = i / (float) (RAIL_CORBELS - 1);
                float x = Easing.lerp(-this.openX * 0.90F, this.openX * 0.90F, u);
                // Deepest at the springing, shallowest at the crown.
                float arc = 0.30F + 0.70F * (2.0F * u - 1.0F) * (2.0F * u - 1.0F);
                corbel("railCorbel" + side + "_" + i, x, sz * (this.openZ + unit * 1.3F),
                        0.0F, sz, unit, arc);
            }
        }
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1.0F : 1.0F;
            for (int i = 0; i < END_CORBELS; i++) {
                float u = i / (float) (END_CORBELS - 1);
                float z = Easing.lerp(-this.openZ * 0.80F, this.openZ * 0.80F, u);
                float arc = 0.35F + 0.65F * (2.0F * u - 1.0F) * (2.0F * u - 1.0F);
                corbel("endCorbel" + side + "_" + i, sx * (this.openX + unit * 1.3F), z,
                        sx, 0.0F, unit, arc);
            }
        }
    }

    /** One bracket: three courses, each stepping further out and further down. */
    private void corbel(String name, float x, float z, float outX, float outZ,
                        float unit, float arc) {
        Part g = part(name, this.rig, x, -this.span * 0.046F, z);
        for (int c = 0; c < CORBEL_COURSES; c++) {
            float k = (c + 1) / (float) CORBEL_COURSES;
            float h = unit * (0.55F + 1.20F * arc) / CORBEL_COURSES;
            part(name + "_" + c, g, outX * unit * 0.42F * k, -h * c, outZ * unit * 0.42F * k)
                    .box(-unit * (0.90F - 0.16F * c), -h, -unit * (0.90F - 0.16F * c),
                            unit * (1.80F - 0.32F * c), h, unit * (1.80F - 0.32F * c))
                    .colour(c == CORBEL_COURSES - 1 ? IRON_LIGHT : STONE_MID);
        }
    }

    /**
     * A real semicircular arch of wedge blocks under each end of the frame.
     *
     * <p>Each voussoir is pivoted on the circle and turned so its own -Y points
     * radially outward, which is what makes the ring an arch instead of a fan of
     * boxes all facing the same way. The ends are the only place an arch can hang
     * without fouling the doors, and one arch either side is enough to say
     * "gateway" in a single glance from a mile underneath.
     */
    private void buildArches() {
        float r = this.frameZ * 0.92F;
        float ringDepth = this.span * 0.048F;
        float segHalf = r * (float) (Math.PI / (VOUSSOIRS - 1)) * 0.56F;
        float halfThick = this.span * 0.055F;

        for (int side = 0; side < 2; side++) {
            float ax = (side == 0 ? -1.0F : 1.0F) * (this.openX + this.frameX) * 0.5F;
            Part arch = part("arch" + side, this.rig, ax, -this.span * 0.046F, 0.0F);
            for (int j = 0; j < VOUSSOIRS; j++) {
                double phi = Math.PI * j / (VOUSSOIRS - 1);
                boolean key = j == VOUSSOIRS / 2;
                float depth = key ? ringDepth * 1.45F : ringDepth;
                Part v = part("vous" + side + "_" + j, arch,
                        0.0F, -(float) Math.sin(phi) * r, (float) Math.cos(phi) * r);
                // -90 puts the block's own -Y onto the outward radius; see the
                // class note - this is the whole trick that makes it an arch.
                v.rotX = (float) Math.toDegrees(phi) - 90.0F;
                v.box(-halfThick, -depth, -segHalf, halfThick * 2.0F, depth, segHalf * 2.0F)
                        .colour(key ? STONE_LIGHT : (j % 2 == 0 ? STONE_MID : STONE_DARK));
                // A boss on the outer face of every stone: the smallest scale of
                // detail on the arch, and the one that survives longest as the
                // camera pulls away.
                part("vousStud" + side + "_" + j, v, 0.0F, -depth, 0.0F)
                        .box(-halfThick * 0.30F, -this.span * 0.010F, -segHalf * 0.34F,
                                halfThick * 0.60F, this.span * 0.010F, segHalf * 0.68F)
                        .colour(RUST);
            }
        }
    }

    // ---- the colonnade -----------------------------------------------------

    /**
     * Ten hanging pillars. These are the reason the gate stops moving with the
     * camera: they are the nearest geometry to a player standing underneath, they
     * are spread across the whole footprint, and they end at four different
     * depths - so any camera movement at all slides them across each other and
     * across the frame, which is the signal the brain uses to build depth.
     */
    private void buildPillars() {
        for (int i = 0; i < PILLARS; i++) {
            float px = PILLAR_X[i] * this.span;
            float pz = PILLAR_Z[i] * this.span;
            float len = PILLAR_LEN[i] * this.span;
            float r0 = PILLAR_R[i] * this.span;

            Part col = part("pillar" + i, this.rig, px, -this.span * 0.046F, pz);

            part("cap" + i, col, 0.0F, 0.0F, 0.0F)
                    .box(-r0 * 1.34F, -r0 * 0.55F, -r0 * 1.34F,
                            r0 * 2.68F, r0 * 0.55F, r0 * 2.68F)
                    .colour(STONE_LIGHT);

            float y = -r0 * 0.55F;
            float r = r0;
            float band = r0 * 0.14F;
            float drum = (len - r0 * 0.55F - DRUMS * band) / DRUMS;
            for (int d = 0; d < DRUMS; d++) {
                part("drum" + i + "_" + d, col, 0.0F, y, 0.0F)
                        .box(-r, -drum, -r, r * 2.0F, drum, r * 2.0F)
                        .colour(d % 2 == 0 ? STONE_MID : STONE_DARK);
                // The collar is proud of the drum, so it catches the shading and
                // reads as a joint between two stones rather than a painted line.
                part("collar" + i + "_" + d, col, 0.0F, y - drum, 0.0F)
                        .box(-r * 1.16F, -band, -r * 1.16F, r * 2.32F, band, r * 2.32F)
                        .colour(IRON);
                for (int s = 0; s < COLLAR_STUDS; s++) {
                    double a = s * Math.PI * 2.0D / COLLAR_STUDS;
                    Part stud = part("pStud" + i + "_" + d + "_" + s, col,
                            (float) Math.cos(a) * r * 1.16F, y - drum - band * 0.5F,
                            (float) Math.sin(a) * r * 1.16F);
                    stud.rotY = -(float) Math.toDegrees(a);
                    stud.box(0.0F, -r0 * 0.055F, -r0 * 0.055F,
                            r0 * 0.11F, r0 * 0.11F, r0 * 0.11F).colour(RUST);
                }
                y -= drum + band;
                r *= 0.86F;
            }

            // A tapered iron terminal, because a pillar that just stops is a stick.
            part("spike" + i, col, 0.0F, y, 0.0F)
                    .box(-r * 0.62F, -r0 * 1.5F, -r * 0.62F, r * 1.24F, r0 * 1.5F, r * 1.24F)
                    .colour(IRON_LIGHT);
            part("tip" + i, col, 0.0F, y - r0 * 1.5F, 0.0F)
                    .box(-r * 0.26F, -r0 * 0.95F, -r * 0.26F, r * 0.52F, r0 * 0.95F, r * 0.52F)
                    .colour(IRON);

            buttresses(i, col, px, pz, len, r0);

            // Molten wash on the capital. The pillars are lit from above by the
            // gap, so the light has to land on something below the frame or the
            // opening reads as a lamp hanging in front of an unlit backdrop.
            Part w = part("wash" + i, col, 0.0F, 0.0F, 0.0F);
            w.box(-r0 * 1.7F, -r0 * 2.4F, -r0 * 1.7F, r0 * 3.4F, r0 * 2.4F, r0 * 3.4F)
                    .colour(1.0F, 0.34F, 0.07F).glow().alpha(0.0F);
            this.wash[i] = w;
        }
    }

    /**
     * Braces from each pillar back up to the frame: two along the ring and one
     * flying outward. Never inward - a brace reaching in toward the opening would
     * end up inside the arc a leaf swings through.
     */
    private void buttresses(int i, Part col, float px, float pz, float len, float r0) {
        float radial = -(float) Math.toDegrees(Math.atan2(pz, px));
        for (int b = 0; b < BRACES; b++) {
            boolean flying = b == 2;
            float rise = len * (flying ? 0.22F : 0.26F);
            float tilt = flying ? -30.0F : 30.0F;
            Part spoke = part("brace" + i + "_" + b, col, 0.0F, -rise, 0.0F);
            spoke.rotY = radial + (b == 0 ? 90.0F : (b == 1 ? -90.0F : 0.0F));
            Part arm = part("braceArm" + i + "_" + b, spoke, 0.0F, 0.0F, 0.0F);
            arm.rotZ = tilt;
            float armLen = rise / Mth.cos(tilt * Mth.DEG_TO_RAD);
            arm.box(-r0 * 0.20F, 0.0F, -r0 * 0.20F, r0 * 0.40F, armLen, r0 * 0.40F)
                    .colour(flying ? STONE_LIGHT : STONE_MID);
        }
    }

    // ---- the doors ---------------------------------------------------------

    private void buildDoors() {
        this.doorR = leaf("doorR", false);
        this.doorL = leaf("doorL", true);
        // Frame knuckles interleaved with the door's own, so the hinge line is
        // visibly a hinge line. It is also the thing that explains the motion:
        // once the eye has found the barrel it knows exactly what is about to
        // happen and reads the swing as mechanical rather than as a wipe.
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? -1.0F : 1.0F;
            for (int k = 0; k < FRAME_KNUCKLES; k++) {
                float z = (k == 0 ? -1.0F : 1.0F) * this.leafZ * 0.31F;
                knuckle("frameKnuckle" + side + "_" + k, this.rig,
                        sx * this.hingeX, this.hingeY, z,
                        this.leafT * 1.10F, this.leafZ * 0.115F, IRON_LIGHT);
            }
        }
    }

    /**
     * One leaf. It is a slab with a rebate, a raised rim, coffered panels, corner
     * bosses and a ring boss - a door, in other words, and specifically not a
     * card. Every one of those layers projects a different distance below the
     * slab, which is what stops the underside reading as painted-on.
     *
     * @param mirror true for the leaf hinged on the -X jamb
     */
    private Part leaf(String tag, boolean mirror) {
        float sx = mirror ? -1.0F : 1.0F;
        Part door = part(tag, this.rig, sx * this.hingeX, this.hingeY, 0.0F);
        // Authored once as the right-hand leaf running back in -X from its hinge,
        // then YAWED for the other side. Deliberately not a negative scaleX:
        // renderPart clamps every scale with Math.max(0.001F, ...), so a -1 there
        // would collapse the whole leaf to a sliver instead of flipping it.
        door.rotY = mirror ? 180.0F : 0.0F;

        float half = this.leafT * 0.5F;
        part(tag + "Slab", door, 0.0F, 0.0F, 0.0F)
                .box(-this.leafLen, -half, -this.leafZ, this.leafLen, this.leafT,
                        this.leafZ * 2.0F).colour(STONE_MID);
        // The rebate lip: covers the pocket the hinge offset leaves when the gate
        // is shut, and swings clear of the jamb as soon as the leaf moves.
        part(tag + "Lip", door, 0.0F, 0.0F, 0.0F)
                .box(0.0F, -half, -this.leafZ, this.leafT * 0.66F, this.leafT * 0.40F,
                        this.leafZ * 2.0F).colour(IRON);

        // A raised rim round the underside.
        float rimT = this.leafT * 0.34F;
        float rimW = this.leafLen * 0.055F;
        part(tag + "RimN", door, 0.0F, 0.0F, 0.0F)
                .box(-this.leafLen, -half - rimT, this.leafZ - rimW, this.leafLen, rimT, rimW)
                .colour(IRON_LIGHT);
        part(tag + "RimS", door, 0.0F, 0.0F, 0.0F)
                .box(-this.leafLen, -half - rimT, -this.leafZ, this.leafLen, rimT, rimW)
                .colour(IRON_LIGHT);
        part(tag + "RimE", door, 0.0F, 0.0F, 0.0F)
                .box(-this.leafLen, -half - rimT, -this.leafZ, rimW, rimT, this.leafZ * 2.0F)
                .colour(IRON_LIGHT);
        part(tag + "RimW", door, 0.0F, 0.0F, 0.0F)
                .box(-rimW, -half - rimT, -this.leafZ, rimW, rimT, this.leafZ * 2.0F)
                .colour(IRON_LIGHT);

        // The meeting edge, chamfered and heavier than the rest of the rim - it is
        // the edge the whole shot is about, so it gets the mass.
        part(tag + "Meet", door, 0.0F, 0.0F, 0.0F)
                .box(-this.leafLen - this.leafT * 0.18F, -half - this.leafT * 0.20F,
                        -this.leafZ, this.leafT * 0.30F, this.leafT * 1.40F,
                        this.leafZ * 2.0F).colour(IRON);

        panels(tag, door, half);
        ringBoss(tag, door, half);

        // Three on the leaf, two on the frame, sized to sit in each other's gaps
        // the way a real barrel hinge interleaves - which is what makes the hinge
        // line read as a mechanism instead of as a decorated crease.
        for (int k = 0; k < DOOR_KNUCKLES; k++) {
            float z = (k - 1) * this.leafZ * 0.62F;
            knuckle(tag + "Knuckle" + k, door, 0.0F, 0.0F, z,
                    this.leafT * 0.95F, this.leafZ * 0.17F, IRON);
        }

        // The molten strip on the meeting edge. It belongs to the leaf, so it
        // swings with it and turns toward the camera as the door drops - the light
        // on a door edge is how you know the fire is BEHIND the door.
        Part lit = part(tag + "Edge", door, 0.0F, 0.0F, 0.0F);
        lit.box(-this.leafLen - this.leafT * 0.34F, -half - this.leafT * 0.30F,
                        -this.leafZ * 1.02F, this.leafT * 0.44F, this.leafT * 1.60F,
                        this.leafZ * 2.04F)
                .colour(1.0F, 0.42F, 0.10F).glow().alpha(0.0F);
        this.edge[mirror ? 1 : 0] = lit;
        return door;
    }

    /** The coffering, plus a boss at each panel corner. */
    private void panels(String tag, Part door, float half) {
        float m = this.leafLen * 0.085F;
        float pw = (this.leafLen - m * (PANEL_COLS + 1)) / PANEL_COLS;
        float pd = (this.leafZ * 2.0F - m * (PANEL_ROWS + 1)) / PANEL_ROWS;
        float panelT = this.leafT * 0.42F;
        float boss = this.leafT * 0.26F;
        for (int c = 0; c < PANEL_COLS; c++) {
            for (int rw = 0; rw < PANEL_ROWS; rw++) {
                float x0 = -this.leafLen + m + c * (pw + m);
                float z0 = -this.leafZ + m + rw * (pd + m);
                part(tag + "Panel" + c + rw, door, 0.0F, 0.0F, 0.0F)
                        .box(x0, -half - panelT, z0, pw, panelT, pd)
                        .colour(c % 2 == rw % 2 ? STONE_DARK : IRON_LIGHT);
                for (int b = 0; b < 4; b++) {
                    float bx = x0 + pw * (b % 2 == 0 ? 0.15F : 0.85F);
                    float bz = z0 + pd * (b < 2 ? 0.18F : 0.82F);
                    part(tag + "Boss" + c + rw + b, door, 0.0F, 0.0F, 0.0F)
                            .box(bx - boss, -half - panelT - boss * 1.3F, bz - boss,
                                    boss * 2.0F, boss * 1.3F, boss * 2.0F)
                            .colour(RUST);
                }
            }
        }
    }

    /** The great ring on each leaf - the one detail that reads first and farthest. */
    private void ringBoss(String tag, Part door, float half) {
        float rr = this.leafLen * 0.20F;
        Part hub = part(tag + "Ring", door, -this.leafLen * 0.50F, -half, 0.0F);
        part(tag + "Hub", hub, 0.0F, 0.0F, 0.0F)
                .box(-rr * 0.34F, -this.leafT * 0.95F, -rr * 0.34F,
                        rr * 0.68F, this.leafT * 0.95F, rr * 0.68F).colour(IRON);
        float tanHalf = rr * (float) (Math.PI / RING_SEGMENTS) * 1.10F;
        for (int j = 0; j < RING_SEGMENTS; j++) {
            double a = j * Math.PI * 2.0D / RING_SEGMENTS;
            Part seg = part(tag + "RingSeg" + j, hub,
                    (float) Math.cos(a) * rr, 0.0F, (float) Math.sin(a) * rr);
            seg.rotY = -(float) Math.toDegrees(a);
            seg.box(-rr * 0.10F, -this.leafT * 0.62F, -tanHalf,
                    rr * 0.20F, this.leafT * 0.62F, tanHalf * 2.0F).colour(IRON_LIGHT);
        }
    }

    /**
     * A hinge barrel about the Z axis: staves stood round a circle in the XY plane
     * and run along Z, the same tangent-polygon construction BluntActor uses for
     * its shaft, turned on its side.
     */
    private void knuckle(String name, Part parent, float x, float y, float z,
                         float r, float halfLen, int rgb) {
        Part g = part(name, parent, x, y, z);
        float tanHalf = r * (float) Math.tan(Math.PI / KNUCKLE_STAVES) * 1.08F;
        for (int s = 0; s < KNUCKLE_STAVES; s++) {
            double a = s * Math.PI * 2.0D / KNUCKLE_STAVES;
            Part st = part(name + "_" + s, g,
                    (float) Math.cos(a) * r * 0.55F, (float) Math.sin(a) * r * 0.55F, 0.0F);
            st.rotZ = (float) Math.toDegrees(a);
            st.box(0.0F, -tanHalf, -halfLen, r * 0.45F, tanHalf * 2.0F, halfLen * 2.0F)
                    .colour(rgb);
        }
    }

    // ---- the fire behind it ------------------------------------------------

    /**
     * Everything that glows. All of it is additive and none of it writes depth, so
     * it lays over the finished stone - which is what makes light spilling round a
     * door edge look like light rather than like a decal on the door.
     */
    private void buildMolten() {
        // What is on the other side, filling the top of the throat.
        this.maw = part("maw", this.rig, 0.0F, this.thick * 0.90F, 0.0F);
        this.maw.box(-this.openX * 0.88F, 0.0F, -this.openZ * 0.84F,
                        this.openX * 1.76F, this.thick * 0.55F, this.openZ * 1.68F)
                .colour(1.0F, 0.30F, 0.05F).glow().alpha(0.0F);

        // The seam. A stack of slabs up the throat, each scaled in X to exactly
        // the gap the leaves have opened, so the light is never wider than the
        // hole it is coming through. The lowest one hangs below the threshold, and
        // that is the one that reads as light spilling OUT.
        float y0 = -this.span * 0.030F;
        float y1 = this.thick * 0.95F;
        for (int i = 0; i < SEAM_LAYERS; i++) {
            float u = i / (float) (SEAM_LAYERS - 1);
            this.seamSpread[i] = 1.0F + 0.42F * u;
            Part slab = part("seam" + i, this.rig, 0.0F, Easing.lerp(y0, y1, u), 0.0F);
            slab.box(-this.openX, 0.0F, -this.openZ * this.seamSpread[i],
                            this.openX * 2.0F, this.span * 0.026F,
                            this.openZ * 2.0F * this.seamSpread[i])
                    .colour(1.0F, 0.55F, 0.16F).glow().alpha(0.0F);
            this.seam[i] = slab;
        }

        // The rim of the hole, lit from inside.
        float lipT = this.span * 0.012F;
        this.rim[0] = glowBar("rimN", 0.0F, this.hingeY, this.openZ,
                this.openX, lipT, lipT);
        this.rim[1] = glowBar("rimS", 0.0F, this.hingeY, -this.openZ,
                this.openX, lipT, lipT);
        this.rim[2] = glowBar("rimE", this.openX, this.hingeY, 0.0F,
                lipT, lipT, this.openZ);
        this.rim[3] = glowBar("rimW", -this.openX, this.hingeY, 0.0F,
                lipT, lipT, this.openZ);

        // The wash thrown down through the colonnade. Broad, faint and stacked, so
        // the air below the gate is lit rather than the gate floating over black.
        for (int i = 0; i < SPILL_LAYERS; i++) {
            float k = (i + 1) / (float) SPILL_LAYERS;
            Part s = part("spill" + i, this.rig, 0.0F, -this.span * (0.06F + 0.22F * k), 0.0F);
            s.box(-this.openX * (1.0F + 0.9F * k), 0.0F, -this.openZ * (1.0F + 1.1F * k),
                            this.openX * 2.0F * (1.0F + 0.9F * k), this.span * 0.05F,
                            this.openZ * 2.0F * (1.0F + 1.1F * k))
                    .colour(1.0F, 0.26F, 0.05F).glow().alpha(0.0F);
            this.spill[i] = s;
        }
    }

    private Part glowBar(String name, float x, float y, float z,
                         float hx, float hy, float hz) {
        Part p = part(name, this.rig, x, y, z);
        p.box(-hx, -hy, -hz, hx * 2.0F, hy * 2.0F, hz * 2.0F)
                .colour(1.0F, 0.46F, 0.12F).glow().alpha(0.0F);
        return p;
    }

    // ---- animation ---------------------------------------------------------

    @Override
    protected void pose(float time) {
        float open = Mth.clamp(track("open", time, 0.0F), 0.0F, 1.0F);
        float lamp = Math.max(0.0F, track("glow", time, 1.0F));
        float shudder = Mth.clamp(track("shudder", time, 0.0F), 0.0F, 2.0F);
        float descend = Mth.clamp(track("descend", time, 1.0F), 0.0F, 1.0F);

        float angle = swing(open);
        // The leaves hinge on their OUTER edges, so the gap between their tips is
        // the versine of the swing, not the sine. That is why the first twenty
        // degrees barely crack it and the last twenty throw it wide - the doors
        // supply their own "slow to open, then all at once" for free.
        float gap = 1.0F - Mth.cos(angle * Mth.DEG_TO_RAD);

        // Grinding is worst while the leaves are actually travelling. The rate
        // comes straight off the track a couple of ticks back, the same cheap
        // honest derivative GrassBladeActor uses for its whip.
        float before = swing(Mth.clamp(track("open", time - 2.0F, open), 0.0F, 1.0F));
        float rate = Mth.clamp(Math.abs(angle - before) * 0.16F, 0.0F, 1.0F);
        float grind = shudder * (0.28F + 0.72F * rate);

        // The descent node is a PURE translation. renderPart composes the offset
        // inside the rotations, so a list on this node would swing a ninety block
        // drop sideways with it; the list belongs on the rig below, whose own
        // offsets are small enough not to care.
        this.descent.offY = (1.0F - descend) * this.dropHeight;

        judder(time, grind, 1.0F - descend);
        leaves(time, angle, grind);
        fire(time, open, gap, lamp, grind);
    }

    /**
     * The judder, plus the list it comes down with. Frequencies that do not divide
     * into each other so it never settles into a loop the eye can predict, and a
     * rotation as well as a slide so the whole mass tips rather than skating.
     *
     * <p>The list levels off as the gate arrives: something this size settling
     * perfectly square the whole way down reads as an elevator.
     */
    private void judder(float time, float grind, float left) {
        float a = this.span * 0.0055F * grind;
        this.rig.offX = (Mth.sin(time * 2.7F) + 0.45F * Mth.sin(time * 6.13F)) * a;
        this.rig.offY = Mth.sin(time * 3.91F + 1.3F) * a * 0.70F;
        this.rig.offZ = Mth.cos(time * 2.29F) * a * 0.55F;
        this.rig.rotZ = left * 3.6F + Mth.sin(time * 5.3F) * grind * 0.30F;
        this.rig.rotX = left * -2.4F + Mth.cos(time * 4.1F) * grind * 0.22F;
    }

    /**
     * Swing the leaves. Each gets its own judder phase, so they grind against one
     * another instead of shaking as a single piece - two heavy things that shake
     * in step are one heavy thing.
     */
    private void leaves(float time, float angle, float grind) {
        float chatterR = Mth.sin(time * 6.7F) * grind * 0.55F;
        float chatterL = Mth.sin(time * 6.7F + 2.1F) * grind * 0.55F;
        // renderPart composes T(pivot)*Rz*Ry*Rx, so the mirror's 180 yaw is applied
        // to the geometry BEFORE rotZ swings it about the parent's Z axis - which
        // means the mirrored leaf needs the opposite sign or it would shut as its
        // partner opened.
        this.doorR.rotZ = angle + chatterR;
        this.doorL.rotZ = -(angle + chatterL);
    }

    /**
     * Every molten surface, all of it keyed off how far the leaves actually are
     * apart rather than off the raw track. The light IS the gap: if it did not
     * grow with the geometry the eye would immediately read the two as unrelated,
     * and the glow would go back to looking like something painted on the sky.
     */
    private void fire(float time, float open, float gap, float lamp, float grind) {
        // A thread of light shows the moment the sequence starts, before the
        // leaves have measurably parted. It is the tell that says the gate is
        // about to do something, and it costs one curve.
        float bleed = Easing.CUBIC_OUT.apply(Mth.clamp(open / 0.10F, 0.0F, 1.0F)) * 0.09F;
        float lit = Math.max(bleed, gap);
        // Furnaces boil, they do not pulse. Two phases plus the judder, so the
        // light shakes when the stone does.
        float boil = 0.80F + 0.14F * Mth.sin(time * 0.71F)
                + 0.06F * Mth.sin(time * 2.37F) + 0.10F * grind * Mth.sin(time * 8.9F);
        float heat = Mth.clamp(lit * 1.15F, 0.0F, 1.0F);

        // Strictly proportional to the gap, with no floor under it. Glow parts are
        // additive and never write depth, so a maw with any standing brightness
        // would shine straight THROUGH a fully shut pair of doors - which is the
        // one thing that would give the whole structure away as a painted sky.
        molten(this.maw, heat, lamp * lit * 0.95F * boil);

        for (int i = 0; i < SEAM_LAYERS; i++) {
            Part slab = this.seam[i];
            float u = i / (float) (SEAM_LAYERS - 1);
            // Never zero: at a hairline the seam is a thread of light, and a
            // scale that collapses to nothing would blink the thread out a frame
            // before the doors were visibly shut.
            slab.scaleX = Math.max(0.010F, lit * this.seamSpread[i]);
            // Brightest deep in the throat and softer as it spills out below, so
            // the shaft has a direction to it.
            molten(slab, Mth.clamp(heat * (0.55F + 0.65F * u), 0.0F, 1.0F),
                    Math.min(0.95F, lamp * lit * (0.30F + 0.55F * u) * boil));
        }

        float rimAlpha = lamp * lit * 0.62F * boil;
        for (int i = 0; i < 4; i++) {
            molten(this.rim[i], heat, rimAlpha);
        }

        // The leaf edges are lit by what is behind them, so they brighten with the
        // gap and not with the swing.
        molten(this.edge[0], heat, lamp * lit * 0.70F * boil);
        molten(this.edge[1], heat, lamp * lit * 0.70F * boil);

        for (int i = 0; i < SPILL_LAYERS; i++) {
            Part s = this.spill[i];
            float k = (i + 1) / (float) SPILL_LAYERS;
            float grow = 0.35F + 0.65F * lit;
            s.scaleX = grow;
            s.scaleZ = grow;
            s.alpha = Mth.clamp(lamp * lit * (0.16F - 0.09F * k) * boil, 0.0F, 0.30F);
        }

        float washAlpha = Mth.clamp(lamp * lit * 0.13F * boil, 0.0F, 0.22F);
        for (int i = 0; i < PILLARS; i++) {
            this.wash[i].alpha = washAlpha;
        }
    }

    /**
     * The swing itself: three stages that never overlap, so chaining them is exact.
     *
     * <p>QUINT_IN across the first fifth buys three degrees - the leaves strain
     * against their own seal and effectively do not move. CUBIC_IN_OUT then runs
     * them the whole way with the speed in the middle. SPRING_OUT carries them
     * past their stop and rings back onto it, which is the only part of this that
     * makes them feel like they weigh anything.
     */
    private static float swing(float p) {
        float q = Easing.clamp01(p);
        float strain = Easing.clamp01(q / SEAL_END);
        float run = Easing.clamp01((q - SEAL_END) / (RUN_END - SEAL_END));
        float settle = Easing.clamp01((q - RUN_END) / (1.0F - RUN_END));
        float v = Easing.ease(Easing.QUINT_IN, 0.0F, SEAL_ANGLE, strain);
        v = Easing.ease(Easing.CUBIC_IN_OUT, v, OVER_ANGLE, run);
        return Easing.ease(Easing.SPRING_OUT, v, FULL_ANGLE, settle);
    }

    /** Deep red at the edge of the light, white-hot at the heart of it. */
    private static void molten(Part p, float heat, float alpha) {
        p.colour(1.0F, 0.13F + 0.66F * heat, 0.02F + 0.50F * heat * heat);
        p.alpha = Mth.clamp(alpha, 0.0F, 1.0F);
    }

    // ---- what a scene needs to know ----------------------------------------

    /** The structure's full width in blocks at bind scale 1. */
    public float span() {
        return this.span;
    }

    /**
     * World width of the gap between the leaves at this scene time, in blocks -
     * what a cue should measure to fly the camera or a meteor through the hole
     * rather than into a door.
     *
     * <p>Rebuilt from the tracks the way {@link CinematicActor#render} rebuilds
     * the transform, not read off whatever the last frame left behind, so a cue
     * firing on a whole tick gets the answer for that tick.
     */
    public float gap(float time) {
        float angle = swing(Mth.clamp(track("open", time, 0.0F), 0.0F, 1.0F));
        float sc = Math.max(0.001F, track("scale", time, this.baseScale));
        return 2.0F * this.openX * (1.0F - Mth.cos(angle * Mth.DEG_TO_RAD)) * sc;
    }
}
