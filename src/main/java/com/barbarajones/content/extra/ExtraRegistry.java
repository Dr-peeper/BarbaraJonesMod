package com.barbarajones.content.extra;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for the one entity this content pack needs - the recliner seat.
 *
 * <p>It deliberately does NOT go through {@code ModEntities}: the seat is an
 * implementation detail of a single block, it has no spawn egg, no attributes
 * and no gameplay of its own, and keeping it here means the whole extra-content
 * pack is self-contained.
 *
 * <p>It MUST be a {@link DeferredRegister} rather than a plain static field.
 * {@code EntityType.Builder.build()} constructs the EntityType immediately, and
 * that constructor reaches into the entity-type registry for its holder. In a
 * static initialiser that runs while Forge is class-loading event subscribers,
 * long after registries are frozen, which throws "Registry is already frozen"
 * and takes the whole mod down at load. DeferredRegister defers the build into
 * the registry event, which is the only point it is legal.
 */
public final class ExtraRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    /** Tiny, unsummonable, never written to disk - see {@link SeatEntity}. */
    public static final RegistryObject<EntityType<SeatEntity>> RECLINER_SEAT =
            ENTITIES.register("recliner_seat", () -> EntityType.Builder
                    .<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .noSummon()
                    .noSave()
                    .build("recliner_seat"));

    private ExtraRegistry() { }
}
