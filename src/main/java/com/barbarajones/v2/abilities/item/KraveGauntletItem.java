package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModSounds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * KRAVE GAUNTLET - the knockback flash on demand.
 *
 * <p>Cayden's version is a 1-in-10 chance riding on every punch
 * ({@code CaydenCobb.doHurtTarget}, the {@code FLASH_ODDS} roll). This is the
 * same shove and the same tell (sweep + end rod particles, the boom sound) as
 * a charged attack the player chooses to spend a cooldown on instead of
 * gambling for.
 */
public class KraveGauntletItem extends AbilityItem {

    /** How far in front of the player the punch reaches. */
    private static final double REACH = 4.0D;
    private static final float DAMAGE = 6.0F;
    /** Matches the 3.4 horizontal / 0.85 vertical shove CaydenCobb's flash uses. */
    private static final double PUSH_HORIZONTAL = 3.4D;
    private static final double PUSH_VERTICAL = 0.85D;

    public KraveGauntletItem(Item.Properties properties) {
        super(AbilityId.GAUNTLET, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        AABB box = player.getBoundingBox().inflate(REACH, 1.0D, REACH);

        boolean hitAny = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && e.attackable())) {
            Vec3 toTarget = target.position().subtract(origin);
            if (toTarget.length() > REACH + 1.0D) {
                continue;
            }
            // only what is roughly in front of the swing, not anything standing behind him
            if (toTarget.normalize().dot(look) < 0.15D) {
                continue;
            }
            Vec3 flat = new Vec3(toTarget.x, 0.0D, toTarget.z);
            double flatLen = Math.max(0.2D, flat.length());
            Vec3 shove = flat.scale(PUSH_HORIZONTAL / flatLen);
            target.hurt(player.damageSources().playerAttack(player), DAMAGE);
            target.push(shove.x, PUSH_VERTICAL, shove.z);
            target.hurtMarked = true;
            hitAny = true;
        }

        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                origin.x + look.x * 1.5D, origin.y, origin.z + look.z * 1.5D, 3, 0.3D, 0.3D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD,
                origin.x + look.x * 2.0D, origin.y, origin.z + look.z * 2.0D, 14, 0.4D, 0.4D, 0.4D, 0.2D);
        level.playSound(null, player.blockPosition(), ModSounds.COMBAT_HEAVY_HIT.get(),
                player.getSoundSource(), 1.2F, hitAny ? 1.0F : 1.3F);
        player.swing(InteractionHand.MAIN_HAND);
    }
}
