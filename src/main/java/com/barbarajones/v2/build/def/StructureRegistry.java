package com.barbarajones.v2.build.def;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every building the mod knows about, by id.
 *
 * <p>Not a Forge registry on purpose. Structure definitions are plain data, the
 * same on both sides, and never referenced from NBT by numeric id - so there is
 * nothing to freeze, sync or datapack. Keeping them in a plain map means the
 * client can evaluate the ghost preview locally against its own copy of the
 * definition without a single packet.
 *
 * <p><b>Register during your module's {@code init(bus)}</b>, i.e. during mod
 * construction. Anything later and the creative tab will already have been
 * built without your schematics in it.
 */
public final class StructureRegistry {

    private static final Map<ResourceLocation, StructureDef> DEFS =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private StructureRegistry() { }

    /** Registers a definition and returns it, so it can be assigned in one line. */
    public static StructureDef register(StructureDef def) {
        StructureDef previous = DEFS.put(def.id(), def);
        if (previous != null) {
            throw new IllegalStateException("Duplicate structure id " + def.id()
                    + " - two buildings cannot share an id, the schematic item resolves by it.");
        }
        return def;
    }

    /** @return the definition, or null if nothing registered that id. */
    public static StructureDef get(ResourceLocation id) {
        return id == null ? null : DEFS.get(id);
    }

    public static boolean contains(ResourceLocation id) {
        return id != null && DEFS.containsKey(id);
    }

    /** Registration order. Used to populate the creative tab. */
    public static Collection<StructureDef> all() {
        synchronized (DEFS) {
            return java.util.List.copyOf(DEFS.values());
        }
    }

    public static int count() {
        return DEFS.size();
    }
}
