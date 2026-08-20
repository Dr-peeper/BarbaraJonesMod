package com.barbarajones.v2.village;

import com.barbarajones.content.ModItems;
import com.barbarajones.v2.village.ai.VillagerSleepGoal;
import com.barbarajones.v2.village.ai.VillagerUseBuildingGoal;
import com.barbarajones.v2.village.ai.VillagerWanderHomeGoal;
import com.barbarajones.v2.village.menu.KraveTradeMenu;
import com.barbarajones.v2.village.net.PacketVillageOffers;
import com.barbarajones.v2.village.net.VillageNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Krave Villager: the mod's own NPC, not a reskinned vanilla villager.
 *
 * <p>It shares nothing with {@code Villager} - no {@code Brain}, no POI system, no
 * vanilla profession, no {@code MerchantOffer}. That is a deliberate cost. Vanilla
 * villagers drag in the whole village/POI/gossip machinery, which fights any
 * settlement system built on top of it and is the reason "custom villager" mods so
 * often end up feeling like a texture pack. This one is a plain
 * {@link PathfinderMob} with goal-based AI, its own trade list, and its own screen.
 *
 * <h2>One entity type, five jobs</h2>
 * Profession is {@linkplain SynchedEntityData synced entity data}, not a separate
 * entity type. A villager taking a different job is a data change - one renderer,
 * one attribute registration, one spawn egg. See {@link KraveProfession}.
 *
 * <h2>Getting smarter</h2>
 * Trade level runs 1..5 and is driven by XP from two sources: completed trades, and
 * being hand-fed Krave Cereal. Feeding is the fast, visible path -
 * {@link #feedKrave} throws particles, plays a rising chime, and on a level-up
 * flashes the villager and tells everyone nearby. A level-up <em>appends</em> the
 * next tier of offers; nothing the player already relies on is taken away.
 *
 * <h2>Persistence</h2>
 * Offers, XP, level, Krave eaten and the owning village id all live in the entity's
 * own NBT. Village membership is stored on both sides - the entity remembers its
 * village and the village remembers its residents - because either one alone breaks:
 * an entity-only link loses the population count when chunks unload, and a
 * village-only link orphans a villager that wanders out and back.
 */
public class KraveVillagerEntity extends PathfinderMob implements Npc {

    private static final EntityDataAccessor<Integer> DATA_PROFESSION =
            SynchedEntityData.defineId(KraveVillagerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(KraveVillagerEntity.class, EntityDataSerializers.INT);
    /** Ticks left on the "just ate Krave" flash. Purely cosmetic, but synced. */
    private static final EntityDataAccessor<Integer> DATA_GLOW =
            SynchedEntityData.defineId(KraveVillagerEntity.class, EntityDataSerializers.INT);
    /** How much Krave this one has been fed, ever. The renderer uses it. */
    private static final EntityDataAccessor<Integer> DATA_KRAVE_FED =
            SynchedEntityData.defineId(KraveVillagerEntity.class, EntityDataSerializers.INT);

    /** XP a single hand-fed Krave Cereal is worth. */
    public static final int XP_PER_KRAVE = 4;
    /** Ticks between restocks - one Minecraft day. */
    private static final int RESTOCK_INTERVAL = 24000;

    private final List<VillageOffer> offers = new ArrayList<>();

    @Nullable
    private UUID villageId;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private BlockPos claimedBed;

    private int tradeXp;
    private int restockTimer;
    private boolean offersRolled;

    public KraveVillagerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /**
     * Built from {@code createMobAttributes()}, never {@code createLivingAttributes()}.
     * The latter omits FOLLOW_RANGE, which {@code GroundPathNavigation} reads inside
     * its own constructor - the mob would NPE the instant it spawned.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.48D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PROFESSION, KraveProfession.GROCER.ordinal());
        this.entityData.define(DATA_LEVEL, 1);
        this.entityData.define(DATA_GLOW, 0);
        this.entityData.define(DATA_KRAVE_FED, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Sleeping outranks everything except drowning. It holds MOVE and LOOK, so
        // nothing else drags a sleeping villager out of bed.
        this.goalSelector.addGoal(1, new VillagerSleepGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.62D, false));
        this.goalSelector.addGoal(3, new VillagerUseBuildingGoal(this, 0.5D));
        this.goalSelector.addGoal(4, new VillagerWanderHomeGoal(this, 0.45D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 7.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Only Guards go looking for a fight; the rest rely on the walls and on the
        // Guards. Everyone still fights back when hit, which is what HurtByTargetGoal
        // does - a Grocer being punched by a zombie should not stand there smiling.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Monster.class, 10, true, false, this::guardShouldEngage));
    }

    private boolean guardShouldEngage(@Nullable LivingEntity candidate) {
        return getProfession() == KraveProfession.GUARD && candidate != null && candidate.isAlive();
    }

    // ---- synced properties ---------------------------------------------------

    public KraveProfession getProfession() {
        return KraveProfession.byOrdinal(this.entityData.get(DATA_PROFESSION));
    }

    /**
     * Changes the job. Wipes and rerolls the trade list, because a Guard's offers
     * make no sense coming out of a Grocer - the trade level and XP are kept, so
     * retraining costs the offers but not the relationship.
     */
    public void setProfession(KraveProfession profession) {
        this.entityData.set(DATA_PROFESSION, profession.ordinal());
        this.offers.clear();
        this.offersRolled = false;
        if (!level().isClientSide) {
            rollOffersUpTo(getTradeLevel());
        }
    }

    public int getTradeLevel() {
        return Math.max(1, Math.min(VillageTrades.MAX_LEVEL, this.entityData.get(DATA_LEVEL)));
    }

    public int getTradeXp() {
        return this.tradeXp;
    }

    /** Total Krave Cereal hand-fed to this villager. Drives the renderer's swell. */
    public int getKraveFed() {
        return this.entityData.get(DATA_KRAVE_FED);
    }

    /** 0..1, how bright the "just fed" flash currently is. Client-safe. */
    public float getGlow() {
        return Math.min(1.0F, this.entityData.get(DATA_GLOW) / 40.0F);
    }

    @Nullable
    public UUID getVillageId() {
        return this.villageId;
    }

    public void setVillageId(@Nullable UUID id) {
        this.villageId = id;
    }

    @Nullable
    public BlockPos getClaimedBed() {
        return this.claimedBed;
    }

    public void setClaimedBed(@Nullable BlockPos pos) {
        this.claimedBed = pos == null ? null : pos.immutable();
    }

    @Nullable
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    // ---- trades --------------------------------------------------------------

    /**
     * The live offer list. Mutating an offer in here is how a trade is consumed;
     * call {@link #syncOffersTo} afterwards or the client keeps showing full stock
     * until the screen is reopened.
     */
    public List<VillageOffer> getOffers() {
        if (!this.offersRolled && !level().isClientSide) {
            rollOffersUpTo(getTradeLevel());
        }
        return this.offers;
    }

    private void rollOffersUpTo(int level) {
        this.offersRolled = true;
        for (int tier = 1; tier <= level; tier++) {
            this.offers.addAll(VillageTrades.offersFor(getProfession(), tier, this.random));
        }
    }

    /**
     * Recomputes every offer's price from the village's mood. A happy town is a
     * cheap town. Called when the screen opens, so the player sees the current price
     * rather than the one that was cached three days ago.
     */
    public void refreshPrices() {
        int happiness = 50;
        if (level() instanceof ServerLevel server) {
            happiness = KraveVillage.happinessOf(server, blockPosition());
        }
        float multiplier = 1.15F - happiness / 200.0F;
        for (VillageOffer offer : getOffers()) {
            offer.setPriceMultiplier(multiplier);
        }
    }

    /**
     * Records a completed trade: consumes a use, banks the XP, tops up village
     * happiness a little, and levels the villager if that crossed a threshold.
     */
    public void notifyTradeCompleted(VillageOffer offer, Player player) {
        offer.use();
        addTradeXp(offer.xpReward(), player instanceof ServerPlayer sp ? sp : null);
        playSound(SoundEvents.VILLAGER_YES, 0.8F, 1.0F + this.random.nextFloat() * 0.2F);
        if (level() instanceof ServerLevel server) {
            KraveVillage.adjustHappiness(server, blockPosition(), 1);
        }
    }

    /**
     * Adds trade XP and handles the level-up. Returns true if a level was gained,
     * so callers can decide whether to make a fuss about it.
     */
    public boolean addTradeXp(int amount, @Nullable ServerPlayer witness) {
        if (amount <= 0 || getTradeLevel() >= VillageTrades.MAX_LEVEL) {
            return false;
        }
        this.tradeXp += amount;
        int newLevel = VillageTrades.levelForXp(this.tradeXp);
        if (newLevel <= getTradeLevel()) {
            return false;
        }
        int old = getTradeLevel();
        this.entityData.set(DATA_LEVEL, newLevel);
        for (int tier = old + 1; tier <= newLevel; tier++) {
            this.offers.addAll(VillageTrades.offersFor(getProfession(), tier, this.random));
        }
        celebrateLevelUp(newLevel, witness);
        return true;
    }

    private void celebrateLevelUp(int newLevel, @Nullable ServerPlayer witness) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        this.entityData.set(DATA_GLOW, 60);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 1.4D, getZ(), 24, 0.45D, 0.6D, 0.45D, 0.02D);
        server.sendParticles(ParticleTypes.END_ROD,
                getX(), getY() + 1.9D, getZ(), 12, 0.3D, 0.2D, 0.3D, 0.03D);
        server.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL, 0.7F, 1.4F);

        Component msg = Component.translatable("village.barbarajones.villager_level_up",
                getProfession().displayName(), newLevel);
        if (witness != null) {
            witness.displayClientMessage(msg, false);
        } else {
            for (ServerPlayer nearby : server.players()) {
                if (nearby.distanceToSqr(this) < 900.0D) {
                    nearby.displayClientMessage(msg, true);
                }
            }
        }
        syncOffersTo(witness);
    }

    /**
     * Hand-feeding. The visible half of "Krave makes them smarter": every bowl is
     * XP, particles and a rising chime, and the villager physically swells a little
     * as {@link #getKraveFed()} climbs.
     */
    public void feedKrave(ServerPlayer player, ItemStack stack) {
        int fed = getKraveFed() + 1;
        this.entityData.set(DATA_KRAVE_FED, fed);
        this.entityData.set(DATA_GLOW, 40);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        boolean levelled = addTradeXp(XP_PER_KRAVE, player);

        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 1.5D, getZ(), 8, 0.35D, 0.35D, 0.35D, 0.02D);
        server.sendParticles(ParticleTypes.COMPOSTER,
                getX(), getY() + 1.2D, getZ(), 6, 0.3D, 0.3D, 0.3D, 0.01D);
        playSound(SoundEvents.GENERIC_EAT, 0.8F, 1.0F + Math.min(0.6F, fed * 0.02F));

        if (!levelled) {
            int needed = VillageTrades.xpToNextLevel(getTradeLevel(), this.tradeXp);
            Component msg = getTradeLevel() >= VillageTrades.MAX_LEVEL
                    ? Component.translatable("village.barbarajones.feed_maxed", getProfession().displayName())
                    : Component.translatable("village.barbarajones.feed_progress",
                            getProfession().displayName(), needed);
            player.displayClientMessage(msg, true);
        }

        KraveVillage.adjustHappiness(server, blockPosition(), 1);
        syncOffersTo(player);
    }

    /** Pushes the current offer list to one player, or to whoever is trading. */
    public void syncOffersTo(@Nullable ServerPlayer player) {
        ServerPlayer target = player;
        if (target == null && this.tradingPlayer instanceof ServerPlayer sp) {
            target = sp;
        }
        if (target == null || !(target.containerMenu instanceof KraveTradeMenu menu)) {
            return;
        }
        if (menu.getVillager() != this) {
            return;
        }
        VillageNetwork.sendTo(target, new PacketVillageOffers(
                menu.containerId, getOffers(), getTradeLevel(), this.tradeXp, getKraveFed(),
                getProfession().ordinal()));
    }

    // ---- interaction ---------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || isBaby() || !isAlive()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.KRAVE_CEREAL.get()) || held.is(ModItems.GOLDEN_KRAVE.get())) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer sp) {
                // Golden Krave is worth a whole level's worth of ordinary cereal.
                if (held.is(ModItems.GOLDEN_KRAVE.get())) {
                    addTradeXp(XP_PER_KRAVE * 6, sp);
                    this.entityData.set(DATA_GLOW, 60);
                    if (!sp.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                } else {
                    feedKrave(sp, held);
                }
            }
            return InteractionResult.CONSUME;
        }

        if (isTrading() && this.tradingPlayer != player) {
            if (!level().isClientSide) {
                player.displayClientMessage(
                        Component.translatable("village.barbarajones.busy"), true);
            }
            return InteractionResult.CONSUME;
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            openTradeScreen(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Opens the custom trading menu, then immediately pushes the offer list.
     *
     * <p>Offers cannot ride in the menu's own open packet - the menu is constructed
     * on the client before the screen exists - so they go as their own message right
     * after, keyed by container id. That is the same split vanilla uses for
     * merchants, for the same reason.
     */
    public void openTradeScreen(ServerPlayer player) {
        refreshPrices();
        setTradingPlayer(player);
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inv, p) -> new KraveTradeMenu(id, inv, this), getDisplayName()),
                buf -> buf.writeVarInt(getId()));
        syncOffersTo(player);
    }

    @Override
    public Component getDisplayName() {
        if (hasCustomName()) {
            return super.getDisplayName();
        }
        return Component.translatable("village.barbarajones.villager_name",
                getProfession().displayName(), getTradeLevel());
    }

    // ---- lifecycle -----------------------------------------------------------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (!this.offersRolled) {
            rollOffersUpTo(getTradeLevel());
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        int glow = this.entityData.get(DATA_GLOW);
        if (glow > 0) {
            this.entityData.set(DATA_GLOW, glow - 1);
        }

        if (this.tradingPlayer != null
                && (!this.tradingPlayer.isAlive() || distanceToSqr(this.tradingPlayer) > 64.0D)) {
            setTradingPlayer(null);
        }

        if (++this.restockTimer >= RESTOCK_INTERVAL) {
            this.restockTimer = 0;
            restockAll();
        }

        // Adopt the settlement we are standing in, and stay near it. Doing this in
        // tick rather than at spawn means a villager summoned by a spawn egg or
        // dropped in by another module joins the town it lands in.
        if (this.tickCount % 100 == 0 && level() instanceof ServerLevel server) {
            adoptLocalVillage(server);
        }
    }

    private void adoptLocalVillage(ServerLevel server) {
        KraveVillageData data = KraveVillageData.get(server);
        Village village = data.containing(blockPosition());
        if (village == null) {
            return;
        }
        if (!village.id().equals(this.villageId)) {
            this.villageId = village.id();
        }
        if (village.registerVillager(getUUID())) {
            data.setDirty();
        }
        // Keeps pathfinding inside the claim without pinning the mob in place.
        restrictTo(village.origin(), Village.CLAIM_RADIUS);
    }

    private void restockAll() {
        boolean any = false;
        for (VillageOffer offer : getOffers()) {
            if (offer.restock()) {
                any = true;
            }
        }
        if (any) {
            playSound(SoundEvents.VILLAGER_WORK_FARMER, 0.6F, 1.0F);
        }
    }

    @Override
    public void die(DamageSource source) {
        // Tell the settlement before the entity goes away, so the population count
        // never leaks a ghost resident.
        if (level() instanceof ServerLevel server) {
            KraveVillageData.get(server).forgetVillager(getUUID());
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    // ---- sounds --------------------------------------------------------------

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isSleeping() ? null : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    // ---- persistence ---------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Profession", this.entityData.get(DATA_PROFESSION));
        tag.putInt("TradeLevel", this.entityData.get(DATA_LEVEL));
        tag.putInt("TradeXp", this.tradeXp);
        tag.putInt("KraveFed", this.entityData.get(DATA_KRAVE_FED));
        tag.putInt("RestockTimer", this.restockTimer);
        tag.putBoolean("OffersRolled", this.offersRolled);
        if (this.villageId != null) {
            tag.putUUID("VillageId", this.villageId);
        }
        if (this.claimedBed != null) {
            tag.putLong("Bed", this.claimedBed.asLong());
        }
        ListTag list = new ListTag();
        for (VillageOffer offer : this.offers) {
            list.add(offer.save());
        }
        tag.put("Offers", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PROFESSION, tag.getInt("Profession"));
        this.entityData.set(DATA_LEVEL, Math.max(1, tag.getInt("TradeLevel")));
        this.tradeXp = tag.getInt("TradeXp");
        this.entityData.set(DATA_KRAVE_FED, tag.getInt("KraveFed"));
        this.restockTimer = tag.getInt("RestockTimer");
        this.offersRolled = tag.getBoolean("OffersRolled");
        if (tag.hasUUID("VillageId")) {
            this.villageId = tag.getUUID("VillageId");
        }
        if (tag.contains("Bed")) {
            this.claimedBed = BlockPos.of(tag.getLong("Bed"));
        }
        this.offers.clear();
        ListTag list = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            this.offers.add(VillageOffer.load(list.getCompound(i)));
        }
        // An older save, or a villager built by /summon with no Offers tag at all,
        // still needs a trade list - otherwise it opens an empty screen forever.
        if (this.offers.isEmpty()) {
            this.offersRolled = false;
        }
    }
}
