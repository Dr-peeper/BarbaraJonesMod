package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityData;
import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.v2.abilities.AbilityUnlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A hard-won item that lets the player use one of Cayden's powers.
 *
 * <p>Every ability item is craftable the moment its recipe is unlocked in the
 * recipe book like anything else - the item itself is never gated. What is
 * gated is <em>using</em> it: {@link AbilityData#activate} refuses to fire
 * until the matching quest or boss kill has granted it, so a player who
 * cheeses the crafting table still owns an inert prop, not the power.
 *
 * <p>Two activation paths land in exactly the same place. Right-click works
 * out of the box (no keybind needed to test one in hand); the keybind system
 * in {@code AbilityKeys} sends a packet that calls the same
 * {@link AbilityData#activate} for whichever matching item it finds anywhere
 * in the player's inventory, which is what lets one player carry and fire
 * several of these without juggling hotbar slots.
 */
public abstract class AbilityItem extends Item {

    private final AbilityId ability;

    protected AbilityItem(AbilityId ability, Item.Properties properties) {
        super(properties);
        this.ability = ability;
    }

    public AbilityId ability() {
        return this.ability;
    }

    /**
     * The effect itself. Called only after {@link AbilityData#activate} has
     * already confirmed the unlock and the cooldown - implementations never
     * need to check either again.
     */
    public abstract void activate(ServerPlayer player, ItemStack stack);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer server)) {
            // let the server round-trip decide; a client-side "success" here
            // would swing the arm before we know whether it actually fired
            return InteractionResultHolder.pass(stack);
        }
        boolean fired = AbilityData.activate(server, this, stack);
        return fired ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (level == null || !level.isClientSide) {
            return;   // tooltips only ever render client-side; nothing to show on the server
        }
        if (com.barbarajones.v2.abilities.client.AbilityClientState.isUnlocked(this.ability)) {
            tooltip.add(Component.literal(ChatFormatting.GREEN + "Unlocked"));
        } else {
            tooltip.add(Component.literal(ChatFormatting.RED + "LOCKED"));
            tooltip.add(Component.literal(ChatFormatting.GRAY + AbilityUnlocks.hint(this.ability)));
        }
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY
                + "Cooldown: " + (this.ability.cooldownTicks / 20) + "s"
                + (this.ability.hasActiveWindow() ? "  |  Lasts: " + (this.ability.activeTicks / 20) + "s" : "")));
    }
}
