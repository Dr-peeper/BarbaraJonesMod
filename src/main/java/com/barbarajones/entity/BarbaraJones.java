package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.barbara.BarbaraCombat;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Barbara Jones. Calm by default; goes PSYCHO when her stash runs dry or someone
 * waves grass at her; gets euphoric and blows the O's when handed a joint. Once
 * Krave Rage is unlocked she becomes a pet that fights hostiles at your side.
 *
 * <p>Her whole fighting kit lives in {@link BarbaraCombat} and every move in it
 * is paid for out of the same stash that keeps her calm - so the bag the player
 * keeps topping up is simultaneously her mood and her ammunition. Empty that
 * bag and she has nothing left but her fists, which is exactly when she is
 * angriest about it.
 */
public class BarbaraJones extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> RAGING =
            SynchedEntityData.defineId(BarbaraJones.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STASH =
            SynchedEntityData.defineId(BarbaraJones.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(BarbaraJones.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HIGH =
            SynchedEntityData.defineId(BarbaraJones.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PET =
            SynchedEntityData.defineId(BarbaraJones.class, EntityDataSerializers.BOOLEAN);

    private static final int STASH_MAX = 6000;
    private static final int GIFT      = 2400;
    private static final int HIGH_DUR  = 900;
    private static final float SCALE_CALM = 1.0F, SCALE_PSYCHO = 1.85F, SCALE_HIGH = 1.3F;
    private static final double SPEED_CALM = 0.28D, SPEED_PSYCHO = 0.62D, SPEED_HIGH = 0.42D;
    private static final double PROVOKE_RANGE = 12.0D;

    @Nullable
    private UUID petOwner;
    private boolean wasRaging;
    private double lastSpeed = SPEED_CALM;
    /** The smoking kit. Server-side only; nothing in it runs on the client. */
    private final BarbaraCombat combat = new BarbaraCombat(this);
    /** 30s of post-respawn immunity, in ticks - same reason as Cayden's. */
    public static final int GRACE_TICKS = 600;

    private int graceTicks;
    private int puffTimer;
    /** Ticks left of "you smoked MY stash" fury - overrides even pet calm. */
    private int grudge;

    public BarbaraJones(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED_CALM)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)          // all bark
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, SPEED_PSYCHO, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, SPEED_CALM));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        // the Krave Monster is always fair game
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, KraveMonster.class, true));
        // players only while she's actually lost it
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false,
                e -> isRaging() && !isPet()));
        // as a pet she fights hostiles
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 0, false, false,
                e -> isPet()));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RAGING, false);
        this.entityData.define(STASH, STASH_MAX);
        this.entityData.define(SCALE, SCALE_CALM);
        this.entityData.define(HIGH, 0);
        this.entityData.define(PET, false);
    }

    // ---- state --------------------------------------------------------------

    public boolean isRaging()  { return this.entityData.get(RAGING); }
    public int getStash()      { return this.entityData.get(STASH); }
    public float getScale()    { return this.entityData.get(SCALE); }
    public int getHigh()       { return this.entityData.get(HIGH); }
    public boolean isHigh()    { return getHigh() > 0; }
    public boolean isPet()     { return this.entityData.get(PET); }

    private void setStash(int t) { this.entityData.set(STASH, Math.max(0, Math.min(STASH_MAX, t))); }
    private void setHigh(int t)  { this.entityData.set(HIGH, Math.max(0, t)); }

    public void addGrassStash(int amount) { setStash(getStash() + amount); }

    /** A full bag, for anything that wants to draw her supply as a fraction. */
    public static int getStashCapacity() { return STASH_MAX; }

    /** Nothing left to smoke, so nothing left to fight with but her hands. */
    public boolean isDry() { return getStash() <= 0; }

    public BarbaraCombat getCombat() { return this.combat; }

    /**
     * Burn stash on an ability. Refuses rather than part-paying, so a move never
     * half-fires: either she had the grass for it or she did not.
     */
    public boolean spendStash(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (getStash() < amount) {
            return false;
        }
        setStash(getStash() - amount);
        return true;
    }

    /** Top her high up to at least this long - never cuts an existing one short. */
    public void makeHigh(int ticks) {
        setHigh(Math.max(getHigh(), ticks));
    }

    /**
     * Whether players are currently on the wrong side of her. The kit reads this
     * to decide whether the person watching is a bystander or a target - wave
     * grass at Barbara and the smoke screen is aimed at you.
     */
    public boolean isMadAtPlayers() {
        return isRaging() && !isPet();
    }

    public boolean hasGrace() { return this.graceTicks > 0; }

    public void setPet(Player owner) {
        this.entityData.set(PET, true);
        this.petOwner = owner.getUUID();
        this.entityData.set(RAGING, false);
        setTarget(null);
    }

    @Nullable
    public Player getPetOwner() {
        if (this.petOwner == null) {
            return level().getNearestPlayer(this, 32.0D);
        }
        return level().getPlayerByUUID(this.petOwner);
    }

    /**
     * Someone got high on her supply. She takes it personally: she snaps out of
     * any mellow she was in, rages, and comes for them - pet or not.
     */
    public void takeOffenceAt(Player player) {
        if (level().isClientSide) {
            return;
        }
        setHigh(0);                     // the mellow is over
        setStash(0);                    // and so is the stash, apparently
        this.grudge = 400;              // 20 seconds of pure indignation
        this.entityData.set(RAGING, true);
        this.wasRaging = true;
        setTarget(player);
        playSound(ModSounds.BARBARA_RAGE.get(), 1.4F, 1.0F);
    }

    /** Cosmetic wind-up when she hurls Cayden during Krave Rage. */
    public void playThrowAnimation() {
        if (!level().isClientSide) {
            level().playSound(null, blockPosition(), ModSounds.EVT_HOUSE.get(),
                    getSoundSource(), 1.3F, 1.0F);
        }
    }

    // ---- tick ---------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            spawnSmoke();
            return;
        }

        if (this.graceTicks > 0) {
            this.graceTicks--;
            this.fallDistance = 0.0F;
            this.setRemainingFireTicks(0);
            if (this.graceTicks == 0) {
                setInvulnerable(false);
            }
        }

        if (getStash() > 0) {
            setStash(getStash() - 1);
        }
        if (getHigh() > 0) {
            setHigh(getHigh() - 1);
        }
        if (this.tickCount % 20 == 0 && getStash() < STASH_MAX) {
            harvestUnderfoot();
        }

        if (this.grudge > 0) {
            this.grudge--;
        }
        // a grudge overrides even pet-calm: she does not forget whose stash that was
        boolean shouldRage = this.grudge > 0 || (!isPet() && (getStash() <= 0 || isBeingTaunted()));
        if (shouldRage != this.wasRaging) {
            if (shouldRage) {
                playSound(ModSounds.BARBARA_RAGE.get(), 1.4F, 1.0F);
            } else {
                setTarget(null);
                getNavigation().stop();
                playSound(ModSounds.BARBARA_IDLE.get(), 1.0F, 1.0F);
            }
            this.wasRaging = shouldRage;
        }
        this.entityData.set(RAGING, shouldRage);
        updateSpeed(shouldRage);

        float target = shouldRage ? SCALE_PSYCHO : (isHigh() ? SCALE_HIGH : SCALE_CALM);
        float cur = getScale();
        if (Math.abs(cur - target) > 0.005F) {
            this.entityData.set(SCALE, cur + (target - cur) * 0.08F);
        } else if (cur != target) {
            this.entityData.set(SCALE, target);
        }

        if (shouldRage) {
            if (this.horizontalCollision && onGround()) {
                setDeltaMovement(getDeltaMovement().x, 0.45D, getDeltaMovement().z);
            }
        } else {
            if (isHigh()) {
                if (onGround() && this.random.nextInt(35) == 0) {
                    setDeltaMovement(getDeltaMovement().x + (this.random.nextDouble() - 0.5D) * 0.3D,
                            0.42D, getDeltaMovement().z + (this.random.nextDouble() - 0.5D) * 0.3D);
                }
                if (this.random.nextInt(20) == 0) {
                    setYRot(getYRot() + (this.random.nextFloat() - 0.5F) * 60.0F);
                }
            }
            if (isPet() && getTarget() == null && this.tickCount % 20 == 0) {
                followOwner();
            }
        }

        // With an empty bag there is no kit left to run, only the temper. She
        // swings harder for it, so letting her supply die has a cost the player
        // can feel from the other side of the fight as easily as from beside her.
        if (isDry() && shouldRage && this.tickCount % 40 == 0) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false));
        }
        this.combat.tick();
    }

    private void updateSpeed(boolean raging) {
        double want = raging ? SPEED_PSYCHO : (isHigh() ? SPEED_HIGH : SPEED_CALM);
        if (want != this.lastSpeed) {
            var attr = getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                attr.setBaseValue(want);
            }
            this.lastSpeed = want;
        }
    }

    private void followOwner() {
        Player owner = getPetOwner();
        if (owner == null) {
            return;
        }
        double d = distanceToSqr(owner);
        if (d > 12.0D && d < 400.0D) {
            getNavigation().moveTo(owner, SPEED_PSYCHO);
        }
    }

    private boolean isBeingTaunted() {
        for (Player p : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(PROVOKE_RANGE, 4.0D, PROVOKE_RANGE))) {
            if (isGrass(p.getMainHandItem())) {
                return true;
            }
        }
        return false;
    }

    private void harvestUnderfoot() {
        BlockPos base = blockPosition();
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = base.above(dy);
            var state = level().getBlockState(p);
            if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
                level().removeBlock(p, false);
                setStash(getStash() + GIFT);
                playSound(net.minecraft.sounds.SoundEvents.GRASS_BREAK, 0.8F, 1.0F);
                return;
            }
        }
    }

    private void spawnSmoke() {
        double yaw = Math.toRadians(this.yHeadRot);
        double tipX = getX() - Math.sin(yaw) * 0.45D;
        double tipZ = getZ() + Math.cos(yaw) * 0.45D;
        double tipY = getY() + getEyeHeight() - 0.12D * getScale();

        if (isHigh()) {
            if (--this.puffTimer > 0) {
                return;
            }
            this.puffTimer = 12 + this.random.nextInt(10);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5.0D;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        tipX + Math.cos(a) * 0.18D, tipY + Math.sin(a) * 0.18D, tipZ,
                        -Math.sin(yaw) * 0.04D, 0.01D, Math.cos(yaw) * 0.04D);
            }
            level().addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    tipX, tipY + 0.3D, tipZ, 0, 0, 0);
            return;
        }
        if (getStash() <= 0 || --this.puffTimer > 0) {
            return;
        }
        this.puffTimer = 4 + this.random.nextInt(6);
        level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, tipX, tipY, tipZ, 0, 0.02D, 0);
        if (this.random.nextInt(4) == 0) {
            level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, tipX, tipY, tipZ, 0, 0, 0);
        }
    }

    // ---- interaction --------------------------------------------------------

    public static boolean isGrass(ItemStack stack) {
        return stack.is(ModItems.HANDFUL_OF_GRASS.get()) || stack.is(ModItems.DICED_GRASS.get())
                || stack.is(ModItems.BURNT_GRASS.get()) || stack.is(ModItems.ROLLED_JOINT.get())
                || stack.is(Blocks.GRASS.asItem()) || stack.is(Blocks.TALL_GRASS.asItem())
                || stack.is(Blocks.GRASS_BLOCK.asItem());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean joint = held.is(ModItems.ROLLED_JOINT.get());

        if (isGrass(held)) {
            if (!level().isClientSide) {
                setStash(getStash() + GIFT);
                this.entityData.set(RAGING, false);
                setTarget(null);
                getNavigation().stop();
                if (joint) {
                    setHigh(HIGH_DUR);
                    playSound(ModSounds.EVT_OG.get(), 1.0F, 1.0F);
                } else {
                    playSound(ModSounds.BARBARA_IDLE.get(), 1.0F, 1.0F);
                }
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // empty hand = recruit her for the questline
        if (held.isEmpty()) {
            if (!level().isClientSide) {
                boolean advanced = Quests.complete(player, Quests.RECRUIT_BARBARA);
                playSound(ModSounds.BARBARA_IDLE.get(), 1.0F, 1.0F);
                if (!advanced) {
                    boolean already = Quests.isDone(player, Quests.RECRUIT_BARBARA);
                    player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                            + (already
                                    ? "Barbara: \"I'm already with you, sugar.\""
                                    : "Barbara: \"Bring me some grass first, then come back.\"")));
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ---- combat / flavour ---------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.graceTicks > 0) {
            return false;
        }
        if (isPet() && (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE))
                && com.barbarajones.apocalypse.KraveApocalypse.isActiveNear(level(), position())) {
            return false;
        }
        boolean took = super.hurt(source, amount);
        // Blowback answers a thing that is standing right there and can still be
        // smoked in the face - never a fall, a cactus or a stray arrow's owner.
        if (took && !level().isClientSide && source.getDirectEntity() instanceof LivingEntity attacker) {
            this.combat.onHurtBy(attacker);
        }
        return took;
    }

    /** Make her untouchable for a while (used on post-death respawn). */
    public void grantGrace(int ticks) {
        this.graceTicks = Math.max(this.graceTicks, ticks);
        this.invulnerableTime = 20;
        setInvulnerable(true);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            // lots of shoving, not much harm - unless she is dry, in which case
            // the shove is all she has and it goes in with everything behind it
            double shove = isDry() ? 0.8D : 0.35D;
            double yaw = Math.toRadians(getYRot());
            target.push(-Math.sin(yaw) * shove, isDry() ? 0.3D : 0.12D, Math.cos(yaw) * shove);
            target.hurtMarked = true;
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.BARBARA_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BARBARA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BARBARA_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stash", getStash());
        tag.putBoolean("Raging", isRaging());
        tag.putFloat("Scale", getScale());
        tag.putInt("High", getHigh());
        tag.putBoolean("Pet", isPet());
        if (this.petOwner != null) {
            tag.putUUID("PetOwner", this.petOwner);
        }
        this.combat.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStash(tag.getInt("Stash"));
        this.entityData.set(RAGING, tag.getBoolean("Raging"));
        this.wasRaging = isRaging();
        float s = tag.getFloat("Scale");
        this.entityData.set(SCALE, s <= 0.0F ? SCALE_CALM : s);
        setHigh(tag.getInt("High"));
        this.entityData.set(PET, tag.getBoolean("Pet"));
        if (tag.hasUUID("PetOwner")) {
            this.petOwner = tag.getUUID("PetOwner");
        }
        this.combat.load(tag);
    }
}
