package com.barbarajones.v2.build.net;

import com.barbarajones.v2.build.item.KraveSchematicItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "Turn the building." Sent when the player left-clicks or presses the rotate
 * key while holding an armed schematic.
 *
 * <p>The rotation lives in the stack's NBT rather than in a client-side field
 * so the server places what the preview promised, and so the choice survives
 * switching hotbar slots.
 */
public class PacketRotateSchematic {

    private final int delta;

    public PacketRotateSchematic(int delta) {
        this.delta = delta;
    }

    public static void encode(PacketRotateSchematic msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.delta);
    }

    public static PacketRotateSchematic decode(FriendlyByteBuf buf) {
        return new PacketRotateSchematic(buf.readVarInt());
    }

    public static void handle(PacketRotateSchematic msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // Clamp: the only legitimate values are a single quarter turn either
            // way, and a client is never trusted to send anything else.
            int delta = msg.delta >= 0 ? 1 : -1;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (!(stack.getItem() instanceof KraveSchematicItem) || !KraveSchematicItem.armed(stack)) {
                    continue;
                }
                KraveSchematicItem.setTurns(stack, KraveSchematicItem.turns(stack) + delta);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ROTATE_ITEM,
                        SoundSource.PLAYERS, 0.6F, 1.4F);
                return;
            }
        });
        context.setPacketHandled(true);
    }
}
