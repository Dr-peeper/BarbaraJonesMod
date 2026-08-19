package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

/**
 * The Krave Radio. A pocket set with ten stations, all of them the same show.
 *
 * <p>Right-click steps to the next station and plays it; sneak-right-click
 * announces what is on without playing it. The dial position rides in the
 * stack's NBT, so two radios can sit on different stations.
 */
public class KraveRadioItem extends Item {

    private static final String DIAL_TAG = "KraveDial";

    /** Station names, indexed in step with {@link #station(int)}. */
    private static final String[] STATION_NAMES = {
        "94.1 THE GRASS", "Krave FM", "Chepina Radio", "The Lighter Hour",
        "Management Talk", "Democrat Drive-Time", "House Hunters Live",
        "The Golden Arches Report", "Roll Call", "STATIC"
    };

    public KraveRadioItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Tuned to "
                + ChatFormatting.GOLD + STATION_NAMES[dial(stack)]));
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "Sneak-click to read the dial."));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            player.displayClientMessage(Component.literal(ChatFormatting.GOLD
                    + "Dial: " + STATION_NAMES[dial(stack)]), true);
            return InteractionResultHolder.consume(stack);
        }

        int next = (dial(stack) + 1) % STATION_NAMES.length;
        stack.getOrCreateTag().putInt(DIAL_TAG, next);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                station(next), SoundSource.RECORDS, 1.6F, 1.0F);
        player.displayClientMessage(Component.literal(ChatFormatting.LIGHT_PURPLE
                + "Now playing: " + ChatFormatting.WHITE + STATION_NAMES[next]), true);
        player.getCooldowns().addCooldown(this, 30);
        return InteractionResultHolder.consume(stack);
    }

    private static int dial(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return 0;
        }
        int value = tag.getInt(DIAL_TAG);
        // Clamp rather than trust: a hand-edited NBT value would otherwise index
        // straight off the end of the name table.
        return value < 0 || value >= STATION_NAMES.length ? 0 : value;
    }

    private static SoundEvent station(int index) {
        RegistryObject<SoundEvent> track = switch (index) {
            case 0 -> ModSounds.EVT_OG;
            case 1 -> ModSounds.EVT_MUSIC;
            case 2 -> ModSounds.EVT_CHEPINA;
            case 3 -> ModSounds.EVT_LIGHTER;
            case 4 -> ModSounds.EVT_MANAGER;
            case 5 -> ModSounds.EVT_DEMOCRAT;
            case 6 -> ModSounds.EVT_HOUSE;
            case 7 -> ModSounds.EVT_MCD;
            case 8 -> ModSounds.EVT_ROLL;
            default -> ModSounds.KRAVE_VOICE;
        };
        return track.get();
    }
}
