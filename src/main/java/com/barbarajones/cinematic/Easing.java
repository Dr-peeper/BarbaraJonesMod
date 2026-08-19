package com.barbarajones.cinematic;

/**
 * The curve library the whole Krave Apocalypse cinematic is authored against.
 *
 * <p>Nothing in a watchable cutscene moves at a constant rate. Everything the old
 * apocalypse drove with per-tick arithmetic - camera angles, limb rotations,
 * scales, alphas - is driven here by a keyframed {@link Track} that asks one of
 * these curves how far along it should be at this instant.
 *
 * <p>The curves that leave the 0..1 range on purpose are the important ones:
 * {@link #BACK_IN} and {@link #ANTICIPATE} dip BELOW zero, which is the wind-up
 * before a hit; {@link #BACK_OUT}, {@link #ELASTIC_OUT} and {@link #SPRING_OUT}
 * push PAST one and come back, which is how something heavy arrives instead of
 * simply stopping. A track that only ever uses CUBIC_IN_OUT will look correct and
 * feel dead.
 */
@FunctionalInterface
public interface Easing {

    /** Map normalised progress 0..1 to eased progress - may overshoot on purpose. */
    float apply(float t);

    /** Straight line. Only for things genuinely linear, like a constant spin. */
    Easing LINEAR = t -> t;

    /** A hard cut, as a keyframe curve. Useful when a snap IS the intent. */
    Easing STEP = t -> t < 1.0F ? 0.0F : 1.0F;

    Easing SINE_IN_OUT = t -> (float) -(Math.cos(Math.PI * clamp01(t)) - 1.0D) * 0.5F;

    Easing CUBIC_IN = t -> {
        float x = clamp01(t);
        return x * x * x;
    };

    Easing CUBIC_OUT = t -> {
        float x = 1.0F - clamp01(t);
        return 1.0F - x * x * x;
    };

    Easing CUBIC_IN_OUT = t -> {
        float x = clamp01(t);
        if (x < 0.5F) {
            return 4.0F * x * x * x;
        }
        float f = -2.0F * x + 2.0F;
        return 1.0F - f * f * f * 0.5F;
    };

    Easing QUINT_IN = t -> {
        float x = clamp01(t);
        return x * x * x * x * x;
    };

    /** The classic "decelerate hard" - things coasting to a stop under their own mass. */
    Easing QUINT_OUT = t -> {
        float x = 1.0F - clamp01(t);
        return 1.0F - x * x * x * x * x;
    };

    Easing QUINT_IN_OUT = t -> {
        float x = clamp01(t);
        if (x < 0.5F) {
            return 16.0F * x * x * x * x * x;
        }
        float f = -2.0F * x + 2.0F;
        return 1.0F - f * f * f * f * f * 0.5F;
    };

    /** Gravity-ish: barely moves, then all at once. What a falling object does. */
    Easing EXPO_IN = t -> {
        float x = clamp01(t);
        return x <= 0.0F ? 0.0F : (float) Math.pow(2.0D, 10.0D * x - 10.0D);
    };

    Easing EXPO_OUT = t -> {
        float x = clamp01(t);
        return x >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * x);
    };

    /** Pulls back below zero before it goes. The wind-up before every impact. */
    Easing BACK_IN = t -> {
        float x = clamp01(t);
        final float c1 = 1.70158F;
        return (c1 + 1.0F) * x * x * x - c1 * x * x;
    };

    /** Sails past the target then eases back onto it. How weight arrives. */
    Easing BACK_OUT = t -> {
        float x = clamp01(t) - 1.0F;
        final float c1 = 1.70158F;
        return 1.0F + (c1 + 1.0F) * x * x * x + c1 * x * x;
    };

    Easing BACK_IN_OUT = t -> {
        float x = clamp01(t);
        final float c2 = 1.70158F * 1.525F;
        if (x < 0.5F) {
            float f = 2.0F * x;
            return f * f * ((c2 + 1.0F) * f - c2) * 0.5F;
        }
        float f = 2.0F * x - 2.0F;
        return (f * f * ((c2 + 1.0F) * f + c2) + 2.0F) * 0.5F;
    };

    /** A much harder wind-up than BACK_IN, for the big telegraphed blows. */
    Easing ANTICIPATE = t -> {
        float x = clamp01(t);
        final float c1 = 3.4F;
        return (c1 + 1.0F) * x * x * x - c1 * x * x;
    };

    /** Snaps to the target and rings around it. Jaws dropping, flaps bursting open. */
    Easing ELASTIC_OUT = t -> {
        float x = clamp01(t);
        if (x <= 0.0F) {
            return 0.0F;
        }
        if (x >= 1.0F) {
            return 1.0F;
        }
        final float c4 = (float) (2.0D * Math.PI / 3.0D);
        return (float) (Math.pow(2.0D, -10.0D * x) * Math.sin((x * 10.0F - 0.75F) * c4) + 1.0D);
    };

    /** Lands, bounces, lands smaller, settles. For things hitting the ground. */
    Easing BOUNCE_OUT = t -> {
        float x = clamp01(t);
        final float n1 = 7.5625F;
        final float d1 = 2.75F;
        if (x < 1.0F / d1) {
            return n1 * x * x;
        }
        if (x < 2.0F / d1) {
            x -= 1.5F / d1;
            return n1 * x * x + 0.75F;
        }
        if (x < 2.5F / d1) {
            x -= 2.25F / d1;
            return n1 * x * x + 0.9375F;
        }
        x -= 2.625F / d1;
        return n1 * x * x + 0.984375F;
    };

    Easing BOUNCE_IN = t -> 1.0F - BOUNCE_OUT.apply(1.0F - clamp01(t));

    /**
     * A damped spring - overshoots, oscillates a couple of times and dies out.
     * This is the settle-back curve: after a camera is slammed, it should not
     * glide politely home, it should wobble onto its mark.
     */
    Easing SPRING_OUT = t -> {
        float x = clamp01(t);
        return 1.0F - (float) (Math.exp(-6.0D * x) * Math.cos(11.0D * x));
    };

    static float clamp01(float t) {
        return t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** One-off eased blend, for values not worth a whole track. */
    static float ease(Easing curve, float a, float b, float t) {
        return lerp(a, b, curve.apply(t));
    }
}
