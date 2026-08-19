package com.barbarajones.boss.manager;

/**
 * The on-the-spot tasks The Manager barks during his PERFORMANCE REVIEW phase.
 * Each one clears a single "Written Up" stack if the player completes it before
 * the deadline, and adds one if they don't.
 *
 * <p>Every task is checkable from server-visible player state alone - crouching,
 * held items, look direction, distance, ground contact - so none of them need a
 * packet, a key binding or anything client-side. That constraint is why the list
 * is what it is: no "press F", no "open your inventory".
 */
public enum ReviewTask {

    SIT_DOWN("Sit down. SNEAK, and hold it. Three seconds.", 180, 60),
    EYE_CONTACT("Look me in the eye. Three seconds. Don't you dare blink.", 180, 60),
    HANDS_EMPTY("Hands where I can see them. BOTH of them. Empty. Three seconds.", 180, 60),
    STEP_INTO_MY_OFFICE("Step into my office. Four blocks. Three seconds. Move.", 200, 60),
    TAKE_A_LAP("Take a lap. Twenty blocks. I am timing you.", 200, 0),
    JUMP_TO_IT("Jump to it. And I mean that literally. Three jumps.", 160, 0);

    private final String order;
    private final int deadlineTicks;

    /** Ticks of continuous compliance required; 0 for the counting tasks. */
    private final int holdTicks;

    ReviewTask(String order, int deadlineTicks, int holdTicks) {
        this.order = order;
        this.deadlineTicks = deadlineTicks;
        this.holdTicks = holdTicks;
    }

    public String order() {
        return this.order;
    }

    public int deadlineTicks() {
        return this.deadlineTicks;
    }

    public int holdTicks() {
        return this.holdTicks;
    }
}
