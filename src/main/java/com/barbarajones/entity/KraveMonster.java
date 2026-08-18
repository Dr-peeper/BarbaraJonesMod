package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * THE KRAVE MONSTER - an overweight kid in a purple galaxy hoodie, and the
 * mod's first boss. Iron-golem health so he dies fast, but he jumps absurdly
 * high and teleports like an enderman on a sugar rush, leaving a trail of
 * after-images (see the renderer). Landing a hit is the hard part.
 */
public class KraveMonster extends Monster {

    /** Length of the after-image trail rendered by the client. */
    public static final int GHOSTS = 10;
    public final Vec3[] ghostPos = new Vec3[GHOSTS];
    public final float[] ghostYaw = new float[GHOSTS];
    public int ghostHead = 0;
    public boolean ghostFilled = false;

    /**
     * 0 = all fours, 1 = reared up on its hind legs. Eased server-side toward a
     * target each tick (see updateStance) and synced to the client, where
     * KraveMonsterModel blends the whole pose - spine angle, leg swing, the lot -
     * between the two stances by this amount.
     */
    private static final EntityDataAccessor<Float> DATA_REAR =
            SynchedEntityData.defineId(KraveMonster.class, EntityDataSerializers.FLOAT);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(Component.literal("The Krave Monster"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    private int teleportTimer;

    public KraveMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        for (int i = 0; i < GHOSTS; i++) {
            this.ghostPos[i] = Vec3.ZERO;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_REAR, 0.0F);
    }

    /** Client-side, partial-tick-safe: use in setupAnim. */
    public float getRearAmount(float partialTick) {
        return this.entityData.get(DATA_REAR);
    }

    private void updateStance() {
        LivingEntity target = getTarget();
        boolean shouldRear = target != null
                && (distanceToSqr(target) < 9.0D * 9.0D || this.getDeltaMovement().y > 0.5D);
        float current = this.entityData.get(DATA_REAR);
        float next = Mth.clamp(current + (shouldRear ? 0.05F : -0.04F), 0.0F, 1.0F);
        if (next != current) {
            this.entityData.set(DATA_REAR, next);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)          // iron golem
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.5D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- boss bar -----------------------------------------------------------

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ---- tick ---------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        pushGhost();

        if (level().isClientSide) {
            return;
        }
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
        updateStance();

        LivingEntity target = getTarget();

        // absurd hops toward the target
        if (target != null && onGround() && this.random.nextInt(24) == 0) {
            Vec3 to = target.position().subtract(position());
            double len = Math.sqrt(to.x * to.x + to.z * to.z);
            if (len > 0.01D) {
                setDeltaMovement(to.x / len * 0.55D, 0.95D, to.z / len * 0.55D);
            } else {
                setDeltaMovement(getDeltaMovement().x, 0.95D, getDeltaMovement().z);
            }
            playSound(ModSounds.KRAVE_ROAR.get(), 1.0F, 1.2F);
        }

        // blink around constantly - the annoying part
        if (--this.teleportTimer <= 0) {
            this.teleportTimer = 30 + this.random.nextInt(40);
            if (target != null) {
                for (int i = 0; i < 8 && !teleportRandomly(); i++) {
                    // keep trying
                }
            }
        }
    }

    private void pushGhost() {
        this.ghostPos[this.ghostHead] = position();
        this.ghostYaw[this.ghostHead] = this.yBodyRot;
        this.ghostHead = (this.ghostHead + 1) % GHOSTS;
        if (this.ghostHead == 0) {
            this.ghostFilled = true;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // blink away half the time you land a hit. rude.
        if (!level().isClientSide && source.getEntity() != null && this.random.nextBoolean()) {
            for (int i = 0; i < 16 && !teleportRandomly(); i++) {
                // keep trying
            }
        }
        return super.hurt(source, amount);
    }

    private boolean teleportRandomly() {
        double x = getX() + (this.random.nextDouble() - 0.5D) * 16.0D;
        double y = getY() + (this.random.nextInt(12) - 6);
        double z = getZ() + (this.random.nextDouble() - 0.5D) * 16.0D;
        return blinkTo(x, y, z);
    }

    private boolean blinkTo(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > level().getMinBuildHeight()
                && !level().getBlockState(pos).blocksMotion()) {
            pos.move(0, -1, 0);
        }
        if (!level().getBlockState(pos).blocksMotion()) {
            return false;
        }
        Vec3 from = position();
        boolean ok = randomTeleport(x, pos.getY() + 1.0D, z, false);
        if (ok) {
            level().playSound(null, from.x, from.y, from.z, ModSounds.KRAVE_SCREECH.get(),
                    getSoundSource(), 0.8F, 1.4F);
            playSound(ModSounds.KRAVE_SCREECH.get(), 0.8F, 1.4F);
            for (int i = 0; i < 48; i++) {
                double t = i / 47.0D;
                level().addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        from.x + (getX() - from.x) * t + (this.random.nextDouble() - 0.5D) * 2.0D,
                        from.y + (getY() - from.y) * t + this.random.nextDouble() * getBbHeight(),
                        from.z + (getZ() - from.z) * t + (this.random.nextDouble() - 0.5D) * 2.0D,
                        (this.random.nextFloat() - 0.5F) * 0.2F,
                        (this.random.nextFloat() - 0.5F) * 0.2F,
                        (this.random.nextFloat() - 0.5F) * 0.2F);
            }
        }
        return ok;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.KRAVE_CEREAL.get(), 3 + this.random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.DIAMOND, 1 + this.random.nextInt(2)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? ModSounds.KRAVE_LAUGH.get() : ModSounds.KRAVE_ROAR.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KRAVE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KRAVE_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
