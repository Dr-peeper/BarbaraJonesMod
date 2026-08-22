package com.barbarajones.v2.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One settlement. The mutable, server-side truth.
 *
 * <p>Nothing outside this package should hold a reference to a {@code Village};
 * {@link KraveVillage} hands out immutable {@link VillageView} snapshots instead.
 * All of this is owned by {@link KraveVillageData}, which is the {@code SavedData}
 * that actually persists it - <b>every mutation here must be followed by a
 * {@code setDirty()} on that owner</b>, which is why almost every method takes or
 * is called from a context that does exactly that. Forgetting it is the classic
 * SavedData bug: everything works until the world is closed, then the last hour
 * evaporates.
 *
 * <h2>How buildings are tracked</h2>
 * The village keeps a map of {@code BlockPos -> Block} for every placed block that
 * has a registered {@link VillageBuff}. It is maintained from two directions:
 *
 * <ul>
 *   <li><b>Instantly</b>, from block place/break events, so putting down a bed
 *       makes the population counter move within a second.
 *   <li><b>By a rolling sweep</b> - {@value #CELLS_PER_TICK} 16x16x16 cells per
 *       village tick, {@value #TOTAL_CELLS} cells to a full pass. That is roughly
 *       five minutes for a complete re-verification of the claim, at about twelve
 *       thousand block reads every five seconds. The sweep is what corrects drift
 *       from the things that never fire a break event: explosions, pistons, fire,
 *       another mod's world edit, a chunk restored from backup.
 * </ul>
 *
 * The sweep only trusts cells whose chunks were actually loaded when it looked, so
 * a village half-out of render distance never has its far half deleted.
 */
public class Village {

    // ---- claim geometry -----------------------------------------------------

    /** Horizontal claim radius, in blocks, measured from the origin. */
    public static final int CLAIM_RADIUS = 56;
    /** Vertical claim reach, up and down, from the origin. */
    public static final int CLAIM_HEIGHT = 32;

    private static final int CELL = 16;
    private static final int CELLS_X = (CLAIM_RADIUS * 2) / CELL;   // 7
    private static final int CELLS_Y = (CLAIM_HEIGHT * 2) / CELL;   // 4
    private static final int CELLS_Z = (CLAIM_RADIUS * 2) / CELL;   // 7
    static final int TOTAL_CELLS = CELLS_X * CELLS_Y * CELLS_Z;     // 196
    private static final int CELLS_PER_TICK = 3;

    /**
     * Hard ceiling on tracked buff blocks. A player who paves a whole claim in
     * torches would otherwise write an unbounded list into level.dat every save.
     */
    public static final int MAX_TRACKED_BUILDINGS = 4096;

    // ---- economy tuning -----------------------------------------------------

    /** Ticks between village updates. Everything below is "per village tick". */
    public static final int VILLAGE_TICK_INTERVAL = 100;
    /** 72000 game ticks in an hour / VILLAGE_TICK_INTERVAL. */
    private static final int VILLAGE_TICKS_PER_HOUR = 720;
    /** Attraction points needed to pull one new resident in. */
    private static final int ATTRACT_THRESHOLD = 600;

    /** How close a hostile has to be to a resident before it notices them. */
    private static final int AGGRO_RANGE = 16;
    /** Ceiling on retargets per village tick, so a horde cannot spike a tick. */
    private static final int MAX_AGGRO_PER_TICK = 4;

    // ---- identity -----------------------------------------------------------

    private final UUID id;
    private BlockPos origin;
    private String name;

    // ---- membership ---------------------------------------------------------

    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> villagers = new HashSet<>();

    // ---- tracked world state ------------------------------------------------

    private final Map<BlockPos, Block> buildings = new HashMap<>();

    // ---- derived, recomputed every village tick ------------------------------

    private VillageTier tier = VillageTier.WILDERNESS;
    private int buildingScore;
    private int defence;
    private int production;
    private int attraction;
    private int happiness = 50;

    // ---- accumulators, persisted so progress is not lost on reload -----------

    private int produceProgress;
    private int attractProgress;
    private int stockpile;

    /**
     * Cached profession tallies. Refreshed whenever a census can actually see the
     * residents; persisted so a freshly loaded world does not report a defenceless
     * town for the first five minutes.
     */
    private int guardPower;

    /**
     * How many guards are actually standing in the village.
     *
     * <p>Kept next to guardPower rather than derived from it, because they
     * answer different questions and the profession chooser needs both. Power
     * is defence-per-level summed over everyone, so one veteran reads the same
     * as five recruits - which is right for working out whether the place can
     * defend itself, and useless for working out whether the streets are
     * already full of them.
     */
    private int guardCount;
    private int professionProduction;
    private int professionAttraction;
    private int builderBonus;

    // ---- sweep state, deliberately NOT persisted -----------------------------

    private int sweepCursor;
    private final Map<BlockPos, Block> sweepPending = new HashMap<>();
    private final boolean[] sweptCells = new boolean[TOTAL_CELLS];

    // -------------------------------------------------------------------------

    public Village(UUID id, BlockPos origin, String name) {
        this.id = id;
        this.origin = origin.immutable();
        this.name = name;
    }

    // ---- identity -----------------------------------------------------------

    public UUID id() {
        return this.id;
    }

    public BlockPos origin() {
        return this.origin;
    }

    public void setOrigin(BlockPos pos) {
        this.origin = pos.immutable();
        resetSweep();
    }

    public String name() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    // ---- membership ---------------------------------------------------------

    public boolean addMember(UUID playerId) {
        return this.members.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        return this.members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return this.members.contains(playerId);
    }

    public int memberCount() {
        return this.members.size();
    }

    public Set<UUID> villagerIds() {
        return this.villagers;
    }

    public int population() {
        return this.villagers.size();
    }

    public boolean registerVillager(UUID entityId) {
        return this.villagers.add(entityId);
    }

    public boolean unregisterVillager(UUID entityId) {
        return this.villagers.remove(entityId);
    }

    // ---- derived reads ------------------------------------------------------

    public VillageTier tier() {
        return this.tier;
    }

    public int buildingScore() {
        return this.buildingScore;
    }

    public int defence() {
        return this.defence;
    }

    /** Krave per real-world hour at the current rate. */
    public int production() {
        return this.production;
    }

    public int attraction() {
        return this.attraction;
    }

    public int happiness() {
        return this.happiness;
    }

    public int stockpile() {
        return this.stockpile;
    }

    public int trackedBuildingBlocks() {
        return this.buildings.size();
    }

    /** Copy of the tracked positions, for the Builder AI and for debugging. */
    public List<BlockPos> buildingPositions() {
        return new ArrayList<>(this.buildings.keySet());
    }

    public VillageView view() {
        return new VillageView(this.id, this.name, this.origin, CLAIM_RADIUS, this.tier,
                population(), this.buildingScore, this.defence, this.happiness,
                this.production, this.stockpile, this.members.size());
    }

    public boolean contains(BlockPos pos) {
        return Math.abs(pos.getX() - this.origin.getX()) <= CLAIM_RADIUS
                && Math.abs(pos.getZ() - this.origin.getZ()) <= CLAIM_RADIUS
                && Math.abs(pos.getY() - this.origin.getY()) <= CLAIM_HEIGHT;
    }

    /** Squared horizontal distance from the origin - used to pick the nearest village. */
    public double horizontalDistanceSqr(BlockPos pos) {
        double dx = pos.getX() - this.origin.getX();
        double dz = pos.getZ() - this.origin.getZ();
        return dx * dx + dz * dz;
    }

    public AABB claimBox() {
        return new AABB(
                this.origin.getX() - CLAIM_RADIUS, this.origin.getY() - CLAIM_HEIGHT,
                this.origin.getZ() - CLAIM_RADIUS,
                this.origin.getX() + CLAIM_RADIUS + 1, this.origin.getY() + CLAIM_HEIGHT,
                this.origin.getZ() + CLAIM_RADIUS + 1);
    }

    // ---- stockpile ----------------------------------------------------------

    public int stockpileCap() {
        return 64 * (this.tier.index() + 1);
    }

    public void addKrave(int amount) {
        this.stockpile = Mth.clamp(this.stockpile + amount, 0, stockpileCap());
    }

    /** Takes up to {@code amount}; returns how much was actually available. */
    public int withdrawKrave(int amount) {
        int taken = Math.min(Math.max(0, amount), this.stockpile);
        this.stockpile -= taken;
        return taken;
    }

    public void adjustHappiness(int delta) {
        this.happiness = Mth.clamp(this.happiness + delta, 0, 100);
    }

    // ---- building tracking --------------------------------------------------

    /**
     * Called from the block-place event. Returns true if the map changed, which
     * the caller uses to decide whether to mark the SavedData dirty.
     */
    public boolean noteBlockPlaced(BlockPos pos, Block block) {
        if (!contains(pos) || KraveVillage.buffOf(block).isNothing()) {
            return false;
        }
        if (this.buildings.size() >= MAX_TRACKED_BUILDINGS && !this.buildings.containsKey(pos)) {
            return false;
        }
        return this.buildings.put(pos.immutable(), block) != block;
    }

    /** Called from the block-break event. */
    public boolean noteBlockRemoved(BlockPos pos) {
        return this.buildings.remove(pos) != null;
    }

    // ---- the tick -----------------------------------------------------------

    /**
     * One village update. Called every {@value #VILLAGE_TICK_INTERVAL} ticks from
     * {@link VillageEvents}. Returns true if anything changed that needs saving -
     * which in practice is almost always, but the flag keeps the "nothing is
     * happening in an abandoned village" case from dirtying the save file.
     */
    public boolean tick(ServerLevel level) {
        boolean changed = false;

        boolean sweepFinished = sweep(level);
        if (sweepFinished) {
            changed = true;
        }

        // The census only works when the residents are actually loaded. When it
        // cannot see them we keep the last known tallies rather than pretending
        // the town has no guards.
        if (level.hasChunkAt(this.origin)) {
            census(level, sweepFinished);
            changed = true;
        }

        VillageTier before = this.tier;
        recompute();
        if (this.tier != before) {
            announceTierChange(level, before);
            changed = true;
        }

        // production
        this.produceProgress += this.production;
        while (this.produceProgress >= VILLAGE_TICKS_PER_HOUR) {
            this.produceProgress -= VILLAGE_TICKS_PER_HOUR;
            if (this.stockpile < stockpileCap()) {
                this.stockpile++;
            }
            changed = true;
        }

        // attraction
        if (population() < this.tier.populationCap() && this.attraction > 0) {
            this.attractProgress += this.attraction;
            if (this.attractProgress >= ATTRACT_THRESHOLD) {
                this.attractProgress -= ATTRACT_THRESHOLD;
                attractNewResident(level);
                changed = true;
            }
        } else {
            // A full village stops banking attraction, so emptying it does not
            // immediately vomit five villagers out at once.
            this.attractProgress = Math.min(this.attractProgress, ATTRACT_THRESHOLD);
        }

        return changed;
    }

    // ---- sweep --------------------------------------------------------------

    private void resetSweep() {
        this.sweepCursor = 0;
        this.sweepPending.clear();
        java.util.Arrays.fill(this.sweptCells, false);
    }

    /** Scans a few cells. Returns true on the tick a full pass completes. */
    private boolean sweep(ServerLevel level) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int n = 0; n < CELLS_PER_TICK; n++) {
            int index = this.sweepCursor;
            int cx = index % CELLS_X;
            int cy = (index / CELLS_X) % CELLS_Y;
            int cz = index / (CELLS_X * CELLS_Y);

            int baseX = this.origin.getX() - CLAIM_RADIUS + cx * CELL;
            int baseY = this.origin.getY() - CLAIM_HEIGHT + cy * CELL;
            int baseZ = this.origin.getZ() - CLAIM_RADIUS + cz * CELL;

            boolean loaded = level.hasChunkAt(baseX, baseZ)
                    && level.hasChunkAt(baseX + CELL - 1, baseZ + CELL - 1);
            this.sweptCells[index] = loaded;
            if (loaded) {
                scanCell(level, cursor, baseX, baseY, baseZ);
            }

            this.sweepCursor++;
            if (this.sweepCursor >= TOTAL_CELLS) {
                this.sweepCursor = 0;
                commitSweep();
                return true;
            }
        }
        return false;
    }

    private void scanCell(ServerLevel level, BlockPos.MutableBlockPos cursor,
                          int baseX, int baseY, int baseZ) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        for (int dy = 0; dy < CELL; dy++) {
            int y = baseY + dy;
            if (y < minY || y >= maxY) {
                continue;
            }
            for (int dx = 0; dx < CELL; dx++) {
                for (int dz = 0; dz < CELL; dz++) {
                    cursor.set(baseX + dx, y, baseZ + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    Block block = state.getBlock();
                    if (!KraveVillage.buffOf(block).isNothing()
                            && this.sweepPending.size() < MAX_TRACKED_BUILDINGS) {
                        this.sweepPending.put(cursor.immutable(), block);
                    }
                }
            }
        }
    }

    /**
     * Folds the finished pass into the live map. Entries the sweep found are
     * trusted outright; entries it did not find are only deleted if the cell they
     * live in was loaded when we looked. That single condition is the whole reason
     * a village on the edge of render distance does not slowly delete itself.
     */
    private void commitSweep() {
        this.buildings.putAll(this.sweepPending);
        Iterator<Map.Entry<BlockPos, Block>> it = this.buildings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Block> entry = it.next();
            if (this.sweepPending.containsKey(entry.getKey())) {
                continue;
            }
            int cell = cellIndexOf(entry.getKey());
            if (cell < 0 || this.sweptCells[cell]) {
                it.remove();
            }
        }
        this.sweepPending.clear();
    }

    private int cellIndexOf(BlockPos pos) {
        int rx = pos.getX() - (this.origin.getX() - CLAIM_RADIUS);
        int ry = pos.getY() - (this.origin.getY() - CLAIM_HEIGHT);
        int rz = pos.getZ() - (this.origin.getZ() - CLAIM_RADIUS);
        if (rx < 0 || ry < 0 || rz < 0) {
            return -1;
        }
        int cx = rx / CELL;
        int cy = ry / CELL;
        int cz = rz / CELL;
        if (cx >= CELLS_X || cy >= CELLS_Y || cz >= CELLS_Z) {
            return -1;
        }
        return cx + cy * CELLS_X + cz * CELLS_X * CELLS_Y;
    }

    /** True when the last completed pass saw every cell of the claim loaded. */
    private boolean lastSweepWasComplete() {
        for (boolean swept : this.sweptCells) {
            if (!swept) {
                return false;
            }
        }
        return true;
    }

    // ---- census -------------------------------------------------------------

    /**
     * Re-reads the residents to refresh the profession tallies, and - only when
     * the whole claim was loaded for the last full sweep - prunes resident IDs
     * whose entity no longer exists anywhere. Doing that pruning unconditionally
     * would erase a village every time the player walked away from it.
     */
    private void census(ServerLevel level, boolean sweepFinished) {
        List<KraveVillagerEntity> found =
                level.getEntitiesOfClass(KraveVillagerEntity.class, claimBox(),
                        v -> v.isAlive() && this.id.equals(v.getVillageId()));

        int guards = 0;
        int prod = 0;
        int attract = 0;
        int builders = 0;
        int guardHeads = 0;
        for (KraveVillagerEntity v : found) {
            int lvl = v.getTradeLevel();
            KraveProfession job = v.getProfession();
            guards += job.defencePerLevel() * lvl;
            if (job == KraveProfession.GUARD) {
                guardHeads++;
            }
            prod += job.productionPerLevel() * lvl;
            attract += job.attractionPerLevel() * lvl;
            if (job == KraveProfession.BUILDER) {
                builders += lvl;
            }
            // A resident we can see but do not know about (spawn egg, /summon,
            // another mod) joins the village rather than being ignored.
            this.villagers.add(v.getUUID());
        }
        this.guardPower = guards;
        this.guardCount = guardHeads;
        this.professionProduction = prod;
        this.professionAttraction = attract;
        this.builderBonus = builders;

        if (sweepFinished && lastSweepWasComplete()) {
            this.villagers.removeIf(uuid -> {
                Entity e = level.getEntity(uuid);
                return e == null || !e.isAlive();
            });
        }

        aggravateMonsters(level, found);
    }

    /**
     * Points idle hostiles at the residents.
     *
     * <p>Vanilla monsters only know how to hunt {@code Villager}, and a Krave
     * Villager is not one - so without this nothing in the game ever attacks a
     * settlement, the Guard profession has nothing to guard against, and the whole
     * defence rating is a number on a screen. This is what makes it mean something.
     *
     * <p>Kept deliberately cheap and deliberately mild: one entity query per
     * village tick (five seconds), only monsters that have no target already, only
     * within {@value #AGGRO_RANGE} blocks of an actual resident - so it reads as
     * "the zombie found them" rather than as the village magnetising every mob in
     * the chunk - and at most {@value #MAX_AGGRO_PER_TICK} at a time.
     */
    private void aggravateMonsters(ServerLevel level, List<KraveVillagerEntity> residents) {
        if (residents.isEmpty()) {
            return;
        }
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, claimBox(),
                m -> m.isAlive() && m.getTarget() == null);
        if (monsters.isEmpty()) {
            return;
        }
        int done = 0;
        for (Monster monster : monsters) {
            if (done >= MAX_AGGRO_PER_TICK) {
                break;
            }
            KraveVillagerEntity best = null;
            double bestDist = AGGRO_RANGE * AGGRO_RANGE;
            for (KraveVillagerEntity resident : residents) {
                double dist = monster.distanceToSqr(resident);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = resident;
                }
            }
            if (best != null) {
                monster.setTarget(best);
                done++;
            }
        }
    }

    // ---- derived stats ------------------------------------------------------

    private void recompute() {
        int buildingSum = 0;
        int defenceSum = 0;
        int productionSum = 0;
        int attractionSum = 0;
        int happinessSum = 0;

        for (Block block : this.buildings.values()) {
            VillageBuff buff = KraveVillage.buffOf(block);
            buildingSum += buff.building();
            defenceSum += buff.defence();
            productionSum += buff.production();
            attractionSum += buff.attraction();
            happinessSum += buff.happiness();
        }

        this.buildingScore = buildingSum + this.builderBonus;
        this.defence = Math.max(0, defenceSum + this.guardPower);

        this.tier = VillageTier.evaluate(this.buildingScore, population(), this.defence);

        // Happiness drifts one point per village tick toward its target, so a
        // change to the town reads as a trend on the HUD rather than a jump.
        int crowding = Math.max(0, population() - this.tier.populationCap()) * 6;
        int target = Mth.clamp(40 + Math.min(30, this.buildingScore)
                + Math.min(15, this.defence / 3) + happinessSum - crowding, 0, 100);
        if (this.happiness < target) {
            this.happiness++;
        } else if (this.happiness > target) {
            this.happiness--;
        }

        float mood = 0.5F + this.happiness / 100.0F;
        this.production = Math.max(0, Math.round(
                (productionSum + this.professionProduction) * this.tier.productionMultiplier() * mood));
        this.attraction = Math.max(0, Math.round(
                (attractionSum + this.professionAttraction + this.buildingScore / 2.0F) * mood));
    }

    private void announceTierChange(ServerLevel level, VillageTier before) {
        boolean up = this.tier.index() > before.index();
        Component msg = Component.translatable(
                up ? "village.barbarajones.tier_up" : "village.barbarajones.tier_down",
                Component.literal(this.name), this.tier.displayName());
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (contains(player.blockPosition()) || this.members.contains(player.getUUID())) {
                player.displayClientMessage(msg, false);
            }
        }
        level.playSound(null, this.origin,
                up ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ANVIL_LAND,
                SoundSource.NEUTRAL, 0.7F, up ? 1.2F : 0.6F);
    }

    // ---- attraction ---------------------------------------------------------

    /**
     * Pulls one new resident in. The profession is chosen by what the village is
     * short of rather than at random, so a town with no guards and constant raids
     * eventually defends itself instead of hiring a fourth grocer.
     */
    private void attractNewResident(ServerLevel level) {
        BlockPos spot = findSpawnSpot(level);
        if (spot == null) {
            // No safe ground loaded right now. Keep the banked points; try again
            // in five seconds rather than dropping a villager into a wall.
            this.attractProgress = ATTRACT_THRESHOLD - 1;
            return;
        }
        KraveProfession job = chooseNeededProfession(level.getRandom());
        KraveVillagerEntity villager = VillageRegistry.KRAVE_VILLAGER.get().create(level);
        if (villager == null) {
            return;
        }
        villager.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.setProfession(job);
        villager.setVillageId(this.id);
        // Counted the moment he arrives rather than whenever the sweep next
        // reaches him. Waiting is what let a burst of arrivals all be told the
        // village had no guards, one after another, when the first of them was
        // already standing there.
        if (job == KraveProfession.GUARD) {
            this.guardCount++;
            this.guardPower += job.defencePerLevel();
        }
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(spot),
                MobSpawnType.EVENT, null, null);
        level.addFreshEntity(villager);
        this.villagers.add(villager.getUUID());

        level.playSound(null, spot, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 0.6F, 1.1F);
        Component msg = Component.translatable("village.barbarajones.arrival",
                job.displayName(), Component.literal(this.name));
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (contains(player.blockPosition())) {
                player.displayClientMessage(msg, true);
            }
        }
    }

    /** Guards may be at most this share of the population. One in three. */
    private static final int GUARDS_PER_RESIDENTS = 3;

    private KraveProfession chooseNeededProfession(RandomSource random) {
        // Both tests, and the head count is the one that stops the flood.
        //
        // The power test alone produced a village of nothing but guards. It
        // reads guardPower, which is only recomputed by the cell sweep - three
        // cells a tick across a hundred and ninety-six of them, and only
        // counting residents in chunks that happen to be loaded. So the number
        // it consults is both stale and an undercount, while the attraction
        // timer keeps handing it new arrivals. Every one of them came back
        // GUARD until a sweep finally caught up, by which point there were
        // eight of them standing around three houses.
        //
        // Deciding from a lagging figure is the whole bug; the proportional cap
        // is what makes the decision safe regardless of how far behind it is.
        int guardCeiling = Math.max(1, population() / GUARDS_PER_RESIDENTS);
        if (this.guardPower < this.buildingScore / 2 && this.guardCount < guardCeiling) {
            return KraveProfession.GUARD;
        }
        if (this.professionProduction < 8) {
            return KraveProfession.CEREALOGIST;
        }
        if (this.builderBonus < 2) {
            return KraveProfession.BUILDER;
        }
        if (this.professionAttraction < 6) {
            return KraveProfession.COURIER;
        }
        return random.nextInt(3) == 0 ? KraveProfession.random(random) : KraveProfession.GROCER;
    }

    @Nullable
    private BlockPos findSpawnSpot(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = random.nextInt(CLAIM_RADIUS) - CLAIM_RADIUS / 2;
            int dz = random.nextInt(CLAIM_RADIUS) - CLAIM_RADIUS / 2;
            BlockPos probe = this.origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            if (Math.abs(ground.getY() - this.origin.getY()) > CLAIM_HEIGHT) {
                continue;
            }
            BlockState below = level.getBlockState(ground.below());
            if (below.isAir() || below.is(Blocks.WATER) || below.is(Blocks.LAVA)) {
                continue;
            }
            if (level.getBlockState(ground).isAir() && level.getBlockState(ground.above()).isAir()) {
                return ground;
            }
        }
        return null;
    }

    // ---- persistence --------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", this.id);
        tag.putLong("Origin", this.origin.asLong());
        tag.putString("Name", this.name);

        ListTag memberList = new ListTag();
        for (UUID member : this.members) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("U", member);
            memberList.add(entry);
        }
        tag.put("Members", memberList);

        ListTag villagerList = new ListTag();
        for (UUID villager : this.villagers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("U", villager);
            villagerList.add(entry);
        }
        tag.put("Villagers", villagerList);

        ListTag buildingList = new ListTag();
        for (Map.Entry<BlockPos, Block> entry : this.buildings.entrySet()) {
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(entry.getValue());
            if (key == null) {
                continue;
            }
            CompoundTag b = new CompoundTag();
            b.putLong("P", entry.getKey().asLong());
            b.putString("B", key.toString());
            buildingList.add(b);
        }
        tag.put("Buildings", buildingList);

        tag.putInt("Tier", this.tier.index());
        tag.putInt("Happiness", this.happiness);
        tag.putInt("Stockpile", this.stockpile);
        tag.putInt("ProduceProgress", this.produceProgress);
        tag.putInt("AttractProgress", this.attractProgress);
        tag.putInt("GuardPower", this.guardPower);
        tag.putInt("GuardCount", this.guardCount);
        tag.putInt("ProfProduction", this.professionProduction);
        tag.putInt("ProfAttraction", this.professionAttraction);
        tag.putInt("BuilderBonus", this.builderBonus);
        return tag;
    }

    public static Village load(CompoundTag tag) {
        UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID();
        BlockPos origin = BlockPos.of(tag.getLong("Origin"));
        String name = tag.contains("Name") ? tag.getString("Name") : "Krave Village";
        Village village = new Village(id, origin, name);

        ListTag memberList = tag.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < memberList.size(); i++) {
            CompoundTag entry = memberList.getCompound(i);
            if (entry.hasUUID("U")) {
                village.members.add(entry.getUUID("U"));
            }
        }

        ListTag villagerList = tag.getList("Villagers", Tag.TAG_COMPOUND);
        for (int i = 0; i < villagerList.size(); i++) {
            CompoundTag entry = villagerList.getCompound(i);
            if (entry.hasUUID("U")) {
                village.villagers.add(entry.getUUID("U"));
            }
        }

        ListTag buildingList = tag.getList("Buildings", Tag.TAG_COMPOUND);
        for (int i = 0; i < buildingList.size(); i++) {
            CompoundTag b = buildingList.getCompound(i);
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(b.getString("B")));
            // A block from a mod that has since been removed simply drops out of
            // the map instead of taking the whole SavedData load down with it.
            if (block != null && block != Blocks.AIR) {
                village.buildings.put(BlockPos.of(b.getLong("P")), block);
            }
        }

        village.tier = VillageTier.byIndex(tag.getInt("Tier"));
        village.happiness = tag.contains("Happiness") ? tag.getInt("Happiness") : 50;
        village.stockpile = tag.getInt("Stockpile");
        village.produceProgress = tag.getInt("ProduceProgress");
        village.attractProgress = tag.getInt("AttractProgress");
        village.guardPower = tag.getInt("GuardPower");
        village.guardCount = tag.getInt("GuardCount");
        village.professionProduction = tag.getInt("ProfProduction");
        village.professionAttraction = tag.getInt("ProfAttraction");
        village.builderBonus = tag.getInt("BuilderBonus");
        return village;
    }
}
