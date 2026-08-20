package com.barbarajones.v2.quests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One player's quest progress, stored SERVER SIDE.
 *
 * <p>This is the single largest change from the old system, and the reason the old
 * book was so untrustworthy. Progress used to live in the NBT of the Quest Book
 * ItemStack. That meant: lose the book and you lose the questline; craft a second
 * book and you have two divergent questlines; and - worst of all - the screen read
 * the CLIENT's copy of that stack, which is a replica the server updates on its own
 * schedule, so the book routinely drew a state the server did not agree with. A
 * quest could be complete on the server and grey in your hands.
 *
 * <p>Now the server owns every number here and the client is told about it
 * explicitly (see {@link com.barbarajones.v2.quests.net.S2CQuestState}). The client
 * never computes completion; it only draws what it was sent.
 *
 * <p>Storage is the player's {@link Player#PERSISTED_NBT_TAG} bag, under one key,
 * matching how {@code KraveLevel} and the rest of the mod already persist per-player
 * state. Forge copies that tag across death and respawn, which is what we want: the
 * whole point of Rule #1 is that death is common, and losing the questline every
 * time somebody dies would be intolerable.
 */
public final class PlayerQuests {

    private static final String ROOT = "QuestsV2";

    private static final String K_PROGRESS   = "Progress";     // taskKey -> int
    private static final String K_COMPLETED  = "Completed";    // quest ids
    private static final String K_CLAIMED    = "Claimed";      // quest ids whose rewards are spent
    private static final String K_NOTIFIED   = "Notified";     // quests already announced as open
    private static final String K_ABILITIES  = "Abilities";
    private static final String K_SCHEMATICS = "Schematics";
    private static final String K_PLACED     = "Placed";       // block id -> lifetime placements
    private static final String K_TIER       = "VillageTier";
    private static final String K_BOOTSTRAP  = "Bootstrapped"; // starter codex handed out

    private final CompoundTag tag;

    private PlayerQuests(CompoundTag tag) {
        this.tag = tag;
    }

