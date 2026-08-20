package com.barbarajones.v2.machines;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.machines.block.KraveConveyorBlock;
import com.barbarajones.v2.machines.block.KraveExtractorBlock;
import com.barbarajones.v2.machines.block.MachineBlock;
import com.barbarajones.v2.machines.blockentity.KraveConveyorBlockEntity;
import com.barbarajones.v2.machines.blockentity.KraveExtractorBlockEntity;
import com.barbarajones.v2.machines.blockentity.MachineBlockEntity;
import com.barbarajones.v2.machines.menu.MachineMenu;
import com.barbarajones.v2.machines.recipe.MachineRecipe;
import com.barbarajones.v2.machines.recipe.MachineRecipeSerializer;

/**
 * Every registry entry the Krave Automation module owns, and its single entry
 * point.
 *
 * <p>The module keeps its own {@link DeferredRegister} for each registry it needs
 * rather than adding to the shared {@code ModItems} / {@code ModBlocks}. Forge
 * allows any number of DeferredRegisters per registry, and keeping them here
 * means this whole package can be added, moved or deleted without touching a file
 * anyone else is editing.
 *
 * <p>The orchestrator wires it up with one line in the mod constructor:
 * <pre>KraveMachines.init(bus);</pre>
 */
public final class KraveMachines {

    public static final String MODID = BarbaraJonesMod.MODID;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    private KraveMachines() { }

    /**
     * The one call the orchestrator has to make. Must run from the mod
     * constructor, before registry events fire.
     */
    public static void init(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    // =========================================================================
    // Blocks
    // =========================================================================

    /** Painted steel and chocolate-stained plastic. Pickaxe work, survives a blast. */
    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(3.5F, 12.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK);
    }

    private static RegistryObject<Block> machine(MachineKind kind) {
        return BLOCKS.register(kind.id, () -> new MachineBlock(kind, machineProps()));
    }

    public static final RegistryObject<Block> COCOA_PLANTATION = machine(MachineKind.PLANTATION);
    public static final RegistryObject<Block> KRAVE_GRINDER = machine(MachineKind.GRINDER);
    public static final RegistryObject<Block> KRAVE_MIXER = machine(MachineKind.MIXER);
    public static final RegistryObject<Block> KRAVE_EXTRUDER = machine(MachineKind.EXTRUDER);
    public static final RegistryObject<Block> KRAVE_TOASTER = machine(MachineKind.TOASTER);
    public static final RegistryObject<Block> KRAVE_BOXER = machine(MachineKind.BOXER);
    public static final RegistryObject<Block> KRAVE_DEPOT = machine(MachineKind.DEPOT);

