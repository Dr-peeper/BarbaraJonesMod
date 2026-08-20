package com.barbarajones.v2.quests.net;

import com.barbarajones.v2.quests.QuestEngine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The only two things a player can actively ask the quest system to do.
 *
 * <p>Both are re-checked from scratch on the server. The client's copy of quest
 * state is a rendering convenience and is never trusted: a CLAIM for an incomplete
 * quest, an already-claimed quest, or a quest id that does not exist is simply
 * dropped. That is the difference between a book that displays state and a book
 * that decides state, and only one of those is safe.
 */
public class C2SQuestAction {

    public enum Action {
        /** Take the rewards for a completed, unclaimed quest. */
        CLAIM,
        /** Hand over the items a delivery task asks for. */
        DELIVER
    }

    public final Action action;
    public final ResourceLocation quest;

    public C2SQuestAction(Action action, ResourceLocation quest) {
        this.action = action;
        this.quest = quest;
    }

    public static void encode(C2SQuestAction msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeResourceLocation(msg.quest);
    }

    public static C2SQuestAction decode(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        Action[] values = Action.values();
        // A malformed or version-skewed packet must not index out of bounds.
        Action action = ordinal >= 0 && ordinal < values.length ? values[ordinal] : Action.CLAIM;
        return new C2SQuestAction(action, buf.readResourceLocation());
    }

    public static void handle(C2SQuestAction msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            switch (msg.action) {
                case CLAIM -> QuestEngine.claim(player, msg.quest);
                case DELIVER -> QuestEngine.deliver(player, msg.quest);
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }
}
