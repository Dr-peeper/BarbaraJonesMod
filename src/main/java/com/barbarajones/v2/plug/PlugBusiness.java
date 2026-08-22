package com.barbarajones.v2.plug;

import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.ThePlug;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The counter. Everything a player can do with The Plug's job board goes through
 * here, so {@code ThePlug.java} only ever gains three call sites and keeps every
 * line of behaviour it already had.
 *
 * <h2>How a player actually uses him</h2>
 * <ul>
 *   <li><b>Sneak + empty hand</b> - flips to the next job on the board and quotes
 *       the price.
 *   <li><b>Sneak + emeralds / dollars / a $500</b> - hires him for whatever job is
 *       on the board. He does not make change, so a big note is a tip, and a tip
 *       is the fastest honest way to make him better at his job.
 *   <li><b>Sneak + something he likes</b> - a gift. See {@code PlugGifts}.
 *   <li><b>Empty hand, no sneak</b> - collect a finished job, or get talked at.
 * </ul>
 *
 * <p>Sneaking is what keeps all of this out of the way of the two deals he
 * already had: a plain click with Mom's $500 still buys the bundle of nothing,
 * and a plain click with an emerald still buys grass at his usual terrible rate.
 *
 * <p>Every entry point calls {@link PlugJobData#refresh} first. A job coming due
 * is noticed by whichever happens first - the slow timer or the player walking
 * up - and both roads lead to the same idempotent routine, so the timer and the
 * counter can never end up holding different opinions about whether he is back.
 */
public final class PlugBusiness {

    private PlugBusiness() { }

    // ---- entry points --------------------------------------------------------

    /**
     * Sneak-click. Hires him, takes a gift, or flips the board, depending on what
     * is in the player's hand.
     *
     * <p>Currency is checked before gifts on purpose: a $500 note is both
     * something he would happily accept as a present and the largest denomination
     * he takes, and if the gift branch won there would be no way to pay him with
     * one. It is not in the gift table at all for the same reason.
     */
    public static void negotiate(ThePlug plug, Player player, ItemStack held) {
        MinecraftServer server = plug.level().getServer();
        if (server == null) {
            return;
        }
        PlugJobData data = PlugJobData.get(server);
        data.refresh(server);
        PlugReputation rep = data.reputation(player.getUUID());
        RandomSource random = plug.getRandom();

        if (PlugCurrency.isCurrency(held)) {
            hire(plug, player, held, server, data, rep, random);
            return;
        }
        PlugGifts.Gift gift = PlugGifts.find(held);
        if (gift != null) {
            accept(plug, player, held, server, data, rep, gift);
            return;
        }
        if (held.isEmpty()) {
            board(player, server, data, rep);
            return;
        }
        PlugLines.say(player, PlugLines.pick(random, PlugLines.REFUSE_ITEM));
    }

    /**
     * Plain empty-handed click: hand over a finished job if there is one, and
     * otherwise let him talk.
     */
    public static void streetTalk(ThePlug plug, Player player) {
        MinecraftServer server = plug.level().getServer();
        if (server == null) {
            return;
        }
        PlugJobData data = PlugJobData.get(server);
        data.refresh(server);
        PlugReputation rep = data.reputation(player.getUUID());
        RandomSource random = plug.getRandom();
        PlugContract contract = data.contract(player.getUUID());

        if (contract == null) {
            PlugLines.say(player, PlugLines.pick(random, PlugLines.IDLE));
            if (rep.hires() == 0) {
                PlugLines.note(player, "(sneak + click him with an empty hand to see what he does.)");
            }
            return;
        }
        if (contract.isAway()) {
            PlugLines.say(player, PlugLines.pick(random, PlugLines.BUSY));
            reportContract(player, contract, server);
            return;
        }
        deliver(plug, player, data, rep, contract, random);
    }

    // ---- the board -----------------------------------------------------------

    private static void board(Player player, MinecraftServer server,
                              PlugJobData data, PlugReputation rep) {
        PlugJob job = rep.selected().next();
        rep.select(job);
        data.setDirty();

        PlugLines.say(player, "I can " + job.pitch() + ". " + PlugCurrency.quote(job.price()) + ".");
        PlugLines.note(player, "(sneak + click with emeralds, dollars or a $500 to send him.)");

        PlugContract contract = data.contract(player.getUUID());
        if (contract != null) {
            reportContract(player, contract, server);
        } else if (rep.hires() > 0) {
            PlugLines.note(player, "(he rates you " + PlugCompetence.title(rep.level())
                    + " - competence " + rep.level() + "/" + PlugCompetence.MAX_LEVEL
                    + ", " + rep.points() + " rep, " + rep.delivered() + " jobs run.)");
        }
    }

    // ---- hiring --------------------------------------------------------------

    private static void hire(ThePlug plug, Player player, ItemStack held, MinecraftServer server,
                             PlugJobData data, PlugReputation rep, RandomSource random) {
        PlugContract existing = data.contract(player.getUUID());
        if (existing != null) {
            PlugLines.say(player, PlugLines.pick(random,
                    existing.isReady() ? PlugLines.WAITING : PlugLines.BUSY));
            reportContract(player, existing, server);
            return;
        }

        PlugJob job = rep.selected();
        int paid = PlugCurrency.take(player, held, job.price());
        if (paid <= 0) {
            PlugLines.say(player, PlugLines.pick(random, PlugLines.BROKE));
            PlugLines.note(player, "(" + job.label() + " is " + PlugCurrency.quote(job.price()) + ".)");
            return;
        }

        // Competence is read BEFORE this hire's own reputation is added, so the
        // job you just paid for is done by the man you hired, not by the slightly
        // better man that paying for it turned him into. Paying double or more
        // buys one level for this job only - it is never written to his opinion
        // of you, so the two can never drift apart.
        int level = rep.level();
        int tip = paid - job.price();
        int competence = PlugCompetence.clampLevel(level + (tip >= job.price() ? 1 : 0));

        rep.noteHire();
        rep.add(2 + Math.min(4, tip / 2));

        long now = server.overworld().getGameTime();
        int ticks = PlugCompetence.jobTicks(job, competence, random);
        data.put(new PlugContract(player.getUUID(), plug.getUUID(), job, competence, paid, now + ticks));

        plug.level().playSound(null, plug.blockPosition(), SoundEvents.ITEM_PICKUP,
                plug.getSoundSource(), 0.9F, 1.3F);
        PlugLines.say(player, PlugLines.pick(random,
                competence >= 3 ? PlugLines.TAKING_JOB_HIGH : PlugLines.TAKING_JOB_LOW));
        if (tip >= job.price()) {
            PlugLines.say(player, "and you overpaid me. I ain't givin it back but I'ma remember it.");
        }
        PlugLines.say(player, PlugLines.pick(random, PlugLines.LEAVING));
        PlugLines.note(player, "(he's gone to " + job.label() + ". back in about "
                + (ticks / 20) + " seconds.)");
        reportLevel(player, level, rep);

        // Immediately, not on his next sync tick: he should vanish as he says the
        // line, not a second after the player has already turned away.
        syncPresence(plug);
    }

    // ---- delivery ------------------------------------------------------------

    private static void deliver(ThePlug plug, Player player, PlugJobData data,
                                PlugReputation rep, PlugContract contract, RandomSource random) {
        int before = rep.level();

        // A contract outlives the man who took it. If this is not the Plug you
        // paid, he explains the situation rather than the goods vanishing with
        // whoever got shot.
        if (contract.plug() != null && !contract.plug().equals(plug.getUUID())) {
            PlugLines.say(player, PlugLines.pick(random, PlugLines.STAND_IN));
        }

        PlugLines.say(player, contract.line());
        for (ItemStack stack : contract.haul()) {
            give(player, stack.copy());
        }
        if (!contract.bonusLine().isEmpty()) {
            PlugLines.say(player, contract.bonusLine());
        }

        if (contract.isRipOff()) {
            PlugLines.say(player, PlugLines.pick(random, PlugLines.RIP_OFF_FOLLOW_UP));
            PlugLines.note(player, "(he kept all " + contract.paid() + " of your bands.)");
            plug.level().playSound(null, plug.blockPosition(), ModSounds.KRAVE_LAUGH.get(),
                    plug.getSoundSource(), 0.8F, 1.4F);
            // He still thinks that went great, and he does like you more for it.
            // Getting robbed is not a dead end - it is slow progress.
            rep.add(1);
        } else {
            rep.add(3);
        }
        rep.noteDelivery();

        data.clearContract(player.getUUID());
        data.setDirty();
        reportLevel(player, before, rep);
    }

    // ---- gifts ---------------------------------------------------------------

    private static void accept(ThePlug plug, Player player, ItemStack held, MinecraftServer server,
                               PlugJobData data, PlugReputation rep, PlugGifts.Gift gift) {
        int before = rep.level();
        long day = server.overworld().getDayTime() / 24000L;
        int granted = rep.acceptGift(gift.reputation(), day);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        data.setDirty();

        PlugLines.say(player, gift.line());
        if (granted < 0) {
            PlugLines.note(player, "(that cost you.)");
        } else if (granted == 0 && gift.reputation() > 0) {
            PlugLines.note(player, "(he's had enough presents today. try him tomorrow.)");
        }
        plug.level().playSound(null, plug.blockPosition(), SoundEvents.ITEM_PICKUP,
                plug.getSoundSource(), 0.9F, granted < 0 ? 0.7F : 1.5F);
        reportLevel(player, before, rep);
    }

    // ---- presence ------------------------------------------------------------

    /**
     * Makes this Plug's body agree with the books: gone while he is out on a job,
     * standing there the rest of the time.
     *
     * <p>The save data is the only authority on whether a job exists. The flag on
     * the entity records nothing but "I have already applied the away look", so
     * the transition fires exactly once in each direction and can self-correct
     * from either side: wipe the save data and the next sync puts him back,
     * reload the entity mid-job and the next sync hides him again.
     *
     * <p>Cheap by design - a world where nobody has ever hired him has no save
     * file, so this is one cached lookup that returns null and stops.
     */
    public static void syncPresence(ThePlug plug) {
        if (!(plug.level() instanceof ServerLevel level)) {
            return;
        }
        PlugJobData data = PlugJobData.getExisting(level.getServer());
        boolean away = data != null && data.isPlugAway(plug.getUUID());
        if (away == plug.isAwayApplied()) {
            return;
        }
        if (away) {
            plug.setTarget(null);
            plug.setInvisible(true);
            plug.setSilent(true);
            plug.setNoAi(true);
            puff(level, plug, true);
        } else {
            plug.setInvisible(false);
            plug.setSilent(false);
            plug.setNoAi(false);
            puff(level, plug, false);
        }
        plug.setAwayApplied(away);
    }

    private static void puff(ServerLevel level, ThePlug plug, boolean leaving) {
        level.sendParticles(leaving ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                plug.getX(), plug.getY() + 1.0D, plug.getZ(), 24, 0.3D, 0.6D, 0.3D, 0.02D);
    }

    // ---- swinging on him -----------------------------------------------------

    /**
     * Hitting your own Plug. Called from {@link PlugEvents}; costs real standing,
     * because the alternative is a player who farms him for the ski mask between
     * jobs and still gets treated like family.
     */
    static void assaulted(ThePlug plug, Player player) {
        MinecraftServer server = plug.level().getServer();
        if (server == null) {
            return;
        }
        PlugJobData data = PlugJobData.getExisting(server);
        if (data == null) {
            return;
        }
        PlugReputation rep = data.peek(player.getUUID());
        if (rep == null) {
            return;   // never done business with him, so nothing to damage - he
                      // just gets on with being angry the ordinary way
        }
        rep.add(-10);
        data.setDirty();
        PlugLines.say(player, PlugLines.pick(plug.getRandom(), PlugLines.HIT));
    }

    // ---- shared bits ---------------------------------------------------------

    private static void reportContract(Player player, PlugContract contract, MinecraftServer server) {
        if (contract.isReady()) {
            PlugLines.note(player, "(he's holding your " + contract.job().label()
                    + " bag. empty hand, click him.)");
            return;
        }
        int seconds = contract.secondsLeft(server.overworld().getGameTime());
        PlugLines.note(player, "(he's out on " + contract.job().label() + " - about "
                + seconds + " seconds out.)");
    }

    /** Says something only when the level actually moved, so it stays worth reading. */
    private static void reportLevel(Player player, int before, PlugReputation rep) {
        int after = rep.level();
        if (after <= before) {
            return;
        }
        PlugLines.note(player, "(he rates you " + PlugCompetence.title(after)
                + " now - competence " + after + "/" + PlugCompetence.MAX_LEVEL + ".)");
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
