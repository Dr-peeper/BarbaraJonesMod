package com.barbarajones.v2.quests.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One player's progress, server to client.
 *
 * <p>The client NEVER decides whether a quest is done. It is told. That is the fix
 * for the old book, which recomputed completion locally from a replicated ItemStack
 * and so regularly disagreed with the server about what the player had achieved.
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@code full = true} - everything, sent on login and after a reload.</li>
 *   <li>{@code full = false} - a delta. The completed/claimed/ability sets are small
 *       enough to resend wholesale, but {@link #progress} carries only the counters
 *       that actually moved, which is the part that would otherwise grow without
 *       bound as the pack does.</li>
 * </ul>
 */
public class S2CQuestState {

    public final boolean full;
    public final Set<String> completed;
    public final Set<String> claimed;
    public final Set<String> abilities;
    public final Set<String> schematics;
    public final int villageTier;
    /** taskKey -> progress. Only the entries that changed, unless {@link #full}. */
    public final Map<String, Integer> progress;

    public S2CQuestState(boolean full, Set<String> completed, Set<String> claimed,
                         Set<String> abilities, Set<String> schematics, int villageTier,
                         Map<String, Integer> progress) {
        this.full = full;
        this.completed = completed;
        this.claimed = claimed;
        this.abilities = abilities;
        this.schematics = schematics;
        this.villageTier = villageTier;
        this.progress = progress;
    }

    private static void writeSet(FriendlyByteBuf buf, Set<String> set) {
        buf.writeVarInt(set.size());
        for (String s : set) {
            buf.writeUtf(s);
        }
    }

    private static Set<String> readSet(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Set<String> out = new HashSet<>(Math.max(4, n));
        for (int i = 0; i < n; i++) {
            out.add(buf.readUtf());
        }
        return out;
    }

    public static void encode(S2CQuestState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.full);
        writeSet(buf, msg.completed);
        writeSet(buf, msg.claimed);
        writeSet(buf, msg.abilities);
        writeSet(buf, msg.schematics);
        buf.writeVarInt(msg.villageTier);
        buf.writeVarInt(msg.progress.size());
        for (Map.Entry<String, Integer> e : msg.progress.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeVarInt(e.getValue());
        }
    }

    public static S2CQuestState decode(FriendlyByteBuf buf) {
        boolean full = buf.readBoolean();
        Set<String> completed = readSet(buf);
        Set<String> claimed = readSet(buf);
        Set<String> abilities = readSet(buf);
        Set<String> schematics = readSet(buf);
        int tier = buf.readVarInt();
        int n = buf.readVarInt();
        Map<String, Integer> progress = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            progress.put(buf.readUtf(), buf.readVarInt());
        }
        return new S2CQuestState(full, completed, claimed, abilities, schematics, tier, progress);
    }

    public static void handle(S2CQuestState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.barbarajones.v2.quests.client.ClientQuests.acceptState(msg)));
        ctx.get().setPacketHandled(true);
    }
}
