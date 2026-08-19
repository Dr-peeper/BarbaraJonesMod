package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Easing;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Sky-Barbara mid-throw: the same woman as {@link SmokerActor}, but built to be
 * flung around the sky rather than to walk. She blinks out of one patch of red
 * sky, blinks into another, and hurls a clod of torn-up earth the moment she
 * arrives.
 *
 * <p>Three things have to be true or a teleporting giant reads as a rendering
 * fault instead of a performance:
 *
 * <ul>
 *   <li><b>She winds up.</b> The {@code throw} track runs 0..1 through a coil, a
 *       snap and a follow-through. Nearly half the track is spent pulling the
 *       wrong way - arm back, chest turned away from the target, weight on the
 *       back foot - because a throw with no anticipation is indistinguishable
 *       from a limb popping to a new angle.</li>
 *   <li><b>She leaves afterimages.</b> The {@code ghost} track draws four
 *       simplified copies of her standing where she actually was one, two, three
 *       and four moments ago, sampled straight off her own {@code dx/dy/dz}
 *       tracks. A trail is what turns a jump cut into speed; without it the eye
 *       reads a stutter and stops believing the space.</li>
 *   <li><b>She is legible from a mile off.</b> Every key pose throws a limb
 *       clear of the torso outline, and the ghosts are a dark, near-black wash of
 *       her own palette so they hold their shape against a red sky.</li>
 * </ul>
 *
 * <p>Tracks on top of the humanoid set: {@code throw} 0..1 (below zero means the
 * throw is unauthored and the limb tracks are left alone), {@code blink} 0..1
 * where 1 is fully gone, {@code ghost} 0..1 for the strength of the trail, and
 * {@code aim} degrees of yaw onto whatever she is throwing at.
 *
 * <p>While the throw is running it owns both arms, both legs, and the hip and
 * chest twist, so a scene drives the whole action off that one track instead of
 * keyframing eleven joints in sympathy with each other.
 */
public final class ThrowerActor extends HumanoidActor {

    private static final int GHOSTS = 4;

    /** Ticks between one afterimage and the next along her path. */
    private static final float GHOST_LAG = 1.7F;

    /** Longest trail we will draw, in model units - about five of her own heights. */
    private static final float TRAIL_LIMIT = 13.0F;

    /** Below this the trail is just jitter around a standing figure, so it is off. */
    private static final float TRAIL_MIN = 0.25F;

    // Where the coil ends and the snap ends, as fractions of the throw track.
    private static final float WIND_END = 0.42F;
    private static final float SNAP_END = 0.60F;

    /** The clod leaves her hand a hair before the arm reaches full extension. */
    private static final float LET_GO = 0.58F;

    // The afterimages are her own palette dragged most of the way to black: a
    // bright ghost against a red sky turns to soup, a dark one keeps its edges.
    private static final int GHOST_TOP = 0x2A1540;
    private static final int GHOST_JEANS = 0x141B2E;
    private static final int GHOST_SKIN = 0x5E4334;
    private static final int GHOST_HAIR = 0x1B100B;

    /** Spliced in between the root and her hips - see the constructor. */
    private final Part aim;

    /** Every box that belongs to her body, so the blink can fade all of it. */
    private final Part[] body;

    // Afterimage joints, cached rather than looked up by name: pose() runs every
    // frame and "ghost" + i would allocate a string on each one.
    private final Part[] ghost = new Part[GHOSTS];
    private final Part[] ghostTorso = new Part[GHOSTS];
    private final Part[] ghostArmR = new Part[GHOSTS];
    private final Part[] ghostArmL = new Part[GHOSTS];
    private final Part[] ghostLegR = new Part[GHOSTS];
    private final Part[] ghostLegL = new Part[GHOSTS];
    private final Part[][] ghostBoxes = new Part[GHOSTS][];

