package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Easing;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * THE INTERNET MANAGER: the contractor who turns up after the television has
 * been destroyed, in the manner of a utility man arriving to fix an outage he is
 * in fact about to make very much worse.
 *
 * <p>Serves the beat directly after the set is broken: he walks in from off
 * shot paying out cable behind him, kneels on the wreckage to set a charge,
 * stands, and points at his own work a moment before it goes off.
 *
 * <p><b>He is not {@link ManagerActor}.</b> That one is the faceless suit who
 * walks out of the horizon and does not stop - no eyes, no mouth, a long black
 * necktie, nothing on him that has a use. This one is the opposite read and is
 * built to be told apart at a mile: hard hat, hi-vis over a work shirt, a short
 * clip-on tie, a belt of actual tools, boots, a moustache and a headset. He has
 * a face and he is entirely willing to use it on you. The horror in him is that
 * he is polite and equipped, not that he is empty.
 *
 * <p>The cable is the whole point of the character, so it is real geometry:
 * {@link #layCable} strings a run of short links between clamp posts with a
 * catenary dip in each span. Many short segments with sag is what makes a line
 * of boxes read as rope; one long box reads as a pipe and nothing will save it.
 *
 * <p>The run is <b>pinned to the world</b>, not to him. It hangs off a node that
 * counter-transforms his own {@code dx/dy/dz} and {@code yaw} every frame - the
 * same inverse the afterimages in {@link ThrowerActor} use - so cable that has
 * been laid stays exactly where it was laid while he walks away from it. Cable
 * that slid along with the man would be a decal on his back.
 *
 * <p>Tracks on top of the humanoid set:
 * <ul>
 *   <li>{@code unspool} 0..1 - how much of the run is out. It is revealed from
 *       the FAR end (the end he started at) toward his feet, so keying it in
 *       step with {@code walk} keeps the head of the cable under his boots.</li>
 *   <li>{@code plant} 0..1 - down onto one knee to set the charge.</li>
 *   <li>{@code point} 0..1 - the gesture at his work. It aims forward and down
 *       in front of him, so yaw him back toward the run before using it.</li>
 *   <li>{@code charge} - optional override: above 0.5 the charge is in the
 *       ground, below it is in his hand. Unauthored, it follows {@code plant},
 *       which is enough when the plant is the last thing he does with it.</li>
 * </ul>
 */
public final class InternetManagerActor extends HumanoidActor {

    // ---- palette -----------------------------------------------------------

    private static final int HIVIS = 0xC9E93A;
    private static final int HIVIS_DARK = 0x9EBA26;
    private static final int TAPE = 0xE4E9EC;
    private static final int HAT = 0xE8B21A;
    private static final int HAT_DARK = 0xC5900E;
    private static final int LEATHER = 0x33241A;
    private static final int LEATHER_DARK = 0x1D1510;
    private static final int STEEL = 0x9BA3AA;
    private static final int PLASTIC = 0x2A2E34;
    private static final int TIE = 0x62141C;
    private static final int BADGE = 0x1D2A44;
    private static final int CABLE = 0xC85B0D;
    private static final int CABLE_DARK = 0x8A3C07;
    private static final int SLEEVE = 0x1B1917;

    // ---- the cable run -----------------------------------------------------

    /** Links per anchor-to-anchor span. Enough to curve; cheap enough to spam. */
    private static final int LINKS_PER_SPAN = 7;

    /** Ceiling on spans, so a careless scene cannot ask for ten thousand boxes. */
    private static final int MAX_SPANS = 16;

    /** Height of the clamp posts the run is strung between, in model units. */
    private static final float CLAMP_TOP = 0.34F;

    /** Mid-span dip as a fraction of the span, capped so it never sinks. */
    private static final float SAG = 0.13F;

    private static final float CABLE_R = 0.055F;

    /** Sideways wander of the run, so it is not a drawn straight line. */
    private static final float WANDER = 0.055F;

    // Where the charge ends up, in model units from his bind position: half a
    // body length behind him and off to one side, so the pointing arm has
    // something to aim at that is not his own boots.
    private static final float CHARGE_X = 0.46F;
    private static final float CHARGE_Y = 0.22F;
    private static final float CHARGE_Z = -0.62F;

    /** Plant value at which the charge has left his hand for the ground. */
    private static final float SET_AT = 0.86F;

    /** Size multiplier, carried on the root - see the constructor. */
    private final float size;

    // Everything pose() touches by index is cached here. pose() runs per frame,
    // and get("link" + i) would build a string per box per frame.
    private Part[] link = new Part[0];
    private float[] linkPitch = new float[0];
    private float[] linkYaw = new float[0];
    private Part[] clamp = new Part[0];
    private float[] clampAt = new float[0];

    private final Part ground;
    private final Part hat;
    private final Part spanner;
    private final Part reelDrum;
    private final Part reelWound;
    private final Part lead;
    private final Part chargeSet;
    private final Part chargeLamp;
    private final Part chargeHeld;
    private final Part[] tape = new Part[3];

    /**
     * @param size multiplier on the whole model. At 1.0 he is the standard 2.4
     *             units tall, the same as the rest of the cast, so an actor
     *             scale reads as "height in blocks divided by 2.4" exactly as it
     *             does for them.
     */
    public InternetManagerActor(float size) {
        // Weathered outdoor skin, greying hair, chambray work shirt, khaki
        // trousers, oiled leather boots. Nothing on him is grey and nothing is
        // a suit, which is the whole distance between him and the Manager.
        super(0xB98157, 0x594B3C, 0x8FA6BE, 0x6F6449, 0x3A2A1C);

        this.size = Math.max(0.05F, size);
        // The multiplier lives on the root so every number below stays authored
        // at nominal humanoid scale and can be compared with the other actors.
        this.root.scale(this.size, this.size, this.size);

        head();
        vest();
        belt();
        boots();
        this.hat = get("hat");
        this.spanner = get("spanner");

        Part reel = reel();
        this.reelDrum = get("drum");
        this.reelWound = get("wound");
        this.lead = lead(reel);

        // The node that stays where the world is. Everything he leaves behind -
        // the run, the charge - hangs off this, and pose() undoes his own travel
        // and yaw on it every frame so none of it walks off with him.
        this.ground = part("ground", this.root, 0.0F, 0.0F, 0.0F);

        this.chargeSet = part("chargeSet", this.ground, CHARGE_X, 0.0F, CHARGE_Z);
        this.chargeSet.box(-0.13F, 0.0F, -0.10F, 0.26F, 0.20F, 0.20F).colour(PLASTIC);
        part("chargeStrap", this.chargeSet, 0.0F, 0.0F, 0.0F)
                .box(-0.14F, 0.05F, -0.11F, 0.28F, 0.05F, 0.22F).colour(LEATHER_DARK);
        // The stub of cable back into the run: without it the charge is a box
        // sitting near some cable rather than a thing wired into it.
        part("chargeTail", this.chargeSet, -0.13F, 0.07F, 0.0F)
                .box(-0.50F, -0.03F, -0.03F, 0.50F, 0.06F, 0.06F).colour(CABLE);
        this.chargeLamp = part("chargeLamp", this.chargeSet, 0.0F, 0.20F, 0.05F);
        this.chargeLamp.box(-0.05F, 0.0F, -0.05F, 0.10F, 0.07F, 0.10F)
                .colour(1.0F, 0.10F, 0.06F).glow();
        this.chargeSet.visible = false;

        // The same object in his hand, so setting it is one thing moving from
        // one place to another rather than two props swapping over.
        this.chargeHeld = part("chargeHeld", get("handR"), 0.0F, -0.17F, 0.05F);
        this.chargeHeld.box(-0.13F, -0.10F, -0.10F, 0.26F, 0.20F, 0.20F).colour(PLASTIC);
        part("chargeHeldLamp", this.chargeHeld, 0.0F, 0.10F, 0.05F)
                .box(-0.05F, 0.0F, -0.05F, 0.10F, 0.07F, 0.10F)
                .colour(1.0F, 0.10F, 0.06F).glow().alpha(0.55F);
        this.chargeHeld.visible = false;
    }

    // ---- rig ---------------------------------------------------------------

    private void head() {
        Part head = get("head");
        // A hard hat has nowhere to put a trailing lock, and the silhouette is
        // busy enough already.
        get("hairTail").visible = false;

        // Pivoted at the crown, not at the neck, so the follow-through in pose()
        // rocks the hat on his head instead of swinging it round his throat.
        Part hat = part("hat", head, 0.0F, 0.30F, 0.0F);
        part("hatBrim", hat, 0.0F, 0.0F, 0.0F)
                .box(-0.275F, -0.012F, -0.255F, 0.55F, 0.032F, 0.51F).colour(HAT_DARK);
        // The peak is the half of the brim that says hard hat rather than bowler.
        part("hatPeak", hat, 0.0F, 0.0F, 0.0F)
                .box(-0.19F, -0.010F, 0.255F, 0.38F, 0.030F, 0.125F).colour(HAT_DARK);
        part("hatShell", hat, 0.0F, 0.0F, 0.0F)
                .box(-0.245F, 0.020F, -0.225F, 0.49F, 0.200F, 0.45F).colour(HAT);
        // The comb down the middle. One ridge is the difference between a helmet
        // and a yellow box on a head.
        part("hatComb", hat, 0.0F, 0.0F, 0.0F)
                .box(-0.055F, 0.200F, -0.225F, 0.11F, 0.050F, 0.45F).colour(HAT_DARK);
        part("hatPlate", hat, 0.0F, 0.0F, 0.0F)
                .box(-0.085F, 0.055F, 0.225F, 0.17F, 0.080F, 0.02F).colour(BADGE);

        // Moustache: the cheapest possible "this is a specific middle-aged man"
        // and it survives being seen from sixty blocks below.
        part("stache", head, 0.0F, 0.13F, 0.19F)
                .box(-0.125F, 0.0F, 0.0F, 0.25F, 0.055F, 0.035F).colour(0x6B5B48);

        // Headset with a boom mic - he is talking to a dispatcher the whole time,
        // which is far worse than if he were talking to you.
        Part ear = part("earpiece", head, 0.215F, 0.22F, -0.02F);
        ear.box(-0.03F, -0.07F, -0.07F, 0.06F, 0.14F, 0.14F).colour(PLASTIC);
        Part boom = part("boom", ear, -0.01F, -0.03F, 0.06F);
        boom.rotY = 16.0F;
        boom.box(-0.015F, -0.015F, 0.0F, 0.03F, 0.03F, 0.30F).colour(PLASTIC);
        part("boomTip", boom, 0.0F, 0.0F, 0.30F)
                .box(-0.035F, -0.035F, 0.0F, 0.07F, 0.07F, 0.06F).colour(0x14161A);
    }

    private void vest() {
        Part torso = get("torso");

        // Open collar in the same cloth as the shirt: a working man's collar,
        // not the crisp white one the other giant wears.
        part("collar", torso, 0.0F, 0.52F, 0.0F)
                .box(-0.22F, 0.0F, -0.18F, 0.44F, 0.07F, 0.36F).colour(0xA9BDD2);

        // Clip-on, short and wide where the Manager's is long and narrow. Same
        // office uniform, worn by a man who has to bend over in it.
        Part knot = part("tie", torso, 0.0F, 0.50F, 0.168F);
        knot.box(-0.050F, -0.085F, 0.0F, 0.10F, 0.085F, 0.033F).colour(TIE);
        part("tieBlade", knot, 0.0F, -0.085F, 0.010F)
                .box(-0.065F, -0.26F, 0.0F, 0.13F, 0.26F, 0.028F).colour(0x7A1E27);

        // Two front panels with a gap for the tie, plus a back. A vest that
        // wrapped the torso as one box would read as a painted-on shirt.
        panel("vestFrontR", torso, 0.075F, 0.235F, 0.160F);
        panel("vestFrontL", torso, -0.310F, 0.235F, 0.160F);
        panel("vestBack", torso, -0.310F, 0.620F, -0.205F);

        part("strapR", torso, 0.0F, 0.0F, 0.0F)
                .box(0.105F, 0.520F, -0.205F, 0.155F, 0.045F, 0.41F).colour(HIVIS_DARK);
        part("strapL", torso, 0.0F, 0.0F, 0.0F)
                .box(-0.260F, 0.520F, -0.205F, 0.155F, 0.045F, 0.41F).colour(HIVIS_DARK);

        // Retroreflective tape: two bands per panel, standing proud so they
        // catch their own shading instead of z-fighting the vest.
        band("tapeLowR", torso, 0.075F, 0.235F, 0.160F, 0.155F);
        band("tapeLowL", torso, -0.310F, 0.235F, 0.160F, 0.155F);
        band("tapeLowB", torso, -0.310F, 0.620F, -0.205F, 0.155F);
        this.tape[0] = band("tapeHighR", torso, 0.075F, 0.235F, 0.160F, 0.320F);
        this.tape[1] = band("tapeHighL", torso, -0.310F, 0.235F, 0.160F, 0.320F);
        this.tape[2] = band("tapeHighB", torso, -0.310F, 0.620F, -0.205F, 0.320F);

        // The badge: signal bars, because the one thing this man is here about is
        // your connection.
        Part patch = part("patch", torso, 0.185F, 0.415F, 0.205F);
        patch.box(-0.085F, -0.060F, 0.0F, 0.17F, 0.12F, 0.018F).colour(BADGE);
        for (int i = 0; i < 3; i++) {
            part("bar" + i, patch, -0.050F + i * 0.045F, -0.040F, 0.018F)
                    .box(-0.014F, 0.0F, 0.0F, 0.028F, 0.035F + i * 0.028F, 0.012F)
                    .colour(TAPE);
        }
    }

    /** One flat panel of vest cloth, front or back. */
    private void panel(String name, Part torso, float x, float w, float z) {
        part(name, torso, 0.0F, 0.0F, 0.0F)
                .box(x, 0.060F, z, w, 0.480F, 0.045F).colour(HIVIS);
    }

    /** One horizontal run of tape across a panel, proud of the cloth. */
    private Part band(String name, Part torso, float x, float w, float z, float y) {
        Part b = part(name, torso, 0.0F, 0.0F, 0.0F);
        // Outward face of the panel, whichever side of him it is on.
        float out = z < 0.0F ? z - 0.016F : z + 0.045F;
        b.box(x - 0.006F, y, out, w + 0.012F, 0.048F, 0.018F).colour(TAPE);
        return b;
    }

    private void belt() {
        Part hips = get("hips");

        part("belt", hips, 0.0F, 0.03F, 0.0F)
                .box(-0.300F, -0.055F, -0.170F, 0.60F, 0.105F, 0.34F).colour(LEATHER);
        part("buckle", hips, 0.0F, 0.03F, 0.170F)
                .box(-0.060F, -0.050F, 0.0F, 0.12F, 0.095F, 0.020F).colour(0x9A8B5E);

        // Two pouches, deliberately different sizes. Matched kit reads as a
        // costume; mismatched kit reads as a man who has been doing this for
        // twenty years and lost things.
        part("pouchR", hips, 0.255F, -0.020F, 0.030F)
                .box(-0.090F, -0.210F, -0.100F, 0.18F, 0.21F, 0.20F).colour(LEATHER);
        part("pouchL", hips, -0.245F, -0.010F, 0.010F)
                .box(-0.075F, -0.165F, -0.085F, 0.15F, 0.17F, 0.17F).colour(LEATHER_DARK);

        // Hangs off the belt and swings on his stride - see kit() in pose().
        Part spanner = part("spanner", hips, 0.300F, -0.020F, -0.075F);
        spanner.box(-0.030F, -0.340F, -0.016F, 0.06F, 0.34F, 0.032F).colour(STEEL);
        part("spannerJaw", spanner, 0.0F, -0.340F, 0.0F)
                .box(-0.055F, -0.070F, -0.020F, 0.11F, 0.07F, 0.04F).colour(STEEL);

        // Inspection lamp. The only warm light on him until the charge starts
        // blinking, and it is what makes the tool belt read as lit equipment.
        Part lamp = part("lamp", hips, -0.300F, -0.010F, -0.060F);
        lamp.box(-0.045F, -0.190F, -0.045F, 0.09F, 0.19F, 0.09F).colour(0x1E2126);
        part("lampLens", lamp, 0.0F, -0.190F, 0.0F)
                .box(-0.040F, -0.045F, -0.040F, 0.08F, 0.045F, 0.08F)
                .colour(1.0F, 0.86F, 0.55F).glow().alpha(0.60F);
    }

    private void boots() {
        boot("R");
        boot("L");
    }

    private void boot(String side) {
        Part foot = get("foot" + side);
        // Built over the humanoid's shoe rather than replacing it, so the ankle
        // still hinges where the rig expects.
        part("boot" + side, foot, 0.0F, 0.0F, 0.0F)
                .box(-0.135F, -0.115F, -0.215F, 0.27F, 0.130F, 0.40F).colour(LEATHER);
        part("sole" + side, foot, 0.0F, 0.0F, 0.0F)
                .box(-0.145F, -0.150F, -0.225F, 0.29F, 0.040F, 0.42F).colour(LEATHER_DARK);
        part("toe" + side, foot, 0.0F, 0.0F, 0.0F)
                .box(-0.125F, -0.110F, 0.100F, 0.25F, 0.115F, 0.09F).colour(0x59452F);

        Part shin = get("knee" + side);
        part("cuff" + side, shin, 0.0F, -0.380F, 0.0F)
                .box(-0.125F, 0.0F, -0.125F, 0.25F, 0.150F, 0.25F).colour(LEATHER);
        // Kneepads, because the plant puts one of these on the ground and a man
        // who kneels for a living owns a pair.
        part("pad" + side, shin, 0.0F, -0.060F, 0.100F)
                .box(-0.105F, -0.150F, 0.0F, 0.21F, 0.150F, 0.055F).colour(PLASTIC);
    }

    /**
     * The drum in his left hand. The wound cable is four crossed slabs rather
     * than a real cylinder: at these distances it reads round, and the drum is
     * scenery hanging off a hand, not the prop the shot is about.
     */
    private Part reel() {
        Part reel = part("reel", get("handL"), 0.0F, -0.20F, 0.06F);

        part("reelArmR", reel, 0.255F, 0.0F, 0.0F)
                .box(-0.022F, 0.0F, -0.022F, 0.044F, 0.30F, 0.044F).colour(STEEL);
        part("reelArmL", reel, -0.255F, 0.0F, 0.0F)
                .box(-0.022F, 0.0F, -0.022F, 0.044F, 0.30F, 0.044F).colour(STEEL);
        part("reelHandle", reel, 0.0F, 0.30F, 0.0F)
                .box(-0.270F, 0.0F, -0.030F, 0.54F, 0.055F, 0.06F).colour(0x3B4148);

        // Everything that turns lives on one node, so the frame it hangs in can
        // stay still while the drum runs.
        Part drum = part("drum", reel, 0.0F, 0.0F, 0.0F);
        part("axle", drum, 0.0F, 0.0F, 0.0F)
                .box(-0.290F, -0.022F, -0.022F, 0.58F, 0.044F, 0.044F).colour(STEEL);
        wheel("discR", drum, 0.225F);
        wheel("discL", drum, -0.225F);

        Part wound = part("wound", drum, 0.0F, 0.0F, 0.0F);
        for (int i = 0; i < 4; i++) {
            Part slab = part("wound" + i, wound, 0.0F, 0.0F, 0.0F);
            slab.rotX = i * 45.0F;
            slab.box(-0.190F, -0.225F, -0.095F, 0.38F, 0.45F, 0.19F)
                    .colour(i % 2 == 0 ? CABLE : CABLE_DARK);
        }
        return reel;
    }

    /** A disc as three crossed slats: six spokes, three boxes, reads as a wheel. */
    private void wheel(String name, Part drum, float x) {
        Part w = part(name, drum, x, 0.0F, 0.0F);
        for (int i = 0; i < 3; i++) {
            Part slat = part(name + i, w, 0.0F, 0.0F, 0.0F);
            slat.rotX = i * 60.0F;
            slat.box(-0.018F, -0.300F, -0.105F, 0.036F, 0.60F, 0.21F).colour(0x39424A);
        }
    }

    /**
     * The slack between the drum and the ground. Chained joints rather than one
     * curved shape, so a single rotation at the top swings the whole loop - and
     * a cable coming off a reel that did not move at all would be a handrail.
     */
    private Part lead(Part reel) {
        Part top = part("lead", reel, 0.0F, -0.150F, -0.140F);
        Part parent = top;
        for (int i = 0; i < 5; i++) {
            Part seg = part("lead" + i, parent, 0.0F, i == 0 ? 0.0F : -0.130F, 0.0F);
            // Each joint tips it further back, so it leaves the drum hanging and
            // arrives at the ground running the way he came.
            seg.rotX = i == 0 ? 12.0F : 21.0F;
            seg.box(-CABLE_R, -0.130F, -CABLE_R, CABLE_R * 2.0F, 0.130F, CABLE_R * 2.0F)
                    .colour(i % 2 == 0 ? CABLE : CABLE_DARK);
            parent = seg;
        }
        top.visible = false;
        return top;
    }

    // ---- the run he leaves behind ------------------------------------------

    /**
     * Build the run of cable trailing back from his bind position, and return
     * this actor so the call chains onto the constructor.
     *
     * <p>Call it at scene assembly, once, before the timeline ever runs: this is
     * geometry, and geometry is constructor work. A second call is ignored
     * rather than doubling the run.
     *
     * <p>The run leaves his feet and goes back along the model's <b>-Z</b>, which
     * is the direction he walked in from, and it is revealed from that far end
     * toward him as {@code unspool} climbs. Bind him where the work ends up, walk
     * him in on {@code dx/dz} the way {@link ManagerActor} is walked in, and key
     * {@code unspool} on the same shape as the walk - the head of the cable then
     * stays under his boots for free.
     *
     * @param length how far the run reaches back, in model units - the humanoid
     *               is 2.4 of them tall, so 40 is about sixteen of his heights
     * @param spans  anchor-to-anchor spans, clamped to a sane ceiling; each one
     *               gets a clamp post and its own dip
     */
    public InternetManagerActor layCable(float length, int spans) {
        if (this.link.length > 0) {
            return this;
        }
        int used = Mth.clamp(spans, 1, MAX_SPANS);
        int count = used * LINKS_PER_SPAN;
        float run = Math.max(0.5F, length);
        float linkLen = run / count;
        // Capped against the post height so the middle of a span never dips
        // below the ground he is standing on.
        float sag = Math.min(run / used * SAG, CLAMP_TOP * 0.75F);

        this.link = new Part[count];
        this.linkPitch = new float[count];
        this.linkYaw = new float[count];
        this.clamp = new Part[used + 1];
        this.clampAt = new float[used + 1];

        for (int i = 0; i < count; i++) {
            // Pivoted on its FAR boundary with the box reaching back toward the
            // near one, so scaling it in Z grows it the way it was laid: away
            // from him is where the cable came out first.
            float xNear = wander(i);
            float xFar = wander(i + 1);
            float yNear = height(i, sag);
            float yFar = height(i + 1, sag);
            float dx = xNear - xFar;
            float dy = yNear - yFar;
            float span = (float) Math.sqrt(dx * dx + dy * dy + linkLen * linkLen);

            Part seg = part("link" + i, this.ground, xFar, yFar, -(i + 1) * linkLen);
            // rotX is applied innermost and rotY outside it, so this pair aims
            // the box exactly at the next boundary: pitch it, then swing it.
            this.linkPitch[i] = (float) Math.toDegrees(Math.asin(-dy / span));
            this.linkYaw[i] = (float) Math.toDegrees(Math.atan2(dx, linkLen));
            seg.rotX = this.linkPitch[i];
            seg.rotY = this.linkYaw[i];
            // Length, not linkLen, or every joint would show daylight.
            seg.box(-CABLE_R, -CABLE_R, 0.0F, CABLE_R * 2.0F, CABLE_R * 2.0F, span);
            // A black sleeve where the run crosses a clamp: cable is never one
            // unbroken colour over a hundred blocks, and the sleeves are what
            // let the eye count the spans.
            seg.colour(i % LINKS_PER_SPAN == 0 ? SLEEVE
                    : (i % 2 == 0 ? CABLE : CABLE_DARK));
            seg.visible = false;
            this.link[i] = seg;
        }

        for (int j = 0; j <= used; j++) {
            int at = j * LINKS_PER_SPAN;
            Part post = part("clamp" + j, this.ground,
                    wander(at), 0.0F, -at * linkLen);
            post.box(-0.070F, 0.0F, -0.070F, 0.14F, CLAMP_TOP, 0.14F).colour(0x30343A);
            part("clampCap" + j, post, 0.0F, CLAMP_TOP, 0.0F)
                    .box(-0.100F, -0.030F, -0.100F, 0.20F, 0.060F, 0.20F).colour(STEEL);
            post.visible = false;
            this.clamp[j] = post;
            this.clampAt[j] = at;
        }
        return this;
    }

    /**
     * Height of a boundary between links. Flat at the clamps and dipping in
     * between on a parabola - close enough to a catenary that nobody watching a
     * giant string cable across the sky will be checking.
     */
    private static float height(int boundary, float sag) {
        float u = (boundary % LINKS_PER_SPAN) / (float) LINKS_PER_SPAN;
        return CLAMP_TOP - sag * 4.0F * u * (1.0F - u);
    }

    /** Deterministic drift off the centre line, so the run is not a ruled line. */
    private static float wander(int boundary) {
        return WANDER * Mth.sin(boundary * 0.83F);
    }

    // ---- animation ---------------------------------------------------------

    @Override
    protected void pose(float time) {
        super.pose(time);

        float plant = Mth.clamp(track("plant", time, 0.0F), 0.0F, 1.0F);
        if (plant > 0.002F) {
            plantPose(plant);
        }
        float point = Mth.clamp(track("point", time, 0.0F), 0.0F, 1.0F);
        if (point > 0.002F) {
            pointPose(point, time);
        }

        kit(time);

        float unspool = Mth.clamp(track("unspool", time, 0.0F), 0.0F, 1.0F);
        reelSpin(unspool);
        pinGround(time);
        cable(unspool, time);
        charge(plant, point, time);
    }

    /**
     * Down onto one knee. A symmetrical squat is a gym pose; one knee planted
     * with the other foot forward is a man working on the ground, and it is the
     * only version of this that reads as a silhouette from below.
     */
    private void plantPose(float p) {
        float u = Easing.CUBIC_IN_OUT.apply(p);
        Part hips = get("hips");
        // Added to whatever the walk left behind, so a scene can crouch him out
        // of a stride without the two fighting.
        hips.offY -= 0.66F * u;
        hips.rotX += 15.0F * u;
        get("torso").rotX += 13.0F * u;

        get("legR").rotX = Easing.lerp(get("legR").rotX, 24.0F, u);
        get("kneeR").rotX = -Math.abs(Easing.lerp(get("kneeR").rotX, 118.0F, u));
        get("legL").rotX = Easing.lerp(get("legL").rotX, -54.0F, u);
        get("kneeL").rotX = -Math.abs(Easing.lerp(get("kneeL").rotX, 78.0F, u));

        get("armR").rotX = Easing.lerp(get("armR").rotX, -46.0F, u);
        get("elbowR").rotX = -Math.abs(Easing.lerp(get("elbowR").rotX, 58.0F, u));
        // The reel hand goes out for balance rather than staying tucked in - a
        // man with a drum in one hand does not crouch symmetrically.
        get("armL").rotX = Easing.lerp(get("armL").rotX, -16.0F, u);
        get("armL").rotZ = Easing.lerp(get("armL").rotZ, 17.0F, u);
        get("head").rotX += 26.0F * u;
    }

    /**
     * The gesture at his own work, aimed forward and down. BACK_OUT because a
     * point is a flick that overshoots and settles, never a glide - and the arm
     * goes nearly straight, since a bent arm pointing at something is a smudge
     * on the torso at this distance.
     */
    private void pointPose(float p, float time) {
        float u = Easing.BACK_OUT.apply(p);
        Part armR = get("armR");
        // A held arm that is perfectly still is a mannequin's; a degree of drift
        // is all it takes to keep it alive.
        armR.rotX = Easing.lerp(armR.rotX, -58.0F + 1.3F * Mth.sin(time * 0.9F), u);
        armR.rotZ = Easing.lerp(armR.rotZ, -13.0F, u);
        get("elbowR").rotX = -Math.abs(Easing.lerp(get("elbowR").rotX, 3.0F, u));

        get("torso").rotX += 7.0F * u;
        get("torso").rotY += 11.0F * u;
        // He looks where he is pointing, a beat behind the arm.
        get("head").rotX += 24.0F * Easing.CUBIC_OUT.apply(p);
        get("head").rotY -= 9.0F * u;
    }

    /** Hat, tools and tape: everything on him that answers to something else. */
    private void kit(float time) {
        // The hat is loose on his head, so it answers to how far the head moved
        // over the last few ticks. Same cheap spring the hair uses.
        float now = track("headPitch", time, 0.0F);
        float then = track("headPitch", time - 3.0F, 0.0F);
        this.hat.rotX = -(now - then) * 0.55F;
        this.hat.rotZ = 1.3F * Mth.sin(time * 0.07F);

        // Phase-locked to the same stride the walk cycle runs on, so the spanner
        // swings with his step instead of on a clock of its own.
        float walk = track("walk", time, 0.0F);
        float phase = time * walk * 0.16F;
        float swing = walk > 0.0F ? Mth.sin(phase + 1.1F) * 11.0F : 0.0F;
        this.spanner.rotX = swing + 2.0F * Mth.sin(time * 0.05F);
        this.spanner.rotZ = -swing * 0.35F;

        // Tape does not emit, it returns what hits it, so it breathes rather
        // than pulses. Enough to find him against a dark sky, not enough to
        // become a light source.
        // Kept under 1.0 at the peak on purpose: the vertex format packs colour
        // into a byte, so a channel driven past white wraps round to black.
        float sheen = 0.12F + 0.05F * Mth.sin(time * 0.07F);
        for (int i = 0; i < this.tape.length; i++) {
            this.tape[i].colour(0.80F + sheen, 0.82F + sheen, 0.81F + sheen);
        }
    }

    /**
     * The drum turns exactly as much as has come off it. Driving it off time
     * instead would make it a fan bolted to his hand.
     */
    private void reelSpin(float unspool) {
        this.reelDrum.rotX = -unspool * 2160.0F;
        // And it gets visibly emptier. A reel that pays out a hundred blocks of
        // cable and stays fat is the tell that none of it came from there.
        float left = 1.0F - 0.45F * unspool;
        this.reelWound.scale(1.0F, left, left);
        this.lead.visible = unspool > 0.004F;
    }

    /**
     * Undo his own travel and yaw on the node the run hangs from, so laid cable
     * stays in the world while he walks out of it.
     *
     * <p>Rendering does {@code world = pos + R(yaw) * scale * model}, so putting
     * the inverse of that on this one joint pins its children to the bind point.
     * The inverse rotation is the same one {@link ThrowerActor} uses to place its
     * afterimages and {@link BluntActor#tip} uses to find its coal.
     */
    private void pinGround(float time) {
        float inv = 1.0F / Math.max(0.001F, this.scale * this.size);
        float rad = this.yaw * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        float wx = track("dx", time, 0.0F);
        float wy = track("dy", time, 0.0F);
        float wz = track("dz", time, 0.0F);
        this.ground.pivotX = -(wx * cos - wz * sin) * inv;
        this.ground.pivotY = -wy * inv;
        this.ground.pivotZ = -(wx * sin + wz * cos) * inv;
        this.ground.rotY = this.baseYaw - this.yaw;
    }

    /**
     * Reveal the run from its far end toward him. Hiding a link outright rather
     * than fading it is deliberate: cable is either out of the drum or it is
     * not, and a translucent rope is a rope nobody believes.
     */
    private void cable(float unspool, float time) {
        if (this.link.length == 0) {
            return;
        }
        float head = (1.0F - unspool) * this.link.length;
        for (int i = 0; i < this.link.length; i++) {
            Part seg = this.link[i];
            float out = i - head + 1.0F;
            if (out <= 0.004F) {
                seg.visible = false;
                continue;
            }
            seg.visible = true;
            // The link at the head grows out of the drum over its own length,
            // so cable arrives continuously instead of a box at a time.
            seg.scaleZ = Math.min(1.0F, out);

            // Cable that has just hit the ground is still settling; cable that
            // was laid twenty blocks ago has stopped. Anything that kept moving
            // the whole way back would read as a snake.
            float fresh = Mth.clamp(1.0F - (i - head) * 0.22F, 0.0F, 1.0F);
            if (fresh > 0.004F) {
                seg.rotX = this.linkPitch[i] + Mth.sin(time * 0.42F + i * 0.9F) * 5.0F * fresh;
                seg.rotY = this.linkYaw[i] + Mth.cos(time * 0.35F + i * 1.3F) * 4.0F * fresh;
            } else {
                seg.rotX = this.linkPitch[i];
                seg.rotY = this.linkYaw[i];
            }
        }
        for (int j = 0; j < this.clamp.length; j++) {
            // A post is driven in before the cable reaches it, so it appears with
            // the boundary it belongs to rather than after it.
            this.clamp[j].visible = this.clampAt[j] >= head;
        }
    }

    /**
     * Where the charge is and what its lamp is doing. The lamp is the countdown:
     * it is idle while he works, and it goes frantic under the pointing hand,
     * which is the only warning the shot ever gives.
     */
    private void charge(float plant, float point, float time) {
        float authored = track("charge", time, Float.NaN);
        boolean set = Float.isNaN(authored) ? plant >= SET_AT : authored > 0.5F;
        this.chargeSet.visible = set;
        // He takes it off the belt on the way down and it is gone on the way up.
        this.chargeHeld.visible = !set && plant > 0.05F;
        if (!set) {
            return;
        }
        float rate = 0.30F + 2.5F * point;
        float beat = Mth.sin(time * rate);
        // Squared, so it snaps on and dies away instead of fading both ways -
        // an indicator lamp is a switch, not a dimmer.
        this.chargeLamp.alpha = 0.16F + 0.84F * beat * beat;
        this.chargeLamp.colour(1.0F, 0.08F + 0.22F * point, 0.05F);
    }

    // ---- where the work is -------------------------------------------------

    /**
     * World point of the set charge at this scene time - what a cue should hang
     * the detonation, the sound and the shockwave on.
     *
     * <p>Rebuilt from the tracks rather than read off the last frame, the same
     * way {@link BluntActor#tip} does it, so a cue firing on a whole tick gets
     * the answer for that tick. The charge is pinned to the bind position and
     * the bind yaw, because that is the point the ground node is nailed to -
     * bind him where the work ends up.
     */
    public Vec3 chargePoint(float time) {
        float sc = Math.max(0.001F, track("scale", time, this.baseScale)) * this.size;
        float rad = this.baseYaw * Mth.DEG_TO_RAD;
        double x = CHARGE_X * sc;
        double z = CHARGE_Z * sc;
        float sy = Mth.sin(rad);
        float cy = Mth.cos(rad);
        return this.basePos.add(x * cy + z * sy, CHARGE_Y * sc, -x * sy + z * cy);
    }
}
