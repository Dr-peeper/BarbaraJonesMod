package com.barbarajones.client.title;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The mod's own track, playing across the whole front end.
 *
 * <p>Not just the title screen. Clicking Singleplayer should not drop you into
 * silence - the menus either side of it are part of the same moment, so the
 * music carries through the world list and the create-world screen and only
 * stops once you are actually in a world.
 *
 * <p>The instance is held rather than re-played per screen, which is the whole
 * difficulty here: every screen change constructs a new Screen object, and
 * starting a track on each one would restart the song from the top every time
 * you clicked Back. It starts once, keeps playing across screens, and is only
 * stopped when the front end is genuinely left behind.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveMenuMusic {

    private KraveMenuMusic() { }

    /**
     * Under vanilla's own menu music, because both will be asked to play and
     * two tracks at equal volume is just noise. Vanilla's is suppressed below.
     */
    private static final float VOLUME = 0.55F;

    private static SoundInstance playing;

    /** Whether this screen is part of the front end the music belongs to. */
    private static boolean isMenu(Screen screen) {
        // PauseScreen is deliberately excluded. It is a menu, but it is a menu
        // you reach from inside a world, and starting the title track over the
        // top of whatever is happening in that world would be absurd.
        return screen instanceof TitleScreen
                || screen instanceof SelectWorldScreen
                || screen instanceof CreateWorldScreen
                || (screen != null && !(screen instanceof PauseScreen)
                        && Minecraft.getInstance().level == null);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isMenu(event.getScreen())) {
            return;
        }
        // Already going: leave it alone. This is the case that matters - without
        // it, every click between menus would restart the song.
        if (playing != null && mc.getSoundManager().isActive(playing)) {
            return;
        }
        playing = SimpleSoundInstance.forUI(ModSounds.MUSIC_BET.get(), 1.0F, VOLUME);
        mc.getSoundManager().play(playing);
    }

    /**
     * Stops the track once a world is actually loaded.
     *
     * <p>Driven off the client tick rather than a screen-close event, because
     * there is no single event that means "the front end is over" - you can
     * leave it by joining a world, by a direct connect, or by the game
     * restoring a session, and only the presence of a level is true in all
     * three.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (playing == null) {
            return;
        }
        if (mc.level != null) {
            mc.getSoundManager().stop(playing);
            playing = null;
            return;
        }
        // Vanilla's menu music is stopped every tick it tries to start, rather
        // than once. The music manager re-decides on its own timer, so a single
        // stop would be quietly overridden a minute later and the two tracks
        // would end up layered.
        mc.getMusicManager().stopPlaying();
    }

    /** True while the mod's menu track is the thing playing. */
    public static boolean isPlaying() {
        return playing != null && Minecraft.getInstance().getSoundManager().isActive(playing);
    }
}
