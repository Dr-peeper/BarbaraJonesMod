package com.barbarajones.v2.quests.client;

import com.barbarajones.v2.quests.Quest;
import com.barbarajones.v2.quests.QuestFile;
import com.barbarajones.v2.quests.QuestTask;
import com.barbarajones.v2.quests.net.S2CQuestState;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The client's mirror of the quest world. Read-only, and never computes anything the
 * server has not already decided.
 *
 * <p>This is the class the old book did not have, and its absence was the single
 * most visible bug in the mod. The old screen read completion out of the Quest
 * Book's replicated ItemStack NBT and re-derived unlock state locally. A replicated
 * stack lags, and locally-derived unlock state is a second implementation of the
 * server's rules that will eventually disagree with it - so the book showed quests
 * as locked that the server considered done, and vice versa, and the player had no
 * way to tell which was lying.
 *
 * <p>Here, {@link #completed} is literally a set of ids the server sent. If it is in
 * the set the quest is done. There is no second opinion to be wrong.
 */
public final class ClientQuests {

    private ClientQuests() {
    }

    private static QuestFile defs = QuestFile.EMPTY;
    private static Set<String> completed = Set.of();
    private static Set<String> claimed = Set.of();
    private static Set<String> abilities = Set.of();
    private static Set<String> schematics = Set.of();
    private static int villageTier;
    private static final Map<String, Integer> PROGRESS = new HashMap<>();

    /** What the book paints a node as. Derived from server-sent state only. */
    public enum Status {
        /** Dependencies not met. Still shows its objective and what would open it. */
        LOCKED,
        /** Open and being worked on. */
        OPEN,
        /** Done, with rewards still waiting. */
        CLAIMABLE,
        /** Done and paid out. */
        COMPLETE
    }

    // ---- inbound ------------------------------------------------------------

    public static void acceptDefs(QuestFile file) {
        defs = file;
    }

    public static void acceptState(S2CQuestState msg) {
        completed = new HashSet<>(msg.completed);
        claimed = new HashSet<>(msg.claimed);
        abilities = new HashSet<>(msg.abilities);
        schematics = new HashSet<>(msg.schematics);
        villageTier = msg.villageTier;
        if (msg.full) {
            PROGRESS.clear();
        }
        PROGRESS.putAll(msg.progress);
    }

    /** Wipe on disconnect so a second world never inherits the first one's book. */
    public static void clear() {
        defs = QuestFile.EMPTY;
        completed = Set.of();
        claimed = Set.of();
        abilities = Set.of();
        schematics = Set.of();
        villageTier = 0;
        PROGRESS.clear();
    }

    // ---- queries ------------------------------------------------------------

    public static QuestFile file() {
        return defs;
    }

    public static boolean isComplete(ResourceLocation quest) {
        return completed.contains(quest.toString());
    }

    public static boolean isClaimed(ResourceLocation quest) {
        return claimed.contains(quest.toString());
    }

    public static boolean isUnlocked(Quest quest) {
        if (quest.dependencies.isEmpty()) {
            return true;
        }
        int done = 0;
        for (ResourceLocation dep : quest.dependencies) {
            if (isComplete(dep)) {
                done++;
            }
        }
        return done >= quest.minDependencies;
    }

    public static Status status(Quest quest) {
        if (isComplete(quest.id)) {
            return quest.rewards.isEmpty() || isClaimed(quest.id) ? Status.COMPLETE : Status.CLAIMABLE;
        }
        return isUnlocked(quest) ? Status.OPEN : Status.LOCKED;
    }

    public static int progress(String taskKey) {
        return PROGRESS.getOrDefault(taskKey, 0);
    }

    public static int progress(Quest quest, int taskIndex) {
        return progress(quest.taskKey(taskIndex));
    }

    /** 0..1 across all of a quest's tasks. Milestones read as 1. */
    public static float completionFraction(Quest quest) {
        if (quest.tasks.isEmpty()) {
            return isComplete(quest.id) ? 1.0F : 0.0F;
        }
        float total = 0.0F;
        for (int i = 0; i < quest.tasks.size(); i++) {
            QuestTask task = quest.tasks.get(i);
            total += Math.min(1.0F, progress(quest, i) / (float) task.target);
        }
        return total / quest.tasks.size();
    }

    public static boolean hasDeliveryOutstanding(Quest quest) {
        for (int i = 0; i < quest.tasks.size(); i++) {
            if (quest.tasks.get(i) instanceof QuestTask.Deliver && progress(quest, i) < quest.tasks.get(i).target) {
                return true;
            }
        }
        return false;
    }

    public static int completedCount() {
        return completed.size();
    }

    public static int villageTier() {
        return villageTier;
    }

    public static Set<String> abilities() {
        return abilities;
    }

    public static Set<String> schematics() {
        return schematics;
    }
}
