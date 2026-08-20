package com.barbarajones.v2.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * <h1>The Krave Village public API.</h1>
 *
 * Everything another module needs from the settlement system is on this class, and
 * nothing else in {@code com.barbarajones.v2.village} should be touched from
 * outside the package. Four other modules read this, so the contract below is the
 * thing that must not break.
 *
 * <h2>Wiring</h2>
 * Exactly one call is required, from the mod constructor:
 * <pre>{@code
 * KraveVillage.init(FMLJavaModLoadingContext.get().getModEventBus());
 * }</pre>
 * That registers this module's own {@code DeferredRegister}s. Everything else -
 * the Forge event handlers, the client renderers, the network channel - is wired
 * by {@code @Mod.EventBusSubscriber} inside this package and needs no central
 * change.
 *
 * <h2>Reading state</h2>
 * Every read takes a {@link ServerLevel}; villages are per-dimension. Reads are
 * cheap (a map lookup plus a small snapshot allocation) and always return a
 * value - there is no such thing as "the village system is not ready yet". Calling
 * any of these on the client, or with a null level, returns the neutral answer
 * (tier 0, empty optional, multiplier 1.0) rather than throwing.
 *
 * <pre>{@code
 * // the progression gate the quest module uses:
 * if (KraveVillage.tierOf(serverLevel) >= KraveVillage.PORTAL_TIER) {
 *     // the Krave dimension portal may light
 * }
 *
 * // or, equivalently and more readably:
 * if (KraveVillage.isPortalUnlocked(serverLevel)) { ... }
 *
 * // localised state, for something standing in a particular place:
 * KraveVillage.containing(serverLevel, pos)
 *             .ifPresent(v -> hud.show(v.tier(), v.population(), v.production()));
 * }</pre>
 *
 * <h2>Contributing to a village</h2>
 * The one extension point. Teach the system that a block is worth something and it
 * is counted from the moment it is placed, anywhere inside any claim:
 *
 * <pre>{@code
 * KraveVillage.registerVillageBuff(MyBlocks.WATCHTOWER.get(),
 *         VillageBuff.builder().building(3).defence(9).description("...").build());
 *
 * // RegistryObjects are fine too - resolution is deferred until registries thaw:
 * KraveVillage.registerVillageBuff(MyBlocks.CEREAL_SILO,
 *         VillageBuff.builder().building(2).production(6).build());
 * }</pre>
 *
 * Register during mod construction or common setup. Registering later is legal but
 * blocks already in the world are only picked up when the village's rolling sweep
 * next passes over them (about five minutes).
 *
 * <h2>Threading</h2>
 * The buff registry is concurrent, because Forge constructs mods in parallel and a
 * dependent mod may register from its own constructor. Everything else here must
 * be called from the server thread.
 */
public final class KraveVillage {

    // ---- constants other modules are expected to read ------------------------

    /**
     * The tier at which the Krave dimension portal becomes reachable, currently
     * {@link VillageTier#VILLAGE} (3). Gate on this constant, never on a literal -
     * balance moves and the quest module should move with it.
     */
    public static final int PORTAL_TIER = VillageTier.VILLAGE.index();

    /** Defence points to damage reduction. 100 defence hits the cap. */
    public static final float DEFENCE_TO_REDUCTION = 0.006F;

    /** The most raid damage a settlement can ever absorb. */
    public static final float MAX_DAMAGE_REDUCTION = 0.60F;

    // ---- the buff registry ---------------------------------------------------

    private static final Map<Block, VillageBuff> BUFFS = new ConcurrentHashMap<>();
    private static final List<PendingBuff> PENDING = Collections.synchronizedList(new ArrayList<>());

    private KraveVillage() { }

    // =========================================================================
    // Wiring
    // =========================================================================

    /**
     * The module's single entry point. Call once from the mod constructor with the
     * mod event bus. Registers this module's items, entity type and menu type; the
     * built-in {@link VillageBuffs} table is installed during common setup, after
     * registries have thawed.
     */
    public static void init(IEventBus modEventBus) {
        VillageRegistry.init(modEventBus);
    }

    // =========================================================================
    // Reading settlement state
    // =========================================================================

    /**
     * The development tier of the most developed settlement in this dimension,
     * 0..5. Returns 0 for a null level or a dimension with no villages.
     *
     * <p>This is the query the quest module gates the Krave portal on. Compare it
     * against {@link #PORTAL_TIER}, or use {@link #isPortalUnlocked} which does
     * exactly that.
     */
    public static int tierOf(@Nullable ServerLevel level) {
        return tierEnumOf(level).index();
    }

