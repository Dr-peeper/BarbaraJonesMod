package com.barbarajones.v2.quests;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The whole quest graph, immutable once built.
 *
 * <p>Built by {@link QuestLoader} on datapack load, checked by
 * {@link QuestValidator} before it is ever published, then handed out read-only.
 * The server holds one; each client holds a mirror pushed over the wire.
 *
 * <p>Two indexes are maintained up front rather than computed on demand:
 * <ul>
 *   <li>{@link #dependants} - the reverse edges. When a quest completes the
 *       engine has to know which quests might now open. Scanning every quest in
 *       the file for each completion is what made the old fixpoint sweep
 *       quadratic and made "complete then re-sweep then complete again" so easy
 *       to get wrong; with reverse edges the engine touches exactly the nodes
 *       that could have changed.</li>
 *   <li>{@link #roots} - the quests with no dependencies, which is where the
 *       reachability proof starts.</li>
 * </ul>
 */
public final class QuestFile {

    public static final QuestFile EMPTY = new QuestFile(Map.of(), Map.of());

    private final Map<ResourceLocation, Quest> quests;
    private final Map<ResourceLocation, QuestChapter> chapters;
    private final Map<ResourceLocation, List<ResourceLocation>> dependants;
    private final List<ResourceLocation> roots;
    private final List<QuestChapter> orderedChapters;
    private final Map<ResourceLocation, List<Quest>> byChapter;

    public QuestFile(Map<ResourceLocation, QuestChapter> chapters, Map<ResourceLocation, Quest> quests) {
        this.chapters = Collections.unmodifiableMap(new LinkedHashMap<>(chapters));
        this.quests = Collections.unmodifiableMap(new LinkedHashMap<>(quests));

        Map<ResourceLocation, List<ResourceLocation>> reverse = new HashMap<>();
        List<ResourceLocation> rootIds = new ArrayList<>();
        for (Quest q : quests.values()) {
            if (q.dependencies.isEmpty()) {
                rootIds.add(q.id);
            }
            for (ResourceLocation dep : q.dependencies) {
                reverse.computeIfAbsent(dep, k -> new ArrayList<>()).add(q.id);
            }
        }
        this.dependants = Collections.unmodifiableMap(reverse);
        this.roots = Collections.unmodifiableList(rootIds);

        List<QuestChapter> ordered = new ArrayList<>(chapters.values());
        ordered.sort(Comparator.<QuestChapter>comparingInt(c -> c.order)
                .thenComparing(c -> c.id.toString()));
        this.orderedChapters = Collections.unmodifiableList(ordered);

        Map<ResourceLocation, List<Quest>> grouped = new LinkedHashMap<>();
        for (QuestChapter c : ordered) {
            grouped.put(c.id, new ArrayList<>());
        }
        for (Quest q : quests.values()) {
            grouped.computeIfAbsent(q.chapter, k -> new ArrayList<>()).add(q);
        }
        for (List<Quest> list : grouped.values()) {
            list.sort(Comparator.<Quest>comparingInt(q -> q.y)
                    .thenComparingInt(q -> q.x)
                    .thenComparing(q -> q.id.toString()));
        }
        Map<ResourceLocation, List<Quest>> frozen = new LinkedHashMap<>();
        grouped.forEach((k, v) -> frozen.put(k, Collections.unmodifiableList(v)));
        this.byChapter = Collections.unmodifiableMap(frozen);
    }

    // ---- lookups ------------------------------------------------------------

    @Nullable
    public Quest quest(ResourceLocation id) {
        return this.quests.get(id);
    }

    @Nullable
    public QuestChapter chapter(ResourceLocation id) {
        return this.chapters.get(id);
    }

    public Collection<Quest> allQuests() {
        return this.quests.values();
    }

    public Collection<ResourceLocation> allQuestIds() {
        return this.quests.keySet();
    }

    public List<QuestChapter> orderedChapters() {
        return this.orderedChapters;
    }

    public List<Quest> questsIn(ResourceLocation chapter) {
        return this.byChapter.getOrDefault(chapter, List.of());
    }

    /** Quests that name this one as a dependency. Never null. */
    public List<ResourceLocation> dependantsOf(ResourceLocation id) {
        return this.dependants.getOrDefault(id, List.of());
    }

    public List<ResourceLocation> roots() {
        return this.roots;
    }

    public int size() {
        return this.quests.size();
    }

    public boolean isEmpty() {
        return this.quests.isEmpty();
    }

    /**
     * Every quest that must be finished before {@code id} can even be attempted,
     * transitively. Used by the validator's consumption check and by the book when
     * it explains why a node is shut.
     */
    public Set<ResourceLocation> ancestorsOf(ResourceLocation id) {
        Set<ResourceLocation> seen = new HashSet<>();
        List<ResourceLocation> stack = new ArrayList<>();
        Quest start = quest(id);
        if (start == null) {
            return seen;
        }
        stack.addAll(start.dependencies);
        while (!stack.isEmpty()) {
            ResourceLocation next = stack.remove(stack.size() - 1);
            if (!seen.add(next)) {
                continue;
            }
            Quest q = quest(next);
            if (q != null) {
                stack.addAll(q.dependencies);
            }
        }
        return seen;
    }
}
