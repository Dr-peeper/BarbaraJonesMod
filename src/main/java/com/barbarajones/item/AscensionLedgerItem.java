package com.barbarajones.item;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * THE ASCENSION LEDGER - a spiral notebook a runaway kid keeps his training in.
 *
 * <p>Right-clicking Cayden with it opens his upgrade screen; right-clicking the
 * air opens it on whichever Cayden of yours is nearest. The entity itself
 * handles the first case (see {@code CaydenCobb.mobInteract}) because an entity
 * interaction is resolved before the item ever gets its own use call.
 */
public class AscensionLedgerItem extends Item {

    /** How far it will look for a Cayden when used on nothing in particular. */
    private static final double REACH = 20.0D;

    public AscensionLedgerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CaydenCobb near = nearestOwned(level, player);
        if (near == null) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                        + "Nothing to write about. Cayden is not within " + (int) REACH + " blocks."));
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.barbarajones.client.ui.CaydenUpgradeKeys.open(near));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Nullable
    private CaydenCobb nearestOwned(Level level, Player player) {
        AABB box = player.getBoundingBox().inflate(REACH);
        CaydenCobb best = null;
        double bestD = Double.MAX_VALUE;
        for (CaydenCobb c : level.getEntitiesOfClass(CaydenCobb.class, box)) {
            if (!c.isAlive() || !c.isOwnedBy(player)) {
                continue;
            }
            double d = c.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY
                + "Right-click Cayden: open his ascension ledger"));
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY
                + "" + AscensionLadder.MAX + " forms, from Super Saiyan to Ultra Instinct"));
        tooltip.add(Component.literal(ChatFormatting.GOLD
                + "Ki is earned by feeding him and by what dies in front of him."));
    }
}
