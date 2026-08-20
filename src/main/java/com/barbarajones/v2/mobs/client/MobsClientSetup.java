package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.ModMobEntities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * This module's OWN client bootstrap, mirroring {@code client.ClientSetup}
 * but scoped to this package - registers every layer definition and entity
 * renderer for the Kraveling family without touching the shared file. Fires
 * automatically via {@code @Mod.EventBusSubscriber}; nothing needs to call
 * into this class from anywhere.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MobsClientSetup {

    private MobsClientSetup() { }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KravelingModel.LAYER_LOCATION, KravelingModel::createBodyLayer);
        event.registerLayerDefinition(KrispboneModel.LAYER_LOCATION, KrispboneModel::createBodyLayer);
        event.registerLayerDefinition(LoomweaverModel.LAYER_LOCATION, LoomweaverModel::createBodyLayer);
        event.registerLayerDefinition(SoggyModel.LAYER_LOCATION, SoggyModel::createBodyLayer);
        event.registerLayerDefinition(KravajoModel.LAYER_LOCATION, KravajoModel::createBodyLayer);
        event.registerLayerDefinition(MascotModel.LAYER_LOCATION, MascotModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobEntities.KRAVELING.get(), KravelingRenderer::new);
        event.registerEntityRenderer(ModMobEntities.KRISPBONE.get(), KrispboneRenderer::new);
        event.registerEntityRenderer(ModMobEntities.LOOMWEAVER.get(), LoomweaverRenderer::new);
        event.registerEntityRenderer(ModMobEntities.SOGGY.get(), SoggyRenderer::new);
        event.registerEntityRenderer(ModMobEntities.KRAVAJO.get(), KravajoRenderer::new);
        event.registerEntityRenderer(ModMobEntities.MASCOT.get(), MascotRenderer::new);
        event.registerEntityRenderer(ModMobEntities.KRAVE_SHARD.get(), KraveShardRenderer::new);
    }
}
