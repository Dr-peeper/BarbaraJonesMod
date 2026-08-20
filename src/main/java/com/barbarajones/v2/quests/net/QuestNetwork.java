package com.barbarajones.v2.quests.net;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The quest module's own channel.
 *
 * <p>Deliberately separate from {@code ModNetwork}: a dozen agents share this repo
 * and a shared packet-id table is the fastest way to get two modules silently
 * decoding each other's bytes. Forge is happy with any number of channels, so this
 * package owns its own ids from 0 and nobody else can renumber them.
 *
 * <p>Traffic shape, copied from how FTB Quests keeps a large quest book cheap:
 * <ul>
 *   <li>{@link S2CQuestDefs} - the whole quest graph. Sent ONCE per player, on login
 *       and again after a {@code /reload}. It is the big one.</li>
 *   <li>{@link S2CQuestState} - progress. Sent constantly, but only ever carries the
 *       counters that actually moved. This is why the book can update instantly
 *       without re-sending the definitions every time you pick up a wheat.</li>
 *   <li>{@link C2SQuestAction} - the two things a player can actively do: claim a
 *       reward, and hand over items for a delivery task.</li>
 * </ul>
 */
public final class QuestNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BarbaraJonesMod.MODID, "quests_v2"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private QuestNetwork() {
    }

    /** Called once from the module's common setup. */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, S2CQuestDefs.class,
                S2CQuestDefs::encode, S2CQuestDefs::decode, S2CQuestDefs::handle);
        CHANNEL.registerMessage(id++, S2CQuestState.class,
                S2CQuestState::encode, S2CQuestState::decode, S2CQuestState::handle);
        CHANNEL.registerMessage(id++, C2SQuestAction.class,
                C2SQuestAction::encode, C2SQuestAction::decode, C2SQuestAction::handle);
    }

    public static void toPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void toServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
