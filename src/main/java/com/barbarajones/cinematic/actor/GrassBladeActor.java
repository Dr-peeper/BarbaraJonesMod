package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * One blade of grass the size of a tree, thrown through the sky like a spear.
 *
 * <p>Barbara pulls her grass straight out of the ground, so this is what she
 * throws: a torn pale collar at the bottom, roots still on it with soil hanging
 * off them, and a couple of shoots that came up with it. The read has to be
 * instant and unmistakable - "that is a literal piece of grass" - because the
 * horror of the beat lives entirely in the mismatch between what the thing is
 * and how big it is. Every detail here exists to close that read faster.
 *
 * <p>It is folded geometry, never a card. Each segment is a raised central rib
 * with two vanes rolled down off it, so the cross-section is a shallow V. That
 * matters more here than on any other prop in the show: the blade tumbles end
 * over end on the way down, and a flat plane vanishes completely twice per
 * rotation.
 *
 * <p>Authored around its <b>balance point</b> - the torn end hangs 0.38 of the
 * length below the origin and the tip rises above it - so {@code spin} tumbles
 * it about its centre of mass the way a thrown spear tumbles, instead of
 * swinging around its own butt like a gate.
 *
 * <p>The constructor's {@code length} is the blade's length in blocks when bound
 * at scale 1, so {@code new GrassBladeActor(12.0F)} is the javelin and
 * {@code new GrassBladeActor(40.0F)} is the monster.
 *
 * <p>Tracks: {@code spin} (degrees of tumble), {@code pitch} (degrees, tips the
 * plane the tumble happens in), {@code grow} 0..1 (unfurls base to tip as it
 * materialises), {@code glow} 0..1 (superheat on the way down). All default to
 * a blade standing still, whole and cold, so an undriven one still renders.
 */
public final class GrassBladeActor extends CinematicActor {

    private static final int SEGMENTS = 14;
    private static final int SHOOTS = 4;
    private static final int SHOOT_SEGMENTS = 3;

    /** Exactly the number of solid boxes the constructor builds, plus slack. */
    private static final int SOLIDS = SEGMENTS * 3 + SHOOTS * SHOOT_SEGMENTS + 6;

    /** Where the balance point sits, as a fraction of the length above the base. */
    private static final float BALANCE = 0.38F;

    private final float[] curve = new float[SEGMENTS];
    private final Part[] segment = new Part[SEGMENTS];
    private final Part[] ember = new Part[SEGMENTS];

    // Every solid box and the colour it is when cold. Flat arrays rather than
    // name lookups because the superheat pass touches all sixty of them every
    // frame, and get("rib" + i) would allocate a string per box per frame.
    private final Part[] solid = new Part[SOLIDS];
    private final float[] coldR = new float[SOLIDS];
    private final float[] coldG = new float[SOLIDS];
    private final float[] coldB = new float[SOLIDS];
    private final float[] along = new float[SOLIDS];
    private int solids;

    /** True while the solids carry a heat tint that still has to be undone. */
    private boolean tinted;

