package com.barbarajones.world;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.ApocalypseClient;
import com.barbarajones.content.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The ambient event director: the overworld sounding like these people are out
 * there somewhere, without ever showing you one of them.
 *
 * <p>Every few minutes, at most, one "beat" fires - a far-off "I KRAVE THE
 * KRAVE" carried on the wind, a car horn out past the treeline after dark, the
 * scrape of a lighter right behind your shoulder, a cat you cannot find.
 * Nothing here has any gameplay effect; it is purely the world being inhabited.
 *
 * <p>Three rules keep it from turning into noise:
 * <ul>
 *   <li><b>Rate limit.</b> One beat every two to six minutes, randomised, so it
 *       never settles into a rhythm you can predict or get sick of.</li>
 *   <li><b>No repeats.</b> The beat that just played is excluded from the next
 *       draw, so you never get the same cat twice running.</li>
 *   <li><b>Yield to the show.</b> Silent during the Krave Apocalypse, which has
 *       its own soundtrack, and silent outside the overworld - the Krave Kosmos
 *       is not a neighbourhood.</li>
 * </ul>
 *
 * <p>This is the counterpart to {@code DreadClient}: that one escalates with
 * how frightened you should be, this one does not escalate at all. Cosy is the
 * point.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class AmbientDirector {

    /** Ticks after entering a level before anything may fire - lets the world settle. */
    private static final int JOIN_GRACE_TICKS = 600;
    private static final int MIN_GAP_TICKS = 2400;      // two minutes
    private static final int GAP_SPREAD_TICKS = 4800;   // ...up to six

    /** When a beat is allowed to play. */
    private enum When { ANY, NIGHT }

    private enum Beat {
        KRAVE_ON_THE_WIND(6, When.ANY),
        STRAY_CAT(5, When.ANY),
        LIGHTER(4, When.ANY),
        BARBARA_MUTTERING(3, When.ANY),
        CAR_HORN(4, When.NIGHT),
        FAR_OFF_LAUGH(2, When.NIGHT);

        private final int weight;
        private final When when;

        Beat(int weight, When when) {
            this.weight = weight;
            this.when = when;
        }

        boolean allowed(boolean night) {
            return this.when == When.ANY || night;
        }
    }

    private static final Beat[] BEATS = Beat.values();

    private static int cooldown = MIN_GAP_TICKS;
    private static int joinGrace;
    private static ResourceKey<Level> lastLevel;
    private static Beat lastBeat;

    // one queued follow-up, so a beat can be two honks or a line after a click
    private static int echoTicks;
    private static SoundEvent echoSound;
    private static double echoX, echoY, echoZ;
    private static float echoVolume, echoPitch;

    private AmbientDirector() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            tick();
        } catch (Throwable ignored) {
            // atmosphere must never crash the game
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) {
            lastLevel = null;
            return;
        }

        ResourceKey<Level> here = mc.level.dimension();
        if (!here.equals(lastLevel)) {
            lastLevel = here;
            joinGrace = JOIN_GRACE_TICKS;
            echoTicks = 0;
            rearm(player.level().random);
        }
        if (joinGrace > 0) {
            joinGrace--;
            return;
        }

        // the apocalypse and the Kosmos both own the whole soundstage while
        // they are running; a stray cat over the top of either is just wrong,
        // and a follow-up already in flight gets dropped rather than queued
        if (ApocalypseClient.isActive() || !here.equals(Level.OVERWORLD)) {
            echoTicks = 0;
            echoSound = null;
            return;
        }

        if (echoTicks > 0 && --echoTicks == 0 && echoSound != null) {
            player.level().playLocalSound(echoX, echoY, echoZ, echoSound,
                    SoundSource.AMBIENT, echoVolume, echoPitch, false);
            echoSound = null;
        }

        if (--cooldown > 0) {
            return;
        }

        RandomSource random = player.level().random;
        rearm(random);

        long timeOfDay = player.level().getDayTime() % 24000L;
        boolean night = timeOfDay > 13000L && timeOfDay < 23000L;
        Beat beat = pick(random, night);
        if (beat != null) {
            lastBeat = beat;
            fire(beat, player, random);
        }
    }

    private static void rearm(RandomSource random) {
        cooldown = MIN_GAP_TICKS + random.nextInt(GAP_SPREAD_TICKS);
    }

    /** Weighted draw across everything allowed right now, minus whatever played last. */
    private static Beat pick(RandomSource random, boolean night) {
        int total = 0;
        for (Beat beat : BEATS) {
            if (beat != lastBeat && beat.allowed(night)) {
                total += beat.weight;
            }
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (Beat beat : BEATS) {
            if (beat == lastBeat || !beat.allowed(night)) {
                continue;
            }
            roll -= beat.weight;
            if (roll < 0) {
                return beat;
            }
        }
        return null;
    }

    private static void fire(Beat beat, Player player, RandomSource random) {
        switch (beat) {
            case KRAVE_ON_THE_WIND -> {
                SoundEvent voice = random.nextInt(3) == 0
                        ? ModSounds.KRAVE_VOICE.get() : ModSounds.CAYDEN_SHOUT.get();
                playOut(player, voice, 34.0D, 58.0D, 4.0F, 0.72F + random.nextFloat() * 0.12F, random);
            }
            case CAR_HORN -> {
                // No car-horn clip exists in this mod, but the siren cue pitched
                // up and cut into two short blasts lands as one - and it is the
                // right character (something with an engine, out of sight).
                double[] spot = playOut(player, ModSounds.KRAVE_SIREN.get(),
                        30.0D, 55.0D, 3.5F, 1.4F, random);
                queueEcho(ModSounds.KRAVE_SIREN.get(), spot, 3.5F, 1.4F, 11);
            }
            case LIGHTER -> {
                double[] spot = behind(player, 2.5D, 4.5D, random);
                player.level().playLocalSound(spot[0], spot[1], spot[2],
                        SoundEvents.FLINTANDSTEEL_USE, SoundSource.AMBIENT,
                        0.7F, 1.1F + random.nextFloat() * 0.25F, false);
                for (int i = 0; i < 4; i++) {
                    player.level().addParticle(
                            i == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                            spot[0] + (random.nextDouble() - 0.5D) * 0.4D,
                            spot[1] + random.nextDouble() * 0.3D,
                            spot[2] + (random.nextDouble() - 0.5D) * 0.4D,
                            0.0D, 0.02D, 0.0D);
                }
                if (random.nextInt(3) == 0) {
                    queueEcho(ModSounds.EVT_LIGHTER.get(), spot, 0.5F, 1.0F, 26);
                }
            }
            case STRAY_CAT -> {
                SoundEvent meow = random.nextBoolean()
                        ? SoundEvents.CAT_AMBIENT : SoundEvents.CAT_STRAY_AMBIENT;
                double[] spot = playOut(player, meow, 9.0D, 17.0D,
                        1.4F, 0.85F + random.nextFloat() * 0.35F, random);
                if (random.nextInt(3) == 0) {
                    queueEcho(SoundEvents.CAT_PURREOW, spot, 1.2F, 1.1F, 34);
                }
            }
            case BARBARA_MUTTERING ->
                playOut(player, ModSounds.BARBARA_IDLE.get(), 16.0D, 30.0D,
                        2.0F, 0.88F + random.nextFloat() * 0.14F, random);
            case FAR_OFF_LAUGH ->
                playOut(player, ModSounds.KRAVE_LAUGH.get(), 40.0D, 64.0D,
                        4.5F, 0.62F + random.nextFloat() * 0.12F, random);
            default -> { }
        }
    }

    /**
     * Place a sound on a random bearing around the player and play it.
     *
     * <p>Minecraft's audible radius is {@code max(volume, 1) * 16} blocks, so a
     * quiet sound dropped forty blocks out is simply never heard. Anything that
     * should read as far away has to be placed far away <em>and</em> played
     * loud - the distance is what does the attenuating, and the volume only
     * buys enough range for it to arrive at all.
     *
     * @return the position it was played at, for {@link #queueEcho}
     */
    private static double[] playOut(Player player, SoundEvent sound, double minDist, double maxDist,
                                    float volume, float pitch, RandomSource random) {
        double bearing = random.nextDouble() * Math.PI * 2.0D;
        double distance = minDist + random.nextDouble() * (maxDist - minDist);
        double[] spot = {
            player.getX() + Math.cos(bearing) * distance,
            player.getY() + random.nextDouble() * 8.0D - 2.0D,
            player.getZ() + Math.sin(bearing) * distance
        };
        player.level().playLocalSound(spot[0], spot[1], spot[2], sound,
                SoundSource.AMBIENT, volume, pitch, false);
        return spot;
    }

    /** A point just out of view behind the player's head. */
    private static double[] behind(Player player, double minDist, double maxDist, RandomSource random) {
        double distance = minDist + random.nextDouble() * (maxDist - minDist);
        double yaw = Math.toRadians(player.getYRot());
        // getYRot() is measured so that +Z is 0 degrees; negating the look
        // vector puts the point behind the player rather than in front
        return new double[] {
            player.getX() + Math.sin(yaw) * distance,
            player.getY() + 1.0D + (random.nextDouble() - 0.5D),
            player.getZ() - Math.cos(yaw) * distance
        };
    }

    private static void queueEcho(SoundEvent sound, double[] spot, float volume, float pitch, int delay) {
        echoSound = sound;
        echoX = spot[0];
        echoY = spot[1];
        echoZ = spot[2];
        echoVolume = volume;
        echoPitch = pitch;
        echoTicks = delay;
    }
}
