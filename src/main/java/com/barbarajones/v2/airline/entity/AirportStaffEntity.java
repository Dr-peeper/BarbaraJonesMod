package com.barbarajones.v2.airline.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;

/**
 * Shared base for every human-shaped member of airport staff.
 *
 * <p>Deliberately a plain {@link PathfinderMob} and not a {@code Villager}. A
 * vanilla villager drags in the whole Brain/POI/gossip/profession machinery, wants
 * a bed and a job-site block to be happy, and exposes its NBT hooks as {@code
 * public} - which is what made the first pass of these classes fail to compile at
 * all. Staff here need none of that: they stand where the flight puts them, run a
 * handful of goals, and take their orders from
 * {@link com.barbarajones.v2.airline.npc.NPCBehaviorScheduler}.
 *
 * <p>Every subclass is a thin one: it supplies its own extra synced data and a
 * texture, and inherits attributes, goals, sounds and the flight binding from here.
 */
public abstract class AirportStaffEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_FLIGHT_ID =
            SynchedEntityData.defineId(AirportStaffEntity.class, EntityDataSerializers.STRING);

    protected AirportStaffEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Staff are placed by the scheduler, never by natural spawning, so they must
        // survive the despawn sweep when no player is standing in the terminal.
        this.setPersistenceRequired();
    }

    /**
     * createMobAttributes, NOT createLivingAttributes: the former adds FOLLOW_RANGE,
     * which every PathfinderMob's GroundPathNavigation reads in the constructor. Miss
     * it and the entity cannot be constructed at all.
     */
    public static AttributeSupplier.Builder createStaffAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLIGHT_ID, "");
    }

    /** The flight this member of staff is working, or "" while unassigned. */
    public String getAssignedFlight() {
        return this.entityData.get(DATA_FLIGHT_ID);
    }

    public void setAssignedFlight(String flightId) {
        this.entityData.set(DATA_FLIGHT_ID, flightId == null ? "" : flightId);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FlightId", this.getAssignedFlight());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAssignedFlight(tag.getString("FlightId"));
    }
}