    public GrassBladeActor(float length) {
        float len = Math.max(0.5F, length);
        // Width does not scale quite as fast as length. Real grass gets finer as
        // it gets longer, and a forty block blade that is a straight blow-up of
        // a twelve block one reads as a paddle. Anchored on the javelin.
        float wide = len * (float) Math.pow(12.0D / len, 0.12D);
        float segLen = len / SEGMENTS;

        Part body = part("body", this.root, 0.0F, 0.0F, 0.0F);
        Part shaft = part("shaft", body, 0.0F, -BALANCE * len, 0.0F);

        Part parent = shaft;
        for (int i = 0; i < SEGMENTS; i++) {
            float k = (i + 0.5F) / SEGMENTS;

            Part seg = part("seg" + i, parent, 0.0F, i == 0 ? 0.0F : segLen, 0.0F);
            // The curve lives in the joints, so the blade arcs the way a leaf
            // arcs rather than being a straight stick with a crease in it. Most
            // of the bend is in the top half: grass is stiff at the sheath and
            // floppy where it has nothing left to hold up.
            this.curve[i] = 0.5F + 5.0F * k * k;
            seg.rotX = this.curve[i];
            // A little twist per joint rolls the arc out of a single plane. A
            // perfectly planar blade is the one shape a card could have faked.
            seg.rotY = 1.3F;
            this.segment[i] = seg;

            float halfW = wide * unitHalfWidth(k);
            float ribW = Math.max(halfW * 0.30F, wide * 0.005F);
            float ribD = wide * 0.026F * (1.0F - 0.55F * k);
            float vaneT = wide * 0.0075F * (1.0F - 0.50F * k);
            // The keel opens out toward the tip - the fold is what stiffens the
            // base, and the tip is the part that has given up.
            float fold = 26.0F - 14.0F * k;

            // Base to tip: a dark, almost blue green at the torn end running up
            // to a bright new green at the tip, because the tip is the youngest
            // part of the blade and the base has been in shade all summer.
            float r;
            float g;
            float b;
            if (k < 0.55F) {
                float u = k / 0.55F;
                r = mix(0.145F, 0.298F, u);
                g = mix(0.318F, 0.549F, u);
                b = mix(0.086F, 0.157F, u);
            } else {
                float u = (k - 0.55F) / 0.45F;
                r = mix(0.298F, 0.541F, u);
                g = mix(0.549F, 0.792F, u);
                b = mix(0.157F, 0.267F, u);
            }

            Part rib = part("rib" + i, seg, 0.0F, 0.0F, 0.0F);
            rib.box(-ribW, 0.0F, -vaneT, ribW * 2.0F, segLen, ribD + vaneT);
            // The midrib catches the light on any real blade, so it is a shade
            // lighter than the vanes either side of it. It is also the deepest
            // box in the cross-section, which is what keeps a silhouette when
            // the tumble brings the blade edge-on.
            rib.colour(mix(r, 1.0F, 0.22F), mix(g, 1.0F, 0.26F), mix(b, 1.0F, 0.16F));
            remember(rib, k);

            // Two dry patches on opposite sides, plus a browned-off tip. One
            // uniform green is the tell that says "box"; grass is never one
            // colour twice along its own length.
            float dryR = i == 4 ? 0.80F : 0.0F;
            float dryL = i == 9 ? 0.72F : 0.0F;
            float withered = i >= SEGMENTS - 2 ? 0.42F : 0.0F;

            Part vaneR = part("vaneR" + i, seg, 0.0F, 0.0F, 0.0F);
            vaneR.rotY = fold;
            vaneR.box(ribW * 0.72F, 0.0F, -vaneT, halfW - ribW * 0.72F, segLen, vaneT * 2.0F);
            straw(vaneR, r, g, b, Math.max(dryR, withered), k);

            Part vaneL = part("vaneL" + i, seg, 0.0F, 0.0F, 0.0F);
            vaneL.rotY = -fold;
            vaneL.box(-halfW, 0.0F, -vaneT, halfW - ribW * 0.72F, segLen, vaneT * 2.0F);
            straw(vaneL, r, g, b, Math.max(dryL, withered), k);

            // The heat shell: an additive box wrapping the segment, dark until
            // the blade starts to cook. Superheating the solid colour alone
            // never looks hot, it just looks repainted; the bloom around the
            // edge is the part the eye reads as temperature.
            Part shell = part("ember" + i, seg, 0.0F, 0.0F, 0.0F);
            float shellW = halfW * 1.15F + wide * 0.004F;
            float shellD = halfW * 0.55F + ribD;
            shell.box(-shellW, -segLen * 0.05F, -shellD,
                    shellW * 2.0F, segLen * 1.10F, shellD * 2.0F);
            shell.colour(1.0F, 0.35F, 0.06F).glow().alpha(0.0F);
            this.ember[i] = shell;

            parent = seg;
        }

        Part crown = part("crown", shaft, 0.0F, 0.0F, 0.0F);

        // The pale torn collar. Barbara does not cut her grass, she pulls it,
        // and the inch of white at the bottom is the single detail that says so.
        Part sheath = part("sheath", crown, 0.0F, 0.0F, 0.0F);
        sheath.box(-wide * 0.052F, -len * 0.004F, -wide * 0.016F,
                wide * 0.104F, len * 0.040F, wide * 0.032F).colour(0xC6D49B);
        remember(sheath, 0.0F);

        Part firstRoot = null;
        for (int i = 0; i < 3; i++) {
            Part strand = part("root" + i, crown, 0.0F, 0.0F, 0.0F);
            strand.rotY = 34.0F + i * 117.0F;
            strand.rotX = 11.0F + i * 6.0F;
            float thick = wide * (0.011F - i * 0.002F);
            float drop = len * (0.055F - i * 0.012F);
            strand.box(-thick, -drop, -thick, thick * 2.0F, drop, thick * 2.0F)
                    .colour(0xDCD6BC);
            remember(strand, 0.0F);
            if (i == 0) {
                firstRoot = strand;
            }
        }

        // Soil still clinging to the longest root - the thing was in the ground
        // a second ago and nothing about it has had time to tidy itself up.
        Part clod = part("clod", firstRoot, 0.0F, -len * 0.048F, 0.0F);
        clod.box(-wide * 0.020F, -len * 0.020F, -wide * 0.018F,
                wide * 0.040F, len * 0.024F, wide * 0.036F).colour(0x40301E);
        remember(clod, 0.0F);

        buildShoots(crown, len, wide);
    }

