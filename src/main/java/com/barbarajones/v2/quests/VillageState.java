package com.barbarajones.v2.quests;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * How far along the player's village is, on a 0-5 scale.
 *
 * <p>There is no settlement module in this codebase yet, and a quest chain that
 * depends on a module which may never land is a quest chain that deadlocks - which
 * is precisely the class of bug this rewrite exists to eliminate. So the tier is
 * computed here, from things the quest module can observe on its own: blocks the
 * player has personally placed (counted through a real block-place event, never a
 * world scan) and whether Cayden actually has a roof.
 *
 * <p>Every block on the ladder was checked against the recipe pack before it was put
 * there. Each one is craftable from ingredients a player can definitely reach:
 * <ul>
 *   <li>shag carpet - brown wool + a handful of grass</li>
 *   <li>stash box - oak planks + a handful of grass</li>
 *   <li>wood panelling - dark oak planks + brown dye</li>
 *   <li>television - iron, glass, redstone</li>
 *   <li>recliner - red wool + oak logs</li>
 *   <li>krafting bench - four Krave Blocks (obsidian + amethyst)</li>
 * </ul>
 * Deliberately NOT on the ladder: the Krave wood door, whose only recipe
 * ({@code krave_tree_door.json}) produces {@code barbarajones:krave_wood_door}, an id
 * that is not registered. Gating a tier on an item with a broken recipe is exactly
 * the old failure in a new coat.
 *
 * <p>When a real settlement module arrives it does not need to replace any of this.
 * It calls {@link QuestApi#reportVillageTier}, and the stored tier becomes the larger
 * of the two. The tier is monotone, so demolishing a wall can never un-earn a quest
 * that has already been paid out - a player tearing down their starter shack to build
 * something better must not be punished for it.
 */
public final class VillageState {

    private VillageState() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BarbaraJonesMod.MODID, path);
    }

    private static final ResourceLocation SHAG_CARPET    = id("shag_carpet");
    private static final ResourceLocation STASH_BOX      = id("stash_box");
    private static final ResourceLocation WOOD_PANELING  = id("wood_paneling");
    private static final ResourceLocation TELEVISION     = id("television");
    private static final ResourceLocation RECLINER       = id("recliner");
    private static final ResourceLocation KRAFTING_BENCH = id("krafting_bench");
    private static final ResourceLocation KRAVE_PLANKS   = id("krave_planks");

    /** Every block whose placement is worth recording. Anything else is ignored. */
    public static final List<ResourceLocation> TRACKED = List.of(
            SHAG_CARPET, STASH_BOX, WOOD_PANELING, TELEVISION,
            RECLINER, KRAFTING_BENCH, KRAVE_PLANKS);

    public static boolean isTracked(ResourceLocation block) {
        return TRACKED.contains(block);
    }

    /**
     * The tier ladder. Each rung is the thing a player would naturally do next, so
     * the village chain reads as a build order rather than as a shopping list.
     *
     * <ol>
     *   <li>a floor - the first room exists</li>
     *   <li>Cayden has a validated room. Rule #1, with a roof on it.</li>
     *   <li>walls and somewhere to keep things</li>
     *   <li>furniture: it looks lived in</li>
     *   <li>a krafting bench: it is a home, not a box</li>
     * </ol>
     */
    static void recompute(ServerPlayer player, QuestEngine.Tx tx) {
        PlayerQuests data = tx.data;
        int tier = 0;

        if (data.placed(SHAG_CARPET) >= 3) {
            tier = 1;
        }
        // The housing check costs an entity query, so only pay for it while the answer
        // can still change something. The tier never drops, so once 2 is banked it is banked.
        if (tier == 1 && (data.villageTier() >= 2 || caydenIsHoused(player))) {
            tier = 2;
        }
        if (tier == 2 && data.placed(STASH_BOX) >= 1 && data.placed(WOOD_PANELING) >= 8) {
            tier = 3;
        }
        if (tier == 3 && data.placed(TELEVISION) >= 1 && data.placed(RECLINER) >= 1) {
            tier = 4;
        }
        if (tier == 4 && data.placed(KRAFTING_BENCH) >= 1) {
            tier = 5;
        }

        if (data.raiseVillageTier(tier)) {
            tx.setsChanged = true;
        }
    }

    /**
     * Cayden's own housed flag, which the housing validator already maintains. Asking
     * him is cheaper and more honest than re-flood-filling a room from here, and it
     * means the two systems can never disagree about whether the kid has a bed.
     */
    private static boolean caydenIsHoused(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(128.0D);
        for (CaydenCobb cayden : player.level().getEntitiesOfClass(CaydenCobb.class, box)) {
            if (player.getUUID().equals(cayden.getOwnerUUID()) && cayden.isHoused()) {
                return true;
            }
        }
        return false;
    }
}
