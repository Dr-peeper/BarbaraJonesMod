package com.barbarajones.boss.krave;

/**
 * The one authoritative answer to "what is happening in this boss fight".
 *
 * <p>Before this the encounter was a handful of independent booleans -
 * {@code bossFightActive} on the Monster, {@code ssjUntilBossDies} on Cayden,
 * an entry in a static list in the battle controller, a form number, and the
 * death event - and no two of them were ever obliged to agree. That is how a
 * fight could start because Cayden happened to notice the Monster across the
 * dimension, and how a form could advance twice for one defeat.
 *
 * <p>The state lives on the Monster, is synced to clients so the HUD can draw
 * the right thing, and is written to his NBT so unloading the chunk, leaving
 * the dimension or reconnecting resumes the encounter rather than corrupting
 * it. Everything else reads it and nothing else owns it.
 *
 * <p>Ordinals are persisted. Append new states; do not reorder these.
 */
public enum KraveBattleState {

    /**
     * He is here and he is not fighting. The pre-fight state.
     *
     * <p>Neither side may target the other while he is dormant, which is what
     * stops the encounter beginning simply because a player walked into the
     * dimension and Cayden's boss scan reached far enough to see him.
     */
    DORMANT,

    /**
     * The stare-down. Player has arrived, positions are being taken, Cayden is
     * going Super Saiyan. Nothing may deal damage during this.
     */
    CONFRONTATION,

    /** A form is being fought normally. The only state in which damage lands. */
    COMBAT,

    /**
     * The current form is spent and the prompt is up. The Monster cannot die,
     * cannot be damaged further, and the fight is held here until the player
     * answers - or fails, and is asked again.
     */
    QTE,

    /**
     * The form is spent and Cayden is flying to his launch position above the
     * boss. No prompt yet.
     *
     * <p>Split out because the prompt used to appear the instant the threshold
     * was crossed, while Cayden was still wherever the fight had left him -
     * across the arena, underground, or on the far side of the castle. Pressing
     * it then produced a throw from nowhere. There is nothing to press until he
     * is actually in position.
     */
    QTE_PREPARING,

    /**
     * Cayden is in position above the boss and the prompt is live. The only
     * state in which the finisher key does anything.
     */
    QTE_READY,

    /**
     * The player has been thrown and is in the air. Runs to the impact, which
     * is what actually ends the form.
     */
    FINISHER,

    /**
     * Between two attacks of the same form. He is getting back up.
     *
     * <p>Its own state rather than a timer inside the attack, because the
     * requirement that matters most here is that the next prompt does NOT
     * appear while the previous cinematic is still playing. A phase boundary
     * makes that structural instead of something each of six scripts has to
     * remember.
     */
    QTE_RECOVERY,

    /** Between forms: the Monster grows, Cayden ascends to match him. */
    TRANSITION,

    /** The whole encounter is over. Terminal. */
    DEFEATED;

    private static final KraveBattleState[] BY_ID = values();

    public static KraveBattleState byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : DORMANT;
    }

    /**
     * Whether ordinary combat damage is allowed to land right now.
     *
     * <p>Every scripted beat needs the fight to hold still. Without this a
     * stray laser already in flight can kill the Monster during his own
     * finisher, which skips a form and leaves Cayden ascended against nothing.
     */
    public boolean damageable() {
        return this == COMBAT;
    }

    /** Whether the two of them are allowed to acquire each other as targets. */
    public boolean hostile() {
        return this == COMBAT;
    }

    /** Whether this is one of the scripted, non-interactive beats. */
    public boolean scripted() {
        return this == CONFRONTATION || this == QTE || this == QTE_PREPARING
                || this == QTE_READY || this == FINISHER || this == QTE_RECOVERY
                || this == TRANSITION;
    }

    /** Any of the states between the form being spent and the blow landing. */
    public boolean finisherPhase() {
        return this == QTE_PREPARING || this == QTE_READY || this == QTE
                || this == FINISHER || this == QTE_RECOVERY;
    }
}
