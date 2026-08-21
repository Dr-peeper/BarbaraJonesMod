package com.barbarajones.apocalypse;

import com.barbarajones.boss.krave.KraveBattleState;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * The one thing allowed to start the boss fight.
 *
 * <p>Previously nothing did. The encounter began as a side effect of Cayden
 * acquiring a target: his boss scan reaches far past his follow range - it has
 * to, an Ender Dragon circles hundreds of blocks out - and the Krave Monster
 * was a valid target the instant a player set foot in the dimension. So Cayden
 * would notice him from across the Kosmos, fly off, and fight him alone while
 * the player was still working out which way the den was. The player never saw
 * the encounter start because from their side it had already happened.
 *
 * <p>Now the Monster sits {@link KraveBattleState#DORMANT} until a player walks
 * up to him. Neither side may target the other while he is dormant, so the
 * approach is safe and the confrontation is the only door into the fight.
 *
 * <p>Ticked from {@link KraveKosmosBattle#tickAll}, which the server tick
 * already calls - one scan of one dimension a second, and only while somebody
 * is in it.
 */
public final class KraveConfrontation {

    private KraveConfrontation() { }

    /** Scans once a second. Twenty times a second buys nothing here. */
    private static final int SCAN_INTERVAL = 20;

    /**
     * How far Cayden may be from his owner and still be the one who answers.
     *
     * <p>Generous, because he is a pet who lags behind and the confrontation
     * should not fail silently because he stopped to look at something.
     */
    private static final double CAYDEN_RANGE = 64.0D;

    private static int tickCounter;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static void tickAll() {
        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerLevel kosmos = server.getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null || kosmos.players().isEmpty()) {
            return;
        }

        reattachOrphans(kosmos);

        // Searched outward from the players rather than across the dimension.
        // The trigger is proximity anyway, so scanning where nobody is standing
        // would cost real time to find nothing - and there is no cheap way to
        // enumerate one entity type across a whole loaded world.
        for (Player player : kosmos.players()) {
            // Spectators only. Creative was excluded too, which meant the
            // encounter could not be triggered by the one mode you would use to
            // go and test it - it would look broken and be working.
            if (player.isSpectator()) {
                continue;
            }
            for (KraveMonster boss : kosmos.getEntitiesOfClass(KraveMonster.class,
                    player.getBoundingBox().inflate(KraveKosmosBattle.triggerRange()))) {
            if (!boss.isAlive() || boss.getBattleState() == KraveBattleState.DEFEATED) {
                continue;
            }
            // A fight already in progress whose controller did not survive a
            // reload. The controllers live in a static list; the Monster's state
            // is saved. Rebuild the driving half around the saved half rather
            // than leaving him stranded in COMBAT with no prompt coming and no
            // transitions - and without replaying the confrontation, which would
            // reset him to form one.
            if (boss.getBattleState() != KraveBattleState.DORMANT) {
                continue;   // handled by the wider sweep below
            }
            // He and Cayden may not engage each other before the confrontation.
            // Enforced here as well as in Cayden's own targeting, because a
            // target can also arrive through HurtByTargetGoal or a stray splash
            // from an unrelated fight, neither of which consults demandFor.
            //
            // Only Cayden, though. Blanking his target outright would leave him
            // standing there unable to defend himself while a player chipped him
            // down, which is a worse failure than the auto-aggro this replaces -
            // he is still a hostile mob, and walking up and hitting him should
            // still be a bad idea.
            if (boss.getTarget() instanceof CaydenCobb) {
                boss.setTarget(null);
            }

            CaydenCobb cayden = findCayden(kosmos, player);
            if (cayden == null) {
                // No Cayden, no confrontation. He is half the scene, and a fight
                // that started without him would have no transformation ladder
                // and no finisher - the player would simply be standing next to
                // an unkillable boss.
                //
                // Said out loud rather than failing silently. Standing in front
                // of a boss who does nothing, with no explanation, is
                // indistinguishable from the encounter being broken - and that
                // is exactly how it was reported.
                explain(player);
                continue;
            }
            KraveKosmosBattle.start(kosmos, boss, cayden);
            }
        }
    }

    /**
     * Re-attaches a controller to any fight that has lost one.
     *
     * <p>Separate from the confrontation scan, and deliberately not tied to
     * where the player is standing. Cayden fights the boss across the whole
     * arena and well beyond it; requiring the player inside the same
     * twenty-eight blocks meant a fight could be raging with nothing driving
     * it - no threshold check, no prompt, and (since the clamp used to ask
     * whether a controller existed) no protection from being deleted outright.
     *
     * <p>Searched from the boss, who is the thing that needs driving.
     */
    private static void reattachOrphans(ServerLevel kosmos) {
        for (Player player : kosmos.players()) {
            // Widened well past the confrontation trigger. The player is only
            // used as a cheap place to start searching from - the fight itself
            // may be happening a long way from them.
            for (KraveMonster boss : kosmos.getEntitiesOfClass(KraveMonster.class,
                    player.getBoundingBox().inflate(ORPHAN_SCAN))) {
                if (!boss.isAlive()
                        || boss.getBattleState() == KraveBattleState.DORMANT
                        || boss.getBattleState() == KraveBattleState.DEFEATED
                        || KraveKosmosBattle.isActive(boss)) {
                    continue;
                }
                CaydenCobb cayden = nearestCayden(kosmos, boss);
                if (cayden != null) {
                    LOGGER.info("Re-attaching a controller to an orphaned Krave Monster fight at form {}.",
                            boss.getForm());
                    KraveKosmosBattle.resume(kosmos, boss, cayden);
                }
            }
        }
    }

    /** How far from a player an orphaned fight is still found and adopted. */
    private static final double ORPHAN_SCAN = 192.0D;

    /**
     * Any living Cayden near the boss.
     *
     * <p>Looser than {@link #findCayden} on purpose. That one picks whose story
     * this is and rightly insists on ownership; this one is repairing a fight
     * that has already started, where refusing to adopt it because the owner
     * check is fussy leaves it running with nothing driving it - which is
     * strictly worse than adopting the wrong Cayden.
     */
    private static CaydenCobb nearestCayden(ServerLevel kosmos, KraveMonster boss) {
        CaydenCobb best = null;
        double bestSqr = Double.MAX_VALUE;
        for (CaydenCobb c : kosmos.getEntitiesOfClass(CaydenCobb.class,
                boss.getBoundingBox().inflate(96.0D))) {
            if (!c.isAlive()) {
                continue;
            }
            double d = c.distanceToSqr(boss);
            if (d < bestSqr) {
                bestSqr = d;
                best = c;
            }
        }
        return best;
    }

    /** Ticks between repeats of the same explanation, so it is not chat spam. */
    private static final int EXPLAIN_INTERVAL = 200;

    private static final java.util.Map<java.util.UUID, Integer> EXPLAINED =
            new java.util.HashMap<>();

    /**
     * Tells the player why the boss is ignoring them.
     *
     * <p>Throttled per player rather than globally: two people arriving at the
     * den should each be told once, and neither should be told ten times a
     * second. Logged the first time too, because "the encounter did not start"
     * is the single hardest thing to diagnose from a log that contains nothing.
     */
    private static void explain(Player player) {
        int now = player.tickCount;
        Integer last = EXPLAINED.get(player.getUUID());
        if (last != null && now - last < EXPLAIN_INTERVAL) {
            return;
        }
        EXPLAINED.put(player.getUUID(), now);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                net.minecraft.ChatFormatting.DARK_RED
                + "The Krave Monster does not look at you. Cayden is not here."));
        LOGGER.info("Confrontation withheld for {}: no tamed Cayden within {} blocks.",
                player.getGameProfile().getName(), CAYDEN_RANGE);
    }

    /**
     * The player's own Cayden, if he is nearby.
     *
     * <p>Tamed-and-owned specifically: a bred Cayden belonging to somebody else,
     * or a wild one, is not the one this story is about.
     */
    private static CaydenCobb findCayden(ServerLevel kosmos, Player player) {
        CaydenCobb best = null;
        double bestSqr = CAYDEN_RANGE * CAYDEN_RANGE;
        for (CaydenCobb c : kosmos.getEntitiesOfClass(CaydenCobb.class,
                player.getBoundingBox().inflate(CAYDEN_RANGE))) {
            if (!c.isAlive() || !c.isTame() || c.getOwner() != player) {
                continue;
            }
            double d = c.distanceToSqr(player);
            if (d < bestSqr) {
                bestSqr = d;
                best = c;
            }
        }
        return best;
    }
}
