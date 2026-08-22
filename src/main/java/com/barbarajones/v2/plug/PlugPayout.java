package com.barbarajones.v2.plug;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One thing The Plug can come back from a job with, plus the line he says while
 * handing it over.
 *
 * <p>Items are held as {@link Supplier}s rather than {@link Item}s on purpose.
 * The result tables in {@code PlugResults} are {@code static final} arrays, and
 * a mod item read out of a {@code RegistryObject} at class-initialisation time
 * is a hard crash - the registry is not populated yet. A supplier is only
 * dereferenced in {@link #roll}, which cannot run before a player has walked up
 * to him in a running world.
 *
 * <p>An empty {@code drops} array is legal and is not an oversight: that is how
 * the rip-off is expressed. He kept the money, he came back, and he has a
 * perfectly good explanation.
 */
public record PlugPayout(String line, Drop[] drops) {

    /** An item and how many of it, inclusive on both ends. */
    public record Drop(Supplier<Item> item, int min, int max) { }

    /** True when he came back with literally nothing. Used to pick the sound and the rep award. */
    public boolean isRipOff() {
        return this.drops.length == 0;
    }

    /**
     * Rolls the actual stacks for one delivery.
     *
     * <p>Called exactly once per job, at the moment the job finishes, and the
     * result is then stored on the contract. Rolling again when the player
     * finally walks over to collect would let anyone re-roll a bad haul by
     * closing the world, which is the one form of unreliability he does not get
     * to have.
     */
    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        for (Drop drop : this.drops) {
            int spread = drop.max() - drop.min();
            int count = drop.min() + (spread > 0 ? random.nextInt(spread + 1) : 0);
            if (count <= 0) {
                continue;
            }
            out.add(new ItemStack(drop.item().get(), count));
        }
        return out;
    }
}