    /**
     * This player's quest data, created on demand.
     *
     * <p>{@code getCompound} hands back the live instance once the key exists, so
     * writes through the returned tag stick without having to put it back - the same
     * contract the rest of the mod's persistent state relies on. The {@code contains}
     * guard is what makes that true; without it every call would hand out a fresh
     * detached tag and every write would evaporate.
     */
    public static PlayerQuests of(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, new CompoundTag());
        }
        return new PlayerQuests(persisted.getCompound(ROOT));
    }

    private CompoundTag sub(String key) {
        if (!this.tag.contains(key)) {
            this.tag.put(key, new CompoundTag());
        }
        return this.tag.getCompound(key);
    }

    private ListTag list(String key) {
        if (!this.tag.contains(key)) {
            this.tag.put(key, new ListTag());
        }
        return this.tag.getList(key, Tag.TAG_STRING);
    }

    private boolean listContains(String key, String value) {
        ListTag l = list(key);
        for (int i = 0; i < l.size(); i++) {
            if (l.getString(i).equals(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean listAdd(String key, String value) {
        if (listContains(key, value)) {
            return false;
        }
        list(key).add(StringTag.valueOf(value));
        return true;
    }

    private Set<String> listAsSet(String key) {
        Set<String> out = new HashSet<>();
        ListTag l = list(key);
        for (int i = 0; i < l.size(); i++) {
            out.add(l.getString(i));
        }
        return out;
    }

    // ---- task progress ------------------------------------------------------

    public int progress(String taskKey) {
        return sub(K_PROGRESS).getInt(taskKey);
    }

    /**
     * Cumulative bump, for event-driven tasks (a kill happened, a craft happened).
     * Only ever called from a handler that fires exactly once per occurrence.
     *
     * @return true if the stored number actually changed
     */
    public boolean addProgress(String taskKey, int amount, int cap) {
        if (amount <= 0) {
            return false;
        }
        CompoundTag p = sub(K_PROGRESS);
        int now = p.getInt(taskKey);
        if (now >= cap) {
            return false;
        }
        int next = Math.min(cap, now + amount);
        p.putInt(taskKey, next);
        return next != now;
    }

    /**
     * High-water bump, for sampled tasks (how much of this item are you holding,
     * what tier is your village). Idempotent - safe to call on a timer as often as
     * you like, which a cumulative counter emphatically is not.
     *
     * @return true if the stored number actually changed
     */
    public boolean raiseProgress(String taskKey, int observed, int cap) {
        CompoundTag p = sub(K_PROGRESS);
        int now = p.getInt(taskKey);
        int next = Math.min(cap, Math.max(now, observed));
        if (next == now) {
            return false;
        }
        p.putInt(taskKey, next);
        return true;
    }

    /** Every stored counter, for a full client sync. */
    public Map<String, Integer> allProgress() {
        Map<String, Integer> out = new LinkedHashMap<>();
        CompoundTag p = sub(K_PROGRESS);
        for (String key : p.getAllKeys()) {
            out.put(key, p.getInt(key));
        }
        return out;
    }

    // ---- completion ---------------------------------------------------------

    public boolean isComplete(ResourceLocation quest) {
        return listContains(K_COMPLETED, quest.toString());
    }

    public boolean markComplete(ResourceLocation quest) {
        return listAdd(K_COMPLETED, quest.toString());
    }

    public Set<String> completed() {
        return listAsSet(K_COMPLETED);
    }

    public boolean isClaimed(ResourceLocation quest) {
        return listContains(K_CLAIMED, quest.toString());
    }

    public boolean markClaimed(ResourceLocation quest) {
        return listAdd(K_CLAIMED, quest.toString());
    }

    public Set<String> claimed() {
        return listAsSet(K_CLAIMED);
    }

    /** True the first time a quest is announced as newly open; false ever after. */
    public boolean markNotified(ResourceLocation quest) {
        return listAdd(K_NOTIFIED, quest.toString());
    }

    // ---- unlocks ------------------------------------------------------------

    public boolean unlockAbility(String ability) {
        return listAdd(K_ABILITIES, ability);
    }

    public boolean hasAbility(String ability) {
        return listContains(K_ABILITIES, ability);
    }

    public Set<String> abilities() {
        return listAsSet(K_ABILITIES);
    }

    public boolean unlockSchematic(ResourceLocation schematic) {
        return listAdd(K_SCHEMATICS, schematic.toString());
    }

    public boolean hasSchematic(ResourceLocation schematic) {
        return listContains(K_SCHEMATICS, schematic.toString());
    }

    public Set<String> schematics() {
        return listAsSet(K_SCHEMATICS);
    }

    // ---- village ------------------------------------------------------------

    /** Lifetime count of a given building block this player has placed. */
    public int placed(ResourceLocation block) {
        return sub(K_PLACED).getInt(block.toString());
    }

    public int bumpPlaced(ResourceLocation block) {
        CompoundTag p = sub(K_PLACED);
        int now = p.getInt(block.toString()) + 1;
        p.putInt(block.toString(), now);
        return now;
    }

    public int villageTier() {
        return this.tag.getInt(K_TIER);
    }

    /** Monotone: the tier never drops, so knocking a wall down cannot un-earn a quest. */
    public boolean raiseVillageTier(int tier) {
        if (tier <= this.tag.getInt(K_TIER)) {
            return false;
        }
        this.tag.putInt(K_TIER, tier);
        return true;
    }

    // ---- bootstrap ----------------------------------------------------------

    public boolean needsBootstrap() {
        return !this.tag.getBoolean(K_BOOTSTRAP);
    }

    public void markBootstrapped() {
        this.tag.putBoolean(K_BOOTSTRAP, true);
    }
}
