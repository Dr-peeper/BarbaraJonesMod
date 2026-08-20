package com.barbarajones.v2.internet;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What The Internet Manager leaves behind, and the one check other systems
 * gate on.
 *
 * <p>{@link #STATIC_IP_ID} is matched by registry id rather than the
 * {@code InternetContent.STATIC_IP} constant on purpose - the same reason
 * {@code AscensionLadder.isLedger} does it that way: a system in another
 * module that wants to ask "has this player killed The Internet Manager"
 * should be able to compile and run correctly whether or not it is loaded
 * before or after this one, and whether or not this module's jar is even
 * present. A dead {@code RegistryObject} reference would throw; a
 * {@link ResourceLocation} comparison just returns false.
 */
public final class InternetLoot {

    /** One per kill, never a stack of them - it identifies a single connection. */
    public static final ResourceLocation STATIC_IP_ID =
            new ResourceLocation(BarbaraJonesMod.MODID, "static_ip");

    private InternetLoot() { }

    public static boolean isStaticIp(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return STATIC_IP_ID.equals(id);
    }

    /**
     * True if the given player is carrying a Static IP anywhere in their
     * inventory (main, offhand or armor slots all count - it is a trophy, not
     * equipment, so where it is held does not matter). The intended use is a
     * gate: some other ability module gets to decide that a move, a recipe or
     * a door only works for someone who has actually beaten this fight.
     */
    public static boolean hasStaticIp(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isStaticIp(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isStaticIp(stack)) {
                return true;
            }
        }
        return false;
    }
}
