package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.block.KraveDoorBlock;
import com.barbarajones.block.KraveGrassBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
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
            () -> new KraveGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.STONE)
                    .randomTicks()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> KRAVE_DIRT = BLOCKS.register("krave_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .sound(SoundType.NETHERRACK)));
}
