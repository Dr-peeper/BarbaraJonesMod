package com.barbarajones.apocalypse;

import com.barbarajones.boss.krave.KraveAttacks;
import com.barbarajones.boss.krave.KraveBattleState;
import com.barbarajones.boss.krave.KraveDemolition;
import com.barbarajones.boss.krave.KraveCinematic;
import com.barbarajones.boss.krave.KraveFinisher;
import com.barbarajones.boss.krave.KraveFinisherMove;
import com.barbarajones.boss.krave.KraveGrab;
import com.barbarajones.boss.krave.KraveMoveScripts;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveHealingBox;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.net.ModNetwork;
import com.barbarajones.net.PacketKraveQte;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Krave Monster encounter, as one state machine.
 *
 * <p>This used to be a side-car: the real fight was Cayden and the Monster
 * hitting each other, and this class spawned minions and healing boxes around
 * it. That is why the encounter had no shape - it began whenever Cayden
 * happened to notice the Monster, forms advanced through the death event, and
 * five separate booleans across three classes each held part of the truth.
 *
 * <p>Now the Monster carries one authoritative {@link KraveBattleState} (synced
 * and saved), and this drives it:
 *
 * <pre>
 *   DORMANT -> CONFRONTATION -> COMBAT -> QTE -> FINISHER -> TRANSITION
 *                                  ^                              |
 *                                  +------------------------------+
 *                                                                 -> DEFEATED
 * </pre>
 *
 * <p>The loop runs once per form. Each pass through TRANSITION grows the
 * Monster one form and takes Cayden one rung up the ladder, so form N is always
 * met by rung N: Super Saiyan against form one, Ultra Instinct against form six,
 * and everything he has against the Krave God at seven. The last form's
 * finisher goes to DEFEATED instead of looping.
 *
 * <p>Nothing here trusts the client. The prompt is a request to draw something;
 * the answer is a bare "a key went down" with no claims in it, and every
 * decision - was there a window, was it this player's, has it already been
 * used - is made on this side. See {@code PacketKraveQteInput}.
 */
public final class KraveKosmosBattle {

    private static final List<KraveKosmosBattle> ACTIVE = new ArrayList<>();

    /** Ticks between Cayden's meteor barrages. */
    private static final int METEOR_INTERVAL = 90;
    private static final int MINION_INTERVAL = 200;
    private static final int BOX_INTERVAL = 500;
    private static final int MAX_MINIONS = 6;
    private static final int MAX_BOXES = 3;

    /** How near a player must get before the encounter wakes up. */
    private static final double TRIGGER_RANGE = 28.0D;

    /** Length of the stare-down before the first form starts. */
    private static final int CONFRONTATION_TICKS = 110;

    /** How long the player has to answer the prompt. */
    private static final int QTE_WINDOW = 70;

    /** Pause after a missed prompt before it is offered again. */
    private static final int QTE_RETRY_DELAY = 60;

    /** Ticks the thrown player has to connect before the throw is written off. */
    private static final int FINISHER_TIMEOUT = 100;

    /** Ticks the Monster spends growing into his next form. */
    private static final int TRANSITION_TICKS = 90;

    /** How long Cayden gets to reach the launch point before he is placed there. */
    private static final int PREPARE_TIMEOUT = 160;

    /** Close enough to the launch point to count as standing in it. */
    private static final double IN_POSITION = 3.0D;


    /** How fast Cayden climbs to his launch point. */
    private static final double CAYDEN_CLIMB = 1.1D;

    /** Base pause between the attacks of one form, before he is upright again. */
    private static final int RECOVERY_TICKS = 55;

    /**
     * Beat before a player-driven attack is offered.
     *
     * <p>Long enough to read as a pause rather than as the prompt appearing on
     * top of the previous cinematic, short enough that a six-part finisher does
     * not become six flights.
     */
    private static final int SETTLE_TICKS = 20;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private final ServerLevel level;
    private final KraveMonster boss;
    private final CaydenCobb cayden;

    private int t;
    private int minionTimer = 100;
    private int boxTimer = 200;
    private int meteorTimer = 60;

    /** Ticks remaining in whatever scripted beat is running. */
    private int phaseTimer;
    /** Whose prompt is open, or null if there is no live window. */
    private UUID qtePlayer;
    private boolean qteRetry;
    /** The player currently in the air, mid-finisher. */
    private UUID thrown;
    /** Where Cayden throws from, recomputed as the boss drifts. */
    private Vec3 launch;
    private int prepareTicks;
    /** The step the open prompt belongs to, checked back the same way as the form. */
    private int qteStepAsked;
    /** The cinematic currently playing, its clock, and how long it may run. */
    private KraveMoveScripts.Script script;
    private KraveCinematic cinematic;
    private int scriptTimeout;
    private int recoveryTicks;
    /**
     * The form the open prompt belongs to.
     *
     * <p>Sent to the client and compared again when the answer comes back, so a
     * key pressed a moment too late cannot answer the form that has already
     * started.
     */
    private int qteForm;