    public ThrowerActor() {
        // Identical palette to SmokerActor: this has to be recognisably the same
        // woman, or the cast stops being a cast and becomes a list of monsters.
        super(0xC98F6E, 0x3A2317, 0x5B2E86, 0x2B3A63, 0x1A1512);

        // The aim joint is spliced in between the root and the hips so that
        // turning her toward a target does NOT turn her trail with her. The
        // afterimages belong to the path she travelled, which has nothing to do
        // with the direction she is now facing.
        Part hips = get("hips");
        this.aim = part("aim", this.root, 0.0F, 0.0F, 0.0F);
        this.root.children.remove(hips);
        this.aim.children.add(hips);

        // What she throws: a lump of turf ripped out of the ground with the grass
        // still in it. It gives the wind-up something to be about, and a hand
        // that is visibly holding something reads as a throw a beat earlier than
        // an empty one does.
        Part clod = part("clod", get("handR"), 0.0F, -0.17F, 0.03F);
        clod.box(-0.14F, -0.14F, -0.14F, 0.28F, 0.28F, 0.28F).colour(0x4A3524);
        part("clodTurf", clod, 0.0F, 0.14F, 0.0F)
                .box(-0.15F, -0.03F, -0.15F, 0.30F, 0.05F, 0.30F).colour(0x4E7A2E);
        part("clodBlade", clod, 0.05F, 0.16F, -0.04F)
                .rot(-14.0F, 0.0F, 9.0F)
                .box(-0.02F, 0.0F, -0.02F, 0.04F, 0.19F, 0.04F).colour(0x5E8F36);

        // Snapshot her boxes BEFORE the ghosts exist, so the blink fade touches
        // her and nothing else.
        List<Part> boxes = new ArrayList<>();
        collect(this.aim, boxes);
        this.body = boxes.toArray(new Part[0]);

        for (int i = 0; i < GHOSTS; i++) {
            buildGhost(i);
        }
    }

    /**
     * One afterimage. Deliberately a cruder rig than she is: single-rod arms and
     * legs with no elbow or knee, because an afterimage lives for a handful of
     * frames at a distance and only its outline ever survives the trip to the
     * screen. Four full humanoids would cost four times the geometry to draw a
     * silhouette nobody could tell apart from this one.
     */
    private void buildGhost(int i) {
        String tag = "ghost" + i;
        Part g = part(tag, this.root, 0.0F, 0.0F, 0.0F);
        g.visible = false;

        Part hips = part(tag + "Hips", g, 0.0F, 1.15F, 0.0F);
        hips.box(-0.28F, -0.14F, -0.15F, 0.56F, 0.28F, 0.30F).colour(GHOST_JEANS);
        Part torso = part(tag + "Torso", hips, 0.0F, 0.14F, 0.0F);
        torso.box(-0.30F, 0.0F, -0.16F, 0.60F, 0.58F, 0.32F).colour(GHOST_TOP);
        // Neck and head collapsed into one joint at the head's real pivot height,
        // so a ghost's head sits exactly where hers does.
        Part head = part(tag + "Head", torso, 0.0F, 0.67F, 0.0F);
        head.box(-0.21F, 0.0F, -0.19F, 0.42F, 0.42F, 0.38F).colour(GHOST_SKIN);
        Part hair = part(tag + "Hair", head, 0.0F, 0.0F, 0.0F);
        hair.box(-0.23F, 0.16F, -0.21F, 0.46F, 0.30F, 0.34F).colour(GHOST_HAIR);

        Part armR = part(tag + "ArmR", torso, 0.36F, 0.50F, 0.0F);
        armR.box(-0.09F, -0.88F, -0.09F, 0.18F, 0.88F, 0.18F).colour(GHOST_TOP);
        Part armL = part(tag + "ArmL", torso, -0.36F, 0.50F, 0.0F);
        armL.box(-0.09F, -0.88F, -0.09F, 0.18F, 0.88F, 0.18F).colour(GHOST_TOP);
        Part legR = part(tag + "LegR", hips, 0.15F, -0.14F, 0.0F);
        legR.box(-0.11F, -0.94F, -0.11F, 0.22F, 0.94F, 0.22F).colour(GHOST_JEANS);
        Part legL = part(tag + "LegL", hips, -0.15F, -0.14F, 0.0F);
        legL.box(-0.11F, -0.94F, -0.11F, 0.22F, 0.94F, 0.22F).colour(GHOST_JEANS);

        this.ghost[i] = g;
        this.ghostTorso[i] = torso;
        this.ghostArmR[i] = armR;
        this.ghostArmL[i] = armL;
        this.ghostLegR[i] = legR;
        this.ghostLegL[i] = legL;
        this.ghostBoxes[i] = new Part[] { hips, torso, head, hair, armR, armL, legR, legL };
    }

