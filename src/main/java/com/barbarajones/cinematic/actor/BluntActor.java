package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The blunt: forty blocks of rolled joint hanging in the sky, lit, burning down
 * in real time.
 *
 * <p>This is the prop the torching beat is built around and it was missing from
 * the shot, which is why it is built to be unmistakable rather than suggested.
 * The shaft is a twelve-sided tube of staves, not a box - a rectangular prism
 * reads as a plank the instant the camera comes off-axis, and the one thing this
 * object cannot afford is to be mistaken for anything else. Twelve flats around
 * a circle is enough for the silhouette to stay convincingly round while the
 * camera orbits, and cheap enough to segment along its length as well.
 *
 * <p>Authored with the <b>lit tip at y=0</b> and the paper running up +Y, the
 * same convention as {@link TorchActor}'s nozzle and {@link CleaverActor}'s edge,
 * so the actor's {@code dy} track is the height of the coal at the moment it is
 * lit. It does not stay there: a joint burns from the lit end back, so the coal
 * climbs the shaft on the {@code burn} track and the ash follows it. That is what
 * {@link #tip(float)} is for - a scene hanging smoke or fire off the coal has to
 * ask where the coal is NOW, not where the prop was bound.
 *
 * <p>The shaft is banded along its length so the burn can be a gradient rather
 * than a switch: clean paper ahead of the coal, black char immediately behind it,
 * pale grey ash further back, and nothing at all where the ash has crumbled off
 * the end. A single "burnt" colour would make the whole thing change state in one
 * frame, which is the tell that something is a texture swap and not an object.
 *
 * <p>Tracks: {@code ember} 0..1 coal intensity, {@code burn} 0..1 how far the ash
 * has crept, {@code spin}, {@code pitch}, {@code grow}.
 */
public final class BluntActor extends CinematicActor {

    private static final int SIDES = 12;
    private static final int RINGS = 10;
    private static final int SEAM_SEGMENTS = 14;
    private static final int TWIST_FLAPS = 5;

    private static final float[] SIDE_COS = new float[SIDES];
    private static final float[] SIDE_SIN = new float[SIDES];
    private static final float[] SIDE_DEG = new float[SIDES];

    /**
     * Half the flat width of one stave per unit of radius. This is the tangent
     * polygon, so every flat touches the circle at its midpoint; the 1.06 is
     * deliberate overlap, because staves that meet exactly leave hairline gaps
     * the moment anything is scaled.
     */
    private static final float STAVE_HALF = (float) Math.tan(Math.PI / SIDES) * 1.06F;

    static {
        for (int s = 0; s < SIDES; s++) {
            double a = s * Math.PI * 2.0D / SIDES;
            SIDE_COS[s] = (float) Math.cos(a);
            SIDE_SIN[s] = (float) Math.sin(a);
            SIDE_DEG[s] = (float) Math.toDegrees(a);
        }
    }

    /**
     * Fraction of the shaft a full burn eats. Short of the whole thing on
     * purpose - nobody smokes a joint down to the crutch on camera, and the
     * unburnt stub is what tells the eye how long it used to be.
     */
    private static final float BURN_SPAN = 0.78F;

    /**
     * Bands of ash that cling on behind the coal before they flake away. Short,
     * because an ash column that only ever grows would end up longer than the
     * joint was; losing it is what makes the blunt visibly get shorter.
     */
    private static final float ASH_KEEP = 2.6F;

    /**
     * How far the char-to-grey ramp runs, in bands. Kept inside {@link #ASH_KEEP}
     * so the gradient finishes before the ash it is painted on drops off.
     */
    private static final float ASH_RAMP = 2.2F;

    private static final int PAPER = 0xE9E2CE;
    private static final int SEAM = 0xCFC2A4;
    private static final int CRUTCH = 0xAE8250;
    private static final int THROAT = 0x1A130E;
    private static final int TWIST = 0xDCD3BC;

    private final float length;
    private final float radius;
    private final float coreLength;

    /**
     * @param size multiplier on every dimension of the model. At 1.0 the blunt is
     *             2.4 units long, matching {@link HumanoidActor}'s height, so an
     *             actor scale reads the same way it does for the cast.
     */
    public BluntActor(float size) {
        float k = Math.max(0.05F, size);
        this.length = 2.40F * k;
        this.radius = 0.115F * k;
        // The core stops short of the mouth end so the crutch is a real hole and
        // not a green plug you are apparently expected to draw through.
        this.coreLength = this.length * 0.88F;

        Part body = part("body", this.root, 0.0F, 0.0F, 0.0F);

        // --- the packed core -------------------------------------------------
        // Pivoted at the MOUTH end with its box hanging back down to the tip, so
        // burning it away is one scaleY on the group: the end that survives is
        // the end the pivot is on.
        Part core = tube("core", body, this.coreLength, -this.coreLength, this.coreLength,
                this.radius * 0.10F, this.radius * 0.82F, 0x4A6B26);
        for (int i = 0; i < core.children.size(); i++) {
            // Ground-up grass is not one flat green. Deterministic per-stave
            // shading so the cut face reads as packed material, not plastic.
            float shade = 0.76F + ((i * 31) % 13) / 13.0F * 0.46F;
            core.children.get(i).colour(0.29F * shade, 0.42F * shade, 0.15F * shade);
        }
        // Fills the axis so the staves' inner ends are buried rather than all
        // meeting in one coincident sliver down the middle.
        part("corePith", core, 0.0F, 0.0F, 0.0F)
                .box(-this.radius * 0.17F, -this.coreLength, -this.radius * 0.17F,
                        this.radius * 0.34F, this.coreLength, this.radius * 0.34F)
                .colour(0.20F, 0.29F, 0.11F);

        // --- the paper, banded so the burn can be a gradient -------------------
        Part shaft = part("shaft", body, 0.0F, 0.0F, 0.0F);
        float band = this.length / RINGS;
        for (int r = 0; r < RINGS; r++) {
            tube("band" + r, shaft, r * band, 0.0F, band * 1.04F,
                    this.radius * 0.80F, this.radius, PAPER);
        }

        // --- the seam ---------------------------------------------------------
        // It spirals, because a blunt is rolled rather than extruded, and a
        // straight line up the side would read as a moulding mark. Proud of the
        // surface so it catches the shading instead of z-fighting the paper.
        Part seam = part("seam", body, 0.0F, 0.0F, 0.0F);
        float seg = this.length / SEAM_SEGMENTS;
        for (int i = 0; i < SEAM_SEGMENTS; i++) {
            double a = i / (double) SEAM_SEGMENTS * Math.PI * 0.62D;
            Part strip = part("seam" + i, seam, (float) Math.cos(a) * this.radius * 0.99F,
                    i * seg, (float) Math.sin(a) * this.radius * 0.99F);
            strip.rotY = -(float) Math.toDegrees(a);
            strip.box(0.0F, 0.0F, -this.radius * 0.15F, this.radius * 0.10F,
                    seg * 1.06F, this.radius * 0.30F).colour(SEAM);
        }

        // --- the mouth end ----------------------------------------------------
        tube("crutch", body, this.length * 0.90F, 0.0F, this.length * 0.10F,
                this.radius * 0.85F, this.radius * 1.05F, CRUTCH);
        tube("throat", body, this.length * 0.94F, 0.0F, this.length * 0.06F,
                this.radius * 0.12F, this.radius * 0.85F, THROAT);

        // --- the twisted tip --------------------------------------------------
        // Each flap gets its own spoke so the lean happens INSIDE the radial
        // frame: rotZ on the spoke's child tips the flap toward the axis, while
        // rotX swings it sideways. Five flaps converging and all swung the same
        // way is what a twist actually is.
        Part twist = part("twist", body, 0.0F, 0.0F, 0.0F);
        for (int i = 0; i < TWIST_FLAPS; i++) {
            double a = i * Math.PI * 2.0D / TWIST_FLAPS;
            Part spoke = part("twistSpoke" + i, twist, (float) Math.cos(a) * this.radius * 0.62F,
                    0.0F, (float) Math.sin(a) * this.radius * 0.62F);
            spoke.rotY = -(float) Math.toDegrees(a);
            Part flap = part("twistFlap" + i, spoke, 0.0F, 0.0F, 0.0F);
            flap.rotZ = -24.0F;
            flap.rotX = -30.0F;
            flap.box(-this.radius * 0.30F, -this.radius * 1.70F, -this.radius * 0.34F,
                    this.radius * 0.60F, this.radius * 1.70F, this.radius * 0.68F).colour(TWIST);
        }

        // --- the coal ---------------------------------------------------------
        // Everything that burns rides one group, because they all have to travel
        // up the shaft together as the thing is consumed.
        Part coal = part("coal", body, 0.0F, 0.0F, 0.0F);
        glowAll(tube("coalFace", coal, 0.0F, -this.radius * 0.34F, this.radius * 0.68F,
                0.0F, this.radius * 0.84F, 0xFF5A0E));
        glowAll(tube("cherry", coal, 0.0F, -this.radius * 0.17F, this.radius * 0.34F,
                this.radius * 0.79F, this.radius * 1.06F, 0xFF7A16));
        glowAll(tube("halo", coal, 0.0F, -this.radius * 0.55F, this.radius * 1.10F,
                this.radius * 0.98F, this.radius * 2.20F, 0xFF6A1E));
    }

    /**
     * One tube section: SIDES flat staves stood around the Y axis. Each stave is
     * pivoted on the circle and turned so its local +X points straight out, which
     * is what lets {@link #pose} shrink or wobble it radially later with nothing
     * but an offset - no trigonometry per frame.
     */
    private Part tube(String name, Part parent, float pivotY, float y0, float height,
                      float rIn, float rOut, int rgb) {
        Part group = part(name, parent, 0.0F, pivotY, 0.0F);
        float half = rOut * STAVE_HALF;
        for (int s = 0; s < SIDES; s++) {
            Part stave = part(name + "_" + s, group,
                    SIDE_COS[s] * rIn, 0.0F, SIDE_SIN[s] * rIn);
            stave.rotY = -SIDE_DEG[s];
            stave.box(0.0F, y0, -half, rOut - rIn, height, half * 2.0F).colour(rgb);
        }
        return group;
    }

    private static void glowAll(Part group) {
        for (int i = 0; i < group.children.size(); i++) {
            group.children.get(i).glow();
        }
    }

    @Override
    protected void pose(float time) {
        Part body = get("body");
        body.rotY = track("spin", time, 0.0F);
        body.rotX = track("pitch", time, 0.0F);
        float grow = Math.max(0.05F, track("grow", time, 1.0F));
        body.scale(grow, grow, grow);

        float burn = Mth.clamp(track("burn", time, 0.0F), 0.0F, 1.0F);
        float ember = Mth.clamp(track("ember", time, 0.6F), 0.0F, 1.6F);
        float front = burn * BURN_SPAN;

        // A coal does not glow steadily, it answers the draw on it. Two phases
        // that do not divide into each other, so it never settles into a loop the
        // eye can predict.
        float flicker = ember * (0.80F + 0.14F * Mth.sin(time * 0.83F)
                + 0.06F * Mth.sin(time * 2.61F));

        paper(time, front, flicker);
        seam(front);
        // The packed core is eaten from the lit end back. One scale does it,
        // because the group hangs off the mouth end.
        get("core").scaleY = Math.max(0.001F,
                (this.coreLength - front * this.length) / this.coreLength);
        twist(front);
        coal(time, front, flicker);
    }

    /**
     * Clean paper ahead of the coal, char and ash behind it. The band right at
     * the front is the only place the two meet, and keeping that boundary one
     * band wide is what makes the burn read as a line creeping up the shaft
     * rather than the whole prop changing colour.
     */
    private void paper(float time, float front, float flicker) {
        Part shaft = get("shaft");
        for (int r = 0; r < RINGS; r++) {
            Part band = shaft.children.get(r);
            // How many bands of burn have already gone past this one.
            float age = (front - (r + 0.5F) / RINGS) * RINGS;
            for (int s = 0; s < SIDES; s++) {
                Part stave = band.children.get(s);
                if (age <= 0.0F) {
                    // Warmed by the coal beside it. Paper that stays flat white
                    // next to a fire tells the eye the fire is a decal.
                    float lit = Mth.clamp(1.0F + age * 0.55F, 0.0F, 1.0F) * flicker * 0.45F;
                    stave.colour(ramp(0.914F, 1.0F, lit), ramp(0.886F, 0.60F, lit),
                            ramp(0.807F, 0.28F, lit));
                    stave.offX = 0.0F;
                    stave.offZ = 0.0F;
                    stave.alpha = 1.0F;
                } else {
                    float aged = Mth.clamp(age / ASH_RAMP, 0.0F, 1.0F);
                    // Black at the coal cooling to pale grey further back - ash
                    // is a gradient, and the gradient is what dates the burn.
                    stave.colour(ramp(0.20F, 0.66F, aged), ramp(0.15F, 0.63F, aged),
                            ramp(0.13F, 0.59F, aged));
                    // It draws in and crumbles as it cools, on its own phase per
                    // stave so the column goes ragged instead of merely thinner.
                    float flake = Mth.sin(time * 0.29F + r * 1.7F + s * 0.93F) * 0.04F;
                    stave.offX = -this.radius * (0.03F + 0.09F * aged + flake * aged);
                    stave.offZ = Mth.cos(time * 0.23F + s * 1.31F) * this.radius * 0.04F * aged;
                    // The oldest of it has already dropped off the end, which is
                    // what makes the blunt visibly get shorter.
                    stave.alpha = 1.0F - Mth.clamp((age - ASH_KEEP) / 1.8F, 0.0F, 1.0F);
                }
            }
        }
    }

    /** The seam ages on the same clock as the paper it is glued to. */
    private void seam(float front) {
        Part seam = get("seam");
        for (int i = 0; i < SEAM_SEGMENTS; i++) {
            Part strip = seam.children.get(i);
            float age = (front - (i + 0.5F) / SEAM_SEGMENTS) * RINGS;
            if (age <= 0.0F) {
                strip.colour(SEAM);
                strip.alpha = 1.0F;
            } else {
                float aged = Mth.clamp(age / ASH_RAMP, 0.0F, 1.0F);
                strip.colour(ramp(0.17F, 0.56F, aged), ramp(0.13F, 0.53F, aged),
                        ramp(0.11F, 0.50F, aged));
                strip.alpha = 1.0F - Mth.clamp((age - ASH_KEEP) / 1.8F, 0.0F, 1.0F);
            }
        }
    }

    /**
     * The twist is the first thing the fire takes. It chars and curls in on
     * itself over the opening moments of the burn and is gone by the time the
     * coal has moved a band, which is exactly how a lit joint loses it.
     */
    private void twist(float front) {
        Part twist = get("twist");
        float gone = Mth.clamp(front / 0.055F, 0.0F, 1.0F);
        if (gone >= 1.0F) {
            twist.visible = false;
            return;
        }
        twist.visible = true;
        // Uniform on purpose: squashing a group whose children are rotated on two
        // axes would shear them into paper aeroplanes.
        float curl = 1.0F - gone * 0.85F;
        twist.scale(curl, curl, curl);
        for (int i = 0; i < twist.children.size(); i++) {
            Part spoke = twist.children.get(i);
            for (int j = 0; j < spoke.children.size(); j++) {
                Part flap = spoke.children.get(j);
                flap.colour(ramp(0.863F, 0.16F, gone), ramp(0.827F, 0.12F, gone),
                        ramp(0.737F, 0.10F, gone));
                flap.alpha = 1.0F - gone * 0.55F;
            }
        }
    }

    /**
     * The burning end, riding up the shaft as the paper goes. Three layers: the
     * lit cross-section, the ring of live embers on the paper's rim, and a soft
     * bloom around the whole thing that swells on a draw.
     */
    private void coal(float time, float front, float flicker) {
        get("coal").offY = front * this.length;

        Part face = get("coalFace");
        float bloom = 1.0F + flicker * 0.30F;
        face.scale(bloom, 1.0F, bloom);
        for (int s = 0; s < face.children.size(); s++) {
            Part cell = face.children.get(s);
            float beat = flicker * (0.82F + 0.18F * Mth.sin(time * 1.9F + s * 1.27F));
            cell.colour(1.0F, ramp(0.20F, 0.80F, beat), ramp(0.03F, 0.36F, beat));
            cell.alpha = Mth.clamp(0.16F + beat, 0.0F, 1.0F);
        }

        Part cherry = get("cherry");
        for (int s = 0; s < cherry.children.size(); s++) {
            Part cell = cherry.children.get(s);
            // Each ember on the rim lives its own life, so the ring boils instead
            // of pulsing as one lamp - a lamp is the thing this must not look like.
            float beat = flicker * (0.55F + 0.45F * Mth.sin(time * 1.35F + s * 2.11F));
            cell.colour(1.0F, ramp(0.16F, 0.62F, beat), ramp(0.02F, 0.18F, beat));
            cell.alpha = Mth.clamp(0.08F + beat * 0.95F, 0.0F, 1.0F);
            cell.offX = Mth.sin(time * 0.7F + s * 1.63F) * this.radius * 0.04F;
        }

        Part halo = get("halo");
        float reach = 1.0F + flicker * 1.10F;
        halo.scale(reach, 1.0F, reach);
        for (int s = 0; s < halo.children.size(); s++) {
            Part cell = halo.children.get(s);
            cell.alpha = Mth.clamp(0.05F + 0.12F * flicker, 0.0F, 0.5F);
        }
    }

    // ---- where the fire actually is ----------------------------------------

    /**
     * The coal's offset from the actor's own origin, in model units, at this
     * scene time. Zero when it is freshly lit and climbing from there, because
     * the model is authored with the lit tip on the origin.
     */
    public Vec3 tipLocal(float time) {
        float grow = Math.max(0.05F, track("grow", time, 1.0F));
        float up = Mth.clamp(track("burn", time, 0.0F), 0.0F, 1.0F)
                * BURN_SPAN * this.length * grow;
        float pitch = track("pitch", time, 0.0F) * Mth.DEG_TO_RAD;
        float spin = track("spin", time, 0.0F) * Mth.DEG_TO_RAD;
        float sp = Mth.sin(pitch);
        return new Vec3(up * sp * Mth.sin(spin), up * Mth.cos(pitch), up * sp * Mth.cos(spin));
    }

    /**
     * World point of the coal at this scene time - what a cue should hang smoke,
     * fire or a sound on so it leaves the blunt at the burning end rather than
     * from the middle of the prop.
     *
     * <p>Rebuilt from the tracks the same way {@link CinematicActor#render}
     * rebuilds the actor's transform, rather than read off whatever the last
     * frame happened to leave behind, so a cue firing on a whole tick gets the
     * answer for that tick and not for the frame before it.
     */
    public Vec3 tip(float time) {
        Vec3 local = tipLocal(time);
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

    /** Plain linear blend: nothing inside a fire eases. */
    private static float ramp(float a, float b, float u) {
        return a + (b - a) * Mth.clamp(u, 0.0F, 1.0F);
    }
}
