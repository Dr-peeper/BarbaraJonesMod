package com.barbarajones.v2.quests;

import com.barbarajones.progression.KraveLevel;

import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * What a quest actually hands over.
 *
 * <p>The rule this class exists to enforce: a reward that does not GRANT is not a
 * reward. Every subtype's {@link #grant} does a real, observable thing to the
 * server-side player - items into the inventory (or onto the floor if it is full,
 * never silently voided), XP through the existing {@link KraveLevel} curve, an
 * ability or schematic flag written into the player's own persistent data where
 * the rest of the mod can read it back.
 */
public abstract class QuestReward {

    public enum Kind {
        ITEM("item"),
        KRAVE_XP("krave_xp"),
        ABILITY("ability"),
        SCHEMATIC("schematic");

        public final String key;

        Kind(String key) {
            this.key = key;
        }

        public static Kind byKey(String key) {
            for (Kind k : values()) {
                if (k.key.equals(key)) {
                    return k;
                }
            }
            throw new QuestSyntaxException("unknown reward type '" + key + "'");
        }
    }

    public final Kind kind;

    protected QuestReward(Kind kind) {
        this.kind = kind;
    }

    /** One line for the book's reward list. */
    public abstract Component describe();

    /** Do the thing. Server side only; the caller has already checked it is unclaimed. */
    public abstract void grant(ServerPlayer player);

    protected abstract void write(FriendlyByteBuf buf);

    public final void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.kind.ordinal());
        write(buf);
    }

    public static QuestReward decode(FriendlyByteBuf buf) {
        Kind kind = Kind.values()[buf.readVarInt()];
        switch (kind) {
            case ITEM:
                return new ItemReward(buf.readResourceLocation(), buf.readVarInt());
            case KRAVE_XP:
                return new KraveXpReward(buf.readVarInt());
            case ABILITY:
                return new AbilityReward(buf.readUtf());
            case SCHEMATIC:
                return new SchematicReward(buf.readResourceLocation());
            default:
                throw new IllegalStateException("unreachable reward kind " + kind);
        }
    }

    public static QuestReward parse(JsonObject json) {
        Kind kind = Kind.byKey(GsonHelper.getAsString(json, "type"));
        switch (kind) {
            case ITEM: {
                ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
                return new ItemReward(id, GsonHelper.getAsInt(json, "count", 1));
            }
            case KRAVE_XP:
                return new KraveXpReward(GsonHelper.getAsInt(json, "amount"));
            case ABILITY:
                return new AbilityReward(GsonHelper.getAsString(json, "ability"));
            case SCHEMATIC:
                return new SchematicReward(new ResourceLocation(GsonHelper.getAsString(json, "schematic")));
            default:
                throw new QuestSyntaxException("unhandled reward type");
        }
    }

    // =====================================================================

    /** N of an item. Overflow drops at the player's feet rather than vanishing. */
    public static final class ItemReward extends QuestReward {
        public final ResourceLocation item;
        public final int count;

        public ItemReward(ResourceLocation item, int count) {
            super(Kind.ITEM);
            this.item = item;
            this.count = Math.max(1, count);
        }

        @Override
        public Component describe() {
            Item resolved = ForgeRegistries.ITEMS.getValue(this.item);
            Component name = resolved == null
                    ? Component.literal(this.item.toString())
                    : new ItemStack(resolved).getHoverName();
            return Component.translatable("quest.barbarajones.reward.item", this.count, name)
                    .withStyle(ChatFormatting.GOLD);
        }

        @Override
        public void grant(ServerPlayer player) {
            Item resolved = ForgeRegistries.ITEMS.getValue(this.item);
            if (resolved == null) {
                // Validated at load, so this can only happen if a mod unloaded under us.
                QuestModule.LOG.error("Quest reward references missing item {}", this.item);
                return;
            }
            int remaining = this.count;
            int max = new ItemStack(resolved).getMaxStackSize();
            while (remaining > 0) {
                int take = Math.min(remaining, max);
                ItemStack stack = new ItemStack(resolved, take);
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                remaining -= take;
            }
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.item);
            buf.writeVarInt(this.count);
        }
    }

    /** Krave XP, routed through the mod's existing level curve so the HUD updates itself. */
    public static final class KraveXpReward extends QuestReward {
        public final int amount;

        public KraveXpReward(int amount) {
            super(Kind.KRAVE_XP);
            this.amount = Math.max(1, amount);
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.reward.krave_xp", this.amount)
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        }

        @Override
        public void grant(ServerPlayer player) {
            KraveLevel.award(player, this.amount, "quest");
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.amount);
        }
    }

    /**
     * A named ability flag. Written into the player's quest data so
     * {@link QuestApi#hasAbility} answers for any other module - abilities granted
     * by quests are additive to the level-gated perk table, never a replacement.
     */
    public static final class AbilityReward extends QuestReward {
        public final String ability;

        public AbilityReward(String ability) {
            super(Kind.ABILITY);
            this.ability = ability;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.reward.ability",
                    Component.translatable("quest.barbarajones.ability."
                            + this.ability.toLowerCase(Locale.ROOT)))
                    .withStyle(ChatFormatting.AQUA);
        }

        @Override
        public void grant(ServerPlayer player) {
            PlayerQuests data = PlayerQuests.of(player);
            data.unlockAbility(this.ability);
            player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.ability_unlocked",
                    Component.translatable("quest.barbarajones.ability."
                            + this.ability.toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.AQUA));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.ability);
        }
    }

    /**
     * A building schematic. Recorded as an unlock rather than an item, so the
     * village/building module can ask {@link QuestApi#hasSchematic} without the
     * player having to carry a piece of paper around forever.
     */
    public static final class SchematicReward extends QuestReward {
        public final ResourceLocation schematic;

        public SchematicReward(ResourceLocation schematic) {
            super(Kind.SCHEMATIC);
            this.schematic = schematic;
        }

        @Override
        public Component describe() {
            return Component.translatable("quest.barbarajones.reward.schematic",
                    Component.translatable("quest.barbarajones.schematic."
                            + this.schematic.getPath().replace('/', '.')))
                    .withStyle(ChatFormatting.GREEN);
        }

        @Override
        public void grant(ServerPlayer player) {
            PlayerQuests data = PlayerQuests.of(player);
            data.unlockSchematic(this.schematic);
            player.sendSystemMessage(Component.translatable("quest.barbarajones.msg.schematic_unlocked",
                    Component.translatable("quest.barbarajones.schematic."
                            + this.schematic.getPath().replace('/', '.'))).withStyle(ChatFormatting.GREEN));
        }

        @Override
        protected void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.schematic);
        }
    }
}
