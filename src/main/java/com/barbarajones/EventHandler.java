package com.barbarajones;

import com.barbarajones.apocalypse.KraveApocalypse;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

/** Drives the questline, Cayden's arrival, and the Krave Apocalypse. */
public class EventHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String PERSIST = Player.PERSISTED_NBT_TAG;

    /** Hand out the books and spawn Cayden the first time you join. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (Quests.findBook(player) == null) {
            player.getInventory().add(new ItemStack(ModItems.KRAVE_MANUAL.get()));
            player.getInventory().add(new ItemStack(ModItems.QUEST_BOOK.get()));
            player.getInventory().add(new ItemStack(ModItems.RECIPE_BOOK.get()));
            player.getInventory().add(new ItemStack(ModItems.HOUSING_QUERY.get()));
            player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                    + "Read THE KRAVE MANUAL first. Rule #1 is on page one."));
            Quests.onFirstJoin(player);
        }

        CompoundTag persist = persisted(player);
        if (!persist.getBoolean("KraveCaydenSpawned") && player.level() instanceof ServerLevel level) {
            // Never let a cosmetic companion spawn stop a player joining the world:
            // if anything here throws, log it and let the login continue.
            try {
                CaydenCobb cayden = ModEntities.CAYDEN.get().create(level);
                if (cayden != null) {
                    cayden.setPos(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D);
                    cayden.tame(player);
                    level.addFreshEntity(cayden);
                    persist.putBoolean("KraveCaydenSpawned", true);
                    player.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                            + "Cayden Cobb tagged along. Feed him Krave - and build him a house."));
                }
            } catch (Throwable err) {
                LOGGER.error("Failed to spawn Cayden on login; continuing without him", err);
            }
        }
    }

    /** Run the apocalypse cutscene. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            KraveApocalypse.tickAll();
        }
    }

    /** Watch for the player's first box of Krave. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        if (player.tickCount % 40 != 0) {
            return;
        }
        // Sweep the whole graph: any collect quest whose items are now all in the
        // inventory auto-completes, and milestones cascade behind them.
        Quests.tick(player);
    }

    /** Boss and Plug deaths advance the quest; pet deaths trigger the apocalypse. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }

        if (dead instanceof KraveMonster) {
            for (Player player : level.getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(48.0D, 32.0D, 48.0D))) {
                Quests.complete(player, Quests.SLAY_KRAVE);   // the boss falls; ACT II begins
                Quests.complete(player, Quests.REVENGE);      // may already be avenged if Plug fell first
            }
            return;
        }

        if (dead instanceof ThePlug) {
            if (event.getSource().getEntity() instanceof Player player) {
                Quests.complete(player, Quests.REVENGE);
            }
            return;
        }

        LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity le ? le : null;

        // Only a player's OWN pet dying NEAR them triggers the apocalypse.
        if (dead instanceof CaydenCobb cayden) {
            Player owner = cayden.getOwner() instanceof Player p ? p : null;
            if (cayden.isTame() && ownerIsNear(owner, cayden)
                    && !KraveApocalypse.isActiveNear(level, cayden.position())) {
                int stage = nextDeathStage(owner);
                KraveApocalypse.start(level, cayden.position(), killer, owner,
                        true, cayden.getKraveFed(), cayden.isRageUnlocked(), stage, isEndless(owner));
            }
        } else if (dead instanceof BarbaraJones barbara) {
            Player owner = barbara.getPetOwner();
            if (barbara.isPet() && ownerIsNear(owner, barbara)
                    && !KraveApocalypse.isActiveNear(level, barbara.position())) {
                int stage = nextDeathStage(owner);
                KraveApocalypse.start(level, barbara.position(), killer, owner,
                        false, 0, false, stage, isEndless(owner));
            }
        }
    }

    /** Whether THIS player has crossed the 11-death threshold (see nextDeathStage). */
    private boolean isEndless(@Nullable Player owner) {
        return owner != null && persisted(owner).getBoolean("KraveEndless");
    }

    /**
     * Increment the death count. Deaths 1-10 walk the stages in order. From the
     * ELEVENTH death on, the world simply never stops ending: the stage locks at
     * 10 and the apocalypse re-arms itself forever (see ENDLESS below).
     */
    private int nextDeathStage(@Nullable Player owner) {
        if (owner == null) {
            return 1;
        }
        CompoundTag persist = persisted(owner);
        int count = persist.getInt("KraveDeathStage") + 1;
        persist.putInt("KraveDeathStage", count);

        if (count >= 11) {
            if (!persist.getBoolean("KraveEndless")) {
                persist.putBoolean("KraveEndless", true);
                owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED + ""
                        + ChatFormatting.BOLD + "ELEVEN. THERE IS NO STAGE ELEVEN."));
                owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED
                        + "It does not stop now. It never stops now."));
            }
            return 10;
        }
        return count;
    }

    private boolean ownerIsNear(@Nullable Player owner, Entity pet) {
        return owner != null && owner.isAlive() && owner.distanceToSqr(pet) < 48.0D * 48.0D;
    }

    private CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSIST)) {
            data.put(PERSIST, new CompoundTag());
        }
        return data.getCompound(PERSIST);
    }
}
