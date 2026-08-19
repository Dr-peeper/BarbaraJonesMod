package com.barbarajones.client.title;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The custom Krave title screen: mod branding bolted onto the vanilla main menu.
 *
 * <p>Two handlers do the work. {@link #onScreenInit} runs after the vanilla screen
 * has built its widgets and re-lays them out into a single centred column, muting
 * each one's vanilla artwork with {@code setAlpha(0)}. {@link #onScreenRender} then
 * cancels the vanilla render entirely and paints the whole menu itself: the animated
 * Krave backdrop, the logo, the wordmark, the splash line, and a hand-drawn plate
 * behind every button before the widgets themselves are rendered on top.
 *
 * <p>Cancelling is what makes this a title screen rather than an overlay - the
 * vanilla panorama is drawn <em>inside</em> {@code TitleScreen#render}, so anything
 * painted before it is simply covered up. Nothing is removed and nothing is
 * replaced: Singleplayer, Multiplayer, Realms, Mods, Options and Quit are still the
 * same widget objects vanilla created, still in the screen's children list, so every
 * click, keyboard focus and narration path behaves exactly as it always did.
 *
 * <p><b>Failure is not an option here.</b> A cosmetic menu that throws would leave a
 * player unable to start the game at all, so every entry point is wrapped and the
 * first exception latches {@link #disabled} for the rest of the session, restores
 * the widgets' alpha and hands the menu back to vanilla untouched.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveTitleScreen {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final ResourceLocation LOGO =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/title/logo.png");

    /** Pixel size of logo.png - see tools/make_title_assets.ps1. */
    private static final int LOGO_W = 512;
    private static final int LOGO_H = 128;

    /** Vertical gap between menu rows. */
    private static final int GAP = 4;

    private static final long START = Util.getMillis();

    /** Latches on the first failure; from then on the vanilla menu is left alone. */
    private static boolean disabled = false;

    /**
     * Bounds of the card behind the menu, recorded by {@link #relayout} rather than
     * re-measured while drawing. A widget some other mod adds after our handler runs
     * would not have been repositioned, and measuring live would let it drag the card
     * across the whole screen.
     */
    private static int cardX0, cardY0, cardX1, cardY1;
    private static boolean hasCard = false;

    private KraveTitleScreen() { }

    // ---- events -------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (disabled || !(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        try {
            TitleFlicker.reset(Util.getMillis());
            relayout(screen);
        } catch (Throwable err) {
            fail(screen, err);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (disabled || !(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        GuiGraphics gfx = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        float partial = event.getPartialTick();

        // From here on the entire menu is ours to draw.
        event.setCanceled(true);
        try {
            draw(screen, gfx, mouseX, mouseY, partial);
        } catch (Throwable err) {
            fail(screen, err);
            // This frame's backdrop is already lost, but the player must still be able
            // to see and press the buttons on it. The next frame runs vanilla.
            renderWidgetsOnly(screen, gfx, mouseX, mouseY, partial);
        }
    }

    // ---- layout -------------------------------------------------------------

    /**
     * Pulls the main-menu entries into one centred column under the logo and switches
     * off their vanilla artwork.
     *
     * <p>Vanilla scatters these: Realms and Forge's Mods button sit side by side at
     * half width, and the language and accessibility icons are pinned to fixed offsets
     * either side of where Options used to be. Left alone, moving the column would
     * strand the icons in empty space, so they move too.
     */
    private static void relayout(TitleScreen screen) {
        hasCard = false;
        List<AbstractWidget> menu = menuButtons(screen);
        List<AbstractWidget> icons = iconButtons(screen);
        if (menu.isEmpty()) {
            return;
        }

        int w = screen.width;
        int h = screen.height;
        int cx = w / 2;
        int bw = Mth.clamp(w - 60, 120, 200);

        int blockH = -GAP;
        for (AbstractWidget b : menu) {
            blockH += b.getHeight() + GAP;
        }
        if (!icons.isEmpty()) {
            blockH += icons.get(0).getHeight() + GAP * 2;
        }

        int spaceTop = headerBottom(w, h) + 6;
        int spaceBottom = h - 26;                 // the branding footer lives below this
        int y = spaceTop + Math.max(0, (spaceBottom - spaceTop - blockH) / 2);

        int pad = 10;
        cardX0 = cx - bw / 2 - pad;
        cardY0 = y - pad;
        cardX1 = cx + bw / 2 + pad;
        cardY1 = y + blockH + pad;
        hasCard = true;

        for (AbstractWidget b : menu) {
            b.setWidth(bw);
            b.setX(cx - bw / 2);
            b.setY(y);
            // Alpha 0 hides vanilla's grey nine-slice AND its label in one call;
            // drawMenuButton repaints both in the Krave palette.
            b.setAlpha(0.0F);
            y += b.getHeight() + GAP;
        }

        if (!icons.isEmpty()) {
            y += GAP;
            int rowW = -GAP;
            for (AbstractWidget b : icons) {
                rowW += b.getWidth() + GAP;
            }
            int ix = cx - rowW / 2;
            for (AbstractWidget b : icons) {
                b.setX(ix);
                b.setY(y);
                ix += b.getWidth() + GAP;
            }
        }
    }

    /**
     * The main-menu entries.
     *
     * <p>The exact-class test is deliberate. Everything the title screen builds through
     * {@code Button.builder(...)} - including Forge's injected Mods button - is a plain
     * {@link Button}, while the language and accessibility icons are {@link ImageButton}
     * subclasses whose own artwork must survive. An {@code instanceof} test would eat
     * the icons and leave two blank plates in the column.
     */
    private static List<AbstractWidget> menuButtons(Screen screen) {
        List<AbstractWidget> out = new ArrayList<>();
        for (Renderable r : screen.renderables) {
            if (r.getClass() == Button.class) {
                out.add((AbstractWidget) r);
            }
        }
        return out;
    }

    private static List<AbstractWidget> iconButtons(Screen screen) {
        List<AbstractWidget> out = new ArrayList<>();
        for (Renderable r : screen.renderables) {
            if (r instanceof ImageButton icon) {
                out.add(icon);
            }
        }
        return out;
    }

    /** Scale that fits the logo into the top band of whatever window the player has. */
    private static float logoScale(int w, int h) {
        float byWidth = (w - 48.0F) / LOGO_W;
        float byHeight = (h * 0.26F) / LOGO_H;
        return Mth.clamp(Math.min(byWidth, byHeight), 0.16F, 0.95F);
    }

    private static int logoTop(int h) {
        return Math.max(6, Math.round(h * 0.05F));
    }

    private static int logoHeight(int w, int h) {
        return Math.round(LOGO_H * logoScale(w, h));
    }

    /** Bottom of the logo-plus-wordmark block; the button column starts beneath it. */
    private static int headerBottom(int w, int h) {
        return logoTop(h) + logoHeight(w, h) + 16;
    }

    // ---- drawing ------------------------------------------------------------

    private static void draw(TitleScreen screen, GuiGraphics gfx, int mouseX, int mouseY,
                             float partial) {
        Font font = Minecraft.getInstance().font;
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();
        long now = Util.getMillis();
        float t = (now - START) / 1000.0F;

        KraveBackdrop.draw(gfx, w, h, t);
        drawMenuCard(gfx);
        drawLogo(gfx, w, h, t);
        drawWordmark(gfx, font, w, h);
        drawSplash(gfx, font, w, h, now);

        for (Renderable r : screen.renderables) {
            if (r.getClass() == Button.class) {
                drawMenuButton(gfx, font, (AbstractWidget) r, mouseX, mouseY, t);
            } else if (r instanceof ImageButton icon) {
                drawIconPlate(gfx, icon, mouseX, mouseY);
            }
        }
        for (Renderable r : screen.renderables) {
            r.render(gfx, mouseX, mouseY, partial);
        }

        drawFooter(gfx, font, w, h);
        TitleFlicker.draw(gfx, w, h, now);
    }

    /** A dark card behind the whole column so the cereal storm never eats the labels. */
    private static void drawMenuCard(GuiGraphics gfx) {
        if (!hasCard) {
            return;
        }
        gfx.fill(cardX0, cardY0, cardX1, cardY1, 0x9C10041C);
        gfx.fill(cardX0, cardY0, cardX1, cardY0 + 1, 0x60FFC63A);
        gfx.fill(cardX0, cardY1 - 1, cardX1, cardY1, 0x60FFC63A);
    }

    private static void drawLogo(GuiGraphics gfx, int w, int h, float t) {
        float s = logoScale(w, h);
        float bob = Mth.sin(t * 1.1F) * 1.5F;      // slow breath so the mark never sits dead still
        float py = logoTop(h) + bob;

        RenderSystem.enableBlend();
        PoseStack pose = gfx.pose();

        // A blown-up, faint copy of the mark behind the solid one: a cheap bloom that
        // makes the logo look lit from within rather than pasted onto the sky.
        float gs = s * 1.10F;
        pose.pushPose();
        pose.translate(w / 2.0F - LOGO_W * gs / 2.0F, py - LOGO_H * (gs - s) / 2.0F, 0.0F);
        pose.scale(gs, gs, 1.0F);
        gfx.setColor(0.78F, 0.46F, 1.0F, 0.22F + 0.06F * Mth.sin(t * 2.3F));
        gfx.blit(LOGO, 0, 0, 0, 0.0F, 0.0F, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
        pose.popPose();

        pose.pushPose();
        pose.translate(w / 2.0F - LOGO_W * s / 2.0F, py, 0.0F);
        pose.scale(s, s, 1.0F);
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gfx.blit(LOGO, 0, 0, 0, 0.0F, 0.0F, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
        pose.popPose();

        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawWordmark(GuiGraphics gfx, Font font, int w, int h) {
        String mark = ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "THE BARBARA JONES MOD";
        int markW = Math.max(1, font.width(mark));
        float scale = Mth.clamp((w - 48.0F) / markW, 0.75F, 1.75F);
        int y = logoTop(h) + logoHeight(w, h) + 2;

        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(w / 2.0F, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        gfx.drawCenteredString(font, mark, 0, 0, 0xFFFFFFFF);
        pose.popPose();
    }

    private static void drawSplash(GuiGraphics gfx, Font font, int w, int h, long now) {
        String line = KraveSplashes.current(now);
        int alpha = (int) (KraveSplashes.fade(now) * 255.0F);
        // Font treats a packed alpha below 4 as "no alpha given" and forces it opaque,
        // so the tail of a fade has to be dropped rather than drawn.
        if (alpha < 8) {
            return;
        }

        // Vanilla's splash bounce, kept on purpose: it is the one piece of the old menu
        // that everybody recognises instantly.
        float bounce = 1.6F - Mth.abs(
                Mth.sin((now % 1000L) / 1000.0F * ((float) Math.PI * 2.0F)) * 0.12F);
        float scale = Mth.clamp(bounce * 90.0F / (font.width(line) + 24), 0.6F, 1.9F);

        int sx = Math.min(w / 2 + Math.round(LOGO_W * logoScale(w, h) * 0.30F), w - 46);
        int sy = logoTop(h) + Math.round(logoHeight(w, h) * 0.80F);

        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(sx, sy, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(-18.0F));
        pose.scale(scale, scale, 1.0F);
        gfx.drawCenteredString(font, line, 0, -4, (alpha << 24) | 0xFFEE58);
        pose.popPose();
    }

    private static void drawMenuButton(GuiGraphics gfx, Font font, AbstractWidget b,
                                       int mouseX, int mouseY, float t) {
        if (!b.visible) {
            return;
        }
        int x = b.getX();
        int y = b.getY();
        int bw = b.getWidth();
        int bh = b.getHeight();
        // Computed here rather than read from the widget: AbstractWidget only updates
        // its own isHovered inside render(), which happens after these plates are drawn,
        // so trusting it would leave the highlight a frame behind the cursor.
        boolean hover = b.active
                && mouseX >= x && mouseX < x + bw
                && mouseY >= y && mouseY < y + bh;

        int body = !b.active ? 0x88170722 : hover ? 0xE04A1E7C : 0xC8250B40;
        int edge = !b.active ? 0xFF3A2C46 : hover ? 0xFFFFC63A : 0xFF8552C8;

        gfx.fill(x, y, x + bw, y + bh, body);
        gfx.fill(x + 1, y + 1, x + bw - 1, y + bh / 2, hover ? 0x30FFFFFF : 0x18FFFFFF);
        gfx.fill(x, y, x + bw, y + 1, edge);
        gfx.fill(x, y + bh - 1, x + bw, y + bh, edge);
        gfx.fill(x, y, x + 1, y + bh, edge);
        gfx.fill(x + bw - 1, y, x + bw, y + bh, edge);

        if (hover) {
            // Two cereal-brown markers that creep inward - the menu equivalent of a
            // mouth watering.
            int creep = (int) (Mth.abs(Mth.sin(t * 3.4F)) * 3.0F);
            int my = y + bh / 2;
            gfx.fill(x + 5 - creep, my - 1, x + 8 - creep, my + 2, 0xFFC98B4B);
            gfx.fill(x + bw - 8 + creep, my - 1, x + bw - 5 + creep, my + 2, 0xFFC98B4B);
        }

        int label = !b.active ? 0xFF6E6478 : hover ? 0xFFFFF3CC : 0xFFE6D8F6;
        gfx.drawCenteredString(font, b.getMessage(), x + bw / 2,
                y + (bh - font.lineHeight) / 2 + 1, label);
    }

    /** Icon buttons keep their vanilla art, so they only get a plate to sit on. */
    private static void drawIconPlate(GuiGraphics gfx, AbstractWidget b, int mouseX, int mouseY) {
        if (!b.visible) {
            return;
        }
        int x = b.getX();
        int y = b.getY();
        int bw = b.getWidth();
        int bh = b.getHeight();
        boolean hover = mouseX >= x && mouseX < x + bw && mouseY >= y && mouseY < y + bh;
        gfx.fill(x, y, x + bw, y + bh, hover ? 0xC03E1868 : 0xA01D0733);
    }

    private static void drawFooter(GuiGraphics gfx, Font font, int w, int h) {
        // Vanilla draws the Mojang copyright itself inside TitleScreen#render, which we
        // cancelled - so we owe it. It stays exactly where players expect to find it.
        gfx.drawString(font, TitleScreen.COPYRIGHT_TEXT, 2, h - 10, 0xFFFFFFFF);
        gfx.drawString(font, ChatFormatting.DARK_RED + "" + ChatFormatting.BOLD
                + "RULE #1: DON'T LET HIM DIE", 2, h - 21, 0xFFFFFFFF);

        String tag = ChatFormatting.DARK_PURPLE + "Mr Pibb not included.";
        gfx.drawString(font, tag, w - font.width(tag) - 2, h - 10, 0xFFFFFFFF);
    }

    // ---- failure handling ---------------------------------------------------

    private static void fail(Screen screen, Throwable err) {
        if (!disabled) {
            disabled = true;
            LOGGER.error("Krave title screen failed; falling back to the vanilla menu", err);
        }
        restoreWidgets(screen);
    }

    /** Undo {@code setAlpha(0)} - a vanilla fallback full of invisible buttons is worse. */
    private static void restoreWidgets(Screen screen) {
        try {
            for (Renderable r : screen.renderables) {
                if (r instanceof AbstractWidget widget) {
                    widget.setAlpha(1.0F);
                }
            }
        } catch (Throwable ignored) {
            // Nothing further to attempt; the screen still handles its own clicks.
        }
    }

    private static void renderWidgetsOnly(Screen screen, GuiGraphics gfx, int mouseX, int mouseY,
                                          float partial) {
        try {
            for (Renderable r : screen.renderables) {
                r.render(gfx, mouseX, mouseY, partial);
            }
        } catch (Throwable ignored) {
            // Already in the failure path; the next frame is the untouched vanilla screen.
        }
    }
}
