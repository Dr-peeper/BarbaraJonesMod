package com.barbarajones.item;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Krave tool set. Every one of them is genuinely excellent - and comes with
 * a curse that makes it a nightmare to actually live with.
 */
public final class KraveTools {

    private KraveTools() { }

    private static void curseTip(List<Component> tooltip, String line) {
        tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + "Krave Curse: "
                + ChatFormatting.GRAY + line));
    }

    // ---- PICKAXE: you keep eating it ---------------------------------------

    /**
     * Mines like a dream. But it is made of cereal, and every 10-20 blocks you
     * absent-mindedly take a bite - and then feel terrible about it.
     */
    public static class KravePickaxe extends PickaxeItem {

        public KravePickaxe(Properties props) {
            super(KraveTier.INSTANCE, 1, -2.8F, props);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity user) {
            if (!level.isClientSide && user instanceof Player player) {
                int eaten = stack.getOrCreateTag().getInt("BlocksMined") + 1;
                int threshold = 10 + (stack.getOrCreateTag().getInt("NextBite") % 11);
                if (eaten >= threshold) {
                    stack.getOrCreateTag().putInt("BlocksMined", 0);
                    stack.getOrCreateTag().putInt("NextBite", level.random.nextInt(11));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
                    player.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE
                            + "You took another bite of the pickaxe. You feel awful about it."));
                    level.playSound(null, player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 0.8F);
                } else {
                    stack.getOrCreateTag().putInt("BlocksMined", eaten);
                }
            }
            return super.mineBlock(stack, level, state, pos, user);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
            curseTip(tooltip, "every 10-20 blocks you eat some. Slowness + Hunger.");
        }
    }

    // ---- SWORD: Cayden wants a bite ----------------------------------------

    /**
     * Hits hard - but it smells incredible, so Cayden teleports directly into
     * your face trying to eat it every time you swing.
     */
    public static class KraveSword extends SwordItem {

        public KraveSword(Properties props) {
            super(KraveTier.INSTANCE, 3, -2.4F, props);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (!attacker.level().isClientSide && attacker instanceof Player player
                    && attacker.level().random.nextInt(3) == 0) {
                List<CaydenCobb> nearby = attacker.level().getEntitiesOfClass(CaydenCobb.class,
                        attacker.getBoundingBox().inflate(24.0D));
                for (CaydenCobb c : nearby) {
                    if (!c.isOwnedBy(player)) {
                        continue;
                    }
                    // directly in front of your eyes. blocking everything.
                    Vec3 look = player.getLookAngle();
                    c.teleportTo(player.getX() + look.x * 0.9D,
                            player.getEyeY() - 0.6D,
                            player.getZ() + look.z * 0.9D);
                    c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, player.position());
                    player.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                            + "Cayden: \"YO is that KRAVE?? lemme get a bite-\""));
                    player.level().playSound(null, player.blockPosition(),
                            ModSounds.CAYDEN_IDLE.get(), SoundSource.NEUTRAL, 1.2F, 1.0F);
                    break;
                }
            }
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
            curseTip(tooltip, "Cayden teleports into your face to eat it mid-fight.");
        }
    }

    // ---- AXE: it drops crumbs, and he wants them ---------------------------

    /**
     * Chops through wood - but sheds cereal everywhere, and Cayden will body-check
     * you out of the way to get it.
     */
    public static class KraveAxe extends AxeItem {

        public KraveAxe(Properties props) {
            super(KraveTier.INSTANCE, 6.0F, -3.1F, props);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity user) {
            if (!level.isClientSide && level.random.nextInt(4) == 0 && user instanceof Player player) {
                ItemStack crumb = new ItemStack(ModItems.KRAVE_CEREAL.get());
                var drop = new net.minecraft.world.entity.item.ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, crumb);
                level.addFreshEntity(drop);

                for (CaydenCobb c : level.getEntitiesOfClass(CaydenCobb.class,
                        player.getBoundingBox().inflate(20.0D))) {
                    if (!c.isOwnedBy(player)) {
                        continue;
                    }
                    c.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.6D);
                    // and he shoves you aside on the way
                    Vec3 shove = player.position().subtract(c.position()).normalize().scale(0.55D);
                    player.push(shove.x, 0.25D, shove.z);
                    player.hurtMarked = true;
                    break;
                }
            }
            return super.mineBlock(stack, level, state, pos, user);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
            curseTip(tooltip, "sheds cereal. Cayden shoves you aside to get it.");
        }
    }

    // ---- SHOVEL: pure sugar rush -------------------------------------------

    /**
     * Digs absurdly fast, but the sugar rush means you cannot stand still -
     * you involuntarily bounce while you work.
     */
    public static class KraveShovel extends ShovelItem {

        public KraveShovel(Properties props) {
            super(KraveTier.INSTANCE, 1.5F, -3.0F, props);
        }

        @Override
        public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                                 BlockPos pos, LivingEntity user) {
            if (!level.isClientSide && user instanceof Player player && player.onGround()) {
                player.setDeltaMovement(player.getDeltaMovement().x,
                        0.42D + level.random.nextDouble() * 0.15D,
                        player.getDeltaMovement().z);
                player.hurtMarked = true;
                if (level.random.nextInt(6) == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 2));
                    player.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE
                            + "The sugar rush takes hold. You cannot stop bouncing."));
                }
            }
            return super.mineBlock(stack, level, state, pos, user);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
            curseTip(tooltip, "sugar rush - you bounce uncontrollably while digging.");
        }
    }

    // ---- HOE: the crunch is LOUD -------------------------------------------

    /**
     * Tills instantly - but the crunching is deafening, and it draws things
     * out of the dark toward you.
     */
    public static class KraveHoe extends HoeItem {

        public KraveHoe(Properties props) {
            super(KraveTier.INSTANCE, -3, 0.0F, props);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            InteractionResult result = super.useOn(ctx);
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (!level.isClientSide && result.consumesAction() && player != null) {
                level.playSound(null, ctx.getClickedPos(), ModSounds.KRAVE_LAUGH.get(),
                        SoundSource.PLAYERS, 2.0F, 0.9F);
                // the noise pulls hostiles toward you
                for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                        player.getBoundingBox().inflate(32.0D))) {
                    if (mob instanceof net.minecraft.world.entity.monster.Monster monster) {
                        monster.setTarget(player);
                    }
                }
                if (level.random.nextInt(5) == 0) {
                    Zombie drawn = EntityType.ZOMBIE.create(level);
                    if (drawn != null) {
                        drawn.moveTo(player.getX() + (level.random.nextDouble() - 0.5D) * 12.0D,
                                player.getY(), player.getZ() + (level.random.nextDouble() - 0.5D) * 12.0D);
                        drawn.finalizeSpawn((net.minecraft.server.level.ServerLevel) level,
                                level.getCurrentDifficultyAt(drawn.blockPosition()),
                                MobSpawnType.EVENT, null, null);
                        level.addFreshEntity(drawn);
                        drawn.setTarget(player);
                    }
                    player.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE
                            + "Something heard you crunching."));
                }
            }
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                                    List<Component> tooltip, TooltipFlag flag) {
            curseTip(tooltip, "the crunching is deafening. It draws hostiles to you.");
        }
    }
}
