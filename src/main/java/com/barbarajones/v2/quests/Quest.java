package com.barbarajones.v2.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One node of the quest graph, parsed straight out of a JSON file under
 * {@code data/barbarajones/quests/}.
 *
 * <p>A Quest is pure DATA. It knows what it is called, what it asks for, what it
 * hands over and which other quests must be finished before it opens - and
 * nothing whatever about who is playing. Every player-specific number lives in
 * {@link PlayerQuests}. The old system hardcoded the whole questline as a static
 * initialiser in a Java class, which meant a typo in one objective needed a
 * recompile and a wrong prerequisite needed a code review.
 *
 * <p>Layout ({@link #x}/{@link #y}) rides on the quest itself, FTB-style, so the
 * tree screen renders the graph by iterating quests and reading coordinates
 * rather than consulting a second, separately-maintained layout table that can
 * drift out of sync with the quest list.
 */
public final class Quest {

    public final ResourceLocation id;
    public final ResourceLocation chapter;
    /** Translation key. Never raw text - the lang file is the single source of wording. */
    public final String titleKey;
    /** Translation key for the "what do I actually do" line. Shown even while locked. */
    public final String objectiveKey;
    /** Optional translation key for flavour text. May be null. */
    @Nullable
    public final String loreKey;
    public final ItemStack icon;
    /** Grid coordinates. The tree screen multiplies these by a cell size. */
    public final int x;
    public final int y;
    public final List<ResourceLocation> dependencies;
    /**
     * How many dependencies must be complete for this quest to unlock. Defaults to
     * "all of them"; a smaller number makes an any-N-of gate - useful where a boss
     * can be reached down more than one branch.
     */
    public final int minDependencies;
    public final List<QuestTask> tasks;
    public final List<QuestReward> rewards;
    /** True: rewards land the instant the quest completes. False: the player claims them. */
    public final boolean autoClaim;

    private Quest(ResourceLocation id, ResourceLocation chapter, String titleKey, String objectiveKey,
                  @Nullable String loreKey, ItemStack icon, int x, int y,
                  List<ResourceLocation> dependencies, int minDependencies,
                  List<QuestTask> tasks, List<QuestReward> rewards, boolean autoClaim) {
        this.id = id;
        this.chapter = chapter;
        this.titleKey = titleKey;
        this.objectiveKey = objectiveKey;
        this.loreKey = loreKey;
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.dependencies = Collections.unmodifiableList(dependencies);
        this.minDependencies = minDependencies;
        this.tasks = Collections.unmodifiableList(tasks);
        this.rewards = Collections.unmodifiableList(rewards);
        this.autoClaim = autoClaim;
    }

    // ---- text ---------------------------------------------------------------

    public Component title() {
        return Component.translatable(this.titleKey);
    }

    public Component objective() {
        return Component.translatable(this.objectiveKey);
    }

    @Nullable
    public Component lore() {
        return this.loreKey == null ? null : Component.translatable(this.loreKey);
    }

    /** Stable per-task progress key: {@code <quest id>#<index>}. Readable in an NBT dump. */
    public String taskKey(int index) {
        return this.id + "#" + index;
    }

    /** A quest with no tasks completes as soon as it unlocks - a pure milestone node. */
    public boolean isMilestone() {
        return this.tasks.isEmpty();
    }

    // ---- parsing ------------------------------------------------------------

    /**
     * @param id the file's resource id, e.g. {@code barbarajones:spine/wake_up}
     * @throws QuestSyntaxException naming the offending file, so a datapack typo
     *         points at a filename rather than at a stack trace inside Gson
     */
    public static Quest parse(ResourceLocation id, JsonObject json) {
        try {
            ResourceLocation chapter = new ResourceLocation(GsonHelper.getAsString(json, "chapter"));
            String titleKey = GsonHelper.getAsString(json, "title");
            String objectiveKey = GsonHelper.getAsString(json, "objective");
            String loreKey = json.has("lore") ? GsonHelper.getAsString(json, "lore") : null;

            ItemStack icon = new ItemStack(Items.PAPER);
            if (json.has("icon")) {
                ResourceLocation iconId = new ResourceLocation(GsonHelper.getAsString(json, "icon"));
                if (!ForgeRegistries.ITEMS.containsKey(iconId)) {
                    throw new QuestSyntaxException("icon " + iconId + " is not a registered item");
                }
                icon = new ItemStack(ForgeRegistries.ITEMS.getValue(iconId));
            }

            int x = GsonHelper.getAsInt(json, "x", 0);
            int y = GsonHelper.getAsInt(json, "y", 0);

            List<ResourceLocation> deps = new ArrayList<>();
            if (json.has("dependencies")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "dependencies");
                for (int i = 0; i < arr.size(); i++) {
                    String raw = arr.get(i).getAsString();
                    ResourceLocation dep = ResourceLocation.tryParse(raw);
                    if (dep == null) {
                        throw new QuestSyntaxException("dependency " + raw + " is not a valid id");
                    }
                    deps.add(dep);
                }
            }
            int minDeps = GsonHelper.getAsInt(json, "min_dependencies", deps.size());

            List<QuestTask> tasks = new ArrayList<>();
            if (json.has("tasks")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "tasks");
                for (int i = 0; i < arr.size(); i++) {
                    tasks.add(QuestTask.parse(GsonHelper.convertToJsonObject(arr.get(i), "task")));
                }
            }

            List<QuestReward> rewards = new ArrayList<>();
            if (json.has("rewards")) {
                JsonArray arr = GsonHelper.getAsJsonArray(json, "rewards");
                for (int i = 0; i < arr.size(); i++) {
                    rewards.add(QuestReward.parse(GsonHelper.convertToJsonObject(arr.get(i), "reward")));
                }
            }

            boolean autoClaim = GsonHelper.getAsBoolean(json, "auto_claim", false);

            return new Quest(id, chapter, titleKey, objectiveKey, loreKey, icon, x, y,
                    deps, minDeps, tasks, rewards, autoClaim);
        } catch (QuestSyntaxException e) {
            throw new QuestSyntaxException(id + ": " + e.getRawMessage());
        } catch (Exception e) {
            throw new QuestSyntaxException(id + ": " + e.getMessage());
        }
    }

    // ---- network ------------------------------------------------------------
    // Definitions go to the client once, on login and on datapack reload. Progress
    // travels separately and far more often; see S2CQuestProgress. Keeping the two
    // apart is the whole reason progress updates can be a handful of bytes.

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.id);
        buf.writeResourceLocation(this.chapter);
        buf.writeUtf(this.titleKey);
        buf.writeUtf(this.objectiveKey);
        buf.writeBoolean(this.loreKey != null);
        if (this.loreKey != null) {
            buf.writeUtf(this.loreKey);
        }
        buf.writeItem(this.icon);
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.y);
        buf.writeVarInt(this.dependencies.size());
        for (ResourceLocation dep : this.dependencies) {
            buf.writeResourceLocation(dep);
        }
        buf.writeVarInt(this.minDependencies);
        buf.writeVarInt(this.tasks.size());
        for (QuestTask task : this.tasks) {
            task.encode(buf);
        }
        buf.writeVarInt(this.rewards.size());
        for (QuestReward reward : this.rewards) {
            reward.encode(buf);
        }
        buf.writeBoolean(this.autoClaim);
    }

    public static Quest decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        ResourceLocation chapter = buf.readResourceLocation();
        String titleKey = buf.readUtf();
        String objectiveKey = buf.readUtf();
        String loreKey = buf.readBoolean() ? buf.readUtf() : null;
        ItemStack icon = buf.readItem();
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int depCount = buf.readVarInt();
        List<ResourceLocation> deps = new ArrayList<>(depCount);
        for (int i = 0; i < depCount; i++) {
            deps.add(buf.readResourceLocation());
        }
        int minDeps = buf.readVarInt();
        int taskCount = buf.readVarInt();
        List<QuestTask> tasks = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            tasks.add(QuestTask.decode(buf));
        }
        int rewardCount = buf.readVarInt();
        List<QuestReward> rewards = new ArrayList<>(rewardCount);
        for (int i = 0; i < rewardCount; i++) {
            rewards.add(QuestReward.decode(buf));
        }
        boolean autoClaim = buf.readBoolean();
        return new Quest(id, chapter, titleKey, objectiveKey, loreKey, icon, x, y,
                deps, minDeps, tasks, rewards, autoClaim);
    }
}
