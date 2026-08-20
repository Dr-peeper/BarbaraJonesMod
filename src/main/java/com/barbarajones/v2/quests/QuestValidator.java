package com.barbarajones.v2.quests;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The rule the old questline broke over and over: <b>a quest must be completable.</b>
 *
 * <p>This runs once, at datapack load, before the graph is published to anyone. If
 * it finds a problem it logs every single one in a boxed banner and then throws, so
 * a bad questline fails the world load instead of shipping and quietly stranding a
 * player forty hours in. That is deliberate. The alternative - log a warning and
 * carry on - is exactly how the previous system ended up with quests nobody could
 * ever finish and nobody noticed until a player complained.
 *
 * <p>Nine checks, each of which corresponds to a real way the old questline broke:
 * <ol>
 *   <li><b>Missing dependency</b> - a quest names a prerequisite that does not exist.
 *       Silently un-openable forever.</li>
 *   <li><b>Missing chapter</b> - a quest lands in a chapter with no tab, so it never
 *       renders and cannot be found.</li>
 *   <li><b>Self dependency</b> - a quest that gates on itself.</li>
 *   <li><b>Cycle</b> - A needs B needs A. The old engine's fixpoint loop simply
 *       spun on these; the new engine would deadlock, so they are rejected outright.</li>
 *   <li><b>Unreachable</b> - a quest not reachable from any root by forward edges.
 *       Every quest must descend from something with no prerequisites.</li>
 *   <li><b>min_dependencies out of range</b> - "any 4 of" written on a node with 2
 *       dependencies can never unlock.</li>
 *   <li><b>Unknown registry ids</b> - a task naming an item, entity or dimension that
 *       is not registered. Impossible to satisfy and impossible to spot by eye.</li>
 *   <li><b>Consumed prerequisite</b> - the headline bug. Quest B asks the player to
 *       hand over N of an item that one of B's own prerequisites already took off
 *       them, and the chain never supplied that much. Arithmetic, not vibes:
 *       supplied-along-the-chain minus consumed-along-the-chain must cover what B
 *       asks for.</li>
 *   <li><b>Milestone with rewards nobody can claim</b> - a quest with no tasks is a
 *       pure gate; it must be auto_claim or the player is handed a Claim button on a
 *       node the book has no reason to make them open. (Warning, not fatal.)</li>
 * </ol>
 */
public final class QuestValidator {

    private QuestValidator() {
    }

    /** Thrown when the graph is not playable. Fails the datapack load on purpose. */
    public static class QuestGraphException extends RuntimeException {
        public QuestGraphException(String message) {
            super(message);
        }
    }

    /**
     * @return the warnings (non-fatal notes). Throws {@link QuestGraphException} on
     *         anything actually fatal, after logging the full list.
     */
    public static List<String> validate(QuestFile file) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        checkStructure(file, errors);
        // A broken structure makes every later check produce noise instead of signal,
        // so bail out with the structural errors alone rather than a wall of cascade.
        if (errors.isEmpty()) {
            checkCycles(file, errors);
        }
        if (errors.isEmpty()) {
            checkReachability(file, errors);
            checkRegistryIds(file, errors);
            checkConsumption(file, errors);
            checkMilestones(file, warnings);
        }

        if (!errors.isEmpty()) {
            StringBuilder banner = new StringBuilder();
            banner.append('\n');
            banner.append("+----------------------------------------------------------------+\n");
            banner.append("|  BARBARA JONES QUESTLINE IS NOT PLAYABLE                       |\n");
            banner.append("|  ").append(pad(errors.size() + " problem(s) found in data/barbarajones/quests/", 62))
                    .append("|\n");
            banner.append("+----------------------------------------------------------------+\n");
            for (String e : errors) {
                banner.append("  * ").append(e).append('\n');
                QuestModule.LOG.error("Quest validation: {}", e);
            }
            banner.append("+----------------------------------------------------------------+");
            QuestModule.LOG.error(banner.toString());
            throw new QuestGraphException("Barbara Jones questline failed validation with "
                    + errors.size() + " problem(s); see the log above. Nothing was loaded.");
        }

        for (String w : warnings) {
            QuestModule.LOG.warn("Quest validation: {}", w);
        }
        QuestModule.LOG.info("Questline validated: {} quests across {} chapters, {} root(s), no problems.",
                file.size(), file.orderedChapters().size(), file.roots().size());
        return warnings;
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ---- 1, 2, 3, 6 ---------------------------------------------------------

