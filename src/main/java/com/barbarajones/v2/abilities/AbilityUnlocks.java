package com.barbarajones.v2.abilities;

import com.barbarajones.quest.Quests;
import com.barbarajones.quest.expansion.QuestExpansion;

/**
 * Where each ability's unlock comes from, in one place, so the tooltip, the
 * HUD lock icon and the granting code in {@code AbilityEvents} all say and
 * check the same thing - and, not incidentally, so this matches exactly what
 * the in-game manual's own Player Abilities chapter already documents
 * ("Slay the Krave Monster", "Put The Manager in the ground", "Defeat Mom
 * Cobb", "Finish the Revenge quest", "Reach the Kosmos Ascension (Kosmos
 * Master)", "Finish the entire base questline (Peace at Last)"), written
 * independently by whatever built that chapter. Two gates exist:
 *
 * <ul>
 *   <li><b>Quest-gated</b> - {@link #questFor} returns the quest id.
 *       {@code AbilityEvents} polls {@code Quests.isDone(player, id)} the
 *       same way {@code KraveHud} watches for toast-worthy completions, and
 *       grants the moment it flips true. Every id named here already exists
 *       on the base quest graph (including the expansion branches, which
 *       splice into the same book storage) - nothing needed registering.</li>
 *   <li><b>Boss-gated</b> - {@link AbilityId#LASER} and
 *       {@link AbilityId#CHARM}. {@code AbilityEvents#onDeath} grants on the
 *       matching {@code LivingDeathEvent}, crediting every player standing
 *       in the fight (Cayden and Barbara land plenty of the actual hits,
 *       same as {@code ProgressionEvents} does for the Krave Monster).</li>
 * </ul>
 *
 * <p>{@link AbilityId#GODCORE} is gated on {@link #GODCORE_QUEST} like the
 * other four, but {@code AbilityEvents#watchUnlocks} also accepts a grant
 * from the newer {@code v2.quests} module directly
 * (({@code QuestApi.hasAbility(player, "i_krave_the_krave")}) - that string
 * is not this module's invention, it is already the {@code ability} reward
 * on that module's own questline finale, so recognizing it costs nothing and
 * means a player who finishes either questline's ending gets the same power.
 *
 * <p>A quest system built later that wants to hand these out directly - as a
 * quest reward, a command, anything - should call
 * {@link AbilityData#grant(net.minecraft.server.level.ServerPlayer, AbilityId)}.
 * It is idempotent and does its own sync/notify, so nothing else has to.
 */
public final class AbilityUnlocks {

    private AbilityUnlocks() { }

    /** Quest id gating {@link AbilityId#GAUNTLET}: kill the Krave Monster. */
    public static final String GAUNTLET_QUEST = Quests.SLAY_KRAVE;
    /** Quest id gating {@link AbilityId#TOTEM}: finish the Revenge quest. */
    public static final String TOTEM_QUEST = Quests.REVENGE;
    /** Quest id gating {@link AbilityId#BAND}: finish the Kosmos branch's capstone. */
    public static final String BAND_QUEST = QuestExpansion.KOSMOS_MASTER;
    /** Quest id gating {@link AbilityId#GODCORE}: finish the entire base questline. */
    public static final String GODCORE_QUEST = Quests.PEACE;

    /**
     * The quest id gating {@code id}, or null when it is gated by a boss kill
     * instead (see the class doc). Every quest named here already exists on
     * the graph - nothing to register.
     */
    public static String questFor(AbilityId id) {
        return switch (id) {
            case GAUNTLET -> GAUNTLET_QUEST;
            case TOTEM -> TOTEM_QUEST;
            case BAND -> BAND_QUEST;
            case GODCORE -> GODCORE_QUEST;
            default -> null;
        };
    }

    /** One line of prose for the tooltip and the lock chat message. */
    public static String hint(AbilityId id) {
        return switch (id) {
            case GAUNTLET -> "Unlocks by slaying the Krave Monster.";
            case LASER -> "Unlocks by putting The Manager in the ground.";
            case CHARM -> "Unlocks by defeating Mom Cobb.";
            case TOTEM -> "Unlocks by finishing the Revenge quest.";
            case BAND -> "Unlocks by reaching the Kosmos Ascension (Kosmos Master).";
            case GODCORE -> "Unlocks by finishing the entire questline (Peace at Last).";
        };
    }
}
