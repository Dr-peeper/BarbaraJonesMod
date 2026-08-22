package com.barbarajones.v2.mayor;

import com.barbarajones.v2.build.place.BuildJob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One commission, from the moment the permit is handed over to the moment the
 * last block lands.
 *
 * <h2>The four states</h2>
 * <pre>
 *   FUNDING  --materials all delivered-->  SITING
 *   SITING   --a site passed every check-->  BUILDING
 *   BUILDING --the build job reports done--> DONE  (and the project leaves the queue)
 * </pre>
 *
 * <p>A project can sit in FUNDING or SITING indefinitely without anything going
 * wrong; {@link #stall()} records <em>why</em> it is not moving so the report can
 * say so in a sentence instead of leaving the player staring at a queue that
 * does nothing.
 *
 * <h2>Surviving a reload mid-build</h2>
 * The {@link BuildJob} is deliberately NOT persisted - it cannot be, it is a
 * live write cursor over a level. What makes that safe is a guarantee the
 * placement engine already gives: {@code BuildScheduler} force-finishes every
 * outstanding job when a level unloads or the server stops, so a project that
 * was BUILDING when the world closed is a finished building when it opens again.
 * A BUILDING project that comes back with no job is therefore complete, and
 * {@link #buildFinished()} says so. Treating it as unfinished instead would
 * re-site and re-place a building that is already standing there.
 *
 * <h2>Why deliveries are keyed by item id</h2>
 * The obvious storage is an int per line of the bill, in list order. That breaks
 * the first time somebody reorders a material list, and it breaks silently -
 * every part-funded project in every save suddenly has its iron counted as
 * cobblestone. Keying on the registry id costs a few bytes and cannot do that.
 */
public final class MayorProject {

    /** Where a project is in its life. */
    public enum State {
        /** Waiting on the player to hand over the bill of materials. */
        FUNDING,
        /** Paid for. Waiting on the mayor to find somewhere to put it. */
        SITING,
        /** Going up right now. */
        BUILDING,
        /** Finished. Removed from the queue on the same tick this is set. */
        DONE
    }

    /** Why a project that ought to be moving is not. Purely for the report. */
    public enum Stall {
        NONE("moving"),
        MATERIALS("waiting on materials"),
        NO_SITE("nowhere left to put it"),
        HOUSING_FULL("nobody left to move in"),
        BUSY("she's already on another job"),
        COOLING_OFF("she's having a minute"),
        RANK("she isn't cleared for that yet");

        private final String phrase;

        Stall(String phrase) {
            this.phrase = phrase;
        }

        /** Lower-case fragment, dropped into a sentence in the report. */
        public String phrase() {
            return this.phrase;
        }
    }

    private final ProjectKind kind;
    private final Map<String, Integer> delivered = new HashMap<>();

    private State state = State.FUNDING;
    private Stall stall = Stall.MATERIALS;

    @Nullable
    private BlockPos site;
    private final List<BlockPos> staffSpots = new ArrayList<>();

    /** Live only. See the class javadoc on why this is not saved. */
    @Nullable
    private transient BuildJob job;

    public MayorProject(ProjectKind kind) {
        this.kind = kind;
    }

    public ProjectKind kind() {
        return this.kind;
    }

    public State state() {
        return this.state;
    }

    public Stall stall() {
        return this.stall;
    }

    public void setStall(Stall reason) {
        this.stall = reason;
    }

    /** Where it is going up, once it has been sited. Null before that. */
    @Nullable
    public BlockPos site() {
        return this.site;
    }

    public List<BlockPos> staffSpots() {
        return this.staffSpots;
    }

    // ---- materials ----------------------------------------------------------

    /** How many of this line are still outstanding. */
    public int outstanding(ProjectKind.Material material) {
        return Math.max(0, material.count() - this.delivered.getOrDefault(material.id(), 0));
    }

    public boolean isFunded() {
        for (ProjectKind.Material material : this.kind.materials()) {
            if (outstanding(material) > 0) {
                return false;
            }
        }
        return true;
    }

    /** The lines that still have something owing, in the order the bill lists them. */
    public List<ProjectKind.Material> outstandingLines() {
        List<ProjectKind.Material> out = new ArrayList<>();
        for (ProjectKind.Material material : this.kind.materials()) {
            if (outstanding(material) > 0) {
                out.add(material);
            }
        }
        return out;
    }

    /**
     * Takes what this project can use out of a stack, shrinking it by exactly
     * that much.
     *
     * @return how many items were actually accepted; zero means the stack was
     *         nothing this project wanted, and the caller should say so rather
     *         than silently eating it
     */
    public int deliver(ItemStack stack) {
        if (stack.isEmpty() || this.state != State.FUNDING) {
            return 0;
        }
        for (ProjectKind.Material material : this.kind.materials()) {
            if (!material.matches(stack)) {
                continue;
            }
            int want = outstanding(material);
            if (want <= 0) {
                continue;
            }
            int taken = Math.min(want, stack.getCount());
            this.delivered.merge(material.id(), taken, Integer::sum);
            stack.shrink(taken);
            if (isFunded()) {
                this.state = State.SITING;
                this.stall = Stall.NONE;
            }
            return taken;
        }
        return 0;
    }

    // ---- state transitions --------------------------------------------------

    /**
     * Records that the build has been handed to the placement engine. Called
     * only from the mayor's pipeline, immediately after a successful
     * {@code KraveStructure.place} on the same tick the site was chosen, so
     * there is no window in which the site could go stale.
     */
    public void markBuilding(BlockPos where, List<BlockPos> staff, @Nullable BuildJob buildJob) {
        this.site = where.immutable();
        this.staffSpots.clear();
        this.staffSpots.addAll(staff);
        this.job = buildJob;
        this.state = State.BUILDING;
        this.stall = Stall.NONE;
    }

    /**
     * True once the placement engine has finished with this project.
     *
     * <p>A null job means the world was reloaded since it was submitted, which -
     * per the class javadoc - means the build was force-finished before the save
     * and the building is already standing.
     */
    public boolean buildFinished() {
        return this.state == State.BUILDING && (this.job == null || this.job.isComplete());
    }

    public void markDone() {
        this.state = State.DONE;
        this.stall = Stall.NONE;
        this.job = null;
    }

    // ---- persistence --------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", this.kind.key());
        tag.putString("State", this.state.name());
        tag.putString("Stall", this.stall.name());

        CompoundTag paid = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.delivered.entrySet()) {
            paid.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Delivered", paid);

        if (this.site != null) {
            tag.putLong("Site", this.site.asLong());
        }
        long[] spots = new long[this.staffSpots.size()];
        for (int i = 0; i < spots.length; i++) {
            spots[i] = this.staffSpots.get(i).asLong();
        }
        tag.putLongArray("Staff", spots);
        return tag;
    }

    /** @return the project, or null if the save names a project kind that no longer exists. */
    @Nullable
    public static MayorProject load(CompoundTag tag) {
        ProjectKind kind = ProjectKind.byKey(tag.getString("Kind"));
        if (kind == null) {
            return null;
        }
        MayorProject project = new MayorProject(kind);
        project.state = readState(tag.getString("State"));
        project.stall = readStall(tag.getString("Stall"));

        CompoundTag paid = tag.getCompound("Delivered");
        for (String key : paid.getAllKeys()) {
            project.delivered.put(key, paid.getInt(key));
        }

        if (tag.contains("Site")) {
            project.site = BlockPos.of(tag.getLong("Site"));
        }
        for (long packed : tag.getLongArray("Staff")) {
            project.staffSpots.add(BlockPos.of(packed));
        }
        return project;
    }

    private static State readState(String name) {
        for (State value : State.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return State.FUNDING;
    }

    private static Stall readStall(String name) {
        for (Stall value : Stall.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return Stall.NONE;
    }
}
