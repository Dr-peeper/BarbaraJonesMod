package com.barbarajones.entity;

import com.barbarajones.content.ModItems;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Daniel - Barbara's long-suffering assistant. Wanders about filming, but bolts
 * the moment any Barbara starts raging. Right-click him empty-handed to take
 * the lighter she is always asking about.
 */
public class Daniel extends PathfinderMob {

    private static final double SPEED_CALM = 0.5D;
    private static final double SPEED_FLEE = 0.72D;
    private int fleeCooldown;

    public Daniel(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, SPEED_CALM);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, SPEED_CALM));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (this.fleeCooldown > 0) {
            this.fleeCooldown--;
        }
        if (this.tickCount % 10 == 0) {
            BarbaraJones threat = null;
            double best = Double.MAX_VALUE;
            for (BarbaraJones b : level().getEntitiesOfClass(BarbaraJones.class,
                    getBoundingBox().inflate(14.0D, 6.0D, 14.0D))) {
                if (!b.isRaging()) {
                    continue;
                }
                double d = distanceToSqr(b);
                if (d < best) {
                    best = d;
                    threat = b;
                }
            }
            if (threat != null) {
                fleeFrom(threat);
            } else if (this.fleeCooldown == 0) {
                var attr = getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) {
                    attr.setBaseValue(SPEED_CALM);
                }
            }
        }
    }

    private void fleeFrom(BarbaraJones threat) {
        var attr = getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.setBaseValue(SPEED_FLEE);
        }
        this.fleeCooldown = 60;
        Vec3 away = position().subtract(threat.position());
        double len = Math.sqrt(away.x * away.x + away.z * away.z);
        if (len < 0.01D) {
            return;
        }
        getNavigation().moveTo(getX() + away.x / len * 8.0D, getY(), getZ() + away.z / len * 8.0D, SPEED_FLEE);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player.getItemInHand(hand).isEmpty()) {
            ItemStack lighter = new ItemStack(ModItems.LIGHTER.get());
            if (!player.getInventory().add(lighter)) {
                spawnAtLocation(lighter);
            }
            playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.1F);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
