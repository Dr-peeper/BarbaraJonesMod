package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.net.PacketApocalypse;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
    private static final ResourceLocation GATE_VOID =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/hellgate_void.png");
    private static final ResourceLocation GATE_DOOR =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/hellgate_door.png");

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
        stage = Math.max(1, Math.min(10, msg.stage));
        switch (msg.phase) {
            case PacketApocalypse.PHASE_ONSET -> {
                active = true;
                jumpscareMax = stage <= 2 ? 30 : 44 + stage * 4;
                jumpscareTimer = jumpscareMax;
                deathX = msg.x; deathY = msg.y; deathZ = msg.z;
                safetyTimer = SAFETY_MAX;
                if (stage >= 2 && stage <= 5) {
                    cinLockTimer = 10;
                }
            }
            case PacketApocalypse.PHASE_WRATH -> {
                active = true;
                wrathMax = 200 + stage * 30;
                wrathTimer = wrathMax;
                deathX = msg.x; deathY = msg.y; deathZ = msg.z;
                safetyTimer = SAFETY_MAX;
                cinLockTimer = 10;
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
        // one quick swing onto the show, then the head is free again
        if (cinLockTimer > 0 && mc.player != null) {
            cinLockTimer--;
            double ty = deathY + (stage >= 6 ? 55.0D : 24.0D);
            double dx = deathX - mc.player.getX();
            double dy = ty - (mc.player.getY() + 1.6D);
            double dz = deathZ - mc.player.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float wantYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.001D, horiz)));
            float dYaw = wantYaw - mc.player.getYRot();
            while (dYaw < -180.0F) { dYaw += 360.0F; }
            while (dYaw > 180.0F) { dYaw -= 360.0F; }
            mc.player.setYRot(mc.player.getYRot() + dYaw * 0.45F);
            mc.player.setXRot(mc.player.getXRot() + (wantPitch - mc.player.getXRot()) * 0.45F);
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
        float rnd = mc.level != null ? mc.level.random.nextFloat() : 0.5F;

        PoseStack pose = gfx.pose();
        pose.pushPose();

        // shake (the face itself FLASHES instead of shaking)
        float shake = 0.0F;
        if (flashTimer > 0) {
            shake = 4.0F;
        } else if (wrathTimer > 0) {
            shake = 1.0F + stage * 0.4F + (wrathMax - wrathTimer) * 0.008F;
        }
        if (shake > 0.0F && stage >= 2) {
            pose.translate((rnd - 0.5F) * shake, (rnd - 0.5F) * shake, 0.0F);
        }

        // NO red screen overlay. The red belongs to the SKY, not to a pane of glass
        // in front of the camera - see renderBloodSky(). Anything drawn here would
        // sit on top of the very cinematic it is supposed to be dressing.

        // stage 3: brown Pibb rain streaking down the screen
        if (stage == 3 && active) {
            for (int i = 0; i < 50; i++) {
                int sx = (i * 61 + 17) % w;
                int sy = (int) ((time * 9 + i * 43) % (h + 30)) - 30;
                gfx.fill(sx, sy, sx + 2, sy + 14, 0x8C5C3314);
            }
        }

        // THE FACE: stretched over the ENTIRE screen, flashing hard on/off
        ResourceLocation face = FACES[stage - 1];
        if (jumpscareTimer > 0) {
            boolean on = stage <= 2 ? (jumpscareTimer % 14) < 9 : (jumpscareTimer % 6) < 3;
            if (on) {
                RenderSystem.enableBlend();
                gfx.setColor(1.0F, 1.0F, 1.0F, stage <= 2 ? 0.85F : 1.0F);
                gfx.blit(face, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
                gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
            }
        }

        // stage 10: the face keeps flashing back all wrath long
        if (stage >= 10 && wrathTimer > 0 && jumpscareTimer <= 0 && (time % 25L) < 3L) {
            RenderSystem.enableBlend();
            gfx.setColor(1.0F, 1.0F, 1.0F, 0.9F);
            gfx.blit(face, 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
            gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }

        // blast flash
        if (flashTimer > 0) {
            int a = (int) ((flashTimer / 14.0F) * 0.85F * 255);
            gfx.fill(0, 0, w, h, (a << 24) | 0xFFF2E6);
        }
        pose.popPose();
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER
                || wrathTimer <= 0 || stage < 6) {
            return;
        }
        try {
            renderGates(event.getPoseStack(), event.getPartialTick());
        } catch (Throwable ignored) {
            // world rendering must never crash
        }
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

    private static void renderGates(PoseStack pose, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();
        if (cam == null || mc.level == null) {
            return;
        }
        var camPos = mc.gameRenderer.getMainCamera().getPosition();

        double gy = deathY + 80.0D;
        float H = 50.0F + stage * 8.0F;
        int elapsed = wrathMax - wrathTimer;
        float open = Math.min(1.0F, elapsed / 45.0F);
        if (wrathTimer < 35) {
            open = Math.min(open, wrathTimer / 35.0F);
        }
        float appear = Math.min(1.0F, elapsed / 12.0F);
        long time = mc.level.getGameTime();

        pose.pushPose();
        pose.translate(deathX - camPos.x, gy - camPos.y, deathZ - camPos.z);

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        // 1) churning hellfire void, additive, just above the doors
        RenderSystem.setShaderColor(1.0F, 0.55F, 0.2F, appear);
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, GATE_VOID);
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        float vs = (time % 220L) / 220.0F;
        quadXZ(pose, -H, -H, H, H, 0.6F, 0.0F, vs, 1.0F, 1.0F + vs);

        // 2) the two gate doors, flat, sliding apart along X
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
        RenderSystem.setShaderTexture(0, GATE_DOOR);
        float slide = open * H * 1.05F;
        quadXZ(pose, -H - slide, -H, -slide, H, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        quadXZ(pose, slide, -H, H + slide, H, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    /** A textured HORIZONTAL quad (constant local y). */
    private static void quadXZ(PoseStack pose, float x0, float z0, float x1, float z1, float y,
                               float u0, float v0, float u1, float v1) {
        Matrix4f m = pose.last().pose();
        var buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(m, x0, y, z0).uv(u0, v0).endVertex();
        buf.vertex(m, x0, y, z1).uv(u0, v1).endVertex();
        buf.vertex(m, x1, y, z1).uv(u1, v1).endVertex();
        buf.vertex(m, x1, y, z0).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(buf.end());
    }
}
