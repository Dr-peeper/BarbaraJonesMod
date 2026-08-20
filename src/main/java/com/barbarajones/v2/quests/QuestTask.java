package com.barbarajones.v2.quests;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One measurable thing a quest asks for.
 *
 * <p>The single most important property of every task type here is that progress
 * is <b>monotone</b>: it can go up and it can never go down. That is a deliberate
 * reaction to how the old system broke. The old engine asked
 * "are all of these items in your inventory right now?" and re-asked it on a
 * timer, so a quest that wanted DICED_GRASS was un-completed the moment you
 * burned the diced grass into the BURNT_GRASS the very next quest demanded. The
 * chain locked itself. Nothing in this file can do that: once a counter reaches
 * its target the quest latches complete and spending the items afterwards is
 * simply irrelevant.
 *
 * <p>Two flavours of counting, and which one a type uses is a design decision,
 * not an accident:
 * <ul>
 *   <li><b>Cumulative</b> ({@link Kind#CRAFT}, {@link Kind#KILL}, {@link Kind#PLACE_BUILDING},
 *       {@link Kind#FEED_CAYDEN}, {@link Kind#DELIVER}) - driven by a real event
 *       firing once per occurrence, so every occurrence adds exactly one and
 *       nothing is ever double counted.</li>
 *   <li><b>High-water</b> ({@link Kind#OBTAIN}, {@link Kind#VILLAGE_TIER},
 *       {@link Kind#KRAVE_LEVEL}) - measured by looking at current state and
 *       keeping the largest value ever seen. Safe to sample as often as you like
 *       because {@code max} is idempotent; sampling a cumulative counter on a
 *       timer is exactly how you end up counting the same wheat six times.</li>
 *   <li><b>Latch</b> ({@link Kind#VISIT_DIMENSION}, {@link Kind#DEFEAT_BOSS},
 *       {@link Kind#UNLOCK_ABILITY}) - a one-shot flag, target always 1.</li>
 * </ul>
 */
public abstract class QuestTask {

    /** Every task type the engine knows how to observe. */
    public enum Kind {
        KILL("kill"),
        OBTAIN("obtain"),
        CRAFT("craft"),
        DELIVER("deliver"),
        PLACE_BUILDING("place_building"),
        VILLAGE_TIER("village_tier"),
        FEED_CAYDEN("feed_cayden"),
        DEFEAT_BOSS("defeat_boss"),
        VISIT_DIMENSION("visit_dimension"),
        UNLOCK_ABILITY("unlock_ability"),
        KRAVE_LEVEL("krave_level");

        public final String key;

        Kind(String key) {
            this.key = key;
        }

        @Nullable
        public static Kind byKey(String key) {
            for (Kind k : values()) {
                if (k.key.equals(key)) {
                    return k;
                }
            }
            return null;
        }
    }

    public final Kind kind;
    /** How many times the thing must happen. Always at least 1. */
    public final int target;

    protected QuestTask(Kind kind, int target) {
        this.kind = kind;
        this.target = Math.max(1, target);
    }

    /**
     * Human-readable line for the book, e.g. "Craft 4x Krave Cereal (2/4)".
     * Written from the definition alone so a LOCKED quest can still print exactly
     * what it will ask for - the old book showed locked quests as an empty grey
     * box, which reads to a player as a bug rather than as a gate.
     */
    public abstract Component describe();

    /**
     * Items this task takes out of the player's hands when it completes. Empty for
     * everything except {@link Deliver}. {@link QuestValidator} reads this to prove
     * that no quest is asked to hand over something an earlier quest already ate.
     */
    public List<ItemHold> consumes() {
        return Collections.emptyList();
    }

    /**
     * Items this task hands the player, counted for the validator's supply check.
     * Only {@link Obtain} and {@link Craft} produce anything.
     */
    public List<ItemHold> supplies() {
        return Collections.emptyList();
    }

    protected abstract void write(FriendlyByteBuf buf);

    public final void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.kind.ordinal());
        buf.writeVarInt(this.target);
        write(buf);
    }

    public static QuestTask decode(FriendlyByteBuf buf) {
        Kind kind = Kind.values()[buf.readVarInt()];
        int target = buf.readVarInt();
        switch (kind) {
            case KILL:
                return new Kill(buf.readResourceLocation(), target);
            case OBTAIN:
                return new Obtain(buf.readResourceLocation(), target);
            case CRAFT:
                return new Craft(buf.readResourceLocation(), target);
            case DELIVER:
                return new Deliver(buf.readResourceLocation(), target);
            case PLACE_BUILDING:
                return new PlaceBuilding(buf.readResourceLocation(), target);
            case VILLAGE_TIER:
                return new VillageTier(target);
            case FEED_CAYDEN:
                return new FeedCayden(target);
            case DEFEAT_BOSS:
                return new DefeatBoss(buf.readResourceLocation());
            case VISIT_DIMENSION:
                return new VisitDimension(buf.readResourceLocation());
            case UNLOCK_ABILITY:
                return new UnlockAbility(buf.readUtf());
            case KRAVE_LEVEL:
                return new KraveLevelTask(target);
            default:
                throw new IllegalStateException("unreachable task kind " + kind);
        }
    }

    // ---- parsing ------------------------------------------------------------

    public static QuestTask parse(JsonObject json) {
        String typeKey = GsonHelper.getAsString(json, "type");
        Kind kind = Kind.byKey(typeKey);
        if (kind == null) {
            StringBuilder known = new StringBuilder();
            for (Kind k : Kind.values()) {
                known.append(known.length() == 0 ? "" : ", ").append(k.key);
            }
            throw new QuestSyntaxException("unknown task type '" + typeKey + "'. Known types: " + known);
        }
        int count = GsonHelper.getAsInt(json, "count", 1);
        switch (kind) {
            case KILL:
                return new Kill(rl(json, "entity"), count);
            case OBTAIN:
                return new Obtain(rl(json, "item"), count);
            case CRAFT:
                return new Craft(rl(json, "item"), count);
            case DELIVER:
                return new Deliver(rl(json, "item"), count);
            case PLACE_BUILDING:
                return new PlaceBuilding(rl(json, "building"), count);
            case VILLAGE_TIER:
                return new VillageTier(GsonHelper.getAsInt(json, "tier"));
            case FEED_CAYDEN:
                return new FeedCayden(count);
            case DEFEAT_BOSS:
                return new DefeatBoss(rl(json, "entity"));
            case VISIT_DIMENSION:
                return new VisitDimension(rl(json, "dimension"));
            case UNLOCK_ABILITY:
                return new UnlockAbility(GsonHelper.getAsString(json, "ability"));
            case KRAVE_LEVEL:
                return new KraveLevelTask(GsonHelper.getAsInt(json, "level"));
            default:
                throw new QuestSyntaxException("unhandled task type '" + typeKey + "'");
        }
    }

    private static ResourceLocation rl(JsonObject json, String field) {
        if (!json.has(field)) {
            throw new QuestSyntaxException("task is missing required field '" + field + "'");
        }
        String raw = GsonHelper.getAsString(json, field);
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            throw new QuestSyntaxException("'" + raw + "' is not a valid resource location");
        }
        return parsed;
    }

    /** An item id paired with an amount - the currency the validator does its arithmetic in. */
    public static final class ItemHold {
        public final ResourceLocation item;
        public final int amount;

        public ItemHold(ResourceLocation item, int amount) {
            this.item = item;
            this.amount = amount;
        }
    }

    private static Component itemName(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null ? Component.literal(id.toString()) : new ItemStack(item).getHoverName();
    }

    private static Component entityName(ResourceLocation id) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        return type == null ? Component.literal(id.toString()) : type.getDescription();
    }

    // =====================================================================
    // the types
    // =====================================================================

    /** Kill N of an entity type. Cumulative; driven by LivingDeathEvent. */
    public static final class Kill extends QuestTask {
        public final ResourceLocation entity;

        public Kill(ResourceLocation entity, int target) {
            super(Kind.KILL, target);
            this.entity = entity;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.kill", this.target, entityName(this.entity));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.entity);
        }
    }

    /**
     * Hold N of an item at once, ever. High-water mark: the counter records the
     * largest stack total the player has ever carried and never walks back down,
     * so crafting the item away the second after you get it still counts.
     */
    public static final class Obtain extends QuestTask {
        public final ResourceLocation item;

        public Obtain(ResourceLocation item, int target) {
            super(Kind.OBTAIN, target);
            this.item = item;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.obtain", this.target, itemName(this.item));
        }

        @Override
        public List<ItemHold> supplies() {
            return List.of(new ItemHold(this.item, this.target));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.item);
        }
    }

    /** Craft N of an item, counted over a lifetime by ItemCraftedEvent/ItemSmeltedEvent. */
    public static final class Craft extends QuestTask {
        public final ResourceLocation item;

        public Craft(ResourceLocation item, int target) {
            super(Kind.CRAFT, target);
            this.item = item;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.craft", this.target, itemName(this.item));
        }

        @Override
        public List<ItemHold> supplies() {
            return List.of(new ItemHold(this.item, this.target));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.item);
        }
    }

    /**
     * Hand N of an item over for good - the player presses Submit in the book and
     * the items leave the inventory. The only destructive task type, and the only
     * one the validator has to do supply arithmetic for.
     */
    public static final class Deliver extends QuestTask {
        public final ResourceLocation item;

        public Deliver(ResourceLocation item, int target) {
            super(Kind.DELIVER, target);
            this.item = item;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.deliver", this.target, itemName(this.item));
        }

        @Override
        public List<ItemHold> consumes() {
            return List.of(new ItemHold(this.item, this.target));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.item);
        }
    }

    /**
     * Place N of a building block. "Building" is a block id; the village module
     * counts them via BlockEvent.EntityPlaceEvent, which is a real event and not a
     * world scan.
     */
    public static final class PlaceBuilding extends QuestTask {
        public final ResourceLocation building;

        public PlaceBuilding(ResourceLocation building, int target) {
            super(Kind.PLACE_BUILDING, target);
            this.building = building;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.place_building",
                    this.target, itemName(this.building));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.building);
        }
    }

    /** Reach village tier N. High-water; see {@link VillageState}. */
    public static final class VillageTier extends QuestTask {
        public VillageTier(int tier) {
            super(Kind.VILLAGE_TIER, tier);
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.village_tier", this.target);
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            // target carries the tier
        }
    }

    /** Feed Cayden N bowls. High-water off Cayden's own lifetime fed counter. */
    public static final class FeedCayden extends QuestTask {
        public FeedCayden(int target) {
            super(Kind.FEED_CAYDEN, target);
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.feed_cayden", this.target);
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
        }
    }

    /** Be present for the death of a named boss. Latch. */
    public static final class DefeatBoss extends QuestTask {
        public final ResourceLocation entity;

        public DefeatBoss(ResourceLocation entity) {
            super(Kind.DEFEAT_BOSS, 1);
            this.entity = entity;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.defeat_boss", entityName(this.entity));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.entity);
        }
    }

    /** Set foot in a dimension. Latch. */
    public static final class VisitDimension extends QuestTask {
        public final ResourceLocation dimension;

        public VisitDimension(ResourceLocation dimension) {
            super(Kind.VISIT_DIMENSION, 1);
            this.dimension = dimension;
        }

        @Override
        public Component describe() {
            String key = "dimension." + this.dimension.getNamespace() + "."
                    + this.dimension.getPath().replace('/', '.');
            return Component.translatable("quest.barbarajones.task.visit_dimension",
                    Component.translatable(key));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.dimension);
        }
    }

    /** Own a named ability/perk. Latch, sampled from the perk table. */
    public static final class UnlockAbility extends QuestTask {
        public final String ability;

        public UnlockAbility(String ability) {
            super(Kind.UNLOCK_ABILITY, 1);
            this.ability = ability;
        }

        @Override
        public Component describe() {
            String pretty = this.ability.toLowerCase(Locale.ROOT);
            return Component.translatable("quest.barbarajones.task.unlock_ability",
                    Component.translatable("quest.barbarajones.ability." + pretty));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.ability);
        }
    }

    /** Reach Krave level N. High-water off the existing KraveLevel curve. */
    public static final class KraveLevelTask extends QuestTask {
        public KraveLevelTask(int level) {
            super(Kind.KRAVE_LEVEL, level);
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.task.krave_level", this.target);
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
        }
    }

    /** Convenience for the validator: every item id this task mentions, either way. */
    public List<ResourceLocation> mentionedItems() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ItemHold h : supplies()) {
            out.add(h.item);
        }
        for (ItemHold h : consumes()) {
            out.add(h.item);
        }
        return out;
    }
}
