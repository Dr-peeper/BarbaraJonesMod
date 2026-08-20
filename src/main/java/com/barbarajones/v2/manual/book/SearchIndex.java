package com.barbarajones.v2.manual.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Plain substring search over every chapter's text-bearing elements.
 *
 * <p>No stemming, no ranking beyond "title match beats body match beats
 * caption match" - a manual this size does not need a search engine, it
 * needs "type Cayden, land on the Cayden chapter." Built once from
 * {@link ManualBook#CHAPTERS} and reused for the screen's whole lifetime;
 * chapters are static content, so there is nothing to invalidate.
 */
public final class SearchIndex {

    private SearchIndex() { }

    /**
     * @param chapterIndex  index into {@link ManualBook#CHAPTERS}
     * @param elementIndex  index into that chapter's element list - where {@code Paginator}
     *                      should land the reader
     * @param snippet       the matched line, for the results list
     * @param titleMatch    true when the match was the chapter title itself, for ranking
     */
    public record Hit(int chapterIndex, int elementIndex, String chapterTitle, String snippet, boolean titleMatch) { }

    public static List<Hit> search(String rawQuery) {
        List<Hit> hits = new ArrayList<>();
        String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) {
            return hits;
        }
        List<ManualChapter> chapters = ManualBook.CHAPTERS;
        for (int ci = 0; ci < chapters.size(); ci++) {
            ManualChapter chapter = chapters.get(ci);
            if (chapter.title().toLowerCase(Locale.ROOT).contains(q)) {
                hits.add(new Hit(ci, 0, chapter.title(), chapter.teaser(), true));
            }
            List<PageElement> els = chapter.elements();
            for (int ei = 0; ei < els.size(); ei++) {
                String text = textOf(els.get(ei));
                if (text == null) {
                    continue;
                }
                if (text.toLowerCase(Locale.ROOT).contains(q)) {
                    hits.add(new Hit(ci, ei, chapter.title(), snippet(text, q), false));
                    if (hits.size() >= 40) {
                        return hits;
                    }
                }
            }
        }
        return hits;
    }

    private static String snippet(String text, String q) {
        String trimmed = text.length() > 90 ? text.substring(0, 90) + "..." : text;
        return trimmed;
    }

    private static String textOf(PageElement el) {
        if (el instanceof PageElement.Heading h) {
            return h.text();
        }
        if (el instanceof PageElement.Sub s) {
            return s.text();
        }
        if (el instanceof PageElement.Para p) {
            return p.text();
        }
        if (el instanceof PageElement.Bullets b) {
            return String.join(" ", b.lines());
        }
        if (el instanceof PageElement.Steps s) {
            return String.join(" ", s.lines());
        }
        if (el instanceof PageElement.Callout c) {
            return c.label() + " - " + c.text();
        }
        if (el instanceof PageElement.Table t) {
            StringBuilder sb = new StringBuilder(String.join(" ", t.headers()));
            for (List<String> row : t.rows()) {
                sb.append(' ').append(String.join(" ", row));
            }
            return sb.toString();
        }
        return null;
    }
}
