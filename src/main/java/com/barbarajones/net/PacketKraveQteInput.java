package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server: the player pressed the finisher key.
 *
 * <p>Deliberately carries nothing at all - not the boss, not the form, not
 * whether the press was in time. A payload here is a payload a modified client
 * can lie in, and every field would be one more claim the server has to
 * disprove. The whole message is "a key went down"; the server looks up
 * whether that player is standing in a live prompt and decides for itself.
 *
 * <p>Everything this could be abused for is therefore already handled on the
 * far side: {@code KraveKosmosBattle.onQteInput} ignores it unless there is an
 * open window for that specific player, and consuming the window is the first
 * thing it does, so spamming the key cannot fire the finisher twice.
 */
public class PacketKraveQteInput {

    public PacketKraveQteInput() { }

    public static void encode(PacketKraveQteInput msg, FriendlyByteBuf buf) { }

    public static PacketKraveQteInput decode(FriendlyByteBuf buf) {
        return new PacketKraveQteInput();
    }

    public static void handle(PacketKraveQteInput msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                com.barbarajones.apocalypse.KraveKosmosBattle.onQteInput(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
