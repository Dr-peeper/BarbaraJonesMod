package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * GOD CORE - the Super Saiyan God aura, worn by the player instead of grown
 * into. Matches {@code AscensionLadder.GOD}'s own numbers rather than
 * inventing new ones: that rung is +1450% attack, +150% speed, +16 max
 * health and 38% of incoming damage simply refused. Scaled down from "the
 * final boss's own stat sheet" to "a real, noticeable, temporary power spike
 * a player can survive having" - see the constants below for exactly how much
 * of each was kept.
 *
 * <p>The buffs are real {@link AttributeModifier}s under fixed UUIDs, added
 * here and removed by {@code AbilityEvents} the tick the active window
 * (opened by {@code AbilityData#activate} before this method runs) closes.
 * The damage refusal is not an attribute - Minecraft has no generic "percent
 * damage taken" attribute - so it is applied as a straight multiplier in
 * {@code AbilityEvents#onHurt} instead, gated on {@link #isBuffed}.
 */
public class GodCoreItem extends AbilityItem {

    private static final UUID ATTACK_MOD_ID = UUID.fromString("b1a2c3d4-0001-4a00-8000-000000000001");
    private static final UUID SPEED_MOD_ID = UUID.fromString("b1a2c3d4-0001-4a00-8000-000000000002");
    private static final UUID HEALTH_MOD_ID = UUID.fromString("b1a2c3d4-0001-4a00-8000-000000000003");

    /** Percent bonus to attack damage while active (Cayden's GOD rung is a flat x15.5 on top of his fed scaling). */
    private static final double ATTACK_BONUS_PCT = 1.5D;
    /** Percent bonus to movement speed while active. */
    private static final double SPEED_BONUS_PCT = 0.6D;
    /** Flat bonus max health while active. */
    private static final double HEALTH_BONUS = 12.0D;
    /** Fraction of incoming damage that still lands - see {@code AbilityEvents#onHurt}. */
    public static final float DAMAGE_TAKEN_MULTIPLIER = 0.65F;

    public GodCoreItem(Item.Properties properties) {
        super(AbilityId.GODCORE, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        applyBuffs(player);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    40, 0.4D, 0.6D, 0.4D, 0.05D);
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.TRANSFORM_GODPULSE.get(),
                player.getSoundSource(), 1.2F, 1.0F);
    }

    /** True while this player currently carries the God Core attribute modifiers. */
    public static boolean isBuffed(ServerPlayer player) {
        AttributeInstance atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
        return atk != null && atk.getModifier(ATTACK_MOD_ID) != null;
    }

    public static void applyBuffs(ServerPlayer player) {
        if (isBuffed(player)) {
            return;
        }
        addPercent(player, Attributes.ATTACK_DAMAGE, ATTACK_MOD_ID, "barbarajones:god_core_attack", ATTACK_BONUS_PCT);
        addPercent(player, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID, "barbarajones:god_core_speed", SPEED_BONUS_PCT);

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(HEALTH_MOD_ID) == null) {
            health.addTransientModifier(new AttributeModifier(HEALTH_MOD_ID, "barbarajones:god_core_health",
                    HEALTH_BONUS, AttributeModifier.Operation.ADDITION));
            player.heal((float) HEALTH_BONUS);   // the burst should feel like a top-up, not a slow regen
        }
    }

    public static void removeBuffs(ServerPlayer player) {
        removeIfPresent(player, Attributes.ATTACK_DAMAGE, ATTACK_MOD_ID);
        removeIfPresent(player, Attributes.MOVEMENT_SPEED, SPEED_MOD_ID);
        removeIfPresent(player, Attributes.MAX_HEALTH, HEALTH_MOD_ID);
        // a shrinking max health does not itself clamp current health - see Perks#applyToCayden
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    private static void addPercent(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id, String name, double pct) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, name, pct,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeIfPresent(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }
}
