package com.barbarajones.v2.bonds;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The "escalating buffs" a feeding bond actually pays out.
 *
 * <p>{@code CaydenCobb.applyKraveStats()} already scales his raw attack/speed
 * with {@code getKraveFed()} - that part of "feeding makes him stronger" was
 * real before this module existed. What was missing was anything that reads as
 * a <em>relationship</em> maturing rather than a stat creeping up one point at
 * a time, so this layers a second, chunkier set of passive effects on top, keyed
 * to {@link BondLevel} rather than the raw counter - five clear steps instead of
 * an invisible ramp. Refreshed periodically from {@link FeedingBondEvents}'s
 * tick hook rather than granted once, so it survives death/respawn and a fresh
 * login without needing to be saved anywhere itself.
 */
final class BondBuffs {

    private BondBuffs() { }

    /** Reapplied this often; must outlast the gap so the effect never visibly runs out. */
    static final int REFRESH_INTERVAL = 100;
    private static final int DURATION = REFRESH_INTERVAL + 40;

    static void applyToCayden(CaydenCobb cayden, BondLevel level) {
        if (level == BondLevel.STRANGER) {
            return;
        }
        // Regular: he stops flinching quite so hard.
        give(cayden, MobEffects.DAMAGE_RESISTANCE, 0);
        if (level.index() >= BondLevel.RIDE_OR_DIE.index()) {
            // Ride-or-Die: the regen from CaydenCobb.regenerate() gets a real assist.
            give(cayden, MobEffects.REGENERATION, 0);
        }
        if (level.index() >= BondLevel.BONDED_FOR_LIFE.index()) {
            // Bonded for Life: he swings like he means it.
            give(cayden, MobEffects.DAMAGE_BOOST, 0);
            give(cayden, MobEffects.MOVEMENT_SPEED, 0);
        }
        if (level.index() >= BondLevel.KRAVE_SOULMATE.index()) {
            // Krave Soulmate: nothing slows him down and nothing burns him -
            // the top rung is the one that should feel unfair in his favour.
            give(cayden, MobEffects.DAMAGE_RESISTANCE, 1);
            give(cayden, MobEffects.FIRE_RESISTANCE, 0);
        }
    }

    static void applyToBarbara(BarbaraJones barbara, BondLevel level) {
        if (level == BondLevel.STRANGER) {
            return;
        }
        give(barbara, MobEffects.MOVEMENT_SPEED, 0);
        if (level.index() >= BondLevel.RIDE_OR_DIE.index()) {
            give(barbara, MobEffects.DAMAGE_RESISTANCE, 0);
        }
        if (level.index() >= BondLevel.BONDED_FOR_LIFE.index()) {
            give(barbara, MobEffects.REGENERATION, 0);
            give(barbara, MobEffects.DAMAGE_BOOST, 0);
        }
        if (level.index() >= BondLevel.KRAVE_SOULMATE.index()) {
            // At the top rung feeding her keeps her calm even close to a dry
            // bag - see FeedingBondEvents, which reads this same level to widen
            // the "no need to panic yet" window rather than duplicating a stat.
            give(barbara, MobEffects.DAMAGE_RESISTANCE, 1);
        }
    }

    private static void give(LivingEntity e, net.minecraft.world.effect.MobEffect effect, int amplifier) {
        // ambient + visible: a faint particle trail around the companion, not a
        // HUD icon (that only ever shows for the player's own effects anyway) -
        // a second, quieter "this bond is doing something" tell alongside the
        // nameplate stars and the feed toasts.
        e.addEffect(new MobEffectInstance(effect, DURATION, amplifier, true, true));
    }
}
