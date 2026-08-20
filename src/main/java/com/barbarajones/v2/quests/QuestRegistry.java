package com.barbarajones.v2.quests;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * This module's own registries.
 *
 * <p>Forge allows any number of DeferredRegisters per registry type, and that is the
 * whole reason a dozen agents can work in this repo at once: nothing here needs
 * {@code ModItems} to be edited. One item is registered, the Quest Atlas, and it is
 * added to a vanilla creative tab from this class rather than from the shared
 * {@code ModTabs}.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuestRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    /**
     * The quest book. Named the Atlas so it collides with neither the old
     * {@code quest_book} - still registered elsewhere, and whose NBT the old system
     * used as a save file - nor the existing {@code KraveCodexScreen}, which is a
     * different UI belonging to another module.
     */
    public static final RegistryObject<Item> QUEST_ATLAS = ITEMS.register("quest_atlas",
            () -> new QuestAtlasItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private QuestRegistry() {
    }

    static void init(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    /**
     * Slot the Atlas into a vanilla tab. Going through the shared {@code ModTabs}
     * would mean editing a file three other agents are in; this achieves the same
     * result from inside the module. The orchestrator can move it to the mod's own
     * tab later with a one-line addition there.
     */
    @SubscribeEvent
    public static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUEST_ATLAS);
        }
    }
}
