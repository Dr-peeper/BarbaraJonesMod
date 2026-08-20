package com.barbarajones.v2.bonds;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.render.CaydenRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only wiring for this module. Self-registering via the
 * {@code @Mod.EventBusSubscriber(..., value = Dist.CLIENT)} pattern (see HARD
 * RULE 3) - the orchestrator does not need to call anything here; Forge picks
 * this class up on its own on the client physical side only, so it is never
 * classloaded on a dedicated server.
 *
 * <p>{@link BredCaydenCobb} reuses {@code CaydenRenderer} wholesale rather
 * than shipping a second copy of it - he is visually a Cayden, because he is
 * one; only his {@code EntityType} differs.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BondsClient {

    private BondsClient() { }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BondsRegistry.BRED_CAYDEN.get(), CaydenRenderer::new);
    }
}
