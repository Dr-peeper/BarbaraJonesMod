package com.barbarajones.block.krave;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Every block of the Krave tree, held as plain instances.
 *
 * <p>They live here rather than inline in {@code ModBlocks} for one reason: the
 * tree feature, the pod and the sapling grower all need to name these blocks, and
 * routing that through {@code RegistryObject} fields would make this package
 * depend on the exact field names someone else picked. Instances are safe to
 * build outside a registry event - a {@link Block} constructor only assembles its
 * state definition - and {@code ModBlocks} registers each one by supplier.
 *
 * <p>Vanilla {@code BlockSetType.OAK} and {@code WoodType.OAK} are reused for the
 * door, gate, button and plate. Registering a custom set means hooking the
 * bootstrap before block registration runs, and the only thing it would buy here
 * is a different creak.
 */
public final class KraveWood {

    private KraveWood() { }

    private static BlockBehaviour.Properties trunk(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    // Declared before the un-stripped pair so the strip suppliers below read in
    // dependency order; the lambdas resolve lazily either way.

    public static final RotatedPillarBlock STRIPPED_LOG =
            new KraveLogBlock(trunk(MapColor.TERRACOTTA_ORANGE), null);

    public static final RotatedPillarBlock STRIPPED_WOOD =
            new KraveLogBlock(trunk(MapColor.TERRACOTTA_ORANGE), null);

    public static final RotatedPillarBlock LOG =
            new KraveLogBlock(trunk(MapColor.COLOR_BROWN), () -> STRIPPED_LOG);

    public static final RotatedPillarBlock WOOD =
            new KraveLogBlock(trunk(MapColor.COLOR_BROWN), () -> STRIPPED_WOOD);

    // No LEAVES block here on purpose. The other branch registers its own
    // krave_leaves and that one won the id clash, so this class must not build
    // a second leaves block: a Block that is constructed but never registered
    // leaves an unregistered intrusive holder behind and crashes the game on
    // load. Use ModBlocks.KRAVE_LEAVES.
    public static final SaplingBlock SAPLING = new SaplingBlock(new KraveTreeGrower(),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.DESTROY));

    public static final Block PLANKS = new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .instrument(NoteBlockInstrument.BASS)
            .strength(2.0F, 3.0F)
            .sound(SoundType.WOOD)
            .ignitedByLava());

    public static final StairBlock STAIRS =
            new StairBlock(() -> PLANKS.defaultBlockState(), BlockBehaviour.Properties.copy(PLANKS));

    public static final SlabBlock SLAB = new SlabBlock(BlockBehaviour.Properties.copy(PLANKS));

    public static final FenceBlock FENCE = new FenceBlock(BlockBehaviour.Properties.copy(PLANKS));

    public static final FenceGateBlock FENCE_GATE =
            new FenceGateBlock(BlockBehaviour.Properties.copy(PLANKS), WoodType.OAK);

    public static final DoorBlock DOOR = new DoorBlock(BlockBehaviour.Properties.copy(PLANKS)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY), BlockSetType.OAK);

    public static final TrapDoorBlock TRAPDOOR = new TrapDoorBlock(BlockBehaviour.Properties.copy(PLANKS)
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false), BlockSetType.OAK);

    public static final ButtonBlock BUTTON = new ButtonBlock(BlockBehaviour.Properties.of()
            .noCollission()
            .strength(0.5F)
            .sound(SoundType.WOOD)
            .pushReaction(PushReaction.DESTROY), BlockSetType.OAK, 30, true);

    public static final PressurePlateBlock PRESSURE_PLATE = new PressurePlateBlock(
            PressurePlateBlock.Sensitivity.EVERYTHING,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .forceSolidOn()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.DESTROY),
            BlockSetType.OAK);

    /** Grows on the side of a trunk and is the mod's cocoa supply. See {@link KravePodBlock}. */
    public static final KravePodBlock POD = new KravePodBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .randomTicks()
            .strength(0.2F, 3.0F)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY));
}
