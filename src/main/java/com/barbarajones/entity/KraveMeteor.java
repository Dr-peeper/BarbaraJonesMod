package com.barbarajones.entity;

import com.barbarajones.content.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level.ExplosionInteraction;

/**
 * A typed falling doom-object for the staged apocalypse. One entity covers
 * every stage's rain: METEOR, PIBB, KNIFE (cleaver), GRASS, BOX, GATORADE and
 * the prequel's MOUSE. Rendering is in KraveMeteorRenderer.
 */
public class KraveMeteor extends Entity {

    public static final byte TYPE_METEOR   = 0;
    public static final byte TYPE_PIBB     = 1;
    public static final byte TYPE_KNIFE    = 2;
    public static final byte TYPE_GRASS    = 3;
    public static final byte TYPE_BOX      = 4;
    public static final byte TYPE_GATORADE = 5;
    public static final byte TYPE_MOUSE    = 6;

    /** True when this was thrown by Super Saiyan Cayden - see saiyanStrike(). */
    private boolean saiyan;
    /** Who threw it. The boss only takes full damage from an ascended Cayden. */
    private com.barbarajones.entity.CaydenCobb saiyanOwner;

    private static final EntityDataAccessor<Byte> KIND =
            SynchedEntityData.defineId(KraveMeteor.class, EntityDataSerializers.BYTE);

    public KraveMeteor(EntityType<? extends KraveMeteor> type, Level level) {
        super(type, level);
    }

    public byte getKind() {
        return this.entityData.get(KIND);
    }

    /**
     * Fired by an ascended Cayden during the Kosmos boss fight. These land as
     * strikes rather than bombs: the Krave Monster and his minions take the
     * damage, while Cayden, the player and the pets standing in the blast are
     * untouched, and nothing is dug out of the island.
     */
    public KraveMeteor saiyanStrike(com.barbarajones.entity.CaydenCobb owner) {
        this.saiyan = true;
        this.saiyanOwner = owner;
        return this;
    }

    public KraveMeteor kind(byte k) {
        this.entityData.set(KIND, k);
        return this;
    }

    /** Start it falling with a horizontal drift so it comes in at an angle. */
    public void aim(double driftX, double driftZ) {
        setDeltaMovement(driftX, -1.6D, driftZ);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, TYPE_METEOR);
    }

    @Override
    public void tick() {
        super.tick();
        var m = getDeltaMovement();
        if (m.y > -3.2D) {
            m = m.add(0.0D, -0.06D, 0.0D);
        }
        setDeltaMovement(m);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        if (level().isClientSide) {
            trailParticles();
        } else if (onGround() || this.horizontalCollision || this.tickCount > 200) {
            impact();
        }
    }

    private void trailParticles() {
        byte k = getKind();
        var lead = k == TYPE_PIBB ? net.minecraft.core.particles.ParticleTypes.SPLASH
                : net.minecraft.core.particles.ParticleTypes.FLAME;
        var trail = k == TYPE_KNIFE ? net.minecraft.core.particles.ParticleTypes.CRIT
                : net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE;
        var m = getDeltaMovement();
        for (int i = 0; i < 4; i++) {
            level().addParticle(lead,
                    getX() + (this.random.nextDouble() - 0.5D) * 2.0D,
                    getY() + (this.random.nextDouble() - 0.5D) * 2.0D + 1.0D,
                    getZ() + (this.random.nextDouble() - 0.5D) * 2.0D, 0, 0, 0);
            level().addParticle(trail,
                    getX() - m.x * i * 0.5D, getY() - m.y * i * 0.5D + 1.0D, getZ() - m.z * i * 0.5D, 0, 0, 0);
        }
    }

    private void impact() {
        if (this.saiyan) {
            saiyanImpact();
            discard();
            return;
        }
        switch (getKind()) {
            case TYPE_GATORADE -> level().playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, getSoundSource(), 1.5F, 0.5F);
            case TYPE_PIBB -> level().playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, getSoundSource(), 1.5F, 1.0F);
            case TYPE_MOUSE -> {
                level().playSound(null, blockPosition(),
                        net.minecraft.sounds.SoundEvents.GLASS_BREAK, getSoundSource(), 1.4F, 0.7F);
            }
            case TYPE_KNIFE -> {
                level().playSound(null, blockPosition(), ModSounds.KRAVE_SCREECH.get(), getSoundSource(), 1.0F, 1.8F);
                level().explode(this, getX(), getY(), getZ(), 2.0F, ExplosionInteraction.NONE);
            }
            case TYPE_GRASS -> {
                level().playSound(null, blockPosition(),
                        net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE, getSoundSource(), 1.2F, 0.8F);
                igniteAround();
            }
            case TYPE_BOX -> {
                level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(), 3.5F, 0.6F);
                level().explode(this, getX(), getY(), getZ(), 10.0F, ExplosionInteraction.MOB);
            }
            default -> {
                level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(),
                        3.0F, 0.8F + this.random.nextFloat() * 0.3F);
                level().explode(this, getX(), getY(), getZ(), 6.0F, ExplosionInteraction.MOB);
            }
        }
        discard();
    }

    /**
     * A strike, not a bomb. Hostiles in the crater take real damage; the crew
     * standing next to them take none, and the island keeps its blocks.
     */
    private void saiyanImpact() {
        level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(), 3.0F, 0.7F);
        if (!(level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                getX(), getY() + 1.0D, getZ(), 2, 1.5D, 1.0D, 1.5D, 0.0D);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                getX(), getY() + 1.0D, getZ(), 60, 3.0D, 1.5D, 3.0D, 0.15D);

        var box = getBoundingBox().inflate(6.0D);
        for (var victim : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            if (victim instanceof com.barbarajones.entity.CaydenCobb
                    || victim instanceof com.barbarajones.entity.BarbaraJones
                    || victim instanceof net.minecraft.world.entity.player.Player) {
                continue;   // his own side never eats a meteor
            }
            // Attributed to Cayden: KraveMonster.hurt() cuts anything else to 5%.
            victim.hurt(this.saiyanOwner != null
                    ? sl.damageSources().indirectMagic(this, this.saiyanOwner)
                    : sl.damageSources().magic(), 14.0F);
        }
    }

    private void igniteAround() {
        BlockPos base = blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (this.random.nextInt(3) == 0) {
                    continue;
                }
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (level().getBlockState(p).isAir()
                            && level().getBlockState(p.below()).blocksMotion()) {
                        level().setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        kind(tag.getByte("Kind"));
        this.saiyan = tag.getBoolean("Saiyan");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("Kind", getKind());
        tag.putBoolean("Saiyan", this.saiyan);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 65536.0D;
    }
}
