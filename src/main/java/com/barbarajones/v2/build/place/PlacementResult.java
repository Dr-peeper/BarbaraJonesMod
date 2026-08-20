package com.barbarajones.v2.build.place;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * What came back from {@link KraveStructure#place}.
 *
 * <p>{@link #started()} means validation passed and the build job is now
 * running; it does <i>not</i> mean the last block has landed. Buildings go up
 * over a couple of seconds, so hook completion with
 * {@link com.barbarajones.v2.build.def.StructureDef.Builder#onComplete} rather
 * than by assuming the world is finished when this returns.
 *
 * <p>{@link #message()} is always populated and always safe to hand straight to
 * a player - on failure it says exactly what is in the way and where.
 */
public final class PlacementResult {

    private final boolean started;
    private final Component message;
    private final PlacementCheck check;
    private final BuildJob job;

    PlacementResult(boolean started, Component message, PlacementCheck check, @Nullable BuildJob job) {
        this.started = started;
        this.message = message;
        this.check = check;
        this.job = job;
    }

    public boolean started() {
        return started;
    }

    public Component message() {
        return message;
    }

    /** The validation that was run, including per-column detail. */
    public PlacementCheck check() {
        return check;
    }

    /** The running job, or null if placement was refused. */
    @Nullable
    public BuildJob job() {
        return job;
    }
}
