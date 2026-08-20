package com.barbarajones.v2.machines.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.recipe.RecipeLookup;

/**
 * Everything client-side this module needs, registered from inside the module.
 *
 * <p>Nothing here touches the shared {@code ClientSetup} class. Forge discovers
 * {@code @Mod.EventBusSubscriber} by annotation scan, so this file wires itself
 * up: no central registration call, and no merge conflict with the dozen other
 * things being added to the mod at the same time.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachinesClient {

    private MachinesClient() { }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(KraveMachines.MACHINE_MENU.get(), MachineScreen::new);

            // The conveyor is a three-pixel slab with cut-out rails - without a
            // cutout render type its transparent pixels come out solid black.
            ItemBlockRenderTypes.setRenderLayer(KraveMachines.KRAVE_CONVEYOR.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(KraveMachines.CONVEYOR_BLOCK_ENTITY.get(), KraveConveyorRenderer::new);
    }

    /**
     * Client-side half of the recipe-index invalidation.
     *
     * <p>Fires when the server sends its recipe list on join or after a
     * {@code /reload}. Without this the client keeps a stale index, and any
     * client-side recipe question - what a machine accepts, what it will make -
     * answers from the previous datapack.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
    public static final class ClientRecipeSync {

        private ClientRecipeSync() { }

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            RecipeLookup.invalidate();
        }
    }
}
