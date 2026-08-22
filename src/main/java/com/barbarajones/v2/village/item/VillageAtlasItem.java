package com.barbarajones.v2.village.item;

import com.barbarajones.v2.village.KraveVillage;
import com.barbarajones.v2.village.VillageView;
import com.barbarajones.v2.village.net.PacketVillageStatus;
import com.barbarajones.v2.village.net.VillageNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import java.util.Optional;

/**
 * The Village Atlas. Opens the full settlement screen.
 *
 * <p>Both halves fire on the same use. The server sends a fresh status packet for
 * whichever settlement is relevant - the one the player is standing in, or the
 * nearest one if they are outside every claim - and the client opens the screen
 * immediately without waiting for it. The screen re-reads the cached status every
 * frame, so the numbers fill in a tick later rather than the screen having to be
 * opened twice.
 *
 * <p>Reusable, never consumed, and it works from anywhere. A settlement read-out
 * that only functions while standing inside the settlement is a read-out for a
 * problem you do not have.
 */
public class VillageAtlasItem extends Item {

    public VillageAtlasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            Optional<VillageView> view = KraveVillage.containing(server, player.blockPosition());
            if (view.isEmpty()) {
                view = KraveVillage.nearest(server, player.blockPosition());
            }
            VillageNetwork.sendTo(serverPlayer, PacketVillageStatus.of(view.orElse(null)));
        }

        // Calling a same-named static method on a genuinely separate class
        // (see VillageScreen.openIfNone's own doc comment for exactly why
        // this has to be a separate class and not a lambda body written
        // here) - the isClientSide guard means this is never reached at
        // all server-side, so that class's own Minecraft/Screen references
        // are never touched there either.
        if (level.isClientSide) {
            com.barbarajones.v2.village.client.VillageScreen.openIfNone();
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("village.barbarajones.atlas_tip")
                .withStyle(ChatFormatting.GRAY));
    }
}
