package com.barbarajones.v2.manual;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * The Krave Manual, 2.0 edition - package {@code com.barbarajones.v2.manual}.
 *
 * <p><b>There is nothing for the orchestrator to wire.</b> This module
 * registers zero new items, blocks, or entities: the manual is still the
 * exact same {@code barbarajones:krave_manual} item that already existed
 * (owned by {@code com.barbarajones.item.KraveManualItem}, constructed from
 * the forbidden {@code content.ModItems}), and it opens the exact same way it
 * always has - right-click, {@code ClientPacketHandler.openManual()}. This
 * module only replaced what that click opens: the actual book content and
 * screen, entirely inside this package.
 *
 * <p>{@link #init(IEventBus)} exists only so this module has the same
 * one-entry-point shape every other module documents, per the task's own
 * rules. It is a deliberate no-op - see the class javadoc on
 * {@code book.ManualBook} for where the real content lives, and
 * {@code client.ManualScreen} for the UI. Safe to call, safe not to call.
 *
 * <p><b>Compatibility shim.</b> Two files this module does not own still
 * point at this one by exact name:
 * <ul>
 *   <li>{@code content.ModItems} (forbidden to edit) constructs
 *       {@code new com.barbarajones.item.KraveManualItem(...)} by exact
 *       type - so that class, its package, and its no-arg-friendly
 *       constructor could not move.</li>
 *   <li>{@code client.ClientPacketHandler#openManual()} (not forbidden, but
 *       not this module's to rename either) does
 *       {@code Minecraft.getInstance().setScreen(new
 *       com.barbarajones.client.KraveManualScreen())}.</li>
 * </ul>
 * Both were left exactly where they were, doing exactly what they did.
 * {@code client.KraveManualScreen} is now a two-line subclass of this
 * module's real {@code client.ManualScreen} - see that file for the full
 * explanation. Every line of actual 2.0 manual content, layout, search, and
 * illustration work lives here, in {@code v2.manual}, as asked.
 */
public final class ManualModule {

    private ManualModule() { }

    public static void init(IEventBus bus) {
        // Nothing to register - see the class javadoc.
    }
}
