package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Puts the item descriptions under every one of this mod's items on hover.
 *
 * <p>The text lives in the language file as {@code <kind>.barbarajones.<id>.desc},
 * next to the item's own name, rather than in a table in code. That means a new
 * item gets a description by adding one line to the same file you already had to
 * touch to give it a name - and an item with no description simply shows nothing
 * rather than a raw translation key, because the lookup checks whether the key
 * exists before using it.
 *
 * <p>Block items are checked under BOTH prefixes. A block's name is a
 * {@code block.} key while a plain item's is an {@code item.} key, and a
 * BlockItem is an Item holding a Block - so guessing one prefix silently drops
 * the description for every block in the mod.
 *
 * <p>Wrapped by word here rather than left as one long line: an unwrapped
 * tooltip runs off the side of the screen and the end of every joke is the part
 * you cannot read.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveTooltips {

    /** Characters per line. Roughly two thirds of a 1080p screen at GUI scale 2. */
    private static final int WRAP = 52;

    private KraveTooltips() { }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !BarbaraJonesMod.MODID.equals(id.getNamespace())) {
            return;   // not ours; other mods' tooltips are their business
        }

        String key = keyFor(stack, id);
        if (key == null) {
            return;
        }

        List<Component> lines = event.getToolTip();
        for (String line : wrap(I18n.get(key))) {
            lines.add(Component.literal(line)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    /**
     * The description key for this stack, or null if it has none.
     *
     * <p>A BlockItem is tried as a block first and an item second, because
     * that is the order the names themselves use.
     */
    private static String keyFor(ItemStack stack, ResourceLocation id) {
        if (stack.getItem() instanceof BlockItem) {
            String block = "block." + BarbaraJonesMod.MODID + "." + id.getPath() + ".desc";
            if (I18n.exists(block)) {
                return block;
            }
        }
        String item = "item." + BarbaraJonesMod.MODID + "." + id.getPath() + ".desc";
        return I18n.exists(item) ? item : null;
    }

    /**
     * Greedy word wrap. A word longer than the whole line still gets its own
     * line rather than being dropped - rare, but silently losing text is worse
     * than one overlong line.
     */
    private static List<String> wrap(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > WRAP) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }
}
