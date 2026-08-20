package com.barbarajones.v2.manual.client;

import com.barbarajones.v2.manual.book.PageElement;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

/**
 * Slices a chapter's flat element list into screen-sized pages.
 *
 * <p>Greedy, single pass: keep adding elements to the current page while they
 * fit; the moment one would overflow, close the page and start the next one
 * with that element. A {@link PageElement.HardBreak} always closes the
 * current page (used sparingly, mostly between a chapter's major sections).
 * An element taller than a whole page still goes on its own page rather than
 * being dropped - {@code ManualScreen}'s content viewport is scissored and
 * scrollable specifically so that edge case is a scrollbar, never a crash or
 * lost content.
 *
 * <p>Re-run whenever the available width/height changes (a window resize),
 * so the same chapter always re-flows correctly instead of clipping.
 */
final class Paginator {

    private Paginator() { }

    /** One page: the elements on it, and each one's top-left y within the page (0 at the page top). */
    record Page(List<Integer> elementIndices, List<Integer> ys, int totalHeight) { }

    static List<Page> paginate(Font font, List<PageElement> elements, int width, int height) {
        List<Page> pages = new ArrayList<>();
        List<Integer> curIdx = new ArrayList<>();
        List<Integer> curYs = new ArrayList<>();
        int y = 0;

        for (int i = 0; i < elements.size(); i++) {
            PageElement el = elements.get(i);
            if (el instanceof PageElement.HardBreak) {
                if (!curIdx.isEmpty()) {
                    pages.add(new Page(List.copyOf(curIdx), List.copyOf(curYs), y));
                    curIdx.clear();
                    curYs.clear();
                    y = 0;
                }
                continue;
            }
            int h = ManualRenderer.measure(font, el, width);
            boolean overflow = y > 0 && y + h > height;
            if (overflow) {
                pages.add(new Page(List.copyOf(curIdx), List.copyOf(curYs), y));
                curIdx.clear();
                curYs.clear();
                y = 0;
            }
            curIdx.add(i);
            curYs.add(y);
            y += h;
        }
        if (!curIdx.isEmpty()) {
            pages.add(new Page(List.copyOf(curIdx), List.copyOf(curYs), y));
        }
        if (pages.isEmpty()) {
            pages.add(new Page(List.of(), List.of(), 0));
        }
        return pages;
    }

    /** Which page (0-based) contains a given element index, for search-result jumps. */
    static int pageOf(List<Page> pages, int elementIndex) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).elementIndices().contains(elementIndex)) {
                return i;
            }
        }
        return 0;
    }
}
