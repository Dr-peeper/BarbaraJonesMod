package com.barbarajones.cinematic;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The camera rig: a list of eased shots, a roll track, a field-of-view track and
 * a stack of shake impulses, sampled together into one camera pose per frame.
 *
 * <p>The old cinematic did not have a camera, it had a look-at snap: the player's
 * head was rotated toward the spectacle over a dozen ticks and then abandoned. A
 * cut is not a camera move. Here the camera has a POSITION, so it can dolly in
 * on an actor, orbit it so the parallax sells the scale, drop when something
 * lands and spring back up.
 *
 * <p>Shots blend rather than cut. Each shot declares a blend-in window; while
 * that window runs, the shot is mixed over whatever the previous shots left
 * behind, so consecutive shots never need to agree on their endpoints. A shot
 * with a zero blend is a deliberate hard cut.
 *
 * <p>Shake is a stack of impulses, each decaying over its own lifetime and, if
 * given an epicentre, falling off with distance from it. That distance falloff
 * is what makes a giant's footsteps grow as he closes on the camera without any
 * per-footstep authoring.
 */
public final class CameraRig {

    /** Where the camera is, where it points, and how it is dressed, right now. */
    public static final class State {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float roll;
        public float fov;
    }

    private static final class Shot {
        int start;
        int dur;
        int blend;
        Easing ease = Easing.CUBIC_IN_OUT;
        boolean orbit;
        Vec3 from = Vec3.ZERO;
        Vec3 to = Vec3.ZERO;
        Vec3 lookFrom = Vec3.ZERO;
        Vec3 lookTo = Vec3.ZERO;
        Vec3 centre = Vec3.ZERO;
        double r0;
        double r1;
        double h0;
        double h1;
        float a0;
        float a1;
    }

    private static final class Impulse {
        int at;
        int dur;
        float amp;
        float freq;
        Vec3 epicentre;
        double radius;
    }

    private final List<Shot> shots = new ArrayList<>();
    private final List<Impulse> impulses = new ArrayList<>();
    private final float defaultFov;

    private Track rollTrack;
    private Track fovTrack;

    public CameraRig(float defaultFov) {
        this.defaultFov = defaultFov;
    }

    // ---- authoring ---------------------------------------------------------

    /** Travel from one point to another while the look target also travels. */
    public CameraRig dolly(int start, int dur, int blend, Vec3 from, Vec3 to,
                           Vec3 lookFrom, Vec3 lookTo, Easing ease) {
        Shot s = new Shot();
        s.start = start;
        s.dur = Math.max(1, dur);
        s.blend = blend;
        s.ease = ease == null ? Easing.CUBIC_IN_OUT : ease;
        s.from = from;
        s.to = to;
        s.lookFrom = lookFrom;
        s.lookTo = lookTo;
        this.shots.add(s);
        return this;
    }

    /** Travel while holding one subject in frame. */
    public CameraRig dolly(int start, int dur, int blend, Vec3 from, Vec3 to, Vec3 look, Easing ease) {
        return dolly(start, dur, blend, from, to, look, look, ease);
    }

    /** Sit still. Stillness between two moves is what makes the moves read. */
    public CameraRig hold(int start, int dur, int blend, Vec3 at, Vec3 look) {
        return dolly(start, dur, blend, at, at, look, look, Easing.LINEAR);
    }

    /**
     * Sweep around a subject. Radius and height interpolate too, so one shot can
     * orbit and push in at the same time - the move that makes a static giant
     * look enormous.
     */
    public CameraRig orbit(int start, int dur, int blend, Vec3 centre,
                           double radiusFrom, double radiusTo,
                           double heightFrom, double heightTo,
                           float angleFromDeg, float angleToDeg, Vec3 look, Easing ease) {
        Shot s = new Shot();
        s.start = start;
        s.dur = Math.max(1, dur);
        s.blend = blend;
        s.ease = ease == null ? Easing.CUBIC_IN_OUT : ease;
        s.orbit = true;
        s.centre = centre;
        s.r0 = radiusFrom;
        s.r1 = radiusTo;
        s.h0 = heightFrom;
        s.h1 = heightTo;
        s.a0 = angleFromDeg;
        s.a1 = angleToDeg;
        s.lookFrom = look;
        s.lookTo = look;
        this.shots.add(s);
        return this;
    }

