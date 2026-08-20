package com.barbarajones.v2.bonds;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * ONE OF EACH, ENFORCED.
 *
 * <p>Two jobs, both against {@link CanonicalRegistry}:
 * <ol>
 *   <li><b>Prevent duplicates.</b> {@link #onJoin} fires for every entity
 *       joining a level, including a spawn egg, {@code /summon}, another
 *       player's login-spawn (see {@code EventHandler.onLogin} - it spawns a
 *       personal Cayden per player, which this narrows to "the first player
 *       to log into the world gets the canonical one"), or the quest/apocalypse
 *       spawn sites in {@code QuestExpansionEvents}/{@code KraveApocalypse}.
 *       If a live canonical one is already registered, the newcomer is
 *       cancelled before it ever ticks. {@code BredCaydenCobb} is a distinct
 *       class and is never touched by this check - bred Caydens are exempt by
 *       construction, not by a flag.</li>
 *   <li><b>Restore if lost.</b> {@link #onLevelTick} snapshots the canonical
 *       pair's position and Krave progress every few seconds while they are
 *       alive, and if one goes missing (dead, {@code /kill}ed, or otherwise
 *       removed) and stays missing for a full {@link #RESTORE_GRACE_TICKS}, it
 *       is rebuilt from that snapshot. The long grace period is deliberate: it
 *       gives {@code KraveApocalypse}'s own, much faster respawn cycle every
 *       chance to handle a normal death first. This is a safety net under
 *       that system, not a replacement for it.</li>
 * </ol>
 *
 * <p>Only watches the Overworld - both companions' "home" dimension already
 * means something to {@code CaydenCobb} (see {@code homeIsInThisDimension}),
 * and a canonical pair split across a portal is a false positive, not a loss.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CanonicalGuardEvents {

    /** How often the alive-and-well pair get snapshotted, in ticks. */
    private static final int SNAPSHOT_INTERVAL = 100;
    /** How long one has to be missing, in ticks, before this rebuilds it. ~2 minutes. */
    private static final int RESTORE_GRACE_TICKS = 20 * 120;

    private CanonicalGuardEvents() { }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();

        // getClass() == X.class, not instanceof: BredCaydenCobb extends
        // CaydenCobb specifically so it slips past this exact-type check.
        if (entity.getClass() == CaydenCobb.class) {
            if (!admit(level, CanonicalRegistry.get(level).caydenId(), entity)) {
                event.setCanceled(true);
                return;
            }
            CanonicalRegistry.get(level).setCayden(entity.getUUID());
        } else if (entity.getClass() == BarbaraJones.class) {
            if (!admit(level, CanonicalRegistry.get(level).barbaraId(), entity)) {
                event.setCanceled(true);
                return;
            }
            CanonicalRegistry.get(level).setBarbara(entity.getUUID());
        }
    }

    /** True if this entity may join: no canonical one recorded yet, it IS the recorded one, or the recorded one is gone. */
    private static boolean admit(ServerLevel level, UUID recorded, Entity joining) {
        if (recorded == null || recorded.equals(joining.getUUID())) {
            return true;
        }
        Entity current = level.getEntity(recorded);
        return current == null || !current.isAlive();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()
                || event.level.dimension() != Level.OVERWORLD
                || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % SNAPSHOT_INTERVAL != 0) {
            return;
        }

        CanonicalRegistry reg = CanonicalRegistry.get(level);
        guardCayden(level, reg);
        guardBarbara(level, reg);
    }

    private static void guardCayden(ServerLevel level, CanonicalRegistry reg) {
        UUID id = reg.caydenId();
        if (id == null) {
            return;
        }
        Entity e = level.getEntity(id);
        if (e instanceof CaydenCobb cayden && cayden.getClass() == CaydenCobb.class && cayden.isAlive()) {
            reg.snapshotCayden(cayden.getKraveFed(), cayden.isRageUnlocked(), cayden.getKi(),
                    cayden.getUnlockMask(), cayden.position());
            reg.clearCaydenMissing();
            return;
        }
        if (reg.incCaydenMissing(SNAPSHOT_INTERVAL) >= RESTORE_GRACE_TICKS) {
            restoreCayden(level, reg);
        }
    }

    private static void guardBarbara(ServerLevel level, CanonicalRegistry reg) {
        UUID id = reg.barbaraId();
        if (id == null) {
            return;
        }
        Entity e = level.getEntity(id);
        if (e instanceof BarbaraJones barbara && barbara.isAlive()) {
            reg.snapshotBarbara(barbara.position());
            reg.clearBarbaraMissing();
            return;
        }
        if (reg.incBarbaraMissing(SNAPSHOT_INTERVAL) >= RESTORE_GRACE_TICKS) {
            restoreBarbara(level, reg);
        }
    }

    /**
     * Last-resort rebuild. Everything here is public API on {@code CaydenCobb}
     * - {@code restoreKrave}, the public {@code addKi}, and the public
     * {@code tryUnlock} - so this never reaches into a private field. Ki is
     * topped up by exactly the cost of every rung being re-taught (computed
     * from {@code AscensionLadder.rung(tier).kiCost()}, itself public) plus the
     * snapshot's own leftover balance, so the final Ki total lands on the
     * snapshot value instead of drifting.
     */
    private static void restoreCayden(ServerLevel level, CanonicalRegistry reg) {
        CaydenCobb cayden = ModEntities.CAYDEN.get().create(level);
        if (cayden == null) {
            return;
        }
        Vec3 at = reg.caydenSnapshotPos(level);
        cayden.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        level.addFreshEntity(cayden);
        cayden.restoreKrave(reg.caydenFedSnapshot(), reg.caydenRageSnapshot());
        cayden.grantGrace(CaydenCobb.GRACE_TICKS);

        int mask = reg.caydenUnlockMaskSnapshot();
        int totalCost = 0;
        for (int tier = AscensionLadder.SSJ; tier <= AscensionLadder.MAX; tier++) {
            if (AscensionLadder.unlocked(mask, tier)) {
                totalCost += AscensionLadder.rung(tier).kiCost();
            }
        }
        cayden.addKi(totalCost + reg.caydenKiSnapshot());
        for (int tier = AscensionLadder.SSJ; tier <= AscensionLadder.MAX; tier++) {
            if (AscensionLadder.unlocked(mask, tier)) {
                cayden.tryUnlock(tier, null);
            }
        }

        reg.setCayden(cayden.getUUID());
        reg.clearCaydenMissing();
        announce(level, ChatFormatting.AQUA + "Cayden Cobb turns up again like nothing happened. "
                + "(the canonical registry would like you to know it was worried)");
    }

    private static void restoreBarbara(ServerLevel level, CanonicalRegistry reg) {
        BarbaraJones barbara = ModEntities.BARBARA.get().create(level);
        if (barbara == null) {
            return;
        }
        Vec3 at = reg.barbaraSnapshotPos(level);
        barbara.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        level.addFreshEntity(barbara);
        barbara.grantGrace(BarbaraJones.GRACE_TICKS);
        barbara.addGrassStash(BarbaraJones.getStashCapacity() / 2);

        reg.setBarbara(barbara.getUUID());
        reg.clearBarbaraMissing();
        announce(level, ChatFormatting.AQUA + "Barbara Jones reappears, already lighting one up. "
                + "Nobody asks where she was.");
    }

    private static void announce(ServerLevel level, String message) {
        for (Player p : level.players()) {
            p.sendSystemMessage(Component.literal(message));
        }
    }
}
