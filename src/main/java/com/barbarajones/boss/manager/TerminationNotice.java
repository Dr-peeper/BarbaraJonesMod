package com.barbarajones.boss.manager;

import com.barbarajones.content.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * A sheet of termination paperwork, signed and released. It steers toward its
 * mark, but only slowly, and only while it can actually see them - break the
 * line with a {@link FilingCabinet} and the notice keeps flying straight into
 * the steel and shreds.
 *
 * <p>Landing one hurts and adds a Written Up stack, so ignoring phase 3's cover
 * compounds: paperwork you eat makes you slower, and slower makes the next
 * sheet easier to land.
 */
public class TerminationNotice extends Entity {

    private static final double SPEED = 0.52D;
    private static final double TURN_RATE = 0.16D;
    private static final float DAMAGE = 5.0F;
    private static final int LIFETIME = 160;

    @Nullable
    private UUID targetId;
    @Nullable
    private UUID ownerId;
    @Nullable
    private LivingEntity targetCache;

    public TerminationNotice(EntityType<? extends TerminationNotice> type, Level level) {
        super(type, level);
        this.noPhysics = true;   // block contact is handled by the sweep below
    }

    public TerminationNotice(Level level, TheManager owner, LivingEntity target, Vec3 from) {
        this(ModEntities.TERMINATION_NOTICE.get(), level);
        this.ownerId = owner.getUUID();
        this.targetId = target.getUUID();
        this.targetCache = target;
        setPos(from.x, from.y, from.z);
        Vec3 aim = aimPoint(target).subtract(from);
        if (aim.lengthSqr() > 1.0E-4D) {
            setDeltaMovement(aim.normalize().scale(SPEED));
        } else {
            setDeltaMovement(0.0D, 0.0D, SPEED);
        }
    }

    @Override
    protected void defineSynchedData() {
        // nothing beyond position and motion: the renderer tumbles it from tickCount
    }

    private static Vec3 aimPoint(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide) {
            LivingEntity target = resolveTarget();
            // Cover check comes before steering, so ducking behind a cabinet does
            // not just soften the homing - it switches the homing off entirely.
            if (target != null && !TheManager.isBehindCover(level(), start, aimPoint(target))) {
                Vec3 want = aimPoint(target).subtract(start);
                if (want.lengthSqr() > 1.0E-4D) {
                    motion = motion.normalize().scale(1.0D - TURN_RATE)
                            .add(want.normalize().scale(TURN_RATE))
                            .normalize().scale(SPEED);
                    setDeltaMovement(motion);
                }
            }
            if (checkImpact(start, start.add(motion))) {
                return;
            }
        }

        move(MoverType.SELF, motion);

        if (level().isClientSide) {
            level().addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else if (this.tickCount > LIFETIME || level().getBlockState(blockPosition()).isSuffocating(
                level(), blockPosition())) {
            shred();
        }
    }

    /** Cabinets first, then living targets - cover always wins the tie. */
    private boolean checkImpact(Vec3 start, Vec3 end) {
        AABB sweep = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.35D);

        List<FilingCabinet> cabinets = level().getEntitiesOfClass(FilingCabinet.class, sweep);
        for (FilingCabinet cabinet : cabinets) {
            if (cabinet.getBoundingBox().inflate(0.1D).clip(start, end).isPresent()) {
                cabinet.hurt(level().damageSources().generic(), 4.0F);
                shred();
                return true;
            }
        }

        // Players only. Cayden Cobb wanders into everything, and a stray sheet of
        // HR paperwork killing him would fire the whole Krave Apocalypse - the
        // Manager is firing YOU, not the room.
        for (Player victim : level().getEntitiesOfClass(Player.class, sweep,
                player -> player.isAlive() && !player.isSpectator())) {
            strike(victim);
            return true;
        }
        return false;
    }

    private void strike(Player victim) {
        TheManager owner = resolveOwner();
        victim.hurt(owner != null
                ? level().damageSources().mobAttack(owner)
                : level().damageSources().generic(), DAMAGE);
        if (owner != null && victim instanceof ServerPlayer player) {
            owner.writeUp(player, "Served. Sign here, here, and here.");
        }
        level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, getSoundSource(), 1.0F, 1.6F);
        shred();
    }

    private void shred() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY(), getZ(), 8, 0.2D, 0.2D, 0.2D, 0.03D);
        }
        discard();
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (this.targetCache != null && this.targetCache.isAlive()) {
            return this.targetCache;
        }
        if (this.targetId != null && level() instanceof ServerLevel server
                && server.getEntity(this.targetId) instanceof LivingEntity found && found.isAlive()) {
            this.targetCache = found;
            return found;
        }
        return null;
    }

    @Nullable
    private TheManager resolveOwner() {
        if (this.ownerId != null && level() instanceof ServerLevel server
                && server.getEntity(this.ownerId) instanceof TheManager manager) {
            return manager;
        }
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 65536.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("NoticeTarget")) {
            this.targetId = tag.getUUID("NoticeTarget");
        }
        if (tag.hasUUID("NoticeOwner")) {
            this.ownerId = tag.getUUID("NoticeOwner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.targetId != null) {
            tag.putUUID("NoticeTarget", this.targetId);
        }
        if (this.ownerId != null) {
            tag.putUUID("NoticeOwner", this.ownerId);
        }
    }
}