    /**
     * The smaller leaves that came up with it. They are shorter, darker and lean
     * out harder than the blade they grew beside, which is what stops the base
     * reading as the handle of a weapon.
     */
    private void buildShoots(Part crown, float len, float wide) {
        for (int j = 0; j < SHOOTS; j++) {
            Part shoot = part("shoot" + j, crown, 0.0F, len * (0.010F + 0.012F * j), 0.0F);
            // Irregular spacing on purpose: four shoots at exact right angles
            // read as a machined fitting rather than as a plant.
            shoot.rotY = 34.0F + j * 87.0F + (j % 2) * 21.0F;

            boolean dead = j == 2;              // one of them dried off weeks ago
            float reach = len * (0.15F + 0.04F * ((j * 5) % 3));
            float step = reach / SHOOT_SEGMENTS;

            Part parent = shoot;
            for (int s = 0; s < SHOOT_SEGMENTS; s++) {
                float k = (s + 0.5F) / SHOOT_SEGMENTS;
                Part seg = part("shoot" + j + "seg" + s, parent,
                        0.0F, s == 0 ? 0.0F : step, 0.0F);
                // Out of the stem first, then over. A shoot has no reason to
                // stand as straight as the blade that outgrew it.
                seg.rotX = s == 0 ? 27.0F + j * 3.0F : 14.0F + 10.0F * s;

                float hw = wide * 0.020F * (1.0F - 0.55F * k);
                seg.box(-hw, 0.0F, -hw * 0.5F, hw * 2.0F, step, hw);
                if (dead) {
                    seg.colour(mix(0.60F, 0.76F, k), mix(0.53F, 0.66F, k),
                            mix(0.24F, 0.31F, k));
                } else {
                    seg.colour(mix(0.13F, 0.30F, k), mix(0.28F, 0.52F, k),
                            mix(0.08F, 0.15F, k));
                }
                // Shoots sit at the base, so they are the last thing to cook.
                remember(seg, 0.05F);
                parent = seg;
            }
        }
    }