    private static void collect(Part p, List<Part> out) {
        if (p.sizeX > 0.0F) {
            out.add(p);
        }
        for (int i = 0; i < p.children.size(); i++) {
            collect(p.children.get(i), out);
        }
    }

    @Override
    protected void pose(float time) {
        super.pose(time);

        this.aim.rotY = track("aim", time, 0.0F);

        // Below zero means no throw was authored, so her limbs stay on whatever
        // the humanoid tracks said - the same contract the walk cycle uses.
        float p = track("throw", time, -1.0F);
        if (p >= 0.0F) {
            throwPose(Mth.clamp(p, 0.0F, 1.0F), time);
        }

        trail(Mth.clamp(track("ghost", time, 0.0F), 0.0F, 1.0F), time);
        // Last, because it stamps an alpha over every box she owns.
        blink(Mth.clamp(track("blink", time, 0.0F), 0.0F, 1.0F));
    }

    // ---- the throw ---------------------------------------------------------

    private void throwPose(float p, float time) {
        Part hips = get("hips");
        Part torso = get("torso");

        float hipYaw = hipCoil(p);
        float chestYaw = chestCoil(p);
        hips.rotY = hipYaw;
        torso.rotY = chestYaw;

        // Added to the authored lean rather than replacing it, so a scene can
        // still tip her whole body without fighting the throw.
        hips.rotX += staged(p, 0.07F, 0.0F, -9.0F, 7.0F, 2.0F);
        torso.rotX += staged(p, 0.03F, 0.0F, -12.0F, 20.0F, 8.0F);

        get("armR").rotX = armSwing(p);
        get("armR").rotZ = armSweep(p);
        // Held nearly straight through the coil and the snap: a straight arm
        // sweeping two hundred degrees is a shape you can read from a mile away,
        // and a folded one is a smudge on the torso. It only folds on the way out.
        get("elbowR").rotX = -Math.abs(staged(p, -0.03F, 6.0F, 22.0F, 5.0F, 62.0F));

        get("armL").rotX = offArmSwing(p);
        get("armL").rotZ = staged(p, 0.05F, 3.0F, 15.0F, 8.0F, 20.0F);
        get("elbowL").rotX = -Math.abs(staged(p, 0.02F, 6.0F, 34.0F, 20.0F, 42.0F));

        get("legR").rotX = legDrive(p);
        get("legL").rotX = legLead(p);
        get("kneeR").rotX = -Math.abs(staged(p, 0.06F, 2.0F, 27.0F, 6.0F, 15.0F));
        get("kneeL").rotX = -Math.abs(staged(p, 0.06F, 2.0F, 10.0F, 23.0F, 12.0F));

        // Her head fights the coil to stay on the target: forty degrees of chest
        // turned away with the eyes still on you is the whole menace of a wind-up.
        Part head = get("head");
        head.rotY -= (hipYaw + chestYaw) * 0.72F;
        head.rotX += staged(p, 0.02F, 0.0F, -7.0F, 13.0F, 4.0F);

        // A shout on the release. The jaw already carries whatever the scene
        // authored, so take whichever is wider instead of stamping over it.
        float shout = 26.0F * hump(p, 0.46F, 0.84F);
        Part jaw = get("jaw");
        if (shout > jaw.rotX) {
            jaw.rotX = shout;
        }

        // The hair answers to how far the chest whipped over the last few ticks,
        // so it drags behind the coil and then overtakes it on the snap.
        float chestThen = chestCoil(Mth.clamp(track("throw", time - 3.0F, 0.0F), 0.0F, 1.0F));
        get("hairTail").rotZ += (chestYaw - chestThen) * 0.9F;
        get("hairTail").rotX -= (chestYaw - chestThen) * 0.25F;

        get("clod").visible = p < LET_GO;
    }

