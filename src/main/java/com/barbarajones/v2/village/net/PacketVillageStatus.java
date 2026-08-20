package com.barbarajones.v2.village.net;

import com.barbarajones.v2.village.VillageView;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Server to client: everything the village HUD and the atlas screen need.
 *
 * <p>Settlement state is server-only - it lives in a {@code SavedData} the client
 * never sees - so without this packet the HUD would have nothing to draw. Sent to
 * each player inside a claim every two seconds, and once with {@link #inVillage}
 * false the moment they leave one, so the HUD disappears promptly rather than
 * lingering on stale numbers.
 *
 * <p>Small and fixed-shape by design: nine numbers and a name. An enormous capital
 * costs exactly the same bandwidth as a two-hut camp, because nothing per-building
 * or per-resident is ever sent.
 */
public class PacketVillageStatus {

    /** The "you are not in a village" message. Cheaper than a nullable payload. */
    public static final PacketVillageStatus NONE = new PacketVillageStatus();

    public final boolean inVillage;
    public final String name;
    public final int tier;
    public final int population;
    public final int populationCap;
    public final int buildings;
    public final int defence;
    public final int happiness;
    public final int production;
    public final int stockpile;
    public final int stockpileCap;
    public final BlockPos origin;

    private PacketVillageStatus() {
        this.inVillage = false;
        this.name = "";
        this.tier = 0;
        this.population = 0;
        this.populationCap = 0;
        this.buildings = 0;
        this.defence = 0;
        this.happiness = 0;
        this.production = 0;
        this.stockpile = 0;
        this.stockpileCap = 0;
        this.origin = BlockPos.ZERO;
    }

    public PacketVillageStatus(VillageView view) {
        this.inVillage = true;
        this.name = view.name();
        this.tier = view.tierIndex();
        this.population = view.population();
        this.populationCap = view.tier().populationCap();
        this.buildings = view.buildings();
        this.defence = view.defence();
        this.happiness = view.happiness();
        this.production = view.production();
        this.stockpile = view.stockpile();
        this.stockpileCap = 64 * (view.tierIndex() + 1);
        this.origin = view.origin();
    }

    private PacketVillageStatus(boolean inVillage, String name, int tier, int population,
                                int populationCap, int buildings, int defence, int happiness,
                                int production, int stockpile, int stockpileCap, BlockPos origin) {
        this.inVillage = inVillage;
        this.name = name;
        this.tier = tier;
        this.population = population;
        this.populationCap = populationCap;
        this.buildings = buildings;
        this.defence = defence;
        this.happiness = happiness;
        this.production = production;
        this.stockpile = stockpile;
        this.stockpileCap = stockpileCap;
        this.origin = origin;
    }

    /** Builds the message for a view, or {@link #NONE} for a null one. */
    public static PacketVillageStatus of(@Nullable VillageView view) {
        return view == null ? NONE : new PacketVillageStatus(view);
    }

    /**
     * Whether this message says the same thing as another. Used server-side to skip
     * sends when nothing moved, which is most of the time in a settled village.
     */
    public boolean sameAs(@Nullable PacketVillageStatus other) {
        if (other == null) {
            return false;
        }
        return this.inVillage == other.inVillage
                && this.tier == other.tier
                && this.population == other.population
                && this.buildings == other.buildings
                && this.defence == other.defence
                && this.happiness == other.happiness
                && this.production == other.production
                && this.stockpile == other.stockpile
                && this.name.equals(other.name);
    }

    public static void encode(PacketVillageStatus msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inVillage);
        if (!msg.inVillage) {
            return;
        }
        buf.writeUtf(msg.name, 64);
        buf.writeVarInt(msg.tier);
        buf.writeVarInt(msg.population);
        buf.writeVarInt(msg.populationCap);
        buf.writeVarInt(msg.buildings);
        buf.writeVarInt(msg.defence);
        buf.writeVarInt(msg.happiness);
        buf.writeVarInt(msg.production);
        buf.writeVarInt(msg.stockpile);
        buf.writeVarInt(msg.stockpileCap);
        buf.writeBlockPos(msg.origin);
    }

    public static PacketVillageStatus decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return NONE;
        }
        String name = buf.readUtf(64);
        int tier = buf.readVarInt();
        int population = buf.readVarInt();
        int populationCap = buf.readVarInt();
        int buildings = buf.readVarInt();
        int defence = buf.readVarInt();
        int happiness = buf.readVarInt();
        int production = buf.readVarInt();
        int stockpile = buf.readVarInt();
        int stockpileCap = buf.readVarInt();
        BlockPos origin = buf.readBlockPos();
        return new PacketVillageStatus(true, name, tier, population, populationCap, buildings,
                defence, happiness, production, stockpile, stockpileCap, origin);
    }

    public static void handle(PacketVillageStatus msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.barbarajones.v2.village.client.VillageClientState.acceptStatus(msg)));
        ctx.get().setPacketHandled(true);
    }
}
