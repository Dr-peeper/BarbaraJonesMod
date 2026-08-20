package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * ASCENSION CHARM - brief flight in bursts, the same shape as
 * {@code CaydenCobb.combatFlight()}: not sustained hovering, a launch that
 * runs out.
 *
 * <p>This item only lights the fuse - the actual per-tick lift, the fall
 * damage waiver and the trail live in {@code AbilityEvents#onPlayerTick},
 * which runs for the whole {@link AbilityId#CHARM} active window that
 * {@code AbilityData#activate} already opened before this method ran. That
 * split is why {@code activate()} here is almost nothing: it is the "you
 * pressed the button" beat, not the flight itself.
 */
public class AscensionCharmItem extends AbilityItem {

    public AscensionCharmItem(Item.Properties properties) {
        super(AbilityId.CHARM, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        // an immediate hop off the ground, so the burst reads instantly instead
        // of waiting for the next tick's lift to catch up with a standing player
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
        player.fallDistance = 0.0F;
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.2D, player.getZ(), 24, 0.35D, 0.15D, 0.35D, 0.12D);
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.TRANSFORM_CHARGE.get(),
                player.getSoundSource(), 1.0F, 1.4F);
    }
}
