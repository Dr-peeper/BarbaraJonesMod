package com.barbarajones.v2.quests;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.KraveLevel;
import com.barbarajones.progression.Perks;
import com.barbarajones.v2.quests.net.QuestNetwork;
import com.barbarajones.v2.quests.net.S2CQuestDefs;
import com.barbarajones.v2.quests.net.S2CQuestState;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The engine: it takes observations from the world, turns them into progress, and
 * settles the graph.
 *
 * <p>Three properties it was built to have, each of them a direct answer to how the
 * old system failed:
 *
 * <ol>
 *   <li><b>Progress never goes backwards.</b> Every write here goes through
 *       {@link PlayerQuests#addProgress} (event-driven, exactly once per occurrence)
 *       or {@link PlayerQuests#raiseProgress} (sampled, {@code max}-based). Neither
 *       can lower a number. The old engine re-derived completion from live inventory
 *       every two seconds, so completing a quest and then USING the items it asked
 *       for un-completed it - and since the next quest in the chain had already
 *       consumed them, it could never be completed again.</li>
 *   <li><b>Completion is settled by a bounded walk, not a fixpoint loop.</b> When
 *       something completes, the engine enqueues exactly that quest's dependants
 *       (via the reverse edges {@link QuestFile} keeps) and drains the queue. A quest
 *       can complete once, so the queue is bounded by the edge count. The old
 *       {@code settle()} looped over the entire quest list re-testing everything and
 *       re-announcing on every pass until nothing changed - quadratic, and easy to
 *       leave in a state where a re-entrant call announced the same quest twice or
 *       skipped one entirely.</li>
 *   <li><b>The client is told, never asked.</b> Every mutation ends in a sync packet.
 *       Nothing on the client computes completion.</li>
 * </ol>
 */
public final class QuestEngine {

    /** Never announce more than this many freshly-opened quests in one go. */
    private static final int ANNOUNCE_CAP = 4;
    /** How far to look for the player's Cayden when sampling the feed counter. */
    private static final double CAYDEN_RANGE = 96.0D;

    private QuestEngine() {
    }

    public static QuestFile file() {
        return QuestLoader.file();
    }

    // =====================================================================
    // task index - rebuilt whenever the datapack graph changes identity
    // =====================================================================

    private static QuestFile indexedFile;
    private static Map<QuestTask.Kind, List<Entry>> byKind = Map.of();
    private static Map<ResourceLocation, List<Entry>> obtainByItem = Map.of();

    /** A task, plus where it lives, so progress can be written without a search. */
    public static final class Entry {
        public final Quest quest;
        public final QuestTask task;
        public final String key;

        Entry(Quest quest, QuestTask task, String key) {
            this.quest = quest;
            this.task = task;
            this.key = key;
        }
    }

    private static synchronized void ensureIndex() {
        QuestFile current = file();
        if (current == indexedFile) {
            return;
        }
        Map<QuestTask.Kind, List<Entry>> kinds = new LinkedHashMap<>();
        Map<ResourceLocation, List<Entry>> obtain = new LinkedHashMap<>();
        for (Quest quest : current.allQuests()) {
            for (int i = 0; i < quest.tasks.size(); i++) {
                QuestTask task = quest.tasks.get(i);
                Entry entry = new Entry(quest, task, quest.taskKey(i));
                kinds.computeIfAbsent(task.kind, k -> new ArrayList<>()).add(entry);
                if (task instanceof QuestTask.Obtain o) {
                    obtain.computeIfAbsent(o.item, k -> new ArrayList<>()).add(entry);
                }
            }
        }
        byKind = kinds;
        obtainByItem = obtain;
        indexedFile = current;
    }

    private static List<Entry> tasksOf(QuestTask.Kind kind) {
        ensureIndex();
        return byKind.getOrDefault(kind, List.of());
    }

    /**
     * Is any loaded quest watching for this block being placed? Checked on every
     * single block placement in the world, so it walks only the (short) list of
     * place_building tasks rather than the whole graph.
     */
    public static boolean isTrackedBuilding(ResourceLocation block) {
        for (Entry entry : tasksOf(QuestTask.Kind.PLACE_BUILDING)) {
            if (((QuestTask.PlaceBuilding) entry.task).building.equals(block)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // a batch of changes
    // =====================================================================

    /**
     * One unit of work. Progress writes accumulate here, then {@link #commit} settles
     * the graph once and sends a single delta packet - instead of settling and
     * syncing per individual counter, which is how a busy tick turns into a packet
     * storm.
     */
    public static final class Tx {
        final ServerPlayer player;
        final PlayerQuests data;
        final Map<String, Integer> changed = new LinkedHashMap<>();
        final Set<ResourceLocation> seeds = new LinkedHashSet<>();
        boolean setsChanged;

        Tx(ServerPlayer player) {
            this.player = player;
            this.data = PlayerQuests.of(player);
        }

        void bump(Entry entry, int amount) {
            if (this.data.isComplete(entry.quest.id)) {
                return;   // latched: never re-count into a finished quest
            }
            if (this.data.addProgress(entry.key, amount, entry.task.target)) {
                this.changed.put(entry.key, this.data.progress(entry.key));
                this.seeds.add(entry.quest.id);
            }
        }

        void raise(Entry entry, int observed) {
            if (this.data.isComplete(entry.quest.id)) {
                return;
            }
            if (this.data.raiseProgress(entry.key, observed, entry.task.target)) {
                this.changed.put(entry.key, this.data.progress(entry.key));
                this.seeds.add(entry.quest.id);
            }
        }
    }

    public static Tx begin(ServerPlayer player) {
        return new Tx(player);
    }

    /** Settle the graph from whatever changed, then tell the client. */
    public static void commit(Tx tx) {
        boolean anything = settle(tx);
        if (!tx.changed.isEmpty() || tx.setsChanged || anything) {
            sendDelta(tx);
        }
    }

    // =====================================================================
    // observations
    // =====================================================================

    public static void onKill(ServerPlayer player, ResourceLocation entityId) {
        Tx tx = begin(player);
        for (Entry e : tasksOf(QuestTask.Kind.KILL)) {
            if (((QuestTask.Kill) e.task).entity.equals(entityId)) {
                tx.bump(e, 1);
            }
        }
        for (Entry e : tasksOf(QuestTask.Kind.DEFEAT_BOSS)) {
            if (((QuestTask.DefeatBoss) e.task).entity.equals(entityId)) {
                tx.bump(e, 1);
            }
        }
        commit(tx);
    }

    public static void onCraft(ServerPlayer player, ResourceLocation itemId, int count) {
        if (count <= 0) {
            return;
        }
        Tx tx = begin(player);
        for (Entry e : tasksOf(QuestTask.Kind.CRAFT)) {
            if (((QuestTask.Craft) e.task).item.equals(itemId)) {
                tx.bump(e, count);
            }
        }
        commit(tx);
        // Crafting also changes what is in the bag, so re-sample the obtain tasks
        // immediately rather than waiting up to a second for the next sweep.
        sampleInventory(player);
    }

    public static void onPlaceBlock(ServerPlayer player, ResourceLocation blockId) {
        Tx tx = begin(player);
        int placed = tx.data.bumpPlaced(blockId);
        tx.setsChanged = true;
        for (Entry e : tasksOf(QuestTask.Kind.PLACE_BUILDING)) {
            if (((QuestTask.PlaceBuilding) e.task).building.equals(blockId)) {
                tx.raise(e, placed);
            }
        }
        VillageState.recompute(player, tx);
        raiseTierTasks(tx);
        commit(tx);
    }

    /** Feed the current village tier into every village_tier task. Idempotent. */
    private static void raiseTierTasks(Tx tx) {
        int tier = tx.data.villageTier();
        for (Entry entry : tasksOf(QuestTask.Kind.VILLAGE_TIER)) {
            tx.raise(entry, tier);
        }
    }

    public static void onEnterDimension(ServerPlayer player, ResourceLocation dimension) {
        Tx tx = begin(player);
        for (Entry e : tasksOf(QuestTask.Kind.VISIT_DIMENSION)) {
            if (((QuestTask.VisitDimension) e.task).dimension.equals(dimension)) {
                tx.bump(e, 1);
            }
        }
        commit(tx);
    }

    /**
     * Everything that is cheap to look at and safe to look at repeatedly. Called on
     * a slow timer. Every write in here is a {@code max}, so running it once a second
     * or a hundred times a second produces exactly the same numbers.
     */
    public static void sample(ServerPlayer player) {
        Tx tx = begin(player);
        sampleInventoryInto(tx);
        sampleCayden(tx);
        sampleKraveLevel(tx);
        sampleAbilities(tx);
        VillageState.recompute(player, tx);
        raiseTierTasks(tx);
        commit(tx);
    }

    /** Just the bag, for the moments where waiting a whole second would feel broken. */
    public static void sampleInventory(ServerPlayer player) {
        Tx tx = begin(player);
        sampleInventoryInto(tx);
        commit(tx);
    }

    private static void sampleInventoryInto(Tx tx) {
        ensureIndex();
        if (obtainByItem.isEmpty()) {
            return;
        }
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        var inventory = tx.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id != null && obtainByItem.containsKey(id)) {
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        for (Map.Entry<ResourceLocation, List<Entry>> e : obtainByItem.entrySet()) {
            int held = counts.getOrDefault(e.getKey(), 0);
            if (held <= 0) {
                continue;
            }
            for (Entry entry : e.getValue()) {
                tx.raise(entry, held);
            }
        }
    }

    private static void sampleCayden(Tx tx) {
        List<Entry> tasks = tasksOf(QuestTask.Kind.FEED_CAYDEN);
        if (tasks.isEmpty()) {
            return;
        }
        int best = 0;
        AABB box = tx.player.getBoundingBox().inflate(CAYDEN_RANGE);
        for (CaydenCobb cayden : tx.player.level().getEntitiesOfClass(CaydenCobb.class, box)) {
            if (tx.player.getUUID().equals(cayden.getOwnerUUID())) {
                best = Math.max(best, cayden.getKraveFed());
            }
        }
        if (best <= 0) {
            return;
        }
        for (Entry entry : tasks) {
            tx.raise(entry, best);
        }
    }

    private static void sampleKraveLevel(Tx tx) {
        List<Entry> tasks = tasksOf(QuestTask.Kind.KRAVE_LEVEL);
        if (tasks.isEmpty()) {
            return;
        }
        int level = KraveLevel.getLevel(tx.player);
        for (Entry entry : tasks) {
            tx.raise(entry, level);
        }
    }

    /**
     * An ability counts as held if the level-gated perk table grants it OR a quest
     * has already awarded it. Quest-granted abilities are additive to the perk
     * ladder, never a replacement for it.
     */
    private static void sampleAbilities(Tx tx) {
        for (Entry entry : tasksOf(QuestTask.Kind.UNLOCK_ABILITY)) {
            String ability = ((QuestTask.UnlockAbility) entry.task).ability;
            boolean held = tx.data.hasAbility(ability);
            if (!held) {
                try {
                    held = Perks.has(tx.player, Perks.Perk.valueOf(ability));
                } catch (IllegalArgumentException ignored) {
                    // Not a perk name; it is a pure quest-granted ability, already checked.
                }
            }
            if (held) {
                tx.raise(entry, 1);
            }
        }
    }

    // =====================================================================
    // settling
    // =====================================================================

    private static boolean isUnlocked(PlayerQuests data, Quest quest) {
        if (quest.dependencies.isEmpty()) {
            return true;
        }
        int done = 0;
        for (ResourceLocation dep : quest.dependencies) {
            if (data.isComplete(dep)) {
                done++;
            }
        }
        return done >= quest.minDependencies;
    }

    /** Public because the tree screen's server-side twin and the API both want it. */
    public static boolean isUnlocked(ServerPlayer player, Quest quest) {
        return isUnlocked(PlayerQuests.of(player), quest);
    }

    private static boolean tasksMet(PlayerQuests data, Quest quest) {
        for (int i = 0; i < quest.tasks.size(); i++) {
            if (data.progress(quest.taskKey(i)) < quest.tasks.get(i).target) {
                return false;
            }
        }
        return true;
    }

    /**
     * Drain the work queue. Seeded with the quests whose counters moved, and grown
     * only by the reverse edges of quests that actually completed - so the walk is
     * bounded by the edge count and can never spin.
     */
    private static boolean settle(Tx tx) {
        QuestFile graph = file();
        if (graph.isEmpty()) {
            return false;
        }
        Deque<ResourceLocation> queue = new ArrayDeque<>(tx.seeds);
        // Nothing moved, but a dependency may have completed elsewhere, so on an
        // empty seed set start from the roots and let the walk find the frontier.
        if (queue.isEmpty()) {
            queue.addAll(graph.roots());
        }
        List<Quest> completedNow = new ArrayList<>();
        List<Quest> openedNow = new ArrayList<>();
        int guard = 0;
        int limit = Math.max(256, graph.size() * 32);

        while (!queue.isEmpty() && guard++ < limit) {
            ResourceLocation id = queue.poll();
            Quest quest = graph.quest(id);
            if (quest == null || tx.data.isComplete(id)) {
                // Already done: its dependants were enqueued when it completed, but on
                // a fresh login nothing has been enqueued yet, so push them now.
                if (quest != null) {
                    queue.addAll(graph.dependantsOf(id));
                }
                continue;
            }
            if (!isUnlocked(tx.data, quest)) {
                continue;
            }
            if (tx.data.markNotified(quest.id)) {
                openedNow.add(quest);
                tx.setsChanged = true;
            }
            if (!tasksMet(tx.data, quest)) {
                continue;
            }
            tx.data.markComplete(quest.id);
            tx.setsChanged = true;
            completedNow.add(quest);
            if (quest.autoClaim) {
                grantRewards(tx.player, tx.data, quest);
            }
            queue.addAll(graph.dependantsOf(quest.id));
        }

        if (guard >= limit) {
            QuestModule.LOG.error("Quest settle hit its iteration guard for {} - the graph is probably "
                    + "cyclic, which the validator should have rejected at load.", tx.player.getGameProfile().getName());
        }

        announce(tx.player, completedNow, openedNow);
        return !completedNow.isEmpty() || !openedNow.isEmpty();
    }

    private static void announce(ServerPlayer player, List<Quest> completed, List<Quest> opened) {
        for (Quest quest : completed) {
            player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.complete",
                    quest.title().copy().withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GREEN));
            if (!quest.autoClaim && !quest.rewards.isEmpty()) {
                player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.claimable")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        if (!completed.isEmpty()) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
        int shown = 0;
        for (Quest quest : opened) {
            if (shown++ >= ANNOUNCE_CAP) {
                break;
            }
            player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.opened",
                    quest.title().copy().withStyle(ChatFormatting.YELLOW),
                    quest.objective().copy().withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (opened.size() > ANNOUNCE_CAP) {
            player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.opened_more",
                    opened.size() - ANNOUNCE_CAP).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // =====================================================================
    // rewards
    // =====================================================================

    private static void grantRewards(ServerPlayer player, PlayerQuests data, Quest quest) {
        if (!data.markClaimed(quest.id)) {
            return;   // already paid out; claiming twice is the other classic quest bug
        }
        for (QuestReward reward : quest.rewards) {
            reward.grant(player);
        }
        if (!quest.rewards.isEmpty()) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }

    /** Player pressed Claim. Everything is re-checked here; the client is not trusted. */
    public static void claim(ServerPlayer player, ResourceLocation questId) {
        Quest quest = file().quest(questId);
        if (quest == null) {
            return;
        }
        PlayerQuests data = PlayerQuests.of(player);
        if (!data.isComplete(questId) || data.isClaimed(questId)) {
            return;
        }
        grantRewards(player, data, quest);
        Tx tx = begin(player);
        tx.setsChanged = true;
        commit(tx);
    }

    /**
     * Player pressed Submit on a delivery task. Takes the items, then advances.
     *
     * <p>Delivery is the only place the quest system removes anything from a player,
     * and it only ever happens because they asked for it. The old system's implicit
     * "you must be holding all of these" checks were a permanent, invisible tax on
     * the inventory; this is an explicit trade the player initiates.
     */
    public static void deliver(ServerPlayer player, ResourceLocation questId) {
        Quest quest = file().quest(questId);
        if (quest == null) {
            return;
        }
        PlayerQuests data = PlayerQuests.of(player);
        if (data.isComplete(questId) || !isUnlocked(data, quest)) {
            return;
        }
        Tx tx = begin(player);
        for (int i = 0; i < quest.tasks.size(); i++) {
            if (!(quest.tasks.get(i) instanceof QuestTask.Deliver task)) {
                continue;
            }
            String key = quest.taskKey(i);
            int outstanding = task.target - data.progress(key);
            if (outstanding <= 0) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(task.item);
            if (item == null) {
                continue;
            }
            int taken = takeFromInventory(player, item, outstanding);
            if (taken > 0) {
                Entry entry = new Entry(quest, task, key);
                tx.bump(entry, taken);
            }
        }
        commit(tx);
    }

    private static int takeFromInventory(ServerPlayer player, Item item, int wanted) {
        int taken = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && taken < wanted; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int take = Math.min(stack.getCount(), wanted - taken);
            stack.shrink(take);
            if (stack.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            taken += take;
        }
        if (taken > 0) {
            player.containerMenu.broadcastChanges();
        }
        return taken;
    }

    // =====================================================================
    // sync
    // =====================================================================

    /** Definitions plus the player's whole state. Login, respawn, and after /reload. */
    public static void sendFull(ServerPlayer player) {
        QuestNetwork.toPlayer(player, new S2CQuestDefs(file()));
        PlayerQuests data = PlayerQuests.of(player);
        QuestNetwork.toPlayer(player, new S2CQuestState(true, data.completed(), data.claimed(),
                data.abilities(), data.schematics(), data.villageTier(), data.allProgress()));
    }

    private static void sendDelta(Tx tx) {
        PlayerQuests data = tx.data;
        QuestNetwork.toPlayer(tx.player, new S2CQuestState(false, data.completed(), data.claimed(),
                data.abilities(), data.schematics(), data.villageTier(), tx.changed));
    }

    /** Re-walk the graph without any new observation. Used on login and after a reload. */
    public static void refresh(ServerPlayer player) {
        Tx tx = begin(player);
        settle(tx);
        sendFull(player);
    }
}