    // The key poses, named so the afterimages can strike the same ones she did off
    // a single copy of the numbers. The lead on each is what staggers the drive up
    // her body - hips first, chest next, throwing arm last; limbs that all arrive
    // together read as machinery rather than as a woman throwing something.
    //
    // Note where the twist sits at each stage: the chest arrives square on the
    // target at the RELEASE and only carries past it on the follow-through. A
    // chest already overrotated by the release would fire the clod off across her
    // own body, which is the classic tell of a throw animated backwards from its
    // finish pose.

    private static float hipCoil(float p) {
        return staged(p, 0.07F, 0.0F, 14.0F, -6.0F, -18.0F);
    }

    private static float chestCoil(float p) {
        return staged(p, 0.03F, 0.0F, 30.0F, -8.0F, -26.0F);
    }

    private static float armSwing(float p) {
        return staged(p, 0.0F, 4.0F, 88.0F, -124.0F, -46.0F);
    }

    private static float armSweep(float p) {
        return staged(p, 0.0F, -3.0F, 9.0F, -13.0F, -30.0F);
    }

    private static float offArmSwing(float p) {
        return staged(p, 0.05F, 4.0F, -66.0F, 30.0F, 8.0F);
    }

    private static float legDrive(float p) {
        return staged(p, 0.08F, 0.0F, 19.0F, -13.0F, -4.0F);
    }

    private static float legLead(float p) {
        return staged(p, 0.08F, 0.0F, -13.0F, 15.0F, 5.0F);
    }

    /**
     * One joint through the whole action: rest, cocked, released, settled.
     *
     * <p>The three curves are the throw. BACK_OUT yanks past the cocked pose and
     * settles back onto it, so the wind-up has a snatch in it. CUBIC_IN means the
     * snap is still accelerating when it reaches the release, which puts the peak
     * speed on the exact frame the clod leaves her hand. SPRING_OUT lets the arm
     * overshoot its resting angle and ring back, because nothing that heavy stops
     * where it meant to.
     *
     * @param lead fraction of the throw this joint runs ahead of the throwing arm
     */
    private static float staged(float p, float lead, float rest, float cocked,
                                float release, float settle) {
        float q = Easing.clamp01(p + lead);
        float wind = Easing.clamp01(q / WIND_END);
        float snap = Easing.clamp01((q - WIND_END) / (SNAP_END - WIND_END));
        float thru = Easing.clamp01((q - SNAP_END) / (1.0F - SNAP_END));
        // The three windows never overlap, so each stage starts from wherever the
        // previous one finished and chaining them like this is exact.
        float v = Easing.ease(Easing.BACK_OUT, rest, cocked, wind);
        v = Easing.ease(Easing.CUBIC_IN, v, release, snap);
        return Easing.ease(Easing.SPRING_OUT, v, settle, thru);
    }

    /** A smooth 0-1-0 hump across a window, for one-off pulses like the shout. */
    private static float hump(float p, float from, float to) {
        return Mth.sin(Easing.clamp01((p - from) / (to - from)) * Mth.PI);
    }

    // ---- the teleport ------------------------------------------------------

