package com.barbarajones.v2.mobs;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Registers where/how each Craveling-family mob is allowed to spawn.
 * {@code SpawnPlacements.register} must run exactly once per entity type and
 * has no natural "registration event" of its own, so {@link Mobs#init} calls
 * {@link #register()} from an {@code FMLCommonSetupEvent} listener - the
 * standard place for this across the modding ecosystem, since it has to run
 * after entity types exist but doesn't touch any registry itself.
 *
 * <p>The four hostile mobs use {@code Monster.checkMonsterSpawnRules} - the
 * exact rule zombies/skeletons use (dark enough, valid ground, difficulty
 * check) - so they compete for the same overworld night-time spawn budget a
 * zombie would. The Mascot uses the permissive {@code Mob.checkMobSpawnRules}
 * since it is a passive/fleeing creature that can appear in daylight.
 */
public final class MobSpawnPlacements {

    private MobSpawnPlacements() { }

    static void register() {
        SpawnPlacements.register(ModMobEntities.CRAVELING.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        SpawnPlacements.register(ModMobEntities.KRISPBONE.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        SpawnPlacements.register(ModMobEntities.LOOMWEAVER.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        SpawnPlacements.register(ModMobEntities.SOGGY.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        SpawnPlacements.register(ModMobEntities.MASCOT.get(),
                SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules);
    }
}
