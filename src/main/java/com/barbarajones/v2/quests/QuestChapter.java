package com.barbarajones.v2.quests;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A tab in the quest book. Chapters group quests for navigation only - dependencies
 * are free to cross chapter boundaries, and the tree screen draws those edges as
 * an explicit "continues in &lt;other chapter&gt;" marker rather than hiding them and
 * leaving the player wondering why a node will not open.
 */
public final class QuestChapter {

    public final ResourceLocation id;
    public final String titleKey;
    public final String descriptionKey;
    public final ItemStack icon;
    /** Sort order in the chapter rail; ties break on id. */
    public final int order;

    private QuestChapter(ResourceLocation id, String titleKey, String descriptionKey,
                         ItemStack icon, int order) {
        this.id = id;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.icon = icon;
        this.order = order;
    }

    public Component title() {
        return Component.translatable(this.titleKey);
    }

    public Component description() {
        return Component.translatable(this.descriptionKey);
    }

    public static QuestChapter parse(ResourceLocation id, JsonObject json) {
        try {
            String titleKey = GsonHelper.getAsString(json, "title");
            String descKey = GsonHelper.getAsString(json, "description");
            int order = GsonHelper.getAsInt(json, "order", 0);
            ItemStack icon = new ItemStack(Items.BOOK);
            if (json.has("icon")) {
                ResourceLocation iconId = new ResourceLocation(GsonHelper.getAsString(json, "icon"));
                if (!ForgeRegistries.ITEMS.containsKey(iconId)) {
                    throw new QuestSyntaxException("chapter icon " + iconId + " is not a registered item");
                }
                icon = new ItemStack(ForgeRegistries.ITEMS.getValue(iconId));
            }
            return new QuestChapter(id, titleKey, descKey, icon, order);
        } catch (QuestSyntaxException e) {
            throw new QuestSyntaxException(id + ": " + e.getRawMessage());
        } catch (Exception e) {
            throw new QuestSyntaxException(id + ": " + e.getMessage());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(this.id);
        buf.writeUtf(this.titleKey);
        buf.writeUtf(this.descriptionKey);
        buf.writeItem(this.icon);
        buf.writeVarInt(this.order);
    }

    public static QuestChapter decode(FriendlyByteBuf buf) {
        return new QuestChapter(buf.readResourceLocation(), buf.readUtf(), buf.readUtf(),
                buf.readItem(), buf.readVarInt());
    }
}
