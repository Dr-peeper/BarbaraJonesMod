package com.barbarajones.v2.plug;

import net.minecraft.nbt.CompoundTag;

/**
 * What The Plug thinks of one player, saved with the world.
 *
 * <p>Reputation is the only input to how good he is - {@link PlugCompetence}
 * turns it into a level and the level picks the result row - so this is the one
 * number the whole module is really about. It goes up from hiring him, from
 * overpaying him, from finishing jobs, and from gifts; it goes down if you swing
 * on him.
 *
 * <p>Gifts are capped per day. Without the cap a stack of cigarettes is a
 * shortcut past every job you were supposed to hire him for, and the progression
 * stops being about the relationship and starts being about your inventory. The
 * cap has to be persisted alongside the points, which is why the day marker
 * lives on this record rather than in a lookup somewhere.
 */
public class PlugReputation {

    /** Most reputation gifts can be worth in one in-game day. */
    private static final int GIFT_CAP_PER_DAY = 12;

    private int points;
    private int hires;
    private int delivered;
    private int giftPointsToday;
    private long giftDay = -1L;
    private PlugJob selected = PlugJob.GATHER_WOOD;

    public int points() {
        return this.points;
    }

    public int level() {
        return PlugCompetence.levelFor(this.points);
    }

    public int hires() {
        return this.hires;
    }

    public int delivered() {
        return this.delivered;
    }

    public PlugJob selected() {
        return this.selected;
    }

    public void select(PlugJob job) {
        this.selected = job;
    }

    /** Reputation never goes below zero: at the bottom he is already as bad as he gets. */
    public void add(int amount) {
        this.points = Math.max(0, this.points + amount);
    }

    public void noteHire() {
        this.hires++;
    }

    public void noteDelivery() {
        this.delivered++;
    }

    /**
     * Applies a gift and returns the reputation actually awarded, which may be
     * less than the gift is worth once the daily cap is used up.
     *
     * <p>An insult - a negative entry in {@code PlugGifts} - always lands in
     * full. He does not have a daily limit on being offended.
     */
    public int acceptGift(int value, long day) {
        if (value <= 0) {
            add(value);
            return value;
        }
        if (day != this.giftDay) {
            this.giftDay = day;
            this.giftPointsToday = 0;
        }
        int granted = Math.min(value, Math.max(0, GIFT_CAP_PER_DAY - this.giftPointsToday));
        this.giftPointsToday += granted;
        add(granted);
        return granted;
    }

    // ---- persistence --------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", this.points);
        tag.putInt("Hires", this.hires);
        tag.putInt("Delivered", this.delivered);
        tag.putInt("GiftPointsToday", this.giftPointsToday);
        tag.putLong("GiftDay", this.giftDay);
        tag.putString("Selected", this.selected.name());
        return tag;
    }

    public static PlugReputation load(CompoundTag tag) {
        PlugReputation rep = new PlugReputation();
        rep.points = tag.getInt("Points");
        rep.hires = tag.getInt("Hires");
        rep.delivered = tag.getInt("Delivered");
        rep.giftPointsToday = tag.getInt("GiftPointsToday");
        rep.giftDay = tag.contains("GiftDay") ? tag.getLong("GiftDay") : -1L;
        rep.selected = PlugJob.byName(tag.getString("Selected"));
        return rep;
    }
}
