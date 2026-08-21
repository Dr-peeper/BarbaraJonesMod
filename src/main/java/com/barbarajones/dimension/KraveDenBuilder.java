package com.barbarajones.dimension;

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
 * Builds Krave Monster's den: an imported castle (see
 * {@code data/barbarajones/structures/krave_den.nbt}) placed directly onto
 * the Kosmos's own terrain at the dimension's centre, plus the hidden
 * healing box that protects him. Called exactly once, alongside the boss's
 * own one-time spawn.
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
 * <p>No platform gets built under it any more - an earlier version carved
 * a guaranteed-solid pad first, but that meant tracking the castle's exact
 * footprint just to avoid leaving unfloored gaps at its corners. Placed one
 * block lower instead, straight onto whatever the Kosmos generated there.
 *
 * <p>The castle's own courtyard has no floor of its own (the source build
 * stood it on natural ground, which the import deliberately excluded along
 * with every grass/dirt block) - {@link #BOSS_LOCAL_X}/{@link #BOSS_LOCAL_Z}
 * is a vertical shaft straight through the whole structure with nothing in
 * it, found by the conversion script specifically so the boss standing in
 * it is never blocked by castle geometry.
 */
public final class KraveDenBuilder {

    private static final ResourceLocation CASTLE_ID = new ResourceLocation("barbarajones", "krave_den");

    /** Local (x,z) of the courtyard's open shaft inside krave_den.nbt - see the class doc. */
    private static final int BOSS_LOCAL_X = 14;
    private static final int BOSS_LOCAL_Z = 15;

    private KraveDenBuilder() { }

    public static void buildDen(ServerLevel kosmos, BlockPos center) {
        BlockState air = Blocks.AIR.defaultBlockState();

        placeCastle(kosmos, center);

        // The castle's own roofline caps out around 13 blocks up from where
        // it's placed - clear real sky above just the courtyard shaft the
        // rest of the way, so the boss's tallest forms (up to eighteen
        // blocks of collision box at form seven) have somewhere to actually
        // stand without suffocating in his own den.
        for (int r = -2; r <= 2; r++) {
            for (int r2 = -2; r2 <= 2; r2++) {
                BlockPos col = center.offset(r, 0, r2);
                for (int up = 14; up <= 29; up++) {
                    kosmos.setBlock(col.above(up), air, 2);
                }
            }
        }

        spawnGuardianBox(kosmos, center);
    }

    /**
     * Places the imported castle so its open courtyard shaft lands exactly
     * on {@code center}, one block lower than the shaft itself so the
     * castle's own bottom layer sits flush with the Kosmos's own ground
     * instead of floating above it.
     */
    private static void placeCastle(ServerLevel kosmos, BlockPos center) {
        StructureTemplateManager manager = kosmos.getStructureManager();
        Optional<StructureTemplate> template = manager.get(CASTLE_ID);
        if (template.isEmpty()) {
            return;   // missing/corrupt structure file - nothing to place
        }

        BlockPos origin = center.offset(-BOSS_LOCAL_X, 0, -BOSS_LOCAL_Z);
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
