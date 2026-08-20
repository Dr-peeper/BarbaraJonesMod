package com.barbarajones.v2.mobs;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.block.MilkWebbingBlock;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's OWN block registry: just the sticky milk webbing that
 * {@link com.barbarajones.v2.mobs.entity.ai.LoomweaverWebTrapGoal} places.
 *
 * <p>Deliberately has NO BlockItem and NO creative-tab entry - it is placed
 * by Loomweaver only, never obtained by a player. That is intentional, not a
 * gap; see docs/modules/craveling-mobs.md.
 */
public final class ModMobBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BarbaraJonesMod.MODID);

    private ModMobBlocks() { }

    public static final RegistryObject<Block> MILK_WEBBING =
            BLOCKS.register("milk_webbing", MilkWebbingBlock::new);
}
