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

        // Searched outward from the players rather than across the dimension.
        // The trigger is proximity anyway, so scanning where nobody is standing
        // would cost real time to find nothing - and there is no cheap way to
        // enumerate one entity type across a whole loaded world.
        for (Player player : kosmos.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            for (KraveMonster boss : kosmos.getEntitiesOfClass(KraveMonster.class,
                    player.getBoundingBox().inflate(KraveKosmosBattle.triggerRange()))) {
            if (boss.getBattleState() != KraveBattleState.DORMANT || !boss.isAlive()) {
                continue;
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
                continue;
            }
            KraveKosmosBattle.start(kosmos, boss, cayden);
            }
        }
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
