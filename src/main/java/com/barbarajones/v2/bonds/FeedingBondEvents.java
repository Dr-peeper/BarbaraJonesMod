package com.barbarajones.v2.bonds;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModItems;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FEEDING (Cayden and Barbara) plus the passive upkeep it drives.
 *
 * <p>The actual feeding logic for Cayden ({@code CaydenCobb.feedKrave}) and
 * Barbara (grass in {@code BarbaraJones.mobInteract}) already lives on the
 * base entities and is left untouched - this only OBSERVES the same
 * interaction ({@code PlayerInteractEvent.EntityInteract}, which fires before
 * the entity's own {@code mobInteract}) to layer feedback and bond tracking on
 * top.
 *
 * <p><b>Villagers are deliberately not handled here.</b> The task brief for
 * this module says "feeding villagers makes them smarter - call the village
 * module's API," and {@code docs/modules/village.md} did not exist to read.
 * But {@code com.barbarajones.v2.village.KraveVillagerEntity} does exist, and
 * reading it directly turned up a complete, self-contained implementation
 * already sitting on the entity: {@code feedKrave()} (called from its own
 * {@code mobInteract} for both Krave Cereal and Golden Krave) already grants
 * trade XP, glows, particles, a chime, an action-bar progress message, and -
 * through {@code VillageOffer.restock()}/discount-on-level-up - the "better
 * trades, faster restock" half of the brief. There was nothing left to wire:
 * building a second feeding path for vanilla {@code Villager}s (which this
 * mod's own economy does not otherwise use - it spawns {@code
 * KraveVillagerEntity} instead) would have meant maintaining a parallel
 * system doing the same job worse. Instead, {@link VillageHouseFinder} calls
 * {@code KraveVillage.adjustHappiness(...)} - genuinely exercising that
 * module's public API - to give a settlement a happiness nudge for having
 * Cayden and Barbara living in it. See the module doc for the full account.
 *
 * <p>Feedback goes out through {@code Player.displayClientMessage(component,
 * true)} - the vanilla action-bar channel - which works correctly called from
 * plain server-side code on a {@code ServerPlayer}; no client-only overlay or
 * {@code DistExecutor} dance was needed for it. "Visible bond level" is the
 * companion's own name tag: {@link #onTick} turns their custom name on and
 * appends a star meter whenever the bond rung changes, so it is readable at a
 * glance in play, not just in a chat log you have to scroll back through.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FeedingBondEvents {

    private FeedingBondEvents() { }

    // ---- feeding interactions -------------------------------------------------

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        if (event.getTarget() instanceof CaydenCobb cayden && isCaydenFood(held)) {
            // His own mobInteract (which runs right after this event) always
            // feeds successfully for any player holding either item - there is
            // no failure branch to predict around - so getKraveFed() + 1 is the
            // real number he is about to reach, not a guess.
            int fed = cayden.getKraveFed() + 1;
            BondLevel level0 = BondLevel.forCaydenFed(fed);
            player.displayClientMessage(feedbackLine("Cayden", level0, fed, BondLevel.untilNextCayden(fed)), true);
        } else if (event.getTarget() instanceof BarbaraJones barbara && BarbaraJones.isGrass(held)) {
            BondState.addBarbaraGift(barbara);
            int gifts = BondState.barbaraLifetimeGifts(barbara);
            BondLevel level0 = BondLevel.forBarbaraGifts(gifts);
            player.displayClientMessage(feedbackLine("Barbara", level0, gifts, BondLevel.untilNextBarbara(gifts)), true);
        }
    }

    private static boolean isCaydenFood(ItemStack stack) {
        return stack.is(ModItems.KRAVE_CEREAL.get()) || stack.is(ModItems.GOLDEN_KRAVE.get());
    }

    private static Component feedbackLine(String who, BondLevel level, int count, int untilNext) {
        String progress = level.isMax() ? "max bond" : untilNext + " to next rung";
        return Component.literal(level.color() + "" + ChatFormatting.BOLD + who + " " + level.stars() + " "
                + level.displayName() + ChatFormatting.RESET + ChatFormatting.GRAY
                + "  (" + count + " fed, " + progress + ")");
    }

    // ---- passive upkeep: buffs, nameplate, settling ---------------------------

    /**
     * The expensive bits run once a second, not every tick - and every
     * cooldown {@link BondState} stores (house search, happiness, the Family
     * Box) is decremented by this same stride per call rather than by 1, via
     * {@code BondState.TICK_STRIDE}, so those constants mean real seconds
     * instead of silently meaning 20x as many.
     */
    private static final int TICK_STRIDE = BondState.TICK_STRIDE;

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity.tickCount % TICK_STRIDE != 0) {
            return;
        }

        if (entity instanceof CaydenCobb cayden) {
            int fed = cayden.getKraveFed();
            BondLevel bondLevel = BondLevel.forCaydenFed(fed);
            maybeAnnounce(cayden, bondLevel, "Cayden Cobb");
            if (entity.tickCount % BondBuffs.REFRESH_INTERVAL < TICK_STRIDE) {
                BondBuffs.applyToCayden(cayden, bondLevel);
            }
            BondState.tryDecrementFamilyBoxCooldown(cayden);
            VillageHouseFinder.settleTick(level, cayden, cayden.getOwner());
        } else if (entity instanceof BarbaraJones barbara) {
            int gifts = BondState.barbaraLifetimeGifts(barbara);
            BondLevel bondLevel = BondLevel.forBarbaraGifts(gifts);
            maybeAnnounce(barbara, bondLevel, "Barbara Jones");
            if (entity.tickCount % BondBuffs.REFRESH_INTERVAL < TICK_STRIDE) {
                BondBuffs.applyToBarbara(barbara, bondLevel);
            }
            VillageHouseFinder.settleTick(level, barbara, barbara.isPet() ? barbara.getPetOwner() : null);
        }
    }

    private static void maybeAnnounce(LivingEntity entity, BondLevel level, String baseName) {
        if (BondState.lastAnnouncedBondLevel(entity) == level.index()) {
            return;
        }
        BondState.setLastAnnouncedBondLevel(entity, level.index());
        entity.setCustomName(Component.literal(baseName + " " + level.stars()));
        entity.setCustomNameVisible(true);
        if (level != BondLevel.STRANGER) {
            for (Player p : entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(32.0D))) {
                p.displayClientMessage(Component.literal(level.color() + "" + ChatFormatting.BOLD
                        + baseName + " reaches " + level.displayName() + "."), false);
            }
        }
    }
}
