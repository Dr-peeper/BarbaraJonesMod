package com.barbarajones.v2.quests.client;

import com.barbarajones.v2.quests.Quest;
import com.barbarajones.v2.quests.QuestChapter;
import com.barbarajones.v2.quests.QuestReward;
import com.barbarajones.v2.quests.QuestTask;
import com.barbarajones.v2.quests.net.C2SQuestAction;
import com.barbarajones.v2.quests.net.QuestNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The quest book: a chapter rail, a navigable dependency graph, and a detail panel.
 *
 * <p>Three rules this screen was written to obey, all of them reactions to the old one:
 *
 * <ol>
 *   <li><b>A locked quest is never a blank grey box.</b> It shows its title, its full
 *       objective, every task it will ask for, and an explicit "Unlocked by" list
 *       with a tick or a cross against each prerequisite. A player must always be
 *       able to answer "why can't I do this yet" from the book alone. The old screen
 *       drew locked quests as dark text with nothing behind it, which is
 *       indistinguishable from a bug.</li>
 *   <li><b>Dependencies are drawn, not implied.</b> Edges are real lines between real
 *       nodes, coloured by whether the prerequisite is satisfied, so the shape of the
 *       questline is visible at a glance. Edges leaving the chapter are drawn as a
 *       stub with the target chapter named, rather than silently dropped.</li>
 *   <li><b>Nothing here decides anything.</b> Every status comes from
 *       {@link ClientQuests}, which only holds what the server sent.</li>
 * </ol>
 */
public class QuestTreeScreen extends Screen {

    private static final int RAIL_W = 108;
    private static final int PANEL_W = 186;
    private static final int CELL = 40;
    private static final int NODE = 28;

    private static final int C_BACKDROP   = 0xE8140A12;
    private static final int C_PANEL      = 0xF01B1018;
    private static final int C_CANVAS     = 0xC00E0810;
    private static final int C_BORDER     = 0xFF5A3A2A;
    private static final int C_TEXT       = 0xFFE8DCCC;
    private static final int C_DIM        = 0xFF8A7A6A;

    private static final int C_COMPLETE   = 0xFF2E6B33;
    private static final int C_CLAIMABLE  = 0xFFC79A2E;
    private static final int C_OPEN       = 0xFF7A4A22;
    private static final int C_LOCKED     = 0xFF2A2228;

    private static final int C_EDGE_DONE  = 0xFF4CA85A;
    private static final int C_EDGE_OPEN  = 0xFF6A5348;

    @Nullable
    private ResourceLocation chapterId;
    @Nullable
    private Quest selected;

    private float panX;
    private float panY;
    private float zoom = 1.0F;
    private boolean dragging;

    /** Panel scroll, so a quest with many tasks does not run off the bottom. */
    private int panelScroll;

    public QuestTreeScreen() {
        super(Component.translatable("screen.barbarajones.quests.title"));
    }

    @Override
    protected void init() {
        List<QuestChapter> chapters = ClientQuests.file().orderedChapters();
        if (this.chapterId == null && !chapters.isEmpty()) {
            selectChapter(chapters.get(0).id);
        }
    }

    private void selectChapter(ResourceLocation id) {
        this.chapterId = id;
        this.selected = null;
        this.panelScroll = 0;
        centreOnChapter();
    }

    /** Frame the chapter so the player never opens onto empty space. */
    private void centreOnChapter() {
        List<Quest> quests = currentQuests();
        if (quests.isEmpty()) {
            this.panX = 0;
            this.panY = 0;
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Quest q : quests) {
            minX = Math.min(minX, q.x);
            maxX = Math.max(maxX, q.x);
            minY = Math.min(minY, q.y);
            maxY = Math.max(maxY, q.y);
        }
        this.panX = (minX + maxX) * 0.5F * CELL;
        this.panY = (minY + maxY) * 0.5F * CELL;
        this.zoom = 1.0F;
    }

