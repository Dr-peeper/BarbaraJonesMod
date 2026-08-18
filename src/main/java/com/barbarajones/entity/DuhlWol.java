package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.synced.EntityDataAccessor;
import net.minecraft.network.synced.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Duhl Wol - a debt collector who pulls up in a car every day and demands
 * escalating tribute. Once paid, he leaves you alone for 1-2 days. Hit him or
 * fail to deliver in 5 minutes and he turns hostile - 4x zombie HP, 1.5x damage,
 * and a nasty pin attack.
 */
public class DuhlWol extends Mob {

    private static final EntityDataAccessor<Integer> OWED = SynchedEntityData.defineId(DuhlWol.class, net.minecraft.network.synced.EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TIMER = SynchedEntityData.defineId(DuhlWol.class, net.minecraft.network.synced.EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(DuhlWol.class, net.minecraft.network.synced.EntityDataSerializers.BOOLEAN);

    // what Duhl wants today: 0=dirt, 1=stone, 2=andesite, 3=iron, 4=emerald, 5=dollars
    private static final int[] WANT = { 0, 1, 2, 3, 4, 5 };
    private static final ItemStack[] ITEMS = {
        new ItemStack(net.minecraft.world.level.block.Blocks.DIRT.asItem()),
        new ItemStack(net.minecraft.world.level.block.Blocks.STONE.asItem()),
        new ItemStack(net.minecraft.world.level.block.Blocks.ANDESITE.asItem()),
        new ItemStack(net.minecraft.world.level.block.Blocks.IRON_ORE.asItem()),
        new ItemStack(net.minecraft.world.level.block.Blocks.EMERALD_ORE.asItem()),
        new ItemStack(ModItems.DOLLARS.get(), 4)  // 4 dollars
    };

    private int pinCooldown = 0;

    public DuhlWol(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWED, 0);
        this.entityData.define(TIMER, 0);
        this.entityData.define(HOSTILE, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)              // 4x zombie (10)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)            // 1.5x zombie (2)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
    }

    public void setOweStage(int stage) {
        this.entityData.set(OWED, stage);
    }

    public int getOweStage() {
        return this.entityData.get(OWED);
    }

    public ItemStack getWantedItem() {
        int stage = getOweStage();
        if (stage < 0 || stage >= ITEMS.length) {
            return ItemStack.EMPTY;
        }
        return ITEMS[stage].copy();
    }

    public void setTimer(int ticks) {
        this.entityData.set(TIMER, ticks);
    }

    public int getTimer() {
        return this.entityData.get(TIMER);
    }

    public void setHostile(boolean h) {
        this.entityData.set(HOSTILE, h);
    }

    public boolean isHostileMode() {
        return this.entityData.get(HOSTILE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        // timer countdown
        int t = getTimer();
        if (t > 0) {
            setTimer(t - 1);
            if (t == 1) {
                // time up - go hostile
                setHostile(true);
                for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0D))) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED
                            + "Duhl Wol: \"You had your chance, homie. Now we do this my way.\""));
                }
            }
        }

        // pin cooldown
        if (pinCooldown > 0) {
            pinCooldown--;
        }

        // aggro near players if hostile
        if (isHostileMode()) {
            Player nearest = this.level().getNearestPlayer(this, 30.0D);
            if (nearest != null) {
                this.setTarget(nearest);
                if (this.tickCount % 40 == 0) {
                    playSound(ModSounds.KRAVE_LAUGH.get(), 1.0F, 0.7F);
                }
                // pin attack (every 60 ticks if in range)
                if (this.getTarget() != null && this.distanceToSqr(this.getTarget()) < 9.0D && pinCooldown <= 0) {
                    if (this.tickCount % 60 == 0) {
                        Player p = (Player) this.getTarget();
                        p.setDeltaMovement(0, 0, 0);
                        for (int i = 0; i < 20; i++) {
                            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.3, 0.3, 0.1);
                        }
                        pinCooldown = 40;   // ~2 seconds of pin
                        playSound(SoundEvents.GENERIC_EXPLODE, 0.8F, 1.1F);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack want = getWantedItem();
        if (want.isEmpty() || !held.is(want.getItem())) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            int stage = getOweStage();
            if (stage < ITEMS.length - 1) {
                // advance to next stage
                setOweStage(stage + 1);
                playSound(ModSounds.KRAVE_LAUGH.get(), 0.8F, 0.9F);
                player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                        + "Duhl Wol: \"Aight, that'll work. Come back tomorrow with this.\""));
            } else {
                // final payment - he leaves
                pay(player);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void pay(Player player) {
        setTimer(0);
        setHostile(false);
        setOweStage(0);
        player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                + "Duhl Wol: \"Appreciate it, homie. I'll be back.\""));
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 0.6F, 0.5F);
        this.kill();
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        if (!this.level().isClientSide && getTimer() > 0) {
            setHostile(true);
            setTimer(100);   // 5 seconds left to run
            Player p = src.getEntity() instanceof Player ? (Player) src.getEntity() : null;
            if (p != null) {
                p.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED
                        + "Duhl Wol: \"Oh, you gonna do it like that? NOW we fight.\""));
            }
        }
        return super.hurt(src, amt);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DuhlStage", getOweStage());
        tag.putInt("DuhlTimer", getTimer());
        tag.putBoolean("DuhlHostile", isHostileMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setOweStage(tag.getInt("DuhlStage"));
        setTimer(tag.getInt("DuhlTimer"));
        setHostile(tag.getBoolean("DuhlHostile"));
    }
}