    private static void checkStructure(QuestFile file, List<String> errors) {
        if (file.isEmpty()) {
            errors.add("no quests loaded at all - data/barbarajones/quests/ is empty or every file failed to parse");
            return;
        }
        if (file.roots().isEmpty()) {
            errors.add("no root quest: every quest has at least one dependency, so nothing can ever start");
        }
        for (Quest q : file.allQuests()) {
            if (file.chapter(q.chapter) == null) {
                errors.add(q.id + " is in chapter " + q.chapter + ", which has no chapter file "
                        + "(expected data/barbarajones/quests/chapters/" + q.chapter.getPath() + ".json)");
            }
            Set<ResourceLocation> seenDeps = new HashSet<>();
            for (ResourceLocation dep : q.dependencies) {
                if (dep.equals(q.id)) {
                    errors.add(q.id + " lists itself as a dependency, so it can never unlock");
                } else if (file.quest(dep) == null) {
                    errors.add(q.id + " depends on " + dep + ", which is not a quest in this pack");
                } else if (!seenDeps.add(dep)) {
                    errors.add(q.id + " lists " + dep + " as a dependency twice");
                }
            }
            if (q.minDependencies > q.dependencies.size()) {
                errors.add(q.id + " needs " + q.minDependencies + " of its dependencies complete but only "
                        + "has " + q.dependencies.size() + " - it can never unlock");
            }
            if (q.minDependencies < 0) {
                errors.add(q.id + " has a negative min_dependencies (" + q.minDependencies + ")");
            }
            if (q.titleKey.isEmpty() || q.objectiveKey.isEmpty()) {
                errors.add(q.id + " has an empty title or objective key; a quest with no objective text "
                        + "is exactly the grey box this rewrite exists to delete");
            }
        }
    }

    // ---- 4: cycles ----------------------------------------------------------

