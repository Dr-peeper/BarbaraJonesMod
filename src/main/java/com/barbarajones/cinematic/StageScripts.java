package com.barbarajones.cinematic;

import com.barbarajones.cinematic.actor.BoxTitanActor;
import com.barbarajones.cinematic.actor.CleaverActor;
import com.barbarajones.cinematic.actor.ManagerActor;
import com.barbarajones.cinematic.actor.SmokerActor;
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
 * <p>Four authored scripts cover all ten death stages. Adding another means
 * writing one more builder here and one line in {@link #forStage} - there is no
 * per-tick arithmetic anywhere in this file, only keys and cues.
 *
 * <p>Actor position tracks are named {@code dx/dy/dz} and are OFFSETS from where
 * the actor was bound, never absolute world coordinates. Tracks are float, and a
 * world coordinate out at a few million blocks has lost enough precision by then
 * to make a giant visibly stutter.
 */
public final class StageScripts {

    private StageScripts() { }

    /** Pick the script for this death stage. */
    public static Scene forStage(int stage, Vec3 origin, Vec3 playerEye) {
        if (stage <= 2) {
            return theCraving(origin, playerEye);
        }
        if (stage <= 5) {
            return theTorching(origin, playerEye);
        }
        if (stage <= 8) {
            return boxfall(origin, playerEye);
        }
        return armageddon(origin, playerEye);
    }

    // ------------------------------------------------------------------------
    // STAGES 1-2: THE CRAVING / THE O'S
    // She rises out of the ground, takes one drag, and blows three colossal smoke
    // rings straight down the lens. Small cast; all the weight is on timing.
    // ------------------------------------------------------------------------

    private static Scene theCraving(Vec3 o, Vec3 eye) {
        final int end = 132;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 barbara = o.add(0.0D, -2.0D, 34.0D);
        Vec3 head = barbara.add(0.0D, 28.0D, 0.0D);

        // --- camera ---------------------------------------------------------
        // Push in and crane onto her while she is still rising, so the move and
        // the reveal happen together and neither has to carry the shot alone.
        rig.dolly(0, 46, 0, eye, o.add(-2.0D, 6.0D, -12.0D), o.add(0.0D, 2.0D, 0.0D), head,
                Easing.CUBIC_IN_OUT);
        // The snap: twelve ticks, EXPO_OUT, straight at her face.
        rig.dolly(44, 12, 5, o.add(-2.0D, 6.0D, -12.0D), o.add(-1.0D, 7.5D, -6.0D), head,
                Easing.EXPO_OUT);
        // Then swing around her so the O's cross the lens with real parallax.
        rig.orbit(56, 46, 10, barbara, 34.0D, 25.0D, 19.0D, 26.0D, -108.0F, -46.0F, head,
                Easing.CUBIC_IN_OUT);
        rig.dolly(102, 30, 12, o.add(17.4D, 24.0D, 16.0D), o.add(-8.0D, 16.0D, -26.0D),
                head, o.add(0.0D, 3.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(56.0F, -3.6F, Easing.BACK_OUT)
                .key(102.0F, -1.2F)
                .key(end, 0.0F, Easing.SPRING_OUT));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(44.0F, 66.0F)
                .key(52.0F, 54.0F, Easing.EXPO_OUT)
                .key(64.0F, 75.0F, Easing.BACK_OUT)
                .key(102.0F, 70.0F)
                .hold(end));

        rig.shake(48, 16, 0.55F, 2.1F, head, 60.0D);
        rig.shake(90, 14, 0.30F, 1.7F, null, 0.0D);
        rig.shake(100, 14, 0.30F, 1.7F, null, 0.0D);
        rig.shake(110, 14, 0.32F, 1.7F, null, 0.0D);

        // --- the giant ------------------------------------------------------
        SmokerActor she = new SmokerActor();
        she.bind(tl, "barb", barbara, 180.0F, 13.0F);

        tl.track("barb.dy", Track.from(0.0F, -34.0F)
                .hold(8.0F)
                // QUINT_OUT: she comes up fast and settles under her own mass.
                .key(44.0F, 0.0F, Easing.QUINT_OUT)
                .hold(96.0F)
                .key(end, -30.0F, Easing.CUBIC_IN));

        // Head bowed, then the snap up with an overshoot that springs back.
        tl.track("barb.headPitch", Track.from(0.0F, 34.0F)
                .hold(40.0F)
                .key(48.0F, -17.0F, Easing.BACK_OUT)
                .key(58.0F, -6.0F, Easing.SPRING_OUT)
                .hold(98.0F)
                .key(126.0F, 28.0F));
        tl.track("barb.spine", Track.from(0.0F, 9.0F)
                .hold(40.0F)
                .key(48.0F, -7.0F, Easing.BACK_OUT)
                .key(58.0F, 0.0F, Easing.SPRING_OUT)
                .hold(100.0F)
                .key(end, 10.0F));

        // The joint reaches her mouth before anything else happens - that is the
        // tell that a drag is coming.
        tl.track("barb.armR", Track.from(0.0F, 6.0F)
                .key(24.0F, 2.0F)
                .key(42.0F, -95.0F, Easing.BACK_OUT)
                .hold(76.0F)
                .key(96.0F, -18.0F)
                .key(end, 6.0F));
        tl.track("barb.elbowR", Track.from(0.0F, 8.0F)
                .key(24.0F, 14.0F)
                .key(42.0F, 120.0F, Easing.BACK_OUT)
                .hold(76.0F)
                .key(96.0F, 30.0F)
                .key(end, 8.0F));
        tl.track("barb.armL", Track.from(0.0F, 5.0F).key(48.0F, 16.0F).key(end, 4.0F));

        // Jaw shut for the drag, then one pulse per ring.
        tl.track("barb.jaw", Track.from(0.0F, 0.0F)
                .hold(46.0F)
                .key(50.0F, 3.0F)
                .key(56.0F, 33.0F, Easing.ELASTIC_OUT)
                .key(63.0F, 16.0F)
                .key(68.0F, 31.0F, Easing.ELASTIC_OUT)
                .key(75.0F, 14.0F)
                .key(80.0F, 30.0F, Easing.ELASTIC_OUT)
                .key(88.0F, 9.0F)
                .key(104.0F, 0.0F));

        tl.track("barb.ember", Track.from(0.0F, 0.25F)
                .hold(40.0F)
                .key(50.0F, 1.4F, Easing.EXPO_OUT)
                .key(58.0F, 0.55F)
                .key(66.0F, 1.15F)
                .key(74.0F, 0.5F)
                .key(82.0F, 1.05F)
                .key(92.0F, 0.4F)
                .key(end, 0.18F));
        tl.track("barb.breathe", Track.constant(1.0F).wobble(0.028F, 47.0F, 0.0F, 0.0F));
        tl.track("barb.bob", Track.constant(0.0F).wobble(0.02F, 63.0F, 0.3F, 0.0F));

        // One track per ring: 0 the moment it leaves her, 1 when it is spent.
        tl.track("barb.ring0", Track.from(56.0F, 0.0F).key(106.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring1", Track.from(68.0F, 0.0F).key(118.0F, 1.0F, Easing.LINEAR));
        tl.track("barb.ring2", Track.from(80.0F, 0.0F).key(130.0F, 1.0F, Easing.LINEAR));

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 0.9F, 0.85F));
        tl.cue(40, sound(head, ModSounds.KRAVE_VOICE, 1.1F, 0.95F));
        tl.cue(48, sound(head, ModSounds.KRAVE_SCREECH, 0.9F, 1.05F));
        tl.cue(56, sound(head, ModSounds.EVT_OG, 1.3F, 1.0F));
        tl.cue(56, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE, 40, 5.0D, 0.35D));
        tl.cue(68, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE, 40, 5.0D, 0.35D));
        tl.cue(80, burst(head.add(0.0D, 0.0D, -5.0D), ParticleTypes.CAMPFIRE_COSY_SMOKE, 40, 5.0D, 0.35D));
        tl.cue(106, sound(o, ModSounds.KRAVE_LAUGH, 1.0F, 0.9F));

        return new Scene("The O's", o, tl, rig, 26).add(she);
    }

    // ------------------------------------------------------------------------
    // STAGES 3-5: THE TORCHING
    // The torch drops out of the sky, cocks, fires, sweeps the world - and then
    // the cleaver comes down on top of all of it.
    // ------------------------------------------------------------------------

    private static Scene theTorching(Vec3 o, Vec3 eye) {
        final int end = 202;
        Timeline tl = new Timeline(end);
        CameraRig rig = new CameraRig(70.0F);

        Vec3 nozzle = o.add(-4.0D, 30.0D, 8.0D);
        Vec3 burnPoint = o.add(-4.0D, 1.0D, 8.0D);
        Vec3 cleaverAt = o.add(3.0D, 0.0D, -4.0D);

        // --- camera ---------------------------------------------------------
        rig.dolly(0, 50, 0, eye, o.add(-26.0D, 4.0D, -20.0D), o.add(0.0D, 3.0D, 0.0D),
                o.add(-4.0D, 40.0D, 8.0D), Easing.QUINT_OUT);
        // Whip down to where the flame is about to land. Fourteen ticks: fast
        // enough to read as a reaction, slow enough not to be a cut.
        rig.dolly(50, 14, 5, o.add(-26.0D, 4.0D, -20.0D), o.add(-20.0D, 8.0D, -14.0D),
                o.add(-4.0D, 34.0D, 8.0D), burnPoint, Easing.CUBIC_IN_OUT);
        rig.orbit(64, 54, 10, o.add(-4.0D, 4.0D, 8.0D), 30.0D, 22.0D, 6.0D, 13.0D,
                200.0F, 302.0F, o.add(-4.0D, 14.0D, 8.0D), Easing.SINE_IN_OUT);
        // Reposition for the cleaver while it is still being hoisted.
        rig.dolly(118, 40, 12, o.add(7.7D, 17.0D, -10.7D), o.add(20.0D, 12.0D, -22.0D),
                o.add(-4.0D, 14.0D, 8.0D), o.add(3.0D, 24.0D, -4.0D), Easing.CUBIC_IN_OUT);
        // The plunge drags the camera down with it...
        rig.dolly(158, 4, 2, o.add(20.0D, 12.0D, -22.0D), o.add(20.0D, 5.5D, -21.0D),
                o.add(3.0D, 8.0D, -4.0D), Easing.QUINT_IN);
        // ...and the spring back is what puts the weight in.
        rig.dolly(162, 24, 8, o.add(20.0D, 5.5D, -21.0D), o.add(22.0D, 13.0D, -26.0D),
                o.add(3.0D, 6.0D, -4.0D), Easing.SPRING_OUT);
        rig.dolly(186, 16, 10, o.add(22.0D, 13.0D, -26.0D), o.add(31.0D, 19.0D, -35.0D),
                o.add(0.0D, 4.0D, 0.0D), Easing.CUBIC_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(52.0F, -2.6F, Easing.BACK_OUT)
                .key(100.0F, 1.6F, Easing.SINE_IN_OUT)
                .key(157.0F, 0.0F)
                .key(160.0F, 9.5F, Easing.CUBIC_IN)
                .key(182.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(44.0F, 64.0F)
                .key(53.0F, 89.0F, Easing.EXPO_OUT)
                .key(68.0F, 72.0F, Easing.BACK_OUT)
                .key(118.0F, 70.0F)
                .key(157.0F, 65.0F, Easing.CUBIC_IN)
                .key(161.0F, 97.0F, Easing.EXPO_OUT)
                .key(180.0F, 70.0F, Easing.SPRING_OUT)
                .hold(end));

        rig.shake(50, 22, 0.95F, 2.3F, nozzle, 55.0D);
        rig.shake(64, 56, 0.26F, 1.6F, null, 0.0D);
        rig.shake(159, 36, 3.4F, 2.4F, cleaverAt, 44.0D);
        rig.shake(168, 20, 0.75F, 1.9F, cleaverAt, 60.0D);

        // --- the torch ------------------------------------------------------
        TorchActor torch = new TorchActor();
        torch.bind(tl, "torch", nozzle, 0.0F, 9.0F);

        tl.track("torch.dy", Track.from(0.0F, 48.0F)
                .hold(8.0F)
                .key(44.0F, 0.0F, Easing.QUINT_OUT)
                // A hard stop is a bug. It recoils up, then settles.
                .key(52.0F, 4.0F, Easing.BACK_OUT)
                .key(62.0F, 1.0F, Easing.SPRING_OUT)
                .hold(120.0F)
                .key(148.0F, 56.0F, Easing.CUBIC_IN));
        // Anticipation: the trigger pulls the WRONG WAY first.
        tl.track("torch.trigger", Track.from(0.0F, 0.0F)
                .hold(36.0F)
                .key(45.0F, -24.0F, Easing.BACK_IN)
                .key(51.0F, 27.0F, Easing.ELASTIC_OUT)
                .hold(118.0F)
                .key(130.0F, 0.0F));
        tl.track("torch.flame", Track.from(0.0F, 0.0F)
                .hold(50.0F)
                .key(51.0F, 0.0F, Easing.STEP)
                .key(57.0F, 1.0F, Easing.EXPO_OUT)
                .key(64.0F, 0.84F, Easing.CUBIC_OUT)
                .key(73.0F, 1.0F, Easing.SPRING_OUT)
                .hold(114.0F)
                .key(126.0F, 0.0F, Easing.CUBIC_IN));
        tl.track("torch.sweep", Track.from(0.0F, 0.0F)
                .hold(58.0F)
                .key(66.0F, -48.0F, Easing.CUBIC_IN_OUT)
                .key(94.0F, 46.0F, Easing.SINE_IN_OUT)
                .key(118.0F, -20.0F, Easing.SINE_IN_OUT)
                .hold(end));
        tl.track("torch.pitch", Track.constant(0.0F).wobble(2.4F, 33.0F, 0.0F, 0.0F));

        // --- the cleaver ----------------------------------------------------
        CleaverActor cleaver = new CleaverActor();
        cleaver.bind(tl, "knife", cleaverAt, 24.0F, 16.0F);
        cleaver.groundY = o.y;

        tl.track("knife.show", Track.from(0.0F, 0.0F).key(116.0F, 1.0F, Easing.STEP).hold(end));
        tl.track("knife.dy", Track.from(116.0F, 50.0F)
                // Hoisted UP before it comes down. Everything heavy winds up.
                .key(136.0F, 66.0F, Easing.CUBIC_OUT)
                .hold(152.0F)
                .key(159.0F, 0.4F, Easing.QUINT_IN)
                .key(162.0F, -2.4F, Easing.CUBIC_OUT)
                .key(169.0F, 1.7F, Easing.BACK_OUT)
                .key(184.0F, 0.9F, Easing.SPRING_OUT)
                .hold(end));
        tl.track("knife.tilt", Track.from(116.0F, 0.0F)
                .hold(152.0F)
                .key(159.0F, -6.0F, Easing.CUBIC_IN)
                .key(163.0F, 3.5F, Easing.BACK_OUT)
                .key(178.0F, 0.0F, Easing.SPRING_OUT)
                // The quiver while it hangs, dying out as it steadies for the drop.
                .wobble(2.4F, 5.0F, 0.0F, 42.0F));
        tl.track("knife.spin", Track.from(116.0F, -20.0F)
                .hold(152.0F)
                .key(159.0F, 4.0F, Easing.CUBIC_IN)
                .hold(end));
        tl.track("knife.heat", Track.from(116.0F, 0.2F)
                .hold(157.0F)
                .key(160.0F, 1.9F, Easing.STEP)
                .key(178.0F, 0.3F, Easing.CUBIC_OUT)
                .hold(end));
        tl.track("knife.dust", Track.from(158.0F, 0.0F).key(198.0F, 1.0F, Easing.LINEAR));

        // --- score ----------------------------------------------------------
        tl.cue(2, sound(o, ModSounds.KRAVE_RUMBLE, 1.0F, 0.8F));
        tl.cue(42, sound(nozzle, ModSounds.EVT_LIGHTER, 1.3F, 1.0F));
        tl.cue(51, sound(nozzle, SoundEvents.FLINTANDSTEEL_USE, 2.4F, 0.5F));
        tl.cue(53, sound(burnPoint, ModSounds.KRAVE_SIREN, 1.1F, 1.1F));
        tl.cue(53, burst(burnPoint, ParticleTypes.FLAME, 90, 9.0D, 0.55D));
        tl.cue(130, sound(o, ModSounds.KRAVE_SCREECH, 1.2F, 0.85F));
        tl.cue(158, sound(cleaverAt, ModSounds.KRAVE_BOOM, 4.0F, 0.55F));
        tl.cue(159, burst(cleaverAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.EXPLOSION, 26, 7.0D, 0.2D));
        tl.cue(160, burst(cleaverAt.add(0.0D, 1.0D, 0.0D), ParticleTypes.LARGE_SMOKE, 120, 11.0D, 0.5D));
        tl.cue(186, sound(o, ModSounds.KRAVE_LAUGH, 1.0F, 0.75F));

        return new Scene("The Torching", o, tl, rig, 28).add(torch).add(cleaver);
    }

    // ------------------------------------------------------------------------
    // STAGES 6-8: BOXFALL
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
    // STAGES 9-10: KRAVE ARMAGEDDON
    // The Manager walks in from the horizon in near silence, turns, and calls
    // every other actor down at once.
    // ------------------------------------------------------------------------

    private static Scene armageddon(Vec3 o, Vec3 eye) {
        final int end = 286;
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
        rig.orbit(192, 94, 18, o, 62.0D, 48.0D, 27.0D, 16.0D, 252.0F, 334.0F,
                o.add(0.0D, 14.0D, 0.0D), Easing.SINE_IN_OUT);

        rig.roll(Track.from(0.0F, 0.0F)
                .key(62.0F, 3.2F, Easing.SINE_IN_OUT)
                .key(96.0F, -2.0F)
                .key(150.0F, 0.0F)
                .key(153.0F, 13.5F, Easing.CUBIC_IN)
                .key(186.0F, 0.0F, Easing.SPRING_OUT)
                .hold(end));
        rig.fov(Track.from(0.0F, 70.0F)
                .key(66.0F, 62.0F, Easing.CUBIC_IN_OUT)
                .key(80.0F, 57.0F)
                .key(96.0F, 70.0F)
                .key(142.0F, 93.0F, Easing.CUBIC_IN_OUT)
                .key(150.0F, 86.0F)
                .key(154.0F, 105.0F, Easing.EXPO_OUT)
                .key(186.0F, 74.0F, Easing.SPRING_OUT)
                .key(end, 70.0F));

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
                .hold(240.0F)
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
        tl.cue(250, sound(o, ModSounds.KRAVE_LAUGH, 1.2F, 0.7F));

        return new Scene("KRAVE ARMAGEDDON", o, tl, rig, 40)
                .add(mgr).add(cleaver).add(box).add(she);
    }

    // ---- authoring helpers -------------------------------------------------

    /** The yaw that turns an actor's front (+Z) toward a world point. */
    private static float facing(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(to.x - from.x, to.z - from.z));
    }

    /** Where a walking actor stands at progress u, matching its track's easing. */
    private static Vec3 walkPoint(Vec3 from, Vec3 to, float u) {
        float e = Easing.SINE_IN_OUT.apply(u);
        return new Vec3(from.x + (to.x - from.x) * e, from.y + (to.y - from.y) * e,
                from.z + (to.z - from.z) * e);
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
