package com.barbarajones.v2.mobs;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KravajoEntity;
import com.barbarajones.v2.mobs.entity.KravelingEntity;
import com.barbarajones.v2.mobs.entity.KrispboneEntity;
import com.barbarajones.v2.mobs.entity.LoomweaverEntity;
import com.barbarajones.v2.mobs.entity.MascotEntity;
import com.barbarajones.v2.mobs.entity.SoggyEntity;
import com.barbarajones.v2.mobs.entity.projectile.KraveShardEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's OWN entity registry - separate from {@code content.ModEntities}
 * so this file can be edited without touching anything another agent has open.
 * See {@link Mobs#init} for the single entry point the orchestrator calls.
 *
 * <p>EntityType.Builder.build() is called INSIDE each supplier lambda, never in
 * a static field initialiser - doing it outside runs before the registry
 * thaws and throws "Registry is already frozen".
 */
public final class ModMobEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    private ModMobEntities() { }

    public static final RegistryObject<EntityType<KravelingEntity>> KRAVELING =
            ENTITIES.register("kraveling", () -> EntityType.Builder
                    .of(KravelingEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build("kraveling"));

    public static final RegistryObject<EntityType<KrispboneEntity>> KRISPBONE =
            ENTITIES.register("krispbone", () -> EntityType.Builder
                    .of(KrispboneEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(12)
                    .build("krispbone"));

    public static final RegistryObject<EntityType<LoomweaverEntity>> LOOMWEAVER =
            ENTITIES.register("loomweaver", () -> EntityType.Builder
                    .of(LoomweaverEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 0.85F).clientTrackingRange(10)
                    .build("loomweaver"));

    public static final RegistryObject<EntityType<SoggyEntity>> SOGGY =
            ENTITIES.register("soggy", () -> EntityType.Builder
                    .of(SoggyEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.8F).clientTrackingRange(10)
                    .build("soggy"));

    /** The sky flea. Flying, harmless, everywhere. */
    public static final RegistryObject<EntityType<KravajoEntity>> KRAVAJO =
            ENTITIES.register("kravajo", () -> EntityType.Builder
                    .of(KravajoEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.5F).clientTrackingRange(8).updateInterval(2)
                    .build("kravajo"));

    public static final RegistryObject<EntityType<MascotEntity>> MASCOT =
            ENTITIES.register("the_mascot", () -> EntityType.Builder
                    .of(MascotEntity::new, MobCategory.CREATURE)
                    .sized(0.7F, 2.1F).clientTrackingRange(12)
                    .build("the_mascot"));

    // Krispbone's ranged attack - MISC category, no spawn egg, no attribute entry.
    public static final RegistryObject<EntityType<KraveShardEntity>> KRAVE_SHARD =
            ENTITIES.register("krave_shard", () -> EntityType.Builder
                    .<KraveShardEntity>of(KraveShardEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(16).updateInterval(10)
                    .build("krave_shard"));
}
