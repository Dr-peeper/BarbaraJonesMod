package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GOLDEN KRAVE. One box in a hundred thousand comes out of the factory like
 * this, and Cayden can smell it through the cardboard.
 *
 * <p>Eating it is good for you, but the real reason to hold one is rule number
 * one: opening a Golden Krave anywhere near Cayden puts him back to full health
 * instantly, no matter how bad the fight has gone. It is the only hard heal for
 * him in the mod, which is why it is this rare.
 */
public class GoldenKraveItem extends Item {

    /** Cayden notices it from further away than he notices anything else. */
    private static final double SMELL_RANGE = 24.0D;

    public GoldenKraveItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GOLD + "\"I KRAVE THE KRAVE!\""));
        tooltip.add(Component.literal(ChatFormatting.GRAY
                + "Fully heals Cayden Cobb when eaten near him."));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide) {
            return result;
        }

        level.playSound(null, entity.blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                SoundSource.PLAYERS, 1.4F, 1.0F);

        int saved = 0;
        for (CaydenCobb cayden : level.getEntitiesOfClass(CaydenCobb.class,
                entity.getBoundingBox().inflate(SMELL_RANGE))) {
            if (cayden.getHealth() < cayden.getMaxHealth()) {
                saved++;
            }
            cayden.setHealth(cayden.getMaxHealth());
        }

        if (entity instanceof Player player) {
            player.sendSystemMessage(Component.literal(saved > 0
                    ? ChatFormatting.GOLD + "Cayden inhales the whole box. He is fine. He is FINE."
                    : ChatFormatting.GOLD + "Golden Krave. Somewhere, a kid just woke up."));
        }
        return result;
    }
}
