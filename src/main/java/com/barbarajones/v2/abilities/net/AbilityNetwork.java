package com.barbarajones.v2.abilities.net;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The abilities module's own network channel.
 *
 * <p>A dozen agents cannot all add {@code registerMessage} calls to one
 * shared channel without stepping on each other's packet ids, so this module
 * owns a completely separate channel instead - same pattern rule #3 asks for
 * with registries, applied to networking. Nothing outside this package ever
 * touches it directly.
 */
public final class AbilityNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BarbaraJonesMod.MODID, "abilities"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private AbilityNetwork() { }

    /** Called once from {@code PlayerAbilities.init(bus)} at mod construction. */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PacketActivateAbility.class,
                PacketActivateAbility::encode, PacketActivateAbility::decode, PacketActivateAbility::handle);
        CHANNEL.registerMessage(id++, PacketAbilitySync.class,
                PacketAbilitySync::encode, PacketAbilitySync::decode, PacketAbilitySync::handle);
    }

    /** Client -> server: "I pressed the keybind for this ability." */
    public static void sendActivate(int abilityIndex) {
        CHANNEL.sendToServer(new PacketActivateAbility(abilityIndex));
    }

    /** Server -> owner: full unlock/cooldown/active snapshot for the HUD. */
    public static void sendSync(ServerPlayer player, long now, int mask, long[] cooldownEnd, long[] activeEnd) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketAbilitySync(now, mask, cooldownEnd, activeEnd));
    }
}
