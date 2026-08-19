package com.barbarajones.client.ui;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.ApocalypseClient;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The objective tracker: a small Krave panel in the top-left showing what the
 * questline wants from you right now, plus a progress pip and the overall bar.
 *
 * <p>It reads straight off the Quest Book's NBT, which rides normal inventory sync,
 * so there is no packet and no server state behind any of this. No book in your
 * inventory means no panel - the HUD never nags a player who has thrown it away.
 *
 * <p>The same tick loop diffs the book's completed set against the previous tick and
 * raises a {@link KraveToasts} popup for anything new, and watches the player's
 * experience level for level-ups. The very first sync after joining is swallowed on
 * purpose, otherwise logging in would fire one toast per already-finished quest.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveHud {

    private static final int X = 6;
    private static final int Y = 6;
    private static final int WIDTH = 178;
    /** How many lines of objective text the panel will show before truncating. */
    private static final int OBJECTIVE_LINES = 2;
    /** Quest sweeps are cheap but pointless every tick; a quarter second is plenty. */
    private static final int WATCH_INTERVAL = 5;

    private static Set<String> lastDone;
    private static int lastLevel = -1;
    private static int watchTimer;

    private KraveHud() { }

    // ---- the watcher (drives the toasts) ------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            watch();
        } catch (Throwable ignored) {
            // the tracker must never take the game down
        }
    }

    private static void watch() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            lastDone = null;
            lastLevel = -1;
            return;
        }
        if (--watchTimer > 0) {
            return;
        }
        watchTimer = WATCH_INTERVAL;

        // level-ups off the vanilla XP level. Going down (death, spending on an
        // anvil) is deliberately silent.
        int level = player.experienceLevel;
        if (lastLevel >= 0 && level > lastLevel) {
            KraveToasts.levelUp(level, level >= 30 ? "Barbara is proud." : "Keep eating.");
        }
        lastLevel = level;

        ItemStack book = Quests.findBook(player);
        if (book == null) {
            lastDone = null;
            return;
        }
        Set<String> now = doneSet(book);
        if (lastDone == null) {
            lastDone = now;          // first sync after joining: seed, do not announce
            return;
        }
        for (Quests.Quest q : Quests.ALL) {
            if (now.contains(q.id) && !lastDone.contains(q.id)) {
                KraveToasts.questComplete(q.title, q.branch);
            }
        }
        lastDone = now;
    }

    private static Set<String> doneSet(ItemStack book) {
        Set<String> done = new HashSet<>();
        for (Quests.Quest q : Quests.ALL) {
            if (Quests.isDone(book, q.id)) {
                done.add(q.id);
            }
        }
        return done;
    }

    // ---- the panel ----------------------------------------------------------

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        // pinned to one overlay so this draws once a frame, not once per overlay
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) {
            return;
        }
        // a screen is up: only ours keep the HUD visible behind them
        if (mc.screen != null && !(mc.screen instanceof KraveScreen)) {
            return;
        }
        // the apocalypse is its own show; a quest tracker over it is just litter
        if (ApocalypseClient.isActive()) {
            return;
        }
        try {
            ItemStack book = Quests.findBook(mc.player);
            if (book != null) {
                draw(event.getGuiGraphics(), mc.font, book);
            }
        } catch (Throwable ignored) {
            // the HUD must never take the game down
        }
    }

    private static void draw(GuiGraphics gfx, Font font, ItemStack book) {
        int done = Quests.doneCount(book);
        int total = Math.max(1, Quests.total());
        float frac = Mth.clamp(done / (float) total, 0.0F, 1.0F);

        Quests.Quest current = currentQuest(book);
        boolean finished = current == null;

        int pad = KraveTheme.BORDER;
        int cx = X + pad + KraveTheme.PAD_S;
        int cw = WIDTH - (pad + KraveTheme.PAD_S) * 2;

        String headline = finished
                ? "PEACE AT LAST"
                : "[" + current.branch + "] " + current.title;
        List<String> objective = finished
                ? KraveTheme.wrap(font, "Every branch walked. Every item collected.",
                        cw - 9, OBJECTIVE_LINES)
                : KraveTheme.wrap(font, current.objective, cw - 9, OBJECTIVE_LINES);

        int height = pad * 2 + KraveTheme.TITLE_H + KraveTheme.PAD_S
                + 10 + objective.size() * KraveTheme.LINE_H + 5 + KraveTheme.BAR_H + 3;

        int accent = finished ? KraveTheme.GOLD : KraveTheme.BOX_RED;
        KraveTheme.panel(gfx, X, Y, WIDTH, height, accent);
        KraveTheme.titleBar(gfx, font, X + pad, Y + pad, WIDTH - pad * 2,
                "Krave Quest", done + "/" + total);

        int y = Y + pad + KraveTheme.TITLE_H + KraveTheme.PAD_S;

        // the progress pip: it breathes, so a glance tells you the tracker is live
        int pipColor = finished ? KraveTheme.GOLD
                : KraveTheme.lerpColor(KraveTheme.BOX_RED, KraveTheme.GOLD, KraveTheme.breathe());
        KraveTheme.pip(gfx, cx, y + 2, pipColor);

        gfx.drawString(font, ChatFormatting.BOLD + KraveTheme.trimTo(font, headline, cw - 9),
                cx + 9, y, finished ? KraveTheme.GOLD : KraveTheme.MILK, true);
        y += 10;

        for (String line : objective) {
            gfx.drawString(font, line, cx + 9, y, KraveTheme.TEXT_DIM, false);
            y += KraveTheme.LINE_H;
        }
        y += 5;

        String percent = (int) (frac * 100.0F) + "%";
        int barW = cw - font.width(percent) - 5;
        KraveTheme.progressBar(gfx, cx, y, barW, KraveTheme.BAR_H, frac,
                finished ? KraveTheme.GOLD : KraveTheme.GRASS);
        gfx.drawString(font, percent, cx + cw - font.width(percent), y, KraveTheme.TEXT_DIM, false);
    }

    /**
     * The quest to put on screen: the first one in graph order that is unlocked and
     * still open. Graph order runs hub, story spine, then the branches, so the canon
     * thread wins whenever it is actually available.
     */
    @Nullable
    private static Quests.Quest currentQuest(ItemStack book) {
        for (Quests.Quest q : Quests.ALL) {
            if (!Quests.isDone(book, q.id) && Quests.isUnlocked(book, q)) {
                return q;
            }
        }
        return null;
    }
}
