package com.barbarajones.v2.machines;

/**
 * The one inventory layout every Krave machine uses.
 *
 * <p>All seven kinds keep a five-slot {@code ItemStackHandler} even when they do
 * not use every slot. That is a deliberate trade: slot indices are then constants
 * rather than something computed per kind, NBT written by one machine is readable
 * by another (a Grinder placed where a Toaster was does not eat its contents),
 * and the capability wiring below has exactly one shape to reason about.
 *
 * <p>What varies per kind is which slots are *live* - see
 * {@link MachineKind#inputCount()}, {@link MachineKind#hasFuel} and
 * {@link MachineKind#hasOutput}. Dead slots reject insertion and are never given a
 * {@code Slot} in the menu, so they cannot be reached by a player or a hopper.
 */
public final class MachineSlots {

    public static final int INPUT_0 = 0;
    public static final int INPUT_1 = 1;
    public static final int INPUT_2 = 2;
    public static final int FUEL = 3;
    public static final int OUTPUT = 4;

    public static final int SIZE = 5;

    /** Slot indices of the three input slots, in GUI order. */
    public static final int[] INPUTS = { INPUT_0, INPUT_1, INPUT_2 };

    private MachineSlots() { }
}
