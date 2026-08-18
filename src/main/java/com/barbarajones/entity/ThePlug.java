package com.barbarajones.entity;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * THE PLUG. Black ski mask, city-park energy. Right-click him holding Mom's
 * $500 and he "sells" you the bundle: off-brand pastries, fake weed, and
 * "cocaine" that is snow he scraped off the ground.
 *
 * He also owns a sniper: an instant long-range hitscan with a particle tracer.
 * Every so often he just uses it on Cayden.
 */
public class ThePlug extends Monster {

    private int sniperCooldown;

    public ThePlug(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.4D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (this.sniperCooldown > 0) {
            this.sniperCooldown--;
        }

        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) {
            setTarget(null);
            target = null;
        }

        // canon: occasionally he just snipes Cayden
        if (target == null && this.sniperCooldown <= 0 && this.random.nextInt(200) == 0) {
            List<CaydenCobb> caydens = level().getEntitiesOfClass(CaydenCobb.class,
                    getBoundingBox().inflate(40.0D, 16.0D, 40.0D));
            if (!caydens.isEmpty()) {
                setTarget(caydens.get(0));
                target = caydens.get(0);
            }
        }

        if (target != null && this.sniperCooldown <= 0 && hasLineOfSight(target)) {
            snipe(target);
            this.sniperCooldown = 70;
        }
    }

    private void snipe(LivingEntity target) {
        lookAt(target, 360.0F, 360.0F);
        level().playSound(null, blockPosition(), ModSounds.KRAVE_BOOM.get(), getSoundSource(), 1.2F, 1.9F);

        Vec3 from = position().add(0.0D, 1.6D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            for (int i = 0; i <= 24; i++) {
                double t = i / 24.0D;
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        from.x + (to.x - from.x) * t,
                        from.y + (to.y - from.y) * t,
                        from.z + (to.z - from.z) * t, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }

        boolean wasCayden = target instanceof CaydenCobb;
        target.hurt(level().damageSources().mobAttack(this), 8.0F);

        if (wasCayden && !target.isAlive()) {
            for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(48.0D))) {
                p.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                        + "The Plug: \"got that mothafucker.\""));
            }
        }
    }

    /** The $500 deal. You will regret it. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.FIVE_HUNDRED_DOLLARS.get())) {
            if (!level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                give(player, new ItemStack(ModItems.OFF_BRAND_PASTRIES.get()));
                give(player, new ItemStack(ModItems.FAKE_WEED.get(), 2));
                give(player, new ItemStack(ModItems.FAKE_COCAINE.get(), 3));
                player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                        + "The Plug: \"it's real. trust.\""));
                Quests.advanceTo(player, Quests.ACT2_SEWER);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.is(ModItems.FAKE_WEED.get())) {
            if (!level().isClientSide) {
                player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                        + "The Plug: *shrug* \"it was real.\""));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // the actual product: emeralds for grass. His prices are not good.
        if (held.is(net.minecraft.world.item.Items.EMERALD)) {
            if (!level().isClientSide) {
                int paid = Math.min(held.getCount(), 8);
                if (!player.getAbilities().instabuild) {
                    held.shrink(paid);
                }
                // 2 handfuls per emerald - and a 1-in-4 chance he shorts you
                int grass = paid * 2;
                if (this.random.nextInt(4) == 0) {
                    grass = Math.max(1, grass / 2);
                    player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                            + "The Plug: \"that's the whole thing bro, I weighed it.\""));
                } else {
                    player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                            + "The Plug: \"pleasure doin business.\""));
                }
                give(player, new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), grass));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            spawnAtLocation(stack);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.SKI_MASK.get()));
        spawnAtLocation(new ItemStack(ModItems.SNIPER_SCOPE.get()));
        if (this.random.nextBoolean()) {
            spawnAtLocation(new ItemStack(ModItems.FIVE_HUNDRED_DOLLARS.get()));   // your money back
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KRAVE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KRAVE_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
