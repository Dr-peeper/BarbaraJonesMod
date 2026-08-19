package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
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

import java.util.EnumSet;

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
    /** Which time round this is: 1 through 4. Synced so the renderer can grow him. */
    private static final EntityDataAccessor<Integer> FORM =
            SynchedEntityData.defineId(KraveMonster.class, EntityDataSerializers.INT);

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
        this.entityData.define(FORM, 1);
    }

    /** Client-side, partial-tick-safe: use in setupAnim. */
    public float getRearAmount(float partialTick) {
        return this.entityData.get(DATA_REAR);
    }

    private void updateStance() {
        LivingEntity target = getTarget();
        // Widened from 9x9 so he spends more time reared-up (taller silhouette)
        // near a player - reinforces the bigger-scale fix behaviorally too.
        boolean shouldRear = target != null
                && (distanceToSqr(target) < 14.0D * 14.0D || this.getDeltaMovement().y > 0.5D);
        float current = this.entityData.get(DATA_REAR);
        float next = Mth.clamp(current + (shouldRear ? 0.05F : -0.04F), 0.0F, 1.0F);
        if (next != current) {
            this.entityData.set(DATA_REAR, next);
        }
    }

    /** Highest Cayden tier this fight has seen, so he never shrinks mid-duel. */
    /** He cannot escalate past this. */
    public static final int FINAL_FORM = 4;

    private int matchedTier;
    private int duelBlink;


    public int getForm() {
        return Math.max(1, Math.min(FINAL_FORM, this.entityData.get(FORM)));
    }

    /**
     * Sets which incarnation this is and rebuilds him around it.
     *
     * <p>Each form is a genuine step up rather than a health bar with a bigger
     * number: he hits harder, moves faster and is physically larger, so you can
     * see across a field which one you are dealing with.
     */
    public void setForm(int form) {
        int f = Math.max(1, Math.min(FINAL_FORM, form));
        this.entityData.set(FORM, f);

        double health = switch (f) {
            case 4 -> 1600.0D;
            case 3 -> 800.0D;
            case 2 -> 380.0D;
            default -> 160.0D;
        };
        double attack = switch (f) {
            case 4 -> 22.0D;
            case 3 -> 15.0D;
            case 2 -> 9.0D;
            default -> 5.0D;
        };
        double speed = switch (f) {
            case 4 -> 0.62D;
            case 3 -> 0.50D;
            case 2 -> 0.40D;
            default -> 0.32D;
        };

        var maxHp = getAttribute(Attributes.MAX_HEALTH);
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (maxHp != null) {
            maxHp.setBaseValue(health);
        }
        if (atk != null) {
            atk.setBaseValue(attack);
        }
        if (spd != null) {
            spd.setBaseValue(speed);
        }
        setHealth(getMaxHealth());
    }

    /** The name shown on the boss bar, so the escalation is legible. */
    public net.minecraft.network.chat.Component formTitle() {
        String suffix = switch (getForm()) {
            case 4 -> " - FINAL FORM";
            case 3 -> " - THIRD FORM";
            case 2 -> " - SECOND FORM";
            default -> "";
        };
        return net.minecraft.network.chat.Component.literal("The Krave Monster" + suffix);
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
        this.goalSelector.addGoal(0, new RivalDuelGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.5D, true));
        this.goalSelector.addGoal(1, new MouthBeamGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // He answers Cayden directly. Without this he only ever hunted players,
        // so the duel the whole mod builds toward never actually started.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, CaydenCobb.class, true));
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
        if (!level().isClientSide) {
            matchRival();
        }
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
                KraveBlink.tryRandomBlink(this, this.random, 16.0D, 8, 20, 8, ModSounds.KRAVE_SCREECH.get());
            }
        }
    }

    /**
     * His signature move is jumping absurdly high toward the target (see the
     * "absurd hops" block above) - now that the Kosmos has real elevation,
     * that lands him from real heights. Taking fall damage from his own
     * jump was never the intent.
     */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    private void pushGhost() {
        this.ghostPos[this.ghostHead] = position();
        this.ghostYaw[this.ghostHead] = this.yBodyRot;
        this.ghostHead = (this.ghostHead + 1) % GHOSTS;
        if (this.ghostHead == 0) {
            this.ghostFilled = true;
        }
    }

    /**
     * Set true while a KraveKosmosBattle is actively fighting this boss.
     * While active, only Super Saiyan Cayden (directly, or via his Krave
     * Lasers - both attribute the hit to him as the damage source's entity)
     * can do meaningful damage; "only Cayden can fight him to the death"
     * meant literally. Outside an active fight, normal rules apply, so a
     * player poking him early while exploring the Kosmos isn't punished.
     */
    private boolean bossFightActive;

    public void setBossFightActive(boolean active) {
        this.bossFightActive = active;
    }

    /**
     * Scales him to whatever Cayden has turned into.
     *
     * <p>He is built as a 100-health boss, which an Ultra Instinct Cayden at
     * twelve times damage deletes in about two hits. Rather than nerf Cayden -
     * the transformations are the point - the Monster grows to match, and is
     * healed to full when he does so the fight starts properly rather than with
     * a chunk already missing.
     *
     * <p>He never scales back down within a fight: dropping his maximum health
     * mid-duel would leave him instantly near-dead the moment Cayden powered
     * down for a tick.
     */
    private void matchRival() {
        // Superseded by the form ladder: his strength now comes from which
        // incarnation he is, not from what Cayden happens to have turned into.
        if (true) {
            return;
        }
        int tier = 0;
        if (getTarget() instanceof CaydenCobb c && c.isAlive()) {
            tier = Math.max(c.getTier(), c.isSuperSaiyan() ? 1 : 0);
        }
        if (tier <= this.matchedTier) {
            return;
        }
        this.matchedTier = tier;

        double health = switch (tier) {
            case 3 -> 1800.0D;
            case 2 -> 900.0D;
            default -> 400.0D;
        };
        double attack = switch (tier) {
            case 3 -> 24.0D;
            case 2 -> 16.0D;
            default -> 10.0D;
        };
        double speed = switch (tier) {
            case 3 -> 0.70D;
            case 2 -> 0.55D;
            default -> 0.45D;
        };

        var maxHp = getAttribute(Attributes.MAX_HEALTH);
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        if (maxHp != null) {
            maxHp.setBaseValue(health);
        }
        if (atk != null) {
            atk.setBaseValue(attack);
        }
        if (spd != null) {
            spd.setBaseValue(speed);
        }
        setHealth(getMaxHealth());

        playSound(ModSounds.KRAVE_ROAR.get(), 2.0F, 0.6F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    getX(), getY() + 1.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(64.0D))) {
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    net.minecraft.ChatFormatting.DARK_PURPLE + ""
                    + net.minecraft.ChatFormatting.BOLD + "HE MATCHES HIM."));
        }
    }

    /**
     * A blink-strike duel: close instantly, hit, vanish, reappear somewhere
     * else. Runs only against Cayden, so ordinary players still fight the
     * slower boss they can actually handle.
     */
    static class RivalDuelGoal extends net.minecraft.world.entity.ai.goal.Goal {

        private final KraveMonster boss;
        private int strikeCooldown;

        RivalDuelGoal(KraveMonster boss) {
            this.boss = boss;
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.boss.getTarget() instanceof CaydenCobb c && c.isAlive() && c.isSuperSaiyan();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity foe = this.boss.getTarget();
            if (foe == null) {
                return;
            }
            this.boss.getLookControl().setLookAt(foe, 60.0F, 60.0F);

            double dist = this.boss.distanceTo(foe);

            // Out of reach: blink straight onto him rather than walking over.
            if (dist > 4.0D) {
                if (--this.boss.duelBlink <= 0) {
                    this.boss.duelBlink = 8;
                    Vec3 at = foe.position();
                    double ang = this.boss.getRandom().nextDouble() * Math.PI * 2.0D;
                    this.boss.blinkNear(at.x + Math.cos(ang) * 2.5D, at.y,
                            at.z + Math.sin(ang) * 2.5D);
                } else {
                    // and close hard in between, so he is never simply standing
                    Vec3 to = foe.position().subtract(this.boss.position());
                    double len = Math.max(0.4D, to.length());
                    this.boss.setDeltaMovement(this.boss.getDeltaMovement().scale(0.7D)
                            .add(to.scale(0.28D / len)));
                }
                return;
            }

            if (--this.strikeCooldown <= 0) {
                this.strikeCooldown = 11;
                this.boss.doHurtTarget(foe);
                this.boss.playSound(ModSounds.KRAVE_BOOM.get(), 1.0F, 1.5F);
                // recoil apart so the duel keeps moving instead of grinding
                Vec3 away = this.boss.position().subtract(foe.position());
                double len = Math.max(0.5D, away.length());
                this.boss.setDeltaMovement(away.scale(0.55D / len).add(0.0D, 0.25D, 0.0D));
                this.boss.hurtMarked = true;
            }
        }
    }

    /** Public wrapper so the duel goal can reposition him. */
    void blinkNear(double x, double y, double z) {
        KraveBlink.blinkTo(this, x, y, z, ModSounds.KRAVE_SCREECH.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KraveForm", getForm());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("KraveForm")) {
            setForm(tag.getInt("KraveForm"));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // blink away half the time you land a hit. rude.
        if (!level().isClientSide && source.getEntity() != null && this.random.nextBoolean()) {
            KraveBlink.tryRandomBlink(this, this.random, 16.0D, 6, 6, 16, ModSounds.KRAVE_SCREECH.get());
        }
        float applied = amount;
        if (this.bossFightActive
                && !(source.getEntity() instanceof CaydenCobb cayden && cayden.isSuperSaiyan())) {
            applied = amount * 0.05F;
        }
        return super.hurt(source, applied);
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

    /**
     * The mouth beam: a blue "kamehameha"-style bolt fired at players beyond
     * melee range. Claims NO goal flags (not even LOOK) so it can never
     * conflict with anything else in the selector - it was originally
     * LOOK-only, but vanilla's MeleeAttackGoal (registered at the same
     * priority) also claims LOOK, which made the two goals mutually
     * exclusive and starved this one down to firing once. setLookAt() below
     * doesn't need the flag - that's just a normal API call, not gated by
     * goal-selector exclusivity.
     */
    static class MouthBeamGoal extends Goal {
        private static final double MIN_RANGE = 5.0D;
        private static final double MAX_RANGE_SQR = 24.0D * 24.0D;

        private final KraveMonster monster;
        private int cooldown;

        MouthBeamGoal(KraveMonster monster) {
            this.monster = monster;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.monster.getTarget();
            return target instanceof Player
                    && target.isAlive()
                    && this.monster.distanceToSqr(target) > MIN_RANGE * MIN_RANGE
                    && this.monster.distanceToSqr(target) < MAX_RANGE_SQR
                    && this.monster.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = this.monster.getTarget();
            if (target == null) {
                return;
            }
            this.monster.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.cooldown <= 0) {
                this.cooldown = 60 + this.monster.random.nextInt(40);
                fireBeam(target);
            }
        }

        private void fireBeam(LivingEntity target) {
            if (!(this.monster.level() instanceof ServerLevel)) {
                return;
            }
            // Mouth origin computed purely from server-visible entity state -
            // deliberately not from KraveMonsterModel/KraveMonsterRenderer,
            // which are client-only classes a server-executed Goal must not
            // touch (risks a dedicated-server classloading crash).
            Vec3 from = this.monster.position()
                    .add(0.0D, this.monster.getBbHeight() * 0.75D, 0.0D)
                    .add(this.monster.getViewVector(1.0F).scale(this.monster.getBbWidth() * 0.6D));
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            KraveMouthBeam beam = new KraveMouthBeam(this.monster.level(), this.monster, from, aim);
            this.monster.level().addFreshEntity(beam);
            this.monster.level().playSound(null, this.monster.blockPosition(),
                    ModSounds.KRAVE_BEAM_FIRE.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        }
    }
}
