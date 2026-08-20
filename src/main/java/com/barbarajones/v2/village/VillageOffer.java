package com.barbarajones.v2.village;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * One line on a Krave Villager's trade list.
 *
 * <p>Deliberately <em>not</em> vanilla's {@code MerchantOffer}. This module ships
 * its own menu and its own screen, and the extra fields below - the level at which
 * the offer unlocks, and the price drift - are what make the "feed it cereal, get
 * better trades" loop legible. Rolling our own also means the wire format is ours:
 * three ItemStacks, four ints and a float, with no dependence on vanilla's trade
 * NBT shape.
 *
 * <p><b>Restocking.</b> {@link #uses} counts up to {@link #maxUses}; at
 * {@code uses >= maxUses} the offer is out of stock and the result slot stays
 * empty. {@link KraveVillagerEntity} calls {@link #restock()} on a day timer,
 * which pulls uses back down rather than zeroing them, so a heavily-farmed trade
 * takes several days to fully recover while a lightly-used one is back next
 * morning.
 *
 * <p><b>Price drift.</b> {@link #priceMultiplier} is applied to cost A at read
 * time by {@link #currentCostA()}. Village happiness feeds into it, so a
 * miserable town charges more. Never mutate the base cost for that - it has to
 * survive so the discount can be recomputed from scratch every time.
 */
public final class VillageOffer {

    private final ItemStack costA;
    private final ItemStack costB;
    private final ItemStack result;
    private final int maxUses;
    private final int xpReward;
    private final int requiredLevel;

    private int uses;
    private float priceMultiplier;

    public VillageOffer(ItemStack costA, ItemStack costB, ItemStack result,
                        int maxUses, int xpReward, int requiredLevel) {
        this.costA = costA == null ? ItemStack.EMPTY : costA;
        this.costB = costB == null ? ItemStack.EMPTY : costB;
        this.result = result == null ? ItemStack.EMPTY : result;
        this.maxUses = Math.max(1, maxUses);
        this.xpReward = Math.max(0, xpReward);
        this.requiredLevel = Math.max(1, requiredLevel);
        this.priceMultiplier = 1.0F;
    }

    // ---- reads --------------------------------------------------------------

    /** The unmodified first cost. Use this for display of "was" prices. */
    public ItemStack baseCostA() {
        return this.costA;
    }

    /**
     * The first cost as the player must actually pay it right now, after the
     * happiness/tier price multiplier. Always at least one item if the base cost
     * was non-empty - a free trade is a duplication bug waiting to happen.
     */
    public ItemStack currentCostA() {
        if (this.costA.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int count = Math.round(this.costA.getCount() * this.priceMultiplier);
        count = Math.max(1, Math.min(this.costA.getMaxStackSize(), count));
        ItemStack copy = this.costA.copy();
        copy.setCount(count);
        return copy;
    }

    /** The optional second cost. Never discounted - it is usually a token item. */
    public ItemStack costB() {
        return this.costB;
    }

    public ItemStack result() {
        return this.result;
    }

    public int uses() {
        return this.uses;
    }

    public int maxUses() {
        return this.maxUses;
    }

    public int xpReward() {
        return this.xpReward;
    }

    /** The villager trade level at which this offer appears at all. */
    public int requiredLevel() {
        return this.requiredLevel;
    }

    public float priceMultiplier() {
        return this.priceMultiplier;
    }

    public boolean isOutOfStock() {
        return this.uses >= this.maxUses;
    }

    /** True when the player is being charged less than the base price. */
    public boolean isDiscounted() {
        return this.priceMultiplier < 0.999F && !this.costA.isEmpty();
    }

    // ---- writes -------------------------------------------------------------

    public void setPriceMultiplier(float value) {
        this.priceMultiplier = Math.max(0.25F, Math.min(2.0F, value));
    }

    public void use() {
        this.uses = Math.min(this.maxUses, this.uses + 1);
    }

    /**
     * One day's worth of recovery. Partial on purpose - see the class note.
     * Returns true if anything actually came back, so the caller can decide
     * whether the restock chime is worth playing.
     */
    public boolean restock() {
        if (this.uses <= 0) {
            return false;
        }
        int back = Math.max(1, this.maxUses / 2);
        this.uses = Math.max(0, this.uses - back);
        return true;
    }

    /**
     * Whether these two stacks pay for this offer. Order-insensitive: the player
     * may drop the costs into either slot, which is what everyone expects and
     * what vanilla does.
     */
    public boolean satisfiedBy(ItemStack slotA, ItemStack slotB) {
        if (isOutOfStock()) {
            return false;
        }
        ItemStack needA = currentCostA();
        ItemStack needB = this.costB;
        return (covers(slotA, needA) && covers(slotB, needB))
                || (covers(slotB, needA) && covers(slotA, needB));
    }

    /**
     * Consumes the payment from the two stacks. Only call after
     * {@link #satisfiedBy} returned true for the same stacks; it re-checks the
     * ordering itself so a swapped payment is still taken from the right slot.
     */
    public void take(ItemStack slotA, ItemStack slotB) {
        ItemStack needA = currentCostA();
        ItemStack needB = this.costB;
        if (covers(slotA, needA) && covers(slotB, needB)) {
            shrink(slotA, needA);
            shrink(slotB, needB);
        } else {
            shrink(slotB, needA);
            shrink(slotA, needB);
        }
    }

    private static boolean covers(ItemStack given, ItemStack need) {
        if (need.isEmpty()) {
            return true;
        }
        return !given.isEmpty()
                && ItemStack.isSameItemSameTags(given, need)
                && given.getCount() >= need.getCount();
    }

    private static void shrink(ItemStack given, ItemStack need) {
        if (!need.isEmpty() && !given.isEmpty()) {
            given.shrink(need.getCount());
        }
    }

    // ---- serialisation ------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("CostA", this.costA.save(new CompoundTag()));
        tag.put("CostB", this.costB.save(new CompoundTag()));
        tag.put("Result", this.result.save(new CompoundTag()));
        tag.putInt("Uses", this.uses);
        tag.putInt("MaxUses", this.maxUses);
        tag.putInt("Xp", this.xpReward);
        tag.putInt("ReqLevel", this.requiredLevel);
        tag.putFloat("Price", this.priceMultiplier);
        return tag;
    }

    public static VillageOffer load(CompoundTag tag) {
        VillageOffer offer = new VillageOffer(
                ItemStack.of(tag.getCompound("CostA")),
                ItemStack.of(tag.getCompound("CostB")),
                ItemStack.of(tag.getCompound("Result")),
                tag.getInt("MaxUses"),
                tag.getInt("Xp"),
                tag.getInt("ReqLevel"));
        offer.uses = tag.getInt("Uses");
        offer.priceMultiplier = tag.contains("Price") ? tag.getFloat("Price") : 1.0F;
        return offer;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeItem(this.costA);
        buf.writeItem(this.costB);
        buf.writeItem(this.result);
        buf.writeVarInt(this.uses);
        buf.writeVarInt(this.maxUses);
        buf.writeVarInt(this.xpReward);
        buf.writeVarInt(this.requiredLevel);
        buf.writeFloat(this.priceMultiplier);
    }

    public static VillageOffer read(FriendlyByteBuf buf) {
        ItemStack a = buf.readItem();
        ItemStack b = buf.readItem();
        ItemStack r = buf.readItem();
        int uses = buf.readVarInt();
        int maxUses = buf.readVarInt();
        int xp = buf.readVarInt();
        int req = buf.readVarInt();
        float price = buf.readFloat();
        VillageOffer offer = new VillageOffer(a, b, r, maxUses, xp, req);
        offer.uses = uses;
        offer.priceMultiplier = price;
        return offer;
    }
}
