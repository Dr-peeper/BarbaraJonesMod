package com.barbarajones.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> victim trigger: "you were just hit by Krave Monster." No payload
 * needed - the client just starts its hit-vignette fade on receipt. See
 * KraveHitClient. Replaces the generic ambient Dread vignette as the Krave
 * Kosmos's actual tension cue (Dread stands down there entirely - see
 * DreadClient.tick()).
 */
public class PacketKraveHit {

    public static void encode(PacketKraveHit msg, FriendlyByteBuf buf) {
        // no fields
    }

    public static PacketKraveHit decode(FriendlyByteBuf buf) {
        return new PacketKraveHit();
    }

    public static void handle(PacketKraveHit msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.barbarajones.client.ClientPacketHandler.handleKraveHit(msg)));
        ctx.get().setPacketHandled(true);
    }
}
