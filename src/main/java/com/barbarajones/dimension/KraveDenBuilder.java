package com.barbarajones.dimension;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds Krave Monster's den: a real floating island of Krave Dirt/Grass,
 * chocolate dripping off its underside, with an imported castle (see
 * {@code data/barbarajones/structures/krave_den.nbt}) sitting flush on its
 * top and a hidden healing box guarding him. Called exactly once, alongside
 * the boss's own one-time spawn.
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
 * <p>Everything here builds {@link #DEN_HEIGHT_OFFSET} blocks above
 * {@code center} (which is {@link KraveDimensions#BOSS_ISLAND}), not at it -
 * high enough to sit clear of whatever the Kosmos's own end-islands-style
 * terrain does at that spot, so the island reads as genuinely floating
 * rather than fused into a hillside. {@link com.barbarajones.block.KraveDoorBlock#ensureBossExists}
 * spawns the boss at that same raised height for exactly that reason - if
 * you change this constant, his spawn point has to move with it.
 *
 * <p>The island's top is flat only under the castle's own footprint
 * ({@link #CASTLE_FLAT_HALF_X}/{@link #CASTLE_FLAT_HALF_Z}) - past that it
 * rolls into real hills, tapering to a rounded, deliberately pointed
 * underside with chocolate pouring off it (a whole visible falling column
 * per drip, not a single spring left to fluid physics - the same lesson
 * {@link com.barbarajones.worldgen.feature.KraveWaterfallFeature} already
 * documents elsewhere in this dimension). A cleared ring of air below the
 * point guarantees it never touches whatever terrain happens to be
 * underneath, on top of the height offset already doing most of that work.
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

    /** How far above KraveDimensions.BOSS_ISLAND the whole den sits - see the class doc. */
    public static final int DEN_HEIGHT_OFFSET = 30;

    /** Local (x,z) of the courtyard's open shaft inside krave_den.nbt - see the class doc. */
    private static final int BOSS_LOCAL_X = 14;
    private static final int BOSS_LOCAL_Z = 15;

    /** Comfortably past the castle's own ~15-16 block half-footprint, so the whole thing sits on real ground. */
    private static final int ISLAND_RADIUS = 24;
    private static final int ISLAND_MAX_DEPTH = 22;
    /** The island's top stays perfectly flat inside this box - past it, hills start. */
    private static final int CASTLE_FLAT_HALF_X = 17;
    private static final int CASTLE_FLAT_HALF_Z = 17;
    private static final int HILL_RAMP = 10;
    private static final int HILL_HEIGHT = 5;
    /** Extra clearance below the island's deepest point, so it never touches whatever is under it. */
    private static final int FLOAT_CLEARANCE = 16;

    private static final int DRIP_COUNT = 6;
    private static final int DRIP_LENGTH = 26;

    private KraveDenBuilder() { }

    public static void buildDen(ServerLevel kosmos, BlockPos center) {
        BlockPos anchor = center.above(DEN_HEIGHT_OFFSET);

        buildIsland(kosmos, anchor);
        placeCastle(kosmos, anchor);

        // The castle's own roofline caps out around 13 blocks above the
        // flat zone it sits on - clear real sky above just the courtyard
        // shaft the rest of the way, so the boss's tallest forms (up to
        // eighteen blocks of collision box at form seven) have somewhere to
        // actually stand without suffocating in his own den.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int r = -2; r <= 2; r++) {
            for (int r2 = -2; r2 <= 2; r2++) {
                BlockPos col = anchor.offset(r, 0, r2);
                for (int up = 13; up <= 28; up++) {
                    kosmos.setBlock(col.above(up), air, 2);
                }
            }
        }

        spawnGuardianBox(kosmos, anchor);
    }

    /**
     * A rounded island of Krave Grass/Dirt: flat under the castle, rolling
     * into hills past that, and tapering to a pointed underside with
     * nothing but cleared air below it. {@code anchor}'s own Y is the flat
     * zone's surface.
     */
    private static void buildIsland(ServerLevel kosmos, BlockPos anchor) {
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

                // Flat inside the castle's own footprint; past that, ramp
                // into real hills over HILL_RAMP blocks so the transition
                // doesn't read as a cliff at the flat zone's edge.
                double pastFlatX = Math.max(0.0D, Math.abs(dx) - CASTLE_FLAT_HALF_X);
                double pastFlatZ = Math.max(0.0D, Math.abs(dz) - CASTLE_FLAT_HALF_Z);
                double pastFlat = Math.sqrt(pastFlatX * pastFlatX + pastFlatZ * pastFlatZ);
                double hillT = Math.min(1.0D, pastFlat / HILL_RAMP);
                int surface = hillT <= 0.0D ? 0
                        : (int) Math.round((random.nextDouble() * 2.0D - 1.0D) * hillT * HILL_HEIGHT);

                double t = 1.0D - dist / ISLAND_RADIUS;   // 1 at the center, 0 at the rim
                int depth = 4 + (int) Math.round(t * t * ISLAND_MAX_DEPTH) + random.nextInt(3);

                BlockPos top = anchor.offset(dx, surface, dz);
                kosmos.setBlock(top, grass, 2);
                for (int y = 1; y < depth; y++) {
                    kosmos.setBlock(top.below(y), dirt, 2);
                }
                for (int up = 1; up <= 3; up++) {
                    kosmos.setBlock(top.above(up), air, 2);
                }
                // Guaranteed clear air well below the deepest point this
                // column could ever reach, regardless of what the Kosmos's
                // own terrain generated there - the island floats for real.
                for (int down = 1; down <= FLOAT_CLEARANCE; down++) {
                    kosmos.setBlock(top.below(depth + down), air, 2);
                }
            }
        }

        buildChocolateDrips(kosmos, anchor);
    }

    /**
     * A handful of chocolate streams poured straight down from points around
     * the island's underside - placed as a whole visible column of source
     * blocks rather than a single spring left to vanilla fluid physics,
     * same lesson {@link com.barbarajones.worldgen.feature.KraveWaterfallFeature}
     * documents: a single source block and hope does not reliably read as a
     * waterfall on a freshly generated chunk.
     */
    private static void buildChocolateDrips(ServerLevel kosmos, BlockPos anchor) {
        BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
        var random = ThreadLocalRandom.current();

        for (int i = 0; i < DRIP_COUNT; i++) {
            double angle = (Math.PI * 2.0D / DRIP_COUNT) * i + random.nextDouble(-0.4D, 0.4D);
            double r = ISLAND_RADIUS * (0.6D + random.nextDouble(0.0D, 0.3D));
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);

            int bottomY = anchor.getY() + HILL_HEIGHT;   // start above the highest a hill could reach here
            int scanned = 0;
            while (!kosmos.getBlockState(new BlockPos(anchor.getX() + dx, bottomY - 1, anchor.getZ() + dz)).isAir()
                    && scanned < ISLAND_MAX_DEPTH + HILL_HEIGHT + 5) {
                bottomY--;
                scanned++;
            }

            BlockPos drip = new BlockPos(anchor.getX() + dx, bottomY, anchor.getZ() + dz);
            kosmos.setBlock(drip, chocolate, 3);
            for (int down = 1; down <= DRIP_LENGTH; down++) {
                kosmos.setBlock(drip.below(down), chocolate, 3);
            }
        }
    }

    /**
     * Places the imported castle so its open courtyard shaft lands exactly
     * on {@code anchor}, flush with the island's flat top.
     */
    private static void placeCastle(ServerLevel kosmos, BlockPos anchor) {
        StructureTemplateManager manager = kosmos.getStructureManager();
        Optional<StructureTemplate> template = manager.get(CASTLE_ID);
        if (template.isEmpty()) {
            return;   // missing/corrupt structure file - nothing to place
        }

        BlockPos origin = anchor.offset(-BOSS_LOCAL_X, 0, -BOSS_LOCAL_Z);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKeepLiquids(false);
        template.get().placeInWorld(kosmos, origin, origin, settings, kosmos.getRandom(), 2);

        // Declare it off-limits to the fight that happens on top of it. This is
        // the whole reason the castle used to lose walls: the courtyard IS the
        // boss arena, and the arena is full of things that destroy terrain.
        // Taken from the template's real size rather than from the constants
        // above, so the protected volume cannot drift away from the building.
        Vec3i size = template.get().getSize();
        KraveArena.protectCastle(kosmos, new BoundingBox(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + size.getX(), origin.getY() + size.getY(), origin.getZ() + size.getZ()));
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
    private static void spawnGuardianBox(ServerLevel kosmos, BlockPos anchor) {
        var bossId = KraveKosmosData.get(kosmos).getBossId();
        KraveMonster boss = bossId != null && kosmos.getEntity(bossId) instanceof KraveMonster m ? m : null;

        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(kosmos);
        if (box == null) {
            return;
        }
        BlockPos pos = anchor.above(1);
        box.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        box.setElite(true);
        if (boss != null) {
            box.setHealTarget(boss);
        }
        kosmos.addFreshEntity(box);
    }
}
