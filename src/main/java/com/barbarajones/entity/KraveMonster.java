package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.boss.krave.KraveBattleState;
import com.barbarajones.progression.AscensionLadder;

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

import javax.annotation.Nullable;

import java.util.EnumSet;

/**
 * THE KRAVE MONSTER - an overweight kid in a purple galaxy hoodie, and the
 * mod's first boss. Iron-golem health so he dies fast, but he jumps absurdly
 * high and teleports like an enderman on a sugar rush, leaving a trail of
 * after-images (see the renderer). Landing a hit is the hard part.
 */
public class KraveMonster extends Monster {

    /** Boss-phase logging. One line per state change, never per tick. */
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

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
    /** True once his form has been decided, so it is settled exactly once. */
    private boolean formSettled;

    /** Which time round this is: 1 through 4. Synced so the renderer can grow him. */
    private static final EntityDataAccessor<Integer> FORM =
            SynchedEntityData.defineId(KraveMonster.class, EntityDataSerializers.INT);

    /**
     * Where the encounter is, as a {@link KraveBattleState} ordinal.
     *
     * <p>Synced because the client draws the QTE prompt off it, and written to
     * NBT because a boss fight that forgets which phase it was in when the
     * chunk unloads is worse than one that never started.
     */
    private static final EntityDataAccessor<Integer> BATTLE =
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
        this.entityData.define(BATTLE, KraveBattleState.DORMANT.ordinal());
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

    /** He cannot escalate past this. */
    // Six incarnations now, one per rung of Cayden's ladder (SSJ through
    // Ultra Instinct - see KRAVE_FORM_DEMAND in CaydenCobb) instead of four,
    // so nothing between SSJ2 and GOD gets skipped over.
    public static final int FINAL_FORM = 7;

    private int duelBlink;


    public int getForm() {
        return Math.max(1, Math.min(FINAL_FORM, this.entityData.get(FORM)));
    }

    public KraveBattleState getBattleState() {
        return KraveBattleState.byId(this.entityData.get(BATTLE));
    }

    /**
     * Moves the encounter to a new phase.
     *
     * <p>Setting the same state twice is a no-op rather than a duplicate log
     * line and a second round of transition effects. The controller ticks
     * twenty times a second, so anything that re-fires on re-entry would fire
     * continuously for as long as the phase lasted.
     *
     * <p>Logged once per change, which is the level of detail that is actually
     * useful when a fight goes wrong: enough to reconstruct the sequence,
     * never per tick.
     */
    public void setBattleState(KraveBattleState next) {
        KraveBattleState now = getBattleState();
        if (now == next) {
            return;
        }
        this.entityData.set(BATTLE, next.ordinal());
        if (!level().isClientSide) {
            LOGGER.info(
                    "Krave Monster {}: {} -> {} (form {} of {})",
                    getUUID(), now, next, getForm(), FINAL_FORM);
        }
        // The old boolean is kept in step rather than left to rot: other systems
        // still read it, and two sources of truth that can disagree is the exact
        // problem the state machine exists to remove.
        this.bossFightActive = next.hostile() || next.scripted();
    }

    /**
     * The share of a form health bar at which the fight stops being a fight.
     *
     * <p>Forms do not end by dying. At this threshold normal damage stops
     * landing and the finisher takes over, which is what stops Cayden ending a
     * phase with one lucky hit before the player is ever asked to help.
     */
    public static final float FINISHER_AT = 0.15F;

    public boolean atFinisherThreshold() {
        return getHealth() <= getMaxHealth() * FINISHER_AT;
    }


    /**
     * Wakes a summoned Monster so the confrontation can pick him up.
     *
     * <p>The Krave Box and the tenth Cayden death each summon their own
     * Monster, and those should still start a fight rather than standing there.
     * They no longer start it by jumping straight to COMBAT, which is what made
     * a summoned boss skip the confrontation, the form ladder and the prompt:
     * he simply stays DORMANT and the confrontation trigger finds him like any
     * other, running the same opening every other encounter gets.
     */
    public void spawnHostile() {
        setBattleState(KraveBattleState.DORMANT);
    }

