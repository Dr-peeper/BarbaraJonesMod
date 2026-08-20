package com.barbarajones.v2.build.net;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * This module's own channel.
 *
 * <p>Deliberately not the mod's shared "main" channel: packet ids there are
 * hand-numbered, several agents are adding to it at once, and a duplicate
 * discriminator is a silent, miserable bug. A separate channel costs nothing and
 * cannot collide.
 *
 * <p>There is exactly one packet, and it goes client to server. Everything else
 * this module does on the client - the whole ghost preview - is computed
 * locally from the structure definition and the client's own copy of the world,
 * so there is nothing to sync.
 */
public final class BuildNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BarbaraJonesMod.MODID, "v2_build"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private BuildNetwork() { }

    public static void register() {
        CHANNEL.registerMessage(0, PacketRotateSchematic.class,
                PacketRotateSchematic::encode, PacketRotateSchematic::decode, PacketRotateSchematic::handle);
    }

    /** Client -> server. */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /** Unused today, but the module owns its channel and a server -> client path will be wanted eventually. */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
