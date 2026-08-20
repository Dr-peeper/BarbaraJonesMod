package com.barbarajones.v2.village.net;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The village module's own network channel.
 *
 * <p>Deliberately separate from the mod's main {@code ModNetwork} channel. Forge is
 * happy with any number of channels per mod, and owning ours means this module can
 * add, reorder or remove messages without renumbering somebody else's packets - the
 * kind of merge conflict that produces a silent desync rather than a compile error.
 *
 * <h2>Messages</h2>
 * <table>
 *   <tr><th>id</th><th>message</th><th>direction</th><th>when</th></tr>
 *   <tr><td>0</td><td>{@link PacketVillageOffers}</td><td>S to C</td>
 *       <td>trade screen opened, and after every trade, feed or level-up</td></tr>
 *   <tr><td>1</td><td>{@link PacketSelectOffer}</td><td>C to S</td>
 *       <td>player clicks a trade row</td></tr>
 *   <tr><td>2</td><td>{@link PacketVillageStatus}</td><td>S to C</td>
 *       <td>every two seconds, to players standing inside a claim</td></tr>
 * </table>
 *
 * <p>The status packet is a small delta rather than a dump of the settlement: tier,
 * population, production, defence, happiness, stockpile and a name. That is enough
 * for the HUD and the atlas screen, and it means an enormous village costs the same
 * bandwidth as a tiny one.
 */
public final class VillageNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BarbaraJonesMod.MODID, "village"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private VillageNetwork() { }

    /**
     * Registers every message. Called once from common setup by
     * {@code VillageRegistry}; registering twice throws, so it is guarded there by
     * running inside {@code enqueueWork}.
     */
    public static void register() {
        CHANNEL.registerMessage(0, PacketVillageOffers.class,
                PacketVillageOffers::encode, PacketVillageOffers::decode, PacketVillageOffers::handle);
        CHANNEL.registerMessage(1, PacketSelectOffer.class,
                PacketSelectOffer::encode, PacketSelectOffer::decode, PacketSelectOffer::handle);
        CHANNEL.registerMessage(2, PacketVillageStatus.class,
                PacketVillageStatus::encode, PacketVillageStatus::decode, PacketVillageStatus::handle);
    }

    /** Server to one client. */
    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /** Client to server. */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
