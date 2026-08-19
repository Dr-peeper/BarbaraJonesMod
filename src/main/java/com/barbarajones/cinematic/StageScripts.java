package com.barbarajones.cinematic;

import com.barbarajones.cinematic.actor.BluntActor;
import com.barbarajones.cinematic.actor.HellGateActor;
import com.barbarajones.cinematic.actor.BoxTitanActor;
import com.barbarajones.cinematic.actor.C4CableActor;
import com.barbarajones.cinematic.actor.CleaverActor;
import com.barbarajones.cinematic.actor.GrassBladeActor;
import com.barbarajones.cinematic.actor.InternetManagerActor;
import com.barbarajones.cinematic.actor.ManagerActor;
import com.barbarajones.cinematic.actor.SkyTelevisionActor;
import com.barbarajones.cinematic.actor.SmokerActor;
import com.barbarajones.cinematic.actor.ThrowerActor;
import com.barbarajones.cinematic.actor.TorchActor;
import com.barbarajones.content.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

/**
 * The authored death stages.
 *
 * <p>Every script here is written as three movements, in this order, because a
 * beat that skips any of them does not land:
 *
 * <ol>
 *   <li><b>Anticipation.</b> Something moves the wrong way first. The carton
 *       rises before it falls, the trigger cocks backwards before it fires, the
 *       Manager leans away before his head comes down. The audience does not know
 *       what is coming, but the shot has already told them something is.</li>
 *   <li><b>Impact.</b> One frame where everything arrives together: the curve
 *       overshoots its target, the camera drops, the shake fires from the exact
 *       point of contact, the sound hits.</li>
 *   <li><b>Aftermath.</b> Nothing stops dead. Flaps swing past and settle, dust
 *       is thrown up and dragged back down, the camera springs and rings out, and
 *       only then does the shot open up and hand the view back.</li>
 * </ol>
 *
 * <p><b>One script per stage, ten of them.</b> This used to bucket ten stages
 * into four scripts, which meant stages 1 and 2 were the same footage, and 3, 4
 * and 5 were the same footage again. Dying twice in a row played the identical
 * film, so the beat read as "the camera took me somewhere and nothing happened".
 * Every stage now has its own cast, its own camera language and its own kind of
 * violence, and consecutive stages deliberately alternate between dread and
 * chaos so that no two in a row even move at the same speed.
 *
 * <p>Actor position tracks are named {@code dx/dy/dz} and are OFFSETS from where
 * the actor was bound, never absolute world coordinates. Tracks are float, and a
 * world coordinate out at a few million blocks has lost enough precision by then
 * to make a giant visibly stutter.
 *
 * <p>The other rule that is easy to break and impossible to see in code: a prop
 * only exists if the camera is pointed at it. Every {@code rig.dolly} and
 * {@code rig.orbit} below is authored against the world position of the thing it
 * is meant to be showing, computed from the same constants the actor is bound
 * with, so moving an actor moves the shot that covers it.
 */
public final class StageScripts {

    private StageScripts() { }

    /** Pick the script for this death stage. */
    public static Scene forStage(int stage, Vec3 origin, Vec3 playerEye) {
        // Clamped rather than trusted: a stage of 0 arriving from a desynced
        // packet must open on the smallest beat in the show, not the largest.
        return switch (Math.max(1, Math.min(10, stage))) {
            case 1 -> theCraving(origin, playerEye);
            case 2 -> theOs(origin, playerEye);
            case 3 -> theDicing(origin, playerEye);
            case 4 -> theCleaving(origin, playerEye);
            case 5 -> theTorching(origin, playerEye);
            case 6 -> boxfall(origin, playerEye);
            case 7 -> ashfall(origin, playerEye);
            case 8 -> theBarrage(origin, playerEye);   // theInterview() is still unwritten - the agent that owned it was stopped
            case 9 -> theManager(origin, playerEye);
            default -> armageddon(origin, playerEye);
        };
    }

    // ------------------------------------------------------------------------
    // STAGE 1: THE CRAVING
    // One giant, one drag, ONE ring - and the camera climbs into its path so the
    // O swallows the lens. Smallest cast in the show: everything rides on timing,
    // and nothing here is allowed to appear in stage 2 the same way twice.
    // ------------------------------------------------------------------------

