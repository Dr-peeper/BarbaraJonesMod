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

    /**
     * Which form the prompt on screen was for.
     *
     * <p>The one field, and it is not trusted - it is compared against the form
     * the server is actually asking about and the press is dropped if they
     * disagree. That turns a late keypress from something that answers the NEXT
     * form into something that answers nothing.
     */
    private final int form;

    /**
     * Which attack of that form the prompt was for.
     *
     * <p>The pair identifies the prompt exactly. Neither is trusted - both are
     * compared against what the server is actually asking and the press is
     * dropped if either disagrees. That turns a late keypress from something
     * that answers the NEXT attack into something that answers nothing, which
     * in a six-part finisher is the difference between skipping a move and
     * ending the whole encounter early.
     */
    private final int step;

    public PacketKraveQteInput(int form, int step) {
        this.form = form;
        this.step = step;
    }

    public static void encode(PacketKraveQteInput msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.form);
        buf.writeVarInt(msg.step);
    }

    public static PacketKraveQteInput decode(FriendlyByteBuf buf) {
        return new PacketKraveQteInput(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(PacketKraveQteInput msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                com.barbarajones.apocalypse.KraveKosmosBattle.onQteInput(sender, msg.form, msg.step);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
