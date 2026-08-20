package com.barbarajones.v2.abilities.item;

import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.KraveMeteor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * METEOR TOTEM - calls the apocalypse's own falling meteor down on whatever
 * the player is looking at, reusing {@link KraveMeteor} exactly as the sky
 * itself does rather than a second falling-rock entity.
 *
 * <p>Deliberately does <em>not</em> call {@code KraveMeteor.saiyanStrike()} -
 * that path exists to make a meteor Cayden's own attack (friendly-fire-proof,
 * attributed for boss damage gates). Skipping it means this lands as the same
 * plain, indiscriminate blast the apocalypse itself drops: real splash
 * damage, real terrain destruction, and yes, it will hurt the caller if they
 * stand where they aimed. That is the cost the brief asked for, not a bug.
 */
public class MeteorTotemItem extends AbilityItem {

    private static final double RANGE = 40.0D;
    private static final int COUNT = 3;
    private static final double DROP_HEIGHT = 28.0D;

    public MeteorTotemItem(Item.Properties properties) {
        super(AbilityId.TOTEM, properties);
    }

    @Override
    public void activate(ServerPlayer player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = player.getEyePosition();
        Vec3 reach = from.add(player.getLookAngle().scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(from, reach, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        Vec3 targetPos = hit.getType() == HitResult.Type.MISS ? from.add(player.getLookAngle().scale(RANGE))
                : hit.getLocation();

        for (int i = 0; i < COUNT; i++) {
            KraveMeteor meteor = ModEntities.METEOR.get().create(level);
            if (meteor == null) {
                continue;
            }
            double ox = (player.getRandom().nextDouble() - 0.5D) * 4.0D;
            double oz = (player.getRandom().nextDouble() - 0.5D) * 4.0D;
            double spawnY = clearHeight(level, targetPos, ox, oz);
            meteor.kind(KraveMeteor.TYPE_METEOR);
            meteor.setPos(targetPos.x + ox, spawnY, targetPos.z + oz);
            meteor.aim(-ox * 0.05D, -oz * 0.05D);
            level.addFreshEntity(meteor);
        }

        level.sendParticles(ParticleTypes.FLAME, targetPos.x, targetPos.y + 1.0D, targetPos.z,
                12, 0.6D, 0.2D, 0.6D, 0.05D);
        level.playSound(null, player.blockPosition(), ModSounds.KRAVE_ROAR.get(),
                player.getSoundSource(), 1.3F, 0.9F);
    }

    /** Walk up from the target until a real ceiling stops it, same trick CaydenCobb's callMeteor uses. */
    private static double clearHeight(ServerLevel level, Vec3 target, double ox, double oz) {
        BlockPos scan = BlockPos.containing(target.x + ox, target.y + 1.0D, target.z + oz);
        int clear = 0;
        int cap = (int) DROP_HEIGHT;
        for (int i = 0; i < cap; i++) {
            BlockPos above = scan.above(i + 1);
            if (!level.getBlockState(above).isAir()) {
                break;
            }
            clear = i + 1;
        }
        return target.y + Math.max(3, clear);
    }
}
