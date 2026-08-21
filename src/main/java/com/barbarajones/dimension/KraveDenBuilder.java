package com.barbarajones.dimension;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * Builds Krave Monster's den: a guaranteed-solid platform - carved
 * independently of the surrounding procedural terrain, since this is a fixed
 * one-time structure tied to a fixed coordinate (the dimension origin) -
 * with an imported castle (see {@code data/barbarajones/structures/krave_den.nbt})
 * placed on top of it, and the hidden healing boxes that protect him.
 * Called exactly once, alongside the boss's own one-time spawn.
 *
 * <p>The castle came from a fan-built fortress schematic, converted to a
 * vanilla structure NBT and remapped block-for-block onto krave materials
 * (stone bricks/tuff/cobblestone -> the three Chocolate/Krave stone tones,
 * spruce -> Krave Planks/Stairs/Slab/Trapdoor/Log, spruce doors -> the new
 * plain Chocolate Door). Oak elements and small accessories (torches,
 * barrels, ladders, banners, candles, cobwebs) were left vanilla - no krave
 * equivalent exists or was wanted for those. See tools/make_krave_castle_textures.ps1
 * for the two new stone recolors and the door texture.
 *
 * <p>The castle's own courtyard has no floor of its own (the source build
 * stood it on natural ground, which the import deliberately excluded along
 * with every grass/dirt block) - {@link #BOSS_LOCAL_X}/{@link #BOSS_LOCAL_Z}
 * is a vertical shaft straight through the whole structure with nothing in
 * it, found by the conversion script specifically so the platform below and
 * the boss standing on it are never blocked by castle geometry.
 */
public final class KraveDenBuilder {

    private static final ResourceLocation CASTLE_ID = new ResourceLocation("barbarajones", "krave_den");

    /**
     * Half-extents of the platform, in blocks each direction from center.
     * The castle is 30x31 with its courtyard shaft (see BOSS_LOCAL_X/Z
     * below) offset 14/15 blocks from its own edges - a CIRCLE of even a
     * generous radius misses a rectangle's corners (corner distance here is
     * ~21, well past any radius that still hugs the straight edges), which
     * is exactly why the castle used to float at its corners with no floor
     * under them. A rectangle sized to the actual footprint, plus a
     * 2-block margin, has no such gap.
     */
    private static final int HALF_X = 17;
    private static final int HALF_Z = 17;

    /** Local (x,z) of the courtyard's open shaft inside krave_den.nbt - see the class doc. */
    private static final int BOSS_LOCAL_X = 14;
    private static final int BOSS_LOCAL_Z = 15;

    private KraveDenBuilder() { }

    public static void buildDen(ServerLevel kosmos, BlockPos center) {
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // Solid platform under the whole castle footprint, independent of
        // the surrounding procedural terrain - carve air above it and fill
        // ground below, so the den (and the boss standing in its courtyard)
        // never depends on the noise function happening to line up here.
        for (int dx = -HALF_X; dx <= HALF_X; dx++) {
            for (int dz = -HALF_Z; dz <= HALF_Z; dz++) {
                BlockPos base = center.offset(dx, 0, dz);
                kosmos.setBlock(base, grass, 2);
                for (int down = 1; down <= 3; down++) {
                    kosmos.setBlock(base.below(down), dirt, 2);
                }
                for (int up = 1; up <= 16; up++) {
                    kosmos.setBlock(base.above(up), air, 2);
                }
            }
        }

        placeCastle(kosmos, center);

        // The castle's own roofline caps out around 14 blocks up - clear
        // real sky above just the courtyard shaft the rest of the way, so
        // the boss's tallest forms (up to eighteen blocks of collision box
        // at form seven) have somewhere to actually stand without
        // suffocating in his own den.
        for (int r = -2; r <= 2; r++) {
            for (int r2 = -2; r2 <= 2; r2++) {
                BlockPos col = center.offset(r, 0, r2);
                for (int up = 15; up <= 30; up++) {
                    kosmos.setBlock(col.above(up), air, 2);
                }
            }
        }

        spawnGuardianBox(kosmos, center);
    }

    /**
     * Places the imported castle so its open courtyard shaft lands exactly
     * on {@code center}, resting on the platform built just above.
     */
    private static void placeCastle(ServerLevel kosmos, BlockPos center) {
        StructureTemplateManager manager = kosmos.getStructureManager();
        Optional<StructureTemplate> template = manager.get(CASTLE_ID);
        if (template.isEmpty()) {
            return;   // missing/corrupt structure file - leave the bare platform rather than crash
        }

        BlockPos origin = center.offset(-BOSS_LOCAL_X, 1, -BOSS_LOCAL_Z);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKeepLiquids(false);
        template.get().placeInWorld(kosmos, origin, origin, settings, kosmos.getRandom(), 2);
    }

    /**
     * One elite Krave Box at the den's exact center - the boss's own
     * protector, bigger and with double the shield capacity (see
     * KraveHealingBox.setElite). The four ordinary boxes ringing the
     * landing island instead of the den are placed separately, once, by
     * KraveDoorBlock.ensureLandingBoxesExist - they used to sit here too,
     * but clustering all five at the den meant nobody ever found one
     * anywhere else in the Kosmos.
     */
    private static void spawnGuardianBox(ServerLevel kosmos, BlockPos center) {
        var bossId = KraveKosmosData.get(kosmos).getBossId();
        KraveMonster boss = bossId != null && kosmos.getEntity(bossId) instanceof KraveMonster m ? m : null;

        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
        if (box == null) {
            return;
        }
        BlockPos pos = center.above(1);
        box.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        box.setElite(true);
        if (boss != null) {
            box.setHealTarget(boss);
        }
        kosmos.addFreshEntity(box);
    }
}
