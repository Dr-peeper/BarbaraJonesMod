package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.block.KraveDoorBlock;

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

    // ---- Krave Kosmos terrain: coherent grass/dirt pair, vanilla-style ------

    public static final RegistryObject<Block> KRAVE_GRASS = BLOCKS.register("krave_grass",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> KRAVE_DIRT = BLOCKS.register("krave_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .sound(SoundType.NETHERRACK)));

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
}
