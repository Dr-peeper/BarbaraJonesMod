package com.barbarajones.entity;

import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A fixed red star hanging permanently over the Krave Kosmos - not a real
 * light source (Minecraft entities don't cast light; the actual fix for the
 * dimension being too dark is has_skylight/ambient_light in the
 * dimension_type), but it gives the sky something to visibly be lit BY, and
 * it glows regardless of the dimension's own ambient light (see
 * KraveRedStarRenderer's use of RenderType.eyes(), the same always-full-
 * bright layer vanilla uses for enderman/spider eyes).
 *
 * <p>Recoloring vanilla's own sun sprite was considered and rejected: that
 * texture is a single shared global asset with no per-dimension path, so
 * overriding it would also recolor the real Overworld's sun. This is a
 * completely separate, custom object instead - fixed high above the boss
 * island, not orbiting, spawned once, ever, alongside the boss and the
 * leviathans.
 */
public class KraveRedStar extends Entity {

    /** Height above KraveDimensions.BOSS_ISLAND - clear of the den, the leviathans' orbit, and the island's own hills. */
    private static final double STAR_ALTITUDE = 230.0D;

    public KraveRedStar(EntityType<? extends KraveRedStar> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** One, spawned once, ever - same one-time-flag pattern as the boss and the leviathans. */
    public static void ensureSpawned(ServerLevel kosmos) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        if (data.isRedStarEverSpawned()) {
            return;
        }
        data.setRedStarEverSpawned(true);

        KraveRedStar star = com.barbarajones.content.ModEntities.KRAVE_RED_STAR.get().create(kosmos);
        if (star == null) {
            return;
        }
        star.moveTo(KraveDimensions.BOSS_ISLAND.x, STAR_ALTITUDE, KraveDimensions.BOSS_ISLAND.z, 0.0F, 0.0F);
        kosmos.addFreshEntity(star);
    }

    @Override
    protected void defineSynchedData() { }

    @Override
    public void tick() {
        super.tick();
        // A slow spin so the crossed-quad billboard reads as something
        // turning in space rather than a completely static cardboard cutout.
        setYRot((this.tickCount * 0.05F) % 360.0F);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4_000_000.0D;
    }
}
