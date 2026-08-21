package com.barbarajones.apocalypse;

import com.barbarajones.boss.krave.KraveBattleState;
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
    public static void onQteInput(ServerPlayer player) {
        for (KraveKosmosBattle b : ACTIVE) {
            if (b.boss.getBattleState() == KraveBattleState.QTE
                    && player.getUUID().equals(b.qtePlayer)) {
                b.qtePlayer = null;
                b.beginFinisher(player);
                return;
            }
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

        return switch (this.boss.getBattleState()) {
            case CONFRONTATION -> { tickConfrontation(); yield false; }
            case COMBAT -> { tickCombat(); yield false; }
            case QTE -> { tickQte(); yield false; }
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
        this.boss.setForm(1);
        this.boss.restoreForPhase();
        this.boss.setTarget(null);
        this.cayden.setTarget(null);

        announce(ChatFormatting.DARK_RED, "" + ChatFormatting.BOLD, "THE KRAVE MONSTER STIRS.");
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
            this.cayden.ascendTo(AscensionLadder.SSJ);
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
            enterQte();
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

    // ---- QTE ----------------------------------------------------------------

    private void enterQte() {
        this.boss.setBattleState(KraveBattleState.QTE);
        this.boss.setTarget(null);
        this.cayden.setTarget(null);
        this.qteRetry = false;
        openWindow();
    }

    /**
     * Puts the prompt up for the nearest player.
     *
     * <p>One player is chosen and named in the window rather than accepting the
     * first press from anybody. On a server that means the prompt belongs to
     * somebody in particular, and a second player mashing the key cannot fire
     * a finisher that was never offered to them.
     */
    private void openWindow() {
        ServerPlayer target = nearestPlayer();
        if (target == null) {
            // Nobody here to ask. Hold the phase - the fight is frozen anyway -
            // and offer it again as soon as somebody arrives.
            this.phaseTimer = QTE_RETRY_DELAY;
            this.qtePlayer = null;
            return;
        }
        this.qtePlayer = target.getUUID();
        this.phaseTimer = QTE_WINDOW;
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
                new PacketKraveQte(QTE_WINDOW, this.boss.getForm(), this.qteRetry));
        this.level.playSound(null, target.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.PLAYERS, 1.4F, 1.9F);
    }

    private void tickQte() {
        // Held completely still. This is the pause the whole finisher depends on.
        this.boss.setDeltaMovement(this.boss.getDeltaMovement().scale(0.4D));
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
        //
        // Written as one branch originally, which announced the miss and
        // reopened the prompt on the same tick: the pause never happened and the
        // player was told the moment had passed while looking at the new prompt.
        if (this.qtePlayer != null) {
            clearPrompt();
            this.qteRetry = true;
            announce(ChatFormatting.GRAY, "", "The moment passes. Cayden holds him.");
            this.phaseTimer = QTE_RETRY_DELAY;
            return;
        }
        // Never a softlock: the boss stays at the threshold and undamageable, so
        // a player who cannot manage the timing is delayed, not stopped - and one
        // who walked away is asked again the moment they come back.
        openWindow();
    }

    // ---- FINISHER -----------------------------------------------------------

    /**
     * Cayden picks the player up and throws them at the Monster.
     *
     * <p>The player is the projectile, which is the point - the finisher is the
     * one part of the fight that is not Cayden doing it for them.
     */
    private void beginFinisher(ServerPlayer player) {
        this.boss.setBattleState(KraveBattleState.FINISHER);
        this.thrown = player.getUUID();
        this.phaseTimer = FINISHER_TIMEOUT;
        clearPrompt();

        announce(ChatFormatting.GOLD, "" + ChatFormatting.BOLD, "CAYDEN HAS YOU.");

        // Snatched up to Cayden, then flung. Teleporting the player here is
        // deliberate and bounded - it is one scripted grab, not a navigation
        // shortcut - and it is the only way the throw can start from his hands.
        Vec3 grab = this.cayden.position().add(0.0D, this.cayden.getBbHeight() + 0.5D, 0.0D);
        player.teleportTo(grab.x, grab.y, grab.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;

        Vec3 aim = this.boss.position()
                .add(0.0D, this.boss.getBbHeight() * 0.55D, 0.0D)
                .subtract(grab).normalize().scale(2.6D);
        player.setDeltaMovement(aim.x, aim.y + 0.35D, aim.z);
        player.hurtMarked = true;
        // Survives being used as ammunition. Without this the impact he is being
        // thrown into is the thing that kills him.
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, FINISHER_TIMEOUT + 60, 5,
                false, false));
        player.fallDistance = 0.0F;

        this.level.playSound(null, this.cayden.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.PLAYERS, 1.8F, 0.8F);
    }

    private void tickFinisher() {
        ServerPlayer player = this.thrown == null ? null
                : this.level.getServer().getPlayerList().getPlayer(this.thrown);
        if (player == null || player.level() != this.level) {
            // They logged out or changed dimension mid-throw. Put the prompt
            // back rather than leaving the phase with no projectile in it.
            this.thrown = null;
            enterQte();
            return;
        }

        player.fallDistance = 0.0F;
        this.level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 0.5D, player.getZ(), 6, 0.2D, 0.2D, 0.2D, 0.02D);

        boolean hit = player.getBoundingBox().inflate(1.5D).intersects(this.boss.getBoundingBox());
        if (hit) {
            impact(player);
            return;
        }
        if (--this.phaseTimer <= 0) {
            // The throw missed - knocked off course, or he moved. Not a
            // softlock: back to the prompt and try again.
            this.thrown = null;
            announce(ChatFormatting.GRAY, "", "Wide. Cayden resets.");
            enterQte();
        }
    }

    /** The blow that actually ends a form. */
    private void impact(ServerPlayer player) {
        this.thrown = null;
        player.setDeltaMovement(player.getDeltaMovement().scale(-0.4D).add(0.0D, 0.5D, 0.0D));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        Vec3 at = this.boss.position().add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D);
        this.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 6, 2.0D, 2.0D, 2.0D, 0.0D);
        this.level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 3, 1.0D, 1.0D, 1.0D, 0.0D);
        this.level.playSound(null, this.boss.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                SoundSource.HOSTILE, 2.0F, 0.5F);

        // The impact craters the arena. Routed through the boss's own demolition
        // so it obeys the same arena floor and castle protection as everything
        // else - a finisher that punched through the island would end the fight
        // by dropping it into the void.
        com.barbarajones.boss.krave.KraveDemolition.crater(
                this.level, this.boss, this.boss.position(), 9.0D, 4);

        if (this.boss.getForm() >= KraveMonster.FINAL_FORM) {
            finishEncounter();
        } else {
            enterTransition();
        }
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
