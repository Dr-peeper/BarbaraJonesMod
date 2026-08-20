package com.barbarajones.v2.build.def;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A character-to-{@link BlockState} vocabulary shared across every
 * {@link StructureDef} that references it, so a whole village's worth of
 * buildings reads as one material palette rather than each one picking its
 * own blocks independently. See {@code HousePalette} for the concrete table
 * the house module builds from this.
 */
public final class Palette {

    private final Map<Character, Supplier<BlockState>> states;

    private Palette(Map<Character, Supplier<BlockState>> states) {
        this.states = states;
    }

    /** Resolves a character to a fresh {@link BlockState}, or {@code null} if this palette has no entry for it. */
    BlockState resolve(char key) {
        Supplier<BlockState> supplier = this.states.get(key);
        return supplier == null ? null : supplier.get();
    }

    boolean has(char key) {
        return this.states.containsKey(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Character, Supplier<BlockState>> states = new HashMap<>();

        public Builder block(char key, Block block) {
            this.states.put(key, block::defaultBlockState);
            return this;
        }

        public Builder block(char key, RegistryObject<? extends Block> block) {
            this.states.put(key, () -> block.get().defaultBlockState());
            return this;
        }

        public Builder state(char key, Supplier<BlockState> state) {
            this.states.put(key, state);
            return this;
        }

        public Palette build() {
            return new Palette(Map.copyOf(this.states));
        }
    }
}
