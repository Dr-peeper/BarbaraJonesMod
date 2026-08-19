package com.barbarajones.cinematic;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.cinematic.actor.CinematicActor;
import com.barbarajones.net.PacketApocalypse;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Runs the cinematic system: owns the camera, ticks the scene, draws the actors.
 *
 * <p>This takes over the camera half of the apocalypse client. It deliberately
 * does not touch the fullscreen face, the blood sky or the fog - those stay
 * exactly where they are and composite over this perfectly well, because
 * everything here happens out in the world rather than on a pane of glass in
 * front of the lens.
 *
 * <p>Minecraft will not let a mod move the camera directly, so the rig drives a
 * {@link CameraAnchor} and the camera is pointed at that. Every path out of a
 * running scene goes through {@link #stop()}, which puts the camera back on the
 * player, and a hard countdown ends the show if anything at all goes wrong - a
 * thrown exception, a dimension change, a packet that never arrives. A cutscene
 * that can strand somebody inside a locked camera is worse than no cutscene.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class CinematicDirector {

    /** Floor on the camera leash, in blocks, however low the render distance is. */
    private static final int MIN_LEASH = 48;

    private static final CameraRig.State STATE = new CameraRig.State();
    private static final CameraRig.State FRAME = new CameraRig.State();

    private static Scene scene;
    private static CameraAnchor anchor;
    private static boolean owned;
    private static int activeStage;
    private static int safety;
    private static CameraType restoreView;

    private static int settle;
    private static int settleTotal = 1;
    private static double settleX;
    private static double settleY;
    private static double settleZ;
    private static float settleYaw;
    private static float settlePitch;
    private static float settleRoll;
    private static float settleFov;

    private CinematicDirector() { }

    // ---- entry points ------------------------------------------------------

    /** The single hand-off from the apocalypse packet handler. */
    public static void handle(PacketApocalypse msg) {
        try {
            switch (msg.phase) {
                case PacketApocalypse.PHASE_ONSET, PacketApocalypse.PHASE_WRATH ->
                        begin(msg.stage, new Vec3(msg.x, msg.y, msg.z));
                case PacketApocalypse.PHASE_BLAST -> {
                    if (scene != null) {
                        // The server called an unscheduled detonation. Answer it in
                        // the rig rather than ignore it: a bang with no camera
                        // response reads as a bug, not as restraint.
                        scene.rig().shake(scene.timeline().tick(), 22, 2.2F, 2.2F,
                                scene.origin(), 50.0D);
                    }
                }
                default -> release();
            }
        } catch (Throwable err) {
            stop();
        }
    }

    /** True while the director owns the camera. */
    public static boolean isRunning() {
        return scene != null;
    }

    private static void begin(int stage, Vec3 origin) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // Stages 6+ send ONSET and WRATH on the same server tick, and the second
        // must not restart the beat the first one just opened.
        if (scene != null && activeStage == stage && scene.timeline().tick() < 20) {
            return;
        }
        stop();
        activeStage = stage;
        scene = StageScripts.forStage(stage, origin, mc.player.getEyePosition(1.0F));
        anchor = new CameraAnchor(mc.level);
        safety = scene.timeline().duration() + scene.settleTicks() + 80;
        settle = 0;
        // In third person the camera pulls itself back off its subject and clips
        // against blocks on the way. Every shot here is composed for the exact
        // point the rig puts the lens, so take the view mode for the duration and
        // hand it straight back in stop().
        if (!mc.options.getCameraType().isFirstPerson()) {
            restoreView = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    /** End early but gracefully - the camera still flies home rather than cuts. */
    private static void release() {
        if (scene != null && settle <= 0) {
            startSettle(scene.settleTicks());
        }
    }

    /** Hard stop. Always safe to call from anywhere, however broken things are. */
    private static void stop() {
        scene = null;
        settle = 0;
        safety = 0;
        anchor = null;
        owned = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() != mc.player) {
            mc.setCameraEntity(mc.player);
        }
        if (restoreView != null) {
            mc.options.setCameraType(restoreView);
            restoreView = null;
        }
    }

    // ---- the clock ---------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || (scene == null && settle <= 0)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stop();
            return;
        }
        if (--safety <= 0) {
            stop();
            return;
        }
        try {
            run(mc);
        } catch (Throwable err) {
            stop();
        }
    }

    private static void run(Minecraft mc) {
        // The scene keeps advancing right through the hand-back, so the aftermath
        // - dust falling, cereal bouncing, hair still swinging - plays out behind
        // the departing camera instead of freezing or popping out of existence.
        if (scene != null) {
            scene.advance();
        }
        if (settle > 0) {
            runSettle(mc);
            return;
        }
        if (scene == null) {
            stop();
            return;
        }

        // Pose for the tick we just arrived at: the frames that follow interpolate
        // from the previous pose toward this one, the same contract every entity
        // in the game renders under.
        boolean posed = scene.rig().sample(scene.timeline().tick(), STATE);
        if (posed) {
            clampToPlayer(mc);
            boolean fresh = !owned || mc.getCameraEntity() != anchor;
            if (fresh) {
                mc.setCameraEntity(anchor);
                owned = true;
            }
            placeAnchor(fresh);
        }
        if (scene.finished()) {
            if (posed) {
                startSettle(scene.settleTicks());
            } else {
                stop();
            }
        }
    }

    /**
     * The hand-back. The camera does not cut to the player's eyes, it flies to
     * them while the roll unwinds and the field of view comes home, so the last
     * thing the beat does is as composed as the first.
     */
    private static void startSettle(int ticks) {
        settleX = STATE.x;
        settleY = STATE.y;
        settleZ = STATE.z;
        settleYaw = STATE.yaw;
        settlePitch = STATE.pitch;
        settleRoll = STATE.roll;
        settleFov = STATE.fov;
        settleTotal = Math.max(1, ticks);
        settle = settleTotal;
        safety = Math.max(safety, settleTotal + 20);
    }

    private static void runSettle(Minecraft mc) {
        settle--;
        float e = Easing.CUBIC_IN_OUT.apply(settleProgress(0.0F));
        Vec3 eye = mc.player.getEyePosition(1.0F);
        STATE.x = Mth.lerp((double) e, settleX, eye.x);
        STATE.y = Mth.lerp((double) e, settleY, eye.y);
        STATE.z = Mth.lerp((double) e, settleZ, eye.z);
        // Unwrapped on purpose: the sequence has to stay continuous or the
        // anchor's own rotation interpolation takes the long way round.
        STATE.yaw = settleYaw + Mth.wrapDegrees(mc.player.getYRot() - settleYaw) * e;
        STATE.pitch = Mth.lerp(e, settlePitch, mc.player.getXRot());
        STATE.roll = settleRoll * (1.0F - e);

        if (anchor != null && mc.getCameraEntity() != anchor) {
            mc.setCameraEntity(anchor);
        }
        placeAnchor(false);
        if (settle <= 0) {
            stop();
        }
    }

    /** 0..1 through the hand-back, at fractional time. */
    private static float settleProgress(float partial) {
        return Easing.clamp01(1.0F - (settle - partial) / settleTotal);
    }

    private static void placeAnchor(boolean snap) {
        if (anchor == null) {
            return;
        }
        // The camera sits at the anchor's EYE, so the anchor's feet go one eye
        // height lower. Reading the value back rather than assuming it keeps the
        // cancellation exact whatever the entity's dimensions turn out to be.
        double y = STATE.y - anchor.getEyeHeight();
        if (snap) {
            anchor.snap(STATE.x, y, STATE.z, STATE.yaw, STATE.pitch);
        } else {
            anchor.place(STATE.x, y, STATE.z, STATE.yaw, STATE.pitch);
        }
    }

    /**
     * Chunks are only loaded around the player, so a camera that wandered past
     * the render distance would be staring into unloaded void. Leash it to what
     * is actually loaded rather than to a guessed constant.
     */
    private static void clampToPlayer(Minecraft mc) {
        double leash = Math.max(MIN_LEASH, mc.options.getEffectiveRenderDistance() * 16.0D - 32.0D);
        Vec3 p = mc.player.position();
        double dx = STATE.x - p.x;
        double dy = STATE.y - p.y;
        double dz = STATE.z - p.z;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > leash && d > 0.0001D) {
            double k = leash / d;
            STATE.x = p.x + dx * k;
            STATE.y = p.y + dy * k;
            STATE.z = p.z + dz * k;
        }
    }

    // ---- camera application ------------------------------------------------

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!owned) {
            return;
        }
        if (settle <= 0 && scene != null) {
            // Angles are re-sampled per FRAME rather than lerped from the anchor:
            // shake and roll are high frequency, and smoothing them across a tick
            // visibly softens the punch out of every impact.
            if (scene.rig().sample(scene.time((float) event.getPartialTick()), FRAME)) {
                event.setYaw(FRAME.yaw);
                event.setPitch(FRAME.pitch);
                event.setRoll(FRAME.roll);
            }
            return;
        }
        // Settling: yaw and pitch ride the anchor's own interpolation, which is
        // already smooth, but the roll has to be unwound per frame.
        event.setRoll(settleRoll * (1.0F - Easing.CUBIC_IN_OUT.apply(
                settleProgress((float) event.getPartialTick()))));
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (!owned) {
            return;
        }
        if (settle <= 0 && scene != null) {
            if (scene.rig().sample(scene.time((float) event.getPartialTick()), FRAME)
                    && FRAME.fov > 1.0F) {
                event.setFOV(FRAME.fov);
            }
            return;
        }
        // Blend back toward whatever the game itself wanted, not a hard-coded 70:
        // the player may have their own field of view, a speed effect, a spyglass.
        float e = Easing.CUBIC_IN_OUT.apply(settleProgress((float) event.getPartialTick()));
        event.setFOV(Mth.lerp((double) e, settleFov, event.getFOV()));
    }

    // ---- the world pass ----------------------------------------------------

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // AFTER_WEATHER is the last stage before the transparency chain is
        // composited, which is exactly where additive glow has to land or it gets
        // thrown away under Fabulous graphics.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || scene == null) {
            return;
        }
        try {
            drawActors(event.getPoseStack(), event.getPartialTick());
        } catch (Throwable err) {
            // Rendering a cutscene must never take the game down with it.
            stop();
        }
    }

    private static void drawActors(PoseStack poseStack, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || scene == null) {
            return;
        }
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        float time = scene.time(partial);
        // Hold full opacity for the first part of the hand-back, then dissolve.
        // The giants going out with the shot is deliberate; blinking out of
        // existence the instant the camera lands would undo the whole beat.
        float fade = settle > 0
                ? 1.0F - Easing.CUBIC_IN_OUT.apply(
                        Easing.clamp01((settleProgress(partial) - 0.45F) / 0.55F))
                : 1.0F;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (CinematicActor actor : scene.actors()) {
            actor.opacity = fade;
            actor.render(poseStack, buffers, camPos, time);
        }
        CinematicActor.flush(buffers);
    }

    // ---- safety net --------------------------------------------------------

    @SubscribeEvent
    public static void onRenderTickGuard(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !owned) {
            return;
        }
        // The camera entity can be taken away underneath us by a respawn, a
        // dimension change or another mod. If it is not ours any more, neither is
        // the show.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getCameraEntity() != anchor) {
            stop();
        }
    }
}
