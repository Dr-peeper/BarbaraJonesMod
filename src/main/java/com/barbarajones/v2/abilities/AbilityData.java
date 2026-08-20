package com.barbarajones.v2.abilities;

import com.barbarajones.v2.abilities.item.AbilityItem;
import com.barbarajones.v2.abilities.net.AbilityNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player unlocks, cooldowns and active-effect windows for every ability
 * in {@link AbilityId}.
 *
 * <p>Stored exactly the way {@code KraveLevel} and {@code PlayerStats} store
 * everything else: a plain sub-tag inside the player's persistent NBT
 * ({@link Player#PERSISTED_NBT_TAG}), which Forge copies across death and
 * respawn on its own. No capability, no extra lifecycle to wire up.
 *
 * <p>The unlock mask lives here, granted by {@code AbilityEvents} off the
 * base questline's own quest ids and the two boss kills it names (see
 * {@link AbilityUnlocks}) - the same conditions the in-game manual's own
 * Player Abilities chapter already documents, which this module matches
 * rather than reinvents. {@link AbilityId#GODCORE} additionally recognizes a
 * grant from the newer quest module directly ({@code v2.quests.QuestApi.
 * hasAbility(player, "i_krave_the_krave")}, already the reward on that
 * module's own {@code bosses/peace} finale) as an alternate, no-code-needed
 * path to the same unlock - see {@code AbilityEvents#watchUnlocks}.
 *
 * <p>Cooldown and active-window fields are stored as the absolute
 * {@code level.getGameTime()} tick they end on, not a countdown. That is what
 * lets the HUD (client-side, on a totally different clock) draw an accurate
 * bar off one synced number instead of needing a tick-perfect decrement every
 * frame.
 */
public final class AbilityData {

    private static final String ROOT = "KraveAbilities";
    private static final String KEY_MASK = "UnlockMask";
    private static final String KEY_CD = "CdEnd";     // + index
    private static final String KEY_ACT = "ActEnd";   // + index

    private AbilityData() { }

    // ---- storage --------------------------------------------------------

    private static CompoundTag data(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    // ---- unlocks ----------------------------------------------------------

    public static int unlockMask(Player player) {
        return data(player).getInt(KEY_MASK);
    }

    public static boolean isUnlocked(Player player, AbilityId id) {
        return (unlockMask(player) & (1 << id.index)) != 0;
    }

    /**
     * Grant an ability. Idempotent - granting something already unlocked is a
     * silent no-op, so both the automatic watcher in {@code AbilityEvents}
     * and a future direct call (a quest reward, a command) can hit this
     * without checking first. Announces itself, plays a sound, and re-syncs
     * the HUD; that is the entire contract anything granting an ability needs
     * to lean on.
     */
    public static void grant(ServerPlayer player, AbilityId id) {
        if (player.level().isClientSide || isUnlocked(player, id)) {
            return;
        }
        CompoundTag tag = data(player);
        tag.putInt(KEY_MASK, tag.getInt(KEY_MASK) | (1 << id.index));
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                + "POWER UNLOCKED: " + ChatFormatting.RESET + ChatFormatting.GOLD + id.displayName));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player);
    }

    // ---- cooldowns ----------------------------------------------------------

    public static long cooldownEnd(Player player, AbilityId id) {
        return data(player).getLong(KEY_CD + id.index);
    }

    public static boolean onCooldown(Player player, AbilityId id, long now) {
        return now < cooldownEnd(player, id);
    }

    private static void startCooldown(ServerPlayer player, AbilityId id, long now) {
        data(player).putLong(KEY_CD + id.index, now + id.cooldownTicks);
    }

    // ---- active windows (Charm's flight, Band's dodge, God Core's aura) -----

    public static long activeEnd(Player player, AbilityId id) {
        return data(player).getLong(KEY_ACT + id.index);
    }

    public static boolean isActive(Player player, AbilityId id, long now) {
        return now < activeEnd(player, id);
    }

    public static void setActiveEnd(ServerPlayer player, AbilityId id, long endTick) {
        data(player).putLong(KEY_ACT + id.index, endTick);
    }

    // ---- activation ---------------------------------------------------------

    /**
     * The one door every activation path (right-click use, the keybind
     * packet) walks through: checked here so an unlock can never be bypassed
     * and a cooldown can never be re-triggered by a second code path.
     *
     * @return true if the ability actually fired.
     */
    public static boolean activate(ServerPlayer player, AbilityItem item, ItemStack stack) {
        AbilityId id = item.ability();
        if (player.level().isClientSide) {
            return false;
        }
        if (!isUnlocked(player, id)) {
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "You have not earned the " + id.displayName + " yet. "
                    + ChatFormatting.GRAY + AbilityUnlocks.hint(id)));
            return false;
        }
        long now = player.level().getGameTime();
        if (onCooldown(player, id, now)) {
            long secs = (cooldownEnd(player, id) - now) / 20L + 1L;
            player.displayClientMessage(Component.literal(ChatFormatting.YELLOW
                    + id.displayName + " recharging: " + secs + "s"), true);
            return false;
        }
        item.activate(player, stack);
        startCooldown(player, id, now);
        if (id.hasActiveWindow()) {
            setActiveEnd(player, id, now + id.activeTicks);
        }
        sync(player);
        return true;
    }

    // ---- sync -----------------------------------------------------------

    /** Full state snapshot to this player's own client, for the HUD and tooltips. */
    public static void sync(ServerPlayer player) {
        long now = player.level().getGameTime();
        int mask = unlockMask(player);
        long[] cd = new long[AbilityId.COUNT];
        long[] act = new long[AbilityId.COUNT];
        for (AbilityId id : AbilityId.VALUES) {
            cd[id.index] = cooldownEnd(player, id);
            act[id.index] = activeEnd(player, id);
        }
        AbilityNetwork.sendSync(player, now, mask, cd, act);
    }
}
