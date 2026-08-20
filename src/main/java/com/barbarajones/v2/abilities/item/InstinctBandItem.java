package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * INSTINCT BAND - Ultra Instinct's "the body answers before he does", worn as
 * a wristband. While the window {@code AbilityData#activate} just opened is
 * running, {@code AbilityEvents#onHurt} refuses every hit outright, exactly
 * like {@code CaydenCobb.hurt()} does for its {@code dodgePercent} roll, only
 * at 100% instead of a per-hit chance - "dodge-everything", per the brief,
 * not "dodge most things".
 *
 * <p>This class only announces the window opening; the actual refusal lives
 * in the shared hurt listener so every source of damage (melee, projectiles,
 * fall, fire) goes through one check instead of being reimplemented per
 * damage type here.
 */
public class InstinctBandItem extends AbilityItem {

    public InstinctBandItem(Item.Properties properties) {
        super(AbilityId.BAND, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    30, 0.4D, 0.5D, 0.4D, 0.18D);
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.TRANSFORM_ULTRA_HUM.get(),
                player.getSoundSource(), 1.0F, 1.0F);
    }
}
