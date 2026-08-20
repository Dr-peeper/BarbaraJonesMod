package com.barbarajones.v2.village;

import com.barbarajones.content.ModBlocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The built-in buff table: what the base game and this mod contribute to a
 * settlement out of the box.
 *
 * <p>Installed once from {@code VillageRegistry} during common setup, so it lands
 * after registries have thawed and <em>before</em> any world exists. Other modules
 * add to it with {@link KraveVillage#registerVillageBuff}; a later registration for
 * the same block wins, so this table is a default rather than a rule.
 *
 * <p><b>The design intent.</b> A village should grow because the player built a
 * place worth living in, not because they placed one magic totem. So the numbers
 * here are small and broad: a bed is worth more than a plank, a door and a roof and
 * a light together make a house, and a wall of cobble is worth almost nothing until
 * there is something behind it. Nothing in this table is worth more than three
 * building points, and the tier requirements are set on the assumption that a real
 * village is dozens of ordinary blocks rather than a handful of special ones.
 *
 * <p>Only blocks listed here (or registered by another module) are tracked at all,
 * which is what keeps the per-village sweep cheap: the map holds beds and doors and
 * workstations, not every cobblestone in the claim.
 */
public final class VillageBuffs {

    private static boolean installed;

    private VillageBuffs() { }

    /**
     * Registers the whole base table. Idempotent - calling it twice is harmless,
     * which matters because a dedicated server and an integrated one reach common
     * setup by slightly different routes.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;

        housing();
        work();
        light();
        defence();
        kraveIndustry();
        comfort();
    }

    // ---- housing: the thing that actually attracts people --------------------

    private static void housing() {
        // A bed is a resident. It is the single strongest attraction signal in the
        // table, and every vanilla bed colour counts the same.
        VillageBuff bed = VillageBuff.builder()
                .building(2).attraction(6).happiness(2)
                .description("village.barbarajones.buff.bed").build();
        for (Block block : new Block[]{
                Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED,
                Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED,
                Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED,
                Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED}) {
            KraveVillage.registerVillageBuff(block, bed);
        }

        // A door is what turns four walls into a building.
        VillageBuff door = VillageBuff.builder()
                .building(1).attraction(2).happiness(1)
                .description("village.barbarajones.buff.door").build();
        for (Block block : new Block[]{
                Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
                Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR, Blocks.CHERRY_DOOR,
                Blocks.BAMBOO_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR, Blocks.IRON_DOOR}) {
            KraveVillage.registerVillageBuff(block, door);
        }
        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_DOOR, door);
    }

    // ---- workstations: what makes a village more than a dormitory ------------

    private static void work() {
        VillageBuff bench = VillageBuff.builder()
                .building(1).production(1).attraction(2)
                .description("village.barbarajones.buff.workstation").build();
        for (Block block : new Block[]{
                Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
                Blocks.SMITHING_TABLE, Blocks.FLETCHING_TABLE, Blocks.CARTOGRAPHY_TABLE,
                Blocks.LOOM, Blocks.STONECUTTER, Blocks.GRINDSTONE, Blocks.BARREL,
                Blocks.CAULDRON, Blocks.COMPOSTER, Blocks.BREWING_STAND}) {
            KraveVillage.registerVillageBuff(block, bench);
        }

        // Storage. Worth a little, capped in practice by the tracking limit.
        KraveVillage.registerVillageBuff(Blocks.CHEST, VillageBuff.builder()
                .building(1).production(1)
                .description("village.barbarajones.buff.storage").build());
        KraveVillage.registerVillageBuff(Blocks.TRAPPED_CHEST, VillageBuff.builder()
                .building(1).defence(1)
                .description("village.barbarajones.buff.storage").build());

        // The mod's own bench is the best workstation in the game, obviously.
        KraveVillage.registerVillageBuff(ModBlocks.KRAFTING_BENCH, VillageBuff.builder()
                .building(2).production(4).attraction(3).happiness(2)
                .description("village.barbarajones.buff.krafting_bench").build());

        KraveVillage.registerVillageBuff(Blocks.BELL, VillageBuff.builder()
                .building(1).defence(2).attraction(4).happiness(3)
                .description("village.barbarajones.buff.bell").build());
    }

    // ---- light: a dark village is a raid waiting to happen -------------------

    private static void light() {
        VillageBuff lamp = VillageBuff.builder()
                .defence(1).happiness(1)
                .description("village.barbarajones.buff.light").build();
        for (Block block : new Block[]{
                Blocks.TORCH, Blocks.WALL_TORCH, Blocks.LANTERN, Blocks.SOUL_LANTERN,
                Blocks.CAMPFIRE, Blocks.SEA_LANTERN, Blocks.GLOWSTONE, Blocks.SHROOMLIGHT,
                Blocks.JACK_O_LANTERN, Blocks.END_ROD}) {
            KraveVillage.registerVillageBuff(block, lamp);
        }
    }

    // ---- fortification -------------------------------------------------------

    private static void defence() {
        KraveVillage.registerVillageBuff(Blocks.IRON_BARS, VillageBuff.fortification(1));
        KraveVillage.registerVillageBuff(Blocks.OBSIDIAN, VillageBuff.builder()
                .defence(2).description("village.barbarajones.buff.wall").build());

        VillageBuff wall = VillageBuff.builder()
                .defence(1).description("village.barbarajones.buff.wall").build();
        for (Block block : new Block[]{
                Blocks.COBBLESTONE_WALL, Blocks.STONE_BRICK_WALL, Blocks.DEEPSLATE_BRICK_WALL,
                Blocks.BLACKSTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL, Blocks.ANDESITE_WALL}) {
            KraveVillage.registerVillageBuff(block, wall);
        }

        // Chocolate fencing is this mod's palisade.
        KraveVillage.registerVillageBuff(ModBlocks.CHOCOLATE_FENCE, VillageBuff.builder()
                .defence(2).happiness(1)
                .description("village.barbarajones.buff.chocolate_fence").build());
        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_FENCE, VillageBuff.builder()
                .defence(2)
                .description("village.barbarajones.buff.krave_fence").build());
        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_FENCE_GATE, VillageBuff.builder()
                .building(1).defence(2)
                .description("village.barbarajones.buff.krave_fence").build());
    }

    // ---- the Krave economy ---------------------------------------------------

    private static void kraveIndustry() {
        // The centre of the whole system. A solid block of compressed Krave is a
        // granary, and it is the only thing in the base table worth real production.
        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_BLOCK, VillageBuff.builder()
                .building(3).production(12).attraction(5).happiness(4)
                .description("village.barbarajones.buff.krave_block").build());

        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_POD, VillageBuff.builder()
                .production(4).attraction(1)
                .description("village.barbarajones.buff.krave_pod").build());

        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_LOG, VillageBuff.builder()
                .production(1)
                .description("village.barbarajones.buff.krave_log").build());
        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_LEAVES, VillageBuff.builder()
                .production(1).happiness(1)
                .description("village.barbarajones.buff.krave_leaves").build());
        KraveVillage.registerVillageBuff(ModBlocks.CHOCOLATE_LOG, VillageBuff.builder()
                .production(2)
                .description("village.barbarajones.buff.chocolate_log").build());

        KraveVillage.registerVillageBuff(ModBlocks.KRAVE_GRASS, VillageBuff.builder()
                .production(1).happiness(1)
                .description("village.barbarajones.buff.krave_grass").build());

        KraveVillage.registerVillageBuff(ModBlocks.GRASS_CROP, VillageBuff.builder()
                .production(1).happiness(1)
                .description("village.barbarajones.buff.grass_crop").build());
    }

    // ---- the reason anyone stays ---------------------------------------------

    private static void comfort() {
        KraveVillage.registerVillageBuff(ModBlocks.RECLINER, VillageBuff.builder()
                .building(1).happiness(4).attraction(2)
                .description("village.barbarajones.buff.recliner").build());
        KraveVillage.registerVillageBuff(ModBlocks.TELEVISION, VillageBuff.builder()
                .building(1).happiness(3).attraction(2)
                .description("village.barbarajones.buff.television").build());
        KraveVillage.registerVillageBuff(ModBlocks.BOOMBOX, VillageBuff.builder()
                .happiness(2).attraction(1)
                .description("village.barbarajones.buff.boombox").build());
        KraveVillage.registerVillageBuff(ModBlocks.SHAG_CARPET, VillageBuff.builder()
                .happiness(1)
                .description("village.barbarajones.buff.carpet").build());
        KraveVillage.registerVillageBuff(ModBlocks.WOOD_PANELING, VillageBuff.builder()
                .happiness(1)
                .description("village.barbarajones.buff.paneling").build());

        // The stash box is exactly as good for morale as it sounds, and exactly as
        // bad for the neighbourhood.
        KraveVillage.registerVillageBuff(ModBlocks.STASH_BOX, VillageBuff.builder()
                .building(1).happiness(5).attraction(3).defence(-1)
                .description("village.barbarajones.buff.stash_box").build());

        KraveVillage.registerVillageBuff(Blocks.FLOWER_POT, VillageBuff.builder()
                .happiness(1).description("village.barbarajones.buff.decor").build());
        KraveVillage.registerVillageBuff(Blocks.HAY_BLOCK, VillageBuff.builder()
                .production(1).description("village.barbarajones.buff.hay").build());
    }
}
