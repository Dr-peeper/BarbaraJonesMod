package com.barbarajones.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumed to leave the Krave Kosmos: steps the holder back to wherever (and
 * whichever dimension) they stepped through the Krave Door from. Exists
 * because generating a paired return portal in floating-island terrain isn't
 * reliable - a carried item is simpler and can't strand anyone.
 */
public class KraveTetherItem extends Item {

    public KraveTetherItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            return InteractionResultHolder.fail(stack);
        }
        CompoundTag persist = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persist.contains("KraveReturnDim")) {
            return InteractionResultHolder.fail(stack);
        }

        ResourceKey<Level> returnDim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(persist.getString("KraveReturnDim")));
        ServerLevel dest = serverPlayer.getServer().getLevel(returnDim);
        if (dest == null) {
            return InteractionResultHolder.fail(stack);
        }

        double x = persist.getDouble("KraveReturnX");
        double y = persist.getDouble("KraveReturnY");
        double z = persist.getDouble("KraveReturnZ");
        // Take the companions home as well - stranding Cayden in the Kosmos is
        // a death sentence, and he would trigger the apocalypse from another
        // dimension entirely.
        java.util.List<net.minecraft.world.entity.Entity> escort =
                com.barbarajones.dimension.PetEscort.gather(serverPlayer);

        serverPlayer.teleportTo(dest, x, y, z, serverPlayer.getYRot(), serverPlayer.getXRot());
        com.barbarajones.dimension.PetEscort.deliver(escort, dest,
                new net.minecraft.world.phys.Vec3(x, y, z));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
