package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.render.BarbaraRenderer;
import com.barbarajones.client.render.CaydenRenderer;
import com.barbarajones.client.render.EmberCherryRenderer;
import com.barbarajones.client.render.SmokeRingRenderer;
import com.barbarajones.client.render.DuhlWolRenderer;
import com.barbarajones.client.render.DuhlWolCarRenderer;
import com.barbarajones.client.render.GiantKraveBoxRenderer;
import com.barbarajones.client.render.HumanoidLikeRenderer;
import com.barbarajones.client.render.KraveHealingBoxRenderer;
import com.barbarajones.client.render.KraveLaserRenderer;
import com.barbarajones.client.render.KraveLeviathanModel;
import com.barbarajones.client.render.KraveLeviathanRenderer;
import com.barbarajones.client.render.KraveMouthBeamRenderer;
import com.barbarajones.client.render.KraveMeteorRenderer;
import com.barbarajones.client.render.KraveMonsterModel;
import com.barbarajones.client.render.KraveMonsterRenderer;
import com.barbarajones.client.render.KraveTornadoRenderer;
import com.barbarajones.client.render.NuggetRenderer;
import com.barbarajones.client.render.SkyCinematicRenderer;
import com.barbarajones.boss.manager.FilingCabinetRenderer;
import com.barbarajones.boss.manager.ManagerModel;
import com.barbarajones.boss.manager.ManagerRenderer;
import com.barbarajones.boss.manager.TerminationNoticeRenderer;
import com.barbarajones.boss.mom.client.MomCobbBossRenderer;
import com.barbarajones.boss.mom.client.MomKraveStashRenderer;
import com.barbarajones.boss.mom.client.ThrownHouseholdRenderer;
import com.barbarajones.content.ModEntities;
import com.barbarajones.menu.ModMenus;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Binds every entity to its renderer. */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() { }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BARBARA.get(), BarbaraRenderer::new);
        event.registerEntityRenderer(ModEntities.CAYDEN.get(), CaydenRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAVE_MONSTER.get(), KraveMonsterRenderer::new);
        event.registerEntityRenderer(ModEntities.NUGGET.get(), NuggetRenderer::new);

        event.registerEntityRenderer(ModEntities.DANIEL.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "daniel", 0.5F));
        event.registerEntityRenderer(ModEntities.MOM.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "mom", 0.5F));
        event.registerEntityRenderer(ModEntities.PLUG.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "plug", 0.5F));
        event.registerEntityRenderer(ModEntities.DUHL_WOL.get(), DuhlWolRenderer::new);
        event.registerEntityRenderer(ModEntities.DUHL_WOL_CAR.get(), DuhlWolCarRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAVE_MINION.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "krave_minion", 0.4F));
        event.registerEntityRenderer(ModEntities.KRAVE_HEALING_BOX.get(), KraveHealingBoxRenderer::new);

        // Barbara's kit. Both renderer classes were written and both entities
        // were registered; nothing ever connected them, so the first Torching
        // spawned a smoke ring the client had no renderer for and the render
        // dispatcher NPE'd straight out of the game.
        event.registerEntityRenderer(ModEntities.SMOKE_RING.get(), SmokeRingRenderer::new);
        event.registerEntityRenderer(ModEntities.EMBER_CHERRY.get(), EmberCherryRenderer::new);
        // The recliner seat is a mount with nothing to draw, but "nothing to
        // draw" still has to BE a renderer or it fails the same way on sitting.
        event.registerEntityRenderer(com.barbarajones.content.extra.ExtraRegistry.RECLINER_SEAT.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);

        event.registerEntityRenderer(ModEntities.METEOR.get(), KraveMeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAVE_LASER.get(), KraveLaserRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAVE_MOUTH_BEAM.get(), KraveMouthBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.GIANT_BOX.get(), GiantKraveBoxRenderer::new);
        event.registerEntityRenderer(ModEntities.TORNADO.get(), KraveTornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.SKY_CINEMATIC.get(), SkyCinematicRenderer::new);
        event.registerEntityRenderer(ModEntities.MOM_BOSS.get(), MomCobbBossRenderer::new);
        event.registerEntityRenderer(ModEntities.MOM_STASH.get(), MomKraveStashRenderer::new);
        event.registerEntityRenderer(ModEntities.MOM_THROWN.get(), ThrownHouseholdRenderer::new);
        event.registerEntityRenderer(ModEntities.THE_MANAGER.get(), ManagerRenderer::new);
        event.registerEntityRenderer(ModEntities.MANAGER_MINION.get(),
                ctx -> new HumanoidLikeRenderer<>(ctx, "manager_minion", 0.5F));
        event.registerEntityRenderer(ModEntities.TERMINATION_NOTICE.get(), TerminationNoticeRenderer::new);
        event.registerEntityRenderer(ModEntities.FILING_CABINET.get(), FilingCabinetRenderer::new);
        event.registerEntityRenderer(ModEntities.KRAVE_LEVIATHAN.get(), KraveLeviathanRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KraveMonsterModel.LAYER_LOCATION, KraveMonsterModel::createBodyLayer);
        event.registerLayerDefinition(KraveLeviathanModel.LAYER_LOCATION, KraveLeviathanModel::createBodyLayer);
        event.registerLayerDefinition(ManagerModel.LAYER_LOCATION, ManagerModel::createBodyLayer);
        event.registerLayerDefinition(com.barbarajones.client.render.BarbaraModel.LAYER,
                com.barbarajones.client.render.BarbaraModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.KRAFTING_BENCH.get(), KraftingBenchScreen::new));
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(new ResourceLocation(BarbaraJonesMod.MODID, "krave_kosmos"), new KraveKosmosEffects());
    }
}
