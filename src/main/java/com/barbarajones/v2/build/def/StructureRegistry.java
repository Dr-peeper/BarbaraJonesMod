package com.barbarajones.v2.build.def;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The in-memory index of every {@link StructureDef} any module has built,
 * keyed by its id. Modules like {@code KraveHouseDefs} call
 * {@link #register} once during mod init; {@code KraveSchematicItem} and
 * anything else that needs to actually place a structure calls {@link #get}
 * with the id it was constructed with.
 */
public final class StructureRegistry {

    private static final Map<ResourceLocation, StructureDef> DEFS = new LinkedHashMap<>();

    private StructureRegistry() { }

    public static void register(StructureDef def) {
        DEFS.put(def.id(), def);
    }

    public static StructureDef get(ResourceLocation id) {
        return DEFS.get(id);
    }

    public static Map<ResourceLocation, StructureDef> all() {
        return Map.copyOf(DEFS);
    }
}