    private KraveKosmosBattle(ServerLevel level, KraveMonster boss, CaydenCobb cayden) {
        this.level = level;
        this.boss = boss;
        this.cayden = cayden;
    }

    // ---- entry points -------------------------------------------------------

    /**
     * Begins the encounter, if it is not already running.
     *
     * <p>Called from the confrontation trigger rather than from Cayden's attack
     * goal. That is the whole of requirement five: the fight starting was
     * previously a side effect of Cayden acquiring a target, so walking into the
     * dimension was enough to set it off.
     */
    public static void start(ServerLevel level, KraveMonster boss, CaydenCobb cayden) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss == boss) {
                return;                       // never two controllers on one boss
            }
        }
        if (boss.getBattleState() == KraveBattleState.DEFEATED) {
            return;
        }
        KraveKosmosBattle battle = new KraveKosmosBattle(level, boss, cayden);
        ACTIVE.add(battle);
        battle.enterConfrontation();
    }

    /**
     * Re-attaches a controller to a fight that is already in progress.
     *
     * <p>Used after a reload, where the Monster still carries a saved COMBAT
     * state but the controller that was driving him is gone. Deliberately does
     * NOT replay the confrontation: he has already been confronted, and running
     * the stare-down again would reset him to form one and undo the fight.
     */
    public static void resume(ServerLevel level, KraveMonster boss, CaydenCobb cayden) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss == boss) {
                return;
            }
        }
        KraveKosmosBattle battle = new KraveKosmosBattle(level, boss, cayden);
        ACTIVE.add(battle);
        battle.boss.setTarget(cayden);
        cayden.setTarget(boss);
        // Matched to the form he was already on, so a fight resumed at form five
        // does not restart with a Super Saiyan.
        cayden.ascendTo(Math.min(boss.getForm(), AscensionLadder.ULTRA));
    }

    public static boolean isActive(KraveMonster boss) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss == boss) {
                return true;
            }
        }
        return false;
    }

    public static void tickAll() {
        if (!ACTIVE.isEmpty()) {
            for (KraveKosmosBattle b : new ArrayList<>(ACTIVE)) {
                if (b.tick()) {
                    ACTIVE.remove(b);
                }
            }
        }
        KraveConfrontation.tickAll();
    }

    /**
     * A player pressed the finisher key.
     *
     * <p>Every guard that matters is here rather than in the packet: there must
     * be a live window, it must be this player's, and the window is consumed
     * before anything else happens so a held key or a spamming client cannot run
     * the finisher twice.
     */
    public static void onQteInput(ServerPlayer player, int claimedForm, int claimedStep) {
        for (KraveKosmosBattle b : ACTIVE) {
            KraveBattleState state = b.boss.getBattleState();
            if (state != KraveBattleState.QTE_READY && state != KraveBattleState.QTE) {
                continue;                       // not asking, or not asking yet
            }
            if (!player.getUUID().equals(b.qtePlayer)) {
                continue;                       // this prompt is not theirs
            }
            if (claimedForm != b.qteForm || claimedStep != b.qteStepAsked) {
                // Both halves matter. The wrong form skips a stage; the wrong
                // step skips an attack inside one.
                LOGGER.info("[CraveBoss] Ignoring a stale press: claimed form {} step {}, open form {} step {}.",
                        claimedForm, claimedStep, b.qteForm, b.qteStepAsked);
                continue;
            }
            b.qtePlayer = null;                 // consumed before anything else
            b.beginFinisher(player);
            return;
        }
    }

    // ---- the machine --------------------------------------------------------

    /** @return true when this fight is over and should be dropped. */
    private boolean tick() {
        this.t++;

        if (!this.boss.isAlive()) {
            // He should only ever die at the end of the last finisher, but a
            // command or the void can still take him. Retire cleanly rather than
            // ticking a controller whose boss is gone.
            if (this.cayden.isAlive()) {
                this.cayden.powerDown();
            }
            clearPrompt();
            return true;
        }
        if (!this.cayden.isAlive()) {
            announce(ChatFormatting.RED, "", "Cayden is down. The Krave Monster settles.");
            this.boss.setBattleState(KraveBattleState.DORMANT);
            this.boss.setTarget(null);
            clearPrompt();
            return true;
        }

        catchTheFallen();

        // Validated through the whole encounter, not just the cinematic. He does
        // not arrive under the island at throw time - he gets there during the
        // fight, chasing a boss that is above him - so checking only once the
        // finisher starts is checking long after it happened.
        //
        // Twice a second rather than every tick: it reads blockstates, and a
        // position that has been invalid for half a second is no worse than one
        // that has been invalid for one tick.
        if (this.t % 10 == 0
                && !KraveFinisher.isValidPosition(this.level, this.cayden, this.boss)) {
            KraveFinisher.recover(this.level, this.cayden, this.boss);
        }

        return switch (this.boss.getBattleState()) {
            case CONFRONTATION -> { tickConfrontation(); yield false; }
            case COMBAT -> { tickCombat(); yield false; }
            case QTE_PREPARING -> { tickPreparing(); yield false; }
            // QTE and QTE_READY run the same window. The bare QTE state only
            // still exists so a world saved mid-prompt loads into something
            // meaningful instead of falling through to a missing case.
            case QTE_READY, QTE -> { tickReady(); yield false; }
            case FINISHER -> { tickFinisher(); yield false; }
            case QTE_RECOVERY -> { tickRecovery(); yield false; }
            case TRANSITION -> { tickTransition(); yield false; }
            case DEFEATED -> true;
            case DORMANT -> true;             // something reset him; stop driving
        };
    }

    // ---- CONFRONTATION ------------------------------------------------------

    private void enterConfrontation() {
        this.boss.setBattleState(KraveBattleState.CONFRONTATION);
        this.phaseTimer = CONFRONTATION_TICKS;
        // Whatever form he is carrying, NOT a hardcoded one. A fresh body is
        // already form one - that is settled at spawn - and forcing it here
        // would instead wipe the progress of a boss who legitimately kept his
        // form: one reseated at his den after being killed outside his
        // finisher, or one loaded from a save. Hardcoding the answer is only
        // correct in the case where it changes nothing.
        this.boss.restoreForPhase();
        this.boss.setTarget(null);
        this.cayden.setTarget(null);

        announce(ChatFormatting.DARK_RED, "" + ChatFormatting.BOLD, "THE KRAVE MONSTER STIRS.");
        // The player job, said out loud. This line existed in the old
        // controller and was lost when it was rewritten - so the healing boxes
        // went on refilling him with nothing anywhere telling anyone they were
        // the reason, which reads as the boss being bugged rather than as a
        // mechanic being ignored.
        announce(ChatFormatting.GOLD, "",
                "Destroy the Krave Boxes - they heal him. Clear the minions. Cayden has the rest.");
        this.level.playSound(null, this.boss.blockPosition(), ModSounds.KRAVE_ROAR.get(),
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private void tickConfrontation() {
        // Both of them held still and facing each other. Positions are forced
        // every tick rather than once, because AI goals keep running underneath
        // and would otherwise wander them apart mid-cinematic.
        Vec3 facing = this.boss.position().subtract(this.cayden.position());
        this.cayden.getLookControl().setLookAt(this.boss, 30.0F, 30.0F);
        this.boss.getLookControl().setLookAt(this.cayden, 30.0F, 30.0F);
        this.cayden.setDeltaMovement(this.cayden.getDeltaMovement().scale(0.5D));
        this.boss.setDeltaMovement(this.boss.getDeltaMovement().scale(0.5D));

        // Close him to a duelling distance if he is miles off, without
        // teleporting: this beat lasts long enough to fly it.
        double dist = facing.length();
        if (dist > 14.0D) {
            this.cayden.setNoGravity(true);
            this.cayden.setDeltaMovement(facing.normalize().scale(0.55D));
        } else {
            this.cayden.setNoGravity(false);
        }

        if (this.phaseTimer == CONFRONTATION_TICKS - 40) {
            announce(ChatFormatting.GOLD, "" + ChatFormatting.BOLD, "Cayden steps forward.");
            // Matched to the form actually in front of him. Form one opens with
            // Super Saiyan; a fight resumed at form five opens with Blue.
            this.cayden.ascendTo(Math.min(this.boss.getForm(), AscensionLadder.ULTRA));
        }
        if (--this.phaseTimer <= 0) {
            beginCombat();
        }
    }

    // ---- COMBAT -------------------------------------------------------------

    private void beginCombat() {
        this.boss.setBattleState(KraveBattleState.COMBAT);
        this.boss.restoreForPhase();
        this.boss.setTarget(this.cayden);
        this.cayden.setTarget(this.boss);
        announce(ChatFormatting.DARK_RED, "" + ChatFormatting.BOLD,
                "FORM " + this.boss.getForm() + ".");
    }

    private void tickCombat() {
        if (this.boss.atFinisherThreshold()) {
            // Start of this form's sequence. The step resets here and nowhere
            // else, so a failed attack retries itself rather than sending the
            // player back to the first one.
            this.boss.setQteStep(0);
            enterPreparing();
            return;
        }
        if (--this.meteorTimer <= 0) {
            this.meteorTimer = METEOR_INTERVAL;
            meteorBarrage();
        }
        if (--this.minionTimer <= 0) {
            this.minionTimer = MINION_INTERVAL;
            if (countNearby(KraveMinion.class) < MAX_MINIONS) {
                spawnMinion();
            }
        }
        if (--this.boxTimer <= 0) {
            this.boxTimer = BOX_INTERVAL;
            if (countNearby(KraveHealingBox.class) < MAX_BOXES) {
                spawnHealingBox();
            }
        }
    }

    // ---- QTE_PREPARING -> QTE_READY -> FINISHER -----------------------------

    /**
     * The form is spent. Cayden breaks off and climbs.
     *
     * <p>Nothing is offered to the player yet. The prompt used to appear on the
     * tick the threshold was crossed, wherever Cayden happened to be - across
     * the arena, inside the castle, or under the island - and answering it
     * produced a throw from nowhere. There is nothing to press until he is in
     * position.
     */
    private void enterPreparing() {
        this.boss.setBattleState(KraveBattleState.QTE_PREPARING);
        this.boss.setTarget(null);
        this.cayden.setTarget(null);
        this.launch = null;
        this.prepareTicks = 0;
        clearPrompt();
        announce(ChatFormatting.GOLD, "" + ChatFormatting.BOLD, "CAYDEN BREAKS OFF.");
    }

    private void tickPreparing() {
        holdBoss();

        KraveFinisherMove move = KraveFinisherMove.atStep(this.boss.getQteStep());

        // Recomputed while he moves rather than fixed once: the boss drifts, and
        // a launch point pinned to where he was standing ten seconds ago is no
        // longer above him.
        if (this.launch == null || this.t % 20 == 0) {
            Vec3 fresh = KraveFinisher.launchPoint(this.level, this.boss);
            if (fresh != null) {
                this.launch = fresh;
            }
        }

        // Only the aerial throw is launched from Kaiden. The other five are the
        // player's own moves, and gating them on Kaiden finishing a climb he
        // does not need to make would stall every one of them behind a
        // twenty-block flight for no reason.
        if (move != KraveFinisherMove.AERIAL_THROW) {
            // He still has to be somewhere sensible - two of the later moves are
            // his as much as the player's, and all of them want him on screen.
            if (this.launch != null) {
                flyCaydenTo(this.launch.add(4.0D, -6.0D, 0.0D));
            }
            if (++this.prepareTicks > SETTLE_TICKS) {
                enterReady();
            }
            return;
        }

        if (this.launch == null) {
            // Nowhere to throw from - he is under a roof or buried. Give the
            // fight back rather than hanging here with no prompt and no way out.
            if (++this.prepareTicks > PREPARE_TIMEOUT) {
                LOGGER.warn("[CraveBoss] No launch point above the boss after {} ticks; resuming combat.",
                        this.prepareTicks);
                this.boss.restoreForPhase();
                beginCombat();
            }
            return;
        }

        flyCaydenTo(this.launch);

        if (this.cayden.position().distanceTo(this.launch) < IN_POSITION) {
            enterReady();
            return;
        }
        if (++this.prepareTicks > PREPARE_TIMEOUT) {
            // He cannot get there under his own power. This is the one case a
            // teleport is the right answer - it is recovery from a navigation
            // failure, not his way of travelling.
            LOGGER.info("[CraveBoss] Kaiden could not reach the launch point in {} ticks; placing him.",
                    this.prepareTicks);
            KraveFinisher.recover(this.level, this.cayden, this.boss);
            enterReady();
        }
    }

    /** In position above the boss. Now the prompt means something. */
    private void enterReady() {
        this.boss.setBattleState(KraveBattleState.QTE_READY);
        this.qteRetry = false;
        openWindow();
    }

    /**
     * Puts the prompt up for the nearest player.
     *
     * <p>One player is chosen and named in the window rather than accepting the
     * first press from anybody. On a server the prompt then belongs to somebody
     * in particular, and a second player mashing the key cannot fire a finisher
     * that was never offered to them.
     */
    private void openWindow() {
        ServerPlayer target = nearestPlayer();
        if (target == null) {
            // Nobody here to ask. Hold - the fight is frozen anyway - and offer
            // it again as soon as somebody arrives.
            this.phaseTimer = QTE_RETRY_DELAY;
            this.qtePlayer = null;
            return;
        }
        this.qtePlayer = target.getUUID();
        this.phaseTimer = QTE_WINDOW;
        // The form is carried so a press held over from the previous form cannot
        // answer this one: the server compares it back on arrival.
        // Form AND step are both carried and both compared back on arrival. A
        // press held over from the previous attack must not answer this one: in
        // a six-part finisher that skips a move, and at the end of form six it
        // is the difference between playing the finale and ending the encounter.
        this.qteForm = this.boss.getForm();
        this.qteStepAsked = this.boss.getQteStep();
        KraveFinisherMove move = KraveFinisherMove.atStep(this.qteStepAsked);
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                new PacketKraveQte(QTE_WINDOW, this.qteForm, this.qteRetry,
                        this.qteStepAsked, KraveFinisherMove.stepsFor(this.qteForm),
                        move.key(), move.caption()));
        this.level.playSound(null, target.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.PLAYERS, 1.4F, 1.9F);
        LOGGER.info("[CraveBoss] QTE ready for {}: form {} step {} of {}, key {}; Kaiden at {}/{}/{}.",
                target.getGameProfile().getName(), this.qteForm,
                this.qteStepAsked + 1, KraveFinisherMove.stepsFor(this.qteForm),
                KraveFinisherMove.atStep(this.qteStepAsked).key(),
                (int) this.cayden.getX(), (int) this.cayden.getY(), (int) this.cayden.getZ());
    }

    private void tickReady() {
        holdBoss();
        // Held at the launch point. Without this he drifts off it during the
        // window and the throw starts from somewhere else after all.
        if (this.launch != null) {
            flyCaydenTo(this.launch);
        }
        this.cayden.getLookControl().setLookAt(this.boss, 30.0F, 30.0F);

        if (this.t % 5 == 0) {
            this.level.sendParticles(ParticleTypes.END_ROD,
                    this.boss.getX(), this.boss.getY() + this.boss.getBbHeight() * 0.6D, this.boss.getZ(),
                    12, 2.0D, 2.0D, 2.0D, 0.05D);
        }

        if (--this.phaseTimer > 0) {
            return;
        }
        // Two things end at zero and they are not the same thing. A live window
        // running out is a miss; the pause after a miss running out is the cue
        // to ask again. qtePlayer is what tells them apart - it is set only
        // while a prompt is actually up.
        if (this.qtePlayer != null) {
            clearPrompt();
            this.qteRetry = true;
            announce(ChatFormatting.GRAY, "", "The moment passes. Cayden holds him.");
            this.phaseTimer = QTE_RETRY_DELAY;
            return;
        }
        openWindow();
    }

    // ---- FINISHER -----------------------------------------------------------

    /**
     * Runs the cinematic for the current step.
     *
     * <p>Which one is decided entirely by the step index. There is no per-form
     * branching anywhere, because form N simply means the first N moves in
     * order - so the six-part finisher and the one-part finisher are the same
     * code path with a different stopping point, and adding a seventh attack
     * would be one enum constant and one script.
     */
    private void beginFinisher(ServerPlayer player) {
        this.boss.setBattleState(KraveBattleState.FINISHER);
        this.thrown = player.getUUID();
        clearPrompt();

        KraveFinisherMove move = KraveFinisherMove.atStep(this.boss.getQteStep());
        this.script = KraveMoveScripts.forMove(move);
        this.cinematic = new KraveCinematic(this.level, this.boss, this.cayden, player);
        this.scriptTimeout = KraveMoveScripts.timeoutFor(move);

        LOGGER.info("[CraveBoss] Finisher {} ({}) authorised at form {} step {}/{}.",
                move, move.key(), this.boss.getForm(),
                this.boss.getQteStep() + 1, KraveFinisherMove.stepsFor(this.boss.getForm()));
    }

    /**
     * Ticks the running cinematic, and cleans up however it ends.
     *
     * <p>The timeout is the safety net every script needs and none of them
     * implement. A move can be interrupted by terrain, a disconnect, or simply
     * failing to connect, and none of those may leave the boss held in mid-air
     * with his navigation off - which is a far worse outcome than a retry.
     * Release therefore happens here, on every exit path, rather than at the
     * end of each script where only the successful path would reach it.
     */
    private void tickFinisher() {
        ServerPlayer player = this.thrown == null ? null
                : this.level.getServer().getPlayerList().getPlayer(this.thrown);
        if (player == null || player.level() != this.level
                || this.script == null || this.cinematic == null) {
            abandonScript();
            enterPreparing();
            return;
        }

        boolean done;
        try {
            done = this.script.tick(this.cinematic);
        } catch (Exception e) {
            // One broken cinematic must not take the encounter with it.
            LOGGER.error("[CraveBoss] Finisher script failed; offering the step again.", e);
            abandonScript();
            enterPreparing();
            return;
        }
        this.cinematic.t++;

        if (done) {
            finishStep();
            return;
        }
        if (--this.scriptTimeout <= 0) {
            LOGGER.info("[CraveBoss] Finisher step {} timed out at form {}; offering it again.",
                    this.boss.getQteStep() + 1, this.boss.getForm());
            announce(ChatFormatting.GRAY, "", "He twists out of it. Again.");
            abandonScript();
            enterPreparing();
        }
    }

    /**
     * Puts everything a cinematic touched back the way it was.
     *
     * <p>Called on success, on timeout, on exception and on a vanished player.
     * Every one of the six scripts switches off gravity for somebody at some
     * point, and three of them carry the boss; leaving any of that set is how a
     * failed finisher becomes a permanently frozen boss.
     */
    private void abandonScript() {
        // Put the boss back on the ground the move started from, before the
        // cinematic is dropped. Without this a failed sky move leaves him
        // hundreds of blocks up, and the retry - which anchors to where he is -
        // starts from there and climbs again. That is a loop with no exit, and
        // it is what a single failed slam actually did.
        if (this.cinematic != null && this.boss.isAlive()
                && this.boss.getY() > this.cinematic.origin.y + 3.0D) {
            Vec3 back = this.cinematic.origin;
            LOGGER.info("[CraveBoss] Cinematic left the boss at y={}; returning him to y={}.",
                    (int) this.boss.getY(), (int) back.y);
            this.boss.teleportTo(back.x, back.y, back.z);
            this.boss.setDeltaMovement(Vec3.ZERO);
            this.boss.fallDistance = 0.0F;
            this.boss.hurtMarked = true;
        }
        this.script = null;
        this.cinematic = null;
        KraveGrab.release(this.boss, Vec3.ZERO);
        this.boss.setNoGravity(false);
        this.cayden.setNoGravity(false);
        if (this.thrown != null) {
            ServerPlayer p = this.level.getServer().getPlayerList().getPlayer(this.thrown);
            if (p != null) {
                p.setNoGravity(false);
                p.fallDistance = 0.0F;
            }
        }
        this.thrown = null;
    }

    /**
     * One attack landed. Either the form is over, or he gets back up.
     *
     * <p>The step advances only here, on a cinematic that actually completed -
     * never on a timer, and never on the keypress. That is what makes "only the
     * last attack defeats the form" structural rather than something each of
     * six scripts has to be trusted to honour.
     */
    private void finishStep() {
        int form = this.boss.getForm();
        int step = this.boss.getQteStep();
        abandonScript();

        if (KraveFinisherMove.isLastStep(form, step)) {
            LOGGER.info("[CraveBoss] Form {} finished on step {} of {}.",
                    form, step + 1, KraveFinisherMove.stepsFor(form));
            if (form >= KraveMonster.FINAL_FORM) {
                finishEncounter();
            } else {
                enterTransition();
            }
            return;
        }
        this.boss.setQteStep(step + 1);
        enterRecovery();
    }

    // ---- QTE_RECOVERY -------------------------------------------------------

    /**
     * He gets back up between attacks.
     *
     * <p>A real phase rather than a pause inside a script, because the thing it
     * exists for is that the NEXT prompt must not appear while the previous
     * cinematic is still playing. A phase boundary makes that true by
     * construction, instead of six scripts each having to remember it.
     */
    private void enterRecovery() {
        this.boss.setBattleState(KraveBattleState.QTE_RECOVERY);
        this.recoveryTicks = RECOVERY_TICKS + this.boss.getQteStep() * 10;
        // Back to the threshold, not to full. The bar has to stay on the floor
        // or the next attack would have a health bar to chew through first, and
        // the finisher sequence would turn back into a fight.
        this.boss.setHealth(this.boss.getMaxHealth() * KraveMonster.FINISHER_AT);
    }

    private void tickRecovery() {
        holdBoss();
        int step = this.boss.getQteStep();
        if (this.recoveryTicks == RECOVERY_TICKS + step * 10) {
            new KraveCinematic(this.level, this.boss, this.cayden, null).stagger(step + 1);
            announce(ChatFormatting.DARK_RED, "" + ChatFormatting.BOLD, recoveryLine(step));
        }
        if (this.t % 4 == 0) {
            this.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.boss.getX(), this.boss.getY() + 1.0D, this.boss.getZ(),
                    12 + step * 4, 1.8D, 1.0D, 1.8D, 0.02D);
        }
        if (--this.recoveryTicks <= 0) {
            enterPreparing();
        }
    }

    /** Increasingly desperate, the further into a form he gets. */
    private static String recoveryLine(int step) {
        return switch (step) {
            case 1 -> "HE GETS BACK UP.";
            case 2 -> "HE CLIMBS OUT OF THE CRATER.";
            case 3 -> "HE IS STILL STANDING.";
            case 4 -> "HE WILL NOT GO DOWN.";
            case 5 -> "HE REFUSES.";
            default -> "NOT ENOUGH.";
        };
    }


    /**
     * Pins the boss for the length of the cinematic.
     *
     * <p>He does not get to sprint out from under a finisher. Without this the
     * throw is aimed at somewhere he no longer is by the time it arrives.
     */
    private void holdBoss() {
        this.boss.setDeltaMovement(this.boss.getDeltaMovement().multiply(0.1D, 1.0D, 0.1D));
        this.boss.getNavigation().stop();
        this.boss.getLookControl().setLookAt(
                this.cayden.getX(), this.cayden.getY(), this.cayden.getZ(), 30.0F, 30.0F);
    }

    /**
     * Flies Cayden toward a point, refusing to go below the boss on the way.
     *
     * <p>The floor is the whole of the underground bug. His steering takes the
     * direct vector and checks it for obstacles, which says nothing about which
     * side of the terrain he is on: a boss on a floating island, approached from
     * below, is a clear straight line up through open sky into the underside of
     * the world. From down there the line is still clear, so he kept trying.
     */
    private void flyCaydenTo(Vec3 target) {
        this.cayden.setNoGravity(true);
        this.cayden.fallDistance = 0.0F;

        Vec3 to = target.subtract(this.cayden.position());
        double dist = to.length();
        if (dist < 0.05D) {
            this.cayden.setDeltaMovement(this.cayden.getDeltaMovement().scale(0.6D));
            return;
        }
        Vec3 dir = to.scale(1.0D / dist);
        // Climb first, cross second. Going up is always safe here - the launch
        // point is open air by construction - and it is being under things that
        // gets him stuck.
        if (this.cayden.getY() < target.y - 2.0D) {
            dir = new Vec3(dir.x * 0.35D, 1.0D, dir.z * 0.35D).normalize();
        }
        this.cayden.setDeltaMovement(this.cayden.getDeltaMovement().scale(0.6D)
                .add(dir.scale(CAYDEN_CLIMB)));
        this.cayden.hurtMarked = true;
        this.cayden.getLookControl().setLookAt(this.boss, 30.0F, 30.0F);
    }


    // ---- TRANSITION ---------------------------------------------------------

    private void enterTransition() {
        this.boss.setBattleState(KraveBattleState.TRANSITION);
        this.phaseTimer = TRANSITION_TICKS;
        this.boss.setTarget(null);
        this.cayden.setTarget(null);

        int next = this.boss.getForm() + 1;
        this.boss.setForm(next);
        this.boss.restoreForPhase();

        // Form N is met by rung N. Form seven has no rung above Ultra to ask
        // for, so it is met with everything he has, which is the intended shape
        // of a final form.
        this.cayden.ascendTo(Math.min(next, AscensionLadder.ULTRA));

        announce(ChatFormatting.DARK_RED, "" + ChatFormatting.BOLD, "HE IS NOT FINISHED.");
    }

    private void tickTransition() {
        this.boss.setDeltaMovement(this.boss.getDeltaMovement().scale(0.5D));
        if (this.t % 3 == 0) {
            this.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.boss.getX(), this.boss.getY() + 1.0D, this.boss.getZ(),
                    30, this.boss.getBbWidth(), this.boss.getBbHeight() * 0.5D, this.boss.getBbWidth(), 0.08D);
        }
        if (--this.phaseTimer <= 0) {
            beginCombat();
        }
    }

    // ---- DEFEATED -----------------------------------------------------------

    private void finishEncounter() {
        this.boss.setBattleState(KraveBattleState.DEFEATED);
        announce(ChatFormatting.GREEN, "" + ChatFormatting.BOLD, "IT STOPS NOW.");
        clearPrompt();
        if (this.cayden.isAlive()) {
            this.cayden.powerDown();
        }
        // Killed only now, at the end of the last finisher, so the existing death
        // handling - loot, quests, the Kosmos defeat flag in KraveKosmosData -
        // runs exactly once and at the right moment.
        this.boss.hurt(this.boss.damageSources().genericKill(), Float.MAX_VALUE);
    }

    // ---- shared -------------------------------------------------------------

    private void clearPrompt() {
        if (this.qtePlayer != null) {
            ServerPlayer p = this.level.getServer().getPlayerList().getPlayer(this.qtePlayer);
            if (p != null) {
                ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new PacketKraveQte(0, this.boss.getForm(), false, 0, 1, "", ""));
            }
            this.qtePlayer = null;
        }
    }

    private ServerPlayer nearestPlayer() {
        ServerPlayer best = null;
        double bestSqr = Double.MAX_VALUE;
        for (Player p : this.level.getEntitiesOfClass(Player.class,
                this.boss.getBoundingBox().inflate(64.0D))) {
            if (!(p instanceof ServerPlayer sp) || sp.isSpectator()) {
                continue;
            }
            double d = sp.distanceToSqr(this.boss);
            if (d < bestSqr) {
                bestSqr = d;
                best = sp;
            }
        }
        return best;
    }

    /** How far below the boss counts as having left the world. */
    private static final double VOID_LINE = 24.0D;

    /**
     * Puts anyone who fell out of the arena back into it.
     *
     * <p>His attacks demolish the ground, and the Kosmos is floating islands
     * over open void: between a crater and a knockback the fight can throw you
     * off the edge of the world with no way back. The demolition already refuses
     * to dig below the slab it started on, so the arena cannot be destroyed out
     * from under the fight - this covers the other way out, which is being
     * knocked over the side of it.
     *
     * <p>Only while the fight is running, so it never becomes a general
     * no-fall rule that quietly breaks the rest of the dimension.
     */
    private void catchTheFallen() {
        double line = this.boss.getY() - VOID_LINE;
        for (Player p : this.level.getEntitiesOfClass(Player.class,
                this.boss.getBoundingBox().inflate(160.0D))) {
            if (p.getY() > line || p.isSpectator() || p.isCreative()) {
                continue;
            }
            Vec3 back = this.boss.position().add(
                    (this.level.random.nextDouble() - 0.5D) * 12.0D,
                    6.0D,
                    (this.level.random.nextDouble() - 0.5D) * 12.0D);
            p.teleportTo(back.x, back.y, back.z);
            p.setDeltaMovement(0.0D, 0.0D, 0.0D);
            p.fallDistance = 0.0F;
            p.hurtMarked = true;
            p.sendSystemMessage(Component.literal(
                    ChatFormatting.DARK_RED + "The Kosmos is not done with you."));
        }
    }

    private int countNearby(Class<? extends net.minecraft.world.entity.Entity> type) {
        return this.level.getEntitiesOfClass(type, this.boss.getBoundingBox().inflate(80.0D)).size();
    }

    /** Rains Cayden's meteors down on the boss - and only on the boss' side. */
    private void meteorBarrage() {
        Vec3 at = this.boss.position();
        int count = 3 + this.level.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            com.barbarajones.entity.KraveMeteor m = ModEntities.METEOR.get().create(this.level);
            if (m == null) {
                continue;
            }
            double ox = (this.level.random.nextDouble() - 0.5D) * 10.0D;
            double oz = (this.level.random.nextDouble() - 0.5D) * 10.0D;
            m.saiyanStrike(this.cayden);
            m.setPos(at.x + ox, at.y + 42.0D + i * 3.0D, at.z + oz);
            m.aim(-ox * 0.05D, -oz * 0.05D);
            this.level.addFreshEntity(m);
        }
        this.cayden.playSound(ModSounds.KRAVE_ROAR.get(), 1.4F, 1.2F);
    }

    private void spawnMinion() {
        Player owner = this.cayden.getOwner() instanceof Player p ? p : null;
        Vec3 near = owner != null ? owner.position() : this.boss.position();
        double ang = this.level.random.nextDouble() * Math.PI * 2.0D;
        KraveMinion minion = ModEntities.KRAVE_MINION.get().create(this.level);
        if (minion == null) {
            return;
        }
        minion.setPos(near.x + Math.cos(ang) * 10.0D, near.y, near.z + Math.sin(ang) * 10.0D);
        this.level.addFreshEntity(minion);
    }

    private void spawnHealingBox() {
        double ang = this.level.random.nextDouble() * Math.PI * 2.0D;
        KraveHealingBox box = ModEntities.KRAVE_HEALING_BOX.get().create(this.level);
        if (box == null) {
            return;
        }
        Vec3 pos = this.boss.position();
        box.setPos(pos.x + Math.cos(ang) * 8.0D, pos.y, pos.z + Math.sin(ang) * 8.0D);
        box.setHealTarget(this.boss);
        this.level.addFreshEntity(box);
    }

    private void announce(ChatFormatting color, String extra, String message) {
        for (Player p : this.level.getEntitiesOfClass(Player.class, this.boss.getBoundingBox().inflate(96.0D))) {
            p.sendSystemMessage(Component.literal(color + extra + message));
        }
    }

    /** Distance a player must close before the encounter wakes. */
    static double triggerRange() {
        return TRIGGER_RANGE;
    }
}