    public static final RegistryObject<Block> KRAVE_CONVEYOR = BLOCKS.register("krave_conveyor",
            () -> new KraveConveyorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> KRAVE_EXTRACTOR = BLOCKS.register("krave_extractor",
            () -> new KraveExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    // =========================================================================
    // Items
    // =========================================================================

    private static Item.Properties props() {
        return new Item.Properties();
    }

    private static RegistryObject<Item> blockItem(RegistryObject<Block> block, String id) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), props()));
    }

    public static final RegistryObject<Item> COCOA_PLANTATION_ITEM =
            blockItem(COCOA_PLANTATION, MachineKind.PLANTATION.id);
    public static final RegistryObject<Item> KRAVE_GRINDER_ITEM =
            blockItem(KRAVE_GRINDER, MachineKind.GRINDER.id);
    public static final RegistryObject<Item> KRAVE_MIXER_ITEM =
            blockItem(KRAVE_MIXER, MachineKind.MIXER.id);
    public static final RegistryObject<Item> KRAVE_EXTRUDER_ITEM =
            blockItem(KRAVE_EXTRUDER, MachineKind.EXTRUDER.id);
    public static final RegistryObject<Item> KRAVE_TOASTER_ITEM =
            blockItem(KRAVE_TOASTER, MachineKind.TOASTER.id);
    public static final RegistryObject<Item> KRAVE_BOXER_ITEM =
            blockItem(KRAVE_BOXER, MachineKind.BOXER.id);
    public static final RegistryObject<Item> KRAVE_DEPOT_ITEM =
            blockItem(KRAVE_DEPOT, MachineKind.DEPOT.id);
    public static final RegistryObject<Item> KRAVE_CONVEYOR_ITEM =
            blockItem(KRAVE_CONVEYOR, "krave_conveyor");
    public static final RegistryObject<Item> KRAVE_EXTRACTOR_ITEM =
            blockItem(KRAVE_EXTRACTOR, "krave_extractor");

    // NOTE: barbarajones:krave_syrup is NOT registered here. The economy module
    // (com.barbarajones.v2.economy.KraveEconomy.KRAVE_SYRUP) owns that id, its
    // texture, its lang key and its crafting recipe, and its own docs name it as
    // the id the machines module should build on. Registering a second one would
    // be a duplicate-registration crash at load. KraveFuels reads theirs.

    /** Five syrups boiled down. One slot of this runs a Grinder for most of a day. */
    public static final RegistryObject<Item> DENSE_KRAVE_SYRUP =
            ITEMS.register("dense_krave_syrup", () -> new Item(props().stacksTo(16)));

    /** Wet chocolate dough. Intermediate: Mixer out, Extruder in. */
    public static final RegistryObject<Item> KRAVE_BATTER =
            ITEMS.register("krave_batter", () -> new Item(props()));

    /**
     * Extruded but untoasted. Edible, barely - it is raw dough and it will sit in
     * you like a brick, which is exactly what the hunger effect is saying.
     */
    public static final RegistryObject<Item> RAW_KRAVE_PIECE =
            ITEMS.register("raw_krave_piece", () -> new Item(props().food(
                    new FoodProperties.Builder().nutrition(1).saturationMod(0.1F).build())));

    /** Flat-packed cardboard. The Boxer folds it around finished cereal. */
    public static final RegistryObject<Item> KRAVE_CARTON =
            ITEMS.register("krave_carton", () -> new Item(props()));

    /**
     * The end of the chain and the thing the village actually wants: a sealed
     * case of Krave, ready to ship.
     */
    public static final RegistryObject<Item> BOXED_KRAVE =
            ITEMS.register("boxed_krave", () -> new Item(props().stacksTo(16).rarity(Rarity.UNCOMMON)));

    // =========================================================================
    // Block entities
    // =========================================================================

    /**
     * One block-entity type for all seven machines.
     *
     * <p>{@code build()} must stay inside this supplier. Called from a static
     * field initialiser it runs before the registries thaw and throws "Registry
     * is already frozen", which takes the whole mod down at load.
     */
    public static final RegistryObject<BlockEntityType<MachineBlockEntity>> MACHINE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("krave_machine", () -> BlockEntityType.Builder
                    .of(MachineBlockEntity::new,
                            COCOA_PLANTATION.get(),
                            KRAVE_GRINDER.get(),
                            KRAVE_MIXER.get(),
                            KRAVE_EXTRUDER.get(),
                            KRAVE_TOASTER.get(),
                            KRAVE_BOXER.get(),
                            KRAVE_DEPOT.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<KraveConveyorBlockEntity>> CONVEYOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("krave_conveyor", () -> BlockEntityType.Builder
                    .of(KraveConveyorBlockEntity::new, KRAVE_CONVEYOR.get())
                    .build(null));

    public static final RegistryObject<BlockEntityType<KraveExtractorBlockEntity>> EXTRACTOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("krave_extractor", () -> BlockEntityType.Builder
                    .of(KraveExtractorBlockEntity::new, KRAVE_EXTRACTOR.get())
                    .build(null));

    // =========================================================================
    // Menu
    // =========================================================================

    public static final RegistryObject<MenuType<MachineMenu>> MACHINE_MENU =
            MENUS.register("krave_machine", () -> IForgeMenuType.create(MachineMenu::new));

    // =========================================================================
    // Recipe types and serializers
    // =========================================================================

    private static RegistryObject<RecipeType<MachineRecipe>> recipeType(String name) {
        return RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(MODID, name)));
    }

    public static final RegistryObject<RecipeType<MachineRecipe>> GRINDING = recipeType("grinding");
    public static final RegistryObject<RecipeType<MachineRecipe>> MIXING = recipeType("mixing");
    public static final RegistryObject<RecipeType<MachineRecipe>> EXTRUDING = recipeType("extruding");
    public static final RegistryObject<RecipeType<MachineRecipe>> TOASTING = recipeType("toasting");
    public static final RegistryObject<RecipeType<MachineRecipe>> BOXING = recipeType("boxing");

    public static final RegistryObject<RecipeSerializer<?>> GRINDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("grinding", () -> new MachineRecipeSerializer(GRINDING, 1, 1));
    public static final RegistryObject<RecipeSerializer<?>> MIXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixing", () -> new MachineRecipeSerializer(MIXING, 2, 3));
    public static final RegistryObject<RecipeSerializer<?>> EXTRUDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("extruding", () -> new MachineRecipeSerializer(EXTRUDING, 1, 1));
    public static final RegistryObject<RecipeSerializer<?>> TOASTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("toasting", () -> new MachineRecipeSerializer(TOASTING, 1, 1));
    public static final RegistryObject<RecipeSerializer<?>> BOXING_SERIALIZER =
            RECIPE_SERIALIZERS.register("boxing", () -> new MachineRecipeSerializer(BOXING, 1, 2));

    private static final Map<MachineKind, RegistryObject<RecipeType<MachineRecipe>>> RECIPE_TYPE_BY_KIND =
            new EnumMap<>(MachineKind.class);

    static {
        RECIPE_TYPE_BY_KIND.put(MachineKind.GRINDER, GRINDING);
        RECIPE_TYPE_BY_KIND.put(MachineKind.MIXER, MIXING);
        RECIPE_TYPE_BY_KIND.put(MachineKind.EXTRUDER, EXTRUDING);
        RECIPE_TYPE_BY_KIND.put(MachineKind.TOASTER, TOASTING);
        RECIPE_TYPE_BY_KIND.put(MachineKind.BOXER, BOXING);
    }

    /** Null for the Plantation and the Depot, which are not recipe-driven. */
    @Nullable
    public static RegistryObject<RecipeType<MachineRecipe>> recipeTypeFor(MachineKind kind) {
        return RECIPE_TYPE_BY_KIND.get(kind);
    }
}
