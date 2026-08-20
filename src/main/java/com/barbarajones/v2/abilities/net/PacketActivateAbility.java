package com.barbarajones.v2.abilities.net;

import com.barbarajones.v2.abilities.AbilityData;
import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.v2.abilities.item.AbilityItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: "activate whichever ability is bound to this keybind
 * slot." Carries only the {@link AbilityId} index - never an item slot or a
 * stack - because the player might be carrying the matching item anywhere in
 * their inventory, not just in hand. That is what "equipped" means for this
 * system: carried, not held.
 *
 * <p>The server re-derives everything: which item that maps to, whether the
 * player actually has one, whether it is unlocked, whether it is off
 * cooldown. A malicious or desynced client can send garbage here and the
 * worst it does is a no-op.
 */
public class PacketActivateAbility {

    public final int abilityIndex;

    public PacketActivateAbility(int abilityIndex) {
        this.abilityIndex = abilityIndex;
    }

    public static void encode(PacketActivateAbility msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.abilityIndex);
    }

    public static PacketActivateAbility decode(FriendlyByteBuf buf) {
        return new PacketActivateAbility(buf.readVarInt());
    }

    public static void handle(PacketActivateAbility msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            AbilityId id = AbilityId.byIndex(msg.abilityIndex);
            if (id == null) {
                return;
            }
            ItemStack found = findCarried(player, id);
            if (found.isEmpty()) {
                return;   // nothing bound to that slot in their inventory right now - silent
            }
            AbilityData.activate(player, (AbilityItem) found.getItem(), found);
        });
        context.setPacketHandled(true);
    }

    /** The first matching ability item anywhere in the player's inventory or offhand. */
    private static ItemStack findCarried(ServerPlayer player, AbilityId id) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof AbilityItem ai && ai.ability() == id) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && stack.getItem() instanceof AbilityItem ai && ai.ability() == id) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
