package com.barbarajones.v2.mobs.entity.projectile;

import com.barbarajones.v2.mobs.ModMobItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A hardened shard of Krave flicked by {@link com.barbarajones.v2.mobs.entity.KrispboneEntity}
 * - short range, low individual damage, fired three at a time in a spread
 * rather than one precise shot. Built on {@link ThrowableItemProjectile} for
 * the free physics/rendering (renders as the krave_shard item via
 * ThrownItemRenderer), same as vanilla snowballs/eggs.
 */
public class KraveShardEntity extends ThrowableItemProjectile {

    private static final float DAMAGE = 2.0F;

    public KraveShardEntity(EntityType<? extends KraveShardEntity> type, Level level) {
        super(type, level);
    }

    public KraveShardEntity(Level level, LivingEntity thrower) {
        super(com.barbarajones.v2.mobs.ModMobEntities.KRAVE_SHARD.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModMobItems.KRAVE_SHARD.get();
    }

    @Override
    protected float getGravity() {
        return 0.02F; // flat-ish flick, not a lobbed arc
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        if (!level().isClientSide) {
            boolean hurt = target.hurt(
                    owner instanceof LivingEntity living
                            ? level().damageSources().mobProjectile(this, living)
                            : level().damageSources().generic(),
                    DAMAGE);
            if (hurt) {
                level().playSound(null, target.blockPosition(), SoundEvents.STONE_HIT, SoundSource.HOSTILE, 0.8F, 1.4F);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().playSound(null, blockPosition(), SoundEvents.STONE_HIT,
                    SoundSource.HOSTILE, 0.3F, 1.8F);
            discard();
        } else {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
