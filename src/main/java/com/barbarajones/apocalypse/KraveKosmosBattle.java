package com.barbarajones.apocalypse;

import com.barbarajones.boss.krave.KraveAttacks;
import com.barbarajones.boss.krave.KraveBattleState;
import com.barbarajones.boss.krave.KraveDemolition;
import com.barbarajones.boss.krave.KraveFinisher;
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

    /** How long the player is held aloft before release - the beat that sells it. */
    private static final int HOLD_TICKS = 22;

    /**
     * Blocks per tick on the way down. Far past ordinary knockback on purpose:
     * this is a scripted dive, and at survivable Minecraft speeds it reads as
     * falling rather than being thrown.
     */
    private static final double THROW_SPEED = 2.8D;

    /** How fast Cayden climbs to his launch point. */
    private static final double CAYDEN_CLIMB = 1.1D;

    /** Slack on the impact test, so a fast dive cannot tunnel past the hitbox. */
    private static final double IMPACT_REACH = 2.5D;

    /** Throws recovered before the prompt is offered again. */
    private static final int MAX_THROW_RETRIES = 2;

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
    private int holdTicks;
    private int retries;
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
    public static void onQteInput(ServerPlayer player, int claimedForm) {
        for (KraveKosmosBattle b : ACTIVE) {
            KraveBattleState state = b.boss.getBattleState();
            if (state != KraveBattleState.QTE_READY && state != KraveBattleState.QTE) {
                continue;                       // not asking, or not asking yet
            }
            if (!player.getUUID().equals(b.qtePlayer)) {
                continue;                       // this prompt is not theirs
            }
            if (claimedForm != b.qteForm) {
                // A press held over from a prompt that has already been
                // answered or expired. Answering the current form with it would
                // skip a stage.
                LOGGER.info("[CraveBoss] Ignoring a stale finisher press for form {} (current {}).",
                        claimedForm, b.qteForm);
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

        // Recomputed while he climbs rather than fixed once: the boss drifts,
        // and a launch point pinned to where he was standing ten seconds ago is
        // no longer above him.
        if (this.launch == null || this.t % 20 == 0) {
            Vec3 fresh = KraveFinisher.launchPoint(this.level, this.boss);
            if (fresh != null) {
                this.launch = fresh;
            }
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
        this.qteForm = this.boss.getForm();
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                new PacketKraveQte(QTE_WINDOW, this.qteForm, this.qteRetry));
        this.level.playSound(null, target.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.PLAYERS, 1.4F, 1.9F);
        LOGGER.info("[CraveBoss] QTE ready for {} at form {}; Kaiden at {}/{}/{}.",
                target.getGameProfile().getName(), this.qteForm,
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
     * Cayden takes the player up and throws them down at the Monster.
     *
     * <p>Pressing the key does not teleport anybody into the boss. It authorises
     * a sequence: the player is lifted to Cayden, held there long enough to see
     * where they are and what is underneath them, and then launched. The blow is
     * landed by the collision at the end, not by a timer.
     */
    private void beginFinisher(ServerPlayer player) {
        this.boss.setBattleState(KraveBattleState.FINISHER);
        this.thrown = player.getUUID();
        this.phaseTimer = FINISHER_TIMEOUT;
        this.holdTicks = HOLD_TICKS;
        clearPrompt();

        Vec3 hold = holdPosition();
        player.teleportTo(hold.x, hold.y, hold.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        // Survives being used as ammunition, and survives the fall if the throw
        // goes wrong. Long enough to cover the whole cinematic and the recovery.
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                FINISHER_TIMEOUT + 120, 5, false, false));

        announce(ChatFormatting.GOLD, "" + ChatFormatting.BOLD, "CAYDEN HAS YOU.");
        this.level.playSound(null, this.cayden.blockPosition(), ModSounds.KRAVE_ROAR.get(),
                SoundSource.PLAYERS, 1.6F, 1.4F);
        LOGGER.info("[CraveBoss] Finisher authorised at form {}; launch {}/{}/{}, boss {}/{}/{}.",
                this.boss.getForm(), (int) hold.x, (int) hold.y, (int) hold.z,
                (int) this.boss.getX(), (int) this.boss.getY(), (int) this.boss.getZ());
    }

    /** Just under Cayden, so the throw visibly starts from his hands. */
    private Vec3 holdPosition() {
        Vec3 base = this.launch != null ? this.launch : this.cayden.position();
        return base.add(0.0D, -1.6D, 0.0D);
    }

    private void tickFinisher() {
        holdBoss();

        ServerPlayer player = this.thrown == null ? null
                : this.level.getServer().getPlayerList().getPlayer(this.thrown);
        if (player == null || player.level() != this.level) {
            // Logged out or changed dimension mid-throw. Put the prompt back
            // rather than leaving the phase with no projectile in it.
            this.thrown = null;
            enterPreparing();
            return;
        }
        player.fallDistance = 0.0F;

        // ---- the hold ------------------------------------------------------
        if (this.holdTicks > 0) {
            this.holdTicks--;
            Vec3 hold = holdPosition();
            player.teleportTo(hold.x, hold.y, hold.z);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            this.cayden.getLookControl().setLookAt(this.boss, 60.0F, 60.0F);
            this.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    hold.x, hold.y, hold.z, 8, 0.5D, 0.5D, 0.5D, 0.15D);
            if (this.holdTicks == 0) {
                release(player);
            }
            return;
        }

        // ---- the fall ------------------------------------------------------
        // Re-aimed every tick. A dead-reckoned shot misses whenever the boss
        // shifts even slightly, and missing every time is exactly what the log
        // showed: a five-second timeout, over and over.
        Vec3 aim = this.boss.position()
                .add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D)
                .subtract(player.position());
        double dist = aim.length();
        if (dist > 0.01D) {
            Vec3 steer = aim.scale(THROW_SPEED / dist);
            // Blended rather than assigned, so it reads as a dive with weight
            // behind it instead of a position update every tick.
            player.setDeltaMovement(player.getDeltaMovement().scale(0.45D).add(steer.scale(0.55D)));
            player.hurtMarked = true;
        }
        this.level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 0.4D, player.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.03D);
        this.level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 0.4D, player.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.01D);

        // The collision IS the success condition. Nothing here advances the form
        // because enough ticks went by.
        if (player.getBoundingBox().inflate(IMPACT_REACH).intersects(this.boss.getBoundingBox())) {
            impact(player);
            return;
        }
        if (--this.phaseTimer <= 0) {
            missed(player);
        }
    }

    /** The moment of release: velocity down the line to the boss. */
    private void release(ServerPlayer player) {
        Vec3 aim = this.boss.position()
                .add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D)
                .subtract(player.position());
        double dist = Math.max(0.01D, aim.length());
        player.setDeltaMovement(aim.scale(THROW_SPEED / dist));
        player.hurtMarked = true;
        this.level.playSound(null, player.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.PLAYERS, 2.0F, 0.7F);
        announce(ChatFormatting.GOLD, "" + ChatFormatting.BOLD, "GO.");
    }

    /**
     * The throw did not connect. Never a silent phase advance.
     *
     * <p>Two chances at recovering the same throw before the prompt is offered
     * again, because being blocked by a stray minion should not cost the player
     * the whole sequence.
     */
    private void missed(ServerPlayer player) {
        this.thrown = null;
        if (++this.retries <= MAX_THROW_RETRIES) {
            LOGGER.info("[CraveBoss] Throw missed at form {} (retry {}). Resetting to the launch point.",
                    this.boss.getForm(), this.retries);
            announce(ChatFormatting.GRAY, "", "Wide. Cayden catches you.");
            beginFinisher(player);
            return;
        }
        this.retries = 0;
        announce(ChatFormatting.GRAY, "", "Cayden resets.");
        enterPreparing();
    }

    /** The blow that actually ends a form. */
    private void impact(ServerPlayer player) {
        this.thrown = null;
        this.retries = 0;
        player.setDeltaMovement(player.getDeltaMovement().scale(-0.25D).add(0.0D, 0.6D, 0.0D));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        Vec3 at = this.boss.position().add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D);
        this.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 8, 2.5D, 2.5D, 2.5D, 0.0D);
        this.level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 4, 1.0D, 1.0D, 1.0D, 0.0D);
        this.level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 2, 0.5D, 0.5D, 0.5D, 0.0D);
        this.level.playSound(null, this.boss.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.HOSTILE, 2.0F, 0.4F);

        // A ring of blown-back air at the point of contact, plus the crater.
        KraveAttacks.dome(this.level, at, 10.0D, ParticleTypes.SOUL_FIRE_FLAME);
        KraveAttacks.shockwave(this.level, this.boss, null, this.boss.position(), 8.0D, 0.0F, 5);
        // Routed through the boss's own demolition so it obeys the same arena
        // floor and castle protection as everything else. A finisher that
        // punched through the island would end the fight by dropping it into the
        // void.
        KraveDemolition.crater(this.level, this.boss, this.boss.position(), 10.0D, 5);

        // Hit-stop: he is stopped dead for a beat rather than sliding away from
        // the blow that just landed.
        this.boss.setDeltaMovement(Vec3.ZERO);
        this.boss.hurtMarked = true;
        this.boss.hurtTime = 20;

        LOGGER.info("[CraveBoss] Form {} finished by impact at {}/{}/{}.",
                this.boss.getForm(), (int) at.x, (int) at.y, (int) at.z);

        if (this.boss.getForm() >= KraveMonster.FINAL_FORM) {
            finishEncounter();
        } else {
            enterTransition();
        }
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
                        new PacketKraveQte(0, this.boss.getForm(), false));
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
