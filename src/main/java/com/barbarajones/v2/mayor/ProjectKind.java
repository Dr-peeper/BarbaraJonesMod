package com.barbarajones.v2.mayor;

import com.barbarajones.content.ModItems;
import com.barbarajones.v2.mayor.def.MayorPrefabs;
import com.barbarajones.v2.village.KraveProfession;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * The ten things the player can commission, and everything true about each of
 * them: what it costs, what rank Barbara has to be to attempt it, what it
 * builds, who moves in, and what finishing it is worth to her.
 *
 * <p>One enum rather than ten classes, because every field here is data and the
 * pipeline in {@code KraveMayor} treats them all identically. The permit items
 * are registered by iterating these constants, so adding a project is one entry
 * and no other edits.
 *
 * <p><b>The {@link #key()} strings are written into save data.</b> Rename a
 * constant and every queued project in every existing world silently drops on
 * load. Append new constants at the end; never change a key.
 */
public enum ProjectKind {

    /**
     * The first thing anyone builds and the only thing that is free of anything
     * you cannot dig up with your hands.
     */
    KRAVE_SHACK("krave_shack", "Krave Shack Kit", MayorPrefabs.KRAVE_SHACK,
            "kit_krave_shack", 0, 2,
            new KraveProfession[]{KraveProfession.BUILDER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 20),
                    mat(() -> Items.DIRT, 8),
                    mat(ModItems.KRAVE_LOG_ITEM, 4))),

    /**
     * One more segment of road on whichever spur is shortest. What it looks like
     * depends on Barbara's rank, and it never looks better than last time - see
     * {@code RoadKit}.
     */
    ROAD_EXPANSION("road", "Road Expansion", null,
            "permit_road", 0, 1,
            new KraveProfession[0],
            List.of(mat(() -> Items.DIRT, 24),
                    mat(() -> Items.GRAVEL, 16),
                    mat(() -> Items.COARSE_DIRT, 8))),

    /** A shack that has been repaired enough times to have an annexe. */
    PATCHWORK_HOUSE("patchwork_house", "House Permit", MayorPrefabs.PATCHWORK_HOUSE,
            "permit_house", 1, 3,
            new KraveProfession[]{KraveProfession.GUARD, KraveProfession.COURIER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 32),
                    mat(() -> Items.COBBLESTONE, 16),
                    mat(() -> Items.OAK_PLANKS, 8),
                    mat(() -> Items.WHITE_WOOL, 6))),

    /** Three walls, a counter and a tarpaulin. The first shop. */
    MARKET_STALL("market_stall", "Market Stall Permit", MayorPrefabs.MARKET_STALL,
            "permit_market_stall", 1, 3,
            new KraveProfession[]{KraveProfession.GROCER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 16),
                    mat(() -> Items.OAK_PLANKS, 8),
                    mat(() -> Items.WHITE_WOOL, 6),
                    mat(() -> Items.HAY_BLOCK, 4))),

    /** A shop with a grille on it and more Krave in the back than out front. */
    CORNER_STORE("corner_store", "Corner Store Permit", MayorPrefabs.CORNER_STORE,
            "permit_corner_store", 2, 4,
            new KraveProfession[]{KraveProfession.GROCER, KraveProfession.COURIER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 32),
                    mat(() -> Items.COBBLESTONE, 24),
                    mat(() -> Items.IRON_INGOT, 8),
                    mat(() -> Items.GLASS, 8),
                    mat(ModItems.DOLLARS, 16))),

    /** Two storeys, every ground-floor window barred, television always on. */
    TRAP_HOUSE("trap_house", "Trap House Kit", MayorPrefabs.TRAP_HOUSE,
            "kit_trap_house", 2, 4,
            new KraveProfession[]{KraveProfession.GUARD, KraveProfession.CEREALOGIST,
                    KraveProfession.COURIER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 48),
                    mat(() -> Items.COBBLESTONE, 32),
                    mat(() -> Items.IRON_INGOT, 12),
                    mat(ModItems.DOLLARS, 24))),

    /** Every workstation in the game, in one shed, in everybody's way. */
    WORKSHOP("workshop", "Workshop Kit", MayorPrefabs.WORKSHOP,
            "kit_workshop", 3, 5,
            new KraveProfession[]{KraveProfession.BUILDER, KraveProfession.BUILDER},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 32),
                    mat(() -> Items.COBBLESTONE, 24),
                    mat(() -> Items.IRON_INGOT, 12),
                    mat(ModItems.KRAFTING_BENCH_ITEM, 1))),

    /** Three floors of the same flat. Six beds and one ladder. */
    STACKED_TENEMENT("stacked_tenement", "Stack Permit", MayorPrefabs.STACKED_TENEMENT,
            "permit_tenement", 4, 7,
            new KraveProfession[]{KraveProfession.GUARD, KraveProfession.BUILDER,
                    KraveProfession.COURIER, KraveProfession.GROCER,
                    KraveProfession.CEREALOGIST, KraveProfession.GUARD},
            List.of(mat(ModItems.KRAVE_PLANKS_ITEM, 64),
                    mat(() -> Items.COBBLESTONE, 48),
                    mat(() -> Items.WHITE_WOOL, 16),
                    mat(() -> Items.LADDER, 8),
                    mat(ModItems.DOLLARS, 32))),

    /**
     * The building that turns the village into an income. While one stands, the
     * mayor accrues a cut every tick and hands it over in Dollars.
     */
    PLUG_HEADQUARTERS("plug_hq", "Plug Headquarters", MayorPrefabs.PLUG_HEADQUARTERS,
            "permit_plug_headquarters", 5, 8,
            new KraveProfession[]{KraveProfession.GUARD, KraveProfession.GUARD,
                    KraveProfession.COURIER},
            List.of(mat(() -> Items.COBBLESTONE, 64),
                    mat(ModItems.KRAVE_PLANKS_ITEM, 48),
                    mat(() -> Items.IRON_INGOT, 24),
                    mat(ModItems.KRAVE_BLOCK_ITEM, 8),
                    mat(ModItems.FIVE_HUNDRED_DOLLARS, 1))),

    /** Sixteen courses of shed. The tallest shanty in the overworld. */
    KRAVE_SPIRE("krave_spire", "Spire Charter", MayorPrefabs.KRAVE_SPIRE,
            "permit_krave_spire", 6, 12,
            new KraveProfession[]{KraveProfession.CEREALOGIST, KraveProfession.GUARD},
            List.of(mat(() -> Items.COBBLESTONE, 96),
                    mat(ModItems.KRAVE_PLANKS_ITEM, 64),
                    mat(ModItems.KRAVE_BLOCK_ITEM, 16),
                    mat(() -> Items.IRON_INGOT, 32),
                    mat(ModItems.FIVE_HUNDRED_DOLLARS, 2)));

    private static final ProjectKind[] ALL = values();

    private final String key;
    private final String title;
    @Nullable
    private final ResourceLocation structure;
    private final String itemId;
    private final int minRank;
    private final int clout;
    private final KraveProfession[] staff;
    private final List<Material> materials;

    ProjectKind(String key, String title, @Nullable ResourceLocation structure, String itemId,
                int minRank, int clout, KraveProfession[] staff, List<Material> materials) {
        this.key = key;
        this.title = title;
        this.structure = structure;
        this.itemId = itemId;
        this.minRank = minRank;
        this.clout = clout;
        this.staff = staff;
        this.materials = materials;
    }

    /** Stable save key. Never change one of these; see the class javadoc. */
    public String key() {
        return this.key;
    }

    /** What the permit item and the report call it. */
    public String title() {
        return this.title;
    }

    /**
     * The structure this builds, or null for {@link #ROAD_EXPANSION}, which
     * picks its definition from Barbara's rank at the moment it is sited rather
     * than having one of its own.
     */
    @Nullable
    public ResourceLocation structure() {
        return this.structure;
    }

    /** Registry path of the permit item that commissions this. */
    public String itemId() {
        return this.itemId;
    }

    public int minRank() {
        return this.minRank;
    }

    /** What finishing it adds to Barbara's clout, which is what ranks her up. */
    public int clout() {
        return this.clout;
    }

    /**
     * Who moves in when it is finished, in the order of the definition's
     * {@code staff0}, {@code staff1}, ... markers. Empty for a road.
     */
    public KraveProfession[] staff() {
        return this.staff.clone();
    }

    public int residents() {
        return this.staff.length;
    }

    public List<Material> materials() {
        return this.materials;
    }

    public boolean isRoad() {
        return this == ROAD_EXPANSION;
    }

    /**
     * The line on the permit's tooltip. Kept as a switch rather than a
     * constructor argument so the constant list above stays readable as a table
     * of numbers - the prose is the odd one out among these fields, not the norm.
     */
    public String flavour() {
        switch (this) {
            case KRAVE_SHACK:
                return "Four walls, a roof you can stand on, and a lock she keeps the key to.";
            case ROAD_EXPANSION:
                return "More road. Not better road. She wants that understood up front.";
            case PATCHWORK_HOUSE:
                return "Sleeps two, if they get on. There is an annexe. Do not ask whose.";
            case MARKET_STALL:
                return "Somewhere to sell the cereal that is not the back of a van.";
            case CORNER_STORE:
                return "Open all hours. Shut whenever she likes.";
            case TRAP_HOUSE:
                return "The television stays on. Nobody has ever admitted to owning it.";
            case WORKSHOP:
                return "Every bench in the county, in one shed, in everybody's way.";
            case STACKED_TENEMENT:
                return "Three floors, six beds, one ladder. Mind the third rung.";
            case PLUG_HEADQUARTERS:
                return "The corner, with a roof on it. Pays weekly. Ask nothing.";
            case KRAVE_SPIRE:
            default:
                return "The tallest thing for miles and still nobody has fixed the door.";
        }
    }

    @Nullable
    public static ProjectKind byKey(String key) {
        for (ProjectKind kind : ALL) {
            if (kind.key.equals(key)) {
                return kind;
            }
        }
        return null;
    }

    public static ProjectKind[] all() {
        return ALL.clone();
    }

    // =====================================================================

    /**
     * One line of a project's bill of materials.
     *
     * <p>The item is held as a {@link Supplier} for the usual reason: these
     * lists are built in an enum's static initialiser, which runs during mod
     * construction, before the item registry has thawed. Resolving a
     * {@code RegistryObject} there throws.
     */
    public static final class Material {

        private final Supplier<Item> item;
        private final int count;

        private Material(Supplier<Item> item, int count) {
            this.item = item;
            this.count = count;
        }

        public Item item() {
            return this.item.get();
        }

        public int count() {
            return this.count;
        }

        /** Registry id of the item, used as the save key for what has been delivered. */
        public String id() {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item());
            return key == null ? "minecraft:air" : key.toString();
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(item());
        }

        /** Display name of the item, for the report and the tooltip. */
        public net.minecraft.network.chat.Component displayName() {
            return item().getDescription();
        }
    }

    private static Material mat(Supplier<Item> item, int count) {
        return new Material(item, count);
    }
}
