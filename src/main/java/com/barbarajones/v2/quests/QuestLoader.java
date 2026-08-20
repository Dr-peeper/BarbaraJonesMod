package com.barbarajones.v2.quests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads {@code data/&lt;namespace&gt;/quests/} into a {@link QuestFile}.
 *
 * <p>File layout:
 * <ul>
 *   <li>{@code quests/chapters/&lt;name&gt;.json} - a chapter (a tab in the book).</li>
 *   <li>{@code quests/&lt;anything else&gt;.json} - a quest. Subfolders are free; the
 *       quest's id is its full path, e.g. {@code barbarajones:spine/first_house}.</li>
 * </ul>
 *
 * <p>Because it is a datapack listener, quests reload with {@code /reload} and can be
 * overridden or extended by any datapack, which is the entire point of moving them
 * out of a hardcoded Java list.
 *
 * <p>Failure policy: a single bad file is fatal. A quest pack that half-loads is
 * worse than one that does not load, because the half that vanished is exactly the
 * half nobody notices until the chain dead-ends.
 */
public final class QuestLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIRECTORY = "quests";
    private static final String CHAPTER_PREFIX = "chapters/";

    /** Rebuilt on every reload; never mutated in place. */
    private static volatile QuestFile current = QuestFile.EMPTY;

    public QuestLoader() {
        super(GSON, DIRECTORY);
    }

    /** The live server-side graph. Safe to read from any thread. */
    public static QuestFile file() {
        return current;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager manager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, QuestChapter> chapters = new LinkedHashMap<>();
        Map<ResourceLocation, Quest> quests = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "quest file");
            if (id.getPath().startsWith(CHAPTER_PREFIX)) {
                ResourceLocation chapterId = new ResourceLocation(id.getNamespace(),
                        id.getPath().substring(CHAPTER_PREFIX.length()));
                chapters.put(chapterId, QuestChapter.parse(chapterId, json));
            } else {
                quests.put(id, Quest.parse(id, json));
            }
        }

        QuestFile built = new QuestFile(chapters, quests);
        // Throws on anything unplayable. Deliberately BEFORE the assignment, so a
        // broken pack leaves the previous good graph alone instead of replacing it
        // with rubble.
        QuestValidator.validate(built);
        current = built;
        QuestModule.LOG.info("Loaded {} quests in {} chapters from datapacks.",
                built.size(), built.orderedChapters().size());
    }
}