    /** Puts a form back on its feet for the next phase. */
    public void restoreForPhase() {
        setHealth(getMaxHealth());
        this.hurtTime = 0;
        this.invulnerableTime = 0;
    }

    /**
     * Sets which incarnation this is and rebuilds him around it.
     *
     * <p>Each form is a genuine step up rather than a health bar with a bigger
     * number: he hits harder, moves faster and is physically larger, so you can
     * see across a field which one you are dealing with. These are the
     * standing/idle baseline numbers, and now the only ones.
     *
     * <p>matchRival() used to re-scale health, attack, speed AND form live
     * against Cayden's measured damage output every three seconds - a second
     * owner of everything this method sets, ratcheting upward and never down.
     * It topped his CURRENT health up each time it raised his maximum, so he
     * regained health faster than an early-form Cayden could take it off and
     * his bar settled at a level it would not drop below. And because the
     * finisher threshold is a share of a maximum that kept growing, the prompt
     * never fired either. The form table is the single owner now.
     */
    public void setForm(int form) {
        this.formSettled = true;
        int f = Math.max(1, Math.min(FINAL_FORM, form));
        this.entityData.set(FORM, f);
        // Without this the collision box keeps the PREVIOUS form's size until
        // something else happens to invalidate it, so he grows visually and
        // stays hittable only where he used to be.
        refreshDimensions();

        // Roughly tripled across the board, and steepened toward the top.
        // Cayden at rung N against form N deals enough that the old numbers were
        // gone in seconds - the player watched six health bars evaporate and
        // never got to participate in any of them. Each form now has to be
        // fought down to its finisher threshold rather than deleted.
        double health = switch (f) {
            case 7 -> 32000.0D;  // THE KRAVE GOD
            case 6 -> 15000.0D;  // Overload
            case 5 -> 10000.0D;  // Milk abomination
            case 4 -> 6500.0D;   // Swarm
            case 3 -> 4000.0D;   // Double chocolate
            case 2 -> 2000.0D;   // Chocolate-filled
            default -> 900.0D;   // Awakening
        };
        double attack = switch (f) {
            case 7 -> 110.0D;
            case 6 -> 78.0D;
            case 5 -> 60.0D;
            case 4 -> 46.0D;
            case 3 -> 32.0D;
            case 2 -> 22.0D;
            default -> 14.0D;
        };
        double speed = switch (f) {
            // Health and attack both got a case 7 when he grew from six forms to
            // seven. This did not, so the KRAVE GOD fell through to default and
            // fought at 0.32 - the slowest speed in the table, shared with his
            // weakest form. The final boss was the easiest thing in the fight to
            // walk away from.
            case 7 -> 0.90D;
            case 6 -> 0.82D;
            case 5 -> 0.74D;
            case 4 -> 0.66D;
            case 3 -> 0.55D;
            case 2 -> 0.42D;
            default -> 0.32D;
        };

        // A boss with no armour and no knockback resistance is a punching bag
        // with a large health pool: he was being staggered out of his own
        // windups and shoved around the arena by the thing he is meant to
        // overpower. Knockback resistance reaches 1.0 by form four - past that
        // point nothing moves him at all, which is most of what makes the late
        // forms feel like a different creature.
        double armour = 4.0D + f * 3.0D;
        double tough = f * 1.5D;
        // Climbs from the 0.7 the supplier already gave him to immovable at the
        // top. Starting the curve lower would have been a downgrade for form
        // one, which is the sort of buff that quietly makes things worse.
        double knockRes = Math.min(1.0D, 0.70D + 0.05D * f);
        // How hard HIS hits throw. ATTACK_SPEED is a player-only attribute and
        // mobs do not read it, so pace comes from the moveset cooldowns and from
        // this: a hit that sends you across the courtyard is pressure in a way
        // that a bigger damage number on its own is not.
        double knockDealt = 0.4D + f * 0.35D;

        var maxHp = getAttribute(Attributes.MAX_HEALTH);
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = getAttribute(Attributes.MOVEMENT_SPEED);
        var arm = getAttribute(Attributes.ARMOR);
        var armTough = getAttribute(Attributes.ARMOR_TOUGHNESS);
        var kb = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        var kbDealt = getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (arm != null) {
            arm.setBaseValue(armour);
        }
        if (armTough != null) {
            armTough.setBaseValue(tough);
        }
        if (kb != null) {
            kb.setBaseValue(knockRes);
        }
        if (kbDealt != null) {
            kbDealt.setBaseValue(knockDealt);
        }
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
        // The bar is how the player reads the escalation, so it has to follow
        // the form rather than staying on the name it was constructed with.
        this.bossEvent.setName(formTitle());
    }

