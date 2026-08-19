package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.block.KraveDirtBlock;
import com.barbarajones.block.KraveDoorBlock;
import com.barbarajones.block.KraveGrassBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import com.barbarajones.content.extra.BoomboxBlock;
import com.barbarajones.content.extra.ReclinerBlock;
import com.barbarajones.content.extra.StashBoxBlock;
import com.barbarajones.content.extra.TelevisionBlock;
import net.minecraft.world.level.material.MapColor;
import com.barbarajones.block.KraftingBenchBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Blocks. Currently just the Krave Kosmos portal frame - the mod's first custom blocks. */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BarbaraJonesMod.MODID);

    private ModBlocks() { }

    public static final RegistryObject<Block> KRAVE_BLOCK = BLOCKS.register("krave_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(6.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> KRAVE_DOOR = BLOCKS.register("krave_door",
            () -> new KraveDoorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(6.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .requiresCorrectToolForDrops(), BlockSetType.OAK));

    public static final RegistryObject<LiquidBlock> CHOCOLATE_BLOCK = BLOCKS.register("chocolate",
            () -> new LiquidBlock(() -> (net.minecraft.world.level.material.FlowingFluid) ModFluids.CHOCOLATE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .noCollission()
                            .strength(100.0F)
                            .lightLevel(state -> 6)
                            .sound(SoundType.HONEY_BLOCK)));

    // ---- Krave Kosmos terrain: literal reskins of vanilla dirt/grass, not an
    // original block with its own feel - same hardness, sound, tool rules as
    // the overworld blocks they're standing in for, just the Kosmos texture.
    // (This exact fix was lost once already in a merge that picked an older
    // version of this file - KraveGrassBlock/KraveDirtBlock's growth-direction
    // logic survived on disk unreferenced. If this regresses again, check
    // here first before re-deriving the fix from scratch.) ---------------

    public static final RegistryObject<Block> KRAVE_GRASS = BLOCKS.register("krave_grass",
            () -> new KraveGrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).randomTicks()));

    public static final RegistryObject<Block> KRAVE_DIRT = BLOCKS.register("krave_dirt",
            () -> new KraveDirtBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).randomTicks()));

    // ---- the house and the yard (behaviour in com.barbarajones.content.extra)
    public static final RegistryObject<Block> STASH_BOX = BLOCKS.register("stash_box",
            () -> new StashBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .randomTicks()));
    public static final RegistryObject<Block> BOOMBOX = BLOCKS.register("boombox",
            () -> new BoomboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Block> RECLINER = BLOCKS.register("recliner",
            () -> new ReclinerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));
    public static final RegistryObject<Block> TELEVISION = BLOCKS.register("television",
            () -> new TelevisionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> state.getValue(TelevisionBlock.LIT) ? 11 : 0)));
    public static final RegistryObject<Block> SEWER_PIPE = BLOCKS.register("sewer_pipe",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SHAG_CARPET = BLOCKS.register("shag_carpet",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)));
    public static final RegistryObject<Block> WOOD_PANELING = BLOCKS.register("wood_paneling",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)));

    /** Barbara's crop - plant Grass Seeds on dirt or farmland. */
    public static final RegistryObject<Block> GRASS_CROP = BLOCKS.register("grass_crop",
            () -> new com.barbarajones.block.GrassCropBlock(
                    BlockBehaviour.Properties.copy(Blocks.WHEAT).noOcclusion()));

    // ---- the Krave tree and its ore ---------------------------------------
    // The block objects live in block/krave; these bind them to ids. They were
    // written and then never registered, so the whole tree was dead source.
    public static final RegistryObject<Block> KRAVE_LOG =
            BLOCKS.register("krave_log", () -> com.barbarajones.block.krave.KraveWood.LOG);
    public static final RegistryObject<Block> KRAVE_WOOD =
            BLOCKS.register("krave_wood", () -> com.barbarajones.block.krave.KraveWood.WOOD);
    public static final RegistryObject<Block> STRIPPED_KRAVE_LOG =
            BLOCKS.register("stripped_krave_log", () -> com.barbarajones.block.krave.KraveWood.STRIPPED_LOG);
    public static final RegistryObject<Block> STRIPPED_KRAVE_WOOD =
            BLOCKS.register("stripped_krave_wood", () -> com.barbarajones.block.krave.KraveWood.STRIPPED_WOOD);    public static final RegistryObject<Block> KRAVE_SAPLING =
            BLOCKS.register("krave_sapling", () -> com.barbarajones.block.krave.KraveWood.SAPLING);
    public static final RegistryObject<Block> KRAVE_PLANKS =
            BLOCKS.register("krave_planks", () -> com.barbarajones.block.krave.KraveWood.PLANKS);
    public static final RegistryObject<Block> KRAVE_STAIRS =
            BLOCKS.register("krave_stairs", () -> com.barbarajones.block.krave.KraveWood.STAIRS);
    public static final RegistryObject<Block> KRAVE_SLAB =
            BLOCKS.register("krave_slab", () -> com.barbarajones.block.krave.KraveWood.SLAB);
    public static final RegistryObject<Block> KRAVE_FENCE =
            BLOCKS.register("krave_fence", () -> com.barbarajones.block.krave.KraveWood.FENCE);
    public static final RegistryObject<Block> KRAVE_FENCE_GATE =
            BLOCKS.register("krave_fence_gate", () -> com.barbarajones.block.krave.KraveWood.FENCE_GATE);
    public static final RegistryObject<Block> KRAVE_DOOR_BLOCK =
            BLOCKS.register("krave_door_block", () -> com.barbarajones.block.krave.KraveWood.DOOR);
    public static final RegistryObject<Block> KRAVE_TRAPDOOR =
            BLOCKS.register("krave_trapdoor", () -> com.barbarajones.block.krave.KraveWood.TRAPDOOR);
    public static final RegistryObject<Block> KRAVE_BUTTON =
            BLOCKS.register("krave_button", () -> com.barbarajones.block.krave.KraveWood.BUTTON);
    public static final RegistryObject<Block> KRAVE_PRESSURE_PLATE =
            BLOCKS.register("krave_pressure_plate", () -> com.barbarajones.block.krave.KraveWood.PRESSURE_PLATE);
    public static final RegistryObject<Block> KRAVE_POD =
            BLOCKS.register("krave_pod", () -> com.barbarajones.block.krave.KraveWood.POD);
    public static final RegistryObject<Block> KRAVE_ORE =
            BLOCKS.register("krave_ore", () -> com.barbarajones.block.krave.KraveOre.ORE);
    public static final RegistryObject<Block> DEEPSLATE_KRAVE_ORE =
            BLOCKS.register("deepslate_krave_ore", () -> com.barbarajones.block.krave.KraveOre.DEEPSLATE_ORE);

    // ---- from the other branch: chocolate trees and the Krafting Bench ----
    // His krave_leaves won the id clash with the Krave tree's own leaves block,
    // so the tree now drops and grows through this one.

    public static final RegistryObject<Block> CHOCOLATE_LOG = BLOCKS.register("chocolate_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));

    public static final RegistryObject<Block> KRAVE_LEAVES = BLOCKS.register("krave_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    public static final RegistryObject<Block> CHOCOLATE_PLANKS = BLOCKS.register("chocolate_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));

    public static final RegistryObject<Block> CHOCOLATE_FENCE = BLOCKS.register("chocolate_fence",
            () -> new net.minecraft.world.level.block.FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)));

    public static final RegistryObject<Block> KRAFTING_BENCH = BLOCKS.register("krafting_bench",
            () -> new KraftingBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.WOOD)));
}
