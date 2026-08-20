package com.barbarajones.v2.manual.book;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * One illustration in the manual: whatever {@link #stack()} resolves to, drawn
 * with the game's own item renderer, plus a short caption.
 *
 * <p>Per the task brief, every picture in this book is "drawn from item/block
 * renders" rather than hand-painted - there is no portrait art anywhere in the
 * manual, only real {@link ItemStack} icons (a Krave Cereal box, a spawn egg
 * standing in for a mob that has no held item, a tool, a block). That is a
 * deliberate constraint, not a shortcut: it means every illustration in the
 * book is guaranteed to look exactly like the thing does in your inventory.
 *
 * <p>The stack is resolved lazily, from a {@link Supplier}, and every call site
 * in this package goes through {@link #of} rather than {@code new ItemStack(...)}
 * directly. Two reasons:
 * <ol>
 *   <li>Some content this book documents (a couple of Craveling-family items,
 *       some machine blocks) lives in sibling modules that were still being
 *       written elsewhere in the codebase as this one was built - see
 *       {@code docs/modules/krave-manual.md}'s gap list. A lazy, guarded
 *       resolve means a page about them still opens and still reads clearly
 *       even on a build where that class or id is not there yet, instead of
 *       taking the whole book down with it.</li>
 *   <li>Some of these constants are touched before the item/block registries
 *       have necessarily finished thawing (this class can be loaded early by
 *       the JVM once anything in the manual package is referenced). Lazy
 *       resolution defers the actual registry read to first render, by which
 *       point the game is long past that window.</li>
 * </ol>
 */
public final class Icon {

    /** Shown whenever the real stack cannot be resolved - never a hard crash. */
    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

    private final Supplier<ItemStack> supplier;
    private final String label;

    private ItemStack cached;
    private boolean resolved;

    private Icon(Supplier<ItemStack> supplier, String label) {
        this.supplier = supplier;
        this.label = label;
    }

    public static Icon of(Supplier<ItemStack> supplier, String label) {
        return new Icon(supplier, label);
    }

    public static Icon of(Supplier<ItemStack> supplier) {
        return new Icon(supplier, "");
    }

    /** Resolves and memoises the stack. Never throws, never returns null. */
    public ItemStack stack() {
        if (!this.resolved) {
            this.resolved = true;
            ItemStack s;
            try {
                s = this.supplier.get();
            } catch (RuntimeException | LinkageError ex) {
                s = null;
            }
            this.cached = (s == null || s.isEmpty()) ? FALLBACK : s;
        }
        return this.cached;
    }

    public String label() {
        return this.label;
    }

    public static final Icon EMPTY = Icon.of(() -> ItemStack.EMPTY, "");
    public static final Icon BLANK_SLOT = Icon.of(() -> ItemStack.EMPTY, "");
}