    /** Iterative three-colour DFS. Recursion would blow the stack on a deep pack. */
    private static void checkCycles(QuestFile file, List<String> errors) {
        Map<ResourceLocation, Integer> colour = new HashMap<>();   // 0 unseen, 1 open, 2 closed
        Map<ResourceLocation, ResourceLocation> parent = new HashMap<>();

        for (ResourceLocation start : file.allQuestIds()) {
            if (colour.getOrDefault(start, 0) != 0) {
                continue;
            }
            Deque<ResourceLocation> stack = new ArrayDeque<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                ResourceLocation cur = stack.peek();
                int c = colour.getOrDefault(cur, 0);
                if (c == 0) {
                    colour.put(cur, 1);
                    Quest q = file.quest(cur);
                    if (q != null) {
                        for (ResourceLocation dep : q.dependencies) {
                            int dc = colour.getOrDefault(dep, 0);
                            if (dc == 1) {
                                errors.add("dependency cycle: " + describeCycle(parent, cur, dep));
                                colour.put(dep, 2);   // break it so we report once, not forever
                            } else if (dc == 0) {
                                parent.put(dep, cur);
                                stack.push(dep);
                            }
                        }
                    }
                } else {
                    stack.pop();
                    if (c == 1) {
                        colour.put(cur, 2);
                    }
                }
            }
        }
    }

    private static String describeCycle(Map<ResourceLocation, ResourceLocation> parent,
                                        ResourceLocation from, ResourceLocation backEdgeTo) {
        List<String> path = new ArrayList<>();
        path.add(backEdgeTo.toString());
        ResourceLocation cur = from;
        int guard = 0;
        while (cur != null && guard++ < 512) {
            path.add(cur.toString());
            if (cur.equals(backEdgeTo)) {
                break;
            }
            cur = parent.get(cur);
        }
        java.util.Collections.reverse(path);
        return String.join(" -> ", path);
    }

    // ---- 5: reachability ----------------------------------------------------

    private static void checkReachability(QuestFile file, List<String> errors) {
        Set<ResourceLocation> reached = new HashSet<>(file.roots());
        Deque<ResourceLocation> frontier = new ArrayDeque<>(file.roots());
        while (!frontier.isEmpty()) {
            ResourceLocation cur = frontier.poll();
            for (ResourceLocation child : file.dependantsOf(cur)) {
                Quest q = file.quest(child);
                if (q == null || reached.contains(child)) {
                    continue;
                }
                // A node opens once minDependencies of its parents are done, so it is
                // reachable as soon as that many of them are.
                int reachedParents = 0;
                for (ResourceLocation dep : q.dependencies) {
                    if (reached.contains(dep)) {
                        reachedParents++;
                    }
                }
                if (reachedParents >= q.minDependencies) {
                    reached.add(child);
                    frontier.add(child);
                }
            }
        }
        for (Quest q : file.allQuests()) {
            if (!reached.contains(q.id)) {
                errors.add(q.id + " is unreachable: no path of completable quests leads to it "
                        + "(dependencies: " + q.dependencies + ")");
            }
        }
    }

    // ---- 7: registry ids ----------------------------------------------------

    private static void checkRegistryIds(QuestFile file, List<String> errors) {
        for (Quest q : file.allQuests()) {
            for (int i = 0; i < q.tasks.size(); i++) {
                QuestTask task = q.tasks.get(i);
                String where = q.id + " task " + i + " (" + task.kind.key + ")";
                if (task instanceof QuestTask.Kill k) {
                    requireEntity(errors, where, k.entity);
                } else if (task instanceof QuestTask.DefeatBoss b) {
                    requireEntity(errors, where, b.entity);
                } else if (task instanceof QuestTask.Obtain o) {
                    requireItem(errors, where, o.item);
                } else if (task instanceof QuestTask.Craft c) {
                    requireItem(errors, where, c.item);
                } else if (task instanceof QuestTask.Deliver d) {
                    requireItem(errors, where, d.item);
                } else if (task instanceof QuestTask.PlaceBuilding p) {
                    // A "building" is a block id; it must be a block AND have an item form,
                    // otherwise the player has no way to place it.
                    if (!ForgeRegistries.BLOCKS.containsKey(p.building)) {
                        errors.add(where + " names building " + p.building + ", which is not a registered block");
                    } else if (!ForgeRegistries.ITEMS.containsKey(p.building)) {
                        errors.add(where + " names building " + p.building + ", which has no item form, "
                                + "so a player has nothing to place");
                    }
                }
            }
            for (QuestReward reward : q.rewards) {
                if (reward instanceof QuestReward.ItemReward ir) {
                    requireItem(errors, q.id + " reward", ir.item);
                }
            }
            if (q.icon.isEmpty()) {
                errors.add(q.id + " has an empty icon stack");
            }
        }
    }

    private static void requireItem(List<String> errors, String where, ResourceLocation id) {
        if (!ForgeRegistries.ITEMS.containsKey(id)) {
            errors.add(where + " names item " + id + ", which is not registered");
        }
    }

    private static void requireEntity(List<String> errors, String where, ResourceLocation id) {
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            errors.add(where + " names entity " + id + ", which is not registered");
        }
    }

    // ---- 8: the consumption check ------------------------------------------

    /**
     * The bug this whole rewrite is a response to, expressed as arithmetic.
     *
     * <p>For each quest that asks the player to hand items over ({@code deliver}),
     * walk its full ancestor set. Add up everything the chain told the player to
     * obtain or craft; subtract everything the chain already took back. If the
     * remainder does not cover what this quest wants, the questline has asked for
     * something it previously ate, and the player is stuck.
     *
     * <p>Note the shape of the fix: {@code obtain} and {@code craft} tasks are
     * high-water/cumulative and so are NOT invalidated by later consumption - that
     * is the engine-level half of the fix. This check is the data-level half: it
     * proves the player was ever told to make enough in the first place.
     */
    private static void checkConsumption(QuestFile file, List<String> errors) {
        for (Quest q : file.allQuests()) {
            Map<ResourceLocation, Integer> wanted = new HashMap<>();
            for (QuestTask task : q.tasks) {
                for (QuestTask.ItemHold hold : task.consumes()) {
                    wanted.merge(hold.item, hold.amount, Integer::sum);
                }
            }
            if (wanted.isEmpty()) {
                continue;
            }

            Set<ResourceLocation> ancestors = file.ancestorsOf(q.id);
            Map<ResourceLocation, Integer> supplied = new HashMap<>();
            Map<ResourceLocation, Integer> consumed = new HashMap<>();

            // The quest's own obtain/craft tasks count as supply for its own deliver tasks:
            // "craft 2 Krave Boxes, then hand one over" is a perfectly good quest.
            List<Quest> chain = new ArrayList<>();
            chain.add(q);
            for (ResourceLocation a : ancestors) {
                Quest aq = file.quest(a);
                if (aq != null) {
                    chain.add(aq);
                }
            }
            for (Quest link : chain) {
                for (QuestTask task : link.tasks) {
                    for (QuestTask.ItemHold hold : task.supplies()) {
                        supplied.merge(hold.item, hold.amount, Integer::sum);
                    }
                    if (link != q) {
                        for (QuestTask.ItemHold hold : task.consumes()) {
                            consumed.merge(hold.item, hold.amount, Integer::sum);
                        }
                    }
                }
                for (QuestReward reward : link.rewards) {
                    if (reward instanceof QuestReward.ItemReward ir) {
                        supplied.merge(ir.item, ir.count, Integer::sum);
                    }
                }
            }

            for (Map.Entry<ResourceLocation, Integer> want : wanted.entrySet()) {
                ResourceLocation item = want.getKey();
                int need = want.getValue();
                int have = supplied.getOrDefault(item, 0) - consumed.getOrDefault(item, 0);
                if (have < need) {
                    int eaten = consumed.getOrDefault(item, 0);
                    errors.add(q.id + " asks the player to hand over " + need + "x " + item
                            + ", but the quest chain leading to it only ever provides "
                            + supplied.getOrDefault(item, 0)
                            + (eaten > 0 ? " and a prerequisite already consumes " + eaten : "")
                            + " (net " + have + "). Add an obtain/craft task for the shortfall, "
                            + "or reduce the amount delivered.");
                }
            }
        }
    }

    // ---- 9: milestones ------------------------------------------------------

    private static void checkMilestones(QuestFile file, List<String> warnings) {
        for (Quest q : file.allQuests()) {
            if (q.isMilestone() && !q.rewards.isEmpty() && !q.autoClaim) {
                warnings.add(q.id + " has no tasks but does not auto_claim; it will sit in the book "
                        + "as a completed quest with an unclaimed reward. Set \"auto_claim\": true.");
            }
            if (q.isMilestone() && q.dependencies.isEmpty() && q.rewards.isEmpty()) {
                warnings.add(q.id + " is a root milestone with no tasks and no rewards; it does nothing "
                        + "except exist. That may be intentional as a tree anchor.");
            }
        }
    }
}
