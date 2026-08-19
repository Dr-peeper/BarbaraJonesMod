package com.barbarajones.boss.mom;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A box of Krave that Mom Cobb has confiscated and stacked up around herself in
 * the last act of her fight. It does nothing on its own - it just sits there
 * sparkling so you can find it - but she will walk over, telegraph a long
 * feeding wind-up and eat it for a large heal (see {@link MomCobbBoss}).
 *
 * <p>Destroying one is the whole job of phase three. Destroy it DURING her
 * wind-up and she comes up with a handful of nothing and staggers, which is the
 * fight's biggest damage window.
 *
 * <p>Registered as a {@link Monster} for the same reason
 * {@code KraveHealingBox} is: it needs an attribute map and a health pool that
 * a player can chew through, and being a Monster means Cayden helps you smash
 * them. It has no goals and never moves.
 */
public class MomKraveStash extends Monster {

    private static final float STASH_HEALTH = 24.0F;

    @Nullable
    private UUID bossId;

    public MomKraveStash(EntityType<? extends MomKraveStash> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, STASH_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                // A stationary target that flinched across the room every time
                // you hit it would be miserable to burst down under pressure.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // intentionally empty - a cardboard box does not pathfind
    }

    /** Set at spawn time so the box can name its owner when it goes down. */
    public void setOwnerBoss(MomCobbBoss boss) {
        this.bossId = boss.getUUID();
    }

    @Nullable
    public MomCobbBoss resolveBoss() {
        if (this.bossId == null || !(level() instanceof ServerLevel sl)) {
            return null;
        }
        return sl.getEntity(this.bossId) instanceof MomCobbBoss boss && boss.isAlive() ? boss : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) {
            return;
        }
        // Phase two leaves blackout zones lying around, so the marker particle is
        // END_ROD rather than smoke - it is the one cue that still reads with
        // Blindness up and a smoke dome overhead.
        if (this.tickCount % 8 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 0.7D, getZ(), 2, 0.25D, 0.3D, 0.25D, 0.005D);
        }
        if (this.tickCount % 40 == 0) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.5D, getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        MomCobbBoss boss = resolveBoss();
        if (boss != null) {
            boss.onStashDestroyed(this);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // Smashing the box scatters what was inside it. That is the reward loop.
        spawnAtLocation(new ItemStack(ModItems.KRAVE_CEREAL.get(), 1 + this.random.nextInt(3)));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ITEM_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KRAVE_BOOM.get();
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.bossId != null) {
            tag.putUUID("MomBoss", this.bossId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("MomBoss")) {
            this.bossId = tag.getUUID("MomBoss");
        }
    }
}
