package com.barbarajones.v2.mayor;

import com.barbarajones.v2.village.Village;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

import java.util.List;

/**
 * What Barbara tells you when you ask how it is going.
 *
 * <h2>Why chat lines and not a screen</h2>
 * The brief was explicit that this must stay readable and must not become a
 * management spreadsheet, and a screen is how it becomes one. A screen has room
 * for everything, so everything ends up on it; six lines of chat has room for
 * exactly the six things that matter, which forces the question "does the player
 * need this number" to be answered honestly for every field.
 *
 * <p>It is also, practically, the option with no client surface at all - no menu
 * type, no packet, no screen registration, nothing to keep in sync with the
 * server's idea of the queue. The report is generated on the server from live
 * state and sent as text, so it can never be stale or wrong.
 *
 * <h2>The six lines</h2>
 * Village and rank; population against the cap; the job in hand and why it is or
 * is not moving; what is still owing on it; what the next rank unlocks; and the
 * money. Anything that does not change a decision the player is about to make is
 * not on the list.
 */
public final class MayorReport {

    private MayorReport() { }

    /**
     * Prints the report to one player and hands over any money owing at the same
     * time - asking for the numbers and collecting the take are one gesture on
     * purpose, so there is no second verb to discover.
     *
     * @param collected how many Dollars were just paid out; zero prints nothing
     */
    public static void send(Player player, Village village, MayorSettlement settlement,
                            int residentCap, boolean raging, int collected) {
        MayorRank rank = settlement.rank();

        player.sendSystemMessage(Component.literal("── " + village.name() + " ──")
                .withStyle(ChatFormatting.GOLD));

        MayorRank next = rank.next();
        MutableComponent rankLine = Component.literal("Mayor: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Barbara Jones, " + rank.title())
                        .withStyle(ChatFormatting.WHITE));
        if (next != null) {
            rankLine.append(Component.literal(
                    "  (" + settlement.cloutToNextRank() + " more jobs' worth to " + next.title() + ")")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(rankLine);

        player.sendSystemMessage(Component.literal("Population: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(village.population() + "/" + residentCap)
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("   Built: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(settlement.completedTotal()
                                + " (" + settlement.roadSegmentsLaid() + " of it road)")
                        .withStyle(ChatFormatting.WHITE)));

        if (raging) {
            player.sendSystemMessage(Component.literal(
                    "She is not doing paperwork while she is like this. Sort her out first.")
                    .withStyle(ChatFormatting.RED));
        }

        sendProjectLines(player, settlement);
        sendUnlockLine(player, rank);

        if (collected > 0) {
            player.sendSystemMessage(Component.literal("Your cut: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("$" + collected).withStyle(ChatFormatting.GREEN)));
        } else if (settlement.completedCount(ProjectKind.PLUG_HEADQUARTERS) > 0
                || settlement.completedCount(ProjectKind.CORNER_STORE) > 0
                || settlement.completedCount(ProjectKind.MARKET_STALL) > 0) {
            player.sendSystemMessage(Component.literal("Your cut: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("nothing in the tin yet.")
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    private static void sendProjectLines(Player player, MayorSettlement settlement) {
        MayorProject head = settlement.head();
        if (head == null) {
            player.sendSystemMessage(Component.literal(
                    "Nothing on the books. Bring her a permit and she'll find somewhere to put it.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        MutableComponent works = Component.literal("On the books: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(head.kind().title()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  (" + statePhrase(head) + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
        BlockPos site = head.site();
        if (site != null && head.state() == MayorProject.State.BUILDING) {
            works.append(Component.literal(
                            "  " + site.getX() + ", " + site.getY() + ", " + site.getZ())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        player.sendSystemMessage(works);

        List<ProjectKind.Material> owing = head.outstandingLines();
        if (!owing.isEmpty()) {
            MutableComponent line = Component.literal("Still wants: ").withStyle(ChatFormatting.GRAY);
            for (int i = 0; i < owing.size(); i++) {
                ProjectKind.Material material = owing.get(i);
                if (i > 0) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
                }
                line.append(Component.literal(head.outstanding(material) + "x ")
                                .withStyle(ChatFormatting.YELLOW))
                        .append(material.displayName().copy().withStyle(ChatFormatting.WHITE));
            }
            player.sendSystemMessage(line);
        }

        int queued = settlement.queue().size();
        if (queued > 1) {
            player.sendSystemMessage(Component.literal(
                    "Also on the list: " + (queued - 1) + " more.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void sendUnlockLine(Player player, MayorRank rank) {
        ProjectKind unlock = nextUnlock(rank);
        if (unlock == null) {
            player.sendSystemMessage(Component.literal(
                    "There is nothing left she does not know how to build.")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        player.sendSystemMessage(Component.literal("Next unlock: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(unlock.title()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" at " + MayorRank.byIndex(unlock.minRank()).title())
                        .withStyle(ChatFormatting.DARK_GRAY)));
    }

    /**
     * The cheapest project she cannot attempt yet.
     *
     * <p>Cheapest by required rank, then by clout, so the answer is the thing
     * that will actually unlock next rather than whichever constant happens to
     * come first in the enum.
     */
    @Nullable
    private static ProjectKind nextUnlock(MayorRank rank) {
        ProjectKind best = null;
        for (ProjectKind kind : ProjectKind.all()) {
            if (kind.minRank() <= rank.index()) {
                continue;
            }
            if (best == null || kind.minRank() < best.minRank()
                    || (kind.minRank() == best.minRank() && kind.clout() < best.clout())) {
                best = kind;
            }
        }
        return best;
    }

    /** One short phrase describing where a project has got to, and why. */
    private static String statePhrase(MayorProject project) {
        switch (project.state()) {
            case BUILDING:
                return "going up now";
            case SITING:
                return project.stall() == MayorProject.Stall.NONE
                        ? "paid for, looking for a plot"
                        : project.stall().phrase();
            case FUNDING:
            default:
                return project.stall().phrase();
        }
    }
}
