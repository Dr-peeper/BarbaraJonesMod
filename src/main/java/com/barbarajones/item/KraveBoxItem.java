package com.barbarajones.item;

import com.barbarajones.content.ModEntities;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An empty Krave box. Crush it to summon THE KRAVE MONSTER - but only once
 * Barbara and Cayden are both on your side, AND only after the Kosmos's own
 * resident Krave Monster has been beaten at least once. Summoning one here
 * before that would let a player fight (and win) the boss without ever
 * setting foot in the Kosmos at all, which undercuts the entire point of
 * getting there.
 */
public class KraveBoxItem extends Item {

    public KraveBoxItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!Quests.isUnlocked(player, Quests.SUMMON_KRAVE)) {
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "[Krave Quest] "
                    + ChatFormatting.GRAY + "Nothing happens. Recruit Barbara and stock a full bowl first."));
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel kosmos = level.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null || !KraveKosmosData.get(kosmos).isBossEverDefeated()) {
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "[Krave Quest] "
                    + ChatFormatting.GRAY + "Nothing happens. The Krave Monster in the Kosmos hasn't fallen yet."));
            return InteractionResultHolder.fail(stack);
        }

        double yaw = Math.toRadians(player.getYRot());
        double x = player.getX() - Math.sin(yaw) * 4.0D;
        double z = player.getZ() + Math.cos(yaw) * 4.0D;

        // Always a fresh Krave Monster, even if one is already alive in the
        // Kosmos - crushing another box summons another one, full stop, not
        // "borrow the existing one if there's not already an encounter."
        KraveMonster monster = ModEntities.KRAVE_MONSTER.get().create(level);
        if (monster != null) {
            monster.moveTo(x, player.getY() + 1.0D, z, player.getYRot() + 180.0F, 0.0F);
            level.addFreshEntity(monster);
            monster.setForm(com.barbarajones.EventHandler.nextKraveForm(player));
            // Awake and hostile on arrival. This summon is its own encounter,
            // not the scripted Kosmos one - there is no confrontation coming to
            // wake it, so a dormant Monster here would just stand there.
            monster.spawnHostile();
            monster.setTarget(player);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.2F);
        Quests.complete(player, Quests.SUMMON_KRAVE);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
