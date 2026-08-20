package com.barbarajones.v2.economy;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.economy.block.KraveMortarBlock;
import com.barbarajones.v2.economy.item.KraveMortarBlockItem;
import com.barbarajones.v2.economy.item.KraveSyrupItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * KRAVE CRAFTING ECONOMY - package {@code com.barbarajones.v2.economy}.
 *
 * <p>Everything this module registers lives on this one class, behind the one
 * entry point the orchestrator needs to call: {@link #init(IEventBus)}.
 * See {@code docs/modules/krave-economy.md} for the full write-up, every
 * registered id, and the recipe web that ties them together.
 *
 * <p>Deliberately does NOT re-register {@code barbarajones:krave_cereal},
 * {@code barbarajones:krave_dust} or {@code barbarajones:golden_krave} - all
 * three already exist in {@code com.barbarajones.content.ModItems} (read-only
 * from here) and already fill the "plain Krave", "ground Krave Dust" and
 * "top-tier Krave" roles this module's tiered line needs. This module adds
 * exactly the pieces that were missing: the Rich Krave mid-tier, Krave Syrup
 * as a new intermediate, and the Krave Mortar block that makes Krave Dust by
 * hand.
 */
public final class KraveEconomy {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BarbaraJonesMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BarbaraJonesMod.MODID);

    private KraveEconomy() { }

    private static FoodProperties.Builder food(int nutrition, float saturationMod) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturationMod);
    }

    // ---- the Krave Mortar: manual cocoa -> dust grinding -------------------

    public static final RegistryObject<Block> KRAVE_MORTAR = BLOCKS.register("krave_mortar",
            () -> new KraveMortarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final RegistryObject<Item> KRAVE_MORTAR_ITEM = ITEMS.register("krave_mortar",
            () -> new KraveMortarBlockItem(KRAVE_MORTAR.get(), new Item.Properties()));

    // ---- Krave Syrup: new intermediate, machines/armour/abilities can build
    // on top of it. barbarajones:krave_syrup -------------------------------

    public static final RegistryObject<Item> KRAVE_SYRUP = ITEMS.register("krave_syrup",
            () -> new KraveSyrupItem(new Item.Properties()
                    .food(food(3, 0.5F)
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100), 1.0F)
                            .build())));

    // ---- Rich Krave: the crafted mid-tier between plain Krave (Krave Cereal)
    // and Golden Krave. barbarajones:rich_krave -----------------------------

    public static final RegistryObject<Item> RICH_KRAVE = ITEMS.register("rich_krave",
            () -> new Item(new Item.Properties()
                    .food(food(6, 0.6F)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400), 1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200), 1.0F)
                            .build())));

    // ---- a dedicated creative tab so testers can find this module's new
    // items without waiting on ModTabs.java to be updated centrally --------

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("krave_economy",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Krave Economy"))
                    .icon(() -> new ItemStack(RICH_KRAVE.get()))
                    .displayItems((params, output) -> {
                        output.accept(KRAVE_MORTAR_ITEM.get());
                        output.accept(RICH_KRAVE.get());
                        output.accept(KRAVE_SYRUP.get());
                        // surfaced for convenience - registered elsewhere, not by this module
                        output.accept(com.barbarajones.content.ModItems.KRAVE_DUST.get());
                        output.accept(com.barbarajones.content.ModItems.KRAVE_CEREAL.get());
                        output.accept(com.barbarajones.content.ModItems.GOLDEN_KRAVE.get());
                    })
                    .build());

    /** The one entry point the orchestrator wires up: registers everything above. */
    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        BLOCKS.register(bus);
        TABS.register(bus);
    }
}
