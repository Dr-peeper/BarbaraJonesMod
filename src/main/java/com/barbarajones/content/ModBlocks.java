package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.block.KraftingBenchBlock;
import com.barbarajones.block.KraveDirtBlock;
import com.barbarajones.block.KraveDoorBlock;
import com.barbarajones.block.KraveGrassBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
                    // Obsidian's own hardness (50) and blast resistance (1200) -
                    // it was 6 before, nowhere near obsidian-tough despite the
                    // portal-frame role. requiresCorrectToolForDrops() alone
                    // doesn't gate the tool TIER though - see the
                    // needs_diamond_tool tag addition for that half.
                    .strength(50.0F, 1200.0F)
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
    // the overworld blocks they're standing in for, just the Kosmos texture. ---

    public static final RegistryObject<Block> KRAVE_GRASS = BLOCKS.register("krave_grass",
            () -> new KraveGrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK).randomTicks()));

    public static final RegistryObject<Block> KRAVE_DIRT = BLOCKS.register("krave_dirt",
            () -> new KraveDirtBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).randomTicks()));

    // ---- Chocolate trees: vanilla log/leaves behavior, reskinned -----------

    public static final RegistryObject<Block> CHOCOLATE_LOG = BLOCKS.register("chocolate_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));

    public static final RegistryObject<Block> KRAVE_LEAVES = BLOCKS.register("krave_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));

    // ---- Krafting Bench: a red Krave Box that combines pickaxe+axe+shovel ---

    public static final RegistryObject<Block> KRAFTING_BENCH = BLOCKS.register("krafting_bench",
            () -> new KraftingBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.WOOD)));
}
