package com.barbarajones.v2.build.item;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.net.BuildNetwork;
import com.barbarajones.v2.build.net.PacketRotateSchematic;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Left-clicking with an armed schematic rotates the building instead of
 * swinging at the world.
 *
 * <p>This lives on the common event bus rather than in the client package on
 * purpose: the cancel has to happen on <i>both</i> sides. Cancel only on the
 * client and the server happily starts mining whatever the player was pointing
 * at; cancel only on the server and the client plays a block-breaking animation
 * for something that is never going to break.
 *
 * <p>The client's own repeat guard is a plain tick counter rather than a check
 * on the event's action enum, so nothing here depends on which Forge build is
 * underneath.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SchematicInput {

    /** Minimum client ticks between two rotations from a held-down left click. */
    private static final int ROTATE_INTERVAL = 5;

    private static int clientCooldown;

    private SchematicInput() { }

    /** Called from the client tick handler so the cooldown drains even without clicks. */
    public static void tickClientCooldown() {
        if (clientCooldown > 0) {
            clientCooldown--;
        }
    }

    /** True if the client may send a rotation right now; arms the cooldown if so. */
    public static boolean claimClientRotate() {
        if (clientCooldown > 0) {
            return false;
        }
        clientCooldown = ROTATE_INTERVAL;
        return true;
    }

    private static boolean isArmedSchematic(ItemStack stack) {
        return stack.getItem() instanceof KraveSchematicItem && KraveSchematicItem.armed(stack);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!isArmedSchematic(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel().isClientSide && claimClientRotate()) {
            BuildNetwork.sendToServer(new PacketRotateSchematic(1));
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!isArmedSchematic(event.getItemStack())) {
            return;
        }
        if (event.getLevel().isClientSide && claimClientRotate()) {
            BuildNetwork.sendToServer(new PacketRotateSchematic(1));
        }
    }
}
