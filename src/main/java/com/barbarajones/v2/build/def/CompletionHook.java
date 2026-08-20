package com.barbarajones.v2.build.def;

import net.minecraft.server.level.ServerLevel;

/**
 * Runs on the server, once, on the tick a building's last block lands.
 *
 * <p>This is where a building stops being scenery and becomes content: seed the
 * chest, name the sign, spawn the shopkeeper, register the place with the
 * settlement tracker. Hooks run in the order they were added and each one is
 * wrapped in a try/catch by the placer - a hook that throws logs and is skipped
 * rather than leaving a finished building in a half-initialised state.
 */
@FunctionalInterface
public interface CompletionHook {

    void onComplete(ServerLevel level, PlacementContext ctx);
}
