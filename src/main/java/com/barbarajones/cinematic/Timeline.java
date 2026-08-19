package com.barbarajones.cinematic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named bundle of {@link Track}s plus a list of one-shot cues, with its own
 * tick clock.
 *
 * <p>This is the thing the old apocalypse was missing. A beat used to be
 * scattered across a tick() method as {@code if (t == 42) doThing()} plus
 * hand-rolled sine maths, which meant you could not see the shape of the beat
 * anywhere - only its symptoms. Here a beat is one object: continuous values
 * live on tracks and are sampled at fractional time for smooth rendering,
 * discrete moments live as cues and fire exactly once on a whole tick.
 */
public final class Timeline {

    private static final class Cue {
        final int time;
        final Runnable action;

        Cue(int time, Runnable action) {
            this.time = time;
            this.action = action;
        }
    }

    private final Map<String, Track> tracks = new LinkedHashMap<>();
    private final List<Cue> cues = new ArrayList<>();
    private final int duration;

    private int tick;
    private int nextCue;

    public Timeline(int duration) {
        this.duration = Math.max(1, duration);
    }

    public Timeline track(String name, Track track) {
        this.tracks.put(name, track);
        return this;
    }

    /** Fire once, on the whole tick, in authoring order among equal times. */
    public Timeline cue(int time, Runnable action) {
        int i = 0;
        while (i < this.cues.size() && this.cues.get(i).time <= time) {
            i++;
        }
        this.cues.add(i, new Cue(time, action));
        return this;
    }

    /** One client tick. Fires every cue that is now due, then advances. */
    public void advance() {
        while (this.nextCue < this.cues.size() && this.cues.get(this.nextCue).time <= this.tick) {
            Cue c = this.cues.get(this.nextCue++);
            try {
                c.action.run();
            } catch (Throwable ignored) {
                // a cue is a sound or a puff of dust; it must never kill the show
            }
        }
        this.tick++;
    }

    public int tick() {
        return this.tick;
    }

    public int duration() {
        return this.duration;
    }

    public boolean finished() {
        return this.tick >= this.duration;
    }

    /** Sample a track at fractional time, or return the fallback if unauthored. */
    public float value(String name, float time, float fallback) {
        Track t = this.tracks.get(name);
        return t == null ? fallback : t.sample(time);
    }
}