    /**
     * Fade her out and back in. She also pinches inward as she goes: a figure
     * that only loses alpha looks like a draw bug, one that compresses toward a
     * vertical seam looks like something took her away.
     */
    private void blink(float b) {
        float pinch = 1.0F - 0.74F * b;
        this.aim.scale(pinch, 1.0F + 0.5F * b, pinch);
        // CUBIC_OUT holds her solid and then drops her. A linear fade spends most
        // of the blink as a translucent smear of a woman, which is the one thing
        // a teleport must never look like.
        float solid = Easing.CUBIC_OUT.apply(1.0F - b);
        for (int i = 0; i < this.body.length; i++) {
            this.body[i].alpha = solid;
        }
    }

    /**
     * Stand the afterimages where she actually was, one lag apart, by sampling
     * her own position tracks in the past. Extrapolating a straight line from a
     * velocity would draw a trail she never travelled; the tracks already know
     * the route, including the moment it turned a corner.
     */
    private void trail(float amount, float time) {
        if (amount <= 0.004F) {
            for (int i = 0; i < GHOSTS; i++) {
                this.ghost[i].visible = false;
            }
            return;
        }

        float dxNow = track("dx", time, 0.0F);
        float dyNow = track("dy", time, 0.0F);
        float dzNow = track("dz", time, 0.0F);
        float inv = 1.0F / Math.max(0.001F, this.scale);
        // Her path is authored in world space, but the trail hangs off a rig that
        // has already been yawed and scaled, so the offset has to come back the
        // other way through both before it means anything.
        float rad = (float) Math.toRadians(this.yaw);
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);

        for (int i = 0; i < GHOSTS; i++) {
            float lag = GHOST_LAG * (i + 1);
            float wx = track("dx", time - lag, dxNow) - dxNow;
            float wy = track("dy", time - lag, dyNow) - dyNow;
            float wz = track("dz", time - lag, dzNow) - dzNow;
            float mx = (wx * cos - wz * sin) * inv;
            float my = wy * inv;
            float mz = (wx * sin + wz * cos) * inv;

            float len = (float) Math.sqrt(mx * mx + my * my + mz * mz);
            Part g = this.ghost[i];
            if (len < TRAIL_MIN) {
                // She is standing still. A trail on a stationary figure is just a
                // heap of copies of her, which reads as a graphical fault.
                g.visible = false;
                continue;
            }
            if (len > TRAIL_LIMIT) {
                // A blink can cross hundreds of blocks in a tick; past a few body
                // heights a trail stops being a streak and becomes litter dropped
                // on the far side of the sky.
                float k = TRAIL_LIMIT / len;
                mx *= k;
                my *= k;
                mz *= k;
            }

            g.visible = true;
            g.pivotX = mx;
            g.pivotY = my;
            g.pivotZ = mz;
            // Each one carries the aim she held at the moment it marks, so a trail
            // that swings around a target visibly turns as it goes.
            g.rotY = track("aim", time - lag, 0.0F);
            float shrink = 1.0F - 0.035F * (i + 1);
            g.scale(shrink, shrink, shrink);

            // A straight linear fade down the trail. All four have to stay visible
            // or the streak ends up with a head and no tail.
            float alpha = amount * (0.62F - 0.13F * i);
            Part[] boxes = this.ghostBoxes[i];
            for (int b = 0; b < boxes.length; b++) {
                boxes[b].alpha = alpha;
            }

            // And each is frozen in the pose she held then, so the trail strings
            // the whole wind-up out across the sky behind her.
            float pThen = track("throw", time - lag, -1.0F);
            if (pThen >= 0.0F) {
                float q = Mth.clamp(pThen, 0.0F, 1.0F);
                this.ghostTorso[i].rotY = chestCoil(q);
                this.ghostArmR[i].rotX = armSwing(q);
                this.ghostArmR[i].rotZ = armSweep(q);
                this.ghostArmL[i].rotX = offArmSwing(q);
                this.ghostLegR[i].rotX = legDrive(q);
                this.ghostLegL[i].rotX = legLead(q);
            }
        }
    }
}
