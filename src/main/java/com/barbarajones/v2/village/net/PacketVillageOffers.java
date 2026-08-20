package com.barbarajones.v2.village.net;

import com.barbarajones.v2.village.VillageOffer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server to client: the trade list for the villager whose screen is open.
 *
 * <p>Offers cannot ride in the menu-open packet, because the client builds the menu
 * before the screen exists and before the entity is guaranteed to be tracked. So
 * they go as their own message immediately afterwards, keyed by {@code containerId}
 * - the same split vanilla uses for merchants.
 *
 * <p>Resent after every trade, every feeding and every level-up. Without that
 * resend the client keeps showing the old stock levels and the old XP bar until the
 * screen is closed and reopened, which reads as the trade not having worked.
 */
public class PacketVillageOffers {

    public final int containerId;
    public final List<VillageOffer> offers;
    public final int tradeLevel;
    public final int tradeXp;
    public final int kraveFed;
    public final int profession;

    public PacketVillageOffers(int containerId, List<VillageOffer> offers, int tradeLevel,
                               int tradeXp, int kraveFed, int profession) {
        this.containerId = containerId;
        this.offers = offers;
        this.tradeLevel = tradeLevel;
        this.tradeXp = tradeXp;
        this.kraveFed = kraveFed;
        this.profession = profession;
    }

    public static void encode(PacketVillageOffers msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.containerId);
        buf.writeVarInt(msg.tradeLevel);
        buf.writeVarInt(msg.tradeXp);
        buf.writeVarInt(msg.kraveFed);
        buf.writeVarInt(msg.profession);
        buf.writeVarInt(msg.offers.size());
        for (VillageOffer offer : msg.offers) {
            offer.write(buf);
        }
    }

    public static PacketVillageOffers decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int level = buf.readVarInt();
        int xp = buf.readVarInt();
        int fed = buf.readVarInt();
        int profession = buf.readVarInt();
        int count = buf.readVarInt();
        List<VillageOffer> offers = new ArrayList<>(Math.max(0, Math.min(count, 64)));
        for (int i = 0; i < count; i++) {
            offers.add(VillageOffer.read(buf));
        }
        return new PacketVillageOffers(containerId, offers, level, xp, fed, profession);
    }

    public static void handle(PacketVillageOffers msg, Supplier<NetworkEvent.Context> ctx) {
        // DistExecutor keeps the client-only handler off a dedicated server's
        // classpath entirely, rather than merely off its execution path.
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.barbarajones.v2.village.client.VillageClientState.acceptOffers(msg)));
        ctx.get().setPacketHandled(true);
    }
}
