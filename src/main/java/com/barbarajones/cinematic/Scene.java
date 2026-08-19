package com.barbarajones.cinematic;

import com.barbarajones.cinematic.actor.CinematicActor;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * One authored beat, complete: the timeline that drives it, the camera rig that
 * shoots it, and the actors that perform in it.
 *
 * <p>A scene is built fresh every time it plays, because every keyframe in it is
 * expressed in absolute world coordinates around wherever the pet actually died.
 * That is deliberate - authoring in world space means a shot reads as "the camera
 * ends up twenty blocks north-west at head height", which you can picture, rather
 * than as a chain of offsets you cannot.
 *
 * <p>Client-only: it holds {@link CinematicActor}s, which touch rendering classes.
 */
public final class Scene {

    private final String name;
    private final Vec3 origin;
    private final Timeline timeline;
    private final CameraRig rig;
    private final List<CinematicActor> actors = new ArrayList<>();
    private final int settleTicks;

    public Scene(String name, Vec3 origin, Timeline timeline, CameraRig rig, int settleTicks) {
        this.name = name;
        this.origin = origin;
        this.timeline = timeline;
        this.rig = rig;
        this.settleTicks = Math.max(1, settleTicks);
    }

    public Scene add(CinematicActor actor) {
        this.actors.add(actor);
        return this;
    }

    public String name() {
        return this.name;
    }

    public Vec3 origin() {
        return this.origin;
    }

    public Timeline timeline() {
        return this.timeline;
    }

    public CameraRig rig() {
        return this.rig;
    }

    public List<CinematicActor> actors() {
        return this.actors;
    }

    /** Ticks spent handing the camera back to the player once the beat is over. */
    public int settleTicks() {
        return this.settleTicks;
    }

    public void advance() {
        this.timeline.advance();
    }

    public boolean finished() {
        return this.timeline.finished();
    }

    /**
     * Fractional scene time for rendering.
     *
     * <p>One behind the timeline's tick on purpose. The tick handler advances the
     * clock and then poses the camera for the NEW tick, so during the frames that
     * follow, the game is interpolating from the previous tick toward that one -
     * exactly as it does for every entity in the world. Rendering actors at the
     * raw tick would run them a whole tick ahead of the camera watching them.
     */
    public float time(float partialTick) {
        return this.timeline.tick() - 1 + partialTick;
    }
}