    /**
     * There is no per-form model or texture (that needs real art, not code),
     * so the escalation between forms has to come from silhouette (see the
     * per-form scale in KraveMonsterRenderer), a body-color tint (also in the
     * renderer, via shader color) and this: a standing particle aura that
     * gets heavier and nastier with each form. Form 1/2 have none - they're
     * "just" a big monster. Form 3 wreathes him in red. Form 4 is supposed to
     * be the scariest thing you've fought - thick soul-fire and smoke.
     */
    private void formAura() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        int form = getForm();
        if (this.tickCount % 3 != 0) {
            return;
        }
        double r = getBbWidth() * 0.6D;
        double h = getBbHeight();
        int count = switch (form) {
            case 6 -> 14;
            case 5 -> 10;
            case 4 -> 7;
            case 3 -> 4;
            case 2 -> 2;
            default -> 1;
        };
        for (int i = 0; i < count; i++) {
            double ang = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = this.random.nextDouble() * r;
            double x = getX() + Math.cos(ang) * dist;
            double z = getZ() + Math.sin(ang) * dist;
            double y = getY() + this.random.nextDouble() * h;
            switch (form) {
                case 6 -> {
                    // Ultra: a cold white-blue corona, sharp and constant -
                    // the scariest of the set, and the quietest looking.
                    sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                                    new org.joml.Vector3f(0.85F, 0.92F, 1.0F), 2.0F),
                            x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.0D);
                    if (this.random.nextInt(4) == 0) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                x, y, z, 1, 0.01D, 0.02D, 0.01D, 0.01D);
                    }
                }
                case 5 -> {
                    // Blue: a corrupted mirror of Cayden's own Blue - cold
                    // flame instead of warm fire.
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.01D);
                    sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                                    new org.joml.Vector3f(0.25F, 0.4F, 1.0F), 1.8F),
                            x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.0D);
                }
                case 4 -> {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.01D);
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            x, y, z, 1, 0.03D, 0.06D, 0.03D, 0.005D);
                }
                case 3 -> sl.sendParticles(
                        new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(1.0F, 0.15F, 0.1F), 1.6F),
                        x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.0D);
                // Forms 1-2 read as "just a big monster" no longer - a plain
                // gold spark, thin and infrequent, the same family as
                // Cayden's own SSJ/SSJ2 color so the very first transformation
                // already reads as an escalation rather than nothing at all.
                default -> sl.sendParticles(
                        new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(1.0F, 0.85F, 0.25F), 1.2F),
                        x, y, z, 1, 0.02D, 0.05D, 0.02D, 0.0D);
            }
        }
    }

    /** The name shown on the boss bar, so the escalation is legible. */
    public net.minecraft.network.chat.Component formTitle() {
        String suffix = switch (getForm()) {
            case 7 -> " - THE KRAVE GOD";
            case 6 -> " - OVERLOAD";
            case 5 -> " - BLUE FORM";
            case 4 -> " - GOD FORM";
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
                // Both re-scaled per form in setForm. Declared here so the
                // attribute map is guaranteed to carry them rather than relying
                // on what the parent supplier happens to include.
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new RivalDuelGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.5D, true));
        // The full moveset. Above the plain melee and beam goals, which stay as
        // the between-attacks filler rather than being what he does.
        this.goalSelector.addGoal(0, new com.barbarajones.boss.krave.KraveMovesetGoal(this));
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

    /**
     * Refuses Cayden as a target until the encounter has actually begun.
     *
     * <p>The mirror of the same rule on Cayden. His own targeting goal takes
     * CaydenCobb.class directly, so before this he would acquire him while
     * dormant and swing - and the confrontation scanner blanked that target once
     * a second, so he attacked, lost the target mid-swing, re-acquired, and
     * never landed anything. Enforced here it simply never happens.
     *
     * <p>Players are deliberately still fair game while he is dormant. Walking
     * up and hitting him should still be a bad idea; he is a hostile mob, not a
     * statue.
     */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof CaydenCobb && !getBattleState().hostile()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
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
        if (!level().isClientSide && !this.formSettled) {
            // Form one, dormant, always - unless NBT already said otherwise, in
            // which case readAdditionalSaveData has set formSettled and this
            // never runs.
            //
            // A body with no saved encounter on it is a new encounter. It does
            // not inherit where the last one got to, from the player's progress
            // or from anything else: the ladder is the content, and a boss that
            // opens at form five has skipped four fifths of it.
            setForm(1);
            this.formSettled = true;
        }
        if (!level().isClientSide) {
            tickGauntletReset();
            tickArenaAnchor();
        }
        pushGhost();

        if (level().isClientSide) {
            return;
        }
        this.bossEvent.setProgress(getHealth() / getMaxHealth());
        updateStance();
        formAura();

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
            playSound(roarSound(), 1.0F, 1.2F);
        }

        // blink around constantly - the annoying part
        if (--this.teleportTimer <= 0) {
            this.teleportTimer = 30 + this.random.nextInt(40);
            if (target != null) {
                KraveBlink.tryRandomBlink(this, this.random, 16.0D, 8, 20, 8, screechSound());
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
     * The height his attacks are allowed to dig down to, remembered from where
     * he first stood.
     *
     * <p>His moves tear the arena apart on purpose, and the Kosmos is floating
     * islands over open void. Measured against his CURRENT feet the floor sinks
     * with him as the ground gives way, so the island gets dug out from under
     * the fight one attack at a time until everyone falls out of the world.
     * Anchored to where the fight began, the arena erodes to a scarred plate
     * and stops there.
     *
     * <p>Set lazily rather than at spawn: he can be summoned in mid-air or on a
     * pillar he then steps off, and the height that matters is the one he is
     * actually fighting at.
     */
    private int arenaFloor = Integer.MAX_VALUE;

    /** Grounded ticks, not necessarily in a row, before the anchor is left alone. */
    private static final int ARENA_SETTLE = 20;
    private int arenaSettleTicks;

    public int arenaFloor() {
        if (this.arenaFloor == Integer.MAX_VALUE) {
            this.arenaFloor = net.minecraft.util.Mth.floor(getY()) - 8;
        }
        return this.arenaFloor;
    }

    /**
     * Re-anchors the floor while he is still settling onto the arena.
     *
     * <p>A boss that spawns a few blocks up and falls would otherwise anchor to
     * his spawn height and protect a slab of empty air above the ground,
     * leaving his attacks unable to scratch the surface he stands on.
     *
     * <p>Grounded ticks are counted cumulatively rather than consecutively, and
     * that distinction is the whole safety of it. Requiring an unbroken run he
     * never gets - he jumps constantly - the anchor would keep following him,
     * and since each re-anchor sits eight blocks under wherever he landed, and
     * he lands lower every time his own craters take the ground away, the arena
     * would walk itself into the void one attack at a time. Counted this way it
     * settles once, early, and never moves again.
     */
    private void tickArenaAnchor() {
        if (this.arenaSettleTicks >= ARENA_SETTLE) {
            return;
        }
        if (onGround()) {
            this.arenaSettleTicks++;
            this.arenaFloor = net.minecraft.util.Mth.floor(getY()) - 8;
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
                    // He arrives THROUGH whatever was standing there. Tied to
                    // the blink rather than to the tick, so the wreckage marks
                    // where the fight actually went.
                    this.boss.smashLanding();
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
                // Swings faster the further he has escalated. Mobs ignore the
                // ATTACK_SPEED attribute entirely, so pace has to come from the
                // goal that owns the swing.
                this.strikeCooldown = Math.max(4, 12 - this.boss.getForm());
                this.boss.doHurtTarget(foe);
                this.boss.playSound(ModSounds.COMBAT_HEAVY_HIT.get(), 1.2F, 0.9F);
                this.boss.smashStrike(foe);
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
        KraveBlink.blinkTo(this, x, y, z, screechSound());
    }

    /**
     * The ground he lands on stops being ground.
     *
     * <p>Scaled to how big he currently is and routed through the shared
     * demolition, so it obeys the same per-tick budget, the same arena floor and
     * the same castle protection as every other destructive thing he does. A
     * second independent way of removing blocks is a second way to accidentally
     * delete the fortress.
     */
    void smashLanding() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        double r = 2.0D + getBbWidth() * 0.35D;
        com.barbarajones.boss.krave.KraveDemolition.crater(sl, this, position(), r, 2);
        sl.playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(),
                net.minecraft.sounds.SoundSource.HOSTILE, 1.3F, 0.7F);
    }

    /**
     * A connecting hit throws its victim through whatever is behind them.
     *
     * <p>The knockback comes from ATTACK_KNOCKBACK on the hit itself; this is
     * the terrain half - the wall they are about to be driven into is cleared
     * ahead of them, so being hit by him relocates you rather than parking you
     * against the nearest block.
     */
    void smashStrike(LivingEntity foe) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 through = foe.position().subtract(position()).normalize().scale(3.0D);
        com.barbarajones.boss.krave.KraveDemolition.carve(sl, this,
                foe.position().add(through), 2.5D + getForm() * 0.4D, 3, 0,
                com.barbarajones.boss.krave.KraveDemolition.BUDGET_LIGHT);
    }

    /** Forms 4+ (the ones with no dedicated model/texture to lean on) get the newer, harsher variants instead of reusing what forms 1-3 already sound like. */
    SoundEvent roarSound() {
        return getForm() >= 4 ? ModSounds.MONSTER_ROAR_2.get() : ModSounds.KRAVE_ROAR.get();
    }

    SoundEvent screechSound() {
        return getForm() >= 4 ? ModSounds.MONSTER_SCREECH_2.get() : ModSounds.KRAVE_SCREECH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KraveForm", getForm());
        tag.putInt("KraveBattleState", getBattleState().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("KraveForm")) {
            setForm(tag.getInt("KraveForm"));
        } else {
            this.formSettled = false;
        }
        if (tag.contains("KraveBattleState")) {
            KraveBattleState saved = KraveBattleState.byId(tag.getInt("KraveBattleState"));
            // A fight interrupted mid-cinematic cannot resume from the middle of
            // one: the thrown player, the held positions and the pending
            // scheduled effects are all gone. Rewind to the combat for that form
            // and let it reach the finisher again, rather than restoring a
            // scripted state with nothing left to drive it and wedging the
            // encounter permanently.
            this.entityData.set(BATTLE, (saved.scripted() ? KraveBattleState.COMBAT : saved).ordinal());
            this.bossFightActive = getBattleState().hostile();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Nothing lands outside actual combat. Every scripted beat - the
        // confrontation, the prompt, the finisher, the transition - needs the
        // fight to hold still, and a laser already in flight does not know the
        // phase changed under it. Without this a stray hit can kill him during
        // his own finisher, which skips a form and leaves Cayden ascended in an
        // empty courtyard.
        //
        // Commands and the void still work: those bypass invulnerability, which
        // is deliberately not blocked here - an unkillable boss is a far worse
        // failure than a skipped phase.
        if (!level().isClientSide
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && getBattleState().scripted()) {
            return false;
        }
        // blink away half the time you land a hit. rude.
        if (!level().isClientSide && source.getEntity() != null && this.random.nextBoolean()) {
            KraveBlink.tryRandomBlink(this, this.random, 16.0D, 6, 6, 16, screechSound());
        }
        float applied = amount;
        if (this.bossFightActive
                && !(source.getEntity() instanceof CaydenCobb cayden && cayden.isSuperSaiyan())) {
            applied = amount * 0.05F;
        }
        // A Cayden who cannot match this form fights well and still loses. He is
        // allowed to hurt it - at one tier short he can take it to a sliver, which
        // is the whole point, because a fight lost at 10% teaches you exactly
        // which form you are missing. What he cannot do is finish it. Without the
        // floor he simply grinds any form down eventually and "he cannot beat it"
        // quietly degrades into "it takes him a while".
        if (source.getEntity() instanceof CaydenCobb attacker && attacker.isMortallyOutmatched()) {
            applied *= attacker.outmatchedDealtScale();
            float floor = getMaxHealth() * attacker.outmatchedFloor();
            float headroom = getHealth() - floor;
            if (headroom <= 0.0F) {
                applied = 0.0F;
                if (!level().isClientSide && this.random.nextInt(20) == 0) {
                    playSound(screechSound(), 1.2F, 0.7F);
                }
            } else {
                applied = Math.min(applied, headroom);
            }
        }
        // A form is defeated by its finisher, never by running out of health.
        // Clamping the last hit rather than refusing it keeps the feedback - he
        // still flashes and recoils - while making it impossible to end a phase
        // before the player has been asked to end it.
        //
        // Only for the scripted encounter. A Krave Box summon is a fully
        // independent fight with the old death-driven gauntlet behind it, and
        // clamping that one would leave it unkillable and the gauntlet unable
        // to advance.
        //
        // THE SNAP MATTERS. Clamping `applied` to the headroom is only half of
        // it, and on its own it makes the threshold unreachable rather than
        // reachable: super.hurt applies armour, enchantments and effects AFTER
        // this, so what actually lands is strictly LESS than the headroom, and
        // his health settles strictly ABOVE the floor every single time. The
        // test is health <= floor, so it was never once true - not flakily,
        // but by construction. He hit fifteen percent, stuck there, and the
        // prompt never came, because the clamp meant to create that threshold
        // was the thing preventing it.
        //
        // It only became impossible when he was given armour in the same change
        // as the clamp. Before that the full clamped amount landed and hit the
        // floor exactly, which is the sort of coincidence that hides a bug for
        // exactly as long as it takes to add a defensive stat.
        boolean reachesFloor = false;
        float floor = getMaxHealth() * FINISHER_AT;
        if (!level().isClientSide && getBattleState() == KraveBattleState.COMBAT
                && com.barbarajones.apocalypse.KraveKosmosBattle.isActive(this)) {
            float headroom = getHealth() - floor;
            // Still clamped first, so no single hit can kill him outright
            // whatever it is - the snap below cannot rescue a corpse.
            reachesFloor = headroom > 0.0F && applied >= headroom;
            applied = headroom <= 0.0F ? 0.0F : Math.min(applied, headroom);
        }
        // The scripted encounter also cuts what actually lands. Tripling the
        // health bars was not enough on its own - Cayden ascended puts out
        // enough that a bar he cleared in three seconds he now clears in nine,
        // which is still not a fight. Each form has to last long enough to
        // read as a stage rather than a hiccup, and it has to survive long
        // enough for the player to be given something to do.
        if (!level().isClientSide && getBattleState() == KraveBattleState.COMBAT
                && com.barbarajones.apocalypse.KraveKosmosBattle.isActive(this)) {
            applied *= SCRIPTED_DAMAGE_SCALE;
        }
        boolean landed = super.hurt(source, applied);
        // Put him exactly on the floor when a hit was big enough to reach it,
        // rather than wherever armour happened to leave him. This is what makes
        // atFinisherThreshold() actually become true, and it is done AFTER the
        // hit so it does not care what armour, resistance or absorption did in
        // between - only that the blow was, before mitigation, enough.
        if (reachesFloor && getHealth() > floor) {
            setHealth(floor);
            LOGGER.info("[CraveBoss] Form {} spent: health pinned to the finisher threshold ({} of {}).",
                    getForm(), String.format("%.1f", floor), String.format("%.1f", getMaxHealth()));
        }
        return landed;
    }

    /**
     * How much of a hit lands during the scripted encounter.
     *
     * <p>Was 0.2, which was tuned against Ultra Instinct - where Cayden puts
     * out around a thousand damage a second - without checking what it did at
     * the bottom of the ladder. At form one he is Super Saiyan 1 with an
     * attack multiplier of four and barely any feeding behind it: about
     * thirty-five damage a second, cut to seven. Four healing boxes restore
     * eight. The first form was not hard, it was arithmetically impossible -
     * he gained health faster than he could lose it, forever.
     *
     * <p>0.6 keeps every form a real fight at both ends of that range: roughly
     * a minute at form one, and the top forms still last because their health
     * bars grew rather than because his hits stopped counting.
     */
    private static final float SCRIPTED_DAMAGE_SCALE = 0.6F;

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.KRAVE_CEREAL.get(), 3 + this.random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.DIAMOND, 1 + this.random.nextInt(2)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? ModSounds.KRAVE_LAUGH.get() : roarSound();
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
                // A higher form is a crazier one - he leans on the beam far
                // harder at Ultra than at his very first transformation
                // instead of firing at the same lazy rate regardless of how
                // far the fight has escalated. Form 1: ~3-5s between shots.
                // Form 6: ~1-1.7s, closer to a barrage than an occasional bolt.
                int form = this.monster.getForm();
                int base = Math.max(20, 60 - (form - 1) * 8);
                int variance = Math.max(10, 40 - (form - 1) * 5);
                this.cooldown = base + this.monster.random.nextInt(variance);
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

    /** Ticks with nobody engaging him before the gauntlet resets to form one. */
    private static final int RESET_AFTER = 20 * 30;

    private int unengagedTicks;

    /**
     * Puts him back to his first form once a fight is abandoned.
     *
     * <p>His escalation is persistent - each death revives him one form higher,
     * and that form is written to NBT. That is correct DURING a fight and wrong
     * the moment one ends without him dying: the Kosmos boss is spawned exactly
     * once, ever, so a player who took him to Ultra and then walked out came
     * back to a permanently Ultra-form monster and never saw forms one through
     * five again. The gauntlet is the point of him.
     *
     * <p>So an abandoned fight rewinds. Thirty seconds with no target and nobody
     * within sixty-four blocks and he is back to form one at full health, which
     * is also the ordinary boss-arena convention: leave, and you start over.
     */
    private void tickGauntletReset() {
        if (getForm() <= 1) {
            this.unengagedTicks = 0;
            return;
        }
        boolean engaged = getTarget() != null && getTarget().isAlive();
        if (!engaged) {
            engaged = level().getNearestPlayer(this, 64.0D) != null;
        }
        if (engaged) {
            this.unengagedTicks = 0;
            return;
        }
        if (++this.unengagedTicks < RESET_AFTER) {
            return;
        }
        this.unengagedTicks = 0;
        setForm(1);
        setHealth(getMaxHealth());
    }
    /**
     * His collision box, per form.
     *
     * <p>The registered size is one fixed 1.5 x 3.3 for every form, which was
     * already wrong when the renderer scaled him 1.3x to 5.8x and is unusable
     * now that form one opens at 5.8x: you would swing at a monster filling the
     * screen and hit air three blocks off the ground, and his own melee reach
     * is measured from this box too.
     *
     * <p>These grow with him but deliberately fall short of the visual scale.
     * Matching 14x exactly would be a box roughly twenty-six blocks tall, which
     * suffocates in his den and bulldozes terrain on every step. Reading bigger
     * than you collide is standard for bosses - the Ender Dragon is the obvious
     * precedent - and it keeps him hittable without making him a wrecking ball.
     */
    private static final float[] FORM_WIDTH  = { 4.2F, 5.0F, 5.8F, 6.8F, 7.8F,  9.0F, 11.0F };
    private static final float[] FORM_HEIGHT = { 9.0F, 10.5F, 12.0F, 14.0F, 16.0F, 18.0F, 22.0F };

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(
            net.minecraft.world.entity.Pose pose) {
        int i = net.minecraft.util.Mth.clamp(getForm() - 1, 0, FORM_WIDTH.length - 1);
        return net.minecraft.world.entity.EntityDimensions.scalable(FORM_WIDTH[i], FORM_HEIGHT[i]);
    }
}
