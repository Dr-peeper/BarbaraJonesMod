package com.barbarajones.cinematic;

/**
 * One authored value at one moment on a {@link Track}.
 *
 * <p>The easing belongs to the key you are travelling TOWARDS, not the one you
 * are leaving. That is the convention every animation tool uses and it is the
 * one that reads correctly when you author a beat top to bottom: "at tick 40 the
 * cleaver is up here, and it gets to tick 52 down there by QUINT_IN" is written
 * as the tick-52 key carrying QUINT_IN.
 */
public final class Keyframe {

    public final float time;
    public final float value;
    public final Easing ease;

    public Keyframe(float time, float value, Easing ease) {
        this.time = time;
        this.value = value;
        this.ease = ease == null ? Easing.LINEAR : ease;
    }
}
