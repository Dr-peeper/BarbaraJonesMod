package com.barbarajones.v2.mayor;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModTabs;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.v2.build.def.PlacementContext;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.place.BuildScheduler;
import com.barbarajones.v2.build.place.KraveStructure;
import com.barbarajones.v2.build.place.PlacementResult;
import com.barbarajones.v2.mayor.def.MayorPrefabs;
import com.barbarajones.v2.village.KraveProfession;
import com.barbarajones.v2.village.KraveVillageData;
import com.barbarajones.v2.village.KraveVillagerEntity;
import com.barbarajones.v2.village.Village;
import com.barbarajones.v2.village.VillageRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>Barbara Jones, mayor.</h1>
 *
 * The module that turns her from a mob you feed grass to into the person who
 * runs the Krave Village: a queue of discrete, funded, sited, staged building
 * projects, a rank that climbs as she finishes them, and a town that gets very
 * large without ever getting tidy.
 *
 * <h2>Wiring</h2>
 * Two lines, and the second one is already written.
 * <pre>{@code
 * // in the mod constructor, after KraveBuild.init(bus) and KraveVillage.init(bus):
 * com.barbarajones.v2.mayor.KraveMayor.init(bus);
 * }</pre>
 * The tick is driven from {@code BarbaraJones#tick}, which calls {@link #tick}
 * every server tick and is gated here rather than there. Everything else - the
 * creative tab, the item registry - is wired inside this package.
 *
 * <h2>The pipeline</h2>
 * A project moves through four states and is only ever pushed by the mayor tick:
 * <pre>
 *   permit handed over  ->  FUNDING
 *   materials delivered ->  SITING
 *   a plot passed every check, KraveStructure.place accepted it -> BUILDING
 *   the BuildJob reports complete -> staff move in, clout is booked, DONE
 * </pre>
 * <b>At most one project is submitted per mayor tick</b>, and a mayor tick is
 * one in every {@value #TICK_INTERVAL} - so the village grows in discrete
 * events, ten seconds apart at the very fastest, and there is no code path
 * anywhere in this module that places blocks continuously.
 *
 * <h2>The five things that keep it safe</h2>
 * <ol>
 *   <li><b>It only runs while Barbara is loaded and near her village.</b> The
 *       tick is her tick. Nothing here can build in a chunk nobody is standing
 *       in, because nothing here runs when she is not there.
 *   <li><b>Every placement goes through {@code KraveStructure.place}.</b> That
 *       is what enforces the two big guarantees this module never re-implements:
 *       {@code TerrainRules} refuses any footprint containing a manufactured or
 *       {@code PlayerBuiltLedger}-recorded block, so nothing the player built is
 *       ever overwritten; and {@code BuildScheduler} lays the blocks over a few
 *       seconds and force-finishes on level unload, so no building is ever left
 *       half-placed.
 *   <li><b>{@link MayorSafety}</b> adds the four things the placement engine
 *       cannot know: the boss arena, the Kraved Castle, the void, and unloaded
 *       chunks.
 *   <li><b>Bounded everything.</b> Settlement radius by rank and never past the
 *       village claim; {@link MayorSettlement#MAX_QUEUE} commissions;
 *       {@link MayorRank#concurrentProjects} builds at once, and never more than
 *       {@value #MAX_LEVEL_JOBS} jobs in the level whatever the rank says; a
 *       {@value #PROJECT_COOLDOWN} tick pause after each completion.
 *   <li><b>Nothing authoritative is static.</b> Every number is in
 *       {@link MayorData}, a {@code SavedData}. The fields on this class are the
 *       item registry and constants.
 * </ol>
 *
 * <h2>Population, and the one number two systems could have fought over</h2>
 * The village module already spawns residents on its own attraction timer,
 * capped by {@code VillageTier#populationCap}. If the mayor had its own cap the
 * two would disagree and whichever ran more often would win. It does not:
 * {@link #residentCap} is the smaller of the tier cap and
 * {@link MayorRank#residentSupport}, so the mayor is always the more
 * conservative of the two and can never push a settlement past the ceiling the
 * village module is already enforcing.
 */
public final class KraveMayor {

    // ---- registry ----------------------------------------------------------

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BarbaraJonesMod.MODID);

    /** One permit item per project, registered in a loop. */
    private static final Map<ProjectKind, RegistryObject<Item>> PERMITS = registerPermits();

    // ---- tuning ------------------------------------------------------------

    /** Ticks between mayor updates. Everything below is "per mayor tick". */
    public static final int TICK_INTERVAL = 200;

    /** Quiet spell after each finished project before the next one may start. */
    public static final int PROJECT_COOLDOWN = 600;

    /**
     * Hard ceiling on placement jobs running in one level, on top of the
     * per-rank limit. Two mayors in two villages in the same dimension must not
     * be able to add up to a stutter.
     */
    public static final int MAX_LEVEL_JOBS = 4;

    /** How far outside a claim Barbara will still consider herself its mayor. */
    public static final int MAYOR_RANGE = 96;

    private KraveMayor() { }

    private static Map<ProjectKind, RegistryObject<Item>> registerPermits() {
        Map<ProjectKind, RegistryObject<Item>> map = new EnumMap<>(ProjectKind.class);
        for (ProjectKind kind : ProjectKind.all()) {
            map.put(kind, ITEMS.register(kind.itemId(),
                    () -> new MayorPermitItem(new Item.Properties().stacksTo(4), kind)));
        }
        return Collections.unmodifiableMap(map);
    }

    /** The permit item that commissions a project. */
    public static RegistryObject<Item> permit(ProjectKind kind) {
        return PERMITS.get(kind);
    }

    /**
     * The module's single wiring call. From the mod constructor, on the mod
     * event bus.
     *
     * <p>{@code registerAll} bakes nineteen structure definitions here rather
     * than later on purpose: definitions must exist before anything can reference
     * them, and mod construction is the last moment that is guaranteed to happen
     * before a world loads.
     */
    public static void init(IEventBus modEventBus) {
        MayorPrefabs.registerAll();
        ITEMS.register(modEventBus);
    }

    /**
     * Puts the permits in the mod's creative tab.
     *
     * <p>Its own subscriber rather than an entry in {@code V2Tabs}, which is
     * another module's file. Registering an item and then never adding it to a
     * tab is a mistake that looks completely healthy - it compiles, it
     * registers, it works in commands, and the player can never find it.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Tabs {

        private Tabs() { }

        @SubscribeEvent
        public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
            if (!event.getTabKey().equals(ModTabs.MAIN.getKey())) {
                return;
            }
            for (RegistryObject<Item> item : ITEMS.getEntries()) {
                event.accept(item.get());
            }
        }
    }

    // =====================================================================
    // The tick
    // =====================================================================

    /**
     * Called every tick from {@code BarbaraJones#tick}; does work on one tick in
     * {@value #TICK_INTERVAL}.
     *
     * <p>The gate lives here rather than at the call site so that the interval
     * and the pipeline it paces stay in one file.
     */
    public static void tick(BarbaraJones barbara) {
        if (!(barbara.level() instanceof ServerLevel level)) {
            return;
        }
        if (barbara.tickCount % TICK_INTERVAL != 0) {
            return;
        }
        KraveVillageData villages = KraveVillageData.getExisting(level);
        if (villages == null) {
            return;
        }
        Village village = villageFor(villages, barbara.blockPosition());
        if (village == null) {
            return;
        }

        MayorData office = MayorData.get(level);
        pruneDissolvedVillages(villages, office);

        MayorSettlement settlement = office.settlementFor(village.id());
        if (!settlement.claim(barbara.getUUID(), level.getGameTime())) {
            // Another Barbara already holds this job. Two mayors on one village
            // would run two copies of the pipeline and build everything twice.
            return;
        }

        // A raging Barbara starts nothing new. This is the one place the stash
        // mechanic reaches into the town: let her supply run dry and the public
        // works stop until somebody sorts her out. Work already going up still
        // finishes and still gets booked - the builders do not walk off site
        // because the mayor is having a moment.
        boolean changed = settlement.accruePayout();
        if (advance(level, villages, village, settlement, !barbara.isRaging())) {
            changed = true;
        }
        if (changed) {
            office.setDirty();
        }
    }

    /**
     * One pass over the queue. Returns true if anything changed that needs
     * saving.
     *
     * <p>Only one project may reach {@code KraveStructure.place} per pass. A
     * project that cannot start does not block the ones behind it - it records
     * why and the loop carries on - so a Spire charter handed over at Squatter
     * sits in the list going nowhere without stopping the shacks.
     *
     * <p>A refusal deliberately does <em>not</em> count as a change worth
     * saving. The stall reason is live state the report reads directly, and
     * marking the file dirty every ten seconds because a queue is still waiting
     * on materials would have every idle village in the world rewriting its save
     * entry forever. The cost is that a reason can be one tick stale immediately
     * after a reload, which nothing can observe.
     *
     * @param mayStart false while she is raging: nothing new is begun, but
     *                 anything already going up still completes
     */
    private static boolean advance(ServerLevel level, KraveVillageData villages, Village village,
                                   MayorSettlement settlement, boolean mayStart) {
        boolean changed = false;
        boolean submitted = false;
        long now = level.getGameTime();

        Iterator<MayorProject> it = settlement.queue().iterator();
        while (it.hasNext()) {
            MayorProject project = it.next();
            switch (project.state()) {
                case FUNDING:
                    project.setStall(MayorProject.Stall.MATERIALS);
                    break;
                case SITING:
                    if (!mayStart) {
                        project.setStall(MayorProject.Stall.COOLING_OFF);
                    } else if (!submitted && trySubmit(level, village, settlement, project, now)) {
                        submitted = true;
                        changed = true;
                    }
                    break;
                case BUILDING:
                    if (project.buildFinished()) {
                        complete(level, villages, village, settlement, project, now);
                        project.markDone();
                        it.remove();
                        changed = true;
                    }
                    break;
                case DONE:
                default:
                    it.remove();
                    changed = true;
                    break;
            }
        }
        return changed;
    }

    /**
     * Tries to turn a funded project into a building going up.
     *
     * <p>Every refusal sets a stall reason and returns false, which is a normal
     * outcome rather than an error - the report reads those reasons back to the
     * player so a queue that is not moving always says why.
     */
    private static boolean trySubmit(ServerLevel level, Village village, MayorSettlement settlement,
                                     MayorProject project, long now) {
        ProjectKind kind = project.kind();
        MayorRank rank = settlement.rank();

        if (rank.index() < kind.minRank()) {
            project.setStall(MayorProject.Stall.RANK);
            return false;
        }
        if (settlement.building() >= rank.concurrentProjects()
                || BuildScheduler.activeJobs(level) >= MAX_LEVEL_JOBS) {
            project.setStall(MayorProject.Stall.BUSY);
            return false;
        }
        if (now - settlement.lastCompletionTick() < PROJECT_COOLDOWN) {
            project.setStall(MayorProject.Stall.COOLING_OFF);
            return false;
        }
        if (kind.residents() > 0
                && village.population() + kind.residents() > residentCap(village, rank)) {
            project.setStall(MayorProject.Stall.HOUSING_FULL);
            return false;
        }

        SiteSelector.Site site;
        if (kind.isRoad()) {
            site = SiteSelector.pickRoad(level, village, settlement);
        } else {
            StructureDef def = MayorPrefabs.get(kind.structure());
            site = def == null ? null : SiteSelector.pickBuilding(level, village, settlement, def);
        }
        if (site == null) {
            project.setStall(MayorProject.Stall.NO_SITE);
            return false;
        }

        PlacementResult result = KraveStructure.place(level, site.anchor(), site.rotation(), site.def());
        if (!result.started()) {
            // The engine refused after all - almost always because another job
            // claims overlapping space. Try somewhere else in ten seconds.
            project.setStall(MayorProject.Stall.NO_SITE);
            return false;
        }

        project.markBuilding(site.centre(), staffSpots(site, kind), result.job());
        settlement.noteSite(site.centre());
        if (site.roadDirection() >= 0) {
            settlement.extendRoadSpur(site.roadDirection());
        }
        announce(level, village, "Barbara has started on the " + kind.title() + ".");
        return true;
    }

    /**
     * Resolves the definition's {@code staffN} markers to world positions, now,
     * while the rotation and origin are still to hand.
     *
     * <p>They are persisted on the project rather than recomputed at completion
     * because the {@code PlacementCheck} they come from does not survive a
     * reload, and a building finished after a restart still has to know where to
     * put its residents.
     */
    private static List<BlockPos> staffSpots(SiteSelector.Site site, ProjectKind kind) {
        List<BlockPos> out = new ArrayList<>(kind.residents());
        StructureDef def = site.def();
        for (int i = 0; i < kind.residents(); i++) {
            BlockPos local = def.markers().get("staff" + i);
            if (local == null) {
                continue;
            }
            out.add(PlacementContext.world(def, site.rotation(), site.check().origin(),
                    local.getX(), local.getY(), local.getZ()));
        }
        return out;
    }

    /** Books a finished project: residents in, clout up, mood up, everybody told. */
    private static void complete(ServerLevel level, KraveVillageData villages, Village village,
                                 MayorSettlement settlement, MayorProject project, long now) {
        MayorRank before = settlement.rank();

        moveStaffIn(level, village, settlement, project);
        settlement.noteCompleted(project.kind(), now);
        village.adjustHappiness(3);
        villages.setDirty();

        announce(level, village, "The " + project.kind().title() + " is up. Mind the step.");
        MayorRank after = settlement.rank();
        if (after != before) {
            announce(level, village, "Barbara Jones is now " + after.title() + ".");
            level.playSound(null, village.origin(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.7F, 1.1F);
        }
    }

    /**
     * Spawns the building's residents at its own staff markers and enrols them
     * in the settlement, which is the same enrolment path the village module's
     * own attraction spawner uses.
     *
     * <p>The cap is re-tested inside the loop rather than once outside it,
     * because the population moves as each one lands and because minutes may
     * have passed since the project was sited.
     */
    private static void moveStaffIn(ServerLevel level, Village village, MayorSettlement settlement,
                                    MayorProject project) {
        KraveProfession[] jobs = project.kind().staff();
        List<BlockPos> spots = project.staffSpots();
        int cap = residentCap(village, settlement.rank());

        for (int i = 0; i < jobs.length && i < spots.size(); i++) {
            if (village.population() >= cap) {
                return;
            }
            BlockPos at = spots.get(i);
            if (!level.hasChunkAt(at)) {
                continue;
            }
            KraveVillagerEntity villager = VillageRegistry.KRAVE_VILLAGER.get().create(level);
            if (villager == null) {
                continue;
            }
            villager.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            villager.setProfession(jobs[i]);
            villager.setVillageId(village.id());
            villager.finalizeSpawn(level, level.getCurrentDifficultyAt(at),
                    MobSpawnType.EVENT, null, null);
            level.addFreshEntity(villager);
            village.registerVillager(villager.getUUID());
        }
    }

    /**
     * How many residents this settlement may hold, and the only answer to that
     * question this module recognises.
     *
     * <p>The smaller of what the village's own tier allows and what Barbara can
     * feed. Taking the minimum rather than picking one is what stops the mayor
     * and the village module owning the same number and disagreeing about it -
     * see the class javadoc.
     */
    public static int residentCap(Village village, MayorRank rank) {
        return Math.min(village.tier().populationCap(), rank.residentSupport());
    }

    /** Drops office records for villages that have since been dissolved. */
    private static void pruneDissolvedVillages(KraveVillageData villages, MayorData office) {
        for (UUID id : office.villageIds()) {
            if (villages.get(id) == null) {
                office.forget(id);
            }
        }
    }

    @Nullable
    private static Village villageFor(KraveVillageData villages, BlockPos where) {
        Village inside = villages.containing(where);
        if (inside != null) {
            return inside;
        }
        Village nearest = villages.nearest(where);
        if (nearest != null
                && nearest.horizontalDistanceSqr(where) <= (double) MAYOR_RANGE * MAYOR_RANGE) {
            return nearest;
        }
        return null;
    }

    private static void announce(ServerLevel level, Village village, String text) {
        Component message = Component.literal(text).withStyle(ChatFormatting.GRAY);
        for (ServerPlayer player : level.players()) {
            if (village.contains(player.blockPosition()) || village.isMember(player.getUUID())) {
                player.displayClientMessage(message, false);
            }
        }
    }

    // =====================================================================
    // Talking to her
    // =====================================================================

    /**
     * The mayor half of {@code BarbaraJones#mobInteract}.
     *
     * <p>Claims the interaction in exactly two cases, both of which the client
     * can decide for itself so that the two sides never disagree about who
     * handled the click:
     * <ul>
     *   <li>the held item is a {@link MayorPermitItem} - commission it;
     *   <li>the player is sneaking - deliver materials if the stack is something
     *       a queued project wants, otherwise print the report and hand over any
     *       money owing.
     * </ul>
     * Grass is excluded even while sneaking: feeding her is the older gesture
     * and the one people reach for by reflex, and it must not stop working.
     *
     * @return {@link InteractionResult#PASS} when this is not a mayoral
     *         interaction, leaving her existing behaviour untouched
     */
    public static InteractionResult interact(BarbaraJones barbara, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean permit = held.getItem() instanceof MayorPermitItem;
        if (!permit && (!player.isShiftKeyDown() || BarbaraJones.isGrass(held))) {
            return InteractionResult.PASS;
        }
        if (!(barbara.level() instanceof ServerLevel level) || !(player instanceof ServerPlayer server)) {
            // The client half of the interaction. It has already agreed with the
            // server about who is handling this click - both sides ran the same
            // two tests above - so it says yes, swings the arm, and lets the
            // server do all the actual work.
            return InteractionResult.sidedSuccess(barbara.level().isClientSide);
        }

        KraveVillageData villages = KraveVillageData.getExisting(level);
        Village village = villages == null ? null : villageFor(villages, barbara.blockPosition());
        if (village == null) {
            say(server, "Barbara: \"There's no town here yet, sugar. Plant a charter and I'll run it.\"");
            return InteractionResult.CONSUME;
        }

        MayorData office = MayorData.get(level);
        MayorSettlement settlement = office.settlementFor(village.id());
        // Being spoken to counts as a heartbeat, so the Barbara you are talking
        // to is the Barbara who holds the job.
        settlement.claim(barbara.getUUID(), level.getGameTime());

        if (permit) {
            commission(server, held, office, settlement);
            return InteractionResult.CONSUME;
        }
        if (deliver(server, held, office, settlement)) {
            return InteractionResult.CONSUME;
        }

        int collected = settlement.collectPayout();
        if (collected > 0) {
            payDollars(server, collected);
        }
        MayorReport.send(server, village, settlement,
                residentCap(village, settlement.rank()), barbara.isRaging(), collected);
        issueStarterPermit(server, settlement);
        office.setDirty();
        return InteractionResult.CONSUME;
    }

    /**
     * Hands the player their first Krave Shack Kit the first time they ask
     * Barbara about a settlement she is running.
     *
     * <p>Not a nicety: there is no recipe for a permit, and recipes live in data
     * files this module does not own, so without this the whole mayoral system
     * would be reachable only from the creative tab. If recipes are added later,
     * this stays - it is the tutorial beat that tells the player what the items
     * are for.
     */
    private static void issueStarterPermit(ServerPlayer player, MayorSettlement settlement) {
        if (!settlement.issueStarterPermit()) {
            return;
        }
        ItemStack kit = new ItemStack(permit(ProjectKind.KRAVE_SHACK).get());
        if (!player.addItem(kit)) {
            player.drop(kit, false);
        }
        say(player, "Barbara hands you a folded, chocolate-smudged " + ProjectKind.KRAVE_SHACK.title()
                + ". \"Get me the makings and I'll get somebody off the ground.\"");
    }

    /** Takes a permit and puts the project on the books. */
    private static void commission(ServerPlayer player, ItemStack held, MayorData office,
                                   MayorSettlement settlement) {
        ProjectKind kind = MayorPermitItem.kindOf(held);
        if (kind == null) {
            return;
        }
        if (settlement.queueFull()) {
            say(player, "Barbara: \"That list is as long as I'm carrying it. One thing at a time.\"");
            return;
        }
        settlement.enqueue(new MayorProject(kind));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        office.setDirty();

        say(player, "Barbara takes the " + kind.title() + ".");
        if (settlement.rank().index() < kind.minRank()) {
            // Accepted anyway. It sits in the queue with its reason on it rather
            // than being refused, which is why the RANK stall reason exists.
            say(player, "\"I'm only " + settlement.rank().title() + ", mind. It'll keep.\"");
        } else {
            say(player, "\"Bring me the makings and I'll find it a plot.\"");
        }
    }

    /**
     * Offers a stack to the first queued project that wants any of it.
     *
     * @return true if anything was taken, in which case the stack has already
     *         been shrunk by exactly that much
     */
    private static boolean deliver(ServerPlayer player, ItemStack held, MayorData office,
                                   MayorSettlement settlement) {
        if (held.isEmpty()) {
            return false;
        }
        for (MayorProject project : settlement.queue()) {
            int taken = project.deliver(held);
            if (taken <= 0) {
                continue;
            }
            office.setDirty();
            if (project.isFunded()) {
                say(player, "Barbara: \"That's the lot. Give me a minute to find somewhere.\"");
            } else {
                say(player, "Barbara takes " + taken + ". \"Keep it coming.\"");
            }
            return true;
        }
        return false;
    }

    /**
     * Hands over the take in Dollars, split into stacks by hand.
     *
     * <p>The payout cap is well over a stack, and a single oversized
     * {@link ItemStack} is the sort of thing that works until some other mod's
     * inventory handler sees it.
     */
    private static void payDollars(ServerPlayer player, int amount) {
        int left = amount;
        while (left > 0) {
            int batch = Math.min(64, left);
            left -= batch;
            ItemStack money = new ItemStack(ModItems.DOLLARS.get(), batch);
            if (!player.addItem(money)) {
                player.drop(money, false);
            }
        }
    }

    private static void say(ServerPlayer player, String line) {
        player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GRAY));
    }
}
