package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.net.PacketApocalypse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Client-side director for the staged Krave Apocalypse: per-stage uncanny face
 * flashed fullscreen, the horizontal gates of hell overhead, a blood-black
 * flickering sky, blast flash and escalating shake.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class ApocalypseClient {

    private static final ResourceLocation[] FACES = new ResourceLocation[10];
    static {
        for (int i = 0; i < 10; i++) {
            FACES[i] = new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/face_" + (i + 1) + ".png");
        }
    }

    private static float redLevel = 0.0F;
    private static boolean active = false;
    private static int stage = 1;
    private static int jumpscareTimer = 0, jumpscareMax = 1;
    private static int flashTimer = 0;
    private static int wrathTimer = 0, wrathMax = 240;
    private static int cinLockTimer = 0;
    private static int safetyTimer = 0;
    private static final int SAFETY_MAX = 700;

    private static double deathX, deathY, deathZ;

    private ApocalypseClient() { }

    public static void handle(PacketApocalypse msg) {
        // The keyframe director owns the 3D staging and the camera from here.
        // This class keeps the fullscreen face and the blood sky, which work.
        com.barbarajones.cinematic.CinematicDirector.handle(msg);
        stage = Math.max(1, Math.min(10, msg.stage));
        switch (msg.phase) {
            case PacketApocalypse.PHASE_ONSET -> {
                active = true;
                jumpscareMax = stage <= 2 ? 30 : 44 + stage * 4;
                jumpscareTimer = jumpscareMax;
                deathX = msg.x; deathY = msg.y; deathZ = msg.z;
                safetyTimer = SAFETY_MAX;
                if (stage >= 2 && stage <= 5) {
                    cinLockTimer = 18;   // a longer, eased swing reads far smoother than a snap
                }
            }
            case PacketApocalypse.PHASE_WRATH -> {
                active = true;
                wrathMax = 200 + stage * 30;
                wrathTimer = wrathMax;
                deathX = msg.x; deathY = msg.y; deathZ = msg.z;
                safetyTimer = SAFETY_MAX;
                cinLockTimer = 16;
            }
            case PacketApocalypse.PHASE_BLAST -> flashTimer = 14;
            default -> clearAll();
        }
    }

    private static void clearAll() {
        active = false;
        wrathTimer = 0;
        jumpscareTimer = 0;
        flashTimer = 0;
        cinLockTimer = 0;
        safetyTimer = 0;
    }

    private static boolean anyActive() {
        return redLevel > 0.0F || flashTimer > 0 || jumpscareTimer > 0 || wrathTimer > 0;
    }

    /** True while the full apocalypse show is on screen - the ambient Dread stands down for it. */
    public static boolean isActive() {
        return anyActive();
    }

    // ---- easing + the camera swing -----------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // hard safety: never let the cinematic get stuck on
        if (safetyTimer > 0) {
            safetyTimer--;
            if (safetyTimer == 0) {
                clearAll();
            }
        } else if (active) {
            clearAll();
        }

        float target = active ? 1.0F : 0.0F;
        redLevel += (target - redLevel) * 0.1F;
        if (redLevel < 0.01F && !active) {
            redLevel = 0.0F;
        }
        if (jumpscareTimer > 0) { jumpscareTimer--; }
        if (flashTimer > 0) { flashTimer--; }
        if (wrathTimer > 0) { wrathTimer--; }

        Minecraft mc = Minecraft.getInstance();
        // One smooth swing onto the show, then the head is free again. Instead of a
        // fixed fraction toward the target each tick (which snaps, then crawls), we
        // capture the starting orientation once and glide to the target along a
        // smoothstep curve - it eases in, eases out, and the client interpolates the
        // per-tick yaw/pitch between frames, so the whole move reads as one silky pan.
        if (cinLockTimer > 0 && mc.player != null
                && !com.barbarajones.cinematic.CinematicDirector.isRunning()) {
            if (!swingCaptured) {
                swingStartYaw = mc.player.getYRot();
                swingStartPitch = mc.player.getXRot();
                swingTotal = Math.max(1, cinLockTimer);
                swingCaptured = true;
            }
            double ty = deathY + (stage >= 6 ? 55.0D : 24.0D);
            double dx = deathX - mc.player.getX();
            double dy = ty - (mc.player.getY() + 1.6D);
            double dz = deathZ - mc.player.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float wantYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.001D, horiz)));

            float prog = 1.0F - (cinLockTimer - 1) / (float) swingTotal;   // 0 -> 1
            float ease = prog * prog * (3.0F - 2.0F * prog);               // smoothstep
            mc.player.setYRot(swingStartYaw + Mth.wrapDegrees(wantYaw - swingStartYaw) * ease);
            mc.player.setXRot(swingStartPitch + (wantPitch - swingStartPitch) * ease);

            cinLockTimer--;
            if (cinLockTimer <= 0) {
                swingCaptured = false;
            }
        } else {
            swingCaptured = false;
        }

        // stage 7+: STOP LOOKING AT THE CAMERA
        if (wrathTimer > 0 && stage >= 7 && mc.player != null) {
            if (mc.player.getXRot() < -55.0F) {
                lookUpTicks++;
                if (lookUpTicks == 30) {
                    jumpscareTimer = 10;
                    jumpscareMax = 10;
                    mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            net.minecraft.ChatFormatting.DARK_RED + ""
                            + net.minecraft.ChatFormatting.BOLD + "STOP LOOKING AT THE CAMERA."));
                }
                if (lookUpTicks > 60) {
                    lookUpTicks = 0;
                }
            } else {
                lookUpTicks = 0;
            }
        }
    }

    private static int lookUpTicks = 0;

    // eased camera-swing state (captured once per swing, see onClientTick)
    private static boolean swingCaptured = false;
    private static float swingStartYaw = 0.0F;
    private static float swingStartPitch = 0.0F;
    private static int swingTotal = 1;

    /** Smoothstep: 0..1 in, eased 0..1 out. */
    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** A continuous real-time clock in seconds, independent of tick rate - drives smooth shake/pulse. */
    private static float clock() {
        return (float) (Util.getMillis() % 1_000_000L) * 0.001F;
    }

    // ---- blood-black flickering sky ----------------------------------------

    @SubscribeEvent
    public static void onFog(ViewportEvent.ComputeFogColor event) {
        if (redLevel <= 0.0F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float flick = 1.0F;
        if (wrathTimer > 0 && mc.level != null) {
            flick = 0.75F + (float) Math.sin(mc.level.getGameTime() * 1.3F) * 0.25F;
        }
        // Just enough haze on distant terrain that the world looks LIT by the red
        // sky. It must never approach a wall of colour - the sky carries the mood
        // now, so the fog only has to agree with it.
        float k = Math.min(0.45F, redLevel * (0.22F + stage * 0.02F));
        float rTarget = (wrathTimer > 0 ? 0.60F : 0.48F) * flick;
        event.setRed(event.getRed() * (1.0F - k) + rTarget * k);
        event.setGreen(event.getGreen() * (1.0F - k) + 0.07F * k);
        event.setBlue(event.getBlue() * (1.0F - k) + 0.07F * k);
    }

    // ---- the fullscreen overlay --------------------------------------------

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (!anyActive()) {
            return;
        }
        try {
            draw(event.getGuiGraphics());
        } catch (Throwable ignored) {
            // cosmetics must never crash the game
        }
    }

    private static void draw(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        int w = gfx.guiWidth(), h = gfx.guiHeight();
        long time = mc.level != null ? mc.level.getGameTime() : 0L;
        float ft = mc.getFrameTime();     // partial tick: makes every fade frame-smooth
        float rt = clock();               // continuous real-time seconds

        PoseStack pose = gfx.pose();
        pose.pushPose();

        // --- SHAKE: a smooth, organic rumble (not per-frame random jitter) ----
        // Independent X/Y built from a couple of detuned sine waves plus a faint
        // roll about screen-centre, so it feels like a camera being rattled rather
        // than pixels twitching. Blast decays the amplitude smoothly.
        float shake = 0.0F;
        if (flashTimer > 0) {
            shake = 3.2F * smooth((flashTimer - ft) / 14.0F);
        } else if (wrathTimer > 0) {
            shake = 0.8F + stage * 0.28F + (wrathMax - wrathTimer) * 0.006F;
        }
        if (shake > 0.0F && (stage >= 2 || flashTimer > 0)) {
            float sx = (Mth.sin(rt * 19.0F) + 0.6F * Mth.sin(rt * 31.3F)) * shake;
            float sy = (Mth.cos(rt * 23.0F) + 0.6F * Mth.sin(rt * 28.7F)) * shake;
            float roll = Mth.sin(rt * 13.0F) * shake * 0.12F;
            pose.translate(sx, sy, 0.0F);
            pose.translate(w / 2.0F, h / 2.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.translate(-w / 2.0F, -h / 2.0F, 0.0F);
        }

        // NO red screen overlay. The red belongs to the SKY, not to a pane of glass
        // in front of the camera - see renderBloodSky(). Anything drawn here would
        // sit on top of the very cinematic it is supposed to be dressing.

        // stage 3: brown Pibb rain, continuous streaks with a little sway and depth
        if (stage == 3 && active) {
            RenderSystem.enableBlend();
            for (int i = 0; i < 64; i++) {
                float speed = 90.0F + (i * 37 % 60);
                int sx = (int) (((i * 61 + 17) % w) + Mth.sin(rt * 2.0F + i) * 3.0F);
                int sy = (int) ((rt * speed * 10.0F + i * 43) % (h + 40)) - 40;
                int len = 10 + (i * 13 % 18);
                int a = 0x60 + (i * 29 % 0x50);
                gfx.fill(sx, sy, sx + 2, sy + len, (a << 24) | 0x5C3314);
            }
            RenderSystem.disableBlend();
        }

        // THE FACE: stretched over the ENTIRE screen. Smooth attack/hold/release
        // envelope with a soft flash pulse, a gentle scale "breath", and a chromatic
        // split that grows with intensity - reads as a living jumpscare, not a strobe.
        ResourceLocation face = FACES[stage - 1];
        if (jumpscareTimer > 0) {
            float life = Mth.clamp((jumpscareTimer - ft) / jumpscareMax, 0.0F, 1.0F);  // 1 -> 0
            float elapsed = 1.0F - life;
            float base = smooth(elapsed / 0.12F) * smooth(life / 0.25F);               // snap in, ease out
            float pulseHz = stage <= 2 ? 6.0F : 11.0F;
            float pulse = 0.5F + 0.5F * Mth.sin(rt * pulseHz * 6.2831855F);
            float flash = (stage <= 2 ? 0.40F : 0.20F) + (stage <= 2 ? 0.55F : 0.80F) * pulse;
            float alpha = Mth.clamp(base * flash * (stage <= 2 ? 0.9F : 1.0F), 0.0F, 1.0F);
            drawFace(gfx, face, w, h, alpha, 1.0F + 0.06F * base + 0.015F * Mth.sin(rt * 9.0F),
                    (stage >= 3 ? 3.0F : 1.0F) * base * (0.6F + 0.4F * pulse));
        }

        // stage 10: the face keeps surging back all wrath long, on a smooth cycle
        if (stage >= 10 && wrathTimer > 0 && jumpscareTimer <= 0) {
            float cyc = ((time % 25L) + ft) / 25.0F;               // 0 -> 1 each 25 ticks
            float a = smooth(1.0F - Mth.abs(cyc - 0.12F) / 0.12F) * 0.9F;
            if (a > 0.01F) {
                drawFace(gfx, face, w, h, a, 1.0F + 0.03F * a, 2.0F * a);
            }
        }

        // blast flash - eased fade instead of a linear pop
        if (flashTimer > 0) {
            int a = (int) (smooth((flashTimer - ft) / 14.0F) * 0.9F * 255);
            gfx.fill(0, 0, w, h, (a << 24) | 0xFFF2E6);
        }
        pose.popPose();
    }

    /**
     * Blit the fullscreen face with a scale "breath" about screen-centre and an RGB
     * chromatic split (a red ghost nudged one way, a blue ghost the other, the solid
     * face on top) - cheap, and it turns a flat flash into something that crawls.
     */
    private static void drawFace(GuiGraphics gfx, ResourceLocation face, int w, int h,
                                 float alpha, float scale, float split) {
        RenderSystem.enableBlend();
        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(w / 2.0F, h / 2.0F, 0.0F);
        pose.scale(scale, scale, 1.0F);
        pose.translate(-w / 2.0F, -h / 2.0F, 0.0F);

        int dx = Math.round(split);
        if (dx > 0) {
            gfx.setColor(1.0F, 0.2F, 0.2F, alpha * 0.4F);
            gfx.blit(face, dx, 0, 0, 0.0F, 0.0F, w, h, w, h);
            gfx.setColor(0.3F, 0.4F, 1.0F, alpha * 0.4F);
            gfx.blit(face, -dx, 0, 0, 0.0F, 0.0F, w, h, w, h);
        }
        gfx.setColor(1.0F, 1.0F, 1.0F, alpha);
        gfx.blit(face, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        RenderSystem.disableBlend();
    }

    // ---- the gates of hell: a HORIZONTAL plane high overhead ---------------

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // The blood sky is real geometry drawn right after the vanilla sky and
        // BEFORE terrain, so the world still draws on top of it normally. That is
        // what makes it a sky and not an overlay.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && redLevel > 0.0F) {
            try {
                renderBloodSky(event.getPoseStack());
            } catch (Throwable ignored) {
                // world rendering must never crash
            }
            return;
        }
        // The gates used to be drawn here as a flat overhead plane. They are
        // real articulated geometry now - see HellGateActor, staged by the
        // cinematic director - so there is nothing left for this hook to do
        // beyond the blood sky above.
    }

    /**
     * A blood-red sky shell centred on the camera. Depth writes are off and this
     * runs before terrain, so it paints the heavens without occluding a single
     * block - the ground, the mobs and the cinematic all stay perfectly readable.
     */
    private static void renderBloodSky(PoseStack pose) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float a = Math.min(1.0F, redLevel * 1.15F);
        if (a <= 0.01F) {
            return;
        }
        long t = mc.level.getGameTime();
        float flick = wrathTimer > 0
                ? 0.86F + (float) Math.sin(t * 1.1F) * 0.14F
                : 0.94F + (float) Math.sin(t * 0.22F) * 0.06F;

        // dark clotted crimson overhead, furnace red down at the horizon
        float rTop = 0.26F * flick, gTop = 0.012F, bTop = 0.03F;
        float rHor = 0.86F * flick, gHor = 0.05F,  bHor = 0.04F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        final float R = 120.0F;      // shell radius
        final float TOP = 120.0F;
        final float BOT = -70.0F;    // below the horizon too, so it wraps

        Matrix4f m = pose.last().pose();
        var buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // four walls, bright at the horizon fading dark toward the zenith
        float[][] walls = {
            { -R, -R,  R, -R },   // north
            {  R, -R,  R,  R },   // east
            {  R,  R, -R,  R },   // south
            { -R,  R, -R, -R }    // west
        };
        for (float[] wl : walls) {
            buf.vertex(m, wl[0], BOT, wl[1]).color(rHor, gHor, bHor, a).endVertex();
            buf.vertex(m, wl[2], BOT, wl[3]).color(rHor, gHor, bHor, a).endVertex();
            buf.vertex(m, wl[2], TOP, wl[3]).color(rTop, gTop, bTop, a).endVertex();
            buf.vertex(m, wl[0], TOP, wl[1]).color(rTop, gTop, bTop, a).endVertex();
        }
        // the lid
        buf.vertex(m, -R, TOP, -R).color(rTop, gTop, bTop, a).endVertex();
        buf.vertex(m, -R, TOP,  R).color(rTop, gTop, bTop, a).endVertex();
        buf.vertex(m,  R, TOP,  R).color(rTop, gTop, bTop, a).endVertex();
        buf.vertex(m,  R, TOP, -R).color(rTop, gTop, bTop, a).endVertex();

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

}
