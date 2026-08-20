package com.barbarajones.v2.bonds;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModTabs;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registries for the FEEDING / BREEDING / COMPANION BOND systems
 * ({@code com.barbarajones.v2.bonds}).
 *
 * <p>Owns its own {@link DeferredRegister}s per HARD RULE 3, so nothing here
 * touches {@code ModItems}, {@code ModEntities} or {@code ModTabs} - it only
 * reads {@code ModTabs.MAIN}'s key to add the Family Box to the tab the rest
 * of the mod's items already live on.
 *
 * <p><b>The one line the orchestrator needs to add</b>, in
 * {@code BarbaraJonesMod}'s constructor, right alongside the other modules'
 * {@code .init(bus)} calls:
 * <pre>    com.barbarajones.v2.bonds.BondsRegistry.init(bus);</pre>
 * Entity attribute registration and the creative-tab addition are wired
 * internally as listeners added by {@code init(bus)} itself - nothing else is
 * needed. (Client-side rendering registers itself separately via
 * {@link BondsClient}'s own {@code @Mod.EventBusSubscriber} and needs no
 * orchestrator wiring at all - see that class.)
 */
public final class BondsRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BarbaraJonesMod.MODID);

    private BondsRegistry() { }

    /** A Cayden born of overfeeding rather than the login handler. See {@link BredCaydenCobb}. */
    public static final RegistryObject<EntityType<BredCaydenCobb>> BRED_CAYDEN =
            ENTITY_TYPES.register("bred_cayden_cobb", () -> EntityType.Builder
                    .of(BredCaydenCobb::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.4F).clientTrackingRange(10)
                    .build("bred_cayden_cobb"));

    /** The cloning-by-overfeeding gag itself. See {@link KraveCloningBoxItem}. */
    public static final RegistryObject<Item> KRAVE_CLONING_BOX = ITEMS.register("krave_cloning_box",
            () -> new KraveCloningBoxItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        ENTITY_TYPES.register(bus);
        bus.addListener(BondsRegistry::onAttributeCreation);
        bus.addListener(BondsRegistry::onBuildCreativeTab);
    }

    /** Every LivingEntity needs an AttributeSupplier or it NPEs the instant one spawns - see the KNOWN TRAPS list. */
    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BRED_CAYDEN.get(), BredCaydenCobb.createAttributes().build());
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModTabs.MAIN.getKey())) {
            event.accept(KRAVE_CLONING_BOX.get());
        }
    }
}
