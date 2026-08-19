package com.barbarajones.entity;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModSounds;
import com.barbarajones.housing.HousingResult;
import com.barbarajones.housing.HousingValidator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Cayden Cobb - your Krave-addicted companion. He fights every hostile mob on
 * sight (and they hunt him back), gets stronger and fatter the more Krave you
 * feed him, and - Terraria-style - he will only settle in a room that actually
 * meets the housing requirements.
 *
 * Claim a home by right-clicking him while standing in the room you want him
 * to live in. If the room later stops qualifying (you break the door, steal the
 * light, tear the roof off) he complains and moves out.
 */
public class CaydenCobb extends TamableAnimal {

    private static final EntityDataAccessor<Integer> FED =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RAGE =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOUSED =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SSJ =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);
    /** Half-ascension: cornered and fighting for his life. Synced for the aura. */
    private static final EntityDataAccessor<Boolean> DESPERATE =
            SynchedEntityData.defineId(CaydenCobb.class, EntityDataSerializers.BOOLEAN);

    public static final int RAGE_THRESHOLD = 25;
    /** How long a transformation lasts before powering down on its own. */
    private static final int SSJ_DURATION_TICKS = 6000;
    /** Below this fraction of max health he half-ascends to save himself. */
    private static final float DESPERATE_AT = 0.40F;
    /** He must climb back above this to calm down - hysteresis stops it flickering. */
    private static final float DESPERATE_OFF = 0.70F;
    /** Odds of a raw power flash on any given hit: 1 in this. */
    private static final int FLASH_ODDS = 10;
    /** Odds of a meteor answering a desperate punch: 1 in this. */
    private static final int METEOR_ODDS = 3;
    /** Heal a point this often, once out of combat. */
    private static final int REGEN_INTERVAL = 60;
    /** How long since being hit before he starts healing again. */
    private static final int OUT_OF_COMBAT_TICKS = 100;
    private static final double BASE_SPEED = 0.5D;
    private static final double BASE_DAMAGE = 3.0D;
    /** How far he'll wander from a claimed home before heading back. */
    private static final double HOME_LEASH = 22.0D;

    @Nullable
    /** 30s of post-respawn immunity, in ticks. */
    public static final int GRACE_TICKS = 600;

    private BlockPos home;
    /** Dimension the home was claimed in - a house in the Overworld is not a house in the Kosmos. */
    private String homeDim = "";
    private int homeCheckTimer = 100;
    private int graceTicks;
    /** Client-only: ticks since the ascension became visible, or -1. */
    private int ssjClientAge = -1;
    private int ssjTicks;
    /** While true the transformation has no timer: it ends when the boss does. */
    private boolean ssjUntilBossDies;

    public CaydenCobb(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, BASE_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SsjFlyAttackGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new CaydenFollowOrHomeGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // always hostile toward hostiles - except the fight-support mobs, which
        // ignore him and he ignores them; the player handles those, not Cayden
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                e -> !(e instanceof KraveMinion) && !(e instanceof KraveHealingBox)));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FED, 0);
        this.entityData.define(RAGE, false);
        this.entityData.define(HOUSED, false);
        this.entityData.define(SSJ, false);
        this.entityData.define(DESPERATE, false);
    }

    // ---- krave state -------------------------------------------------------

    public int getKraveFed() {
        return this.entityData.get(FED);
    }

    public boolean isRageUnlocked() {
        return this.entityData.get(RAGE);
    }

    public boolean isHoused() {
        return this.entityData.get(HOUSED);
    }

    public float getFatScale() {
        return Math.min(2.4F, 1.0F + getKraveFed() * 0.03F);
    }

    /** Used when he respawns from the Krave blast - keep his hard-won progress. */
    public void restoreKrave(int fed, boolean rage) {
        this.entityData.set(FED, fed);
        this.entityData.set(RAGE, rage);
        applyKraveStats();
    }

    private void applyKraveStats() {
        int fed = getKraveFed();
        double atkBase = BASE_DAMAGE + fed / 5;
        double spdBase = Math.max(0.12D, BASE_SPEED - fed * 0.012D);
        if (isSuperSaiyan()) {
            atkBase *= 4.0D;
            spdBase = BASE_SPEED * 1.6D;
        }
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (atk != null) {
            atk.setBaseValue(atkBase);
        }
        if (spd != null) {
            spd.setBaseValue(spdBase);
        }
    }

    // ---- super saiyan --------------------------------------------------------

    public boolean isSuperSaiyan() {
        return this.entityData.get(SSJ);
    }

    /** Client-side ticks since he ascended; -1 when not ascended. */
    public int ticksSinceAscension() {
        return this.ssjClientAge;
    }

    /** Arriving in the Kosmos. He ascends, shouts, and stays ascended until the boss falls. */
    public void onEnterKosmos() {
        if (level().isClientSide) {
            return;
        }
        this.ssjUntilBossDies = true;
        becomeSuperSaiyan();
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.6F, 1.0F);
    }

    /** Liquid chocolate does this too - see EventHandler.onLivingTick. */
    public void becomeSuperSaiyan() {
        if (isSuperSaiyan() || level().isClientSide) {
            return;
        }
        this.entityData.set(SSJ, true);
        this.ssjTicks = SSJ_DURATION_TICKS;
        setNoGravity(true);
        applyKraveStats();
        heal(getMaxHealth());
        playSound(ModSounds.KRAVE_BOOM.get(), 1.4F, 0.7F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    80, 0.6D, 0.9D, 0.6D, 0.08D);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                    + "CAYDEN COBB HAS ASCENDED."));
        }
    }

    /** Back to being a kid who eats too much cereal. */
    public void powerDown() {
        boolean was = isSuperSaiyan();
        this.entityData.set(SSJ, false);
        this.ssjTicks = 0;
        this.ssjUntilBossDies = false;
        setNoGravity(false);
        applyKraveStats();
        if (was && !level().isClientSide) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                        + "Cayden powers down. He is just a kid again."));
            }
        }
    }

    // ---- housing -----------------------------------------------------------

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    /** Try to claim the room the player is standing in. Reports problems if not. */
    public void tryClaimHome(Player player) {
        HousingResult result = HousingValidator.validate(level(), player.blockPosition());
        if (result.valid) {
            this.home = result.anchor;
            this.entityData.set(HOUSED, true);
            this.homeDim = level().dimension().location().toString();
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                    + "Cayden moves in. (" + result.volume + " blocks of space)"));
            playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.4F);
        } else {
            this.home = null;
            this.entityData.set(HOUSED, false);
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "Cayden refuses to live here:"));
            for (String problem : result.problems) {
                player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + " - " + problem));
            }
            playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.4F);
        }
    }

    /** True only when he is standing in the dimension his house is actually in. */
    public boolean homeIsInThisDimension() {
        return this.home != null
                && level().dimension().location().toString().equals(this.homeDim);
    }

    /** Periodically re-check the claimed home; he moves out if you ruin it. */
    private void recheckHome() {
        // His house is in the Overworld. Validating those coordinates against
        // Kosmos terrain would evict him for a room that is not even here.
        if (!homeIsInThisDimension()) {
            return;
        }
        if (this.home == null || !(level() instanceof ServerLevel)) {
            return;
        }
        HousingResult result = HousingValidator.validate(level(), this.home);
        if (!result.valid) {
            this.entityData.set(HOUSED, false);
            BlockPos lost = this.home;
            this.home = null;
            List<Player> near = level().getEntitiesOfClass(Player.class,
                    new AABB(lost).inflate(32.0D));
            for (Player p : near) {
                p.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "Cayden moved out! " + ChatFormatting.GRAY + result.summary()));
            }
        } else {
            this.home = result.anchor;
            this.entityData.set(HOUSED, true);
        }
    }

    // ---- tick --------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Client-side age of the transformation, used by SsjAuraLayer for the
            // one-second ground shockwave. Derived from the synced SSJ flag rather
            // than networked separately - there is nothing here worth a packet.
            if (isSuperSaiyan()) {
                this.ssjClientAge = this.ssjClientAge < 0 ? 0 : this.ssjClientAge + 1;
            } else {
                this.ssjClientAge = -1;
            }
            return;
        }

        if (this.graceTicks > 0) {
            this.graceTicks--;
            // He respawns airborne. Without this he banks a lethal fall distance
            // during the grace window and dies the instant it runs out.
            this.fallDistance = 0.0F;
            this.setRemainingFireTicks(0);
            if (this.tickCount % 4 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        2, 0.35D, 0.4D, 0.35D, 0.0D);
            }
            if (this.graceTicks == 0) {
                setInvulnerable(false);
            }
        }

        if (!isSuperSaiyan() && getFluidTypeHeight(ModFluids.CHOCOLATE_TYPE.get()) > 0.0D) {
            becomeSuperSaiyan();
        }
        if (isSuperSaiyan() && this.ssjUntilBossDies && this.tickCount % 20 == 0
                && level() instanceof ServerLevel sl) {
            // Go and find him. The Kosmos is far too big to rely on follow range.
            if (!(getTarget() instanceof KraveMonster boss) || !boss.isAlive()) {
                for (KraveMonster candidate : sl.getEntitiesOfClass(KraveMonster.class,
                        getBoundingBox().inflate(512.0D, 256.0D, 512.0D))) {
                    if (candidate.isAlive()) {
                        setTarget(candidate);
                        break;
                    }
                }
            }
        }

        regenerate();
        updateDesperation();

        if (isSuperSaiyan()) {
            // Linked to the boss fight, the transformation has no clock on it -
            // it lasts exactly as long as the Krave Monster does.
            if (!this.ssjUntilBossDies && --this.ssjTicks <= 0) {
                powerDown();
            } else if (this.tickCount % 5 == 0 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        3, 0.35D, 0.6D, 0.35D, 0.03D);
            }
        }

        // auto-tame to whoever is nearby, however he got here
        if (!isTame()) {
            Player near = level().getNearestPlayer(this, 12.0D);
            if (near != null) {
                tame(near);
                near.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                        + "Cayden Cobb imprinted on you. Feed him Krave - and build him a house."));
            }
        }

        if (this.tickCount % 10 == 0 && isTame()
                && getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
            com.barbarajones.net.ModNetwork.sendTo(owner,
                    new com.barbarajones.net.PacketCaydenStatus(getHealth(), getMaxHealth(),
                            (int) Math.sqrt(distanceToSqr(owner)), isHoused()));
        }

        if (--this.homeCheckTimer <= 0) {
            this.homeCheckTimer = 200;
            recheckHome();
        }

        // hostiles hunt him back
        if (this.tickCount % 20 == 0) {
            for (Monster m : level().getEntitiesOfClass(Monster.class, getBoundingBox().inflate(12.0D))) {
                if (m.isAlive() && m.getTarget() == null) {
                    m.setTarget(this);
                }
            }
        }
    }

    // ---- interaction -------------------------------------------------------

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        var held = player.getItemInHand(hand);

        if (held.is(com.barbarajones.content.ModItems.KRAVE_CEREAL.get())) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    tame(player);
                }
                feedKrave(player);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }

        // empty hand from the owner: claim the room you're standing in
        if (held.isEmpty() && isTame() && isOwnedBy(player)) {
            if (!level().isClientSide) {
                tryClaimHome(player);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void feedKrave(Player player) {
        this.entityData.set(FED, getKraveFed() + 1);
        int fed = getKraveFed();
        heal(4.0F);
        applyKraveStats();
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.2F);
        // he announces it. every single time.
        level().playSound(null, blockPosition(), ModSounds.CAYDEN_SHOUT.get(),
                getSoundSource(), 1.0F, 1.0F);

        if (fed % 5 == 0) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                    + "Cayden's attack rose to " + (int) getAttributeValue(Attributes.ATTACK_DAMAGE)
                    + "! (slower, and fatter...)"));
        }
        if (fed >= RAGE_THRESHOLD && !isRageUnlocked()) {
            this.entityData.set(RAGE, true);
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + ""
                    + ChatFormatting.BOLD + "KRAVE RAGE UNLOCKED!"));
        }
    }

    // ---- misc --------------------------------------------------------------

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    /**
     * Heals like a well-fed player: a point every few seconds, but only once he
     * has been left alone for a moment. Without this he accumulated every scrape
     * from every fight until something trivial finished him off - and his death
     * is the one thing this mod is built around avoiding.
     */
    private void regenerate() {
        if (getHealth() >= getMaxHealth() || !isAlive()) {
            return;
        }
        // hurtTime is set on every hit; use the vanilla last-hurt clock so he
        // does not heal through a fight he is currently losing.
        if (getLastHurtByMob() != null
                && this.tickCount - getLastHurtByMobTimestamp() < OUT_OF_COMBAT_TICKS) {
            return;
        }
        if (this.tickCount % REGEN_INTERVAL == 0) {
            heal(1.0F);
            if (level() instanceof ServerLevel sl && getHealth() < getMaxHealth()) {
                sl.sendParticles(ParticleTypes.HEART,
                        getX(), getY() + getBbHeight() + 0.2D, getZ(),
                        1, 0.25D, 0.1D, 0.25D, 0.0D);
            }
        }
    }

    // ---- flashes of the real thing -----------------------------------------

    public boolean isDesperate() {
        return this.entityData.get(DESPERATE);
    }

    /**
     * Cornered, he half-ascends. Not the full transformation - no flight, no
     * invulnerability - but enough to fight his way out: damage resistance,
     * fists that burn, and the occasional meteor.
     *
     * <p>Entering and leaving use different thresholds on purpose. A single
     * threshold makes him strobe in and out of the state while he trades blows
     * around that health value.
     */
    private void updateDesperation() {
        float frac = getMaxHealth() <= 0.0F ? 1.0F : getHealth() / getMaxHealth();
        boolean now = isDesperate();

        if (!now && !isSuperSaiyan() && frac <= DESPERATE_AT && getTarget() != null) {
            this.entityData.set(DESPERATE, true);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, false));
            addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
            playSound(ModSounds.KRAVE_ROAR.get(), 1.3F, 1.35F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.6D, getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                        40, 0.5D, 0.7D, 0.5D, 0.06D);
            }
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                        + "Cayden is cornered - he is not going down like this."));
            }
        } else if (now && (frac >= DESPERATE_OFF || getTarget() == null || isSuperSaiyan())) {
            this.entityData.set(DESPERATE, false);
        }

        // top the effects up while it lasts, so a long fight does not run them out
        if (isDesperate() && this.tickCount % 100 == 0) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1, false, false));
            addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));
        }
        if (isDesperate() && this.tickCount % 3 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    2, 0.32D, 0.45D, 0.32D, 0.01D);
        }
    }

    /**
     * Every punch is a chance for the power to show through: a one-in-ten
     * launch, and while desperate, burning fists and a one-in-five meteor.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!hit || level().isClientSide) {
            return hit;
        }

        // 1-in-10: a flash of the full thing. Ten times the shove.
        if (this.random.nextInt(FLASH_ODDS) == 0) {
            double yaw = Math.toRadians(getYRot());
            target.push(-Math.sin(yaw) * 3.4D, 0.85D, Math.cos(yaw) * 3.4D);
            target.hurtMarked = true;   // without this the client never sees the launch
            playSound(ModSounds.KRAVE_BOOM.get(), 1.1F, 1.6F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        3, 0.3D, 0.3D, 0.3D, 0.0D);
                sl.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + 0.4D, target.getZ(),
                        18, 0.2D, 0.3D, 0.2D, 0.22D);
            }
        }

        if (isDesperate()) {
            target.setSecondsOnFire(4);          // fire aspect, near enough
            if (this.random.nextInt(METEOR_ODDS) == 0) {
                callMeteor(target);
            }
        }
        return hit;
    }

    /**
     * Drops one meteor on whatever he just hit. KraveMeteor already refuses to
     * damage Cayden, Barbara or players, and attributing it to him means the
     * Krave Monster's damage gate does not shrug it off - but the crater still
     * sets fires, which is why the desperate state carries fire resistance.
     */
    private void callMeteor(net.minecraft.world.entity.Entity target) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        com.barbarajones.entity.KraveMeteor m =
                com.barbarajones.content.ModEntities.METEOR.get().create(sl);
        if (m == null) {
            return;
        }
        double ox = (this.random.nextDouble() - 0.5D) * 3.0D;
        double oz = (this.random.nextDouble() - 0.5D) * 3.0D;

        // Drop from as high as there is actually room for. Spawning at a fixed
        // 34 blocks put the meteor inside the rock whenever the fight was in a
        // cave or indoors, where it collided on its first tick and "landed"
        // somewhere nobody could see. Walk up from the target instead and stop
        // under whatever ceiling is there.
        BlockPos scan = BlockPos.containing(target.getX() + ox, target.getY() + 1.0D, target.getZ() + oz);
        int clear = 0;
        for (int i = 0; i < 34; i++) {
            BlockPos above = scan.above(i + 1);
            if (!sl.getBlockState(above).isAir()) {
                break;
            }
            clear = i + 1;
        }
        if (clear < 3) {
            return;   // pinned against a low ceiling: no room for a meteor at all
        }

        double spawnY = target.getY() + clear;
        m.saiyanStrike(this);
        m.setPos(target.getX() + ox, spawnY, target.getZ() + oz);
        m.aim(-ox * 0.06D, -oz * 0.06D);
        sl.addFreshEntity(m);

        // Telegraph it, so the strike reads as his doing rather than random noise.
        playSound(ModSounds.KRAVE_SCREECH.get(), 1.2F, 1.5F);
        sl.sendParticles(ParticleTypes.SMALL_FLAME,
                target.getX(), target.getY() + target.getBbHeight() + 0.4D, target.getZ(),
                14, 0.35D, 0.2D, 0.35D, 0.02D);

    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Ascended for the Kosmos showdown he simply cannot be killed. That is
        // the deal the user asked for: this is the one fight where he gets the
        // full apocalypse arsenal and does not die for using it.
        if (isSuperSaiyan() && this.ssjUntilBossDies) {
            return false;
        }
        // 30 seconds of total immunity after he claws his way back out. He respawns
        // mid-apocalypse - in the air, next to fire, inside the tornado - and used to
        // immediately splatter on landing and trigger the whole thing over again.
        if (this.graceTicks > 0) {
            return false;
        }
        // the apocalypse's own fire/blasts can't kill him (no death loops)
        if (isTame() && (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE))
                && com.barbarajones.apocalypse.KraveApocalypse.isActiveNear(level(), position())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Make him untouchable for a while (used on post-death respawn). */
    public void grantGrace(int ticks) {
        this.graceTicks = Math.max(this.graceTicks, ticks);
        this.invulnerableTime = 20;
        setInvulnerable(true);
    }

    public boolean hasGrace() {
        return this.graceTicks > 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CAYDEN_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CAYDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CAYDEN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KraveFed", getKraveFed());
        tag.putBoolean("KraveRage", isRageUnlocked());
        tag.putBoolean("Ssj", isSuperSaiyan());
        tag.putInt("SsjTicks", this.ssjTicks);
        tag.putBoolean("SsjBossLinked", this.ssjUntilBossDies);
        if (this.home != null) {
            tag.putInt("HomeX", this.home.getX());
            tag.putInt("HomeY", this.home.getY());
            tag.putInt("HomeZ", this.home.getZ());
            tag.putString("HomeDim", this.homeDim);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FED, tag.getInt("KraveFed"));
        this.entityData.set(RAGE, tag.getBoolean("KraveRage"));
        if (tag.getBoolean("Ssj")) {
            this.entityData.set(SSJ, true);
            this.ssjTicks = Math.max(1, tag.getInt("SsjTicks"));
            this.ssjUntilBossDies = tag.getBoolean("SsjBossLinked");
            setNoGravity(true);
        }
        if (tag.contains("HomeX")) {
            this.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            this.homeDim = tag.getString("HomeDim");
            if (this.homeDim.isEmpty()) {
                this.homeDim = level().dimension().location().toString();
            }
            this.entityData.set(HOUSED, true);
        }
        applyKraveStats();
    }

    /**
     * Only relevant transformed and locked onto Krave Monster: flies toward him,
     * firing Krave Lasers at range and switching to melee once close. This is
     * intentionally the mod's only way to actually hurt Krave Monster during the
     * fight (see KraveMonster.hurt()'s damage gating) - the player's job is the
     * minions and healing boxes, not this fight directly.
     */
    static class SsjFlyAttackGoal extends Goal {
        private static final double RANGE = 5.0D;
        private final CaydenCobb cayden;
        private int laserCooldown;
        private int meleeCooldown;

        SsjFlyAttackGoal(CaydenCobb cayden) {
            this.cayden = cayden;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.cayden.isSuperSaiyan() && this.cayden.getTarget() instanceof KraveMonster target
                    && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            if (this.cayden.getTarget() instanceof KraveMonster boss
                    && this.cayden.level() instanceof ServerLevel sl) {
                com.barbarajones.apocalypse.KraveKosmosBattle.start(sl, boss, this.cayden);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.cayden.getTarget();
            if (target == null) {
                return;
            }
            this.cayden.getLookControl().setLookAt(target, 30.0F, 30.0F);

            Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                    .subtract(this.cayden.position().add(0.0D, this.cayden.getBbHeight() * 0.5D, 0.0D));
            double dist = to.length();

            if (dist > RANGE) {
                Vec3 dir = to.scale(1.0D / Math.max(dist, 0.01D));
                this.cayden.setDeltaMovement(dir.scale(0.5D));
                if (--this.laserCooldown <= 0) {
                    this.laserCooldown = 25;
                    fireLaser(target);
                }
            } else {
                this.cayden.setDeltaMovement(this.cayden.getDeltaMovement().scale(0.6D));
                if (--this.meleeCooldown <= 0) {
                    this.meleeCooldown = 15;
                    this.cayden.doHurtTarget(target);
                }
            }
        }

        private void fireLaser(LivingEntity target) {
            if (!(this.cayden.level() instanceof ServerLevel)) {
                return;
            }
            Vec3 from = this.cayden.position().add(0.0D, this.cayden.getBbHeight() * 0.6D, 0.0D);
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            KraveLaser laser = new KraveLaser(this.cayden.level(), this.cayden, from, aim);
            this.cayden.level().addFreshEntity(laser);
            this.cayden.level().playSound(null, this.cayden.blockPosition(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 1.6F);
        }
    }

    /** Follow the owner when unhoused; stay near home when housed. */
    static class CaydenFollowOrHomeGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final CaydenCobb cayden;

        CaydenFollowOrHomeGoal(CaydenCobb cayden) {
            this.cayden = cayden;
        }

        @Override
        public boolean canUse() {
            return this.cayden.getTarget() == null;
        }

        @Override
        public void tick() {
            BlockPos home = this.cayden.getHome();
            if (home != null && this.cayden.homeIsInThisDimension()) {
                if (this.cayden.blockPosition().distSqr(home) > HOME_LEASH * HOME_LEASH) {
                    this.cayden.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D);
                }
                return;
            }
            LivingEntity owner = this.cayden.getOwner();
            if (owner != null && this.cayden.distanceToSqr(owner) > 36.0D
                    && this.cayden.distanceToSqr(owner) < 1024.0D) {
                this.cayden.getNavigation().moveTo(owner, 1.1D);
            }
        }
    }
}
