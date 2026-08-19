package com.barbarajones.client.ui;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Krave toasts: the little cereal-box popups that slide down from the top of the
 * screen when a quest completes or you gain a level.
 *
 * <p>They are strictly <b>queued</b> - exactly one is on screen at a time. Finishing
 * one collect quest usually cascades into two or three more completing on the same
 * tick, and three toasts stacked on top of each other is unreadable noise, so extras
 * wait their turn. The queue is capped; past the cap new toasts are dropped rather
 * than allowed to build a minute-long backlog.
 *
 * <p>Top-centre on purpose: the quest HUD owns the top-left and vanilla's own
 * advancement toasts own the top-right.
 *
 * <p>Anything in the mod can raise one - {@link #questComplete}, {@link #levelUp},
 * {@link #info}, {@link #danger} - from the client side only.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveToasts {

    /** Ticks spent sliding in, sitting still, and sliding back out. */
    private static final int IN_TICKS = 5;
    private static final int HOLD_TICKS = 70;
    private static final int OUT_TICKS = 6;
    private static final int LIFE = IN_TICKS + HOLD_TICKS + OUT_TICKS;

    private static final int MAX_QUEUED = 8;
    private static final int TOAST_H = 40;
    private static final int MIN_W = 156;
    private static final int MAX_W = 250;
    private static final int TOP_GAP = 6;

    /** What kind of news this is: sets the band colour, the icon and the sting. */
    public enum Kind {
        QUEST(KraveTheme.GRASS, KraveTheme.ICON_CHECK, "QUEST COMPLETE",
                () -> ModSounds.CAYDEN_SHOUT.get(), 1.0F, 0.55F),
        LEVEL(KraveTheme.GOLD, KraveTheme.ICON_STAR, "LEVEL UP",
                () -> ModSounds.EVT_OG.get(), 1.0F, 0.7F),
        INFO(KraveTheme.PURPLE_LIGHT, KraveTheme.ICON_BOX, "KRAVE", null, 1.0F, 0.0F),
        DANGER(KraveTheme.DANGER, KraveTheme.ICON_SKULL, "WARNING",
                () -> ModSounds.KRAVE_SCREECH.get(), 1.25F, 0.45F);

        private final int accent;
        private final int icon;
        private final String label;
        private final Supplier<SoundEvent> sting;
        private final float pitch;
        private final float volume;

        Kind(int accent, int icon, String label, Supplier<SoundEvent> sting,
             float pitch, float volume) {
            this.accent = accent;
            this.icon = icon;
            this.label = label;
            this.sting = sting;
            this.pitch = pitch;
            this.volume = volume;
        }
    }

    private static final class Toast {
        private final Kind kind;
        private final String title;
        private final String subtitle;
        private final int icon;
        private int age;

        Toast(Kind kind, String title, String subtitle, int icon) {
            this.kind = kind;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.icon = icon;
        }
    }

    private static final Deque<Toast> QUEUE = new ArrayDeque<>();
    private static Toast current;

    private KraveToasts() { }

    // ---- public API ---------------------------------------------------------

    /** "QUEST COMPLETE - Roll Up", with the branch name underneath. */
    public static void questComplete(String questTitle, String branch) {
        push(Kind.QUEST, questTitle, branch == null ? "" : branch + " branch",
                KraveTheme.ICON_CHECK);
    }

    public static void levelUp(int level) {
        levelUp(level, "Keep eating.");
    }

    public static void levelUp(int level, String flavour) {
        push(Kind.LEVEL, "Level " + level, flavour, KraveTheme.ICON_STAR);
    }

    public static void info(String title, String subtitle) {
        push(Kind.INFO, title, subtitle, KraveTheme.ICON_BOX);
    }

    public static void danger(String title, String subtitle) {
        push(Kind.DANGER, title, subtitle, KraveTheme.ICON_SKULL);
    }

    /** The general form: any kind, any icon index from {@link KraveTheme}. */
    public static void push(Kind kind, String title, String subtitle, int iconIndex) {
        if (kind == null || QUEUE.size() >= MAX_QUEUED) {
            return;
        }
        QUEUE.addLast(new Toast(kind, title, subtitle, iconIndex));
    }

    /** Drop everything - used on world change so old news never follows you. */
    public static void clear() {
        QUEUE.clear();
        current = null;
    }

    // ---- driver -------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        try {
            advance(mc);
        } catch (Throwable ignored) {
            // never let a popup take the game down
        }
    }

    private static void advance(Minecraft mc) {
        if (current != null) {
            current.age++;
            if (current.age >= LIFE) {
                current = null;
            }
        }
        if (current == null && !QUEUE.isEmpty()) {
            current = QUEUE.pollFirst();
            playSting(mc, current.kind);
        }
    }

    private static void playSting(Minecraft mc, Kind kind) {
        if (kind.sting == null || kind.volume <= 0.0F) {
            return;
        }
        try {
            SoundEvent sound = kind.sting.get();
            if (sound != null) {
                mc.getSoundManager().play(
                        SimpleSoundInstance.forUI(sound, kind.pitch, kind.volume));
            }
        } catch (Throwable ignored) {
            // a missing sound must not stop the popup from showing
        }
    }

    // ---- render -------------------------------------------------------------

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        // one specific overlay, so this draws exactly once a frame instead of
        // once per registered overlay
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || current == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) {
            return;
        }
        // a screen is up: only ours are allowed to keep the toast visible
        if (mc.screen != null && !(mc.screen instanceof KraveScreen)) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), mc.font, current);
        } catch (Throwable ignored) {
            // never let a popup take the game down
        }
    }

    private static void draw(GuiGraphics gfx, Font font, Toast toast) {
        float appear;
        if (toast.age < IN_TICKS) {
            appear = toast.age / (float) IN_TICKS;
        } else if (toast.age < IN_TICKS + HOLD_TICKS) {
            appear = 1.0F;
        } else {
            appear = 1.0F - (toast.age - IN_TICKS - HOLD_TICKS) / (float) OUT_TICKS;
        }
        appear = Mth.clamp(appear, 0.0F, 1.0F);
        // smoothstep, so it settles instead of snapping to a stop
        float ease = appear * appear * (3.0F - 2.0F * appear);

        String label = toast.kind.label;
        int w = Mth.clamp(Math.max(Math.max(font.width(toast.title), font.width(toast.subtitle)),
                font.width(label)) + 46, MIN_W, MAX_W);
        int x = (gfx.guiWidth() - w) / 2;
        int y = (int) (-TOAST_H + (TOAST_H + TOP_GAP) * ease);

        KraveTheme.panel(gfx, x, y, w, TOAST_H, toast.kind.accent);

        // a hairline timer across the top of the body, so the popup visibly has a
        // clock on it - it sits above the text rather than under it, where the
        // subtitle's descenders would collide with it
        int remaining = Mth.clamp(LIFE - OUT_TICKS - toast.age, 0, HOLD_TICKS);
        int barW = (w - KraveTheme.BORDER * 2) * remaining / HOLD_TICKS;
        if (barW > 0) {
            gfx.fill(x + KraveTheme.BORDER, y + KraveTheme.BORDER,
                    x + KraveTheme.BORDER + barW, y + KraveTheme.BORDER + 2,
                    KraveTheme.withAlpha(toast.kind.accent, 0.75F));
        }

        KraveTheme.icon(gfx, toast.icon, x + 8, y + 12);

        int textX = x + 30;
        int textW = w - 30 - KraveTheme.PAD;
        gfx.drawString(font, ChatFormatting.BOLD + label, textX, y + 8, toast.kind.accent, false);
        gfx.drawString(font, KraveTheme.trimTo(font, toast.title, textW),
                textX, y + 18, KraveTheme.MILK, true);
        if (!toast.subtitle.isEmpty()) {
            gfx.drawString(font, KraveTheme.trimTo(font, toast.subtitle, textW),
                    textX, y + 28, KraveTheme.TEXT_DIM, false);
        }
    }
}
