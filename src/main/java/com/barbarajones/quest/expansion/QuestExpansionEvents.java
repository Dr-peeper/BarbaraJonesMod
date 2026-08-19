package com.barbarajones.quest.expansion;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.DuhlWol;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.housing.HousingResult;
import com.barbarajones.housing.HousingValidator;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The engine behind every event quest on the expansion board.
 *
 * <p>{@link QuestExpansion} says what the objectives are; this class is what actually
 * watches the world and calls {@code Quests.complete()} when one of them is met. It
 * registers itself on the Forge bus through {@code @Mod.EventBusSubscriber}, so no
 * shared file has to be touched to switch it on.
 *
 * <p>Three jobs:
 * <ul>
 *   <li><b>The sweep</b> - a throttled server-side player tick that reads live state
 *       (inventory counts, Cayden's stats, dimension, depth, vanilla stat counters)
 *       and grants anything that now qualifies.</li>
 *   <li><b>Interaction hooks</b> - feeding Barbara, tipping Daniel, collaring Nugget,
 *       bribing Mom Cobb, paying Duhl Wol, smoking, and burning fake product.</li>
 *   <li><b>The cast call</b> - the supporting characters have no natural spawns, so a
 *       quest that needs one of them will, once, walk that character out of the treeline
 *       near the player rather than leaving the objective permanently unreachable.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class QuestExpansionEvents {

    private QuestExpansionEvents() { }

    /** Same 2-second cadence EventHandler's own quest sweep uses... */
    private static final int SWEEP_TICKS = 40;
    /** ...but deliberately on a different tick, so the two never land together. */
    private static final int SWEEP_PHASE = 13;
    /** Flood-filling a room is expensive; housing and cast checks run on this cadence. */
    private static final int SLOW_TICKS  = 200;

    private static final double CAST_CLEAR_RADIUS = 96.0D;

    // =====================================================================
    // the sweep
    // =====================================================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        if (player.tickCount % SWEEP_TICKS != SWEEP_PHASE) {
            return;
        }
        ItemStack book = Quests.findBook(player);
        if (book == null) {
            return;                     // no Quest Book, no progress to record
        }
        boolean slow = player.tickCount % SLOW_TICKS == SWEEP_PHASE;
        sweep(player, book, slow);
        if (slow) {
            castCall(player, book);
        }
    }

    private static void sweep(ServerPlayer player, ItemStack book, boolean slow) {
        Level level = player.level();
        CompoundTag tag = QuestProgress.data(player);
        boolean inKosmos = level.dimension().equals(KraveDimensions.KRAVE_KOSMOS);

        meters(player, tag, inKosmos);
        stash(player, book);
        provider(player, book, level, inKosmos, slow);
        kosmos(player, book, inKosmos);
        infamy(player, book);
        domestic(player, book, level);
        grind(player, book);
    }

    /** Time-and-distance counters that only a poll can measure. */
    private static void meters(ServerPlayer player, CompoundTag tag, boolean inKosmos) {
        if (inKosmos) {
            QuestProgress.bump(player, QuestProgress.KOSMOS_TICKS, SWEEP_TICKS);

            int px = Mth.floor(player.getX());
            int pz = Mth.floor(player.getZ());
            if (tag.contains(QuestProgress.KOSMOS_X)) {
                double dx = px - tag.getInt(QuestProgress.KOSMOS_X);
                double dz = pz - tag.getInt(QuestProgress.KOSMOS_Z);
                double step = Math.sqrt(dx * dx + dz * dz);
                // Further than anyone can walk in one sweep means a teleport - and the
                // Krave Monster teleports people constantly. Island hopping is walked.
                if (step < 40.0D) {
                    QuestProgress.bump(player, QuestProgress.KOSMOS_DIST, (int) step);
                }
            }
            tag.putInt(QuestProgress.KOSMOS_X, px);
            tag.putInt(QuestProgress.KOSMOS_Z, pz);
        } else {
            // Drop the anchor on the way out so the portal jump isn't measured as travel.
            tag.remove(QuestProgress.KOSMOS_X);
            tag.remove(QuestProgress.KOSMOS_Z);
            if (player.getY() < 40.0D) {
                QuestProgress.bump(player, QuestProgress.SEWER_TICKS, SWEEP_TICKS);
            }
        }
    }

    // ---- THE STASH ----------------------------------------------------------

    private static void stash(ServerPlayer player, ItemStack book) {
        if (pending(book, QuestExpansion.STASH_HANDFULS)
                && count(player, ModItems.HANDFUL_OF_GRASS.get()) >= QuestExpansion.STASH_HANDFUL_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_HANDFULS);
        }
        if (pending(book, QuestExpansion.STASH_DICED)
                && count(player, ModItems.DICED_GRASS.get()) >= QuestExpansion.STASH_DICED_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_DICED);
        }
        if (pending(book, QuestExpansion.STASH_CURED)
                && count(player, ModItems.BURNT_GRASS.get()) >= QuestExpansion.STASH_BURNT_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_CURED);
        }
        if (pending(book, QuestExpansion.STASH_PAPERS)
                && count(player, ModItems.ROLLING_PAPER.get()) >= QuestExpansion.STASH_PAPER_COUNT
                && count(player, ModItems.ROLLED_JOINT.get()) >= QuestExpansion.STASH_JOINT_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_PAPERS);
        }
        if (pending(book, QuestExpansion.STASH_BROWNIES)
                && count(player, ModItems.GRASS_BROWNIE.get()) >= QuestExpansion.STASH_BROWNIE_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_BROWNIES);
        }
        if (pending(book, QuestExpansion.STASH_TOP_SHELF)
                && count(player, ModItems.GOLDEN_JOINT.get()) >= QuestExpansion.STASH_GOLDEN_COUNT) {
            Quests.complete(player, QuestExpansion.STASH_TOP_SHELF);
        }
        if (pending(book, QuestExpansion.STASH_WITHDRAWAL)) {
            for (BarbaraJones barbara : player.level()
                    .getEntitiesOfClass(BarbaraJones.class, around(player, 24.0D))) {
                if (barbara.isRaging()) {
                    Quests.complete(player, QuestExpansion.STASH_WITHDRAWAL);
                    break;
                }
            }
        }
    }

    // ---- PROVIDER -----------------------------------------------------------

    private static void provider(ServerPlayer player, ItemStack book, Level level,
                                 boolean inKosmos, boolean slow) {
        if (pending(book, QuestExpansion.PROVIDER_PANTRY)
                && count(player, ModItems.KRAVE_CEREAL.get()) >= QuestExpansion.PANTRY_CEREAL
                && count(player, ModItems.KRAVE_MILK.get()) >= QuestExpansion.PANTRY_MILK) {
            Quests.complete(player, QuestExpansion.PROVIDER_PANTRY);
        }

        CaydenCobb cayden = nearestCayden(player, level);
        if (cayden == null || !cayden.isAlive()) {
            return;
        }

        // A day only counts if the kid is still standing at the end of it.
        long day = level.getDayTime() / 24000L;
        int days = QuestProgress.get(player, QuestProgress.CAYDEN_DAYS);
        if (QuestProgress.newDay(player, QuestProgress.CAYDEN_DAY_MARK, day)) {
            days = QuestProgress.bump(player, QuestProgress.CAYDEN_DAYS, 1);
        }

        int fed = cayden.getKraveFed();
        if (pending(book, QuestExpansion.PROVIDER_FIRST_BOWL) && fed >= 1) {
            Quests.complete(player, QuestExpansion.PROVIDER_FIRST_BOWL);
        }
        if (pending(book, QuestExpansion.PROVIDER_TEN) && fed >= QuestExpansion.CAYDEN_FED_TEN) {
            Quests.complete(player, QuestExpansion.PROVIDER_TEN);
        }
        if (pending(book, QuestExpansion.PROVIDER_RAGE)
                && (fed >= QuestExpansion.CAYDEN_FED_RAGE || cayden.isRageUnlocked())) {
            Quests.complete(player, QuestExpansion.PROVIDER_RAGE);
        }
        if (pending(book, QuestExpansion.PROVIDER_ROOF) && cayden.isHoused()) {
            Quests.complete(player, QuestExpansion.PROVIDER_ROOF);
        }
        if (pending(book, QuestExpansion.PROVIDER_FIVE_DAYS)
                && days >= QuestExpansion.PROVIDER_DAYS) {
            Quests.complete(player, QuestExpansion.PROVIDER_FIVE_DAYS);
        }
        if (pending(book, QuestExpansion.PROVIDER_SPOTLESS)
                && days >= QuestExpansion.PROVIDER_DAYS && deathStages(player) == 0) {
            Quests.complete(player, QuestExpansion.PROVIDER_SPOTLESS);
        }
        if (inKosmos && pending(book, QuestExpansion.KOSMOS_ASCENSION) && cayden.isSuperSaiyan()) {
            Quests.complete(player, QuestExpansion.KOSMOS_ASCENSION);
        }

        // Re-validating the room is a flood fill of up to 900 blocks: slow cadence only,
        // and only while one of the two size quests is still outstanding.
        boolean wantsSize = pending(book, QuestExpansion.PROVIDER_UPGRADE)
                || pending(book, QuestExpansion.PROVIDER_MANSION);
        if (slow && wantsSize && cayden.isHoused()) {
            BlockPos home = cayden.getHome();
            if (home != null) {
                HousingResult room = HousingValidator.validate(level, home);
                if (room.valid && room.volume >= QuestExpansion.HOUSE_UPGRADE_AIR) {
                    Quests.complete(player, QuestExpansion.PROVIDER_UPGRADE);
                }
                if (room.valid && room.volume >= QuestExpansion.HOUSE_MANSION_AIR) {
                    Quests.complete(player, QuestExpansion.PROVIDER_MANSION);
                }
            }
        }
    }

    // ---- THE KOSMOS ---------------------------------------------------------

    private static void kosmos(ServerPlayer player, ItemStack book, boolean inKosmos) {
        if (!inKosmos) {
            return;
        }
        if (pending(book, QuestExpansion.KOSMOS_ARRIVAL)) {
            Quests.complete(player, QuestExpansion.KOSMOS_ARRIVAL);
        }
        if (pending(book, QuestExpansion.KOSMOS_HIGH)
                && player.getY() >= QuestExpansion.KOSMOS_CEILING) {
            Quests.complete(player, QuestExpansion.KOSMOS_HIGH);
        }
        if (pending(book, QuestExpansion.KOSMOS_HOP)
                && QuestProgress.get(player, QuestProgress.KOSMOS_DIST) >= QuestExpansion.KOSMOS_TRAVEL) {
            Quests.complete(player, QuestExpansion.KOSMOS_HOP);
        }
        if (pending(book, QuestExpansion.KOSMOS_RESIDENT)
                && QuestProgress.get(player, QuestProgress.KOSMOS_TICKS) >= QuestExpansion.KOSMOS_STAY_TICKS) {
            Quests.complete(player, QuestExpansion.KOSMOS_RESIDENT);
        }
        if (pending(book, QuestExpansion.KOSMOS_ISLAND)) {
            double dx = player.getX() - KraveDimensions.BOSS_ISLAND.x;
            double dz = player.getZ() - KraveDimensions.BOSS_ISLAND.z;
            if (dx * dx + dz * dz <= QuestExpansion.KOSMOS_ISLAND_RANGE * QuestExpansion.KOSMOS_ISLAND_RANGE) {
                Quests.complete(player, QuestExpansion.KOSMOS_ISLAND);
            }
        }
        if (pending(book, QuestExpansion.KOSMOS_CHOCOLATE)
                && player.getFluidTypeHeight(ModFluids.CHOCOLATE_TYPE.get()) > 0.0D) {
            Quests.complete(player, QuestExpansion.KOSMOS_CHOCOLATE);
        }
    }

    // ---- INFAMY -------------------------------------------------------------

    private static void infamy(ServerPlayer player, ItemStack book) {
        if (pending(book, QuestExpansion.INFAMY_RICH)
                && count(player, ModItems.DOLLARS.get()) >= QuestExpansion.INFAMY_DOLLARS) {
            Quests.complete(player, QuestExpansion.INFAMY_RICH);
        }
        if (pending(book, QuestExpansion.INFAMY_CASE)
                && count(player, ModItems.OFF_BRAND_PASTRIES.get()) >= QuestExpansion.INFAMY_PASTRY_CASE) {
            Quests.complete(player, QuestExpansion.INFAMY_CASE);
        }
        if (pending(book, QuestExpansion.INFAMY_SEWER)
                && QuestProgress.get(player, QuestProgress.SEWER_TICKS) >= QuestExpansion.INFAMY_SEWER_TICKS) {
            Quests.complete(player, QuestExpansion.INFAMY_SEWER);
        }
        if (pending(book, QuestExpansion.INFAMY_GRATE)
                && player.getY() < QuestExpansion.INFAMY_GRATE_DEPTH
                && player.getMainHandItem().is(ModItems.SEWER_GRATE.get())) {
            Quests.complete(player, QuestExpansion.INFAMY_GRATE);
        }
    }

    // ---- DOMESTIC -----------------------------------------------------------

    private static void domestic(ServerPlayer player, ItemStack book, Level level) {
        if (pending(book, QuestExpansion.DOMESTIC_NUGGET_MEET)
                || pending(book, QuestExpansion.DOMESTIC_NUGGET_TAME)) {
            for (Nugget nugget : level.getEntitiesOfClass(Nugget.class, around(player, 12.0D))) {
                Quests.complete(player, QuestExpansion.DOMESTIC_NUGGET_MEET);
                if (nugget.isTame() && nugget.isOwnedBy(player)) {
                    Quests.complete(player, QuestExpansion.DOMESTIC_NUGGET_TAME);
                }
            }
        }
        if (pending(book, QuestExpansion.DOMESTIC_MOM_MEET)
                && !level.getEntitiesOfClass(MomCobb.class, around(player, 12.0D)).isEmpty()) {
            Quests.complete(player, QuestExpansion.DOMESTIC_MOM_MEET);
        }
        if (pending(book, QuestExpansion.DOMESTIC_DUHL_MEET)
                && !level.getEntitiesOfClass(DuhlWol.class, around(player, 24.0D)).isEmpty()) {
            Quests.complete(player, QuestExpansion.DOMESTIC_DUHL_MEET);
        }
    }

    // ---- THE GRIND ----------------------------------------------------------

    private static void grind(ServerPlayer player, ItemStack book) {
        int xp = player.experienceLevel;
        if (pending(book, QuestExpansion.GRIND_LEVEL_TEN) && xp >= QuestExpansion.GRIND_LVL_ONE) {
            Quests.complete(player, QuestExpansion.GRIND_LEVEL_TEN);
        }
        if (pending(book, QuestExpansion.GRIND_LEVEL_THIRTY) && xp >= QuestExpansion.GRIND_LVL_TWO) {
            Quests.complete(player, QuestExpansion.GRIND_LEVEL_THIRTY);
        }
        if (pending(book, QuestExpansion.GRIND_LEVEL_FIFTY) && xp >= QuestExpansion.GRIND_LVL_THREE) {
            Quests.complete(player, QuestExpansion.GRIND_LEVEL_FIFTY);
        }
        if (pending(book, QuestExpansion.GRIND_CLEAN) && xp >= QuestExpansion.GRIND_LVL_TWO
                && stat(player, Stats.DEATHS) == 0) {
            Quests.complete(player, QuestExpansion.GRIND_CLEAN);
        }
        if (pending(book, QuestExpansion.GRIND_WALK)
                && stat(player, Stats.WALK_ONE_CM) >= QuestExpansion.GRIND_WALK_CM) {
            Quests.complete(player, QuestExpansion.GRIND_WALK);
        }
        if (pending(book, QuestExpansion.GRIND_JUMP)
                && stat(player, Stats.JUMP) >= QuestExpansion.GRIND_JUMPS) {
            Quests.complete(player, QuestExpansion.GRIND_JUMP);
        }
        if (pending(book, QuestExpansion.GRIND_PAIN)
                && stat(player, Stats.DAMAGE_TAKEN) >= QuestExpansion.GRIND_DAMAGE_TENTHS) {
            Quests.complete(player, QuestExpansion.GRIND_PAIN);
        }
        if (pending(book, QuestExpansion.GRIND_KILLS)
                && stat(player, Stats.MOB_KILLS) >= QuestExpansion.GRIND_MOB_KILLS) {
            Quests.complete(player, QuestExpansion.GRIND_KILLS);
        }
        if (pending(book, QuestExpansion.GRIND_TIME)
                && stat(player, Stats.PLAY_TIME) >= QuestExpansion.GRIND_PLAY_TICKS) {
            Quests.complete(player, QuestExpansion.GRIND_TIME);
        }
        if (pending(book, QuestExpansion.GRIND_HOARDER)
                && modItemVariety(player) >= QuestExpansion.GRIND_VARIETY) {
            Quests.complete(player, QuestExpansion.GRIND_HOARDER);
        }
    }

    /** How many DIFFERENT items from this mod the player is carrying right now. */
    private static int modItemVariety(Player player) {
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && key.getNamespace().equals(BarbaraJonesMod.MODID)) {
                seen.add(stack.getItem());
            }
        }
        return seen.size();
    }

    // =====================================================================
    // the cast call
    // =====================================================================

    /**
     * None of the supporting cast has a natural spawn, so a quest that needs one of
     * them would otherwise sit unlocked forever on a survival world. Once the quest
     * that wants a character is live, that character turns up near the player - once
     * each, only in the Overworld, and only if there isn't one around already.
     */
    private static void castCall(ServerPlayer player, ItemStack book) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;                       // nobody follows you into the Kosmos uninvited
        }
        // Staggered so the entire supporting cast doesn't walk out of the trees at once.
        if (player.getRandom().nextInt(3) != 0) {
            return;
        }

        if (arrive(player, level, book, QuestProgress.CAST_BARBARA, QuestExpansion.STASH_SUPPLIER,
                BarbaraJones.class, () -> ModEntities.BARBARA.get().create(level),
                ChatFormatting.GREEN,
                "A lighter sparks somewhere behind you. Barbara Jones has smelled the grass.")) {
            return;
        }
        if (arrive(player, level, book, QuestProgress.CAST_DANIEL, QuestExpansion.DOMESTIC_DANIEL,
                Daniel.class, () -> ModEntities.DANIEL.get().create(level),
                ChatFormatting.AQUA,
                "Daniel is standing at the treeline, not saying anything. He has the lighter.")) {
            return;
        }
        if (arrive(player, level, book, QuestProgress.CAST_NUGGET, QuestExpansion.DOMESTIC_NUGGET_MEET,
                Nugget.class, () -> ModEntities.NUGGET.get().create(level),
                ChatFormatting.GOLD,
                "A ginger cat strolls out of the grass like she pays rent here. NUGGET!")) {
            return;
        }
        arrive(player, level, book, QuestProgress.CAST_MOM, QuestExpansion.DOMESTIC_MOM_MEET,
                MomCobb.class, () -> ModEntities.MOM.get().create(level),
                ChatFormatting.LIGHT_PURPLE,
                "Mom Cobb is here, and she is asking where Cayden has been sleeping.");
    }

    /**
     * One cast arrival. The entity is built from a supplier so the three characters
     * that are not wanted this pass are never constructed at all. Returns true if the
     * character was actually placed.
     */
    private static boolean arrive(ServerPlayer player, ServerLevel level, ItemStack book,
                                  String flag, String questId, Class<? extends Mob> type,
                                  Supplier<? extends Mob> factory, ChatFormatting colour,
                                  String line) {
        if (QuestProgress.flag(player, flag) || !wants(player, book, questId)) {
            return false;
        }
        if (!level.getEntitiesOfClass(type, around(player, CAST_CLEAR_RADIUS)).isEmpty()) {
            QuestProgress.setFlag(player, flag);   // one is already out there; don't add another
            return false;
        }
        Mob mob = factory.get();
        if (mob == null) {
            return false;
        }
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        double dist = 12.0D + player.getRandom().nextDouble() * 6.0D;
        mob.setPos(player.getX() + Math.cos(angle) * dist, player.getY(),
                player.getZ() + Math.sin(angle) * dist);
        if (!level.addFreshEntity(mob)) {
            return false;
        }
        QuestProgress.setFlag(player, flag);
        say(player, colour, line);
        return true;
    }

    // =====================================================================
    // interaction hooks
    // =====================================================================

    /** Smoking, and eating anything the questline counts. */
    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        ItemStack book = Quests.findBook(player);
        if (book == null) {
            return;
        }
        Item item = event.getItem().getItem();

        if (item == ModItems.ROLLED_JOINT.get() || item == ModItems.GOLDEN_JOINT.get()) {
            int smoked = QuestProgress.bump(player, QuestProgress.JOINTS_SMOKED, 1);
            if (smoked >= QuestExpansion.SMOKE_FIRST) {
                Quests.complete(player, QuestExpansion.STASH_SMOKE_FIVE);
            }
            if (smoked >= QuestExpansion.SMOKE_CHAIN) {
                Quests.complete(player, QuestExpansion.STASH_CHAIN);
            }
        } else if (item == ModItems.KRAVE_CEREAL.get()) {
            if (QuestProgress.bump(player, QuestProgress.CEREAL_EATEN, 1)
                    >= QuestExpansion.GRIND_CEREAL_EATEN) {
                Quests.complete(player, QuestExpansion.GRIND_CEREAL);
            }
        } else if (item == ModItems.MR_PIBB.get()) {
            if (QuestProgress.bump(player, QuestProgress.PIBB_DRUNK, 1)
                    >= QuestExpansion.GRIND_PIBB_DRUNK) {
                Quests.complete(player, QuestExpansion.GRIND_PIBB);
            }
        } else if (item == ModItems.CHICKEN_NUGGETS.get()) {
            if (QuestProgress.bump(player, QuestProgress.NUGGETS_EATEN, 1)
                    >= QuestExpansion.GRIND_NUGGETS_EATEN) {
                Quests.complete(player, QuestExpansion.GRIND_NUGGETS);
            }
        } else if (isFakeProduct(item)) {
            Quests.complete(player, QuestExpansion.INFAMY_TASTE);
        }
    }

    /** Everything you can hand to a member of the cast. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;                        // the off-hand fires a second event; count once
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        ItemStack book = Quests.findBook(player);
        if (book == null) {
            return;
        }
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();

        if (target instanceof BarbaraJones barbara) {
            if (BarbaraJones.isGrass(held)) {
                if (QuestProgress.bump(player, QuestProgress.BARBARA_FED, 1)
                        >= QuestExpansion.BARBARA_FEEDS) {
                    Quests.complete(player, QuestExpansion.STASH_SUPPLIER);
                }
                // Resupplying her mid-episode is the whole quest; she is still raging
                // at this instant because her own interact handler runs after ours.
                if (barbara.isRaging()) {
                    Quests.complete(player, QuestExpansion.STASH_RESUPPLY);
                }
            }
        } else if (target instanceof CaydenCobb cayden) {
            if (held.is(ModItems.KRAVE_CEREAL.get())
                    && cayden.getHealth() < cayden.getMaxHealth() * 0.5F
                    && QuestProgress.bump(player, QuestProgress.CAYDEN_MEDIC, 1)
                            >= QuestExpansion.MEDIC_FEEDS) {
                Quests.complete(player, QuestExpansion.PROVIDER_MEDIC);
            }
        } else if (target instanceof Daniel) {
            if (held.isEmpty()) {
                int lighters = QuestProgress.bump(player, QuestProgress.LIGHTERS, 1);
                Quests.complete(player, QuestExpansion.DOMESTIC_DANIEL);
                if (lighters >= QuestExpansion.DANIEL_LIGHTERS) {
                    Quests.complete(player, QuestExpansion.DOMESTIC_DANIEL_FIVE);
                }
            }
        } else if (target instanceof Nugget nugget) {
            if (held.is(ModItems.NUGGET_COLLAR.get())) {
                collar(player, nugget, held);
            } else if (nugget.isFood(held)
                    && QuestProgress.bump(player, QuestProgress.NUGGET_TREATS, 1)
                            >= QuestExpansion.NUGGET_TREATS) {
                Quests.complete(player, QuestExpansion.DOMESTIC_NUGGET_FED);
            }
        } else if (target instanceof MomCobb mom) {
            if (held.is(ModItems.DONUT_BOX.get())) {
                gift(player, mom, held);
            }
        } else if (target instanceof ThePlug) {
            // ThePlug.mobInteract takes the money and hands back the "product"; all we
            // do is notice that you walked into it a second time.
            if (held.is(ModItems.FIVE_HUNDRED_DOLLARS.get())
                    && QuestProgress.bump(player, QuestProgress.PLUG_DEALS, 1)
                            >= QuestExpansion.INFAMY_PLUG_DEALS) {
                Quests.complete(player, QuestExpansion.INFAMY_REPEAT);
            }
        } else if (target instanceof DuhlWol duhl) {
            ItemStack wanted = duhl.getWantedItem();
            if (!wanted.isEmpty() && held.is(wanted.getItem())) {
                QuestProgress.bump(player, QuestProgress.DUHL_PAYMENTS, 1);
                Quests.complete(player, QuestExpansion.DOMESTIC_DUHL_PAY);
                long day = player.level().getDayTime() / 24000L;
                if (QuestProgress.newDay(player, QuestProgress.DUHL_DAY_MARK, day)
                        && QuestProgress.bump(player, QuestProgress.DUHL_DAYS, 1)
                                >= QuestExpansion.DUHL_TRIBUTE_DAYS) {
                    Quests.complete(player, QuestExpansion.DOMESTIC_DUHL_REG);
                }
            }
        }
    }

    private static void collar(ServerPlayer player, Nugget nugget, ItemStack collar) {
        if (nugget.isTame() && nugget.isOwnedBy(player)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            collar.shrink(1);
        }
        nugget.tame(player);
        nugget.setCustomName(Component.literal("Nugget"));
        nugget.setCustomNameVisible(true);
        nugget.level().playSound(null, nugget.blockPosition(), SoundEvents.CAT_PURR,
                SoundSource.NEUTRAL, 1.0F, 1.0F);
        say(player, ChatFormatting.GOLD, "The collar goes on. Nugget is officially yours - "
                + "as much as any cat is anybody's.");
        Quests.complete(player, QuestExpansion.DOMESTIC_NUGGET_TAME);
    }

    private static void gift(ServerPlayer player, MomCobb mom, ItemStack donuts) {
        if (!player.getAbilities().instabuild) {
            donuts.shrink(1);
        }
        mom.level().playSound(null, mom.blockPosition(), SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL, 1.0F, 0.9F);
        say(player, ChatFormatting.LIGHT_PURPLE,
                "Mom Cobb: \"...donuts. Fine. He can stay at your place ONE more week.\"");
        Quests.complete(player, QuestExpansion.DOMESTIC_MOM_GIFT);
    }

    /**
     * Burning the fake product. The lighter has no right-click behaviour of its own,
     * so this only ever fires while the burn quest is outstanding and there is
     * something fake to burn - it can't quietly eat items later on.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack lighter = event.getItemStack();
        if (!lighter.is(ModItems.LIGHTER.get())) {
            return;
        }
        ItemStack book = Quests.findBook(player);
        if (book == null || !pending(book, QuestExpansion.INFAMY_BURN)) {
            return;
        }
        Item fake = firstFakeInInventory(player);
        if (fake == null || !consumeOne(player, fake)) {
            return;
        }
        lighter.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 1.0F, 0.9F);

        int burned = QuestProgress.bump(player, QuestProgress.FAKES_BURNED, 1);
        say(player, ChatFormatting.RED, "You torch it. That is " + burned + " of "
                + QuestExpansion.INFAMY_BURN_COUNT + " gone. It smells like a house fire.");
        if (burned >= QuestExpansion.INFAMY_BURN_COUNT) {
            Quests.complete(player, QuestExpansion.INFAMY_BURN);
        }
    }

    // =====================================================================
    // deaths
    // =====================================================================

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        Entity source = event.getSource().getEntity();
        ServerPlayer killer = source instanceof ServerPlayer sp ? sp : null;

        if (dead instanceof ThePlug && killer != null) {
            if (QuestProgress.bump(killer, QuestProgress.PLUGS_KILLED, 1)
                    >= QuestExpansion.INFAMY_PLUG_KILLS) {
                Quests.complete(killer, QuestExpansion.INFAMY_THREE);
            }
        }

        if (dead instanceof DuhlWol duhl) {
            if (duhl.isHostileMode() && killer != null) {
                Quests.complete(killer, QuestExpansion.DOMESTIC_DUHL_FIGHT);
            } else if (killer == null && !duhl.isHostileMode() && duhl.getTimer() <= 0) {
                // DuhlWol.pay() zeroes the timer, drops hostility and kills him outright:
                // whoever finally settled up is standing right next to the car.
                for (Player nearby : dead.level().getEntitiesOfClass(Player.class,
                        dead.getBoundingBox().inflate(16.0D))) {
                    Quests.complete(nearby, QuestExpansion.DOMESTIC_DUHL_CLEAR);
                }
            }
        }

        if (killer != null && killer.distanceTo(dead) >= QuestExpansion.INFAMY_SNIPE_RANGE
                && count(killer, ModItems.SNIPER_SCOPE.get()) > 0) {
            Quests.complete(killer, QuestExpansion.INFAMY_SCOPE);
        }
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private static boolean pending(ItemStack book, String id) {
        return !Quests.isDone(book, id);
    }

    /** Unlocked, offered to the player, and still outstanding. */
    private static boolean wants(Player player, ItemStack book, String id) {
        return pending(book, id) && Quests.isUnlocked(player, id);
    }

    private static AABB around(Player player, double radius) {
        return player.getBoundingBox().inflate(radius);
    }

    @Nullable
    private static CaydenCobb nearestCayden(Player player, Level level) {
        for (CaydenCobb cayden : level.getEntitiesOfClass(CaydenCobb.class, around(player, 32.0D))) {
            if (cayden.isTame() && cayden.isOwnedBy(player)) {
                return cayden;
            }
        }
        return null;
    }

    /** How many apocalypses this player has already caused (EventHandler's counter). */
    private static int deathStages(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG)
                .getInt("KraveDeathStage");
    }

    private static int stat(ServerPlayer player, ResourceLocation custom) {
        return player.getStats().getValue(Stats.CUSTOM.get(custom));
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean consumeOne(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean isFakeProduct(Item item) {
        return item == ModItems.FAKE_WEED.get()
                || item == ModItems.FAKE_COCAINE.get()
                || item == ModItems.OFF_BRAND_PASTRIES.get();
    }

    @Nullable
    private static Item firstFakeInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isFakeProduct(stack.getItem())) {
                return stack.getItem();
            }
        }
        return null;
    }

    private static void say(Player player, ChatFormatting colour, String text) {
        player.sendSystemMessage(Component.literal(colour + text));
    }
}
