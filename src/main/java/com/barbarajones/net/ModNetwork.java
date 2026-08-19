package com.barbarajones.net;

import com.barbarajones.BarbaraJonesMod;

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
