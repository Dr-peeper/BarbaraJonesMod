package com.barbarajones.block;

import com.barbarajones.menu.KraftingBenchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * A red Krave Box turned workbench. Right-click opens KraftingBenchMenu,
 * whose three fixed, type-restricted slots are the only way to combine a
 * Krave Pickaxe, Axe and Shovel into the Krave Multitool - an ordinary
 * crafting table can't do it, no matter how they're arranged there.
 */
public class KraftingBenchBlock extends Block {

    public KraftingBenchBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new Provider(pos), pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private record Provider(BlockPos pos) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.literal("Krafting Bench");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new KraftingBenchMenu(id, inv, ContainerLevelAccess.create(player.level(), pos));
        }
    }
}
