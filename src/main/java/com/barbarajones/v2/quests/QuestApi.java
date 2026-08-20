package com.barbarajones.v2.quests;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The seam other modules talk to. Nothing outside this package should touch
 * {@link QuestEngine} or {@link PlayerQuests} directly.
 *
 * <p>Everything here is null-safe, side-only-safe (a client-side Player is a no-op
 * rather than a crash) and cheap to call from a hot path when there is no matching
 * task loaded. Deliberately so: the previous system's hooks were scattered
 * {@code Quests.complete(player, "some_string_id")} calls in EventHandler, which
 * meant a typo in the id was a silent no-op and every new hook needed a shared file
 * edited.
 *
 * <h2>For the village / building module</h2>
 * Call {@link #reportBuildingPlaced} when a structure is stamped into the world and
 * {@link #reportVillageTier} when your own settlement tier changes. Neither is
 * required for the shipped questline to work - the quest module derives a tier of
 * its own from tracked block placements (see {@link VillageState}) - but if you
 * report a higher tier, that is the one that counts.
 *
 * <h2>For the boss / entity modules</h2>
 * Nothing to do. Boss kills are picked up from {@code LivingDeathEvent} in
 * {@link QuestEvents}; a boss only needs to be a registered entity type named in a
 * quest's {@code defeat_boss} task.
 */
public final class QuestApi {

    private QuestApi() {
    }

    private static boolean serverSide(Player player) {
        return player instanceof ServerPlayer && !player.level().isClientSide;
    }

    // ---- reporting in -------------------------------------------------------

    /**
     * A named building was completed. {@code building} is a block id - the same id a
     * {@code place_building} task names - so a structure placer should report the
     * block that anchors the structure.
     */
    public static void reportBuildingPlaced(Player player, ResourceLocation building) {
        if (serverSide(player)) {
            QuestEngine.onPlaceBlock((ServerPlayer) player, building);
        }
    }

    /** An external settlement system says this player's village is at least this good. */
    public static void reportVillageTier(Player player, int tier) {
        if (!serverSide(player)) {
            return;
        }
        ServerPlayer server = (ServerPlayer) player;
        QuestEngine.Tx tx = QuestEngine.begin(server);
        if (tx.data.raiseVillageTier(tier)) {
            tx.setsChanged = true;
        }
        QuestEngine.commit(tx);
        // Push the new tier into any village_tier task straight away rather than waiting
        // for the next sampling tick.
        QuestEngine.sample(server);
    }

    /**
     * Cayden was fed. The engine also samples his own lifetime counter on a timer, so
     * calling this is an optimisation (instant feedback) rather than a requirement -
     * double-reporting cannot inflate the number, because the underlying write is a
     * high-water mark.
     */
    public static void reportCaydenFed(Player player) {
        if (serverSide(player)) {
            QuestEngine.sample((ServerPlayer) player);
        }
    }

    /** An ability was unlocked outside the quest system. */
    public static void reportAbilityUnlocked(Player player, String ability) {
        if (!serverSide(player)) {
            return;
        }
        ServerPlayer server = (ServerPlayer) player;
        PlayerQuests.of(server).unlockAbility(ability);
        QuestEngine.sample(server);
    }

    /** A boss died and you would rather say so explicitly than rely on the death hook. */
    public static void reportBossDefeated(Player player, ResourceLocation bossType) {
        if (serverSide(player)) {
            QuestEngine.onKill((ServerPlayer) player, bossType);
        }
    }

    // ---- asking ------------------------------------------------------------

    public static boolean isComplete(Player player, ResourceLocation quest) {
        return player != null && PlayerQuests.of(player).isComplete(quest);
    }

    /** True once the quest's dependencies are satisfied, whether or not it is finished. */
    public static boolean isUnlocked(Player player, ResourceLocation questId) {
        Quest quest = QuestLoader.file().quest(questId);
        if (quest == null || player == null) {
            return false;
        }
        if (quest.dependencies.isEmpty()) {
            return true;
        }
        int done = 0;
        PlayerQuests data = PlayerQuests.of(player);
        for (ResourceLocation dep : quest.dependencies) {
            if (data.isComplete(dep)) {
                done++;
            }
        }
        return done >= quest.minDependencies;
    }

    public static boolean hasAbility(Player player, String ability) {
        return player != null && PlayerQuests.of(player).hasAbility(ability);
    }

    public static boolean hasSchematic(Player player, ResourceLocation schematic) {
        return player != null && PlayerQuests.of(player).hasSchematic(schematic);
    }

    public static int villageTier(Player player) {
        return player == null ? 0 : PlayerQuests.of(player).villageTier();
    }

    /** How many quests this player has finished, for HUDs and end-of-run screens. */
    public static int completedCount(Player player) {
        return player == null ? 0 : PlayerQuests.of(player).completed().size();
    }

    public static int totalQuests() {
        return QuestLoader.file().size();
    }
}
