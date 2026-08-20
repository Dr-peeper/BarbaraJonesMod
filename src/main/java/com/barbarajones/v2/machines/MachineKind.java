package com.barbarajones.v2.machines;

/**
 * The seven machine variants that share one block class, one block-entity type,
 * one menu type and one screen.
 *
 * <p>This is deliberately data rather than a class hierarchy. Every Krave machine
 * has the same shape - up to three inputs, a syrup burner, one output, a progress
 * bar - and the only things that actually differ are how many input slots are
 * live, where they sit in the GUI, and which {@code MachineProcess} does the work.
 * Encoding that as an enum means a new machine is one enum constant plus one
 * recipe type, not another five files.
 *
 * <p>Slot geometry is vanilla-furnace-derived on purpose: fuel under the inputs at
 * (56,53), flame above it, arrow at (79,34), output at (116,35). Players already
 * know how to read that layout, so a Krave Mixer is legible on first open.
 */
public enum MachineKind {

    /** Farms Krave Pods / cocoa in a radius. Input slot takes cocoa beans to replant. */
    PLANTATION("cocoa_plantation", new int[] { 56 }, 17, true, true),

    /** Cocoa -> Krave Dust. The automated cousin of the economy module's hand mortar. */
    GRINDER("krave_grinder", new int[] { 56 }, 17, true, true),

    /** Krave Dust + milk + sugar -> Krave Batter. The only three-input machine. */
    MIXER("krave_mixer", new int[] { 38, 56, 74 }, 17, true, true),

    /** Krave Batter -> raw Krave pieces. */
    EXTRUDER("krave_extruder", new int[] { 56 }, 17, true, true),

    /** Raw pieces -> finished Krave. */
    TOASTER("krave_toaster", new int[] { 56 }, 17, true, true),

    /** Krave + carton -> a Case of Krave. Two inputs. */
    BOXER("krave_boxer", new int[] { 47, 65 }, 17, true, true),

    /**
     * Ships cases to the village. No fuel, no output slot - what goes in leaves
     * the world and turns into village production. See
     * {@code com.barbarajones.v2.machines.village.VillageLink}.
     */
    DEPOT("krave_depot", new int[] { 56 }, 35, false, false);

    /** Registry path of the block, and the suffix of every translation key for it. */
    public final String id;
    /** X coordinate of each live input slot in the 176x166 GUI. Length == input count. */
    public final int[] inputX;
    /** Y coordinate shared by every input slot. */
    public final int inputY;
    /** Whether slot {@link MachineSlots#FUEL} is live and the flame gauge is drawn. */
    public final boolean hasFuel;
    /** Whether slot {@link MachineSlots#OUTPUT} is live. */
    public final boolean hasOutput;

    MachineKind(String id, int[] inputX, int inputY, boolean hasFuel, boolean hasOutput) {
        this.id = id;
        this.inputX = inputX;
        this.inputY = inputY;
        this.hasFuel = hasFuel;
        this.hasOutput = hasOutput;
    }

    public int inputCount() {
        return inputX.length;
    }

    /** {@code container.barbarajones.krave_grinder} - GUI title. */
    public String menuTranslationKey() {
        return "container.barbarajones." + id;
    }

    /** The 176x166 background this kind draws. */
    public String guiTexture() {
        return "textures/gui/container/" + id + ".png";
    }

    /** Safe lookup for the network path, where a bad byte must not throw. */
    public static MachineKind byOrdinal(int ordinal) {
        MachineKind[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : GRINDER;
    }
}
