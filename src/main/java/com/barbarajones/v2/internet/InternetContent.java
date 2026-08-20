package com.barbarajones.v2.internet;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Everything the internet-outage module registers, in one file with one entry
 * point - see the class javadoc on {@code ExtraRegistry} for why this has to be
 * a set of {@link DeferredRegister}s owned here rather than lines added to the
 * shared {@code ModItems}/{@code ModBlocks}/{@code ModEntities}: a dozen agents
 * are editing those concurrently, and a {@link DeferredRegister} is the only
 * legal place {@code EntityType.Builder.build()} can run (it reaches into the
 * entity-type registry immediately, which is frozen everywhere else by the time
 * a plain static field would run).
 *
 * <p><b>Orchestrator, wire these in {@code BarbaraJonesMod}'s constructor</b>
 * (see {@code docs/modules/internet-outage.md} for the exact lines):
 * <pre>
 *   InternetContent.ITEMS.register(bus);
 *   InternetContent.BLOCKS.register(bus);
 *   InternetContent.ENTITIES.register(bus);
 *   InternetContent.init(bus);
 * </pre>
 */
public final class InternetContent {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    private InternetContent() { }

    // ---- the boss -------------------------------------------------------------

    public static final RegistryObject<EntityType<InternetManagerBoss>> INTERNET_MANAGER =
            ENTITIES.register("internet_manager", () -> EntityType.Builder
                    .<InternetManagerBoss>of(InternetManagerBoss::new, MobCategory.MONSTER)
                    .sized(0.85F, 2.35F)
                    .clientTrackingRange(12)
                    .fireImmune()
                    .build("internet_manager"));

    // ---- the block: place it, call him -----------------------------------------

    public static final RegistryObject<ServiceCallBoxBlock> SERVICE_CALL_BOX =
            BLOCKS.register("service_call_box", () -> new ServiceCallBoxBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .strength(3.5F, 8.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));
    public static final RegistryObject<Item> SERVICE_CALL_BOX_ITEM =
            ITEMS.register("service_call_box",
                    () -> new BlockItem(SERVICE_CALL_BOX.get(), props()));

    // ---- the summon item ---------------------------------------------------

    public static final RegistryObject<Item> ROTARY_PHONE =
            ITEMS.register("rotary_phone", () -> new RotaryPhoneItem(props().stacksTo(1)));

    // ---- loot -------------------------------------------------------------

    /** The gate item - see {@link InternetLoot#hasStaticIp}. */
    public static final RegistryObject<Item> STATIC_IP =
            ITEMS.register("static_ip", () -> new Item(props().stacksTo(8).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FIBER_OPTIC_COIL =
            ITEMS.register("fiber_optic_coil", () -> new Item(props().stacksTo(32)));
    public static final RegistryObject<Item> MANAGERS_HEADSET =
            ITEMS.register("managers_headset", () -> new Item(props().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ---- spawn egg (debug/creative summon) ---------------------------------

    public static final RegistryObject<Item> EGG_INTERNET_MANAGER =
            ITEMS.register("internet_manager_spawn_egg",
                    () -> new ForgeSpawnEggItem(INTERNET_MANAGER, 0xC9E93A, 0x1D2A44, props()));

    // ---- helpers ------------------------------------------------------------

    private static Item.Properties props() {
        return new Item.Properties();
    }

    /**
     * The one entry point. Registers nothing by itself - {@link #ITEMS},
     * {@link #BLOCKS} and {@link #ENTITIES} still each need
     * {@code .register(bus)} - this only exists so a single call from the
     * orchestrator can also run any one-time, non-registry setup this module
     * ever needs. Present now so the wiring doc has one stable line to point
     * at even though there is nothing else to do here yet.
     */
    public static void init(IEventBus bus) {
        // Registration itself happens via ITEMS/BLOCKS/ENTITIES.register(bus),
        // called directly by the orchestrator (see the class javadoc). Nothing
        // else to do at mod-construction time.
    }
}
