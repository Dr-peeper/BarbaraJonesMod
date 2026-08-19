package com.barbarajones.cinematic.actor;

import com.barbarajones.cinematic.Easing;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * THE MANAGER'S CABLE: a run of internet cable strung across the sky, with
 * bundles of charge taped to it every few links, laid in one beat and fired in
 * the next.
 *
 * <p>Serves the Internet Manager's demolition beat. He walks a line of cable out
 * over the death site the way an engineer walks a line of cable out over a
 * bridge, and the horror is entirely in the fact that a piece of household
 * infrastructure is being used as a fuse. That read only survives if the object
 * is unmistakably a <em>cable</em> - a blue jacket with a pale stripe up the top,
 * sagging under its own weight, not a rope and not a wire - and the charges are
 * unmistakably <em>taped on afterwards</em>, which is why they are separate lumps
 * of brick and black tape rather than anything moulded into the run.
 *
 * <p>Two structural decisions carry the whole prop:
 *
 * <ul>
 *   <li><b>The links are siblings, not a chain.</b> Hiding a joint in a chain
 *       hides everything above it, which is exactly wrong here: the detonation
 *       destroys the run from one end while the far end is still hanging intact.
 *       Every link is a child of the run group with its own absolute position, so
 *       any link can be destroyed without touching its neighbours.</li>
 *   <li><b>Both ends of every link come off the same curve function.</b> A link's
 *       pivot is {@code heightAt(u)} and its rotation is the chord to
 *       {@code heightAt(u + du)}, so link {@code i} ends precisely where link
 *       {@code i + 1} begins no matter how violently the run is whipping. That is
 *       the one thing a row of separate boxes can get visibly wrong, and it is
 *       what lets the shock wave ahead of the blast be as violent as it likes.</li>
 * </ul>
 *
 * <p>Tracks:
 * <ul>
 *   <li>{@code arm} 0..1 - charges appear along the run, in order, and their
 *       indicators come alive. Progressive rather than all at once, because he
 *       tapes them on as he walks.</li>
 *   <li>{@code blink} 0..1 - urgency, not phase. It is integrated into a flash
 *       RATE, so winding this track up accelerates the blinking rather than
 *       moving it; that acceleration is the tension in the beat.</li>
 *   <li>{@code fire} 0..1 - the detonation front, travelling from the bind
 *       position toward the {@link #run} end point. Charges blow in sequence as
 *       it reaches them.</li>
 *   <li>{@code sag} - multiplier on how far the run droops. 1 is a cable hung
 *       between two points; 0 pulls it taut.</li>
 * </ul>
 *
 * <p>An undriven cable renders as a whole, armed, slowly blinking run: an actor
 * that needs four tracks authored before it draws anything is an actor that gets
 * left out of a shot.
 */
public final class C4CableActor extends CinematicActor {

    /** Geometry is built once for the longest run we will ever lay. */
    private static final int MAX_SEGMENTS = 40;
    private static final int MAX_BUNDLES = 10;
    private static final int DEFAULT_SEGMENTS = 24;

    /** One bundle of charge per this many links. */
    private static final int SEGMENTS_PER_BUNDLE = 4;

    /** Default run length, in cable radii, for a cable nobody called {@link #run} on. */
    private static final float DEFAULT_SPAN = 120.0F;

    /** Droop at the belly as a fraction of the run's straight-line length. */
    private static final float SAG_FRACTION = 0.11F;

    // The detonation, all measured in link widths so the look of it does not
    // change when a scene lays a longer or shorter run.
    private static final float FLASH_LEAD = 2.6F;   // light spilling ahead of the front
    private static final float BLAST_TAIL = 3.2F;   // flash lingering behind it
    private static final float SHOCK_LEAD = 6.0F;   // whip running ahead of the front
    private static final float DEBRIS_HOLD = 1.4F;  // links stay solid this long after blowing
    private static final float DEBRIS_LIFE = 5.0F;  // and are gone by here
    private static final float BUNDLE_LIFE = 6.0F;
    private static final float KICK = 5.0F;         // debris throw, in cable radii

    // Flashes per tick at rest and at full urgency. The idle rate has to be slow
    // enough to read as "armed and waiting" rather than as a fault light.
    private static final float BLINK_SLOW = 0.030F;
    private static final float BLINK_FAST = 0.78F;
    private static final float BLINK_DUTY = 0.30F;

    /**
     * Phase offset between one indicator and the next. Small on purpose: the
     * blink ripples down the run toward the far end, which quietly tells the eye
     * which way the detonation is going to travel before it travels.
     */
    private static final float BUNDLE_STAGGER = 0.055F;

    private static final int JACKET = 0x24509C;
    private static final int STRIPE = 0xD9DEE6;
    private static final int TAPE = 0x17171A;
    private static final int BRICK = 0xC0AC82;
    private static final int HOUSING = 0x2A2C31;
    private static final int LAMP = 0xFF2A18;

    private static final float CHAR_R = 0.085F;
    private static final float CHAR_G = 0.075F;
    private static final float CHAR_B = 0.070F;

    private final float radius;

    /** Everything hangs off this; its yaw is the run's bearing. */
    private final Part line;

    // Flat arrays rather than get("seg" + i): pose() touches every link and every
    // charge on every frame, and building those names would allocate a string per
    // part per frame.
    private final Part[] seg = new Part[MAX_SEGMENTS];
    private final Part[] stripe = new Part[MAX_SEGMENTS];
    private final Part[] flash = new Part[MAX_SEGMENTS];
    private final float[] jacketR = new float[MAX_SEGMENTS];
    private final float[] jacketG = new float[MAX_SEGMENTS];
    private final float[] jacketB = new float[MAX_SEGMENTS];
    private final float[] side = new float[MAX_SEGMENTS];
    private final float[] spin = new float[MAX_SEGMENTS];

    private final Part[] bundle = new Part[MAX_BUNDLES];
    private final Part[][] brick = new Part[MAX_BUNDLES][];
    private final Part[][] wrap = new Part[MAX_BUNDLES][];
    private final Part[] lamp = new Part[MAX_BUNDLES];
    private final Part[] lampHalo = new Part[MAX_BUNDLES];
    private final Part[] flare = new Part[MAX_BUNDLES];
    private final float[] bundleU = new float[MAX_BUNDLES];

    private int segments = DEFAULT_SEGMENTS;
    private int bundles = DEFAULT_SEGMENTS / SEGMENTS_PER_BUNDLE;
    private float span;
    private float rise;
    private float sagDepth;

    // The blink is integrated rather than sampled, because the track is a RATE.
    // Multiplying time by a changing rate would jerk the phase every time the
    // rate moved, which on a blinking light reads as a broken bulb.
    private float blinkPhase;
    private float blinkClock = Float.NaN;

    /**
     * @param size multiplier on the cable's own dimensions - jacket thickness,
     *             charge bulk, indicator size. The run's LENGTH is not a size:
     *             it comes from {@link #run}, so a scene lays cable in world
     *             terms and uses this only to say how heavy a cable it is. At 1.0
     *             the jacket is 0.8 units across, so a run bound at scale 1 reads
     *             as about a block of cable with charges the size of a car on it.
     */
    public C4CableActor(float size) {
        float k = Math.max(0.05F, size);
        this.radius = 0.40F * k;
        float r = this.radius;

        this.line = part("run", this.root, 0.0F, 0.0F, 0.0F);

        for (int i = 0; i < MAX_SEGMENTS; i++) {
            Part s = part("seg" + i, this.line, 0.0F, 0.0F, 0.0F);
            // Authored exactly ONE unit long. The run's length is unknown until
            // run() is called, so pose() stretches each link onto its own chord
            // with a single scaleX - and because the box only has extent along
            // X, that stretch changes the length and nothing else.
            s.box(0.0F, -r, -r, 1.0F, r * 2.0F, r * 2.0F);

            // Deterministic shading per link. One flat blue over forty boxes is
            // the tell that says "extruded plastic"; a run of cable has been
            // dragged along the ground and does not read as one colour twice.
            float shade = 0.88F + ((i * 29) % 9) / 9.0F * 0.26F;
            this.jacketR[i] = Math.min(1.0F, red(JACKET) * shade);
            this.jacketG[i] = Math.min(1.0F, green(JACKET) * shade);
            this.jacketB[i] = Math.min(1.0F, blue(JACKET) * shade);
            s.colour(this.jacketR[i], this.jacketG[i], this.jacketB[i]);

            // The stripe is the single detail that says "network cable" instead
            // of "hose". Proud of the top face rather than flush with it, so it
            // catches the face shading instead of z-fighting the jacket.
            Part st = part("stripe" + i, s, 0.0F, 0.0F, 0.0F);
            st.box(0.0F, r * 0.96F, -r * 0.42F, 1.0F, r * 0.14F, r * 0.84F).colour(STRIPE);
            this.stripe[i] = st;

            // Additive shell around the link. Recolouring the jacket alone never
            // looks like a detonation, it looks like a repaint; the bloom around
            // the outside is the part the eye reads as light.
            Part fl = part("flash" + i, s, 0.0F, 0.0F, 0.0F);
            fl.box(-0.03F, -r * 2.3F, -r * 2.3F, 1.06F, r * 4.6F, r * 4.6F)
                    .colour(1.0F, 0.45F, 0.10F).glow().alpha(0.0F);
            this.flash[i] = fl;

            // Fixed scatter directions. A random field would make the same
            // detonation land differently every time it played, and the shot is
            // cut to this.
            this.side[i] = ((i * 37) % 11) / 5.0F - 1.0F;
            this.spin[i] = ((i * 53) % 17) / 8.0F - 1.0F;

            this.seg[i] = s;
        }

        float brickLen = r * 3.4F;
        for (int b = 0; b < MAX_BUNDLES; b++) {
            Part g = part("bundle" + b, this.line, 0.0F, 0.0F, 0.0F);
            g.visible = false;

            Part[] bricks = new Part[3];
            for (int c = 0; c < 3; c++) {
                // Three bricks spaced round the jacket, the first one hanging
                // straight under it. A bundle strapped on one side would vanish
                // into the cable's own silhouette from half the angles the
                // camera flies through; gripping it from three sides keeps a
                // lump on the line whichever way you come at it.
                double a = Math.PI + c * Math.PI * 2.0D / 3.0D;
                Part br = part("chg" + b + "_" + c, g, 0.0F,
                        (float) Math.cos(a) * r * 1.55F, (float) Math.sin(a) * r * 1.55F);
                // Turns the brick's local +Y to point straight out from the
                // cable, which is what lets the detonation scatter it radially
                // later with nothing but an offset.
                br.rotX = (float) Math.toDegrees(a);
                br.box(-brickLen * 0.5F, -r * 0.62F, -r * 0.62F,
                        brickLen, r * 1.24F, r * 1.24F).colour(BRICK);
                bricks[c] = br;
            }

            Part[] wraps = new Part[3];
            for (int w = 0; w < 2; w++) {
                // Two bands of tape, slightly proud of the bricks, because tape
                // goes on OVER the charge. This is the detail that says somebody
                // put these here by hand.
                Part tape = part("tape" + b + "_" + w, g,
                        (w == 0 ? -1.0F : 1.0F) * brickLen * 0.34F, 0.0F, 0.0F);
                tape.box(-r * 0.16F, -r * 2.28F, -r * 2.28F,
                        r * 0.32F, r * 4.56F, r * 4.56F).colour(TAPE);
                wraps[w] = tape;
            }

            Part housing = part("det" + b, g, 0.0F, r * 2.05F, 0.0F);
            housing.box(-r * 0.60F, 0.0F, -r * 0.60F, r * 1.20F, r * 0.95F, r * 1.20F)
                    .colour(HOUSING);
            wraps[2] = housing;

            // The indicator sits on top as a cube rather than a lens on one face,
            // so it reads from the ground, from the side and from above - these
            // things are seen from sixty blocks below and from any angle.
            Part led = part("led" + b, housing, 0.0F, r * 0.95F, 0.0F);
            led.box(-r * 0.40F, 0.0F, -r * 0.40F, r * 0.80F, r * 0.62F, r * 0.80F)
                    .colour(LAMP).glow();
            // A small light at this distance is one pixel. The halo is what
            // actually carries the blink across the sky.
            Part halo = part("ledGlow" + b, led, 0.0F, r * 0.31F, 0.0F);
            halo.box(-r * 1.05F, -r * 1.05F, -r * 1.05F, r * 2.10F, r * 2.10F, r * 2.10F)
                    .colour(1.0F, 0.26F, 0.12F).glow().alpha(0.0F);

            Part burst = part("flare" + b, g, 0.0F, 0.0F, 0.0F);
            burst.box(-brickLen * 0.95F, -r * 3.8F, -r * 3.8F,
                    brickLen * 1.90F, r * 7.6F, r * 7.6F)
                    .colour(1.0F, 0.62F, 0.22F).glow().alpha(0.0F);

            this.bundle[b] = g;
            this.brick[b] = bricks;
            this.wrap[b] = wraps;
            this.lamp[b] = led;
            this.lampHalo[b] = halo;
            this.flare[b] = burst;
        }

        // A default run, so an actor bound and never laid still draws a cable.
        this.span = r * DEFAULT_SPAN;
        this.rise = 0.0F;
        this.sagDepth = SAG_FRACTION * this.span;
        layout();
    }

    // ---- laying the run ----------------------------------------------------

    /**
     * Lay this run: {@code segments} links from wherever the actor was bound out
     * to {@code worldEnd}, which is how a scene strings cable between two
     * positions.
     *
     * <p>Call it AFTER {@link CinematicActor#bind}. The actor's bind position is
     * the run's near end and the end point is given in world coordinates, so the
     * bind transform has to be un-applied to get the run into model space - which
     * cannot happen before there is a bind transform to un-apply.
     *
     * <p>The detonation front always travels from the bind position toward this
     * end point. A scene that wants it to blow the other way lays the cable the
     * other way round.
     */
    public C4CableActor run(int segments, Vec3 worldEnd) {
        this.segments = Mth.clamp(segments, 2, MAX_SEGMENTS);

        // The inverse of the yaw-and-scale that CinematicActor.render applies on
        // the way out, so the last link lands on the world point it was given.
        float rad = this.baseYaw * Mth.DEG_TO_RAD;
        float cy = Mth.cos(rad);
        float sy = Mth.sin(rad);
        float inv = 1.0F / Math.max(0.001F, this.baseScale);
        double wx = worldEnd.x - this.basePos.x;
        double wy = worldEnd.y - this.basePos.y;
        double wz = worldEnd.z - this.basePos.z;
        float lx = (float) (wx * cy - wz * sy) * inv;
        float ly = (float) wy * inv;
        float lz = (float) (wx * sy + wz * cy) * inv;

        // The run group carries the bearing, so everything below it is authored
        // in one flat plane along +X and no link ever needs trigonometry of its
        // own to know which way the cable is pointing.
        this.span = Math.max(0.001F, (float) Math.sqrt(lx * lx + lz * lz));
        this.rise = ly;
        this.line.rotY = (float) Math.toDegrees(Math.atan2(-lz, lx));
        this.sagDepth = SAG_FRACTION * (float) Math.sqrt(this.span * this.span + ly * ly);

        layout();
        return this;
    }

    /** Switch on exactly the links and bundles this run uses, and space them. */
    private void layout() {
        this.bundles = Mth.clamp(this.segments / SEGMENTS_PER_BUNDLE, 1, MAX_BUNDLES);
        for (int i = 0; i < MAX_SEGMENTS; i++) {
            this.seg[i].visible = i < this.segments;
        }
        for (int b = 0; b < MAX_BUNDLES; b++) {
            // Half a gap in from each end: a charge sitting exactly on the last
            // link looks like it fell off rather than like it was placed.
            this.bundleU[b] = (b + 0.5F) / this.bundles;
            this.bundle[b].visible = b < this.bundles;
        }
    }

    // ---- animation ---------------------------------------------------------

    @Override
    protected void pose(float time) {
        float sag = track("sag", time, 1.0F);
        float arm = Mth.clamp(track("arm", time, 1.0F), 0.0F, 1.0F);
        float fire = Mth.clamp(track("fire", time, 0.0F), 0.0F, 1.0F);

        // Below zero means "no detonation authored", which is NOT the same as a
        // front resting on the first charge: fire = 0 has to leave the run whole.
        float front = fire <= 0.0001F ? -1.0F : fire;
        advanceBlink(time, Mth.clamp(track("blink", time, 0.0F), 0.0F, 1.0F));

        cable(time, sag, front);
        charges(time, sag, front, arm);
    }

    /**
     * Hang the links on the curve, then destroy the ones the front has passed.
     *
     * <p>Every link reads its own ends off {@link #heightAt} and {@link #driftAt},
     * so the run is a connected polyline by construction. Only a link the blast
     * has already taken is allowed to leave that curve - coming apart is the
     * whole point of it.
     */
    private void cable(float time, float sag, float front) {
        float du = 1.0F / this.segments;
        float head = front * this.segments;

        for (int i = 0; i < this.segments; i++) {
            Part s = this.seg[i];
            // How many link widths ago the detonation front went past here.
            float age = front < 0.0F ? -999.0F : head - (i + 0.5F);
            if (age >= DEBRIS_LIFE) {
                s.visible = false;
                continue;
            }
            s.visible = true;

            float u = i * du;
            float x0 = u * this.span;
            float y0 = heightAt(u, sag, time, front);
            float z0 = driftAt(u, time);
            float dx = du * this.span;
            float dy = heightAt(u + du, sag, time, front) - y0;
            float dz = driftAt(u + du, time) - z0;

            // rotZ is applied outside rotY, so rotZ is the climb in the run's
            // vertical plane and rotY is the swing out of it. Solving them in
            // that order lands the link's far end exactly on the next link's
            // pivot, and scaleX is then just the chord between the two.
            float flat = (float) Math.sqrt(dx * dx + dy * dy);
            s.pivotX = x0;
            s.pivotY = y0;
            s.pivotZ = z0;
            s.rotZ = (float) Math.toDegrees(Math.atan2(dy, dx));
            s.rotY = (float) Math.toDegrees(Math.atan2(-dz, flat));
            s.scaleX = (float) Math.sqrt(flat * flat + dz * dz);

            Part st = this.stripe[i];
            if (age >= 0.0F) {
                // Thrown clear, tumbling, cooling. Up first and then falling,
                // because a chunk of blown cable is a thrown object and not a
                // spark that rises.
                float kick = KICK * this.radius;
                s.pivotY = y0 + kick * (age - age * age * 0.40F);
                s.pivotZ = z0 + kick * this.side[i] * age * 0.85F;
                s.rotY += this.spin[i] * age * 34.0F;
                s.rotZ += this.side[i] * age * 27.0F;

                float heat = Mth.clamp(1.0F - age / 1.15F, 0.0F, 1.0F);
                float fade = 1.0F - Mth.clamp(
                        (age - DEBRIS_HOLD) / (DEBRIS_LIFE - DEBRIS_HOLD), 0.0F, 1.0F);
                s.colour(ramp(CHAR_R, 1.0F, heat), ramp(CHAR_G, 0.88F, heat),
                        ramp(CHAR_B, 0.58F, heat));
                s.alpha = fade;
                st.colour(ramp(CHAR_R, 1.0F, heat), ramp(CHAR_G, 0.93F, heat),
                        ramp(CHAR_B, 0.74F, heat));
                st.alpha = fade;
            } else {
                // Still whole, but washed out by the light coming up the line.
                // Cable that stays flat blue right up against a detonation tells
                // the eye the detonation is a decal stuck on top of it.
                float wash = age > -FLASH_LEAD ? 1.0F + age / FLASH_LEAD : 0.0F;
                wash *= wash * 0.85F;
                s.colour(ramp(this.jacketR[i], 1.0F, wash),
                        ramp(this.jacketG[i], 0.82F, wash),
                        ramp(this.jacketB[i], 0.52F, wash));
                s.alpha = 1.0F;
                st.colour(ramp(red(STRIPE), 1.0F, wash), ramp(green(STRIPE), 0.90F, wash),
                        ramp(blue(STRIPE), 0.66F, wash));
                st.alpha = 1.0F;
            }

            Part fl = this.flash[i];
            float glow = age >= 0.0F
                    ? Mth.clamp(1.0F - age / BLAST_TAIL, 0.0F, 1.0F)
                    : Mth.clamp(1.0F + age / FLASH_LEAD, 0.0F, 1.0F) * 0.60F;
            glow *= glow;
            if (glow <= 0.004F) {
                fl.alpha = 0.0F;
            } else {
                // A detonation front is a lot of small failures, not one lamp
                // being turned up, so every link boils on its own phase.
                float boil = 0.82F + 0.18F * Mth.sin(time * 2.3F + i * 1.7F);
                fl.alpha = Mth.clamp(glow * 0.85F * boil, 0.0F, 1.0F);
                fl.colour(1.0F, ramp(0.34F, 0.95F, glow), ramp(0.06F, 0.72F, glow * glow));
                float bloom = 1.0F + glow * 1.6F;
                fl.scale(1.0F, bloom, bloom);
            }
        }
    }

    /**
     * The charges: taped on in order as {@code arm} runs up, blinking harder and
     * harder, then blown one after another as the front reaches each of them.
     */
    private void charges(float time, float sag, float front, float arm) {
        float half = 0.5F / this.segments;

        for (int b = 0; b < this.bundles; b++) {
            Part g = this.bundle[b];
            float u = this.bundleU[b];
            float appear = Mth.clamp(arm * this.bundles - b, 0.0F, 1.0F);
            float age = front < 0.0F ? -999.0F : (front - u) * this.segments;
            if (appear <= 0.02F || age >= BUNDLE_LIFE) {
                g.visible = false;
                continue;
            }
            g.visible = true;

            // Sits on the cable and leans with it, sampled either side of its own
            // position so it stays glued to the run however the run is moving.
            float dx = 2.0F * half * this.span;
            float dy = heightAt(u + half, sag, time, front) - heightAt(u - half, sag, time, front);
            float dz = driftAt(u + half, time) - driftAt(u - half, time);
            g.pivotX = u * this.span;
            g.pivotY = heightAt(u, sag, time, front);
            g.pivotZ = driftAt(u, time);
            g.rotZ = (float) Math.toDegrees(Math.atan2(dy, dx));
            g.rotY = (float) Math.toDegrees(Math.atan2(-dz, (float) Math.sqrt(dx * dx + dy * dy)));
            // BACK_OUT so it arrives with a snatch - it is being pressed onto the
            // cable by something enormous, not fading politely into existence.
            float pop = Easing.BACK_OUT.apply(appear);
            g.scale(pop, pop, pop);

            float lit;
            if (age >= 0.0F) {
                // The indicator goes to a dead short and then goes with the rest.
                lit = Mth.clamp(1.0F - age / 0.9F, 0.0F, 1.0F);
            } else {
                float ph = frac(this.blinkPhase + b * BUNDLE_STAGGER);
                // Hard on, quick decay, then a faint standby ember: between
                // flashes the run still has to read as a line of dots, or the
                // charges disappear from the shot half the time.
                lit = (ph < BLINK_DUTY ? 1.0F - (ph / BLINK_DUTY) * 0.65F : 0.07F) * appear;
            }

            blast(b, age, lit);
        }
    }

    /** One bundle's state: scatter, tape, indicator and the fireball. */
    private void blast(int b, float age, float lit) {
        boolean blown = age >= 0.0F;
        float scatter = blown ? age : 0.0F;
        float solid = blown
                ? 1.0F - Mth.clamp(age / (BUNDLE_LIFE * 0.55F), 0.0F, 1.0F)
                : 1.0F;

        Part[] bricks = this.brick[b];
        for (int c = 0; c < bricks.length; c++) {
            Part br = bricks[c];
            // Each brick's local +Y points out of the cable, so one offset flings
            // it radially away from the run without any per-frame trigonometry.
            br.offY = this.radius * 4.5F * scatter;
            br.offX = this.radius * 1.3F * (c - 1) * scatter;
            br.alpha = solid;
        }

        Part[] wraps = this.wrap[b];
        for (int w = 0; w < wraps.length; w++) {
            // The tape is the first thing to go - it is what was holding it on.
            wraps[w].alpha = blown ? solid * Math.max(0.0F, 1.0F - age * 0.8F) : 1.0F;
        }

        Part led = this.lamp[b];
        float hot = lit * lit;
        led.colour(1.0F, ramp(0.12F, 0.88F, hot), ramp(0.06F, 0.74F, hot * lit));
        led.alpha = Mth.clamp(0.12F + lit, 0.0F, 1.0F);
        Part halo = this.lampHalo[b];
        halo.alpha = lit * 0.40F;
        float swell = 1.0F + lit * 1.4F;
        halo.scale(swell, swell, swell);

        Part burst = this.flare[b];
        // A shade of lead-in, so the charge lights the instant BEFORE it goes
        // rather than on the same frame - that gap is what makes it a detonation
        // instead of a light switch.
        float fire = age >= -0.35F && age < BUNDLE_LIFE
                ? Mth.clamp(1.0F - age / 2.4F, 0.0F, 1.0F)
                : 0.0F;
        fire *= fire;
        if (fire <= 0.004F) {
            burst.alpha = 0.0F;
        } else {
            burst.alpha = Mth.clamp(fire * 0.90F, 0.0F, 1.0F);
            burst.colour(1.0F, ramp(0.38F, 0.96F, fire), ramp(0.10F, 0.78F, fire * fire));
            float bloom = 1.0F + fire * 2.2F;
            burst.scale(bloom, bloom, bloom);
        }
    }

    // ---- the curve the whole run hangs on -----------------------------------

    /**
     * Height of the run at {@code u} along it, in model units.
     *
     * <p>Everything that moves the cable vertically lives in here rather than
     * being added to a link afterwards. A displacement applied per link would
     * move that link's pivot without moving its neighbour's far end, and the run
     * would come apart into a dashed line the moment anything shook it.
     */
    private float heightAt(float u, float sag, float time, float front) {
        // Zero at both ends: a run is pinned where it was laid, and only the
        // belly of it has any freedom.
        float hang = 4.0F * u * (1.0F - u);
        float y = u * this.rise - this.sagDepth * sag * hang;
        // Idle breathing, so a cable waiting to be fired is never dead still.
        y += Mth.sin(time * 0.071F + u * 5.4F) * this.sagDepth * 0.10F * hang;

        if (front > 0.0F) {
            // The shock ahead of the blast. The cable that has not blown yet is
            // the most frightening thing in the shot, and it has to be visibly
            // hit by what is coming rather than waiting its turn politely.
            float ahead = (u - front) * this.segments;
            if (ahead > 0.0F && ahead < SHOCK_LEAD) {
                float lead = 1.0F - ahead / SHOCK_LEAD;
                y += lead * lead * this.radius * 5.0F * Mth.sin(ahead * 1.15F - time * 1.7F);
            }
        }
        return y;
    }

    /** Sideways sway of the run at {@code u}, on the same pinned-ends rule. */
    private float driftAt(float u, float time) {
        float hang = 4.0F * u * (1.0F - u);
        return Mth.sin(time * 0.055F + u * 3.1F) * this.sagDepth * 0.16F * hang;
    }

    // ---- the blink ----------------------------------------------------------

    /**
     * Advance the indicator phase by the current flash rate.
     *
     * <p>Integrated rather than computed from {@code rate * time} because the
     * rate is the thing being animated: multiplying a moving rate by an absolute
     * clock jumps the phase every time the rate changes, and a blinking light
     * whose phase jumps reads as a rendering fault instead of as a countdown.
     */
    private void advanceBlink(float time, float urgency) {
        // Squared, so almost all of the acceleration is crammed into the last of
        // the track - the run sits there ticking over and then panics.
        float rate = BLINK_SLOW + (BLINK_FAST - BLINK_SLOW) * urgency * urgency;
        float dt = time - this.blinkClock;
        // The negation also catches the NaN of the very first frame. A scene that
        // scrubs or restarts gets a fresh cycle rather than a hundred flashes
        // nobody was there to see.
        if (!(dt > 0.0F) || dt > 8.0F) {
            dt = 0.0F;
        }
        this.blinkClock = time;
        this.blinkPhase += rate * dt;
        this.blinkPhase -= (float) Math.floor(this.blinkPhase);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** Plain linear blend: nothing inside a detonation eases. */
    private static float ramp(float a, float b, float u) {
        return a + (b - a) * Mth.clamp(u, 0.0F, 1.0F);
    }

    private static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }
}
