package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * A trophy taken off something that used to be a problem. Purely a keepsake -
 * hold one up and it plays back the noise the thing made, which is the whole
 * appeal of a trophy.
 */
public class TrophyItem extends Item {

    private final String boast;
    private final Supplier<SoundEvent> roar;

    public TrophyItem(Properties props, String boast, RegistryObject<SoundEvent> roar) {
        super(props);
        this.boast = boast;
        this.roar = roar::get;
    }

    /** Trophies for things that never made a noise fall back to the Krave laugh. */
    public TrophyItem(Properties props, String boast) {
        this(props, boast, ModSounds.KRAVE_LAUGH);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GOLD + boast));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            for (int i = 0; i < 10; i++) {
                level.addParticle(ParticleTypes.END_ROD,
                        player.getX() + (level.random.nextDouble() - 0.5D),
                        player.getY() + 1.2D + level.random.nextDouble() * 0.5D,
                        player.getZ() + (level.random.nextDouble() - 0.5D),
                        0.0D, 0.02D, 0.0D);
            }
            return InteractionResultHolder.success(stack);
        }
        level.playSound(null, player.blockPosition(), roar.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal(ChatFormatting.GOLD + boast), true);
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.consume(stack);
    }
}
