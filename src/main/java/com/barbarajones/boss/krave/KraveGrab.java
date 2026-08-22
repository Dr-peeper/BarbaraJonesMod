package com.barbarajones.boss.krave;

import com.barbarajones.entity.KraveMonster;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The boss, temporarily held by somebody.
 *
 * <p>Four of the six finishers need him physically carried - picked up, flown
 * with, thrown. A damage event with particles over the top does not read as
 * that; you have to actually see him move with the thing holding him.
 *
 * <p>Deliberately a wrapper around his ordinary state rather than a change to
 * it. Everything switched off here is switched back on by {@link #release},
 * and release runs from the sequencer's own cleanup as well as at the end of a
 * successful move - so a cinematic that fails, times out, or is interrupted by
 * a disconnect cannot leave a boss permanently frozen in mid-air with his AI
 * off. That failure mode would be far worse than any of the bugs this replaces.
 */
public final class KraveGrab {

    private KraveGrab() { }

    /**
     * Whether he is currently held.
     *
     * <p>Read by his own tick and damage handling: a held boss does not
     * navigate, does not swing, and does not take knockback, because all three
     * fight the cinematic that is moving him.
     */
    public static boolean isHeld(KraveMonster boss) {
        return boss.cinematicHolder != null;
    }

    /**
     * Picks him up. The offset is where he sits relative to the holder.
     *
     * <p>Idempotent: grabbing an already-grabbed boss just re-points the
     * anchor, which is what a hand-off between the player and Kaiden wants.
     */
    public static void grab(KraveMonster boss, Entity holder, Vec3 offset) {
        boss.cinematicHolder = holder;
        boss.cinematicOffset = offset;
        boss.setNoGravity(true);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.getNavigation().stop();
        boss.setTarget(null);
    }

    /** Where the anchor currently puts him, or null if nobody is holding him. */
    @Nullable
    public static Vec3 anchorFor(KraveMonster boss) {
        Entity holder = boss.cinematicHolder;
        if (holder == null || !holder.isAlive()) {
            return null;
        }
        return holder.position().add(boss.cinematicOffset);
    }

    /**
     * Keeps him at the anchor. Called every tick by whatever is holding him.
     *
     * <p>Position is set directly rather than steered: this is a cinematic, and
     * a boss who lags a few blocks behind the hand carrying him reads as a bug
     * rather than as a hold.
     */
    public static void follow(KraveMonster boss) {
        Vec3 at = anchorFor(boss);
        if (at == null) {
            release(boss, Vec3.ZERO);
            return;
        }
        boss.setPos(at.x, at.y, at.z);
        boss.setDeltaMovement(Vec3.ZERO);
        boss.fallDistance = 0.0F;
        boss.hurtMarked = true;
        // Faces the way he is being carried, so a throw reads as a throw and not
        // as him drifting backwards.
        Entity holder = boss.cinematicHolder;
        if (holder != null) {
            boss.setYRot(holder.getYRot());
            boss.yBodyRot = holder.getYRot();
        }
    }

    /**
     * Lets go, with whatever velocity the move wants to impart.
     *
     * <p>Restores gravity unconditionally. A released boss must fall.
     */
    public static void release(KraveMonster boss, Vec3 velocity) {
        boss.cinematicHolder = null;
        boss.cinematicOffset = Vec3.ZERO;
        boss.setNoGravity(false);
        boss.setDeltaMovement(velocity);
        boss.hurtMarked = true;
    }
}
