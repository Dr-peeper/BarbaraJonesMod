package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.item.CaydenCompassItem;
import com.barbarajones.item.ComputerMouseItem;
import com.barbarajones.item.HousingQueryItem;
import com.barbarajones.item.KraveCleanseItem;
import com.barbarajones.item.KraveTetherItem;
import com.barbarajones.item.KraveTools;
import com.barbarajones.item.JointItem;
import com.barbarajones.item.KraveBoxItem;
import com.barbarajones.item.KraveManualItem;
import com.barbarajones.item.QuestBookItem;
import com.barbarajones.item.RecipeBookItem;
import com.barbarajones.item.RedHatItem;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Every item in the mod, ported to DeferredRegister. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    private ModItems() { }

    // ---- helpers -----------------------------------------------------------

    private static Item.Properties props() {
        return new Item.Properties();
    }

    private static RegistryObject<Item> plain(String name) {
        return ITEMS.register(name, () -> new Item(props()));
    }

    private static RegistryObject<Item> plain(String name, int stack) {
        return ITEMS.register(name, () -> new Item(props().stacksTo(stack)));
    }

    private static FoodProperties.Builder food(int nutrition, float sat) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationMod(sat);
    }

    private static RegistryObject<Item> edible(String name, FoodProperties food) {
        return ITEMS.register(name, () -> new Item(props().food(food)));
    }

    // ---- the grass pipeline -------------------------------------------------

    public static final RegistryObject<Item> HANDFUL_OF_GRASS = plain("handful_of_grass", 16);
    public static final RegistryObject<Item> DICED_GRASS      = plain("diced_grass", 32);
    public static final RegistryObject<Item> BURNT_GRASS      = plain("burnt_grass", 32);
    public static final RegistryObject<Item> ROLLING_PAPER    = plain("rolling_paper", 32);
    public static final RegistryObject<Item> ROLLED_JOINT =
            ITEMS.register("rolled_joint", () -> new JointItem(props().stacksTo(16)));

    // ---- tools --------------------------------------------------------------

    public static final RegistryObject<Item> GRASS_KNIFE =
            ITEMS.register("grass_knife", () -> new Item(props().stacksTo(1).durability(128)));
    public static final RegistryObject<Item> BLOWTORCH =
            ITEMS.register("blowtorch", () -> new Item(props().stacksTo(1).durability(128)));
    public static final RegistryObject<Item> LIGHTER =
            ITEMS.register("lighter", () -> new Item(props().stacksTo(1).durability(64)));
    public static final RegistryObject<Item> MICROPHONE = plain("microphone", 1);
    public static final RegistryObject<Item> CAMERA     = plain("camera", 1);
    public static final RegistryObject<Item> HOUSING_QUERY =
            ITEMS.register("housing_query", () -> new HousingQueryItem(props().stacksTo(1)));

    // ---- drinks & food ------------------------------------------------------

    public static final RegistryObject<Item> MR_PIBB = edible("mr_pibb",
            food(2, 0.3F).alwaysEat().effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160), 0.5F).build());
    public static final RegistryObject<Item> CHEPINA = plain("chepina", 16);
    public static final RegistryObject<Item> PIBB_COCKTAIL = edible("pibb_cocktail",
            food(4, 0.6F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 120), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 160), 0.6F).build());
    public static final RegistryObject<Item> GATORADE = edible("gatorade",
            food(1, 0.1F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 100), 0.4F).build());
    public static final RegistryObject<Item> PIBB_ZERO = edible("pibb_zero",
            food(1, 0.1F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 300), 0.5F).build());
    public static final RegistryObject<Item> KRAVE_MILK = edible("krave_milk",
            food(3, 0.4F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200), 1.0F).build());
    public static final RegistryObject<Item> CHICKEN_NUGGETS = edible("chicken_nuggets", food(6, 0.6F).build());
    public static final RegistryObject<Item> DONUT = edible("donut",
            food(4, 0.4F).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120), 1.0F).build());
    public static final RegistryObject<Item> FRIES     = edible("fries", food(5, 0.5F).build());
    public static final RegistryObject<Item> NUGGET_BOX = edible("nugget_box",
            food(10, 0.9F).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 160), 1.0F).build());
    public static final RegistryObject<Item> DONUT_BOX = plain("donut_box", 16);
    public static final RegistryObject<Item> CEREAL_BOWL = edible("cereal_bowl",
            food(10, 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 240, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600), 1.0F).build());
    public static final RegistryObject<Item> GRASS_BROWNIE = edible("grass_brownie",
            food(6, 0.6F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 240), 0.7F).build());
    public static final RegistryObject<Item> GOLDEN_JOINT = ITEMS.register("golden_joint",
            () -> new Item(props().rarity(Rarity.EPIC).food(food(4, 0.5F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.JUMP, 600, 2), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400), 1.0F).build())));

    // ---- questline ----------------------------------------------------------

    public static final RegistryObject<Item> KRAVE_CEREAL = edible("krave_cereal",
            food(4, 0.3F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400), 1.0F).build());
    public static final RegistryObject<Item> KRAVE_BOX =
            ITEMS.register("krave_box", () -> new KraveBoxItem(props().stacksTo(1)));
    public static final RegistryObject<Item> QUEST_BOOK =
            ITEMS.register("quest_book", () -> new QuestBookItem(props().stacksTo(1)));
    public static final RegistryObject<Item> RECIPE_BOOK =
            ITEMS.register("recipe_book", () -> new RecipeBookItem(props().stacksTo(1)));
    public static final RegistryObject<Item> KRAVE_MANUAL =
            ITEMS.register("krave_manual", () -> new KraveManualItem(props().stacksTo(1)));

    // ---- memorabilia --------------------------------------------------------

    public static final RegistryObject<Item> ASHTRAY        = plain("ashtray", 16);
    public static final RegistryObject<Item> GRASS_SEEDS    = plain("grass_seeds", 64);
    public static final RegistryObject<Item> BONG           = plain("bong", 1);
    public static final RegistryObject<Item> TOWEL          = plain("towel", 16);
    public static final RegistryObject<Item> SOAP           = plain("soap", 16);
    public static final RegistryObject<Item> TOOTHBRUSH     = plain("toothbrush", 1);
    public static final RegistryObject<Item> YELLOW_TEETH   = plain("yellow_teeth", 16);
    public static final RegistryObject<Item> MANAGERS_TIE   = plain("managers_tie", 16);
    public static final RegistryObject<Item> CHILD_SUPPORT_PAPERS = plain("child_support_papers", 64);
    public static final RegistryObject<Item> FLYRICH_POSTER = plain("flyrich_poster", 16);
    public static final RegistryObject<Item> BARBARA_PLUSH  = plain("barbara_plush", 16);
    public static final RegistryObject<Item> RECORD_FLYRICH = plain("record_flyrich", 1);

    // ---- ACT II -------------------------------------------------------------

    public static final RegistryObject<Item> RED_HAT =
            ITEMS.register("red_hat", () -> new RedHatItem(props().stacksTo(1)));
    public static final RegistryObject<Item> RED_SHIRT   = plain("red_shirt", 16);
    public static final RegistryObject<Item> COMPUTER_MOUSE =
            ITEMS.register("computer_mouse", () -> new ComputerMouseItem(props().stacksTo(16)));
    public static final RegistryObject<Item> VIRUS          = plain("virus", 16);
    public static final RegistryObject<Item> MINECRAFT_DISC = plain("minecraft_disc", 16);
    public static final RegistryObject<Item> KRAVE_VIDEO_TAPE = plain("krave_video_tape", 16);
    public static final RegistryObject<Item> TOASTER_PASTRIES = edible("toaster_pastries",
            food(8, 0.8F).effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 120), 1.0F).build());
    public static final RegistryObject<Item> OFF_BRAND_PASTRIES = edible("off_brand_pastries",
            food(1, 0.1F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 160), 0.6F).build());
    public static final RegistryObject<Item> FIVE_HUNDRED_DOLLARS = plain("five_hundred_dollars", 16);
    public static final RegistryObject<Item> DOLLARS = plain("dollars", 64);
    public static final RegistryObject<Item> FAKE_COCAINE = edible("fake_cocaine",
            food(0, 0.0F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200), 1.0F).build());
    public static final RegistryObject<Item> FAKE_WEED = edible("fake_weed",
            food(0, 0.0F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 120), 1.0F).build());
    public static final RegistryObject<Item> SKI_MASK        = plain("ski_mask", 16);
    public static final RegistryObject<Item> SNIPER_SCOPE    = plain("sniper_scope", 16);
    public static final RegistryObject<Item> MOMS_BELT       = plain("moms_belt", 1);
    public static final RegistryObject<Item> ADOPTION_PAPERS = plain("adoption_papers", 64);
    public static final RegistryObject<Item> SEWER_GRATE     = plain("sewer_grate", 16);

    // ---- the cocoa substitute chain (no jungle expedition required) ---------
    // brown mushroom -> [smelt] roasted husk -> + sugar + coal -> cocoa substitute
    // -> [smelt] real cocoa beans. Four steps, all from common overworld stuff.
    public static final RegistryObject<Item> ROASTED_HUSK      = plain("roasted_husk", 64);
    public static final RegistryObject<Item> COCOA_SUBSTITUTE  = plain("cocoa_substitute", 64);

    // ---- the Krave tool set -------------------------------------------------

    public static final RegistryObject<Item> KRAVE_PICKAXE =
            ITEMS.register("krave_pickaxe", () -> new KraveTools.KravePickaxe(props()));
    public static final RegistryObject<Item> KRAVE_SWORD =
            ITEMS.register("krave_sword", () -> new KraveTools.KraveSword(props()));
    public static final RegistryObject<Item> KRAVE_AXE =
            ITEMS.register("krave_axe", () -> new KraveTools.KraveAxe(props()));
    public static final RegistryObject<Item> KRAVE_SHOVEL =
            ITEMS.register("krave_shovel", () -> new KraveTools.KraveShovel(props()));
    public static final RegistryObject<Item> KRAVE_HOE =
            ITEMS.register("krave_hoe", () -> new KraveTools.KraveHoe(props()));

    public static final RegistryObject<Item> CAYDEN_COMPASS =
            ITEMS.register("cayden_compass", () -> new CaydenCompassItem(props().stacksTo(1)));

    // ---- the escape hatch (see Config.ALLOW_KRAVE_CLEANSE) -------------------

    public static final RegistryObject<Item> KRAVE_CLEANSE =
            ITEMS.register("krave_cleanse", () -> new KraveCleanseItem(props().stacksTo(1)));

    // ---- the Krave Kosmos portal ---------------------------------------------

    public static final RegistryObject<Item> KRAVE_BLOCK_ITEM =
            ITEMS.register("krave_block", () -> new BlockItem(ModBlocks.KRAVE_BLOCK.get(), props()));
    public static final RegistryObject<Item> KRAVE_DOOR_ITEM =
            ITEMS.register("krave_door", () -> new BlockItem(ModBlocks.KRAVE_DOOR.get(), props()));
    public static final RegistryObject<Item> KRAVE_TETHER =
            ITEMS.register("krave_tether", () -> new KraveTetherItem(props().stacksTo(4)));

    // ---- liquid chocolate ------------------------------------------------------

    public static final RegistryObject<Item> CHOCOLATE_BUCKET = ITEMS.register("chocolate_bucket",
            () -> new BucketItem(() -> ModFluids.CHOCOLATE.get(), props().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));
}
