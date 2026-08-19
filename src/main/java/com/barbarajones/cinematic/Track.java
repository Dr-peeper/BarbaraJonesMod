package com.barbarajones.cinematic;

import java.util.ArrayList;
import java.util.List;

/**
 * One animated scalar over time, in ticks, sampled at fractional time so it is
 * frame-smooth rather than tick-steppy.
 *
 * <p>Keys may be added in any order; they are kept sorted. Sampling before the
 * first key or after the last one holds that key's value, which means a track
 * only has to describe the window it cares about.
 *
 * <p>The optional {@link #wobble} layer is added on top of the keyed value. It
 * exists because secondary motion - hair still swinging after the head has
 * stopped, a hanging blade quivering, a giant breathing - is a property of the
 * thing, not a beat worth keyframing. Keyframe the intent, wobble the life.
 */
public final class Track {

    private final List<Keyframe> keys = new ArrayList<>();
    private float wobbleAmp;
    private float wobblePeriod = 20.0F;
    private float wobblePhase;
    private float wobbleDecay;

    /** A track that starts at this value at this time. */
    public static Track from(float time, float value) {
        return new Track().key(time, value, Easing.LINEAR);
    }

    /** A track that never changes - handy as a readable "this stays put". */
    public static Track constant(float value) {
        return new Track().key(0.0F, value, Easing.LINEAR);
    }

    /** Add a key, reaching it from the previous one along CUBIC_IN_OUT. */
    public Track key(float time, float value) {
        return key(time, value, Easing.CUBIC_IN_OUT);
    }

    public Track key(float time, float value, Easing ease) {
        Keyframe k = new Keyframe(time, value, ease);
        int i = 0;
        while (i < this.keys.size() && this.keys.get(i).time <= time) {
            i++;
        }
        this.keys.add(i, k);
        return this;
    }

    /**
     * Hold the current value until this time. The dwell is what makes a beat
     * land: without it every pose bleeds straight into the next one and the eye
     * never gets to read any of them.
     */
    public Track hold(float time) {
        if (this.keys.isEmpty()) {
            return this;
        }
        return key(time, this.keys.get(this.keys.size() - 1).value, Easing.LINEAR);
    }

    /**
     * Add a sine on top of everything else.
     *
     * @param decay ticks over which the wobble dies away; 0 means it never does
     */
    public Track wobble(float amplitude, float periodTicks, float phase, float decay) {
        this.wobbleAmp = amplitude;
        this.wobblePeriod = Math.max(0.001F, periodTicks);
        this.wobblePhase = phase;
        this.wobbleDecay = decay;
        return this;
    }

    public float sample(float time) {
        return keyed(time) + wobbleAt(time);
    }

    private float keyed(float time) {
        int n = this.keys.size();
        if (n == 0) {
            return 0.0F;
        }
        if (time <= this.keys.get(0).time) {
            return this.keys.get(0).value;
        }
        Keyframe last = this.keys.get(n - 1);
        if (time >= last.time) {
            return last.value;
        }
        for (int i = 0; i < n - 1; i++) {
            Keyframe a = this.keys.get(i);
            Keyframe b = this.keys.get(i + 1);
            if (time >= a.time && time <= b.time) {
                float span = b.time - a.time;
                // Coincident keys are how you author an instant change; treat the
                // span as already finished rather than dividing by zero.
                float u = span <= 0.0001F ? 1.0F : (time - a.time) / span;
                return Easing.lerp(a.value, b.value, b.ease.apply(u));
            }
        }
        return last.value;
    }

    private float wobbleAt(float time) {
        if (this.wobbleAmp == 0.0F) {
            return 0.0F;
        }
        float amp = this.wobbleAmp;
        if (this.wobbleDecay > 0.0F) {
            float start = this.keys.isEmpty() ? 0.0F : this.keys.get(0).time;
            amp *= 1.0F - Easing.clamp01((time - start) / this.wobbleDecay);
        }
        return amp * (float) Math.sin((time / this.wobblePeriod + this.wobblePhase) * Math.PI * 2.0D);
    }

    /** Time of the last key, so a scene can work out how long it really runs. */
    public float end() {
        return this.keys.isEmpty() ? 0.0F : this.keys.get(this.keys.size() - 1).time;
    }
}
