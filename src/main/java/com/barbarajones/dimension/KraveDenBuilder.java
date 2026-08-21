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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds Krave Monster's den: a real floating island of Krave Dirt/Grass,
 * chocolate dripping off its underside, with an imported castle (see
 * {@code data/barbarajones/structures/krave_den.nbt}) sunk one block into
 * its top and a hidden healing box guarding him. Called exactly once,
 * alongside the boss's own one-time spawn.
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
 * <p>The island exists specifically so the castle has real, natural-looking
 * ground under it instead of floating with nothing below - two earlier
 * versions tried a flat platform (missed the castle's corners) and no
 * ground at all (visibly floating). This one is sized well past the
 * castle's own footprint and tapers to a rounded underside with chocolate
 * pouring off it, the same "place the whole visible cascade directly, don't
 * rely on fluid propagation timing" approach {@link com.barbarajones.worldgen.feature.KraveWaterfallFeature}
 * already uses elsewhere in this dimension.
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

    /** Comfortably past the castle's own ~15-16 block half-footprint, so the whole thing sits on real ground. */
    private static final int ISLAND_RADIUS = 24;
    private static final int ISLAND_MAX_DEPTH = 22;
    private static final int DRIP_COUNT = 6;
    private static final int DRIP_LENGTH = 26;

    private KraveDenBuilder() { }

    public static void buildDen(ServerLevel kosmos, BlockPos center) {
        buildIsland(kosmos, center);
        placeCastle(kosmos, center);

        // The castle's own roofline caps out around 13 blocks above where it
        // was sunk in - clear real sky above just the courtyard shaft the
        // rest of the way, so the boss's tallest forms (up to eighteen
        // blocks of collision box at form seven) have somewhere to actually
        // stand without suffocating in his own den.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int r = -2; r <= 2; r++) {
            for (int r2 = -2; r2 <= 2; r2++) {
                BlockPos col = center.offset(r, 0, r2);
                for (int up = 13; up <= 28; up++) {
                    kosmos.setBlock(col.above(up), air, 2);
                }
            }
        }

        spawnGuardianBox(kosmos, center);
    }

    /**
     * A rounded island of Krave Grass/Dirt, deepest under the center and
     * tapering toward a rounded point at the rim - not a cylinder, so it
     * actually reads as a natural floating island rather than a plug of
     * dirt. {@code center}'s own Y is the island's top surface.
     */
    private static void buildIsland(ServerLevel kosmos, BlockPos center) {
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        var random = ThreadLocalRandom.current();

        for (int dx = -ISLAND_RADIUS; dx <= ISLAND_RADIUS; dx++) {
            for (int dz = -ISLAND_RADIUS; dz <= ISLAND_RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > ISLAND_RADIUS) {
                    continue;
                }
                double t = 1.0D - dist / ISLAND_RADIUS;   // 1 at the center, 0 at the rim
                int depth = 4 + (int) Math.round(t * t * ISLAND_MAX_DEPTH) + random.nextInt(3);

                BlockPos top = center.offset(dx, 0, dz);
                kosmos.setBlock(top, grass, 2);
                for (int y = 1; y < depth; y++) {
                    kosmos.setBlock(top.below(y), dirt, 2);
                }
                for (int up = 1; up <= 3; up++) {
                    kosmos.setBlock(top.above(up), air, 2);
                }
            }
        }

        buildChocolateDrips(kosmos, center);
    }

    /**
     * A handful of chocolate streams poured straight down from points around
     * the island's underside - placed as a whole visible column of source
     * blocks rather than a single spring left to vanilla fluid physics,
     * same lesson {@link com.barbarajones.worldgen.feature.KraveWaterfallFeature}
     * documents: a single source block and hope does not reliably read as a
     * waterfall on a freshly generated chunk.
     */
    private static void buildChocolateDrips(ServerLevel kosmos, BlockPos center) {
        BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
        var random = ThreadLocalRandom.current();

        for (int i = 0; i < DRIP_COUNT; i++) {
            double angle = (Math.PI * 2.0D / DRIP_COUNT) * i + random.nextDouble(-0.4D, 0.4D);
            double r = ISLAND_RADIUS * (0.6D + random.nextDouble(0.0D, 0.3D));
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);

            int bottomY = center.getY();
            while (!kosmos.getBlockState(new BlockPos(center.getX() + dx, bottomY - 1, center.getZ() + dz)).isAir()
                    && center.getY() - bottomY < ISLAND_MAX_DEPTH + 5) {
                bottomY--;
            }

            BlockPos drip = new BlockPos(center.getX() + dx, bottomY, center.getZ() + dz);
            kosmos.setBlock(drip, chocolate, 3);
            for (int down = 1; down <= DRIP_LENGTH; down++) {
                kosmos.setBlock(drip.below(down), chocolate, 3);
            }
        }
    }

    /**
     * Places the imported castle so its open courtyard shaft lands exactly
     * on {@code center}, one block INTO the island's top surface (rather
     * than resting flush on it) so the ground visibly meets the walls
     * instead of the castle looking dropped onto the terrain.
     */
    private static void placeCastle(ServerLevel kosmos, BlockPos center) {
        StructureTemplateManager manager = kosmos.getStructureManager();
        Optional<StructureTemplate> template = manager.get(CASTLE_ID);
        if (template.isEmpty()) {
            return;   // missing/corrupt structure file - nothing to place
        }

        BlockPos origin = center.offset(-BOSS_LOCAL_X, -1, -BOSS_LOCAL_Z);
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
