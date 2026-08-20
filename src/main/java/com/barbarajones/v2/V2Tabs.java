package com.barbarajones.v2;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModTabs;

import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Puts the modules that own items into the creative tab.
 *
 * <p>Six of the eleven 2.0 modules registered items and then never added them to
 * a tab, so their entire contents were unreachable in creative: every spawn egg
 * for the new mobs, everything the economy makes, the schematics, the village
 * items. They existed, they were craftable if you already knew the recipe, and
 * they were invisible to anyone browsing.
 *
 * <p>The reason it happened is that adding to a tab is a SEPARATE step from
 * registering, in a different event, and a module that skips it looks completely
 * healthy - it compiles, it registers, its items work in commands. Nothing fails.
 *
 * <p>Modules that already add themselves (abilities, bonds, build, machines,
 * quests) are deliberately not listed here, so nothing is added twice.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class V2Tabs {

    private V2Tabs() { }

    /** The registers whose owning module has no tab hook of its own. */
    private static List<DeferredRegister<Item>> orphaned() {
        return List.of(
                com.barbarajones.v2.economy.KraveEconomy.ITEMS,
                com.barbarajones.v2.houses.KraveHouses.ITEMS,
                com.barbarajones.v2.internet.InternetContent.ITEMS,
                com.barbarajones.v2.mobs.ModMobItems.ITEMS,
                com.barbarajones.v2.village.VillageRegistry.ITEMS);
    }

    @SubscribeEvent
    public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(ModTabs.MAIN.getKey())) {
            return;
        }
        for (DeferredRegister<Item> register : orphaned()) {
            for (RegistryObject<Item> item : register.getEntries()) {
                event.accept(item.get());
            }
        }
    }
}
