package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

/**
 * The flip phone. Right-click to call somebody; you do not get to pick who.
 * Whoever answers says one thing and hangs up.
 */
public class FlipPhoneItem extends Item {

    /** A caller: who it is, what they say, and the clip that plays. */
    private record Call(String who, String line, RegistryObject<SoundEvent> clip) { }

    public FlipPhoneItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        Call call = roll(level.getRandom());
        player.sendSystemMessage(Component.literal(ChatFormatting.YELLOW + "*brrrrp* *brrrrp* ..."));
        player.sendSystemMessage(Component.literal(
                ChatFormatting.AQUA + call.who() + ": " + ChatFormatting.WHITE + "\"" + call.line() + "\""));
        level.playSound(null, player.blockPosition(), call.clip().get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Built fresh per call rather than held in a static table: the RegistryObjects
     * are only safe to resolve once registration has run, and a call can only
     * happen long after that.
     */
    private static Call roll(RandomSource random) {
        return switch (random.nextInt(8)) {
            case 0 -> new Call("Barbara", "Bring me my stuff. Don't make me come out there.",
                    ModSounds.BARBARA_IDLE);
            case 1 -> new Call("Cayden", "I KRAVE THE KRAVE!!", ModSounds.CAYDEN_SHOUT);
            case 2 -> new Call("Daniel", "You need a light? I got a light.", ModSounds.EVT_LIGHTER);
            case 3 -> new Call("The Manager", "You are not on the schedule. You have never been on the schedule.",
                    ModSounds.EVT_MANAGER);
            case 4 -> new Call("Mom Cobb", "Is my son with you? Put him on.", ModSounds.EVT_HOUSE);
            case 5 -> new Call("Duhl Wol", "Five hundred. Tonight. I know where you sleep.",
                    ModSounds.EVT_DEMOCRAT);
            case 6 -> new Call("The Plug", "Yeah it's real. It's the realest. Meet me by the sewer.",
                    ModSounds.EVT_ROLL);
            default -> new Call("Unknown Number", "...", ModSounds.KRAVE_LAUGH);
        };
    }
}
