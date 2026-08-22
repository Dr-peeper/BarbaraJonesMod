package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.ui.KraveKeys;
import com.barbarajones.net.ModNetwork;
import com.barbarajones.net.PacketKraveQteInput;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The finisher prompt: draws it, and reports the keypress.
 *
 * <p>Nothing here decides anything. It is told to show a prompt for a number of
 * ticks, and when the key goes down it sends an empty message saying so. Whether
 * that press counted - whether there was a window at all, whether it belonged to
 * this player, whether it has already been used - is entirely the server's, in
 * {@code KraveKosmosBattle.onQteInput}. A modified client can send the message
 * whenever it likes and get nothing for it.
 *
 * <p>The local timer is cosmetic. It drains the bar smoothly between packets and
 * hides the prompt if the connection drops mid-window, so a disconnect cannot
 * leave a prompt burned onto the screen.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveQteClient {

    private KraveQteClient() { }

    private static int ticksLeft;
    private static int windowLength;
    private static int form;
    private static boolean retry;
    private static int step;
    private static int totalSteps = 1;
    private static String key = "G";
    private static String caption = "";

    /** Called from the server packet. Zero ticks clears the prompt. */
    public static void accept(int ticks, int bossForm, boolean isRetry,
                              int qteStep, int steps, String qteKey, String qteCaption) {
        ticksLeft = ticks;
        windowLength = Math.max(1, ticks);
        form = bossForm;
        retry = isRetry;
        step = qteStep;
        totalSteps = Math.max(1, steps);
        key = qteKey;
        caption = qteCaption;
    }

    public static boolean active() {
        return ticksLeft > 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            ticksLeft = 0;
            return;
        }
        // EVERY finisher binding is drained every tick, whether or not a prompt
        // is up. Otherwise a click made just before one appears is banked and
        // spent the instant it does - which in a six-part finisher would answer
        // a step the player never actually saw. Only the key the server named
        // counts as an answer; the other five are drained and discarded.
        boolean pressed = false;
        net.minecraft.client.KeyMapping live = KraveKeys.finisherFor(key);
        for (net.minecraft.client.KeyMapping binding : KraveKeys.FINISHERS) {
            boolean any = false;
            while (binding.consumeClick()) {
                any = true;
            }
            if (any && binding == live) {
                pressed = true;
            }
        }
        if (ticksLeft <= 0) {
            return;
        }
        ticksLeft--;
        if (pressed) {
            ModNetwork.CHANNEL.sendToServer(new PacketKraveQteInput(form, step));
            // Cleared locally for responsiveness only. The server clears the
            // real window, and a press it rejects simply does nothing.
            ticksLeft = 0;
        }
    }

    // RenderGuiEvent.Post, not RenderGuiOverlayEvent.Post: the overlay event
    // fires once for every hotbar, crosshair and effect overlay the vanilla HUD
    // draws, so the prompt was being painted a dozen times a frame - which looks
    // fine and costs a dozen times what it should. This one fires once.
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        if (ticksLeft <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        Component title = Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                + "PRESS " + key);
        Component sub = Component.literal((retry ? ChatFormatting.RED + "AGAIN - " : ChatFormatting.GRAY + "")
                + caption);
        // "2 / 4" is the whole reason a multi-step finisher reads as one
        // sequence: without it the third prompt of a four-part form is
        // indistinguishable from the fight having failed and started over.
        Component progress = Component.literal(ChatFormatting.DARK_GRAY
                + "FORM " + form + "   " + (step + 1) + " / " + totalSteps);

        int cx = w / 2;
        int cy = h / 2 + 30;

        RenderSystem.enableBlend();
        // A panel behind it, because this has to read instantly over whatever
        // the arena happens to be doing - which by this point is on fire.
        g.fill(cx - 100, cy - 6, cx + 100, cy + 44, 0xA0000000);
        g.fill(cx - 100, cy - 6, cx + 100, cy - 5, 0xFFFFAA00);

        g.drawCenteredString(mc.font, title, cx, cy + 1, 0xFFFFFF);
        g.drawCenteredString(mc.font, sub, cx, cy + 13, 0xFFFFFF);
        g.drawCenteredString(mc.font, progress, cx, cy + 25, 0xFFFFFF);

        // The bar is the timer. It is the only thing telling the player how long
        // they still have.
        int barW = 190;
        int filled = Math.max(0, Math.min(barW, barW * ticksLeft / windowLength));
        g.fill(cx - barW / 2, cy + 37, cx + barW / 2, cy + 41, 0xFF303030);
        g.fill(cx - barW / 2, cy + 37, cx - barW / 2 + filled, cy + 41, 0xFFFF3020);
        RenderSystem.disableBlend();
    }
}
