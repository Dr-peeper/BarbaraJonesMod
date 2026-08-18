package com.barbarajones.dimension;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds Krave Monster's den: a guaranteed-solid platform - carved
 * independently of the surrounding procedural terrain, since this is a fixed
 * one-time structure tied to a fixed coordinate (the dimension origin) - plus
 * a ring of pillars, and spawns the hidden healing boxes that protect him.
 * Called exactly once, alongside the boss's own one-time spawn.
 */
public final class KraveDenBuilder {

    private static final int RADIUS = 10;
    private static final int PILLAR_HEIGHT = 6;

    private KraveDenBuilder() { }

    public static void buildDen(ServerLevel kosmos, BlockPos center) {
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Solid circular platform, independent of the surrounding procedural
        // terrain - carve air above it and fill ground below, so the den (and
        // the boss standing on it) never depends on the noise function
        // happening to line up here.
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > RADIUS) {
                    continue;
                }
                BlockPos base = center.offset(dx, 0, dz);
                kosmos.setBlock(base, grass, 2);
                for (int down = 1; down <= 3; down++) {
                    kosmos.setBlock(base.below(down), dirt, 2);
                }
                for (int up = 1; up <= 6; up++) {
                    kosmos.setBlock(base.above(up), air, 2);
                }
            }
        }

        // Four pillars ringing the boss at the platform's edge.
        int[][] pillarOffsets = { {RADIUS - 1, 0}, {-(RADIUS - 1), 0}, {0, RADIUS - 1}, {0, -(RADIUS - 1)} };
        for (int[] off : pillarOffsets) {
            BlockPos base = center.offset(off[0], 1, off[1]);
            for (int y = 0; y < PILLAR_HEIGHT; y++) {
                kosmos.setBlock(base.above(y), frame, 2);
            }
        }

        // A modest, deterministic rubble scatter - not randomized, so it's
        // guaranteed not to float or clip through the platform.
        for (int dx = -RADIUS + 2; dx <= RADIUS - 2; dx += 3) {
            for (int dz = -RADIUS + 2; dz <= RADIUS - 2; dz += 4) {
                if ((dx + dz) % 2 != 0) {
                    continue;
                }
                BlockPos base = center.offset(dx, 1, dz);
                if (kosmos.getBlockState(base).isAir()) {
                    kosmos.setBlock(base, frame, 2);
                }
            }
        }

        spawnHealingBoxes(kosmos, center);
    }

    private static void spawnHealingBoxes(ServerLevel kosmos, BlockPos center) {
        var bossId = KraveKosmosData.get(kosmos).getBossId();
        KraveMonster boss = bossId != null && kosmos.getEntity(bossId) instanceof KraveMonster m ? m : null;

        int[][] spots = { {6, 6}, {-6, 6}, {6, -6}, {-6, -6} };
        for (int[] spot : spots) {
            BlockPos pos = center.offset(spot[0], 1, spot[1]);
            KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
            if (box == null) {
                continue;
            }
            box.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            if (boss != null) {
                box.setHealTarget(boss);
            }
            kosmos.addFreshEntity(box);
        }
    }
}
