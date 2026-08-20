package com.barbarajones.v2.build;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModTabs;
import com.barbarajones.v2.build.block.KraveCoreBlock;
import com.barbarajones.v2.build.block.KraveCoreBlockEntity;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureRegistry;
import com.barbarajones.v2.build.item.KraveSchematicItem;
import com.barbarajones.v2.build.net.BuildNetwork;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The schematic and structure placement module: registries, constants and the
 * one entry point.
 *
 * <p>Wiring, for whoever is holding {@code BarbaraJonesMod.java}: add exactly
 * one line to the constructor.
 * <pre>{@code
 * com.barbarajones.v2.build.KraveBuild.init(bus);
 * }</pre>
 * Everything else in this module - the Forge event subscribers, the client
 * renderer, the network channel - registers itself.
 *
 * @see com.barbarajones.v2.build.place.KraveStructure
 * @see com.barbarajones.v2.build.def.StructureDef
 */
public final class KraveBuild {

    /**
     * How long after a building goes up its core block still refunds the
     * schematic. One minute: long enough to look at what you did and change
     * your mind, short enough that it is not a free dismantling tool.
     */
    public static final int REFUND_WINDOW_TICKS = 20 * 60;

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BarbaraJonesMod.MODID);

    /** The building's identity block, and its undo button. */
    public static final RegistryObject<Block> CORE_BLOCK = BLOCKS.register("krave_core",
            () -> new KraveCoreBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)));

    /**
     * Built inside the supplier, never in a field initialiser - a
     * {@code BlockEntityType.Builder.build()} that runs before the registries
     * thaw throws "Registry is already frozen".
     */
    public static final RegistryObject<BlockEntityType<KraveCoreBlockEntity>> CORE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("krave_core",
                    () -> BlockEntityType.Builder.of(KraveCoreBlockEntity::new, CORE_BLOCK.get()).build(null));

    /** The shared schematic. Buildings may also register their own; see {@link KraveSchematicItem}. */
    public static final RegistryObject<Item> SCHEMATIC = ITEMS.register("krave_schematic",
            () -> new KraveSchematicItem(new Item.Properties().stacksTo(16)));

    private KraveBuild() { }

    /**
     * The module's single entry point. Call from the mod constructor with the
     * mod event bus.
     */
    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        BuildNetwork.register();
    }

    /**
     * Drops one schematic per registered building into the mod's creative tab.
     * Registered from here rather than from ModTabs so nothing has to be edited
     * in a file another module owns.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Tabs {

        private Tabs() { }

        @SubscribeEvent
        public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
            if (!event.getTabKey().equals(ModTabs.MAIN.getKey())) {
                return;
            }
            // The blank one first - it is the crafting base every building's
            // schematic is made from, so it needs to be reachable on its own.
            event.accept(new net.minecraft.world.item.ItemStack(SCHEMATIC.get()));
            for (StructureDef def : StructureRegistry.all()) {
                event.accept(KraveSchematicItem.forStructure(def.id()));
            }
        }
    }

    /** Registered here so the module owns its own client setup. */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class ClientBootstrap {

        private ClientBootstrap() { }

        @SubscribeEvent
        public static void onRegisterKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            com.barbarajones.v2.build.client.BuildKeys.register(event);
        }
    }
}