    private List<Quest> currentQuests() {
        return this.chapterId == null ? List.of() : ClientQuests.file().questsIn(this.chapterId);
    }

    // ---- geometry -----------------------------------------------------------

    private int canvasLeft() {
        return RAIL_W + 4;
    }

    private int canvasRight() {
        return this.width - PANEL_W - 4;
    }

    private int canvasTop() {
        return 26;
    }

    private int canvasBottom() {
        return this.height - 8;
    }

    private float screenX(Quest q) {
        float centre = (canvasLeft() + canvasRight()) * 0.5F;
        return centre + (q.x * CELL - this.panX) * this.zoom;
    }

    private float screenY(Quest q) {
        float centre = (canvasTop() + canvasBottom()) * 0.5F;
        return centre + (q.y * CELL - this.panY) * this.zoom;
    }

    private int nodeSize() {
        return Math.max(10, Math.round(NODE * this.zoom));
    }

    // ---- render -------------------------------------------------------------

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        gfx.fill(0, 0, this.width, this.height, C_BACKDROP);

        renderHeader(gfx);
        renderRail(gfx, mouseX, mouseY);
        renderCanvas(gfx, mouseX, mouseY);
        renderPanel(gfx, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, partial);
    }

    private void renderHeader(GuiGraphics gfx) {
        gfx.fill(0, 0, this.width, 22, C_PANEL);
        gfx.hLine(0, this.width, 22, C_BORDER);
        gfx.drawString(this.font, Component.translatable("screen.barbarajones.quests.title")
                .copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 8, 7, C_TEXT, false);

        Component tally = Component.translatable("screen.barbarajones.quests.tally",
                ClientQuests.completedCount(), ClientQuests.file().size());
        int tallyW = this.font.width(tally);
        gfx.drawString(this.font, tally, this.width - tallyW - 8, 7, C_DIM, false);

        Component tier = Component.translatable("screen.barbarajones.quests.village_tier",
                ClientQuests.villageTier());
        gfx.drawString(this.font, tier, this.width - tallyW - 20 - this.font.width(tier), 7, C_DIM, false);
    }

    private void renderRail(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.fill(0, 22, RAIL_W, this.height, C_PANEL);
        gfx.vLine(RAIL_W, 22, this.height, C_BORDER);

        int y = 30;
        for (QuestChapter chapter : ClientQuests.file().orderedChapters()) {
            boolean active = chapter.id.equals(this.chapterId);
            boolean hover = mouseX >= 2 && mouseX <= RAIL_W - 2 && mouseY >= y && mouseY <= y + 22;
            if (active || hover) {
                gfx.fill(2, y, RAIL_W - 2, y + 22, active ? 0xFF3A2418 : 0xFF261A20);
            }
            gfx.renderItem(chapter.icon, 6, y + 3);
            List<net.minecraft.util.FormattedCharSequence> lines =
                    this.font.split(chapter.title(), RAIL_W - 32);
            int ty = y + (lines.size() > 1 ? 2 : 7);
            for (var line : lines) {
                gfx.drawString(this.font, line, 26, ty, active ? 0xFFFFD79A : C_TEXT, false);
                ty += 10;
                if (ty > y + 20) {
                    break;
                }
            }
            y += 24;
        }

        // Progress through the whole book, so there is always a global sense of place.
        int barY = this.height - 20;
        gfx.fill(6, barY, RAIL_W - 6, barY + 6, 0xFF241A1E);
        int total = Math.max(1, ClientQuests.file().size());
        int filled = Math.round((RAIL_W - 12) * (ClientQuests.completedCount() / (float) total));
        gfx.fill(6, barY, 6 + filled, barY + 6, C_EDGE_DONE);
    }

    private void renderCanvas(GuiGraphics gfx, int mouseX, int mouseY) {
        int left = canvasLeft();
        int right = canvasRight();
        int top = canvasTop();
        int bottom = canvasBottom();
        gfx.fill(left, top, right, bottom, C_CANVAS);

        gfx.enableScissor(left, top, right, bottom);
        List<Quest> quests = currentQuests();

        // Edges first so nodes sit on top of them.
        for (Quest quest : quests) {
            for (ResourceLocation depId : quest.dependencies) {
                Quest dep = ClientQuests.file().quest(depId);
                boolean satisfied = ClientQuests.isComplete(depId);
                int colour = satisfied ? C_EDGE_DONE : C_EDGE_OPEN;
                if (dep == null) {
                    continue;
                }
                if (!dep.chapter.equals(quest.chapter)) {
                    drawOffChapterStub(gfx, quest, dep, colour);
                    continue;
                }
                drawLine(gfx, screenX(dep), screenY(dep), screenX(quest), screenY(quest), colour);
            }
        }

        Quest hovered = null;
        for (Quest quest : quests) {
            if (drawNode(gfx, quest, mouseX, mouseY)) {
                hovered = quest;
            }
        }
        gfx.disableScissor();

        if (quests.isEmpty()) {
            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.barbarajones.quests.empty_chapter"),
                    (left + right) / 2, (top + bottom) / 2, C_DIM);
        }

        if (hovered != null) {
            gfx.renderTooltip(this.font, buildHoverLines(hovered), java.util.Optional.empty(),
                    mouseX, mouseY);
        }
    }

    /**
     * An edge whose other end lives in another chapter. Drawn as a short stub with
     * the chapter named rather than dropped: a node with an invisible prerequisite is
     * exactly the "why is this locked" dead end this screen exists to prevent.
     */
    private void drawOffChapterStub(GuiGraphics gfx, Quest quest, Quest dep, int colour) {
        float x = screenX(quest);
        float y = screenY(quest);
        float half = nodeSize() * 0.5F;
        drawLine(gfx, x, y - half - 12, x, y - half, colour);
        QuestChapter other = ClientQuests.file().chapter(dep.chapter);
        Component label = Component.translatable("screen.barbarajones.quests.from_chapter",
                other == null ? Component.literal(dep.chapter.getPath()) : other.title());
        gfx.drawString(this.font, label, (int) (x - this.font.width(label) / 2.0F),
                (int) (y - half - 22), colour, false);
    }

    /**
     * A dependency edge. {@code GuiGraphics.fill} only draws axis-aligned rectangles,
     * so an arbitrary-angle line is stepped out as a run of small quads. The node
     * count is in the dozens, so this is cheap, and it means edges can go anywhere -
     * which in turn means quest layout is free to be a graph rather than a grid of
     * columns.
     */
    private void drawLine(GuiGraphics gfx, float x1, float y1, float x2, float y2, int colour) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 0.5F) {
            return;
        }
        int thickness = this.zoom >= 1.2F ? 2 : 1;
        int steps = Math.min(512, Math.max(2, Math.round(length / 2.0F)));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int px = Math.round(x1 + dx * t);
            int py = Math.round(y1 + dy * t);
            gfx.fill(px, py, px + thickness, py + thickness, colour);
        }
    }

    /** @return true if the mouse is over this node */
    private boolean drawNode(GuiGraphics gfx, Quest quest, int mouseX, int mouseY) {
        int size = nodeSize();
        int half = size / 2;
        int x = Math.round(screenX(quest)) - half;
        int y = Math.round(screenY(quest)) - half;

        ClientQuests.Status status = ClientQuests.status(quest);
        int fill = switch (status) {
            case COMPLETE -> C_COMPLETE;
            case CLAIMABLE -> C_CLAIMABLE;
            case OPEN -> C_OPEN;
            case LOCKED -> C_LOCKED;
        };

        boolean isSelected = this.selected != null && this.selected.id.equals(quest.id);
        boolean hover = mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size
                && mouseX >= canvasLeft() && mouseX < canvasRight()
                && mouseY >= canvasTop() && mouseY < canvasBottom();

        gfx.fill(x - 1, y - 1, x + size + 1, y + size + 1,
                isSelected ? 0xFFFFE08A : hover ? 0xFFB08A5A : C_BORDER);
        gfx.fill(x, y, x + size, y + size, fill);

        // Progress bar across the bottom of the node - visible without opening anything.
        float fraction = ClientQuests.completionFraction(quest);
        if (fraction > 0.0F && status != ClientQuests.Status.COMPLETE) {
            int barW = Math.round((size - 4) * Mth.clamp(fraction, 0.0F, 1.0F));
            gfx.fill(x + 2, y + size - 4, x + 2 + barW, y + size - 2, 0xFF8FD98F);
        }

        if (this.zoom >= 0.65F) {
            gfx.renderItem(quest.icon, x + (size - 16) / 2, y + (size - 16) / 2 - 1);
        }
        if (status == ClientQuests.Status.LOCKED) {
            // A dim wash rather than a blank: the icon still reads, so the player can
            // see what the quest is about before they can do it.
            gfx.fill(x, y, x + size, y + size, 0x99120C10);
        }

        if (this.zoom >= 0.9F) {
            Component title = quest.title();
            int tw = this.font.width(title);
            gfx.drawString(this.font, title, x + half - tw / 2, y + size + 2,
                    status == ClientQuests.Status.LOCKED ? C_DIM : C_TEXT, false);
        }
        return hover;
    }

    private List<Component> buildHoverLines(Quest quest) {
        List<Component> lines = new ArrayList<>();
        lines.add(quest.title().copy().withStyle(ChatFormatting.YELLOW));
        lines.add(statusLine(quest));
        lines.add(Component.translatable("screen.barbarajones.quests.click_for_detail")
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    private Component statusLine(Quest quest) {
        return switch (ClientQuests.status(quest)) {
            case COMPLETE -> Component.translatable("screen.barbarajones.quests.status.complete")
                    .withStyle(ChatFormatting.GREEN);
            case CLAIMABLE -> Component.translatable("screen.barbarajones.quests.status.claimable")
                    .withStyle(ChatFormatting.GOLD);
            case OPEN -> Component.translatable("screen.barbarajones.quests.status.open")
                    .withStyle(ChatFormatting.AQUA);
            case LOCKED -> Component.translatable("screen.barbarajones.quests.status.locked")
                    .withStyle(ChatFormatting.RED);
        };
    }

    // ---- the detail panel ---------------------------------------------------

    private int panelLeft() {
        return this.width - PANEL_W;
    }

    private void renderPanel(GuiGraphics gfx, int mouseX, int mouseY) {
        int left = panelLeft();
        gfx.fill(left, 22, this.width, this.height, C_PANEL);
        gfx.vLine(left, 22, this.height, C_BORDER);

        if (this.selected == null) {
            renderChapterBlurb(gfx, left);
            return;
        }

        Quest quest = this.selected;
        int x = left + 8;
        int width = PANEL_W - 16;
        int y = 30 - this.panelScroll;

        gfx.enableScissor(left + 1, 24, this.width, this.height);

        gfx.renderItem(quest.icon, x, y);
        for (var line : this.font.split(quest.title().copy()
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), width - 22)) {
            gfx.drawString(this.font, line, x + 20, y + 4, C_TEXT, false);
            y += 10;
        }
        y = Math.max(y, 30 - this.panelScroll + 18);
        y += 4;

        gfx.drawString(this.font, statusLine(quest), x, y, C_TEXT, false);
        y += 12;

        // The objective. Always drawn, for every status. This is the whole point.
        for (var line : this.font.split(quest.objective().copy()
                .withStyle(ChatFormatting.WHITE), width)) {
            gfx.drawString(this.font, line, x, y, C_TEXT, false);
            y += 10;
        }
        y += 4;

        Component lore = quest.lore();
        if (lore != null) {
            for (var line : this.font.split(lore.copy().withStyle(ChatFormatting.DARK_GRAY,
                    ChatFormatting.ITALIC), width)) {
                gfx.drawString(this.font, line, x, y, C_DIM, false);
                y += 9;
            }
            y += 4;
        }

        // Tasks, with live counters.
        if (!quest.tasks.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.barbarajones.quests.objectives")
                    .copy().withStyle(ChatFormatting.GOLD), x, y, C_TEXT, false);
            y += 11;
            for (int i = 0; i < quest.tasks.size(); i++) {
                QuestTask task = quest.tasks.get(i);
                int have = ClientQuests.progress(quest, i);
                boolean done = have >= task.target;
                Component text = Component.literal(done ? "[x] " : "[ ] ")
                        .append(task.describe())
                        .append(Component.literal("  " + Math.min(have, task.target) + "/" + task.target))
                        .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY);
                for (var line : this.font.split(text, width - 4)) {
                    gfx.drawString(this.font, line, x + 2, y, done ? 0xFF8FD98F : C_TEXT, false);
                    y += 10;
                }
                int barW = Math.round((width - 8) * Mth.clamp(have / (float) task.target, 0.0F, 1.0F));
                gfx.fill(x + 4, y, x + width - 4, y + 2, 0xFF251B21);
                gfx.fill(x + 4, y, x + 4 + barW, y + 2, done ? C_EDGE_DONE : C_CLAIMABLE);
                y += 6;
            }
            y += 4;
        }


        // How do I actually make this? The tasks name an item; without the grid
        // the player has to leave the game to find out, which is exactly the
        // complaint the old book earned.
        for (ResourceLocation craftable : QuestRecipes.craftables(quest)) {
            QuestRecipes.Grid grid = QuestRecipes.forItem(craftable);
            if (grid == null) {
                continue;   // not craftable - a drop or a reward, nothing to show
            }
            gfx.drawString(this.font, Component.translatable(
                            grid.shapeless() ? "screen.barbarajones.quests.recipe_shapeless"
                                             : "screen.barbarajones.quests.recipe",
                            grid.result().getHoverName())
                    .copy().withStyle(ChatFormatting.GOLD), x, y, C_TEXT, false);
            y += 12;
            y = renderGrid(gfx, grid, x + 2, y, mouseX, mouseY);
            y += 6;
        }

        // Why is this shut? Named, with a tick against the ones already done.
        if (ClientQuests.status(quest) == ClientQuests.Status.LOCKED) {
            Component header = quest.minDependencies < quest.dependencies.size()
                    ? Component.translatable("screen.barbarajones.quests.unlocked_by_any",
                            quest.minDependencies, quest.dependencies.size())
                    : Component.translatable("screen.barbarajones.quests.unlocked_by");
            gfx.drawString(this.font, header.copy().withStyle(ChatFormatting.RED), x, y, C_TEXT, false);
            y += 11;
            for (ResourceLocation depId : quest.dependencies) {
                Quest dep = ClientQuests.file().quest(depId);
                boolean done = ClientQuests.isComplete(depId);
                Component name = dep == null ? Component.literal(depId.toString()) : dep.title();
                Component row = Component.literal(done ? "  [x] " : "  [ ] ").append(name)
                        .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.RED);
                for (var line : this.font.split(row, width - 4)) {
                    gfx.drawString(this.font, line, x, y, done ? 0xFF8FD98F : 0xFFD98F8F, false);
                    y += 10;
                }
                if (dep != null && !done) {
                    // Not just the name - what that prerequisite actually wants.
                    for (var line : this.font.split(dep.objective().copy()
                            .withStyle(ChatFormatting.DARK_GRAY), width - 14)) {
                        gfx.drawString(this.font, line, x + 12, y, C_DIM, false);
                        y += 9;
                    }
                }
            }
            y += 4;
        }

        // Rewards.
        if (!quest.rewards.isEmpty()) {
            gfx.drawString(this.font, Component.translatable("screen.barbarajones.quests.rewards")
                    .copy().withStyle(ChatFormatting.GOLD), x, y, C_TEXT, false);
            y += 11;
            for (QuestReward reward : quest.rewards) {
                for (var line : this.font.split(reward.describe(), width - 4)) {
                    gfx.drawString(this.font, line, x + 2, y, 0xFFE8C87A, false);
                    y += 10;
                }
            }
            y += 4;
        }

        // Buttons. Drawn last so they know where the content ended.
        this.claimButtonY = -1;
        this.deliverButtonY = -1;
        if (ClientQuests.status(quest) == ClientQuests.Status.CLAIMABLE) {
            this.claimButtonY = y;
            drawButton(gfx, x, y, width, Component.translatable("screen.barbarajones.quests.claim"),
                    mouseX, mouseY, C_CLAIMABLE);
            y += 18;
        }
        if (ClientQuests.status(quest) == ClientQuests.Status.OPEN
                && ClientQuests.hasDeliveryOutstanding(quest)) {
            this.deliverButtonY = y;
            drawButton(gfx, x, y, width, Component.translatable("screen.barbarajones.quests.submit"),
                    mouseX, mouseY, C_OPEN);
            y += 18;
        }

        gfx.disableScissor();
        this.panelContentHeight = y + this.panelScroll;
    }

    private int claimButtonY = -1;
    private int deliverButtonY = -1;
    private int panelContentHeight;

    private void drawButton(GuiGraphics gfx, int x, int y, int width, Component label,
                            int mouseX, int mouseY, int colour) {
        boolean hover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 16;
        gfx.fill(x, y, x + width, y + 16, hover ? colour : (colour & 0x00FFFFFF) | 0xC0000000);
        gfx.fill(x + 1, y + 1, x + width - 1, y + 15, hover ? 0x40FFFFFF : 0x20000000);
        int lw = this.font.width(label);
        gfx.drawString(this.font, label, x + (width - lw) / 2, y + 4, 0xFFFFFFFF, false);
    }

    private void renderChapterBlurb(GuiGraphics gfx, int left) {
        QuestChapter chapter = this.chapterId == null ? null : ClientQuests.file().chapter(this.chapterId);
        int x = left + 8;
        int y = 32;
        if (chapter == null) {
            gfx.drawString(this.font, Component.translatable("screen.barbarajones.quests.no_pack")
                    .copy().withStyle(ChatFormatting.RED), x, y, C_TEXT, false);
            return;
        }
        for (var line : this.font.split(chapter.title().copy()
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), PANEL_W - 16)) {
            gfx.drawString(this.font, line, x, y, C_TEXT, false);
            y += 11;
        }
        y += 4;
        for (var line : this.font.split(chapter.description(), PANEL_W - 16)) {
            gfx.drawString(this.font, line, x, y, C_DIM, false);
            y += 10;
        }
        y += 8;
        for (var line : this.font.split(Component.translatable("screen.barbarajones.quests.hint")
                .copy().withStyle(ChatFormatting.DARK_GRAY), PANEL_W - 16)) {
            gfx.drawString(this.font, line, x, y, C_DIM, false);
            y += 10;
        }
    }

    // ---- input --------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX < RAIL_W && mouseY > 22) {
            int y = 30;
            for (QuestChapter chapter : ClientQuests.file().orderedChapters()) {
                if (mouseY >= y && mouseY <= y + 22) {
                    selectChapter(chapter.id);
                    return true;
                }
                y += 24;
            }
            return true;
        }

        if (button == 0 && mouseX >= panelLeft() && this.selected != null) {
            if (this.claimButtonY >= 0 && mouseY >= this.claimButtonY && mouseY <= this.claimButtonY + 16) {
                QuestNetwork.toServer(new C2SQuestAction(C2SQuestAction.Action.CLAIM, this.selected.id));
                return true;
            }
            if (this.deliverButtonY >= 0 && mouseY >= this.deliverButtonY && mouseY <= this.deliverButtonY + 16) {
                QuestNetwork.toServer(new C2SQuestAction(C2SQuestAction.Action.DELIVER, this.selected.id));
                return true;
            }
            return true;
        }

        if (button == 0 && inCanvas(mouseX, mouseY)) {
            for (Quest quest : currentQuests()) {
                int size = nodeSize();
                int half = size / 2;
                int qx = Math.round(screenX(quest)) - half;
                int qy = Math.round(screenY(quest)) - half;
                if (mouseX >= qx && mouseX < qx + size && mouseY >= qy && mouseY < qy + size) {
                    this.selected = quest;
                    this.panelScroll = 0;
                    return true;
                }
            }
            this.selected = null;
            this.dragging = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        return mouseX >= canvasLeft() && mouseX < canvasRight()
                && mouseY >= canvasTop() && mouseY < canvasBottom();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && button == 0) {
            this.panX -= (float) (dragX / this.zoom);
            this.panY -= (float) (dragY / this.zoom);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= panelLeft()) {
            int max = Math.max(0, this.panelContentHeight - this.height + 12);
            this.panelScroll = Mth.clamp(this.panelScroll - (int) (delta * 14), 0, max);
            return true;
        }
        if (inCanvas(mouseX, mouseY)) {
            this.zoom = Mth.clamp(this.zoom + (float) delta * 0.12F, 0.45F, 2.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Draw a 3x3 crafting grid, an arrow, and the result. Returns the y the
     * caller should continue at.
     *
     * <p>Empty trailing rows are skipped: a two-ingredient shapeless recipe draws
     * one row, not three empty ones. The panel is narrow and vertical space is
     * the scarce thing here.
     */
    private int renderGrid(GuiGraphics gfx, QuestRecipes.Grid grid, int x, int y, int mouseX, int mouseY) {
        final int cell = 18;

        int rows = 1;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!grid.cells()[r * 3 + c].isEmpty()) {
                    rows = r + 1;
                }
            }
        }

        ItemStack hovered = ItemStack.EMPTY;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 3; c++) {
                int sx = x + c * cell;
                int sy = y + r * cell;
                gfx.fill(sx, sy, sx + cell - 2, sy + cell - 2, 0xFF251B21);
                gfx.renderOutline(sx, sy, cell - 2, cell - 2, 0xFF3D2E36);
                ItemStack stack = grid.cells()[r * 3 + c];
                if (stack.isEmpty()) {
                    continue;
                }
                gfx.renderItem(stack, sx + 1, sy + 1);
                gfx.renderItemDecorations(this.font, stack, sx + 1, sy + 1);
                if (mouseX >= sx && mouseX < sx + cell - 2 && mouseY >= sy && mouseY < sy + cell - 2) {
                    hovered = stack;
                }
            }
        }

        int midY = y + (rows * cell) / 2 - 4;
        gfx.drawString(this.font, "->", x + 3 * cell + 2, midY, C_TEXT, false);

        int rx = x + 3 * cell + 18;
        gfx.fill(rx, midY - 5, rx + cell - 2, midY + cell - 7, 0xFF251B21);
        gfx.renderOutline(rx, midY - 5, cell - 2, cell - 2, C_EDGE_DONE);
        gfx.renderItem(grid.result(), rx + 1, midY - 4);
        gfx.renderItemDecorations(this.font, grid.result(), rx + 1, midY - 4);
        if (mouseX >= rx && mouseX < rx + cell - 2 && mouseY >= midY - 5 && mouseY < midY + cell - 7) {
            hovered = grid.result();
        }

        // Tooltips last so they sit above every slot drawn this pass.
        if (!hovered.isEmpty()) {
            gfx.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
        return y + rows * cell;
    }
}
