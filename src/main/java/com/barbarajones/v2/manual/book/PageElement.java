package com.barbarajones.v2.manual.book;

import java.util.List;

/**
 * One laid-out unit of the manual, in content order.
 *
 * <p>A chapter is nothing but a flat {@code List<PageElement>} (see
 * {@link ManualChapter}) - there is no hand-authored notion of "page 3 of
 * this chapter" anywhere in this package. {@code client.Paginator} measures
 * every element against the screen's actual content width and height and
 * slices the list into pages at render time, greedily, never splitting one
 * element across a page break. That is what makes the book correct at every
 * window size instead of just the one size it happened to be authored at -
 * resize the game and the same words re-flow into different pages rather
 * than clipping.
 *
 * <p>Every element type here is content-only data: no colour, no font, no
 * pixel maths. {@code client.ManualRenderer} is the only place that turns
 * one of these into pixels, which keeps every chapter file in
 * {@code book.content} pure prose and easy to review without also reading
 * rendering code.
 */
public sealed interface PageElement {

    /** A chapter or section title. Large, gold, with a rule underneath. */
    record Heading(String text) implements PageElement { }

    /** A smaller heading for a subsection within a chapter. */
    record Sub(String text) implements PageElement { }

    /** A wrapped paragraph of body text. {@code color} is a packed ARGB, or 0 for the default ink. */
    record Para(String text, int color) implements PageElement {
        public Para(String text) {
            this(text, 0);
        }
    }

    /** A bulleted list, one wrapped, hanging-indented line per entry. */
    record Bullets(List<String> lines) implements PageElement { }

    /** A numbered list - for anything order-sensitive, like a build sequence. */
    record Steps(List<String> lines) implements PageElement { }

    /** A thin horizontal rule. */
    record Divider() implements PageElement { }

    /** Empty vertical space, in pixels. */
    record Spacer(int px) implements PageElement { }

    /** Forces the next element onto a fresh page - used sparingly, mostly at chapter ends. */
    record HardBreak() implements PageElement { }

    /**
     * A row of item/block-render illustrations with captions underneath -
     * "drawn from item/block renders" is the whole design language of this
     * book's art, and this is the element that puts it on the page.
     */
    record Gallery(List<Icon> icons) implements PageElement { }

    /** A highlighted box for a warning, a tip, or the one sentence that matters most on the page. */
    record Callout(String label, String text, int accent) implements PageElement { }

    /** A simple column table - headers plus rows of strings, e.g. the ascension ladder. */
    record Table(List<String> headers, List<List<String>> rows, List<Integer> widths) implements PageElement { }

    /**
     * An actual crafting-table grid: nine cells, row-major, left to right then
     * top to bottom, {@link Icon#EMPTY} for a blank cell. Shows the real shape
     * of a shaped recipe; for a shapeless one the cells are simply filled in
     * order and {@code shapeless} switches the caption to say so.
     */
    record CraftGrid(List<Icon> cells, boolean shapeless, Icon output, int outputCount, String note)
            implements PageElement { }

    /**
     * Any other "recipe" that is not a crafting-table grid - feeding an entity,
     * right-clicking a block, smelting, a machine's input/output. Drawn as an
     * icon flow (input [+ input ...] -> output) with a verb label so it reads
     * as its own kind of interaction rather than a fake grid.
     */
    record FlowRecipe(String verb, List<Icon> inputs, Icon output, int outputCount, String note)
            implements PageElement { }
}