    private static Scene theCraving(Vec3 o, Vec3 eye) {
        final int end = 140;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 barbara = o.add(0.0D, -2.0D, 34.0D);
        Vec3 head = barbara.add(0.0D, 28.0D, 0.0D);
        // Where the O will be when it is wide enough to fly through: out along her
        // facing and risen, exactly as SmokerActor drifts a ring off her mouth.
        Vec3 inTheRing = o.add(0.0D, 38.0D, 8.0D);

        // --- camera ---------------------------------------------------------
        // Push in and crane onto her while she is still rising, so the move and
        // the reveal happen together and neither has to carry the shot alone.
        rig.dolly(0, 44, 0, eye, o.add(-2.0D, 5.0D, -12.0D), o.add(0.0D, 2.0D, 0.0D), head,
                Easing.CUBIC_IN_OUT);
        // The snap: twelve ticks, EXPO_OUT, straight at her face.
        rig.dolly(44, 12, 5, o.add(-2.0D, 5.0D, -12.0D), o.add(-1.0D, 7.0D, -4.0D), head,
                Easing.EXPO_OUT);
        // Then climb onto the ring's own axis. The lens arrives a beat before the
        // O does, which is the only way a ring can be flown through rather than
        // watched: get there first and let the thing come to you.
        rig.dolly(56, 30, 10, o.add(-1.0D, 7.0D, -4.0D), inTheRing, head, Easing.QUINT_OUT);
        rig.hold(86, 12, 6, inTheRing, head);
        rig.dolly(98, 24, 10, inTheRing, o.add(-15.0D, 30.0D, -18.0D), head,
                o.add(0.0D, 6.0D, 0.0D), Easing.CUBIC_IN_OUT);
        rig.dolly(122, 18, 10, o.add(-15.0D, 30.0D, -18.0D), o.add(-27.0D, 21.0D, -36.0D),
                o.add(0.0D, 4.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(56.0F, -3.6F, Easing.BACK_OUT)
                .key(88.0F, 2.4F, Easing.SINE_IN_OUT)
                .key(end, 0.0F, Easing.SPRING_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(44.0F, 66.0F)
                .key(52.0F, 54.0F, Easing.EXPO_OUT)
                .key(64.0F, 78.0F, Easing.BACK_OUT)
                // Widens again as the ring closes: the walls of the O sliding past
                // the edges of the frame is the whole trick.
                .key(88.0F, 92.0F, Easing.CUBIC_IN_OUT)
                .key(104.0F, 70.0F, Easing.SPRING_OUT)
                .hold(end));

        rig.shake(48, 16, 0.55F, 2.1F, head, 60.0D);
        rig.shake(86, 20, 0.42F, 1.6F, null, 0.0D);

        // --- the giant ------------------------------------------------------
        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", barbara, 180.0F, 13.0F);

        tl.track("barb.dy", Track.from(0.0F, -34.0F)
                .hold(8.0F)
                // QUINT_OUT: she comes up fast and settles under her own mass.
                .key(44.0F, 0.0F, Easing.QUINT_OUT)
                .hold(104.0F)
                .key(end, -30.0F, Easing.CUBIC_IN));

        // Head bowed, then the snap up with an overshoot that springs back.
        tl.track("barb.headPitch", Track.from(0.0F, 34.0F)
                .hold(40.0F)
                .key(48.0F, -17.0F, Easing.BACK_OUT)
                .key(58.0F, -6.0F, Easing.SPRING_OUT)
                .hold(104.0F)
                .key(132.0F, 28.0F));
        tl.track("barb.spine", Track.from(0.0F, 9.0F)
                .hold(40.0F)
                .key(48.0F, -7.0F, Easing.BACK_OUT)
                .key(58.0F, 0.0F, Easing.SPRING_OUT)
                .hold(106.0F)
                .key(end, 10.0F));

        // The joint reaches her mouth before anything else happens - that is the
        // tell that a drag is coming.
        tl.track("barb.armR", Track.from(0.0F, 6.0F)
                .key(24.0F, 2.0F)
                .key(42.0F, -95.0F, Easing.BACK_OUT)
                .hold(84.0F)
                .key(104.0F, -18.0F)
                .key(end, 6.0F));
        tl.track("barb.elbowR", Track.from(0.0F, 8.0F)
                .key(24.0F, 14.0F)
                .key(42.0F, 120.0F, Easing.BACK_OUT)
                .hold(84.0F)
                .key(104.0F, 30.0F)
                .key(end, 8.0F));
        tl.track("barb.armL", Track.from(0.0F, 5.0F).key(48.0F, 16.0F).key(end, 4.0F));

        // Jaw shut for the drag, then ONE long open on the exhale. Stage 2 is the
        // three-ring version of this beat; holding it to a single O here is what
        // keeps the two apart.
        tl.track("barb.jaw", Track.from(0.0F, 0.0F)
                .hold(46.0F)
                .key(50.0F, 3.0F)
                .key(56.0F, 36.0F, Easing.ELASTIC_OUT)
                .hold(78.0F)
                .key(96.0F, 6.0F)
                .key(112.0F, 0.0F));

        tl.track("barb.ember", Track.from(0.0F, 0.25F)
                .hold(40.0F)
                .key(50.0F, 1.5F, Easing.EXPO_OUT)
                .key(62.0F, 0.5F)
                .key(96.0F, 0.28F)
                .key(end, 0.15F));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.028F, 47.0F, 0.0F, 0.0F));
        tl.track("barb.bob", Track.constant(0.0F).wobble(0.02F, 63.0F, 0.3F, 0.0F));

        // 0 the moment it leaves her, 1 when it is spent.
        tl.track("barb.ring0", Track.from(56.0F, 0.0F).key(112.0F, 1.0F, Easing.LINEAR));

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 0.9F, 0.85F));
        tl.cue(40, sound(head, ModSounds.KRAVE_VOICE, 1.1F, 0.95F));
        tl.cue(48, sound(head, ModSounds.KRAVE_SCREECH, 0.9F, 1.05F));
        tl.cue(56, sound(head, ModSounds.EVT_OG, 1.3F, 1.0F));
        tl.cue(56, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                48, 5.0D, 0.35D));
        tl.cue(88, sound(inTheRing, ModSounds.KRAVE_RUMBLE, 1.4F, 1.3F));
        tl.cue(112, sound(o, ModSounds.KRAVE_LAUGH, 1.0F, 0.9F));

        return new Scene("The Craving", o, tl, rig, 26).add(she);
    }

    // ------------------------------------------------------------------------
    // STAGE 2: THE O'S
    // She is already standing when the shot opens - no rise, because stage 1
    // spent its whole first act on one. The camera starts ABOVE her, comes down
    // over her shoulder, and three rings are blown in a rhythm while a second
    // giant stands out on the horizon doing the same thing back.
    // ------------------------------------------------------------------------

    private static Scene theOs(Vec3 o, Vec3 eye) {
        final int end = 178;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 barbara = o.add(-6.0D, -3.0D, 46.0D);
        float yawB = facing(barbara, o);
        Vec3 head = barbara.add(0.0D, 31.0D, 0.0D);
        // The rings leave her mouth and drift along her facing, rising as they go.
        Vec3 inTheRing = local(barbara, yawB, 1.5D, 44.0D, -29.0D);
        Vec3 farAt = o.add(96.0D, -10.0D, -104.0D);

        // --- camera ---------------------------------------------------------
        // Opens looking DOWN on her from above and behind: stage 1 met her at
        // ground level, so this one has to arrive from somewhere else entirely.
        rig.dolly(0, 46, 0, eye, o.add(26.0D, 52.0D, 34.0D), o.add(0.0D, 4.0D, 0.0D), head,
                Easing.CUBIC_IN_OUT);
        // Swoop down and around onto her face while she is already lifting the
        // joint, so the descent and the drag finish on the same tick.
        rig.orbit(46, 46, 12, barbara, 66.0D, 42.0D, 36.0D, 28.0D, 76.0F, -34.0F, head,
                Easing.CUBIC_IN_OUT);
        // Get out onto the line the rings travel, ahead of the second one.
        rig.dolly(92, 34, 12, ring(barbara, -34.0F, 42.0D, 28.0D), inTheRing, head,
                Easing.QUINT_OUT);
        rig.hold(126, 16, 8, inTheRing, head);
        // Peel off toward the far giant: the reveal that there is more than one of
        // her is the payoff, so it gets its own move rather than a background pass.
        rig.dolly(142, 36, 12, inTheRing, o.add(34.0D, 40.0D, -28.0D), head,
                farAt.add(0.0D, 30.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(46.0F, 4.2F, Easing.SINE_IN_OUT)
                .key(92.0F, -2.6F, Easing.BACK_OUT)
                .key(128.0F, 1.8F, Easing.SINE_IN_OUT)
                .key(end, 0.0F, Easing.SPRING_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(46.0F, 62.0F, Easing.CUBIC_IN_OUT)
                .key(84.0F, 58.0F)
                .key(90.0F, 80.0F, Easing.EXPO_OUT)
                .key(126.0F, 96.0F, Easing.CUBIC_IN_OUT)
                .key(146.0F, 68.0F, Easing.SPRING_OUT)
                .hold(end));

        rig.shake(84, 18, 0.60F, 2.0F, head, 55.0D);
        rig.shake(126, 22, 0.45F, 1.6F, null, 0.0D);
        rig.shake(152, 26, 0.30F, 1.5F, null, 0.0D);

        // --- the giant ------------------------------------------------------
        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", barbara, yawB, 14.0F);

        // Already up. She only settles her weight, which reads as somebody who has
        // been standing there the whole time you were dying.
        tl.track("barb.dy", Track.from(0.0F, 1.6F)
                .key(30.0F, 0.0F, Easing.SPRING_OUT)
                .hold(150.0F)
                .key(end, -32.0F, Easing.CUBIC_IN));
        tl.track("barb.headPitch", Track.from(0.0F, 12.0F)
                .key(44.0F, -12.0F, Easing.BACK_OUT)
                .key(56.0F, -3.0F, Easing.SPRING_OUT)
                .hold(140.0F)
                .key(end, 24.0F));
        tl.track("barb.headYaw", Track.from(0.0F, -22.0F)
                .key(52.0F, 0.0F, Easing.CUBIC_IN_OUT)
                .hold(140.0F)
                // She turns to look at the other one before the camera does.
                .key(158.0F, 34.0F, Easing.BACK_OUT)
                .hold(end));
        tl.track("barb.spine", Track.from(0.0F, 4.0F).key(56.0F, -2.0F).hold(end));
        tl.track("barb.armR", Track.from(0.0F, 6.0F)
                .key(20.0F, 2.0F)
                .key(48.0F, -95.0F, Easing.BACK_OUT)
                .hold(120.0F)
                .key(150.0F, -14.0F)
                .key(end, 6.0F));
        tl.track("barb.elbowR", Track.from(0.0F, 8.0F)
                .key(20.0F, 14.0F)
                .key(48.0F, 120.0F, Easing.BACK_OUT)
                .hold(120.0F)
                .key(150.0F, 28.0F)
                .key(end, 8.0F));
        tl.track("barb.armL", Track.from(0.0F, 4.0F).key(60.0F, 18.0F).key(end, 4.0F));

        // Three pulses, and the gaps between them shrink - a rhythm, not a loop.
        tl.track("barb.jaw", Track.from(0.0F, 0.0F)
                .hold(56.0F)
                .key(60.0F, 4.0F)
                .key(66.0F, 34.0F, Easing.ELASTIC_OUT)
                .key(76.0F, 12.0F)
                .key(82.0F, 32.0F, Easing.ELASTIC_OUT)
                .key(90.0F, 10.0F)
                .key(96.0F, 30.0F, Easing.ELASTIC_OUT)
                .key(112.0F, 0.0F)
                .hold(end));
        tl.track("barb.ember", Track.from(0.0F, 0.3F)
                .hold(48.0F)
                .key(58.0F, 1.5F, Easing.EXPO_OUT)
                .key(68.0F, 0.55F)
                .key(80.0F, 1.2F)
                .key(90.0F, 0.5F)
                .key(96.0F, 1.1F)
                .key(120.0F, 0.35F)
                .key(end, 0.2F));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.03F, 45.0F, 0.0F, 0.0F));
        tl.track("barb.bob", Track.constant(0.0F).wobble(0.025F, 59.0F, 0.4F, 0.0F));
        tl.track("barb.ring0", Track.from(66.0F, 0.0F).key(126.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring1", Track.from(82.0F, 0.0F).key(142.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring2", Track.from(96.0F, 0.0F).key(156.0F, 1.0F, Easing.LINEAR));

        // --- the one on the horizon -----------------------------------------
        // Bigger than the near one and a hundred and forty blocks out. Something
        // that large only reads as large next to a second copy of itself.
        SmokerActor far = new SmokerActor();
        far.bind(tl, "far", farAt, facing(farAt, o), 19.0F);
        tl.track("far.dy", Track.from(0.0F, -46.0F)
                .hold(96.0F)
                .key(150.0F, 0.0F, Easing.QUINT_OUT)
                .hold(end));
        tl.track("far.headPitch", Track.from(0.0F, 28.0F)
                .key(154.0F, -10.0F, Easing.BACK_OUT)
                .key(166.0F, -3.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("far.armR", Track.from(0.0F, 6.0F)
                .key(158.0F, -95.0F, Easing.BACK_OUT).hold(end));
        tl.track("far.elbowR", Track.from(0.0F, 8.0F)
                .key(158.0F, 120.0F, Easing.BACK_OUT).hold(end));
        tl.track("far.ember", Track.from(0.0F, 0.0F)
                .hold(150.0F)
                .key(166.0F, 1.4F, Easing.EXPO_OUT)
                .hold(end));
        tl.track("far.breathe", Track.constant(1.0F).wobble(0.03F, 53.0F, 0.0F, 0.0F));

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_SIREN, 1.0F, 1.15F));
        tl.cue(30, sound(head, ModSounds.KRAVE_RUMBLE, 1.0F, 0.8F));
        tl.cue(48, sound(head, ModSounds.KRAVE_VOICE, 1.1F, 1.0F));
        tl.cue(66, sound(head, ModSounds.EVT_OG, 1.3F, 1.0F));
        tl.cue(66, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                40, 5.0D, 0.35D));
        tl.cue(82, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                40, 5.0D, 0.35D));
        tl.cue(96, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                40, 5.0D, 0.35D));
        tl.cue(126, sound(inTheRing, ModSounds.KRAVE_SCREECH, 1.1F, 1.25F));
        tl.cue(150, sound(farAt, ModSounds.KRAVE_BOOM, 2.2F, 0.6F));
        tl.cue(164, sound(farAt, ModSounds.KRAVE_LAUGH, 1.3F, 0.85F));

        return new Scene("The O's", o, tl, rig, 28).add(she).add(far);
    }

    // ------------------------------------------------------------------------
    // STAGE 3: DICED
    // Barbara blinks around the sky and throws literal blades of grass down like
    // javelins. Eight hops, and the gap between them shrinks by two ticks every
    // time, so the beat accelerates without a single number being tuned by hand.
    // The targets spiral INWARD: the last blade lands five blocks from where the
    // pet died, which is the only line of dialogue this scene has.
    // ------------------------------------------------------------------------

    private static Scene theDicing(Vec3 o, Vec3 eye) {
        final int end = 288;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        // Tick she vanishes, ticks the wind-up runs for, ticks the blade is in
        // the air. All three shorten together; nothing else drives the pacing.
        final int[] hop = { 4, 40, 74, 106, 136, 164, 190, 214 };
        final int[] wind = { 34, 32, 30, 28, 26, 24, 22, 20 };
        final int[] flight = { 26, 24, 22, 20, 18, 16, 14, 12 };

        // Where she stands, as an angle and radius around the death point. Never
        // twice in the same quadrant: consecutive hops cross the whole sky so the
        // afterimage trail has somewhere to be drawn.
        final float[] standAt = { 24.0F, 214.0F, 92.0F, 300.0F, 148.0F, 8.0F, 250.0F, 120.0F };
        final double[] standOut = { 86.0D, 78.0D, 92.0D, 70.0D, 80.0D, 64.0D, 74.0D, 56.0D };
        final double[] standUp = { 58.0D, 72.0D, 46.0D, 78.0D, 54.0D, 68.0D, 42.0D, 62.0D };
        final float[] landAt = { 200.0F, 34.0F, 274.0F, 118.0F, 330.0F, 190.0F, 60.0F, 0.0F };
        final double[] landIn = { 34.0D, 30.0D, 27.0D, 24.0D, 21.0D, 17.0D, 12.0D, 5.0D };
        final float[] bladeLen = { 22.0F, 24.0F, 26.0F, 28.0F, 31.0F, 34.0F, 38.0F, 46.0F };

        Vec3 first = ring(o, standAt[0], standOut[0], standUp[0]);
        Vec3 lastLand = ring(o, landAt[7], landIn[7], 0.0D);

        ThrowerActor barb = new ThrowerActor();
        // Bound with yaw 0 so the "aim" track IS the world bearing onto whatever
        // she is throwing at, with nothing to subtract.
        barb.bind(tl, "thr", first, 0.0F, 12.0F);

        // She comes in from off the edge of the world on the first hop, so the
        // opening blink is a real jump and the trail is there from the first frame.
        Vec3 wings = ring(o, standAt[0] + 46.0F, standOut[0] + 96.0D, standUp[0] + 34.0D);
        Track dx = Track.from(0.0F, (float) (wings.x - first.x));
        Track dy = Track.from(0.0F, (float) (wings.y - first.y));
        Track dz = Track.from(0.0F, (float) (wings.z - first.z));
        // Starts fully gone: the first thing she does is arrive.
        Track blink = Track.from(0.0F, 1.0F);
        Track ghost = Track.from(0.0F, 0.0F);
        Track aim = Track.from(0.0F, facing(wings, o));
        // Below zero means the throw is unauthored and her limbs stay on the
        // humanoid tracks - the same contract the walk cycle uses.
        Track hurl = Track.from(0.0F, -1.0F);

        Scene scene = new Scene("Diced", o, tl, rig, 32).add(barb);

        for (int i = 0; i < hop.length; i++) {
            Vec3 stand = ring(o, standAt[i], standOut[i], standUp[i]);
            Vec3 land = ring(o, landAt[i], landIn[i], 0.0D);
            // Her hand, not her feet: a blade that starts at the soles of a giant
            // leaves from twenty-four blocks below where you watched her let go.
            Vec3 hand = stand.add(0.0D, 24.0D, 0.0D);

            int h = hop[i];
            int coil = h + 6;
            // ThrowerActor releases at 0.58-0.60 of its own track. Everything that
            // has to happen on the release is hung off this one number.
            int release = coil + Math.round(wind[i] * 0.585F);
            int hit = release + flight[i];

            // The jump itself. Held, then STEPPED: a position eased over several
            // ticks reads as flying, and the whole point is that she does not fly.
            dx.hold(h + 3.0F).key(h + 4.0F, (float) (stand.x - first.x), Easing.STEP);
            dy.hold(h + 3.0F).key(h + 4.0F, (float) (stand.y - first.y), Easing.STEP);
            dz.hold(h + 3.0F).key(h + 4.0F, (float) (stand.z - first.z), Easing.STEP);
            // Six ticks of blink, three of them fully gone. Any slower and it is
            // a dissolve; any faster and the eye never registers she left.
            blink.hold(h + 1.0F)
                    .key(h + 3.0F, 1.0F, Easing.CUBIC_IN)
                    .key(h + 5.0F, 1.0F, Easing.LINEAR)
                    .key(h + 9.0F, 0.0F, Easing.CUBIC_OUT);
            // The trail is what carries the eye across the gap while she is not
            // anywhere. It peaks ON the jump and decays long after she has landed.
            ghost.hold(h + 1.0F)
                    .key(h + 4.0F, 1.0F, Easing.EXPO_OUT)
                    .key(h + 16.0F, 0.0F, Easing.CUBIC_OUT);
            // Turned onto the new target while she is invisible, so she never
            // visibly swivels - she is simply already facing it when she returns.
            aim.hold(h + 3.0F).key(h + 4.0F, facing(stand, land), Easing.STEP);
            // LINEAR on purpose: the coil, the snap and the follow-through are
            // already baked into ThrowerActor's pose, and easing this would ease
            // them twice.
            hurl.hold(coil - 1.0F)
                    .key(coil, 0.0F, Easing.STEP)
                    .key(coil + wind[i], 1.0F, Easing.LINEAR);

            String id = "blade" + i;
            GrassBladeActor blade = new GrassBladeActor(bladeLen[i]);
            // Bound at the point it will bury itself in, so "hits the ground" is
            // the track reaching zero rather than arithmetic against a length.
            blade.bind(tl, id, land, facing(land, stand), 1.0F);
            spear(tl, id, hand, land, coil, release, hit, end,
                    3 + i % 3, (i % 2 == 0 ? 1.0F : -1.0F) * (9.0F + (i % 3) * 8.0F));
            scene.add(blade);

            // The release: her shout, the earth still coming off it, and the shake
            // fired from HER position rather than the ground, because at this
            // point in the beat nothing has landed yet.
            tl.cue(release, sound(hand, ModSounds.KRAVE_SCREECH, 0.7F + i * 0.06F,
                    1.35F - i * 0.035F));
            tl.cue(release, burst(hand, ParticleTypes.LARGE_SMOKE, 16, 3.2D, 0.45D));
            rig.shake(release, 10, 0.5F + i * 0.05F, 2.6F, hand, 70.0D);
            tl.cue(h, sound(stand, ModSounds.KRAVE_VOICE, 0.5F, 1.55F));

            // And the landing, growing every time.
            impact(tl, rig, land, hit, 0.8F + i * 0.22F);
        }

        tl.track("thr.dx", dx.hold(end));
        tl.track("thr.dy", dy.hold(end));
        tl.track("thr.dz", dz.hold(end));
        tl.track("thr.blink", blink.hold(end));
        tl.track("thr.ghost", ghost.hold(end));
        tl.track("thr.aim", aim.hold(end));
        tl.track("thr.throw", hurl.hold(end));
        // The throw ADDS to lean, spine, headPitch and headYaw, so these bias the
        // whole action without fighting it: she is looking down at the world the
        // entire time, and leaning further over it as the scene accelerates.
        tl.track("thr.headPitch", Track.from(0.0F, 8.0F)
                .key(60.0F, 18.0F)
                .key(200.0F, 27.0F)
                .hold(end));
        tl.track("thr.lean", Track.from(0.0F, 0.0F)
                .key(120.0F, 5.0F)
                .key(230.0F, 11.0F)
                .hold(end));
        tl.track("thr.breathe", Track.constant(1.0F).wobble(0.03F, 49.0F, 0.0F, 0.0F));
        tl.track("thr.bob", Track.constant(0.0F).wobble(0.02F, 41.0F, 0.2F, 0.0F));

        // --- camera ---------------------------------------------------------
        // Ride the FIRST throw all the way down, so the audience is taught what
        // the rest of the scene is before it starts happening three times a second.
        rig.dolly(0, 30, 0, eye, o.add(-30.0D, 14.0D, -46.0D), o.add(0.0D, 3.0D, 0.0D),
                first.add(0.0D, 22.0D, 0.0D), Easing.CUBIC_IN_OUT);
        rig.dolly(30, 28, 8, o.add(-30.0D, 14.0D, -46.0D), o.add(-26.0D, 24.0D, -40.0D),
                first.add(0.0D, 22.0D, 0.0D), ring(o, landAt[0], landIn[0], 3.0D),
                Easing.CUBIC_IN_OUT);
        // Then pull wide and stay wide: from here on she is somewhere else every
        // second, and a camera that chases her would lose both her and the ground.
        rig.dolly(58, 34, 10, o.add(-26.0D, 24.0D, -40.0D), o.add(-48.0D, 34.0D, -22.0D),
                ring(o, landAt[0], landIn[0], 3.0D), o.add(0.0D, 16.0D, 0.0D),
                Easing.CUBIC_IN_OUT);
        // Two orbits, each tighter and lower than the last, so the field of blades
        // closes in on the lens at the same rate her targets close in on the pet.
        rig.orbit(96, 64, 14, o, 74.0D, 54.0D, 34.0D, 22.0D, 214.0F, 336.0F,
                o.add(0.0D, 16.0D, 0.0D), Easing.SINE_IN_OUT);
        rig.orbit(160, 46, 12, o, 54.0D, 40.0D, 22.0D, 15.0D, 336.0F, 452.0F,
                o.add(0.0D, 12.0D, 0.0D), Easing.SINE_IN_OUT);
        // Park on the last target well before the last blade is thrown. The shot
        // knows where it is going to land before she does.
        rig.dolly(206, 32, 10, ring(o, 452.0F, 40.0D, 15.0D), o.add(-4.0D, 9.0D, -34.0D),
                o.add(0.0D, 10.0D, 0.0D), lastLand.add(0.0D, 6.0D, 0.0D), Easing.CUBIC_IN_OUT);
        // The plunge drags the camera down with it...
        rig.dolly(240, 5, 2, o.add(-4.0D, 9.0D, -34.0D), o.add(-4.0D, 4.5D, -32.0D),
                lastLand.add(0.0D, 5.0D, 0.0D), Easing.QUINT_IN);
        // ...and the spring back is what puts the weight in.
        rig.dolly(245, 28, 8, o.add(-4.0D, 4.5D, -32.0D), o.add(-9.0D, 13.0D, -42.0D),
                lastLand.add(0.0D, 5.0D, 0.0D), Easing.SPRING_OUT);
        rig.dolly(273, 15, 10, o.add(-9.0D, 13.0D, -42.0D), o.add(-26.0D, 27.0D, -60.0D),
                o.add(0.0D, 8.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(56.0F, -3.0F, Easing.BACK_OUT)
                .key(90.0F, 2.4F, Easing.SINE_IN_OUT)
                .key(148.0F, -2.2F, Easing.SINE_IN_OUT)
                .key(200.0F, 1.8F, Easing.SINE_IN_OUT)
                .key(243.0F, 0.0F, Easing.LINEAR)
                .key(246.0F, 12.5F, Easing.CUBIC_IN)
                .key(272.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(30.0F, 74.0F)
                .key(55.0F, 64.0F, Easing.CUBIC_IN)
                .key(59.0F, 88.0F, Easing.EXPO_OUT)
                .key(96.0F, 72.0F, Easing.BACK_OUT)
                .key(206.0F, 68.0F)
                .key(242.0F, 61.0F, Easing.CUBIC_IN)
                .key(247.0F, 102.0F, Easing.EXPO_OUT)
                .key(270.0F, 70.0F, Easing.SPRING_OUT)
                .hold(end));

        tl.cue(2, sound(o, ModSounds.KRAVE_SIREN, 1.2F, 1.3F));
        tl.cue(4, sound(first, ModSounds.KRAVE_SPAWN, 1.4F, 1.0F));
        tl.cue(120, sound(o, ModSounds.KRAVE_RUMBLE, 1.2F, 0.7F));
        tl.cue(244, sound(o, ModSounds.KRAVE_ROAR, 1.8F, 0.8F));
        tl.cue(268, sound(o, ModSounds.KRAVE_LAUGH, 1.2F, 0.75F));

        return scene;
    }

    // ------------------------------------------------------------------------
    // STAGE 4: THE CLEAVING
    // One object, one blow, shot entirely from the dirt. Stage 3 was eight fast
    // impacts from a wide lens; this is the opposite film - the camera never
    // leaves ground level, and the cleaver takes ninety ticks to fall.
    // ------------------------------------------------------------------------

    private static Scene theCleaving(Vec3 o, Vec3 eye) {
        final int end = 214;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 fallAt = o.add(2.0D, 0.0D, -8.0D);
        Vec3 hanging = fallAt.add(0.0D, 96.0D, 0.0D);
        Vec3 cocked = fallAt.add(0.0D, 116.0D, 0.0D);
        Vec3 barbara = o.add(-30.0D, -3.0D, 42.0D);
        float yawB = facing(barbara, o);
        Vec3 head = barbara.add(0.0D, 33.0D, 0.0D);

        // --- camera ---------------------------------------------------------
        // Worm's eye from the first frame and it stays there. A blade seen from
        // below is a blade; seen from above it is a shape lying on a field.
        rig.dolly(0, 40, 0, eye, o.add(-17.0D, 1.6D, -30.0D), o.add(0.0D, 3.0D, 0.0D),
                hanging, Easing.QUINT_OUT);
        // Crawl sideways while it is hoisted. Nothing else in frame moves, so a
        // slow parallax is the only thing telling you the shot is still alive.
        rig.dolly(40, 46, 10, o.add(-17.0D, 1.6D, -30.0D), o.add(-12.0D, 1.4D, -26.0D),
                hanging, cocked, Easing.SINE_IN_OUT);
        // Six ticks of absolutely nothing before it comes. The stillness is the
        // anticipation; the wind-up already happened above the top of the frame.
        rig.hold(86, 14, 6, o.add(-12.0D, 1.4D, -26.0D), cocked);
        rig.dolly(100, 5, 2, o.add(-12.0D, 1.4D, -26.0D), o.add(-12.0D, 0.4D, -25.0D),
                fallAt.add(0.0D, 14.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(105, 26, 8, o.add(-12.0D, 0.4D, -25.0D), o.add(-17.0D, 7.0D, -33.0D),
                fallAt.add(0.0D, 6.0D, 0.0D), Easing.SPRING_OUT);
        // Only now does the shot admit there is somebody standing behind it. She
        // has been there since tick 60, out of frame the whole time.
        rig.orbit(131, 58, 14, fallAt, 42.0D, 66.0D, 8.0D, 26.0D, 208.0F, 302.0F,
                head, Easing.SINE_IN_OUT);
        rig.dolly(189, 25, 12, ring(fallAt, 302.0F, 66.0D, 26.0D),
                o.add(46.0D, 34.0D, -42.0D), head, o.add(0.0D, 8.0D, 0.0D),
                Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(86.0F, -1.4F, Easing.SINE_IN_OUT)
                .key(99.0F, 0.0F, Easing.LINEAR)
                .key(103.0F, 15.0F, Easing.CUBIC_IN)
                .key(129.0F, 0.0F, Easing.SPRING_OUT)
                .key(160.0F, -2.4F, Easing.SINE_IN_OUT)
                .key(end, 0.0F, Easing.SINE_IN_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                // Long lens for the hang: it flattens the sky and makes the blade
                // look further away than it is, so the drop covers more ground.
                .key(40.0F, 54.0F, Easing.CUBIC_IN_OUT)
                .key(94.0F, 50.0F)
                .key(99.0F, 60.0F, Easing.CUBIC_IN)
                .key(104.0F, 108.0F, Easing.EXPO_OUT)
                .key(128.0F, 74.0F, Easing.SPRING_OUT)
                .key(160.0F, 70.0F)
                .hold(end));

        rig.shake(60, 40, 0.16F, 1.2F, null, 0.0D);
        rig.shake(101, 44, 5.2F, 2.4F, fallAt, 40.0D);
        rig.shake(110, 24, 1.3F, 1.8F, fallAt, 58.0D);
        rig.shake(150, 40, 0.18F, 1.3F, null, 0.0D);

        // --- the blade ------------------------------------------------------
        CleaverActor cleaver = new CleaverActor();
        cleaver.bind(tl, "knife", fallAt, 34.0F, 22.0F);
        cleaver.groundY = o.y;

        tl.track("knife.dy", Track.from(0.0F, 96.0F)
                // Hoisted UP before it comes down. Everything heavy winds up, and
                // ANTICIPATE pulls harder than BACK_IN for the really big blows.
                .key(86.0F, 116.0F, Easing.ANTICIPATE)
                .hold(94.0F)
                .key(101.0F, 0.3F, Easing.QUINT_IN)
                .key(104.0F, -3.0F, Easing.CUBIC_OUT)
                .key(112.0F, 2.2F, Easing.BACK_OUT)
                .key(130.0F, 1.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("knife.tilt", Track.from(0.0F, 3.0F)
                .hold(94.0F)
                .key(101.0F, -7.0F, Easing.CUBIC_IN)
                .key(106.0F, 4.5F, Easing.BACK_OUT)
                .key(124.0F, 0.0F, Easing.SPRING_OUT)
                // The quiver while it hangs, dying out as it steadies for the drop.
                .wobble(2.8F, 6.0F, 0.0F, 80.0F));
        tl.track("knife.spin", Track.from(0.0F, -26.0F)
                .hold(94.0F)
                .key(101.0F, 6.0F, Easing.CUBIC_IN)
                .hold(end));
        tl.track("knife.heat", Track.from(0.0F, 0.18F)
                .hold(98.0F)
                .key(102.0F, 2.0F, Easing.STEP)
                .key(124.0F, 0.3F, Easing.CUBIC_OUT)
                .hold(end));
        tl.track("knife.dust", Track.from(100.0F, 0.0F).key(178.0F, 1.0F, Easing.LINEAR));

        // --- who swung it ---------------------------------------------------
        // Revealed only by the orbit, and she is not holding anything. Whatever
        // brought the cleaver down is not in this shot at all.
        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", barbara, yawB, 15.0F);
        tl.track("barb.dy", Track.from(0.0F, -40.0F)
                .hold(54.0F)
                .key(112.0F, 0.0F, Easing.QUINT_OUT)
                .hold(end));
        tl.track("barb.headPitch", Track.from(0.0F, 32.0F)
                .hold(112.0F)
                .key(126.0F, -8.0F, Easing.BACK_OUT)
                .key(138.0F, 4.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("barb.armR", Track.from(0.0F, 6.0F)
                .hold(130.0F)
                .key(158.0F, -95.0F, Easing.BACK_OUT).hold(end));
        tl.track("barb.elbowR", Track.from(0.0F, 8.0F)
                .hold(130.0F)
                .key(158.0F, 120.0F, Easing.BACK_OUT).hold(end));
        tl.track("barb.jaw", Track.from(0.0F, 0.0F)
                .hold(164.0F)
                .key(172.0F, 32.0F, Easing.ELASTIC_OUT)
                .key(192.0F, 4.0F)
                .hold(end));
        tl.track("barb.ember", Track.from(0.0F, 0.2F)
                .hold(140.0F)
                .key(168.0F, 1.4F, Easing.EXPO_OUT)
                .key(184.0F, 0.5F)
                .hold(end));
        tl.track("barb.ring0", Track.from(172.0F, 0.0F).key(end, 1.0F, Easing.LINEAR));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.03F, 51.0F, 0.0F, 0.0F));

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 1.1F, 0.6F));
        tl.cue(54, sound(hanging, ModSounds.KRAVE_VOICE, 0.9F, 0.55F));
        tl.cue(86, sound(cocked, ModSounds.KRAVE_SIREN, 0.8F, 0.5F));
        tl.cue(100, sound(fallAt, ModSounds.KRAVE_BOOM, 4.0F, 0.42F));
        tl.cue(101, burst(fallAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 30, 9.0D, 0.2D));
        tl.cue(102, burst(fallAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE,
                150, 14.0D, 0.55D));
        tl.cue(112, sound(fallAt, ModSounds.KRAVE_SCREECH, 1.2F, 0.7F));
        tl.cue(172, sound(head, ModSounds.EVT_CHEPINA, 1.3F, 1.0F));
        tl.cue(196, sound(o, ModSounds.KRAVE_LAUGH, 1.0F, 0.8F));

        return new Scene("The Cleaving", o, tl, rig, 30).add(cleaver).add(she);
    }

    // ------------------------------------------------------------------------
    // STAGE 5: THE TORCHING
    // The three props this beat is named after, in the order the eye needs them:
    // the BLUNT held up unlit, the BLOWTORCH brought under it until the jet
    // visibly touches the coal, and then the GRASS below going up as she lowers
    // the burning end into it.
    //
    // This used to be a torch hanging alone at thirty blocks with the camera
    // orbiting at head height beneath it, which framed the empty sky under the
    // nozzle and nothing else - hence "no blunt, no torch, no grass". Three
    // things were wrong and all three are fixed here:
    //   * the blunt and the grass were never in the scene at all, so no amount of
    //     camera work could have found them;
    //   * the torch was bound thirty blocks above the shots that followed it,
    //     with a flame track long enough to drill two hundred blocks into the
    //     ground, so the only part in frame was a wash of additive orange;
    //   * nothing was placed relative to anything else, so a look target and the
    //     prop it named could drift apart without anybody noticing.
    // Every position below is derived from Barbara's own bind point through
    // local(), and every look target names one of the three props.
    // ------------------------------------------------------------------------

    private static Scene theTorching(Vec3 o, Vec3 eye) {
        final int end = 256;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        // She stands sixteen blocks off the ground because the torch has to fit
        // UNDER the coal: a lighter held below a joint is the readable pose, and
        // it needs eleven blocks of clear air plus the length of the tank.
        Vec3 barbara = o.add(-8.0D, 16.0D, 38.0D);
        float yawB = facing(barbara, o);
        Vec3 head = barbara.add(0.0D, 26.0D, 0.0D);

        // Her raised right fist, in her own frame, at scale 12 - arm forward and
        // up, elbow nearly straight. The blunt is bound at its LIT TIP, so the tip
        // hangs below the fist and the shaft runs up through it.
        Vec3 fist = local(barbara, yawB, 4.3D, 22.0D, 9.4D);
        Vec3 coal = fist.add(0.0D, -7.0D, 0.0D);
        Vec3 bluntMid = coal.add(0.0D, 14.0D, 0.0D);
        // Where the nozzle ends up: eleven blocks under the coal, pointing up.
        Vec3 nozzle = coal.add(0.0D, -11.0D, 0.0D);
        Vec3 torchRest = local(barbara, yawB, -5.0D, 12.0D, 1.0D);

        // The sweep that puts the burning end into the grass. Only the horizontal
        // part comes off this: the drop is authored straight down.
        Vec3 toward = o.subtract(barbara).normalize();
        Vec3 field = o.add(0.0D, 0.0D, 14.0D);

        // --- Barbara --------------------------------------------------------
        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", barbara, yawB, 12.0F);
        // Her own hand-rolled joint is four blocks long and would poke out of the
        // fist that is holding a thirty-one block one. The colossal prop is the
        // point of the scene, so the small one goes.
        she.get("joint").show(false);

        tl.track("barb.dy", Track.from(0.0F, -54.0F)
                .hold(6.0F)
                .key(40.0F, 0.0F, Easing.QUINT_OUT)
                .hold(226.0F)
                .key(end, -20.0F, Easing.CUBIC_IN));
        tl.track("barb.headPitch", Track.from(0.0F, 30.0F)
                .hold(34.0F)
                // Looks at the joint she is raising, then down at the world she is
                // about to put it in. Her eyeline is the audience's instruction.
                .key(46.0F, -16.0F, Easing.BACK_OUT)
                .key(58.0F, -6.0F, Easing.SPRING_OUT)
                .hold(146.0F)
                .key(186.0F, 26.0F, Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("barb.spine", Track.from(0.0F, 8.0F)
                .key(46.0F, -5.0F, Easing.BACK_OUT)
                .key(58.0F, 0.0F, Easing.SPRING_OUT)
                .hold(150.0F)
                .key(188.0F, 12.0F)
                .hold(end));
        // The right arm holds the blunt up, then lowers it into the grass. The
        // prop is a separate actor, so its dy/dz below have to travel with this.
        tl.track("barb.armR", Track.from(0.0F, 6.0F)
                .key(34.0F, 12.0F)
                .key(54.0F, -70.0F, Easing.BACK_OUT)
                .key(66.0F, -66.0F, Easing.SPRING_OUT)
                .hold(150.0F)
                .key(186.0F, -22.0F, Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("barb.elbowR", Track.from(0.0F, 8.0F)
                .key(54.0F, 45.0F, Easing.BACK_OUT)
                .hold(150.0F)
                .key(186.0F, 26.0F)
                .hold(end));
        // The left arm brings the torch across and takes it away again.
        tl.track("barb.armL", Track.from(0.0F, 5.0F)
                .hold(62.0F)
                .key(90.0F, -62.0F, Easing.BACK_OUT)
                .key(100.0F, -58.0F, Easing.SPRING_OUT)
                .hold(130.0F)
                .key(164.0F, 8.0F, Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("barb.elbowL", Track.from(0.0F, 6.0F)
                .hold(62.0F)
                .key(90.0F, 62.0F, Easing.BACK_OUT)
                .hold(130.0F)
                .key(164.0F, 10.0F)
                .hold(end));
        tl.track("barb.jaw", Track.from(0.0F, 0.0F)
                .hold(104.0F)
                // A grunt as it catches, and a long open on the exhale at the end.
                .key(112.0F, 18.0F, Easing.ELASTIC_OUT)
                .key(126.0F, 2.0F)
                .hold(206.0F)
                .key(214.0F, 34.0F, Easing.ELASTIC_OUT)
                .key(236.0F, 6.0F)
                .hold(end));
        tl.track("barb.ember", Track.constant(0.0F));
        tl.track("barb.ring0", Track.from(214.0F, 0.0F).key(end, 1.0F, Easing.LINEAR));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.03F, 47.0F, 0.0F, 0.0F));
        tl.track("barb.bob", Track.constant(0.0F).wobble(0.02F, 61.0F, 0.2F, 0.0F));

        // --- the blunt ------------------------------------------------------
        // Bound at scale 13: the model is 2.4 units long, so this is a thirty-one
        // block joint held by a twenty-nine block woman. Deliberately longer than
        // she is tall - the mismatch is the horror.
        BluntActor blunt = new BluntActor(1.0F);
        blunt.bind(tl, "blunt", coal, yawB, 13.0F);

        tl.track("blunt.show", Track.from(0.0F, 0.0F).key(44.0F, 1.0F, Easing.STEP).hold(end));
        // It condenses into her fist as the arm comes up, overshooting its size so
        // it arrives rather than appears.
        tl.track("blunt.grow", Track.from(44.0F, 0.06F)
                .key(58.0F, 1.08F, Easing.BACK_OUT)
                .key(70.0F, 1.0F, Easing.SPRING_OUT)
                .hold(end));
        // UNLIT until the torch touches it. Default is 0.6, i.e. already burning,
        // which would have thrown away the entire point of the blowtorch.
        tl.track("blunt.ember", Track.from(0.0F, 0.0F)
                .hold(104.0F)
                .key(112.0F, 1.55F, Easing.EXPO_OUT)
                .key(126.0F, 0.75F)
                .key(150.0F, 1.30F)
                .key(168.0F, 0.85F)
                .key(214.0F, 1.60F, Easing.EXPO_OUT)
                .key(238.0F, 0.7F)
                .hold(end));
        tl.track("blunt.burn", Track.from(0.0F, 0.0F)
                .hold(114.0F)
                .key(206.0F, 0.30F, Easing.CUBIC_IN_OUT)
                .key(end, 0.46F));
        // Held vertical while it is lit, then tipped over as she reaches down.
        tl.track("blunt.pitch", Track.from(0.0F, 4.0F)
                .hold(150.0F)
                .key(186.0F, -46.0F, Easing.CUBIC_IN_OUT)
                .key(196.0F, -41.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("blunt.spin", Track.from(0.0F, 0.0F).key(end, -22.0F, Easing.SINE_IN_OUT));
        tl.track("blunt.dy", Track.from(0.0F, 0.0F)
                .hold(150.0F)
                .key(186.0F, -14.0F, Easing.CUBIC_IN_OUT)
                .key(196.0F, -12.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("blunt.dx", Track.from(0.0F, 0.0F)
                .hold(150.0F)
                .key(186.0F, (float) (toward.x * 16.0D), Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("blunt.dz", Track.from(0.0F, 0.0F)
                .hold(150.0F)
                .key(186.0F, (float) (toward.z * 16.0D), Easing.CUBIC_IN_OUT)
                .hold(end));

        // --- the blowtorch --------------------------------------------------
        // Scale 5, so fourteen blocks of it: oversized for her hand on purpose,
        // because a correctly proportioned lighter at this camera distance is two
        // pixels of grey. Pitched almost upside down so the jet goes UP into the
        // coal, which is how anybody actually lights one of these.
        TorchActor torch = new TorchActor();
        torch.bind(tl, "torch", nozzle, yawB, 5.0F);

        tl.track("torch.show", Track.from(0.0F, 0.0F).key(60.0F, 1.0F, Easing.STEP)
                .key(178.0F, 0.0F, Easing.STEP).hold(end));
        tl.track("torch.dx", Track.from(0.0F, (float) (torchRest.x - nozzle.x))
                .hold(64.0F)
                .key(90.0F, 0.0F, Easing.BACK_OUT)
                .hold(132.0F)
                .key(166.0F, (float) (torchRest.x - nozzle.x), Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("torch.dz", Track.from(0.0F, (float) (torchRest.z - nozzle.z))
                .hold(64.0F)
                .key(90.0F, 0.0F, Easing.BACK_OUT)
                .hold(132.0F)
                .key(166.0F, (float) (torchRest.z - nozzle.z), Easing.CUBIC_IN_OUT)
                .hold(end));
        tl.track("torch.dy", Track.from(0.0F, (float) (torchRest.y - nozzle.y))
                .hold(64.0F)
                .key(90.0F, 2.0F, Easing.BACK_OUT)
                .key(100.0F, 0.0F, Easing.SPRING_OUT)
                .hold(132.0F)
                .key(166.0F, (float) (torchRest.y - nozzle.y), Easing.CUBIC_IN_OUT)
                .hold(end));
        // She rolls her wrist over as it comes across, so the tank swings under.
        tl.track("torch.pitch", Track.from(0.0F, 96.0F)
                .hold(64.0F)
                .key(92.0F, 174.0F, Easing.BACK_OUT)
                .key(102.0F, 171.0F, Easing.SPRING_OUT)
                .hold(140.0F)
                .key(166.0F, 110.0F)
                .hold(end)
                .wobble(1.6F, 29.0F, 0.0F, 0.0F));
        // Anticipation: the trigger pulls the WRONG WAY first.
        tl.track("torch.trigger", Track.from(0.0F, 0.0F)
                .hold(94.0F)
                .key(100.0F, -26.0F, Easing.BACK_IN)
                .key(106.0F, 30.0F, Easing.ELASTIC_OUT)
                .hold(136.0F)
                .key(150.0F, 0.0F));
        // A SHORT jet. Each of the eighteen flame segments is 1.35 model units, so
        // at scale 5 a flame track of 0.18 is nineteen blocks - eleven of them to
        // reach the coal and eight of overlap. The old scene ran this to 1.0,
        // which is two hundred blocks of fire straight through the floor.
        tl.track("torch.flame", Track.from(0.0F, 0.0F)
                .hold(104.0F)
                .key(105.0F, 0.0F, Easing.STEP)
                .key(110.0F, 0.20F, Easing.EXPO_OUT)
                .key(118.0F, 0.14F, Easing.CUBIC_OUT)
                .key(126.0F, 0.19F, Easing.SPRING_OUT)
                .hold(140.0F)
                .key(152.0F, 0.0F, Easing.CUBIC_IN)
                .hold(end));
        tl.track("torch.sweep", Track.constant(0.0F).wobble(3.2F, 37.0F, 0.0F, 0.0F));

        // --- the grass ------------------------------------------------------
        // A field of it standing between the lens and her, so the fire has
        // somewhere to go and the shot has a foreground. Bound at the balance
        // point, which sits 0.38 of the length up from the torn end - so binding
        // that high above the ground is what plants them IN it.
        final double[] fx = { -20.0D, -4.0D, 12.0D, 26.0D, -14.0D, 6.0D, 22.0D };
        final double[] fz = { 10.0D, 22.0D, 6.0D, 18.0D, 30.0D, 34.0D, 26.0D };
        final float[] flen = { 19.0F, 25.0F, 17.0F, 22.0F, 28.0F, 21.0F, 16.0F };
        final int[] lit = { 184, 178, 194, 200, 182, 190, 206 };

        Scene scene = new Scene("The Torching", o, tl, rig, 32)
                .add(she).add(blunt).add(torch);

        for (int i = 0; i < fx.length; i++) {
            String id = "grass" + i;
            GrassBladeActor stalk = new GrassBladeActor(flen[i]);
            Vec3 root = o.add(fx[i], flen[i] * 0.38D, fz[i]);
            stalk.bind(tl, id, root, 37.0F * i, 1.0F);
            // Standing grass leans and sways; a field of perfectly upright ones
            // reads as a fence.
            tl.track(id + ".pitch", Track.constant((i % 2 == 0 ? 7.0F : -9.0F) + i)
                    .wobble(2.4F, 43.0F + i * 3.0F, i * 0.13F, 0.0F));
            // It catches, goes white-hot, and is eaten from the tip down. "grow"
            // hides segments from the far end back, so winding it down IS the
            // blade burning away.
            tl.track(id + ".glow", Track.from(0.0F, 0.0F)
                    .hold(lit[i])
                    .key(lit[i] + 16, 1.0F, Easing.CUBIC_IN)
                    .key(lit[i] + 44, 0.72F)
                    .hold(end));
            tl.track(id + ".grow", Track.from(0.0F, 1.0F)
                    .hold(lit[i] + 20)
                    .key(lit[i] + 56, 0.08F, Easing.CUBIC_IN)
                    .hold(end));
            tl.cue(lit[i], sound(root, ModSounds.KRAVE_RUMBLE, 1.6F, 1.7F));
            tl.cue(lit[i], burst(o.add(fx[i], 1.0D, fz[i]), ParticleTypes.FLAME,
                    50, 3.0D, 0.6D));
            tl.cue(lit[i] + 18, burst(o.add(fx[i], 2.0D, fz[i]), ParticleTypes.LARGE_SMOKE,
                    60, 4.0D, 0.5D));
            scene.add(stalk);
        }

        // --- camera ---------------------------------------------------------
        // Every look target below is one of the three props, computed from the
        // same constants they are bound with.
        // 1. Establish her and the whole length of the joint.
        rig.dolly(0, 40, 0, eye, o.add(8.0D, 18.0D, -18.0D), o.add(0.0D, 3.0D, 0.0D),
                bluntMid, Easing.CUBIC_IN_OUT);
        // 2. Push in on the raised blunt while it is still unlit. Sixteen ticks of
        //    a giant holding an unlit joint is the anticipation for the lighter.
        rig.dolly(40, 32, 8, o.add(8.0D, 18.0D, -18.0D), o.add(2.0D, 24.0D, -2.0D),
                bluntMid, coal.add(0.0D, 4.0D, 0.0D), Easing.QUINT_OUT);
        // 3. Drift across as the torch comes up from her other side, framing the
        //    gap between the nozzle and the coal so the audience can see it close.
        rig.dolly(72, 30, 10, o.add(2.0D, 24.0D, -2.0D), o.add(6.0D, 24.0D, 4.0D),
                coal.add(0.0D, 4.0D, 0.0D), coal.add(0.0D, -6.0D, 0.0D),
                Easing.SINE_IN_OUT);
        // 4. Tight on the join at the moment of ignition: thirty blocks out,
        //    centred six blocks BELOW the coal, because the coal, the eleven block
        //    gap and the whole torch under it have to fit in the same frame or the
        //    audience sees a joint lighting itself.
        rig.dolly(102, 18, 6, o.add(6.0D, 24.0D, 4.0D), o.add(4.0D, 26.0D, 2.0D),
                coal.add(0.0D, -6.0D, 0.0D), Easing.EXPO_OUT);
        rig.hold(120, 16, 6, o.add(4.0D, 26.0D, 2.0D), coal.add(0.0D, -6.0D, 0.0D));
        // 5. Pull out and up to her face as she takes the torch away.
        rig.dolly(136, 30, 10, o.add(4.0D, 26.0D, 2.0D), o.add(14.0D, 34.0D, -14.0D),
                coal.add(0.0D, -6.0D, 0.0D), head, Easing.CUBIC_IN_OUT);
        // 6. Crane DOWN with the joint as she lowers it, ending at ground level
        //    with the field in the foreground. The move and the fire arrive
        //    together, which is the only reason the fire reads as caused by her.
        rig.dolly(166, 44, 14, o.add(14.0D, 34.0D, -14.0D), o.add(18.0D, 5.0D, -28.0D),
                head, field.add(0.0D, 6.0D, 0.0D), Easing.CUBIC_IN_OUT);
        // 7. Walk around the burning field with her towering behind it.
        rig.orbit(210, 30, 12, field, 46.0D, 62.0D, 9.0D, 24.0D, 236.0F, 300.0F,
                field.add(0.0D, 10.0D, 0.0D), Easing.SINE_IN_OUT);
        rig.dolly(240, 16, 10, ring(field, 300.0F, 62.0D, 24.0D),
                o.add(44.0D, 34.0D, -46.0D), field.add(0.0D, 10.0D, 0.0D),
                o.add(0.0D, 10.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(40.0F, -2.4F, Easing.SINE_IN_OUT)
                .key(104.0F, 0.0F, Easing.LINEAR)
                .key(110.0F, 5.6F, Easing.BACK_OUT)
                .key(134.0F, -1.6F, Easing.SPRING_OUT)
                .key(190.0F, 3.4F, Easing.SINE_IN_OUT)
                .key(end, 0.0F, Easing.SINE_IN_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(40.0F, 66.0F)
                .key(100.0F, 64.0F, Easing.CUBIC_IN_OUT)
                // Squeezes down on the trigger cock and blows open on the catch,
                // but never so tight that the tank drops off the bottom edge.
                .key(106.0F, 60.0F, Easing.CUBIC_IN)
                .key(112.0F, 84.0F, Easing.EXPO_OUT)
                .key(132.0F, 68.0F, Easing.SPRING_OUT)
                .key(186.0F, 82.0F, Easing.CUBIC_IN_OUT)
                .key(214.0F, 74.0F)
                .key(end, 70.0F));

        rig.shake(106, 14, 0.45F, 2.6F, coal, 40.0D);
        rig.shake(112, 26, 1.10F, 2.2F, coal, 46.0D);
        rig.shake(150, 30, 0.20F, 1.4F, null, 0.0D);
        rig.shake(186, 34, 1.60F, 1.9F, field, 50.0D);
        rig.shake(212, 40, 0.55F, 1.6F, field, 64.0D);

        // --- score ----------------------------------------------------------
        // Everything hung off the burning end asks tip() for the tick it fires on:
        // the coal climbs the shaft as it burns, so the prop's bind point stops
        // being where the fire is the moment "burn" leaves zero.
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 1.0F, 0.8F));
        tl.cue(40, sound(head, ModSounds.KRAVE_VOICE, 1.0F, 0.9F));
        tl.cue(58, sound(coal, ModSounds.EVT_ROLL, 1.3F, 1.0F));
        tl.cue(96, sound(nozzle, ModSounds.EVT_LIGHTER, 1.4F, 1.0F));
        tl.cue(105, sound(nozzle, SoundEvents.FLINTANDSTEEL_USE, 2.6F, 0.5F));
        tl.cue(110, sound(nozzle, SoundEvents.FLINTANDSTEEL_USE, 2.0F, 0.75F));
        tl.cue(112, sound(blunt.tip(112.0F), ModSounds.KRAVE_SCREECH, 1.2F, 0.95F));
        tl.cue(112, burst(blunt.tip(112.0F), ParticleTypes.FLAME, 70, 4.0D, 0.5D));
        tl.cue(118, burst(blunt.tip(118.0F), ParticleTypes.LARGE_SMOKE, 70, 5.0D, 0.4D));
        tl.cue(152, burst(blunt.tip(152.0F), ParticleTypes.LARGE_SMOKE, 60, 6.0D, 0.35D));
        tl.cue(186, sound(field, ModSounds.KRAVE_ROAR, 1.6F, 0.7F));
        tl.cue(196, burst(blunt.tip(196.0F), ParticleTypes.FLAME, 90, 7.0D, 0.6D));
        tl.cue(214, sound(head, ModSounds.EVT_OG, 1.3F, 0.95F));
        tl.cue(214, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE,
                50, 6.0D, 0.35D));
        tl.cue(236, sound(o, ModSounds.KRAVE_LAUGH, 1.1F, 0.8F));

        return scene;
    }

    // ------------------------------------------------------------------------
    // STAGE 6: BOXFALL
    // Forty blocks of cereal carton. The only thing that matters is the landing:
    // squash, flaps past their stop, and a real spray of Krave.
    // ------------------------------------------------------------------------

    private static Scene boxfall(Vec3 o, Vec3 eye) {
        final int end = 212;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 rim = o.add(0.0D, 40.0D, 0.0D);

        // --- camera ---------------------------------------------------------
        rig.dolly(0, 52, 0, eye, o.add(-16.0D, 2.2D, -26.0D), o.add(0.0D, 4.0D, 0.0D),
                o.add(0.0D, 72.0D, 0.0D), Easing.CUBIC_IN_OUT);
        // The camera deliberately LAGS the fall. Letting the carton outrun the
        // frame is what makes forty blocks of cardboard feel fast.
        rig.dolly(52, 26, 5, o.add(-16.0D, 2.2D, -26.0D), o.add(-19.0D, 3.0D, -30.0D),
                o.add(0.0D, 72.0D, 0.0D), o.add(0.0D, 20.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(78, 5, 2, o.add(-19.0D, 3.0D, -30.0D), o.add(-19.0D, 0.6D, -29.0D),
                o.add(0.0D, 10.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(83, 26, 6, o.add(-19.0D, 0.6D, -29.0D), o.add(-20.0D, 5.5D, -32.0D),
                o.add(0.0D, 17.0D, 0.0D), Easing.SPRING_OUT);
        rig.orbit(109, 68, 14, o, 34.0D, 27.0D, 5.0D, 22.0D, 240.0F, 372.0F,
                o.add(0.0D, 20.0D, 0.0D), Easing.SINE_IN_OUT);
        rig.dolly(177, 35, 12, o.add(26.4D, 22.0D, 5.6D), o.add(35.0D, 27.0D, -41.0D),
                o.add(0.0D, 20.0D, 0.0D), o.add(0.0D, 12.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(60.0F, -2.2F)
                .key(78.0F, 0.0F, Easing.STEP)
                .key(81.0F, 11.5F, Easing.CUBIC_IN)
                .key(106.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(50.0F, 64.0F)
                .key(77.0F, 60.0F, Easing.CUBIC_IN)
                .key(81.0F, 102.0F, Easing.EXPO_OUT)
                .key(102.0F, 73.0F, Easing.SPRING_OUT)
                .key(150.0F, 75.0F)
                .key(end, 70.0F));

        rig.shake(44, 26, 0.34F, 1.5F, null, 0.0D);
        rig.shake(79, 42, 4.2F, 2.2F, o, 40.0D);
        rig.shake(87, 24, 1.15F, 1.8F, o, 55.0D);
        rig.shake(120, 60, 0.20F, 1.4F, null, 0.0D);

        // --- the carton -----------------------------------------------------
        BoxTitanActor box = new BoxTitanActor();
        box.bind(tl, "box", o, 18.0F, 17.0F);
        box.groundY = o.y;

        tl.track("box.dy", Track.from(0.0F, 98.0F)
                .key(30.0F, 93.0F, Easing.SINE_IN_OUT)
                // It RISES before it drops. Nine blocks of wind-up.
                .key(46.0F, 102.0F, Easing.BACK_IN)
                .key(79.0F, 0.0F, Easing.EXPO_IN)
                .hold(end));
        tl.track("box.spin", Track.from(0.0F, 0.0F)
                .key(46.0F, 26.0F, Easing.SINE_IN_OUT)
                .key(79.0F, 194.0F, Easing.EXPO_IN)
                .key(98.0F, 203.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("box.squash", Track.from(0.0F, 1.0F)
                .hold(78.0F)
                .key(79.0F, 1.0F, Easing.STEP)
                .key(82.0F, 0.60F, Easing.QUINT_OUT)
                .key(88.0F, 1.15F, Easing.BACK_OUT)
                .key(99.0F, 0.97F, Easing.SPRING_OUT)
                .key(116.0F, 1.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("box.flap", Track.from(0.0F, 2.0F)
                .hold(79.0F)
                // Past the stop, then back: flaps have no brakes.
                .key(86.0F, 126.0F, Easing.BACK_OUT)
                .key(97.0F, 86.0F, Easing.SPRING_OUT)
                .key(114.0F, 97.0F, Easing.SINE_IN_OUT)
                .hold(end));
        tl.track("box.spill", Track.from(0.0F, 0.0F)
                .hold(82.0F)
                .key(83.0F, 0.02F, Easing.STEP)
                .key(184.0F, 1.0F, Easing.CUBIC_OUT)
                .hold(end));

        // A second giant far off behind it, purely for scale: something that big
        // needs something else that big in frame or it just reads as close.
        SmokerActor far = new SmokerActor();
        Vec3 farAt = o.add(-62.0D, -4.0D, 68.0D);
        far.bind(tl, "far", farAt, facing(farAt, o), 11.0F);
        tl.track("far.dy", Track.from(0.0F, -28.0F)
                .hold(22.0F)
                .key(92.0F, 0.0F, Easing.QUINT_OUT)
                .hold(end));
        tl.track("far.headPitch", Track.from(0.0F, 26.0F)
                .key(96.0F, -8.0F, Easing.BACK_OUT)
                .key(108.0F, -2.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("far.armR", Track.from(0.0F, 6.0F).key(100.0F, -95.0F, Easing.BACK_OUT).hold(end));
        tl.track("far.elbowR", Track.from(0.0F, 8.0F).key(100.0F, 120.0F, Easing.BACK_OUT).hold(end));
        tl.track("far.jaw", Track.from(0.0F, 0.0F)
                .hold(118.0F)
                .key(124.0F, 30.0F, Easing.ELASTIC_OUT)
                .key(136.0F, 8.0F)
                .key(150.0F, 26.0F, Easing.ELASTIC_OUT)
                .key(168.0F, 0.0F));
        tl.track("far.ember", Track.from(0.0F, 0.3F)
                .key(112.0F, 1.3F, Easing.EXPO_OUT)
                .key(126.0F, 0.4F)
                .key(150.0F, 1.1F)
                .key(end, 0.2F));
        tl.track("far.ring0", Track.from(124.0F, 0.0F).key(190.0F, 1.0F, Easing.LINEAR));
        tl.track("far.ring1", Track.from(150.0F, 0.0F).key(210.0F, 1.0F, Easing.LINEAR));
        tl.track("far.breathe", Track.constant(1.0F).wobble(0.03F, 51.0F, 0.0F, 0.0F));

        // --- score ----------------------------------------------------------
        tl.cue(4, sound(o, ModSounds.KRAVE_SIREN, 1.3F, 0.9F));
        tl.cue(30, sound(rim, ModSounds.KRAVE_VOICE, 1.0F, 0.7F));
        tl.cue(46, sound(o, ModSounds.KRAVE_RUMBLE, 1.4F, 0.65F));
        tl.cue(79, sound(o, ModSounds.KRAVE_BOOM, 4.0F, 0.5F));
        tl.cue(79, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 34, 14.0D, 0.15D));
        tl.cue(80, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE, 170, 18.0D, 0.6D));
        tl.cue(88, sound(o, ModSounds.EVT_MCD, 1.3F, 1.0F));
        tl.cue(152, sound(o, ModSounds.KRAVE_LAUGH, 1.1F, 0.8F));

        return new Scene("Boxfall", o, tl, rig, 30).add(box).add(far);
    }

    // ------------------------------------------------------------------------
    // STAGE 7: ASHFALL
    // No impact for three and a half minutes of screen time, and that is the
    // point: after the carton, the next thing that happens is nothing. A joint
    // seventy blocks long lies across the sky burning itself down in real time,
    // ash falling out of it onto the world, until whoever is holding it decides
    // they are finished and flicks it away.
    // ------------------------------------------------------------------------

    private static Scene ashfall(Vec3 o, Vec3 eye) {
        final int end = 250;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 litAt = o.add(-30.0D, 44.0D, 26.0D);
        Vec3 farAt = o.add(74.0D, -12.0D, -86.0D);

        // --- the joint ------------------------------------------------------
        // Scale 30, so seventy-two blocks of it and seven thick. Tipped over so it
        // lies diagonally across the sky rather than standing in it: a vertical
        // one would read as a tower, and a tower is not frightening.
        BluntActor blunt = new BluntActor(1.0F);
        blunt.bind(tl, "blunt", litAt, 42.0F, 30.0F);

        tl.track("blunt.pitch", Track.from(0.0F, -64.0F)
                .key(120.0F, -58.0F, Easing.SINE_IN_OUT)
                .hold(196.0F)
                // It tips as it is flicked, so the coal leads the way down.
                .key(212.0F, -96.0F, Easing.CUBIC_IN)
                .hold(end));
        tl.track("blunt.spin", Track.from(0.0F, 0.0F)
                .key(196.0F, 26.0F, Easing.SINE_IN_OUT)
                .key(216.0F, 74.0F, Easing.CUBIC_IN)
                .hold(end));
        // Three draws by somebody the shot never shows. The flares are the only
        // evidence anybody is holding it.
        tl.track("blunt.ember", Track.from(0.0F, 0.55F)
                .key(38.0F, 1.45F, Easing.EXPO_OUT)
                .key(56.0F, 0.5F)
                .key(96.0F, 1.5F, Easing.EXPO_OUT)
                .key(116.0F, 0.45F)
                .key(158.0F, 1.55F, Easing.EXPO_OUT)
                .key(180.0F, 0.5F)
                .key(206.0F, 1.2F)
                .hold(end));
        // Burning down for the whole scene. Everything else here is secondary
        // motion on top of one slow track.
        tl.track("blunt.burn", Track.from(0.0F, 0.02F)
                .key(200.0F, 0.62F, Easing.SINE_IN_OUT)
                .key(end, 0.74F));
        // The flick: sixty blocks of drop with the coal leading.
        tl.track("blunt.dy", Track.from(0.0F, 0.0F)
                .hold(196.0F)
                // Lifted first. Even a flick winds up.
                .key(206.0F, 7.0F, Easing.BACK_IN)
                .key(222.0F, -62.0F, Easing.EXPO_IN)
                .key(228.0F, -58.0F, Easing.CUBIC_OUT)
                .key(244.0F, -61.0F, Easing.SPRING_OUT)
                .hold(end));

        // --- who it belongs to ----------------------------------------------
        // She only stands up at the very end, a hundred and fifty blocks out, so
        // the last thing the scene tells you is whose hand it was in.
        SmokerActor far = new SmokerActor();
        far.bind(tl, "far", farAt, facing(farAt, o), 24.0F);
        tl.track("far.dy", Track.from(0.0F, -60.0F)
                .hold(176.0F)
                .key(224.0F, 0.0F, Easing.QUINT_OUT)
                .hold(end));
        tl.track("far.headPitch", Track.from(0.0F, 30.0F)
                .hold(224.0F)
                .key(238.0F, -10.0F, Easing.BACK_OUT)
                .hold(end));
        tl.track("far.armR", Track.from(0.0F, 6.0F)
                .hold(210.0F)
                .key(230.0F, -40.0F, Easing.BACK_OUT).hold(end));
        tl.track("far.ember", Track.constant(0.0F));
        tl.track("far.breathe", Track.constant(1.0F).wobble(0.03F, 55.0F, 0.0F, 0.0F));

        // --- camera ---------------------------------------------------------
        // Authored AFTER the tracks, because every look target here is
        // blunt.tip(t) - the coal climbs the shaft as it burns, so where the fire
        // is at tick 140 is not where the prop was bound.
        rig.dolly(0, 44, 0, eye, o.add(10.0D, 3.0D, -32.0D), o.add(0.0D, 3.0D, 0.0D),
                blunt.tip(44.0F), Easing.QUINT_OUT);
        // Climb the length of it. Slowly - the shot has nowhere to be.
        rig.dolly(44, 44, 12, o.add(10.0D, 3.0D, -32.0D), o.add(2.0D, 24.0D, -18.0D),
                blunt.tip(44.0F), blunt.tip(88.0F), Easing.SINE_IN_OUT);
        rig.orbit(88, 60, 14, litAt, 78.0D, 58.0D, -14.0D, 2.0D, 206.0F, 292.0F,
                blunt.tip(118.0F), Easing.SINE_IN_OUT);
        // Back down to the dirt to watch the ash arrive on it.
        rig.dolly(148, 44, 14, ring(litAt, 292.0F, 58.0D, 2.0D), o.add(22.0D, 4.0D, -40.0D),
                blunt.tip(148.0F), blunt.tip(192.0F), Easing.CUBIC_IN_OUT);
        rig.dolly(192, 16, 8, o.add(22.0D, 4.0D, -40.0D), o.add(24.0D, 7.0D, -44.0D),
                blunt.tip(192.0F), blunt.tip(208.0F), Easing.SINE_IN_OUT);
        rig.dolly(208, 14, 4, o.add(24.0D, 7.0D, -44.0D), o.add(23.0D, 3.0D, -41.0D),
                blunt.tip(208.0F), o.add(0.0D, 6.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(222, 28, 8, o.add(23.0D, 3.0D, -41.0D), o.add(34.0D, 16.0D, -54.0D),
                o.add(0.0D, 6.0D, 0.0D), farAt.add(0.0D, 34.0D, 0.0D), Easing.SPRING_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(88.0F, 2.8F, Easing.SINE_IN_OUT)
                .key(148.0F, -1.8F, Easing.SINE_IN_OUT)
                .key(219.0F, 0.0F, Easing.LINEAR)
                .key(224.0F, 10.0F, Easing.CUBIC_IN)
                .key(248.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                // Long and getting longer: the dread is that nothing is happening,
                // and a narrow lens is what makes a still frame feel airless.
                .key(44.0F, 58.0F, Easing.CUBIC_IN_OUT)
                .key(148.0F, 54.0F)
                .key(206.0F, 60.0F)
                .key(220.0F, 56.0F, Easing.CUBIC_IN)
                .key(226.0F, 98.0F, Easing.EXPO_OUT)
                .key(246.0F, 72.0F, Easing.SPRING_OUT)
                .hold(end));

        rig.shake(0, 200, 0.10F, 0.9F, null, 0.0D);
        rig.shake(38, 20, 0.30F, 1.5F, null, 0.0D);
        rig.shake(96, 20, 0.34F, 1.5F, null, 0.0D);
        rig.shake(158, 22, 0.38F, 1.5F, null, 0.0D);
        rig.shake(222, 40, 4.6F, 2.3F, o, 44.0D);
        rig.shake(232, 22, 1.2F, 1.8F, o, 60.0D);

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 1.2F, 0.55F));
        // Ash off the burning end every twenty ticks. tip() is asked for the cue's
        // own tick each time, so the fall starts from where the coal has crept to.
        for (int t = 24; t < 200; t += 20) {
            tl.cue(t, burst(blunt.tip(t), ParticleTypes.LARGE_SMOKE, 34, 7.0D, 0.25D));
            tl.cue(t + 6, burst(blunt.tip(t + 6.0F), ParticleTypes.FLAME, 12, 4.0D, 0.2D));
        }
        tl.cue(38, sound(blunt.tip(38.0F), ModSounds.KRAVE_VOICE, 1.1F, 0.5F));
        tl.cue(96, sound(blunt.tip(96.0F), ModSounds.KRAVE_VOICE, 1.2F, 0.45F));
        tl.cue(158, sound(blunt.tip(158.0F), ModSounds.EVT_NOTREADY, 1.3F, 1.0F));
        tl.cue(206, sound(blunt.tip(206.0F), ModSounds.KRAVE_SCREECH, 1.2F, 0.6F));
        tl.cue(222, sound(o, ModSounds.KRAVE_BOOM, 4.0F, 0.4F));
        tl.cue(222, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 30, 12.0D, 0.2D));
        tl.cue(223, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.FLAME, 140, 12.0D, 0.6D));
        tl.cue(224, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE, 180, 20.0D, 0.6D));
        tl.cue(238, sound(farAt, ModSounds.KRAVE_LAUGH, 1.4F, 0.7F));

        HellGateActor gate = hellGate(tl, o, 46.0F, 10, 60, 84, end);
        return new Scene("Ashfall", o, tl, rig, 34).add(blunt).add(far).add(gate);
    }

    // ------------------------------------------------------------------------
    // STAGE 8: THE BARRAGE
    // Everything the show owns, dropped one after another with no gap to recover
    // in: the cleaver, then the carton, then three blades of grass in five
    // seconds. Stage 7 was one object and no impact; this is four impacts and no
    // stillness at all.
    // ------------------------------------------------------------------------

    private static Scene theBarrage(Vec3 o, Vec3 eye) {
        final int end = 268;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 knifeAt = o.add(-14.0D, 0.0D, 10.0D);
        Vec3 boxAt = o.add(22.0D, 0.0D, -12.0D);

        Scene scene = new Scene("The Barrage", o, tl, rig, 34);

        // --- one: the cleaver ------------------------------------------------
        CleaverActor cleaver = new CleaverActor();
        cleaver.bind(tl, "knife", knifeAt, 40.0F, 17.0F);
        cleaver.groundY = o.y;
        tl.track("knife.dy", Track.from(0.0F, 74.0F)
                .key(58.0F, 88.0F, Easing.ANTICIPATE)
                .hold(66.0F)
                .key(78.0F, 0.3F, Easing.QUINT_IN)
                .key(81.0F, -2.6F, Easing.CUBIC_OUT)
                .key(89.0F, 1.9F, Easing.BACK_OUT)
                .key(106.0F, 0.9F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("knife.tilt", Track.from(0.0F, 0.0F)
                .hold(66.0F)
                .key(78.0F, -7.0F, Easing.CUBIC_IN)
                .key(83.0F, 4.0F, Easing.BACK_OUT)
                .key(100.0F, 0.0F, Easing.SPRING_OUT)
                .wobble(2.6F, 5.0F, 0.0F, 60.0F));
        tl.track("knife.heat", Track.from(0.0F, 0.2F)
                .hold(76.0F)
                .key(79.0F, 2.0F, Easing.STEP)
                .key(98.0F, 0.3F, Easing.CUBIC_OUT)
                .hold(end));
        tl.track("knife.dust", Track.from(78.0F, 0.0F).key(150.0F, 1.0F, Easing.LINEAR));
        impact(tl, rig, knifeAt, 78, 2.0F);
        scene.add(cleaver);

        // --- two: the carton -------------------------------------------------
        BoxTitanActor box = new BoxTitanActor();
        box.bind(tl, "box", boxAt, -26.0F, 15.0F);
        box.groundY = o.y;
        tl.track("box.dy", Track.from(0.0F, 96.0F)
                .hold(96.0F)
                .key(112.0F, 106.0F, Easing.ANTICIPATE)
                .key(134.0F, 0.0F, Easing.EXPO_IN)
                .hold(end));
        tl.track("box.spin", Track.from(0.0F, 0.0F)
                .key(112.0F, 20.0F, Easing.SINE_IN_OUT)
                .key(134.0F, 232.0F, Easing.EXPO_IN)
                .key(152.0F, 240.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("box.squash", Track.from(0.0F, 1.0F)
                .hold(133.0F)
                .key(134.0F, 1.0F, Easing.STEP)
                .key(137.0F, 0.58F, Easing.QUINT_OUT)
                .key(143.0F, 1.16F, Easing.BACK_OUT)
                .key(156.0F, 0.97F, Easing.SPRING_OUT)
                .key(174.0F, 1.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("box.flap", Track.from(0.0F, 2.0F)
                .hold(134.0F)
                .key(141.0F, 130.0F, Easing.BACK_OUT)
                .key(153.0F, 86.0F, Easing.SPRING_OUT)
                .key(171.0F, 98.0F, Easing.SINE_IN_OUT)
                .hold(end));
        tl.track("box.spill", Track.from(0.0F, 0.0F)
                .hold(137.0F)
                .key(138.0F, 0.02F, Easing.STEP)
                .key(238.0F, 1.0F, Easing.CUBIC_OUT)
                .hold(end));
        impact(tl, rig, boxAt, 134, 2.4F);
        scene.add(box);

        // --- three: the grass ------------------------------------------------
        // Thrown from off the top of the world by somebody the shot never cuts to,
        // which is worse than showing her: stage 3 already established who throws
        // these, so the audience supplies her for free.
        final float[] fromAt = { 120.0F, 250.0F, 20.0F };
        final float[] toAt = { 300.0F, 70.0F, 200.0F };
        final double[] toIn = { 26.0D, 18.0D, 7.0D };
        final float[] bladeLen = { 30.0F, 36.0F, 48.0F };
        final int[] letGo = { 168, 186, 202 };
        final int[] land = { 190, 206, 220 };

        for (int i = 0; i < 3; i++) {
            String id = "blade" + i;
            Vec3 from = ring(o, fromAt[i], 74.0D, 96.0D + i * 12.0D);
            Vec3 to = ring(o, toAt[i], toIn[i], 0.0D);
            GrassBladeActor blade = new GrassBladeActor(bladeLen[i]);
            blade.bind(tl, id, to, facing(to, from), 1.0F);
            spear(tl, id, from, to, letGo[i] - 16, letGo[i], land[i], end,
                    3 + i, (i % 2 == 0 ? 13.0F : -17.0F));
            impact(tl, rig, to, land[i], 2.0F + i * 0.5F);
            tl.cue(letGo[i], sound(from, ModSounds.KRAVE_SCREECH, 1.1F, 1.1F - i * 0.08F));
            scene.add(blade);
        }

        // --- camera ---------------------------------------------------------
        // One continuous orbit, tightening the whole way, so the barrage is shot
        // like weather rather than like four separate events.
        rig.dolly(0, 44, 0, eye, o.add(-52.0D, 20.0D, -44.0D), o.add(0.0D, 4.0D, 0.0D),
                knifeAt.add(0.0D, 66.0D, 0.0D), Easing.CUBIC_IN_OUT);
        rig.dolly(44, 30, 8, o.add(-52.0D, 20.0D, -44.0D), o.add(-44.0D, 11.0D, -38.0D),
                knifeAt.add(0.0D, 78.0D, 0.0D), knifeAt.add(0.0D, 8.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(74, 5, 2, o.add(-44.0D, 11.0D, -38.0D), o.add(-44.0D, 7.0D, -36.0D),
                knifeAt.add(0.0D, 6.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(79, 24, 7, o.add(-44.0D, 7.0D, -36.0D), o.add(-48.0D, 18.0D, -42.0D),
                knifeAt.add(0.0D, 6.0D, 0.0D), Easing.SPRING_OUT);
        // Whip across to the carton while it is still in the air.
        rig.dolly(103, 26, 8, o.add(-48.0D, 18.0D, -42.0D), o.add(-8.0D, 14.0D, -54.0D),
                knifeAt.add(0.0D, 6.0D, 0.0D), boxAt.add(0.0D, 30.0D, 0.0D),
                Easing.CUBIC_IN_OUT);
        rig.dolly(129, 5, 2, o.add(-8.0D, 14.0D, -54.0D), o.add(-8.0D, 9.0D, -51.0D),
                boxAt.add(0.0D, 12.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(134, 26, 7, o.add(-8.0D, 9.0D, -51.0D), o.add(-4.0D, 20.0D, -58.0D),
                boxAt.add(0.0D, 14.0D, 0.0D), Easing.SPRING_OUT);
        rig.orbit(160, 62, 14, o, 60.0D, 38.0D, 24.0D, 12.0D, 262.0F, 348.0F,
                o.add(0.0D, 14.0D, 0.0D), Easing.SINE_IN_OUT);
        rig.dolly(216, 6, 3, ring(o, 348.0F, 38.0D, 12.0D), ring(o, 352.0F, 36.0D, 6.0D),
                o.add(0.0D, 8.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(222, 28, 8, ring(o, 352.0F, 36.0D, 6.0D), o.add(44.0D, 20.0D, -22.0D),
                o.add(0.0D, 8.0D, 0.0D), Easing.SPRING_OUT);
        rig.dolly(250, 18, 10, o.add(44.0D, 20.0D, -22.0D), o.add(62.0D, 34.0D, -46.0D),
                o.add(0.0D, 8.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(77.0F, 0.0F, Easing.LINEAR)
                .key(81.0F, 10.0F, Easing.CUBIC_IN)
                .key(104.0F, -2.0F, Easing.SPRING_OUT)
                .key(133.0F, 0.0F, Easing.LINEAR)
                .key(137.0F, -12.0F, Easing.CUBIC_IN)
                .key(160.0F, 2.0F, Easing.SPRING_OUT)
                .key(219.0F, 0.0F, Easing.LINEAR)
                .key(223.0F, 14.0F, Easing.CUBIC_IN)
                .key(250.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(74.0F, 62.0F, Easing.CUBIC_IN)
                .key(80.0F, 98.0F, Easing.EXPO_OUT)
                .key(103.0F, 72.0F, Easing.SPRING_OUT)
                .key(130.0F, 64.0F, Easing.CUBIC_IN)
                .key(136.0F, 100.0F, Easing.EXPO_OUT)
                .key(160.0F, 74.0F, Easing.SPRING_OUT)
                .key(218.0F, 66.0F, Easing.CUBIC_IN)
                .key(224.0F, 104.0F, Easing.EXPO_OUT)
                .key(248.0F, 70.0F, Easing.SPRING_OUT)
                .hold(end));

        tl.cue(2, sound(o, ModSounds.KRAVE_SIREN, 1.5F, 0.7F));
        tl.cue(58, sound(knifeAt.add(0.0D, 80.0D, 0.0D), ModSounds.KRAVE_VOICE, 1.1F, 0.55F));
        tl.cue(112, sound(boxAt.add(0.0D, 100.0D, 0.0D), ModSounds.KRAVE_RUMBLE, 1.5F, 0.6F));
        tl.cue(140, sound(o, ModSounds.EVT_MCD, 1.3F, 1.0F));
        tl.cue(220, sound(o, ModSounds.KRAVE_ROAR, 1.8F, 0.75F));
        tl.cue(246, sound(o, ModSounds.KRAVE_LAUGH, 1.2F, 0.7F));

        return scene;
    }

    // ------------------------------------------------------------------------
    // STAGE 9: THE MANAGER
    // He walks in from the horizon and he is the entire cast. No props, no
    // second giant, nothing falls until the last twenty ticks - after the
    // barrage, the most frightening thing left is one figure and a long silence.
    // ------------------------------------------------------------------------

    private static Scene theManager(Vec3 o, Vec3 eye) {
        final int end = 250;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 walkFrom = o.add(-124.0D, 0.0D, -92.0D);
        Vec3 walkTo = o.add(-15.0D, 0.0D, -20.0D);
        Vec3 mgrHead = walkTo.add(0.0D, 22.0D, 0.0D);
        Vec3 knifeAt = o.add(1.0D, 0.0D, 3.0D);

        // --- the Manager ----------------------------------------------------
        ManagerActor mgr = new ManagerActor();
        mgr.bind(tl, "mgr", walkTo, facing(walkTo, o), 9.5F);
        tl.track("mgr.dx", Track.from(0.0F, (float) (walkFrom.x - walkTo.x))
                .key(96.0F, 0.0F, Easing.SINE_IN_OUT).hold(end));
        tl.track("mgr.dz", Track.from(0.0F, (float) (walkFrom.z - walkTo.z))
                .key(96.0F, 0.0F, Easing.SINE_IN_OUT).hold(end));
        tl.track("mgr.walk", Track.from(0.0F, 1.0F)
                .key(94.0F, 1.0F, Easing.LINEAR)
                .key(95.0F, 0.0F, Easing.STEP).hold(end));
        // He leans AWAY before the head comes down. Nothing telegraphs a threat
        // like a body moving in the wrong direction first.
        tl.track("mgr.lean", Track.from(94.0F, 0.0F)
                .key(106.0F, -14.0F, Easing.BACK_IN)
                .key(116.0F, 6.0F, Easing.BACK_OUT)
                .key(134.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("mgr.headPitch", Track.from(94.0F, 0.0F)
                .key(107.0F, -16.0F, Easing.CUBIC_IN_OUT)
                .key(116.0F, 30.0F, Easing.BACK_OUT)
                .key(128.0F, 20.0F, Easing.SPRING_OUT)
                .hold(196.0F)
                .key(214.0F, 32.0F));
        tl.track("mgr.headYaw", Track.from(0.0F, 0.0F)
                .hold(140.0F)
                // He looks at the camera. Not at the world, at the lens.
                .key(158.0F, 26.0F, Easing.BACK_OUT)
                .key(200.0F, 4.0F)
                .hold(end));
        tl.track("mgr.jaw", Track.from(112.0F, 0.0F)
                .key(118.0F, 26.0F, Easing.ELASTIC_OUT)
                .key(136.0F, 3.0F)
                .key(146.0F, 20.0F, Easing.ELASTIC_OUT)
                .key(164.0F, 2.0F).hold(end));
        // One arm, raised late and slowly. The cleaver comes down on the tick it
        // finishes, so his arm is the countdown.
        tl.track("mgr.armR", Track.from(168.0F, 4.0F)
                .key(202.0F, -158.0F, Easing.BACK_OUT)
                .hold(210.0F)
                .key(218.0F, -104.0F, Easing.CUBIC_IN)
                .key(240.0F, -128.0F, Easing.SPRING_OUT).hold(end));
        tl.track("mgr.elbowR", Track.from(168.0F, 6.0F)
                .key(202.0F, 22.0F, Easing.BACK_OUT).hold(end));
        tl.track("mgr.armSpreadR", Track.from(168.0F, -3.0F).key(202.0F, -24.0F).hold(end));
        tl.track("mgr.breathe", Track.constant(1.0F).wobble(0.018F, 44.0F, 0.0F, 0.0F));

        // --- the one thing he calls down --------------------------------------
        CleaverActor cleaver = new CleaverActor();
        cleaver.bind(tl, "knife", knifeAt, 28.0F, 19.0F);
        cleaver.groundY = o.y;
        tl.track("knife.show", Track.from(0.0F, 0.0F).key(196.0F, 1.0F, Easing.STEP).hold(end));
        tl.track("knife.dy", Track.from(196.0F, 88.0F)
                .key(210.0F, 96.0F, Easing.CUBIC_OUT)
                .hold(214.0F)
                .key(220.0F, 0.4F, Easing.QUINT_IN)
                .key(223.0F, -2.8F, Easing.CUBIC_OUT)
                .key(231.0F, 1.8F, Easing.BACK_OUT)
                .key(246.0F, 0.9F, Easing.SPRING_OUT).hold(end));
        tl.track("knife.tilt", Track.from(196.0F, 0.0F)
                .hold(214.0F)
                .key(220.0F, -7.0F, Easing.CUBIC_IN)
                .key(225.0F, 4.0F, Easing.BACK_OUT)
                .key(242.0F, 0.0F, Easing.SPRING_OUT));
        tl.track("knife.heat", Track.from(196.0F, 0.2F)
                .hold(218.0F)
                .key(221.0F, 2.0F, Easing.STEP)
                .key(242.0F, 0.3F, Easing.CUBIC_OUT).hold(end));
        tl.track("knife.dust", Track.from(220.0F, 0.0F).key(end, 1.0F, Easing.LINEAR));

        // --- camera ---------------------------------------------------------
        // Ninety ticks of one almost-static frame on a long lens. Everything the
        // barrage did with cuts, this does by refusing to move.
        rig.dolly(0, 94, 0, eye, o.add(10.0D, 3.2D, -50.0D), o.add(0.0D, 3.0D, 0.0D),
                walkTo.add(0.0D, 13.0D, 0.0D), Easing.CUBIC_IN_OUT);
        // Whip up the length of him, feet to face.
        rig.dolly(94, 14, 5, o.add(10.0D, 3.2D, -50.0D), o.add(2.0D, 2.0D, -44.0D),
                walkTo.add(0.0D, 1.0D, 0.0D), mgrHead, Easing.QUINT_IN_OUT);
        rig.hold(108, 22, 5, o.add(2.0D, 2.0D, -44.0D), mgrHead);
        // Creep in on the face. Two blocks over twenty ticks - barely a move, and
        // that is exactly why it is unbearable.
        rig.dolly(130, 40, 12, o.add(2.0D, 2.0D, -44.0D), o.add(1.0D, 4.0D, -42.0D),
                mgrHead, Easing.SINE_IN_OUT);
        rig.dolly(170, 44, 14, o.add(1.0D, 4.0D, -42.0D), o.add(-14.0D, 26.0D, -58.0D),
                mgrHead, o.add(0.0D, 24.0D, 0.0D), Easing.CUBIC_IN_OUT);
        rig.dolly(214, 6, 2, o.add(-14.0D, 26.0D, -58.0D), o.add(-14.0D, 19.0D, -55.0D),
                o.add(0.0D, 12.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(220, 30, 8, o.add(-14.0D, 19.0D, -55.0D), o.add(-22.0D, 30.0D, -66.0D),
                o.add(0.0D, 10.0D, 0.0D), Easing.SPRING_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(94.0F, 2.6F, Easing.SINE_IN_OUT)
                .key(130.0F, -1.4F)
                .key(219.0F, 0.0F, Easing.LINEAR)
                .key(223.0F, 13.0F, Easing.CUBIC_IN)
                .key(248.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                // The narrowest lens in the show. He gets no wide shot at all until
                // the blade arrives.
                .key(40.0F, 52.0F, Easing.CUBIC_IN_OUT)
                .key(108.0F, 46.0F)
                .key(170.0F, 62.0F, Easing.CUBIC_IN_OUT)
                .key(216.0F, 58.0F, Easing.CUBIC_IN)
                .key(224.0F, 104.0F, Easing.EXPO_OUT)
                .key(246.0F, 72.0F, Easing.SPRING_OUT)
                .hold(end));

        // Footfalls shake from where he actually is at that moment: the distance
        // falloff makes them grow as he closes, with no extra authoring.
        final int[] steps = { 14, 38, 62, 86 };
        for (int i = 0; i < steps.length; i++) {
            Vec3 where = walkPoint(walkFrom, walkTo, steps[i] / 96.0F);
            rig.shake(steps[i], 18, 1.7F, 1.7F, where, 34.0D);
            tl.cue(steps[i], sound(where, ModSounds.KRAVE_BOOM, 0.8F + i * 0.5F, 0.30F));
            tl.cue(steps[i], burst(where.add(0.0D, 0.5D, 0.0D), ParticleTypes.LARGE_SMOKE,
                    34, 4.0D, 0.25D));
        }
        rig.shake(116, 18, 0.9F, 2.2F, mgrHead, 46.0D);
        rig.shake(220, 44, 5.4F, 2.4F, knifeAt, 46.0D);
        rig.shake(230, 24, 1.4F, 1.8F, knifeAt, 60.0D);

        tl.cue(2, sound(o, ModSounds.KRAVE_SIREN, 1.2F, 0.55F));
        tl.cue(116, sound(mgrHead, ModSounds.EVT_MANAGER, 1.5F, 1.0F));
        tl.cue(146, sound(mgrHead, ModSounds.KRAVE_VOICE, 1.3F, 0.6F));
        tl.cue(196, sound(o, ModSounds.KRAVE_RUMBLE, 1.4F, 0.5F));
        tl.cue(220, sound(knifeAt, ModSounds.KRAVE_BOOM, 4.0F, 0.4F));
        tl.cue(220, burst(knifeAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 34, 12.0D, 0.2D));
        tl.cue(221, burst(knifeAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE,
                170, 18.0D, 0.6D));
        tl.cue(238, sound(o, ModSounds.EVT_DEMOCRAT, 1.3F, 1.0F));

        return new Scene("The Manager", o, tl, rig, 36).add(mgr).add(cleaver);
    }

    // ------------------------------------------------------------------------
    // STAGE 10: KRAVE ARMAGEDDON
    // The Manager walks in from the horizon in near silence, turns, and calls
    // every other actor down at once. Stage 9 was him alone with one blade; this
    // is the same walk answered by the entire cast, and it is the only script
    // where four things hit the ground on the same tick.
    // ------------------------------------------------------------------------

    private static Scene armageddon(Vec3 o, Vec3 eye) {
        final int end = 300;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 walkFrom = o.add(-98.0D, 0.0D, -72.0D);
        Vec3 walkTo = o.add(-17.0D, 0.0D, -23.0D);
        Vec3 mgrHead = walkTo.add(0.0D, 21.0D, 0.0D);
        Vec3 cleaverAt = o.add(-2.0D, 0.0D, 7.0D);
        Vec3 boxAt = o.add(27.0D, 0.0D, -15.0D);
        Vec3 smokerAt = o.add(48.0D, -3.0D, 60.0D);

        // --- camera ---------------------------------------------------------
        // Sixty-six ticks of almost nothing. The stillness IS the anticipation.
        rig.dolly(0, 66, 0, eye, o.add(6.0D, 3.5D, -46.0D), o.add(0.0D, 3.0D, 0.0D),
                walkTo.add(0.0D, 12.0D, 0.0D), Easing.CUBIC_IN_OUT);
        // Whip up the length of him, feet to face.
        rig.dolly(66, 14, 5, o.add(6.0D, 3.5D, -46.0D), o.add(-1.0D, 2.0D, -41.0D),
                walkTo.add(0.0D, 1.0D, 0.0D), mgrHead, Easing.QUINT_IN_OUT);
        rig.hold(80, 16, 4, o.add(-1.0D, 2.0D, -41.0D), mgrHead);
        rig.dolly(96, 54, 12, o.add(-1.0D, 2.0D, -41.0D), o.add(-6.0D, 31.0D, -68.0D),
                mgrHead, o.add(0.0D, 26.0D, 0.0D), Easing.CUBIC_IN_OUT);
        rig.dolly(150, 5, 3, o.add(-6.0D, 31.0D, -68.0D), o.add(-6.0D, 21.0D, -63.0D),
                o.add(0.0D, 12.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(155, 32, 8, o.add(-6.0D, 21.0D, -63.0D), o.add(-9.0D, 35.0D, -74.0D),
                o.add(0.0D, 20.0D, 0.0D), Easing.SPRING_OUT);
        rig.orbit(192, 76, 18, o, 62.0D, 48.0D, 27.0D, 16.0D, 252.0F, 334.0F,
                o.add(0.0D, 14.0D, 0.0D), Easing.SINE_IN_OUT);
        // And the sky opens one more time on the way out.
        rig.dolly(268, 6, 3, ring(o, 334.0F, 48.0D, 16.0D), ring(o, 337.0F, 46.0D, 10.0D),
                o.add(0.0D, 10.0D, 0.0D), Easing.QUINT_IN);
        rig.dolly(274, 26, 8, ring(o, 337.0F, 46.0D, 10.0D), o.add(58.0D, 34.0D, -34.0D),
                o.add(0.0D, 10.0D, 0.0D), Easing.SPRING_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(62.0F, 3.2F, Easing.SINE_IN_OUT)
                .key(96.0F, -2.0F)
                .key(150.0F, 0.0F)
                .key(153.0F, 13.5F, Easing.CUBIC_IN)
                .key(186.0F, 0.0F, Easing.SPRING_OUT)
                .key(271.0F, 0.0F, Easing.LINEAR)
                .key(275.0F, 15.0F, Easing.CUBIC_IN)
                .key(end, 0.0F, Easing.SPRING_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(66.0F, 62.0F, Easing.CUBIC_IN_OUT)
                .key(80.0F, 57.0F)
                .key(96.0F, 70.0F)
                .key(142.0F, 93.0F, Easing.CUBIC_IN_OUT)
                .key(150.0F, 86.0F)
                .key(154.0F, 105.0F, Easing.EXPO_OUT)
                .key(186.0F, 74.0F, Easing.SPRING_OUT)
                .key(270.0F, 66.0F, Easing.CUBIC_IN)
                .key(276.0F, 108.0F, Easing.EXPO_OUT)
                .key(end, 70.0F, Easing.SPRING_OUT));

        // Footfalls shake from where he actually is at that moment: the distance
        // falloff makes them grow as he closes, with no extra authoring.
        final int[] steps = { 11, 30, 49, 68 };
        for (int step : steps) {
            rig.shake(step, 16, 1.5F, 1.7F, walkPoint(walkFrom, walkTo, step / 72.0F), 34.0D);
        }
        rig.shake(86, 18, 0.8F, 2.2F, mgrHead, 46.0D);
        rig.shake(150, 44, 5.0F, 2.3F, o, 48.0D);
        rig.shake(160, 26, 1.4F, 1.8F, o, 62.0D);
        rig.shake(200, 86, 0.18F, 1.3F, null, 0.0D);

        // --- the Manager ----------------------------------------------------
        ManagerActor mgr = new ManagerActor();
        mgr.bind(tl, "mgr", walkTo, facing(walkTo, o), 9.0F);
        tl.track("mgr.dx", Track.from(0.0F, (float) (walkFrom.x - walkTo.x))
                .key(72.0F, 0.0F, Easing.SINE_IN_OUT).hold(end));
        tl.track("mgr.dz", Track.from(0.0F, (float) (walkFrom.z - walkTo.z))
                .key(72.0F, 0.0F, Easing.SINE_IN_OUT).hold(end));
        tl.track("mgr.walk", Track.from(0.0F, 1.0F)
                .key(70.0F, 1.0F, Easing.LINEAR)
                .key(71.0F, 0.0F, Easing.STEP).hold(end));
        // He leans AWAY before the head comes down. Nothing telegraphs a threat
        // like a body moving in the wrong direction first.
        tl.track("mgr.lean", Track.from(70.0F, 0.0F)
                .key(79.0F, -13.0F, Easing.BACK_IN)
                .key(87.0F, 5.0F, Easing.BACK_OUT)
                .key(102.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("mgr.headPitch", Track.from(70.0F, 0.0F)
                .key(80.0F, -15.0F, Easing.CUBIC_IN_OUT)
                .key(87.0F, 27.0F, Easing.BACK_OUT)
                .key(97.0F, 18.0F, Easing.SPRING_OUT)
                .hold(150.0F)
                .key(164.0F, 31.0F));
        tl.track("mgr.jaw", Track.from(84.0F, 0.0F)
                .key(89.0F, 27.0F, Easing.ELASTIC_OUT)
                .key(104.0F, 4.0F)
                .key(113.0F, 21.0F, Easing.ELASTIC_OUT)
                .key(128.0F, 2.0F).hold(end));
        // Overlapping action: the left arm is five ticks behind the right the
        // whole way up. Perfectly synchronised limbs read as machinery.
        tl.track("mgr.armR", Track.from(96.0F, 4.0F)
                .key(121.0F, -162.0F, Easing.BACK_OUT)
                .hold(150.0F)
                .key(158.0F, -132.0F, Easing.CUBIC_IN)
                .key(178.0F, -152.0F, Easing.SPRING_OUT).hold(end));
        tl.track("mgr.armL", Track.from(101.0F, 4.0F)
                .key(126.0F, -162.0F, Easing.BACK_OUT)
                .hold(150.0F)
                .key(163.0F, -132.0F, Easing.CUBIC_IN)
                .key(183.0F, -152.0F, Easing.SPRING_OUT).hold(end));
        tl.track("mgr.elbowR", Track.from(96.0F, 6.0F).key(121.0F, 24.0F, Easing.BACK_OUT).hold(end));
        tl.track("mgr.elbowL", Track.from(101.0F, 6.0F).key(126.0F, 24.0F, Easing.BACK_OUT).hold(end));
        tl.track("mgr.armSpreadR", Track.from(96.0F, -3.0F).key(121.0F, -26.0F).hold(end));
        tl.track("mgr.armSpreadL", Track.from(101.0F, 3.0F).key(126.0F, 26.0F).hold(end));
        tl.track("mgr.breathe", Track.constant(1.0F).wobble(0.02F, 44.0F, 0.0F, 0.0F));

        // --- everything he calls down ---------------------------------------
        CleaverActor cleaver = new CleaverActor();
        cleaver.bind(tl, "knife", cleaverAt, 32.0F, 18.0F);
        cleaver.groundY = o.y;
        tl.track("knife.show", Track.from(0.0F, 0.0F).key(110.0F, 1.0F, Easing.STEP).hold(end));
        tl.track("knife.dy", Track.from(110.0F, 54.0F)
                .key(132.0F, 72.0F, Easing.CUBIC_OUT)
                .hold(146.0F)
                .key(152.0F, 0.4F, Easing.QUINT_IN)
                .key(155.0F, -2.6F, Easing.CUBIC_OUT)
                .key(163.0F, 1.8F, Easing.BACK_OUT)
                .key(180.0F, 0.9F, Easing.SPRING_OUT).hold(end));
        tl.track("knife.tilt", Track.from(110.0F, 0.0F)
                .hold(146.0F)
                .key(152.0F, -7.0F, Easing.CUBIC_IN)
                .key(157.0F, 4.0F, Easing.BACK_OUT)
                .key(174.0F, 0.0F, Easing.SPRING_OUT)
                .wobble(2.6F, 5.0F, 0.0F, 40.0F));
        tl.track("knife.heat", Track.from(110.0F, 0.25F)
                .hold(150.0F)
                .key(153.0F, 2.0F, Easing.STEP)
                .key(174.0F, 0.3F, Easing.CUBIC_OUT).hold(end));
        tl.track("knife.dust", Track.from(152.0F, 0.0F).key(206.0F, 1.0F, Easing.LINEAR));

        BoxTitanActor box = new BoxTitanActor();
        box.bind(tl, "box", boxAt, -24.0F, 15.0F);
        box.groundY = o.y;
        tl.track("box.dy", Track.from(0.0F, 104.0F)
                .hold(112.0F)
                .key(128.0F, 112.0F, Easing.ANTICIPATE)
                .key(153.0F, 0.0F, Easing.EXPO_IN).hold(end));
        tl.track("box.spin", Track.from(0.0F, 0.0F)
                .key(128.0F, 22.0F, Easing.SINE_IN_OUT)
                .key(153.0F, 226.0F, Easing.EXPO_IN)
                .key(172.0F, 234.0F, Easing.SPRING_OUT).hold(end));
        tl.track("box.squash", Track.from(0.0F, 1.0F)
                .hold(152.0F)
                .key(153.0F, 1.0F, Easing.STEP)
                .key(156.0F, 0.58F, Easing.QUINT_OUT)
                .key(162.0F, 1.16F, Easing.BACK_OUT)
                .key(174.0F, 0.97F, Easing.SPRING_OUT)
                .key(192.0F, 1.0F, Easing.SPRING_OUT).hold(end));
        tl.track("box.flap", Track.from(0.0F, 2.0F)
                .hold(153.0F)
                .key(160.0F, 130.0F, Easing.BACK_OUT)
                .key(172.0F, 86.0F, Easing.SPRING_OUT)
                .key(190.0F, 98.0F, Easing.SINE_IN_OUT).hold(end));
        tl.track("box.spill", Track.from(0.0F, 0.0F)
                .hold(156.0F)
                .key(157.0F, 0.02F, Easing.STEP)
                .key(258.0F, 1.0F, Easing.CUBIC_OUT).hold(end));

        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", smokerAt, facing(smokerAt, o), 15.0F);
        tl.track("barb.dy", Track.from(0.0F, -37.0F)
                .hold(94.0F)
                .key(134.0F, 0.0F, Easing.QUINT_OUT).hold(end));
        tl.track("barb.headPitch", Track.from(0.0F, 30.0F)
                .key(140.0F, -14.0F, Easing.BACK_OUT)
                .key(152.0F, -4.0F, Easing.SPRING_OUT)
                .hold(254.0F)
                .key(end, 26.0F));
        tl.track("barb.armR", Track.from(94.0F, 6.0F).key(138.0F, -95.0F, Easing.BACK_OUT).hold(end));
        tl.track("barb.elbowR", Track.from(94.0F, 8.0F).key(138.0F, 120.0F, Easing.BACK_OUT).hold(end));
        tl.track("barb.jaw", Track.from(140.0F, 0.0F)
                .key(150.0F, 34.0F, Easing.ELASTIC_OUT)
                .key(162.0F, 12.0F)
                .key(170.0F, 32.0F, Easing.ELASTIC_OUT)
                .key(182.0F, 10.0F)
                .key(190.0F, 30.0F, Easing.ELASTIC_OUT)
                .key(206.0F, 0.0F).hold(end));
        tl.track("barb.ember", Track.from(94.0F, 0.3F)
                .key(146.0F, 1.4F, Easing.EXPO_OUT)
                .key(160.0F, 0.5F)
                .key(172.0F, 1.2F)
                .key(190.0F, 1.1F)
                .key(end, 0.2F));
        tl.track("barb.ring0", Track.from(150.0F, 0.0F).key(214.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring1", Track.from(170.0F, 0.0F).key(234.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring2", Track.from(190.0F, 0.0F).key(254.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.03F, 49.0F, 0.0F, 0.0F));

        Scene scene = new Scene("KRAVE ARMAGEDDON", o, tl, rig, 40)
                .add(mgr).add(cleaver).add(box).add(she);

        // Three blades of grass come down through the aftermath, thrown from
        // behind the camera. Stage 9 ends on one blade; this one keeps going after
        // the big hit, which is the only structural difference the eye can feel.
        final float[] fromAt = { 160.0F, 40.0F, 280.0F };
        final float[] toAt = { 340.0F, 220.0F, 100.0F };
        final double[] toIn = { 30.0D, 20.0D, 9.0D };
        final float[] bladeLen = { 34.0F, 40.0F, 54.0F };
        final int[] letGo = { 214, 236, 258 };
        final int[] land = { 236, 256, 272 };

        for (int i = 0; i < 3; i++) {
            String id = "blade" + i;
            Vec3 from = ring(o, fromAt[i], 84.0D, 104.0D + i * 14.0D);
            Vec3 to = ring(o, toAt[i], toIn[i], 0.0D);
            GrassBladeActor blade = new GrassBladeActor(bladeLen[i]);
            blade.bind(tl, id, to, facing(to, from), 1.0F);
            spear(tl, id, from, to, letGo[i] - 18, letGo[i], land[i], end,
                    3 + i, (i % 2 == 0 ? 15.0F : -19.0F));
            impact(tl, rig, to, land[i], 2.2F + i * 0.6F);
            scene.add(blade);
        }

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_SIREN, 1.4F, 0.8F));
        for (int i = 0; i < steps.length; i++) {
            Vec3 where = walkPoint(walkFrom, walkTo, steps[i] / 72.0F);
            tl.cue(steps[i], sound(where, ModSounds.KRAVE_BOOM, 0.9F + i * 0.45F, 0.32F));
            tl.cue(steps[i], burst(where.add(0.0D, 0.5D, 0.0D), ParticleTypes.LARGE_SMOKE,
                    30, 4.0D, 0.25D));
        }
        tl.cue(86, sound(mgrHead, ModSounds.EVT_MANAGER, 1.4F, 1.0F));
        tl.cue(100, sound(mgrHead, ModSounds.KRAVE_VOICE, 1.2F, 0.7F));
        tl.cue(128, sound(o, ModSounds.KRAVE_RUMBLE, 1.5F, 0.6F));
        tl.cue(152, sound(o, ModSounds.KRAVE_BOOM, 4.0F, 0.45F));
        tl.cue(152, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 40, 18.0D, 0.2D));
        tl.cue(153, burst(o.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE, 200, 24.0D, 0.7D));
        tl.cue(158, sound(o, ModSounds.KRAVE_ROAR, 1.6F, 0.85F));
        tl.cue(204, sound(o, ModSounds.EVT_HOUSE, 1.3F, 1.0F));
        tl.cue(272, sound(o, ModSounds.KRAVE_LAUGH, 1.2F, 0.7F));

        // The full-size gates, over the biggest stage in the mod.
        scene.add(hellGate(tl, o, 88.0F, 8, 44, 66, end));
        return scene;
    }

    // ---- authoring helpers -------------------------------------------------

    /** The yaw that turns an actor's front (+Z) toward a world point. */
    /**
     * Hangs the gates of hell over the death site and grinds them open.
     *
     * <p>These used to be one flat textured quad about a hundred blocks across,
     * eighty blocks up. The transform was right, but a single sheet that size has
     * nothing on it nearer the viewer than anything else, so no camera move
     * changes its silhouette and the eye files it as an overlay stuck to the
     * lens. {@link HellGateActor} is a real structure - frame, arch, a hanging
     * colonnade - which is what gives the parallax that sells it as being up
     * there in the world.
     *
     * <p>{@code open} is driven LINEAR on purpose: the leaves carry their own
     * weight curve internally and are hinged on their outer edges, so the gap is
     * already the versine of the swing. Easing this track as well double-eases it
     * and the doors crawl.
     */
    private static HellGateActor hellGate(Timeline tl, Vec3 o, float span,
                                          int arrive, int grind, int part, int end) {
        HellGateActor gate = new HellGateActor(span);
        Vec3 at = o.add(0.0D, 78.0D, 0.0D);
        gate.bind(tl, "gate", at, 0.0F, 1.0F);

        // It lowers into frame listing, and levels off as it settles.
        tl.track("gate.descend", Track.from((float) arrive, 0.0F)
                .key(arrive + 40.0F, 1.0F, Easing.QUINT_OUT)
                .hold(end));
        // The grind starts before anything moves - the sound of strain first.
        tl.track("gate.shudder", Track.from((float) grind, 0.0F)
                .key(grind + 10.0F, 1.0F, Easing.CUBIC_OUT)
                .key(part + 70.0F, 0.35F, Easing.SINE_IN_OUT)
                .hold(end));
        tl.track("gate.open", Track.from((float) part, 0.0F)
                .key(part + 74.0F, 1.0F, Easing.LINEAR)
                .hold(end));
        tl.track("gate.glow", Track.constant(1.0F));

        tl.cue(grind, sound(at, ModSounds.KRAVE_RUMBLE, 2.0F, 0.5F));
        tl.cue(part, sound(at, ModSounds.KRAVE_BOOM, 2.4F, 0.4F));
        tl.cue(part + 74, sound(at, ModSounds.KRAVE_ROAR, 2.0F, 0.6F));
        return gate;
    }

    private static float facing(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(to.x - from.x, to.z - from.z));
    }

    /**
     * A model-space offset on an actor rotated into world space - the same yaw
     * transform CinematicActor.render applies, so a point worked out on the rig
     * lands where it does on screen.
     *
     * <p>This is how a prop gets put in a giant's hand. Placing it by eye in
     * world coordinates works right up until the giant is moved or turned, and
     * then the joint is floating six blocks from the fist with nothing in the
     * source to say why.
     */
    private static Vec3 local(Vec3 base, float yawDeg, double x, double y, double z) {
        double r = Math.toRadians(yawDeg);
        double s = Math.sin(r);
        double c = Math.cos(r);
        return base.add(x * c + z * s, y, -x * s + z * c);
    }

    /** A point on a circle around a centre - stations, orbits and landing spots. */
    private static Vec3 ring(Vec3 centre, float angleDeg, double radius, double height) {
        double a = Math.toRadians(angleDeg);
        return centre.add(Math.cos(a) * radius, height, Math.sin(a) * radius);
    }

    /** Where a walking actor stands at progress u, matching its track's easing. */
    private static Vec3 walkPoint(Vec3 from, Vec3 to, float u) {
        float e = Easing.SINE_IN_OUT.apply(u);
        return new Vec3(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e,
                from.z + (to.z - from.z) * e);
    }

    /**
     * One thrown blade of grass, from a hand in the sky to the point it buries
     * itself in. The blade must already be bound at {@code to}, because every
     * track here is an OFFSET from that: "it landed" is this reaching zero rather
     * than arithmetic against the blade's own length.
     *
     * <p>The horizontal is LINEAR and the vertical is EXPO_IN, which is the whole
     * of the ballistics. Easing both the same way is what makes a thrown object
     * look like it is being carried on a wire.
     *
     * @param coil    tick it materialises in her hand, at the start of the wind-up
     * @param release tick it leaves her hand
     * @param hit     tick it reaches the ground
     * @param turns   whole end-over-end tumbles on the way down
     * @param lean    degrees it is tipped, and the angle it ends up planted at
     */
    private static void spear(Timeline tl, String id, Vec3 from, Vec3 to,
                              int coil, int release, int hit, int end,
                              int turns, float lean) {
        float ox = (float) (from.x - to.x);
        float oy = (float) (from.y - to.y);
        float oz = (float) (from.z - to.z);
        float planted = turns * 360.0F + lean;

        tl.track(id + ".show", Track.from(0.0F, 0.0F).key(coil, 1.0F, Easing.STEP).hold(end));
        // It unfurls out of nothing while she is still coiling, so the audience
        // sees what is about to be thrown before it is thrown.
        tl.track(id + ".grow", Track.from(coil, 0.0F)
                .key(release - 2, 1.0F, Easing.CUBIC_OUT).hold(end));
        tl.track(id + ".dx", Track.from(0.0F, ox).hold(release)
                .key(hit, 0.0F, Easing.LINEAR).hold(end));
        tl.track(id + ".dz", Track.from(0.0F, oz).hold(release)
                .key(hit, 0.0F, Easing.LINEAR).hold(end));
        tl.track(id + ".dy", Track.from(0.0F, oy).hold(release)
                .key(hit, 0.0F, Easing.EXPO_IN)
                // It rebounds out of the hole and settles back into it. Something
                // this long does not stop on the frame it touches down.
                .key(hit + 5, 2.6F, Easing.CUBIC_OUT)
                .key(hit + 17, 0.5F, Easing.SPRING_OUT).hold(end));
        // A fast-changing spin is also what drives the whip in the blade's own
        // joints, so the tumble and the bend come off this single track.
        tl.track(id + ".spin", Track.from(0.0F, -38.0F).hold(release)
                .key(hit, planted, Easing.EXPO_IN)
                .key(hit + 10, planted - 9.0F, Easing.BACK_OUT).hold(end));
        tl.track(id + ".pitch", Track.from(0.0F, lean * 0.5F).hold(release)
                .key(hit, lean, Easing.CUBIC_IN_OUT)
                .key(hit + 14, lean * 0.72F, Easing.SPRING_OUT).hold(end));
        // Superheat on the way down, cooling in the ground afterwards. The tip
        // leads, because it is the thin end going first.
        tl.track(id + ".glow", Track.from(release, 0.06F)
                .key(hit - 4, 0.92F, Easing.CUBIC_IN)
                .key(hit, 1.0F, Easing.LINEAR)
                .key(hit + 30, 0.10F, Easing.CUBIC_OUT).hold(end));
    }

    /**
     * Everything a thing landing owes the audience: a shake fired from the exact
     * point of contact, a crater flash, and dust that is thrown up and then
     * dragged back down.
     *
     * @param power roughly 1 for a small hit and 3 for the end of the world
     */
    private static void impact(Timeline tl, CameraRig rig, Vec3 at, int hit, float power) {
        rig.shake(hit, Math.round(20.0F + 14.0F * power), 1.5F * power, 2.3F, at, 46.0D);
        rig.shake(hit + 7, 16, 0.5F * power, 1.8F, at, 62.0D);
        // A tick of low rumble BEFORE contact. The ear beats the eye to an impact
        // by about that much in anything that is really happening.
        tl.cue(hit - 1, sound(at, ModSounds.KRAVE_RUMBLE, 0.5F * power, 1.5F));
        tl.cue(hit, sound(at, ModSounds.KRAVE_BOOM, Math.min(4.0F, 1.3F * power),
                Math.max(0.35F, 0.75F - 0.09F * power)));
        tl.cue(hit, burst(at.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION,
                Math.round(10.0F + 11.0F * power), 3.0D + 3.0D * power, 0.2D));
        tl.cue(hit, burst(at.add(0.0D, 1.0D, 0.0D), ParticleTypes.FLAME,
                Math.round(18.0F + 26.0F * power), 2.0D + 3.0D * power, 0.55D));
        tl.cue(hit + 1, burst(at.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE,
                Math.round(40.0F + 55.0F * power), 4.0D + 6.0D * power, 0.45D));
    }

    private static Runnable sound(Vec3 at, RegistryObject<SoundEvent> event, float volume, float pitch) {
        return () -> play(at, event.get(), volume, pitch);
    }

    private static Runnable sound(Vec3 at, SoundEvent event, float volume, float pitch) {
        return () -> play(at, event, volume, pitch);
    }

    private static void play(Vec3 at, SoundEvent event, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.playLocalSound(at.x, at.y, at.z, event, SoundSource.MASTER, volume, pitch, false);
        }
    }

    private static Runnable burst(Vec3 at, ParticleOptions type, int count, double spread, double speed) {
        return () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            for (int i = 0; i < count; i++) {
                double dx = (mc.level.random.nextDouble() - 0.5D) * 2.0D * spread;
                double dy = mc.level.random.nextDouble() * spread * 0.6D;
                double dz = (mc.level.random.nextDouble() - 0.5D) * 2.0D * spread;
                mc.level.addParticle(type, at.x + dx, at.y + dy, at.z + dz,
                        dx * 0.02D * speed, 0.10D * speed, dz * 0.02D * speed);
            }
        };
    }
}
