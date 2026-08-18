package com.barbarajones.item;

import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * The Cayden Compass. Right-click to locate your Cayden anywhere in the world:
 * direction, distance, whether he is housed, and how much Krave he has eaten.
 */
public class CaydenCompassItem extends Item {

    private static final double SEARCH = 512.0D;

    public CaydenCompassItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        CaydenCobb found = nearestOwned(level, player);
        if (found == null) {
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "The compass spins uselessly. Cayden is not within " + (int) SEARCH + " blocks."));
            return InteractionResultHolder.consume(stack);
        }

        double dx = found.getX() - player.getX();
        double dz = found.getZ() - player.getZ();
        double dy = found.getY() - player.getY();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        BlockPos at = found.blockPosition();

        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + ""
                + ChatFormatting.BOLD + "[Cayden Compass]"));
        player.sendSystemMessage(Component.literal(ChatFormatting.YELLOW + "  "
                + compass(dx, dz) + ChatFormatting.WHITE + " - " + dist + " blocks away"
                + (Math.abs(dy) > 4 ? (dy > 0 ? ChatFormatting.GRAY + " (above you)"
                                              : ChatFormatting.GRAY + " (below you)") : "")));
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "  at "
                + at.getX() + ", " + at.getY() + ", " + at.getZ()));
        player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "  "
                + found.getKraveFed() + " Kraves eaten"
                + (found.isRageUnlocked() ? ", KRAVE RAGE ready" : "")
                + (found.isHoused() ? ChatFormatting.GREEN + ", housed"
                                    : ChatFormatting.RED + ", homeless")));
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private CaydenCobb nearestOwned(Level level, Player player) {
        CaydenCobb best = null;
        double bestD = Double.MAX_VALUE;
        AABB box = player.getBoundingBox().inflate(SEARCH, 256.0D, SEARCH);
        for (CaydenCobb c : level.getEntitiesOfClass(CaydenCobb.class, box)) {
            if (!c.isAlive() || !c.isOwnedBy(player)) {
                continue;
            }
            double d = c.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Cardinal/intercardinal bearing from the player to the target. */
    private String compass(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        String[] dirs = { "EAST", "SOUTHEAST", "SOUTH", "SOUTHWEST",
                          "WEST", "NORTHWEST", "NORTH", "NORTHEAST" };
        int idx = (int) Math.round(((angle % 360) + 360) % 360 / 45.0) % 8;
        return dirs[idx];
    }
}
