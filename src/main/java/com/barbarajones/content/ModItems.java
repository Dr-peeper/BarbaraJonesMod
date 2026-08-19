package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.extra.CigaretteItem;
import com.barbarajones.content.extra.CounterfeitBillItem;
import com.barbarajones.content.extra.FlipPhoneItem;
import com.barbarajones.content.extra.GoldenKraveItem;
import com.barbarajones.content.extra.KraveRadioItem;
import com.barbarajones.content.extra.LawnMowerItem;
import com.barbarajones.content.extra.RemoteControlItem;
import com.barbarajones.content.extra.TrophyItem;
import com.barbarajones.content.extra.WateringCanItem;
import com.barbarajones.content.extra.ZippoLighterItem;
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
import com.barbarajones.item.KravePropSpawnerItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
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
    public static final RegistryObject<Item> KRAVE_GRASS_ITEM =
            ITEMS.register("krave_grass", () -> new BlockItem(ModBlocks.KRAVE_GRASS.get(), props()));
    public static final RegistryObject<Item> KRAVE_DIRT_ITEM =
            ITEMS.register("krave_dirt", () -> new BlockItem(ModBlocks.KRAVE_DIRT.get(), props()));

    // ---- liquid chocolate ------------------------------------------------------

    public static final RegistryObject<Item> CHOCOLATE_BUCKET = ITEMS.register("chocolate_bucket",
            () -> new BucketItem(() -> ModFluids.CHOCOLATE.get(), props().stacksTo(1).craftRemainder(net.minecraft.world.item.Items.BUCKET)));

    // ---- spawn eggs ----------------------------------------------------------
    // ForgeSpawnEggItem only accepts EntityType<? extends Mob>, so the seven
    // plain-Entity props (the car, meteors, the tornado, beams, cinematics)
    // cannot have one - KravePropSpawnerItem covers those instead.

    public static final RegistryObject<Item> EGG_BARBARA = ITEMS.register("barbara_jones_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BARBARA, 0x6B4A2F, 0xC08A5A, props()));
    public static final RegistryObject<Item> EGG_CAYDEN = ITEMS.register("cayden_cobb_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CAYDEN, 0x3A1E6E, 0xD9A57F, props()));
    public static final RegistryObject<Item> EGG_KRAVE_MONSTER = ITEMS.register("krave_monster_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KRAVE_MONSTER, 0x281046, 0xB060D0, props()));
    public static final RegistryObject<Item> EGG_NUGGET = ITEMS.register("nugget_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NUGGET, 0xD98A3A, 0xF2C08A, props()));
    public static final RegistryObject<Item> EGG_DANIEL = ITEMS.register("daniel_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DANIEL, 0x2F4F2F, 0xD9A57F, props()));
    public static final RegistryObject<Item> EGG_MOM = ITEMS.register("mom_cobb_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MOM, 0x5A2A3A, 0xD9A57F, props()));
    public static final RegistryObject<Item> EGG_PLUG = ITEMS.register("the_plug_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PLUG, 0x101010, 0x2E7D32, props()));
    public static final RegistryObject<Item> EGG_DUHL_WOL = ITEMS.register("duhl_wol_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DUHL_WOL, 0x7A5232, 0xB0342A, props()));
    public static final RegistryObject<Item> EGG_KRAVE_MINION = ITEMS.register("krave_minion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KRAVE_MINION, 0x3A1E6E, 0x8B1A1A, props()));
    public static final RegistryObject<Item> EGG_KRAVE_HEALING_BOX = ITEMS.register("krave_healing_box_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KRAVE_HEALING_BOX, 0xB060D0, 0x4CAF50, props()));

    /** Cycles through the prop entities that cannot take a spawn egg. */
    public static final RegistryObject<Item> PROP_SPAWNER = ITEMS.register("krave_prop_spawner",
            () -> new KravePropSpawnerItem(props().stacksTo(1)));

    public static final RegistryObject<Item> MOMS_TV_REMOTE = plain("moms_tv_remote", 1);
    public static final RegistryObject<Item> CONFISCATED_KRAVE = edible("confiscated_krave",
            food(8, 0.9F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 120), 1.0F).build());
    public static final RegistryObject<Item> EGG_MOM_BOSS = ITEMS.register("mom_cobb_boss_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MOM_BOSS, 0x7A2233, 0x2A1D18, props()));
    public static final RegistryObject<Item> PINK_SLIP            = plain("pink_slip", 16);
    public static final RegistryObject<Item> SEVERANCE_CHECK      = plain("severance_check", 16);
    public static final RegistryObject<Item> EMPLOYEE_OF_THE_MONTH = plain("employee_of_the_month", 1);
    public static final RegistryObject<Item> EGG_THE_MANAGER = ITEMS.register("the_manager_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.THE_MANAGER, 0x2A2C33, 0x8E1218, props()));
    public static final RegistryObject<Item> EGG_MANAGER_MINION = ITEMS.register("manager_minion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MANAGER_MINION, 0x4C505A, 0x1E2C50, props()));
    // ---- EXTRA CONTENT: block items ----------------------------------------
    public static final RegistryObject<Item> STASH_BOX_ITEM =
            ITEMS.register("stash_box", () -> new BlockItem(ModBlocks.STASH_BOX.get(), props()));
    public static final RegistryObject<Item> BOOMBOX_ITEM =
            ITEMS.register("boombox", () -> new BlockItem(ModBlocks.BOOMBOX.get(), props()));
    public static final RegistryObject<Item> RECLINER_ITEM =
            ITEMS.register("recliner", () -> new BlockItem(ModBlocks.RECLINER.get(), props()));
    public static final RegistryObject<Item> TELEVISION_ITEM =
            ITEMS.register("television", () -> new BlockItem(ModBlocks.TELEVISION.get(), props()));
    public static final RegistryObject<Item> SEWER_PIPE_ITEM =
            ITEMS.register("sewer_pipe", () -> new BlockItem(ModBlocks.SEWER_PIPE.get(), props()));
    public static final RegistryObject<Item> SHAG_CARPET_ITEM =
            ITEMS.register("shag_carpet", () -> new BlockItem(ModBlocks.SHAG_CARPET.get(), props()));
    public static final RegistryObject<Item> WOOD_PANELING_ITEM =
            ITEMS.register("wood_paneling", () -> new BlockItem(ModBlocks.WOOD_PANELING.get(), props()));
    // ---- EXTRA CONTENT: the Krave line -------------------------------------
    /** One box in a hundred thousand. Eating it near Cayden puts him back to full. */
    public static final RegistryObject<Item> GOLDEN_KRAVE = ITEMS.register("golden_krave",
            () -> new GoldenKraveItem(props().stacksTo(4).rarity(Rarity.EPIC).food(food(8, 1.0F)
                    .alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 2), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 900, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 1200, 2), 1.0F).build())));
    public static final RegistryObject<Item> STALE_KRAVE = edible("stale_krave",
            food(2, 0.1F).effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 0.8F).build());
    public static final RegistryObject<Item> OFF_BRAND_KRAVE = edible("off_brand_krave",
            food(3, 0.2F)
                    .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 400), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 200), 0.5F).build());
    public static final RegistryObject<Item> KRAVE_DUST       = plain("krave_dust", 64);
    public static final RegistryObject<Item> KRAVE_FAMILY_BOX = plain("krave_family_box", 16);
    // ---- EXTRA CONTENT: smoking --------------------------------------------
    public static final RegistryObject<Item> CIGARETTE =
            ITEMS.register("cigarette", () -> new CigaretteItem(props().stacksTo(16)));
    public static final RegistryObject<Item> CIGARETTE_PACK = plain("cigarette_pack", 16);
    public static final RegistryObject<Item> CIGAR          = plain("cigar", 16);
    public static final RegistryObject<Item> MATCHBOOK      = plain("matchbook", 16);
    public static final RegistryObject<Item> ASH            = plain("ash", 64);
    public static final RegistryObject<Item> DANIELS_ZIPPO =
            ITEMS.register("daniels_zippo", () -> new ZippoLighterItem(props().stacksTo(1).durability(128)));
    // ---- EXTRA CONTENT: lawn and garden ------------------------------------
    // LawnMowerItem's second argument is the cut radius, so the whacker and the
    // mower are the same class at two sizes.
    public static final RegistryObject<Item> LAWN_MOWER = ITEMS.register("lawn_mower",
            () -> new LawnMowerItem(props().stacksTo(1).durability(250), 3));
    public static final RegistryObject<Item> WEED_WHACKER = ITEMS.register("weed_whacker",
            () -> new LawnMowerItem(props().stacksTo(1).durability(180), 1));
    public static final RegistryObject<Item> WATERING_CAN = ITEMS.register("watering_can",
            () -> new WateringCanItem(props().stacksTo(1).durability(200)));
    public static final RegistryObject<Item> HEDGE_TRIMMER =
            ITEMS.register("hedge_trimmer", () -> new Item(props().stacksTo(1).durability(200)));
    public static final RegistryObject<Item> RAKE =
            ITEMS.register("rake", () -> new Item(props().stacksTo(1).durability(150)));
    public static final RegistryObject<Item> FERTILIZER_BAG = plain("fertilizer_bag", 16);
    // ---- EXTRA CONTENT: the living room ------------------------------------
    public static final RegistryObject<Item> REMOTE_CONTROL =
            ITEMS.register("remote_control", () -> new RemoteControlItem(props().stacksTo(1)));
    public static final RegistryObject<Item> KRAVE_RADIO =
            ITEMS.register("krave_radio", () -> new KraveRadioItem(props().stacksTo(1)));
    public static final RegistryObject<Item> FLIP_PHONE =
            ITEMS.register("flip_phone", () -> new FlipPhoneItem(props().stacksTo(1)));
    public static final RegistryObject<Item> VHS_BLANK             = plain("vhs_blank", 16);
    public static final RegistryObject<Item> VHS_BARBARA_INTERVIEW = plain("vhs_barbara_interview", 16);
    public static final RegistryObject<Item> TV_ANTENNA            = plain("tv_antenna", 16);
    public static final RegistryObject<Item> BURNER_PHONE          = plain("burner_phone", 16);
    public static final RegistryObject<Item> LAPTOP                = plain("laptop", 1);
    public static final RegistryObject<Item> USB_DRIVE             = plain("usb_drive", 16);
    // ---- EXTRA CONTENT: the fridge and the drive-thru ----------------------
    public static final RegistryObject<Item> MR_PIBB_XTRA = edible("mr_pibb_xtra",
            food(3, 0.4F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240), 1.0F).build());
    public static final RegistryObject<Item> MR_PIBB_TWO_LITER = edible("mr_pibb_two_liter",
            food(6, 0.5F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 120), 0.4F).build());
    public static final RegistryObject<Item> CHEPINA_JUG = edible("chepina_jug",
            food(5, 0.5F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200), 1.0F).build());
    public static final RegistryObject<Item> DOUBLE_CHEESEBURGER = edible("double_cheeseburger", food(10, 0.9F).build());
    public static final RegistryObject<Item> CHICKEN_SANDWICH    = edible("chicken_sandwich", food(7, 0.7F).build());
    public static final RegistryObject<Item> HASH_BROWNS         = edible("hash_browns", food(5, 0.5F).build());
    public static final RegistryObject<Item> APPLE_PIE           = edible("apple_pie", food(6, 0.4F).build());
    public static final RegistryObject<Item> MILKSHAKE = edible("milkshake", food(5, 0.6F).alwaysEat().build());
    public static final RegistryObject<Item> SWEET_TEA = edible("sweet_tea",
            food(2, 0.2F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300), 1.0F).build());
    public static final RegistryObject<Item> HONEY_BUN = edible("honey_bun", food(6, 0.5F).build());
    public static final RegistryObject<Item> GAS_STATION_HOT_DOG = edible("gas_station_hot_dog",
            food(6, 0.4F).effect(() -> new MobEffectInstance(MobEffects.HUNGER, 300), 0.35F).build());
    public static final RegistryObject<Item> MICROWAVE_BURRITO = edible("microwave_burrito",
            food(8, 0.6F).effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100), 0.25F).build());
    public static final RegistryObject<Item> SEWER_WATER = edible("sewer_water",
            food(0, 0.0F).alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.POISON, 200), 1.0F)
                    .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 300), 1.0F).build());
    // ---- EXTRA CONTENT: the sewer and the red fit --------------------------
    public static final RegistryObject<Item> SEWER_KEY = plain("sewer_key", 16);
    public static final RegistryObject<Item> RAT_TAIL  = plain("rat_tail", 64);
    public static final RegistryObject<Item> RED_PANTS = plain("red_pants", 16);
    public static final RegistryObject<Item> RED_SHOES = plain("red_shoes", 16);
    // ---- EXTRA CONTENT: cash and scams -------------------------------------
    public static final RegistryObject<Item> COUNTERFEIT_BILL =
            ITEMS.register("counterfeit_bill", () -> new CounterfeitBillItem(props().stacksTo(16)));
    public static final RegistryObject<Item> IOU_NOTE     = plain("iou_note", 64);
    public static final RegistryObject<Item> DEBT_NOTICE  = plain("debt_notice", 64);
    public static final RegistryObject<Item> SCAM_RECEIPT = plain("scam_receipt", 64);
    // ---- EXTRA CONTENT: trophies -------------------------------------------
    // TrophyItem takes the RegistryObject rather than the SoundEvent so the clip
    // is resolved on use, long after sound registration has run.
    public static final RegistryObject<Item> KRAVE_MONSTER_TROPHY = ITEMS.register("krave_monster_trophy",
            () -> new TrophyItem(props().stacksTo(1).rarity(Rarity.EPIC),
                    "You put the Krave Monster in the ground.", ModSounds.KRAVE_ROAR));
    public static final RegistryObject<Item> PLUG_TROPHY = ITEMS.register("plug_trophy",
            () -> new TrophyItem(props().stacksTo(1).rarity(Rarity.RARE),
                    "The weed was fake. The scope was not.", ModSounds.EVT_ROLL));
    public static final RegistryObject<Item> DUHL_WOL_TROPHY = ITEMS.register("duhl_wol_trophy",
            () -> new TrophyItem(props().stacksTo(1).rarity(Rarity.RARE),
                    "He wanted five hundred. He got towed.", ModSounds.EVT_DEMOCRAT));
    public static final RegistryObject<Item> BARBARA_TROPHY = ITEMS.register("barbara_trophy",
            () -> new TrophyItem(props().stacksTo(1).rarity(Rarity.RARE),
                    "Employee of the Month. Nobody remembers the month.", ModSounds.EVT_MANAGER));
    public static final RegistryObject<Item> STASH_JAR     = plain("stash_jar", 16);
    public static final RegistryObject<Item> POCKET_SCALE  = plain("pocket_scale", 1);
    public static final RegistryObject<Item> NUGGET_COLLAR = plain("nugget_collar", 16);
    public static final RegistryObject<Item> CHOCOLATE_BAR = edible("chocolate_bar", food(6, 0.7F).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200), 1.0F).build());
}