    @Override
    protected void pose(float time) {
        float spin = track("spin", time, 0.0F);
        Part body = get("body");
        // rotX is applied innermost and rotZ outermost, so spin is the tumble in
        // the blade's own frame and pitch tips the plane that tumble runs in.
        body.rotX = spin;
        body.rotZ = track("pitch", time, 0.0F);

        float grow = Mth.clamp(track("grow", time, 1.0F), 0.0F, 1.0F);
        Part shaft = get("shaft");
        if (grow <= 0.002F) {
            shaft.visible = false;
            return;
        }
        shaft.visible = true;
        // It thickens as well as lengthening on the way in, so it arrives like
        // something condensing out of the air rather than sliding in from off
        // screen at full width.
        float girth = 0.30F + 0.70F * grow;
        shaft.scale(girth, 1.0F, girth);

        // The root end and the shoots are all there almost at once; it is the
        // blade itself that takes the whole of the grow to unfurl.
        Part crown = get("crown");
        float rooted = Mth.clamp(grow / 0.22F, 0.0F, 1.0F);
        crown.visible = rooted > 0.02F;
        crown.scale(rooted, rooted, rooted);

        float lit = grow * SEGMENTS;
        float heat = Mth.clamp(track("glow", time, 0.0F), 0.0F, 1.0F);

        for (int i = 0; i < SEGMENTS; i++) {
            Part seg = this.segment[i];
            float head = lit - i;
            if (head <= 0.0F) {
                // Hiding a joint hides the whole chain above it, so the blade
                // unfurls from the torn end toward the tip for free.
                seg.visible = false;
                continue;
            }
            seg.visible = true;
            seg.scaleY = Math.min(1.0F, head);

            float k = (i + 0.5F) / SEGMENTS;
            // Whip: this joint answers to where the tumble WAS a moment ago, so
            // the blade bends against its own rotation instead of turning like
            // a rod. Sampling the actor's own track in the past is the cheapest
            // honest spring there is.
            float then = track("spin", time - i * 0.7F, spin);
            seg.rotX = this.curve[i] + (then - spin) * 0.020F * k;
            // Idle flutter, strongest at the tip, so it is never dead still.
            seg.rotZ = Mth.sin(time * 0.21F + i * 0.8F) * 1.5F * k;

            Part shell = this.ember[i];
            float local = Mth.clamp(heat * (0.45F + 1.05F * k), 0.0F, 1.0F);
            if (local <= 0.01F) {
                shell.visible = false;
                continue;
            }
            shell.visible = true;
            // Something cooking in the air does not brighten evenly, it boils.
            float boil = 0.80F + 0.20F * Mth.sin(time * 1.1F + i * 1.9F);
            shell.alpha = local * 0.62F * boil;
            shell.colour(1.0F, 0.30F + 0.62F * local, 0.05F + 0.55F * local * local);
            float bloom = 1.0F + local * 0.35F * boil;
            shell.scale(bloom, 1.0F, bloom);
        }

        // Repainting sixty boxes every frame for nothing is waste: only touch
        // them while the heat is on, plus the one frame that puts them back.
        if (heat > 0.001F || this.tinted) {
            this.tinted = heat > 0.001F;
            for (int i = 0; i < this.solids; i++) {
                Part p = this.solid[i];
                // The tip leads, because it is the thin end going down first.
                float u = Mth.clamp(heat * (0.45F + 1.05F * this.along[i]), 0.0F, 1.0F);
                p.colour(mix(this.coldR[i], 1.0F, u),
                        mix(this.coldG[i], 0.30F + 0.65F * u, u),
                        mix(this.coldB[i], 0.05F + 0.60F * u * u, u));
            }
        }
    }

    /**
     * Widest just above the sheath, then a steady taper toward the tip. The
     * taper never reaches zero: a tip one pixel across z-fights itself into a
     * shimmer at the distances these things are seen from.
     */
    private static float unitHalfWidth(float k) {
        float taper = Math.max(0.20F, (float) Math.pow(1.0D - k, 0.8D));
        float sheath = 0.70F + 0.30F * Math.min(1.0F, k / 0.09F);
        return 0.045F * taper * sheath;
    }

    /** Colour a vane, blending it toward dead straw by {@code dry}. */
    private void straw(Part p, float r, float g, float b, float dry, float k) {
        p.colour(mix(r, 0.72F, dry), mix(g, 0.62F, dry), mix(b, 0.26F, dry));
        remember(p, k);
    }

    /**
     * Record a solid box and the colour it is when cold, so the superheat ramp
     * has something to blend away from - and back to when it cools.
     */
    private void remember(Part p, float alongLength) {
        if (this.solids >= SOLIDS) {
            return;
        }
        this.solid[this.solids] = p;
        this.coldR[this.solids] = p.red;
        this.coldG[this.solids] = p.green;
        this.coldB[this.solids] = p.blue;
        this.along[this.solids] = alongLength;
        this.solids++;
    }

    /** Plain linear blend - none of the ramps in a leaf have easing to them. */
    private static float mix(float a, float b, float u) {
        return a + (b - a) * u;
    }
}
