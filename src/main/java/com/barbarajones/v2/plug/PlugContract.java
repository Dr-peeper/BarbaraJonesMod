package com.barbarajones.v2.plug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One job The Plug is out on, or has come back from and not been paid out yet.
 *
 * <p>A contract is a timestamp and a state, nothing more, which is what lets a
 * job survive a logout, a chunk unload and a server restart without anything
 * ticking. {@code returnTime} is an absolute game time on the overworld clock -
 * the same clock everywhere, since every dimension derives its game time from
 * the primary level data - so "is he back yet" is one comparison against a
 * number read off disk, not a countdown that has to be kept alive in memory.
 *
 * <p>The haul is rolled once, in {@link #finish}, at the moment the job comes
 * due, and then stored here. It is deliberately not rolled when the player walks
 * over to collect: that would make a bad haul re-rollable by quitting to the
 * title screen, and the whole point of the low end is that you have to live with
 * what he brought you.
 */
public class PlugContract {

    private final UUID employer;

    /**
     * Which Plug took the job, so the right body vanishes. Nullable because a
     * contract outlives the man - if he gets shot while he is out, the goods are
     * still owed, and any other Plug will hand them over rather than the payout
     * disappearing with him.
     */
    @Nullable
    private final UUID plug;

    private final PlugJob job;

    /**
     * Competence frozen at hiring time. Gifting him half a village while he is
     * out must not retroactively improve the job he is already on - two systems
     * writing the same number is exactly how a payout ends up disagreeing with
     * the line he says handing it over.
     */
    private final int competence;

    private final int paid;
    private final long returnTime;

    private boolean ready;
    private boolean ripOff;
    private String line = "";
    private String bonusLine = "";
    private final List<ItemStack> haul = new ArrayList<>();

    public PlugContract(UUID employer, @Nullable UUID plug, PlugJob job,
                        int competence, int paid, long returnTime) {
        this.employer = employer;
        this.plug = plug;
        this.job = job;
        this.competence = competence;
        this.paid = paid;
        this.returnTime = returnTime;
    }

    public UUID employer() {
        return this.employer;
    }

    @Nullable
    public UUID plug() {
        return this.plug;
    }

    public PlugJob job() {
        return this.job;
    }

    public int paid() {
        return this.paid;
    }

    /** True while he is still out. Drives whether his body is on the map at all. */
    public boolean isAway() {
        return !this.ready;
    }

    public boolean isReady() {
        return this.ready;
    }

    public boolean isRipOff() {
        return this.ripOff;
    }

    public String line() {
        return this.line;
    }

    public String bonusLine() {
        return this.bonusLine;
    }

    public List<ItemStack> haul() {
        return this.haul;
    }

    public boolean isDue(long now) {
        return now >= this.returnTime;
    }

    /** Seconds left before he is due back, floored at zero. For the status line. */
    public int secondsLeft(long now) {
        return (int) Math.max(0L, (this.returnTime - now) / 20L);
    }

    /**
     * Rolls what he came back with and flips the contract to ready.
     *
     * <p>Safe to call more than once - the second call is a no-op - because both
     * the slow timer and a player walking up to him can be the first thing to
     * notice a job came due, and they must not each roll their own haul.
     */
    public void finish(RandomSource random) {
        if (this.ready) {
            return;
        }
        PlugPayout payout = this.job.payout(PlugCompetence.rollTier(this.competence, random), random);
        this.line = payout.line();
        this.ripOff = payout.isRipOff();
        this.haul.addAll(payout.roll(random));

        if (PlugCompetence.rollBonus(this.competence, random)) {
            PlugPayout bonus = PlugResults.BONUSES[random.nextInt(PlugResults.BONUSES.length)];
            this.bonusLine = bonus.line();
            this.haul.addAll(bonus.roll(random));
        }
        this.ready = true;
    }

    // ---- persistence --------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Employer", this.employer);
        if (this.plug != null) {
            tag.putUUID("Plug", this.plug);
        }
        tag.putString("Job", this.job.name());
        tag.putInt("Competence", this.competence);
        tag.putInt("Paid", this.paid);
        tag.putLong("ReturnTime", this.returnTime);
        tag.putBoolean("Ready", this.ready);
        tag.putBoolean("RipOff", this.ripOff);
        tag.putString("Line", this.line);
        tag.putString("BonusLine", this.bonusLine);

        ListTag stacks = new ListTag();
        for (ItemStack stack : this.haul) {
            stacks.add(stack.save(new CompoundTag()));
        }
        tag.put("Haul", stacks);
        return tag;
    }

    public static PlugContract load(CompoundTag tag) {
        PlugContract contract = new PlugContract(
                tag.getUUID("Employer"),
                tag.hasUUID("Plug") ? tag.getUUID("Plug") : null,
                PlugJob.byName(tag.getString("Job")),
                tag.getInt("Competence"),
                tag.getInt("Paid"),
                tag.getLong("ReturnTime"));
        contract.ready = tag.getBoolean("Ready");
        contract.ripOff = tag.getBoolean("RipOff");
        contract.line = tag.getString("Line");
        contract.bonusLine = tag.getString("BonusLine");

        ListTag stacks = tag.getList("Haul", Tag.TAG_COMPOUND);
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = ItemStack.of(stacks.getCompound(i));
            if (!stack.isEmpty()) {
                contract.haul.add(stack);
            }
        }
        return contract;
    }
}
