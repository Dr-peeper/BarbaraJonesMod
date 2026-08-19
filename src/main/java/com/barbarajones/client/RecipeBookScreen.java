package com.barbarajones.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Barbara's Cookbook: paged list of every crafting recipe in the mod. */
public class RecipeBookScreen extends Screen {

    private static final String G = ChatFormatting.GREEN.toString();
    private static final String Y = ChatFormatting.YELLOW.toString();
    private static final String W = ChatFormatting.WHITE.toString();
    private static final String GR = ChatFormatting.GRAY.toString();

    private static final String[][] PAGES = {
        { Y + "THE GRASS PIPELINE",
          G + "Handful of Grass" + W + " = 3x any grass or fern",
          G + "Diced Grass x2" + W + " = Handful + Grass Knife",
          G + "Burnt Grass" + W + " = Diced + Blowtorch (or smelt)",
          G + "Rolling Paper" + W + " = 1x Paper",
          G + "Rolled Joint" + W + " = Paper + 2x Burnt Grass",
          G + "Golden Joint" + W + " = Rolled Joint + Gold Ingot",
          G + "Grass Brownie" + W + " = Diced + Cocoa + Wheat",
          G + "Grass Seeds x2" + W + " = Handful + Wheat Seeds",
          G + "Bong" + W + " = Glass Bottle + Iron + Diced Grass" },
        { Y + "TOOLS OF THE TRADE",
          G + "Grass Knife" + W + " = Iron + Stick",
          G + "Blowtorch" + W + " = Iron + Flint&Steel + Iron",
          G + "Daniel's Lighter" + W + " = Flint + Iron",
          G + "Microphone" + W + " = 2x Iron + Coal",
          G + "Camera" + W + " = Iron + Glass + Redstone",
          G + "Housing Query" + W + " = Stick + Glass + Iron",
          GR + "  (right-click a room: will Cayden live here?)",
          G + "Ashtray" + W + " = Iron + 2x Cobblestone",
          G + "Toothbrush" + W + " = Stick + Bone",
          G + "Towel" + W + " = 2x Wool + String",
          G + "Soap" + W + " = Slimeball + Sugar" },
        { Y + "DRINKS & FOOD",
          G + "Pibb Cocktail" + W + " = Mr. Pibb + Chepina",
          G + "Pibb ZERO" + W + " = Mr. Pibb + Glass Bottle",
          GR + "  (zero sugar. zero joy.)",
          G + "Krave Milk" + W + " = Milk Bucket + Krave Cereal",
          G + "Cereal Bowl" + W + " = Bowl + Krave + Krave Milk",
          GR + "  (the full breakfast. huge buffs.)",
          G + "Krave Cereal x2" + W + " = 2x Wheat + Sugar + Cocoa",
          G + "Chicken Nuggets x4" + W + " = Cooked Chicken",
          G + "10-Piece Box" + W + " = 2x Nuggets + Paper",
          G + "Fries x2" + W + " = 2x Potato",
          G + "Donut" + W + " = Wheat + Sugar + Egg",
          G + "Donut Box" + W + " = 3x Donut + Paper" },
        { Y + "THE KRAVE QUESTLINE",
          G + "Krave Box" + W + " = 2x Paper + Krave Cereal",
          GR + "  (right-click to summon HIM - stage 3+)",
          G + "Quest Book" + W + " = Book + Krave Cereal",
          G + "This Cookbook" + W + " = Book + Paper",
          GR + " ",
          GR + "Feed Cayden Krave: +1 atk per 5 boxes.",
          GR + "25 boxes = KRAVE RAGE.",
          GR + "Right-click him EMPTY-HANDED in a room to",
          GR + "make him move in. He needs REAL housing:",
          GR + "sealed, 30+ space, 3 tall, light, door, bed, floor.",
          ChatFormatting.DARK_RED + "Every pet death = a DEATH STAGE. Ten of them." },
        { Y + "MEMORABILIA",
          G + "Manager's Tie" + W + " = String + Red Dye",
          G + "Child Support Papers" + W + " = 3x Paper",
          G + "Fly Rich Poster" + W + " = 2x Paper + Purple Dye",
          G + "Barbara Plush" + W + " = 2x Wool + Handful of Grass",
          G + "Yellow Teeth" + W + " = Bone + Yellow Dye",
          GR + "  (them teeth. from the video.)" },
        { Y + "ACT II - THE LORE",
          G + "Backwards Red Hat" + W + " = Red Wool + String + Red Dye",
          GR + "  (right-click: I KRAVE THE KRAVE!)",
          G + "All-Red Fit" + W + " = 2x Red Wool",
          G + "Computer Mouse" + W + " = Iron + Redstone + String",
          GR + "  (right-click: throw it. SMASH.)",
          G + "Pirated MC Download" + W + " = Paper + Redstone",
          G + "COMPUTER VIRUS" + W + " = Download + Rotten Flesh",
          G + "Mom's $500" + W + " = 2x Gold + Paper",
          G + "Name-Brand Pastries" + W + " = Bread + Sugar",
          G + "Ski Mask" + W + " = Black Wool + String",
          G + "Mom's Belt" + W + " = Leather + Iron",
          G + "Adoption Papers" + W + " = 2x Paper + Ink Sac",
          G + "Sewer Grate" + W + " = 2x Iron + Iron Bars",
          G + "Krave Video 1" + W + " = Paper + Krave + Redstone",
          G + "\"Cocaine\" x3" + W + " = Snowball + Paper" }
    };

    private int page = 0;

    public RecipeBookScreen() {
        super(Component.literal("Barbara's Cookbook"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("< Prev"), b -> {
            if (this.page > 0) {
                this.page--;
            }
        }).bounds(this.width / 2 - 80, this.height - 32, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            if (this.page < PAGES.length - 1) {
                this.page++;
            }
        }).bounds(this.width / 2 + 20, this.height - 32, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        int cx = this.width / 2;
        int top = 24;
        String[] lines = PAGES[this.page];
        gfx.fill(cx - 150, top - 12, cx + 150, top + 20 + lines.length * 12, 0xC0101408);

        gfx.drawCenteredString(this.font, ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                + "BARBARA'S COOKBOOK " + ChatFormatting.RESET + ChatFormatting.GRAY
                + "(" + (this.page + 1) + "/" + PAGES.length + ")", cx, top - 6, 0xFFFFFF);

        int y = top + 12;
        for (String line : lines) {
            gfx.drawString(this.font, line, cx - 142, y, 0xFFFFFF);
            y += 12;
        }
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
