package com.barbarajones.net;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.network.BoardFlightPacket;
import com.barbarajones.v2.airline.network.DeboardFlightPacket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's server -> client cinematic channel. */
public final class ModNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BarbaraJonesMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() { }

    public static void register() {
        CHANNEL.registerMessage(0, PacketApocalypse.class,
                PacketApocalypse::encode, PacketApocalypse::decode, PacketApocalypse::handle);
        CHANNEL.registerMessage(1, PacketCaydenStatus.class,
                PacketCaydenStatus::encode, PacketCaydenStatus::decode, PacketCaydenStatus::handle);
        CHANNEL.registerMessage(2, PacketKraveHit.class,
                PacketKraveHit::encode, PacketKraveHit::decode, PacketKraveHit::handle);
        CHANNEL.registerMessage(3, PacketKraveLevel.class,
                PacketKraveLevel::encode, PacketKraveLevel::decode, PacketKraveLevel::handle);
        // Client -> server: the Ascension Ledger buttons. Every other packet
        // here goes the other way, which is how this one got missed - the
        // screen has always sent it and nothing has ever been listening.
        CHANNEL.registerMessage(4, PacketCaydenUpgrade.class,
                PacketCaydenUpgrade::encode, PacketCaydenUpgrade::decode, PacketCaydenUpgrade::handle);

        // Client -> server: the field-power cap set on the ledger screen.
        // Id 7, not 5: the airline module already claimed 5 and 6, and two
        // packets sharing an id silently breaks whichever registers second.
        CHANNEL.registerMessage(7, PacketCaydenFieldCap.class,
                PacketCaydenFieldCap::encode, PacketCaydenFieldCap::decode,
                PacketCaydenFieldCap::handle);
        CHANNEL.registerMessage(5, BoardFlightPacket.class,
                BoardFlightPacket::toBytes, BoardFlightPacket::new, BoardFlightPacket::handle);
        CHANNEL.registerMessage(6, DeboardFlightPacket.class,
                DeboardFlightPacket::toBytes, DeboardFlightPacket::new, DeboardFlightPacket::handle);

        // The boss-fight finisher prompt. 8 and 9 - the ids above are taken and
        // reusing one silently breaks whichever registers second, which is a
        // mistake this file has already made once.
        CHANNEL.registerMessage(8, PacketKraveQte.class,
                PacketKraveQte::encode, PacketKraveQte::decode, PacketKraveQte::handle);
        // Client -> server, and carries nothing: the whole message is that a key
        // went down. Every decision about whether that counts is made server-side.
        CHANNEL.registerMessage(9, PacketKraveQteInput.class,
                PacketKraveQteInput::encode, PacketKraveQteInput::decode,
                PacketKraveQteInput::handle);
    }

    /** Send Cayden's vitals to his owner alone. */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /** Send a cinematic beat to everyone near the death site. */
    public static void sendAround(ServerLevel level, Vec3 pos, double radius, PacketApocalypse msg) {
        CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(pos.x, pos.y, pos.z, radius, level.dimension())), msg);
    }
}
