package com.barbarajones.entity;

import com.barbarajones.content.ModSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A Krave Box that spawns near the boss during the Super Saiyan fight and
 * heals him every few seconds while it survives - the player's job is
 * finding and destroying these (and the minions), not fighting Krave
 * Monster directly. Stands still: no goals are registered.
 */
public class KraveHealingBox extends Monster {

    private static final int HEAL_INTERVAL = 60;
    private static final float HEAL_AMOUNT = 6.0F;

    private UUID healTargetId;
    private KraveMonster healTargetCache;

    public KraveHealingBox(EntityType<? extends KraveHealingBox> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // intentionally empty - this stands still and does nothing but heal the boss
    }

    /** Set once at spawn time by KraveKosmosBattle. */
    public void setHealTarget(KraveMonster monster) {
        this.healTargetCache = monster;
        this.healTargetId = monster.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || this.tickCount % HEAL_INTERVAL != 0) {
            return;
        }
        KraveMonster target = resolveTarget();
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        target.heal(HEAL_AMOUNT);
        level().playSound(null, blockPosition(), ModSounds.KRAVE_LAUGH.get(), getSoundSource(), 0.6F, 1.4F);
    }

    private KraveMonster resolveTarget() {
        if (this.healTargetCache != null && this.healTargetCache.isAlive()) {
            return this.healTargetCache;
        }
        if (this.healTargetId != null && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            var entity = sl.getEntity(this.healTargetId);
            if (entity instanceof KraveMonster monster) {
                this.healTargetCache = monster;
                return monster;
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.KRAVE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KRAVE_BOOM.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.healTargetId != null) {
            tag.putUUID("HealTarget", this.healTargetId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("HealTarget")) {
            this.healTargetId = tag.getUUID("HealTarget");
        }
    }
}
