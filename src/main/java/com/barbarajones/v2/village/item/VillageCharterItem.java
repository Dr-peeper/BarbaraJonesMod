package com.barbarajones.v2.village.item;

import com.barbarajones.v2.village.KraveVillage;
import com.barbarajones.v2.village.KraveVillageData;
import com.barbarajones.v2.village.Village;
import com.barbarajones.v2.village.VillageView;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * The Village Charter. Right-click a block to found a settlement centred on it.
 *
 * <p>Founding is the only way a village comes into existence. There is no automatic
 * "you placed enough beds, have a village" detection, and that is deliberate: the
 * player has to be able to say <em>where</em> the centre is, because the claim is
 * built around it and everything from attraction spawns to the sweep is measured
 * from it. Guessing would put half the player's base outside their own town.
 *
 * <p>Planting inside an existing claim is refused rather than silently ignored, and
 * the item is not consumed - two overlapping villages fighting over the same blocks
 * is a bug factory, and eating the item to tell the player "no" is worse.
 */
public class VillageCharterItem extends Item {

    public VillageCharterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        BlockPos origin = context.getClickedPos().above();
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer sp ? sp : null;

        Optional<VillageView> existing = KraveVillage.containing(server, origin);
        if (existing.isPresent()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(
                        "village.barbarajones.charter_occupied",
                        Component.literal(existing.get().name())).withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        String name = player == null
                ? "Krave Village"
                : Component.translatable("village.barbarajones.default_name",
                        player.getGameProfile().getName()).getString();

        KraveVillage.found(server, origin, player, name);

        // The claim is invisible, so the founding has to be loud enough that the
        // player knows exactly where the centre landed.
        server.sendParticles(ParticleTypes.END_ROD, origin.getX() + 0.5D, origin.getY() + 0.5D,
                origin.getZ() + 0.5D, 60, 0.6D, 1.2D, 0.6D, 0.08D);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, origin.getX() + 0.5D, origin.getY() + 1.0D,
                origin.getZ() + 0.5D, 40, 1.6D, 0.8D, 1.6D, 0.02D);
        server.playSound(null, origin, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 0.9F);

        if (player != null) {
            player.displayClientMessage(Component.translatable(
                    "village.barbarajones.charter_founded", Component.literal(name)), false);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        // Kick the settlement once immediately so the HUD has real numbers rather
        // than a village that reads as empty until the next five-second tick.
        KraveVillageData data = KraveVillageData.get(server);
        Village village = data.containing(origin);
        if (village != null && village.tick(server)) {
            data.setDirty();
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("village.barbarajones.charter_tip_1")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("village.barbarajones.charter_tip_2",
                Village.CLAIM_RADIUS * 2 + 1).withStyle(ChatFormatting.DARK_GRAY));
    }
}
