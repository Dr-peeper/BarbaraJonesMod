package com.barbarajones.block.krave;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Krave Ore - a crystallised sugar seam that drops Krave Dust.
 *
 * <p>Nothing new was invented for the drop: {@code ModItems.KRAVE_DUST} already
 * existed with exactly one use (Off-Brand Krave), so pointing the ore at it turns
 * a dead-end item into the mining reward, and the new dust-to-cereal recipe closes
 * the loop back to the one resource the run actually depends on - keeping Cayden
 * fed.
 *
 * <p>{@link DropExperienceBlock} rather than a plain {@code Block} so the seam
 * pays experience on break the way every other ore in the game does; the
 * {@code UniformInt} is the XP range, not the item count, which comes from the
 * loot table.
 *
 * <p>The faint light level is deliberate. A cave that occasionally glows amber is
 * the cheapest way to make the underground read as this mod's world rather than
 * plain vanilla with extra recipes.
 */
public final class KraveOre {

    private KraveOre() { }

    private static final int GLOW = 3;

    public static final DropExperienceBlock ORE = new DropExperienceBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> GLOW)
                    .requiresCorrectToolForDrops(),
            UniformInt.of(2, 5));

    public static final DropExperienceBlock DEEPSLATE_ORE = new DropExperienceBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> GLOW)
                    .requiresCorrectToolForDrops(),
            UniformInt.of(2, 5));
}
