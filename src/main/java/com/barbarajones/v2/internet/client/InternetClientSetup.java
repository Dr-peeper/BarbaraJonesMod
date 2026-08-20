package com.barbarajones.v2.internet.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.internet.InternetContent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CLIENT ONLY. Self-registered on the MOD bus, {@code Dist.CLIENT}-gated -
 * see {@code ExtraClientSetup} for the identical pattern. This never needs a
 * line added to the shared {@code ClientSetup}; Forge calls every subscriber
 * of {@link EntityRenderersEvent.RegisterLayerDefinitions} and {@link
 * EntityRenderersEvent.RegisterRenderers} on its own.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class InternetClientSetup {

    private InternetClientSetup() { }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(InternetManagerModel.LAYER_LOCATION, InternetManagerModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InternetContent.INTERNET_MANAGER.get(), InternetManagerRenderer::new);
    }
}
