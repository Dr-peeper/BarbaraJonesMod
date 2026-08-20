package com.barbarajones.v2.quests;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Where the world tells the quest system what happened.
 *
 * <p>Every task type is driven by a real event except the handful that genuinely
 * cannot be (how much of an item are you carrying, what level are you, does Cayden
 * have a roof) - and those are sampled with idempotent {@code max} writes on a slow
 * timer, never with a cumulative counter on a poll.
 *
 * <p>The old system polled the entire inventory against the entire quest list every
 * two seconds and treated the result as truth. That is why quests un-completed
 * themselves, and why a completion could arrive up to two seconds after the thing
 * that earned it. Here a kill, a craft, a block placement and a dimension change all
 * register on the tick they happen.
 *
 * <p>Registers itself. No shared file is touched to switch this on.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class QuestEvents {

    /** Sampling cadence for the state that has to be looked at rather than listened for. */
    private static final int SAMPLE_TICKS = 20;
    /**
     * Offset so this sweep never lands on the same tick as another module's. Several
     * subsystems in this mod run on a 20/40-tick cadence; sharing tick 0 with all of
     * them turns a quiet second into a spike.
     */
    private static final int SAMPLE_PHASE = 7;

    private QuestEvents() {
    }

    // ---- datapack lifecycle -------------------------------------------------

    /** Hook the quest JSON loader into the datapack pipeline. */
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestLoader());
    }

    /**
     * Fired on login and after every {@code /reload}. Push the whole graph and the
     * player's whole state, so a client can never be left rendering a quest book from
     * a datapack version the server no longer has.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            QuestEngine.refresh(event.getPlayer());
            return;
        }
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            QuestEngine.refresh(player);
        }
    }

    // ---- player lifecycle ---------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerQuests data = PlayerQuests.of(player);
        if (data.needsBootstrap()) {
            data.markBootstrapped();
            ItemStack atlas = new ItemStack(QuestRegistry.QUEST_ATLAS.get());
            if (!player.getInventory().add(atlas)) {
                player.drop(atlas, false);
            }
        }
        QuestEngine.refresh(player);
    }

    /**
     * Respawn hands us a brand new ServerPlayer object. Forge copies the persistent
     * NBT bag across, so the data survives; the client mirror does not, so re-send it.
     */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestEngine.refresh(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        QuestEngine.onEnterDimension(player, event.getTo().location());
        QuestEngine.sendFull(player);
    }

    // ---- observations -------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(dead.getType());
        if (type == null) {
            return;
        }
        // Credit goes to whoever landed the blow; failing that, to whoever last hurt it,
        // so a boss finished off by a summon or by Barbara still counts for the player.
        Player killer = null;
        if (event.getSource().getEntity() instanceof Player direct) {
            killer = direct;
        } else if (dead.getKillCredit() instanceof Player credited) {
            killer = credited;
        }
        if (killer instanceof ServerPlayer server) {
            QuestEngine.onKill(server, type);
        }
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(result.getItem());
        if (id != null) {
            QuestEngine.onCraft(player, id, result.getCount());
        }
    }

    /** A furnace output is a craft as far as a player is concerned. */
    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack result = event.getSmelting();
        if (result.isEmpty()) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(result.getItem());
        if (id != null) {
            QuestEngine.onCraft(player, id, result.getCount());
        }
    }

    /**
     * Picking something up changes the bag, so re-sample right away. This is a
     * latency optimisation only - the timer would catch it within a second anyway,
     * and because the underlying write is a high-water mark, doing both cannot
     * double count.
     */
    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestEngine.sampleInventory(player);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide()) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) {
            return;
        }
        // Only bother the engine for blocks something actually cares about: every
        // dirt block a player places would otherwise walk the whole task index.
        if (VillageState.isTracked(id) || QuestEngine.isTrackedBuilding(id)) {
            QuestEngine.onPlaceBlock(player, id);
        }
    }

    // ---- the sampled half ---------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.tickCount % SAMPLE_TICKS != SAMPLE_PHASE) {
            return;
        }
        if (QuestLoader.file().isEmpty()) {
            return;
        }
        QuestEngine.sample(player);
    }
}
