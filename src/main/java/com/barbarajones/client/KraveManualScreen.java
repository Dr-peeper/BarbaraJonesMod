package com.barbarajones.client;

/**
 * Compatibility shim, kept at this exact class name and package on purpose:
 * {@link ClientPacketHandler#openManual()} - not owned by the 2.0 manual
 * rewrite - instantiates this exact type by name
 * ({@code new com.barbarajones.client.KraveManualScreen()}), so the class
 * cannot move without also editing that file.
 *
 * <p>Every real line of the 2.0 Krave Manual - eleven chapters, the contents
 * page, chapter navigation, search, item/block-render illustrations, real
 * crafting-grid recipe displays, and the page-turn feel - lives in
 * {@code com.barbarajones.v2.manual}, the package the rewrite task asked
 * for. This file is nothing but the two-line bridge between the old, fixed
 * call site and the new implementation. See
 * {@code com.barbarajones.v2.manual.book.ManualBook} for the book's content
 * and {@code com.barbarajones.v2.manual.client.ManualScreen} for the screen
 * itself, and {@code docs/modules/krave-manual.md} for the full writeup.
 */
public class KraveManualScreen extends com.barbarajones.v2.manual.client.ManualScreen {

    public KraveManualScreen() {
        super();
    }
}
