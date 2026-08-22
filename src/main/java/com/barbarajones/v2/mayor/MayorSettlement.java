package com.barbarajones.v2.mayor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything the mayor's office knows about one settlement: who is running it,
 * how much clout she has, what is in the works, where she has already built, how
 * far the roads reach, and how much of the take is waiting to be collected.
 *
 * <p>Owned by {@link MayorData}, which is the {@code SavedData} that persists
 * it. <b>Every mutation here has to be followed by a {@code setDirty()} on that
 * owner</b> - the pipeline in {@code KraveMayor} does it once at the end of each
 * mayor tick rather than scattering the call, but a new caller that reaches in
 * and changes something must do it too. Forgetting is the classic SavedData bug:
 * everything works right up until the world is closed.
 *
 * <h2>One mayor at a time</h2>
 * Barbara is not the authority on which village she runs - this object is. She
 * asks to {@link #claim} the job every mayor tick and gets refused if somebody
 * else holds it, which is what stops two Barbaras near one settlement from
 * running two copies of the pipeline and building everything twice. The claim
 * lapses after {@link #VACANCY_TICKS} without a heartbeat, so a Barbara who dies
 * or is carried off does not leave the office permanently locked.
 */
public final class MayorSettlement {

    /** Commissions that may be outstanding at once, funded or not. */
    public static final int MAX_QUEUE = 6;

    /**
     * How many build sites are remembered for spacing purposes. Past this the
     * oldest are forgotten, which lets the town infill over its own earliest
     * corners - which is the correct behaviour for a place this crowded, and
     * also stops an unbounded list going into the save file.
     */
    public static final int MAX_SITES = 160;

    /** A minute of no heartbeat and the job is open to the next Barbara. */
    public static final int VACANCY_TICKS = 1200;

    /** Dollars the office will hold before it stops counting. Go and collect. */
    public static final int PAYOUT_CAP = 200;

    private final UUID villageId;

    @Nullable
    private UUID mayorId;
    private long mayorSeenTick;

    private int clout;
    private int siteCursor;
    private int payout;
    private long lastCompletionTick;
    private boolean starterIssued;

    private final List<MayorProject> queue = new ArrayList<>();
    private final Map<String, Integer> completed = new HashMap<>();
    private final List<BlockPos> sites = new ArrayList<>();
    /** Segments laid on each of the four cardinal spurs out of the origin. */
    private final int[] roadSpurs = new int[4];

    public MayorSettlement(UUID villageId) {
        this.villageId = villageId;
    }

    public UUID villageId() {
        return this.villageId;
    }

    // ---- the job ------------------------------------------------------------

    /**
     * Asks for the mayor's job on behalf of one Barbara and refreshes her
     * heartbeat if she already has it.
     *
     * @return true if this Barbara is the mayor as of now
     */
    public boolean claim(UUID barbara, long gameTime) {
        boolean vacant = this.mayorId == null || gameTime - this.mayorSeenTick > VACANCY_TICKS;
        if (!vacant && !this.mayorId.equals(barbara)) {
            return false;
        }
        this.mayorId = barbara;
        this.mayorSeenTick = gameTime;
        return true;
    }

    // ---- rank ---------------------------------------------------------------

    public MayorRank rank() {
        return MayorRank.forClout(this.clout);
    }

    /** Clout still owing before the next rung. Zero at the top. */
    public int cloutToNextRank() {
        MayorRank next = rank().next();
        return next == null ? 0 : Math.max(0, next.cloutRequired() - this.clout);
    }

    // ---- the queue ----------------------------------------------------------

    public List<MayorProject> queue() {
        return this.queue;
    }

    public boolean queueFull() {
        return this.queue.size() >= MAX_QUEUE;
    }

    public boolean enqueue(MayorProject project) {
        if (queueFull()) {
            return false;
        }
        return this.queue.add(project);
    }

    /** The project the report talks about: the first one that is not finished. */
    @Nullable
    public MayorProject head() {
        for (MayorProject project : this.queue) {
            if (project.state() != MayorProject.State.DONE) {
                return project;
            }
        }
        return null;
    }

    /** How many commissions are physically going up right now. */
    public int building() {
        int count = 0;
        for (MayorProject project : this.queue) {
            if (project.state() == MayorProject.State.BUILDING) {
                count++;
            }
        }
        return count;
    }

    // ---- what has been finished ---------------------------------------------

    public int completedCount(ProjectKind kind) {
        return this.completed.getOrDefault(kind.key(), 0);
    }

    public int completedTotal() {
        int total = 0;
        for (int count : this.completed.values()) {
            total += count;
        }
        return total;
    }

    /**
     * Books a finished project: the clout, the tally, and the cooling-off clock
     * that keeps the town from going up all at once.
     */
    public void noteCompleted(ProjectKind kind, long gameTime) {
        this.completed.merge(kind.key(), 1, Integer::sum);
        this.clout += kind.clout();
        this.lastCompletionTick = gameTime;
    }

    public long lastCompletionTick() {
        return this.lastCompletionTick;
    }

    // ---- where things are ---------------------------------------------------

    public List<BlockPos> sites() {
        return this.sites;
    }

    /** Remembers a footprint centre, dropping the oldest once the list is full. */
    public void noteSite(BlockPos centre) {
        this.sites.add(centre.immutable());
        while (this.sites.size() > MAX_SITES) {
            this.sites.remove(0);
        }
    }

    /**
     * Advances and returns the site-spiral cursor.
     *
     * <p>Persisted, which is the whole point: successive projects carry on round
     * the town from where the last one stopped instead of re-rolling the same
     * handful of plots and rejecting them again. The wrap guard costs one branch
     * and covers the sixty-eighth year of continuous play.
     */
    public int nextSiteCursor() {
        this.siteCursor++;
        if (this.siteCursor < 0) {
            this.siteCursor = 0;
        }
        return this.siteCursor;
    }

    // ---- roads --------------------------------------------------------------

    /** Segments already laid on one of the four cardinal spurs. */
    public int roadSpur(int direction) {
        return this.roadSpurs[Math.floorMod(direction, this.roadSpurs.length)];
    }

    public void extendRoadSpur(int direction) {
        int index = Math.floorMod(direction, this.roadSpurs.length);
        this.roadSpurs[index]++;
    }

    public int roadSpurCount() {
        return this.roadSpurs.length;
    }

    public int roadSegmentsLaid() {
        int total = 0;
        for (int spur : this.roadSpurs) {
            total += spur;
        }
        return total;
    }

    // ---- the take -----------------------------------------------------------

    /**
     * One tick's worth of Plug jobs and shop takings.
     *
     * <p>Paid on the <em>count of finished buildings</em> rather than on a flag,
     * so knocking the headquarters down stops the money - the income is a
     * property of what is standing, not of what was once built.
     *
     * @return true if the books actually moved, so the caller only marks the
     *         save dirty when there was something to save. A village with no
     *         shops in it must not be writing a file every ten seconds.
     */
    public boolean accruePayout() {
        int rate = completedCount(ProjectKind.PLUG_HEADQUARTERS) * 3
                + completedCount(ProjectKind.CORNER_STORE)
                + completedCount(ProjectKind.MARKET_STALL);
        if (rate <= 0 || this.payout >= PAYOUT_CAP) {
            return false;
        }
        this.payout = Math.min(PAYOUT_CAP, this.payout + rate);
        return true;
    }

    /** Hands over everything owing and zeroes the book. */
    public int collectPayout() {
        int owed = this.payout;
        this.payout = 0;
        return owed;
    }

    // ---- getting started ----------------------------------------------------

    /**
     * The first Krave Shack Kit, once per settlement.
     *
     * <p>Without this the permits are a creative-tab-only item: there is no
     * recipe for one, and recipes are data files this module does not own. A
     * player who has just founded a village and gone to talk to Barbara has to
     * be able to leave that conversation with something to hand back to her,
     * or the whole module is unreachable in survival.
     *
     * <p>The flag is persisted rather than derived from "has she built nothing
     * yet", because the derived version pays out again every time the player
     * throws the kit away and asks again.
     *
     * @return true exactly once, on the call that should hand the kit over
     */
    public boolean issueStarterPermit() {
        if (this.starterIssued) {
            return false;
        }
        this.starterIssued = true;
        return true;
    }

    // ---- persistence --------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Village", this.villageId);
        if (this.mayorId != null) {
            tag.putUUID("Mayor", this.mayorId);
        }
        tag.putLong("MayorSeen", this.mayorSeenTick);
        tag.putInt("Clout", this.clout);
        tag.putInt("SiteCursor", this.siteCursor);
        tag.putInt("Payout", this.payout);
        tag.putLong("LastCompletion", this.lastCompletionTick);
        tag.putBoolean("Starter", this.starterIssued);
        tag.putIntArray("RoadSpurs", this.roadSpurs.clone());

        ListTag projects = new ListTag();
        for (MayorProject project : this.queue) {
            if (project.state() != MayorProject.State.DONE) {
                projects.add(project.save());
            }
        }
        tag.put("Queue", projects);

        CompoundTag done = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.completed.entrySet()) {
            done.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Completed", done);

        long[] packed = new long[this.sites.size()];
        for (int i = 0; i < packed.length; i++) {
            packed[i] = this.sites.get(i).asLong();
        }
        tag.putLongArray("Sites", packed);
        return tag;
    }

    /** @return the settlement, or null if the entry has no village id to hang on. */
    @Nullable
    public static MayorSettlement load(CompoundTag tag) {
        if (!tag.hasUUID("Village")) {
            return null;
        }
        MayorSettlement s = new MayorSettlement(tag.getUUID("Village"));
        if (tag.hasUUID("Mayor")) {
            s.mayorId = tag.getUUID("Mayor");
        }
        s.mayorSeenTick = tag.getLong("MayorSeen");
        s.clout = tag.getInt("Clout");
        s.siteCursor = tag.getInt("SiteCursor");
        s.payout = Math.min(PAYOUT_CAP, tag.getInt("Payout"));
        s.lastCompletionTick = tag.getLong("LastCompletion");
        s.starterIssued = tag.getBoolean("Starter");

        int[] spurs = tag.getIntArray("RoadSpurs");
        for (int i = 0; i < s.roadSpurs.length && i < spurs.length; i++) {
            s.roadSpurs[i] = Math.max(0, spurs[i]);
        }

        ListTag projects = tag.getList("Queue", Tag.TAG_COMPOUND);
        for (int i = 0; i < projects.size() && s.queue.size() < MAX_QUEUE; i++) {
            MayorProject project = MayorProject.load(projects.getCompound(i));
            if (project != null) {
                s.queue.add(project);
            }
        }

        CompoundTag done = tag.getCompound("Completed");
        for (String key : done.getAllKeys()) {
            s.completed.put(key, done.getInt(key));
        }

        for (long value : tag.getLongArray("Sites")) {
            s.sites.add(BlockPos.of(value));
        }
        while (s.sites.size() > MAX_SITES) {
            s.sites.remove(0);
        }
        return s;
    }
}
