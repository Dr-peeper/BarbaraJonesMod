package com.barbarajones.progression;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Hands out the mod's milestone advancements from code.
 *
 * <p>Most of the Krave advancements ride real vanilla criteria declared in
 * their JSON (killing the monster, entering the Kosmos, feeding Cayden), so
 * they need nothing from here. The ones vanilla cannot express - "reach Krave
 * level 10", "survive an apocalypse" - are declared with
 * {@code minecraft:impossible} and granted through this helper instead. That is
 * the standard way to do it and avoids registering a custom CriterionTrigger
 * just to fire it from one place.
 */
public final class KraveAdvancements {

    private KraveAdvancements() { }

    /**
     * Award every outstanding criterion of {@code barbarajones:<path>}.
     *
     * <p>Missing advancements are ignored rather than thrown: a data pack that
     * removed or renamed one must not take a player's XP award down with it.
     */
    public static void grant(ServerPlayer player, String path) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Advancement advancement = server.getAdvancements()
                .getAdvancement(new ResourceLocation(BarbaraJonesMod.MODID, path));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        // copy first: award() mutates the progress the iterable is reading from
        List<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        for (String criterion : remaining) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