    /**
     * The tier of the settlement containing {@code pos}, or 0 if it is outside
     * every claim. Use this when the answer should be about a <em>place</em> -
     * a block being activated, a mob being hurt - rather than about the dimension
     * as a whole.
     */
    public static int tierOf(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::tierIndex).orElse(0);
    }

    /** As {@link #tierOf(ServerLevel)}, but typed. */
    public static VillageTier tierEnumOf(@Nullable ServerLevel level) {
        if (level == null) {
            return VillageTier.WILDERNESS;
        }
        return KraveVillageData.get(level).bestTier();
    }

    /** True once this dimension holds a settlement developed enough for the portal. */
    public static boolean isPortalUnlocked(@Nullable ServerLevel level) {
        return tierOf(level) >= PORTAL_TIER;
    }

    /**
     * The settlement whose claim contains {@code pos}. Empty if there is none.
     * Overlapping claims resolve to the one with the nearest origin.
     */
    public static Optional<VillageView> containing(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        Village village = KraveVillageData.get(level).containing(pos);
        return village == null ? Optional.empty() : Optional.of(village.view());
    }

    /**
     * The nearest settlement by origin, ignoring claim boundaries. Use this for
     * "point me at my town" behaviour - a compass, a map marker, a courier - where
     * being outside the claim is the whole point.
     */
    public static Optional<VillageView> nearest(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        Village village = KraveVillageData.get(level).nearest(pos);
        return village == null ? Optional.empty() : Optional.of(village.view());
    }

    /** A settlement by its stable id. */
    public static Optional<VillageView> byId(@Nullable ServerLevel level, @Nullable UUID id) {
        if (level == null || id == null) {
            return Optional.empty();
        }
        Village village = KraveVillageData.get(level).get(id);
        return village == null ? Optional.empty() : Optional.of(village.view());
    }

    /** Every settlement in this dimension. Usually one; the API does not assume it. */
    public static List<VillageView> all(@Nullable ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        List<VillageView> out = new ArrayList<>();
        for (Village village : KraveVillageData.get(level).villages()) {
            out.add(village.view());
        }
        return out;
    }

    /** Residents of the settlement at {@code pos}, or 0. */
    public static int populationOf(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::population).orElse(0);
    }

    /** Defence rating of the settlement at {@code pos}, or 0. */
    public static int defenceOf(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::defence).orElse(0);
    }

    /** Krave produced per real-world hour at {@code pos}, or 0. */
    public static int productionOf(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::production).orElse(0);
    }

    /** Happiness, 0..100, of the settlement at {@code pos}. 50 outside any claim. */
    public static int happinessOf(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::happiness).orElse(50);
    }

    // =========================================================================
    // Defence
    // =========================================================================

    /**
     * How much of an incoming hit still lands at {@code pos}: 1.0 out in the
     * wilderness, down to {@code 1 - }{@value #MAX_DAMAGE_REDUCTION} inside a fully
     * fortified capital.
     *
     * <p>The raid module should multiply its damage by this rather than reading the
     * defence rating and inventing its own curve, so that walls feel the same
     * whatever is attacking. This module already applies it to hostile damage dealt
     * to players and residents standing inside a claim - see {@code VillageEvents} -
     * so do not apply it twice to the same hit.
     */
    public static float raidDamageMultiplier(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return containing(level, pos).map(VillageView::raidDamageMultiplier).orElse(1.0F);
    }

    // =========================================================================
    // Mutating settlement state
    // =========================================================================

    /**
     * Founds a settlement at {@code origin}, or returns the existing one if that
     * position is already claimed. The founder, if given, is enrolled as a member.
     *
     * @return the settlement id, never null
     */
    public static UUID found(ServerLevel level, BlockPos origin, @Nullable Player founder, String name) {
        KraveVillageData data = KraveVillageData.get(level);
        Village village = data.create(origin, name);
        if (founder != null) {
            village.addMember(founder.getUUID());
        }
        data.setDirty();
        return village.id();
    }

    /** Enrols a player. Returns false if they were already a member or it is unknown. */
    public static boolean addMember(ServerLevel level, UUID villageId, UUID playerId) {
        KraveVillageData data = KraveVillageData.get(level);
        Village village = data.get(villageId);
        if (village == null || !village.addMember(playerId)) {
            return false;
        }
        data.setDirty();
        return true;
    }

    /** Adds Krave to the settlement store at {@code pos}, capped by tier. */
    public static void addKrave(ServerLevel level, BlockPos pos, int amount) {
        KraveVillageData data = KraveVillageData.get(level);
        Village village = data.containing(pos);
        if (village != null) {
            village.addKrave(amount);
            data.setDirty();
        }
    }

    /**
     * Takes up to {@code amount} Krave out of the settlement store.
     *
     * @return how much was actually available and removed
     */
    public static int withdrawKrave(ServerLevel level, UUID villageId, int amount) {
        KraveVillageData data = KraveVillageData.get(level);
        Village village = data.get(villageId);
        if (village == null) {
            return 0;
        }
        int taken = village.withdrawKrave(amount);
        if (taken > 0) {
            data.setDirty();
        }
        return taken;
    }

    /**
     * Shifts the happiness of the settlement at {@code pos}. Happiness drifts back
     * toward its structural target over the next few minutes, so this is a nudge,
     * not a setting - good for one-off events (a boss killed nearby, a resident
     * murdered) rather than for standing modifiers, which belong on a
     * {@link VillageBuff}.
     */
    public static void adjustHappiness(ServerLevel level, BlockPos pos, int delta) {
        KraveVillageData data = KraveVillageData.get(level);
        Village village = data.containing(pos);
        if (village != null) {
            village.adjustHappiness(delta);
            data.setDirty();
        }
    }

    // =========================================================================
    // The extension point
    // =========================================================================

    /**
     * Declares what a block contributes to any settlement it is placed in.
     *
     * <p>Later registrations for the same block replace earlier ones, so a data
     * pack or an addon can retune the base table. Registering
     * {@link VillageBuff#NONE} removes a block from the system entirely.
     *
     * <p>Safe to call from any mod's constructor, including in parallel.
     */
    public static void registerVillageBuff(Block block, VillageBuff buff) {
        if (block == null || buff == null) {
            return;
        }
        BUFFS.put(block, buff);
    }

    /**
     * Deferred form, for {@code RegistryObject} and friends. The supplier is
     * resolved immediately if it can be, and otherwise queued until common setup -
     * so this is safe to call from a static initialiser, where calling
     * {@code RegistryObject.get()} yourself would throw.
     */
    public static void registerVillageBuff(Supplier<? extends Block> block, VillageBuff buff) {
        if (block == null || buff == null) {
            return;
        }
        try {
            Block resolved = block.get();
            if (resolved != null) {
                BUFFS.put(resolved, buff);
                return;
            }
        } catch (RuntimeException notYet) {
            // registries have not thawed; fall through and queue it
        }
        PENDING.add(new PendingBuff(block, buff));
    }

    /**
     * What this block contributes. Never null - unregistered blocks return
     * {@link VillageBuff#NONE}, for which {@link VillageBuff#isNothing()} is true.
     */
    public static VillageBuff buffOf(@Nullable Block block) {
        if (block == null) {
            return VillageBuff.NONE;
        }
        VillageBuff buff = BUFFS.get(block);
        return buff == null ? VillageBuff.NONE : buff;
    }

    /** True if this block counts for anything at all. Cheap; use it as a filter. */
    public static boolean hasBuff(@Nullable Block block) {
        return block != null && BUFFS.containsKey(block);
    }

    /** How many blocks currently carry a buff. Diagnostics only. */
    public static int registeredBuffCount() {
        return BUFFS.size();
    }

    /**
     * Drains the deferred-registration queue. Called from common setup by
     * {@link VillageRegistry}; there is no reason for anything else to call it.
     */
    static void resolvePendingBuffs() {
        synchronized (PENDING) {
            for (PendingBuff pending : PENDING) {
                try {
                    Block block = pending.block.get();
                    if (block != null) {
                        BUFFS.put(block, pending.buff);
                    }
                } catch (RuntimeException broken) {
                    // A buff for a block that never registered is a mistake in the
                    // caller, not a reason to abort startup for everyone else.
                    com.mojang.logging.LogUtils.getLogger().warn(
                            "[barbarajones] a deferred village buff could not be resolved", broken);
                }
            }
            PENDING.clear();
        }
    }

    private record PendingBuff(Supplier<? extends Block> block, VillageBuff buff) { }

    // =========================================================================
    // Convenience for callers that only have a Level
    // =========================================================================

    /**
     * {@link #tierOf(ServerLevel)} for callers holding an untyped {@link Level}.
     * Returns 0 on the client, where village state does not exist - if you need a
     * tier client-side, read the synced HUD state instead of asking for it here.
     */
    public static int tierOf(@Nullable Level level) {
        return level instanceof ServerLevel server ? tierOf(server) : 0;
    }
}
