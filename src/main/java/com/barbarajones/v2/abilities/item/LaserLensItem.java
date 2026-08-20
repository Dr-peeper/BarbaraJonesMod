package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.KraveLaser;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * LASER LENS - fires the exact same {@link KraveLaser} bolt Super Saiyan
 * Cayden fires in {@code CaydenCobb.combatLasers()}. Per the brief, this
 * reuses that entity outright rather than writing a second projectile: same
 * speed, same hit box, same {@code KRAVE_HURT} impact sound, same particle
 * trail. Only the source (three bolts from wherever the player is looking,
 * instead of from Cayden's chest at whatever he is fighting) is new.
 */
public class LaserLensItem extends AbilityItem {

    private static final int BOLTS = 3;
    private static final double RANGE = 48.0D;

    public LaserLensItem(Item.Properties properties) {
        super(AbilityId.LASER, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        Vec3 from = player.getEyePosition();
        Vec3 aimBase = target(player, from);

        for (int i = 0; i < BOLTS; i++) {
            Vec3 aim = aimBase.add(
                    (player.getRandom().nextDouble() - 0.5D) * 0.6D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.6D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.6D);
            KraveLaser bolt = new KraveLaser(player.level(), player, from, aim);
            player.level().addFreshEntity(bolt);
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.KRAVE_BEAM_FIRE.get(),
                player.getSoundSource(), 1.0F, 1.0F + player.getRandom().nextFloat() * 0.2F);
    }

    /** Whatever is dead-center in the player's crosshair, out to {@link #RANGE}. */
    private static Vec3 target(ServerPlayer player, Vec3 from) {
        Vec3 look = player.getLookAngle();
        Vec3 reach = from.add(look.scale(RANGE));

        HitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                from, reach, net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        double blockDist = hit.getType() == HitResult.Type.MISS ? RANGE : hit.getLocation().distanceTo(from);

        EntityHitResult entityHit = pickEntity(player, from, look, blockDist);
        return entityHit != null ? entityHit.getLocation() : from.add(look.scale(blockDist));
    }

    private static EntityHitResult pickEntity(ServerPlayer player, Vec3 from, Vec3 look, double maxDist) {
        Vec3 end = from.add(look.scale(maxDist));
        var box = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0D);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e.attackable())) {
            var hitBox = candidate.getBoundingBox().inflate(0.4D);
            var clip = hitBox.clip(from, end);
            if (clip.isPresent()) {
                double d = clip.get().distanceToSqr(from);
                if (d < bestDist) {
                    bestDist = d;
                    best = candidate;
                }
            }
        }
        return best == null ? null : new EntityHitResult(best, best.position().add(0.0D, best.getBbHeight() * 0.5D, 0.0D));
    }
}
