package com.barbarajones.v2.manual.book;

import java.util.List;

/**
 * One chapter of the Krave Manual.
 *
 * @param id       stable short id, used only for logging/search result keys
 * @param number   1-based chapter number, as printed in the contents list
 * @param title    the chapter's display title
 * @param teaser   one line shown on the contents page under the title
 * @param icon     the illustration shown next to the title everywhere - a real
 *                 item/block render, never hand-painted art
 * @param accent   packed ARGB used for this chapter's heading rules and tab
 * @param elements the whole chapter, flat, in reading order - see
 *                 {@link PageElement} for why this is deliberately not
 *                 pre-split into pages
 */
public record ManualChapter(String id, int number, String title, String teaser, Icon icon, int accent,
                            List<PageElement> elements) {
}
