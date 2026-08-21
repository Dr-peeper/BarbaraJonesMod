package com.barbarajones.v2.airline.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.render.HumanoidLikeRenderer;
import com.barbarajones.content.ModEntities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Binds the airline entities to their renderers and registers the plane's model layer. */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AirlineClientSetup {

    private AirlineClientSetup() { }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlaneModel.LAYER, PlaneModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PLANE.get(), PlaneEntityRenderer::new);

        // All six staff share the vanilla player rig and differ only by uniform, which
        // is exactly what HumanoidLikeRenderer already does for Daniel, Mom and The Plug.
        event.registerEntityRenderer(ModEntities.PILOT.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "pilot", 0.5F));
        event.registerEntityRenderer(ModEntities.FLIGHT_ATTENDANT.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "flight_attendant", 0.5F));
        event.registerEntityRenderer(ModEntities.GATE_AGENT.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "gate_agent", 0.5F));
        event.registerEntityRenderer(ModEntities.SECURITY_OFFICER.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "security_officer", 0.5F));
        event.registerEntityRenderer(ModEntities.GROUND_CREW.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "ground_crew", 0.5F));
        event.registerEntityRenderer(ModEntities.AIR_TRAFFIC_CONTROLLER.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "air_traffic_controller", 0.5F));
    }
}
