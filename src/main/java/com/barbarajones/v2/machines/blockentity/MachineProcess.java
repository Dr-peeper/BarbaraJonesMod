package com.barbarajones.v2.machines.blockentity;

/**
 * What a machine actually does with its inputs.
 *
 * <p>Everything shared between the seven kinds - the five-slot inventory, the
 * syrup burner, the progress counter, capability exposure, NBT, the GUI, the
 * auto-eject - lives in {@link MachineBlockEntity}. What differs is exactly two
 * questions: "can you run right now, and for how long" and "you finished, do the
 * thing". This interface is those two questions.
 *
 * <p>Three implementations cover all seven machines: a recipe-driven one (five of
 * them, differing only in which {@code RecipeType} they read), the plantation,
 * and the depot.
 */
public interface MachineProcess {

    /** Returned by {@link #workTime} when the machine has nothing to do. */
    int IDLE = -1;

    /**
     * How many ticks of work this machine needs to finish what is in front of it.
     *
     * <p>Called every tick, including while already running, so it doubles as the
     * "is this still valid" check - if the inputs are pulled out mid-craft this
     * returns {@link #IDLE} and progress resets. Implementations must also verify
     * there is room for the result, so a finished craft is never destroyed for
     * want of an output slot.
     *
     * @return tick count, or {@link #IDLE} if the machine cannot run
     */
    int workTime(MachineBlockEntity machine);

    /**
     * Progress has reached {@link #workTime}. Consume the inputs and produce the
     * output. Called on the server only, and only when {@code workTime} last
     * returned a value other than {@link #IDLE}.
     */
    void complete(MachineBlockEntity machine);

    /** Syrup units burned per tick of progress. Zero means this machine runs cold. */
    default int fuelPerTick(MachineBlockEntity machine) {
        return 1;
    }
}
