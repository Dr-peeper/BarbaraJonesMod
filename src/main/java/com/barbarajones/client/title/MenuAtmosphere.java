package com.barbarajones.client.title;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Carries the title screen's unease into the menus behind it.
 *
 * <p>{@link TitleFlicker} only ever ran on the mod's own title screen, so
 * clicking Singleplayer stepped out of the mod and into ordinary Minecraft -
 * the music stopped, the faces stopped, and the world list was just a world
 * list. The front end is one moment; it should hold together across all of it.
 *
 * <p>Deliberately only the flicker, not the whole backdrop. The world list has
 * to stay readable - you are choosing a save, not looking at a menu - so what
 * carries over is the thing that works precisely because you are not sure it
 * happened.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class MenuAtmosphere {

    private MenuAtmosphere() { }

    /**
     * Whether this screen should get the flicker.
     *
     * <p>The title screen itself is excluded. {@link KraveTitleScreen} is not a
     * Screen at all - it is a helper that draws over the vanilla one - so the
     * title screen already gets the flicker through that path, and drawing it
     * again here would double the alpha in the same frame. That turns a thing
     * you are not sure you saw into a thing you definitely saw, which is the
     * entire effect, lost.
     *
     * <p>PauseScreen is excluded because it is reached from inside a world, and
     * a menu horror beat has no business appearing over someone's game.
     */
    private static boolean wants(Screen screen) {
        if (screen == null || screen instanceof TitleScreen
                || screen instanceof PauseScreen) {
            return false;
        }
        // Everything else in the front end: the world list, create-world, and
        // the options screens reached from the menu. A level being loaded is
        // what separates those from an in-game screen.
        return Minecraft.getInstance().level == null;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!wants(screen)) {
            return;
        }
        GuiGraphics gfx = event.getGuiGraphics();
        TitleFlicker.draw(gfx, screen.width, screen.height, Util.getMillis());
    }
}
