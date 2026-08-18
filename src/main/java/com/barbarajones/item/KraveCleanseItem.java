package com.barbarajones.item;

import com.barbarajones.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The escape hatch for endless death-stage mode - see Config.ALLOW_KRAVE_CLEANSE.
 * Off by default, so this does nothing out of the box; the item still exists and
 * is still craftable either way; the config just decides whether using it works.
 */
public class KraveCleanseItem extends Item {

    private static final String PERSIST = Player.PERSISTED_NBT_TAG;

    public KraveCleanseItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        if (!Config.ALLOW_KRAVE_CLEANSE.get()) {
            player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                    + "It does not stop now. It never stops now."));
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag persist = persisted(player);
        if (!persist.getBoolean("KraveEndless")) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                    + "You don't need this. Not yet, anyway."));
            return InteractionResultHolder.fail(stack);
        }

        persist.putBoolean("KraveEndless", false);
        persist.putInt("KraveDeathStage", 10);   // stays at the max non-endless stage
        player.sendSystemMessage(Component.literal(ChatFormatting.GREEN + ""
                + ChatFormatting.BOLD + "It stops now."));
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                + "The eleventh death no longer waits for you."));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    private CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSIST)) {
            data.put(PERSIST, new CompoundTag());
        }
        return data.getCompound(PERSIST);
    }
}
