package com.barbarajones.v2.airline;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Module entry point, kept for symmetry with the other v2 modules.
 *
 * <p>The airline's entities, blocks and items live in the shared Mod* registries
 * rather than in DeferredRegisters of their own, so there is nothing to hand to the
 * bus here. Attribute registration happens in {@link AirlineEvents}.
 */
public final class AirlineModule {

    private AirlineModule() { }

    public static void init(IEventBus bus) {
        // Intentionally empty - see the class note.
    }
}