    /**
     * A shake impulse.
     *
     * @param epicentre where the blow landed, or null for a rig-wide rumble
     * @param radius    distance at which the impulse is halved
     */
    public CameraRig shake(int at, int dur, float amplitude, float frequency,
                           Vec3 epicentre, double radius) {
        Impulse im = new Impulse();
        im.at = at;
        im.dur = Math.max(1, dur);
        im.amp = amplitude;
        im.freq = frequency <= 0.0F ? 1.9F : frequency;
        im.epicentre = epicentre;
        im.radius = radius;
        this.impulses.add(im);
        return this;
    }

    public CameraRig roll(Track track) {
        this.rollTrack = track;
        return this;
    }

    public CameraRig fov(Track track) {
        this.fovTrack = track;
        return this;
    }

    // ---- sampling ----------------------------------------------------------

    /**
     * Fill {@code out} with the camera pose at this fractional tick.
     *
     * @return false if no shot has started yet, in which case out is untouched
     */
    public boolean sample(float time, State out) {
        Vec3 pos = null;
        Vec3 look = null;

        for (Shot s : this.shots) {
            if (time < s.start) {
                continue;
            }
            float local = Mth.clamp((time - s.start) / s.dur, 0.0F, 1.0F);
            float e = s.ease.apply(local);

            Vec3 p;
            if (s.orbit) {
                double ang = Math.toRadians(Mth.lerp(e, s.a0, s.a1));
                double r = Mth.lerp((double) e, s.r0, s.r1);
                double h = Mth.lerp((double) e, s.h0, s.h1);
                p = new Vec3(s.centre.x + Math.cos(ang) * r, s.centre.y + h,
                        s.centre.z + Math.sin(ang) * r);
            } else {
                p = mix(s.from, s.to, e);
            }
            Vec3 l = mix(s.lookFrom, s.lookTo, e);

            if (pos == null) {
                pos = p;
                look = l;
            } else {
                float w = s.blend <= 0
                        ? 1.0F
                        : Easing.CUBIC_IN_OUT.apply(Mth.clamp((time - s.start) / s.blend, 0.0F, 1.0F));
                pos = mix(pos, p, w);
                look = mix(look, l, w);
            }
        }

        if (pos == null) {
            return false;
        }

        float amp = shakeAt(time, pos);
        // Detuned sines rather than per-frame randomness: a real camera on a real
        // rig has mass, so it swings; random jitter just makes pixels crawl.
        float sx = (Mth.sin(time * 1.93F) + 0.6F * Mth.sin(time * 3.11F)) * amp;
        float sy = (Mth.cos(time * 2.37F) + 0.6F * Mth.sin(time * 2.83F)) * amp;
        float sz = (Mth.sin(time * 1.61F) + 0.6F * Mth.cos(time * 3.47F)) * amp;

        out.x = pos.x + sx * 0.22D;
        out.y = pos.y + sy * 0.22D;
        out.z = pos.z + sz * 0.22D;

        double dx = look.x - out.x;
        double dy = look.y - out.y;
        double dz = look.z - out.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        out.yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F + sx * 1.5F;
        out.pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.0001D, horiz))) + sy * 1.2F;
        out.roll = (this.rollTrack == null ? 0.0F : this.rollTrack.sample(time)) + sz * 1.1F;
        out.fov = this.fovTrack == null ? this.defaultFov : this.fovTrack.sample(time);
        return true;
    }

    private float shakeAt(float time, Vec3 camera) {
        float total = 0.0F;
        for (Impulse im : this.impulses) {
            if (time < im.at || time > im.at + im.dur) {
                continue;
            }
            float u = (time - im.at) / im.dur;
            // Full amplitude on the frame of impact, then a fast tail. CUBIC_OUT
            // inverted spends most of its budget early, which is what a real
            // strike does - the world does not ease INTO being hit.
            float a = im.amp * (1.0F - Easing.CUBIC_OUT.apply(u));
            if (im.epicentre != null && im.radius > 0.0D) {
                double d = im.epicentre.distanceTo(camera);
                a *= (float) (1.0D / (1.0D + (d * d) / (im.radius * im.radius)));
            }
            total += a * (0.7F + 0.3F * Mth.sin(time * im.freq));
        }
        return total;
    }

    /** The last authored moment on this rig, shots and shakes alike. */
    public int end() {
        int last = 0;
        for (Shot s : this.shots) {
            last = Math.max(last, s.start + s.dur);
        }
        for (Impulse im : this.impulses) {
            last = Math.max(last, im.at + im.dur);
        }
        return last;
    }

    private static Vec3 mix(Vec3 a, Vec3 b, float t) {
        return new Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }
}
