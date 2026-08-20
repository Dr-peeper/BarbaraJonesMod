package com.barbarajones.v2.build.def;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Maps single characters to block states.
 *
 * <p>Every entry is stored as a {@link Supplier}, never as a resolved
 * {@link BlockState}. That is deliberate and load-bearing: structure
 * definitions are written as static fields in the buildings module and are
 * therefore constructed during mod construction, long before this mod's own
 * blocks exist in the registry. Calling {@code MyBlocks.FOO.get()} at that
 * point throws. Wrapping it in a lambda defers the lookup to the moment the
 * building is actually placed, by which time everything is registered.
 *
 * <p>Two characters are reserved and cannot be redefined:
 * <ul>
 *   <li>{@code '.'} ({@link #KEEP}) - leave whatever the world already has here.
 *       Used to punch holes in a previously drawn box.</li>
 *   <li>{@code ' '} ({@link #AIR}) - carve this position out to air.</li>
 * </ul>
 *
 * <p>Entries rotate with the building by default: the resolved state gets
 * {@link BlockState#rotate(Rotation)} applied, which is what makes stairs,
 * doors, logs and anything with a facing property come out the right way round
 * in all four orientations. Register with
 * {@link Builder#fixed(char, Supplier)} to opt out.
 */
public final class Palette {

    /** Reserved: leave the existing world block untouched. */
    public static final char KEEP = '.';
    /** Reserved: carve this position out to air. */
    public static final char AIR = ' ';

    /** One weighted choice inside a palette entry. */
    public static final class Weighted {
        final int weight;
        final Supplier<BlockState> state;

        private Weighted(int weight, Supplier<BlockState> state) {
            this.weight = Math.max(1, weight);
            this.state = state;
        }
    }

    /** Builds a weighted choice for {@link Builder#weighted(char, List)}. */
    public static Weighted weight(int weight, Supplier<BlockState> state) {
        return new Weighted(weight, state);
    }

    private static final class Entry {
        final List<Weighted> choices;
        final int totalWeight;
        final boolean rotates;

        Entry(List<Weighted> choices, boolean rotates) {
            this.choices = choices;
            this.rotates = rotates;
            int t = 0;
            for (Weighted w : choices) {
                t += w.weight;
            }
            this.totalWeight = Math.max(1, t);
        }

        BlockState pick(RandomSource rng) {
            if (choices.isEmpty()) {
                return null;
            }
            if (choices.size() == 1) {
                return choices.get(0).state.get();
            }
            int roll = rng.nextInt(totalWeight);
            for (Weighted w : choices) {
                roll -= w.weight;
                if (roll < 0) {
                    return w.state.get();
                }
            }
            return choices.get(choices.size() - 1).state.get();
        }
    }

    private final Map<Character, Entry> entries;

    private Palette(Map<Character, Entry> entries) {
        this.entries = entries;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** True if this palette (or the reserved set) knows the character. */
    public boolean has(char key) {
        return key == AIR || key == KEEP || entries.containsKey(key);
    }

    /**
     * Resolves a palette character to the state that should actually be placed,
     * with the building's rotation already applied.
     *
     * @return the state, or {@code null} if the character is unmapped (the
     *         placement engine logs that once and skips the block rather than
     *         crashing mid-build).
     */
    public BlockState resolve(char key, RandomSource rng, Rotation rotation) {
        if (key == AIR) {
            return Blocks.AIR.defaultBlockState();
        }
        Entry e = entries.get(key);
        if (e == null) {
            return null;
        }
        BlockState state = e.pick(rng);
        if (state == null) {
            return null;
        }
        return e.rotates ? state.rotate(rotation) : state;
    }

    /** Copies this palette's entries into a builder so a building can extend a shared palette. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.entries.putAll(this.entries);
        for (Character c : this.entries.keySet()) {
            if (c >= Builder.AUTO_START && c >= b.nextAuto) {
                b.nextAuto = (char) (c + 1);
            }
        }
        return b;
    }

    // ------------------------------------------------------------------

    public static final class Builder {

        private final Map<Character, Entry> entries = new HashMap<>();
        /** Auto-allocated keys live in the Unicode private use area so they can never collide with an author's characters. */
        private static final char AUTO_START = (char) 0xE000;
        private char nextAuto = AUTO_START;

        private Builder() { }

        private Builder define(char key, List<Weighted> choices, boolean rotates) {
            if (key == KEEP || key == AIR) {
                throw new IllegalArgumentException(
                        "Palette character '" + key + "' is reserved ('.' = keep world block, ' ' = air)");
            }
            entries.put(key, new Entry(choices, rotates));
            return this;
        }

        /** Maps a character to a block's default state. Safe for vanilla blocks referenced directly. */
        public Builder block(char key, Block block) {
            return define(key, List.of(weight(1, block::defaultBlockState)), true);
        }

        /** Maps a character to a block resolved lazily - use this for the mod's own blocks. */
        public Builder block(char key, Supplier<Block> block) {
            return define(key, List.of(weight(1, () -> block.get().defaultBlockState())), true);
        }

        /** Maps a character to a fully specified state resolved lazily. */
        public Builder state(char key, Supplier<BlockState> state) {
            return define(key, List.of(weight(1, state)), true);
        }

        /** Maps a character to a state that is already available (vanilla only). */
        public Builder state(char key, BlockState state) {
            return define(key, List.of(weight(1, () -> state)), true);
        }

        /**
         * Maps a character to a state that must NOT be rotated with the
         * building. Rare - you want this for a block whose facing means
         * something absolute (a compass-like decoration), never for walls.
         */
        public Builder fixed(char key, Supplier<BlockState> state) {
            return define(key, List.of(weight(1, state)), false);
        }

        /** Uniform random pick between blocks. Great for crumbly, non-uniform cereal walls. */
        public Builder random(char key, Block... blocks) {
            List<Weighted> list = new ArrayList<>(blocks.length);
            for (Block b : blocks) {
                list.add(weight(1, b::defaultBlockState));
            }
            return define(key, list, true);
        }

        /** Weighted random pick. Build entries with {@link Palette#weight(int, Supplier)}. */
        public Builder weighted(char key, List<Weighted> choices) {
            return define(key, List.copyOf(choices), true);
        }

        /**
         * Allocates a private character for a state you do not want to name.
         * Used internally by the door/bed helpers; also handy when you need one
         * odd block and do not want to burn a readable letter on it.
         *
         * @return the character to hand to {@code set}/{@code fill}/etc.
         */
        public char auto(Supplier<BlockState> state) {
            char key = nextAuto++;
            entries.put(key, new Entry(List.of(weight(1, state)), true));
            return key;
        }

        public Palette build() {
            return new Palette(Map.copyOf(entries));
        }
    }
}
