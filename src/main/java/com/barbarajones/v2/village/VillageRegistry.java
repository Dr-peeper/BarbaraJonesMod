package com.barbarajones.v2.village;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.village.item.VillageAtlasItem;
import com.barbarajones.v2.village.item.VillageCharterItem;
import com.barbarajones.v2.village.menu.KraveTradeMenu;
import com.barbarajones.v2.village.net.VillageNetwork;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Everything this module registers, and the one {@code init(bus)} that starts it.
 *
 * <p>The module owns its own {@link DeferredRegister}s rather than adding entries to
 * the shared {@code ModItems}/{@code ModEntities}/{@code ModMenus} classes. Forge
 * allows any number of DeferredRegisters per registry type, and keeping ours here
 * means this whole package can be written, moved or deleted without another module
 * ever having to open a file it also owns.
 *
 * <h2>Traps this class is deliberately avoiding</h2>
 * <ul>
 *   <li>{@code EntityType.Builder.build()} is <b>inside</b> the DeferredRegister
 *       supplier lambda. In a static field initialiser it runs before registries
 *       thaw and throws "Registry is already frozen".
 *   <li>Attributes are built from {@code createMobAttributes()}, never
 *       {@code createLivingAttributes()} - the latter omits FOLLOW_RANGE, which
 *       {@code GroundPathNavigation} reads in its own constructor, so the mob NPEs
 *       the instant it spawns.
 *   <li>The renderer is registered from this package's own client subscriber. An
 *       entity with no renderer is invisible until it first comes into view, then
 *       takes down the render dispatcher.
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VillageRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BarbaraJonesMod.MODID);

    private VillageRegistry() { }

    // ---- entity --------------------------------------------------------------

    /**
     * The one NPC type. Profession is synced entity data, not a separate entity
     * type, so a villager changing job is a texture swap rather than a respawn -
     * and there is one renderer and one attribute registration to get wrong instead
     * of five.
     */
    public static final RegistryObject<EntityType<KraveVillagerEntity>> KRAVE_VILLAGER =
            ENTITIES.register("krave_villager", () -> EntityType.Builder
                    .<KraveVillagerEntity>of(KraveVillagerEntity::new, MobCategory.CREATURE)
                    .sized(0.62F, 1.95F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("krave_villager"));

    // ---- items ---------------------------------------------------------------

    /**
     * Plant this on a block to found a settlement there. Consumed on use; the
     * position becomes the village origin and the claim is centred on it.
     */
    public static final RegistryObject<Item> VILLAGE_CHARTER =
            ITEMS.register("village_charter", () -> new VillageCharterItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /**
     * Opens the full village screen - tier, population, production, defence and the
     * requirements for the next rung. Works anywhere; reports on the nearest
     * settlement in the dimension.
     */
    public static final RegistryObject<Item> VILLAGE_ATLAS =
            ITEMS.register("village_atlas", () -> new VillageAtlasItem(
                    new Item.Properties().stacksTo(1)));

    /**
     * Spawns a Krave Villager with a random profession. Uses the vanilla spawn-egg
     * item model template, so it needs no texture of its own - only the two colours.
     */
    public static final RegistryObject<Item> KRAVE_VILLAGER_SPAWN_EGG =
            ITEMS.register("krave_villager_spawn_egg", () -> new ForgeSpawnEggItem(
                    KRAVE_VILLAGER, 0x8A5A2A, 0xE9B23C, new Item.Properties()));

    // ---- menu ----------------------------------------------------------------

    /**
     * The trading menu. Built through {@code IForgeMenuType} because the client
     * needs the villager's entity id in the open packet to find the NPC it is
     * trading with.
     */
    public static final RegistryObject<MenuType<KraveTradeMenu>> KRAVE_TRADE =
            MENUS.register("krave_trade", () -> IForgeMenuType.create(
                    (id, inv, buf) -> new KraveTradeMenu(id, inv, buf.readVarInt())));

    // ---- entry point ---------------------------------------------------------

    /**
     * The module's single wiring call. See {@link KraveVillage#init(IEventBus)},
     * which is the documented public form of this.
     */
    public static void init(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    // ---- mod-bus lifecycle ---------------------------------------------------

    /**
     * Attributes for the villager. Registered from this module's own subscriber so
     * the shared {@code ModEntityAttributes} never has to know we exist - Forge is
     * perfectly happy with several listeners on the same event.
     */
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(KRAVE_VILLAGER.get(), KraveVillagerEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Order matters: drain anything another mod queued from a static
            // initialiser first, then lay down our own defaults on top, then open
            // the network channel.
            KraveVillage.resolvePendingBuffs();
            VillageBuffs.install();
            VillageNetwork.register();
        });
    }

}
