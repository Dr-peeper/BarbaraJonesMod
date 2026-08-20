package com.barbarajones.v2.village.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.village.VillageRegistry;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The village module's client wiring, in its own subscriber so nothing has to be
 * added to the shared {@code ClientSetup}.
 *
 * <p>Three things have to happen here and all three are load-bearing:
 *
 * <ul>
 *   <li><b>The layer definition</b>, or {@code bakeLayer} throws when the renderer
 *       is constructed.
 *   <li><b>The entity renderer.</b> An entity with no registered renderer is
 *       invisible right up until the first one comes into view, and then it NPEs
 *       the render dispatcher and takes the client with it. This is the single most
 *       common way a new mob kills a build.
 *   <li><b>The menu screen binding</b>, or opening the trade menu produces a
 *       container the client has no screen for and the player is left holding an
 *       invisible GUI.
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class VillageClientSetup {

    private VillageClientSetup() { }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KraveVillagerModel.LAYER, KraveVillagerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(VillageRegistry.KRAVE_VILLAGER.get(), KraveVillagerRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(VillageKeys.OPEN_VILLAGE);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(VillageRegistry.KRAVE_TRADE.get(), KraveTradeScreen::new));
    }
}
