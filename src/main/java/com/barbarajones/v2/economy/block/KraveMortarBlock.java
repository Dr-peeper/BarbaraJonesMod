package com.barbarajones.v2.economy.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * KRAVE MORTAR - the manual, hands-on way to turn spare Cocoa Beans into
 * Krave Dust, before any automated grinder exists. No block entity, no GUI:
 * right-click while holding cocoa beans and the whole stack (in pairs) is
 * ground on the spot. This is deliberately the "slow, by hand" version of
 * whatever automated cocoa grinder a later machines module builds - keep the
 * ratio here worse than any future machine so the upgrade always feels worth
 * building.
 *
 * <p>Ratio: {@value #COCOA_PER_DUST} Cocoa Beans -&gt; 1 Krave Dust
 * ({@code barbarajones:krave_dust}, registered in
 * {@code com.barbarajones.content.ModItems} - read, never written, from
 * here).
 */
public class KraveMortarBlock extends Block {

    /** Cocoa Beans consumed per Krave Dust produced. Worse than 1:1 on purpose. */
    public static final int COCOA_PER_DUST = 2;

    private static final VoxelShape SHAPE = Shapes.join(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 8.0D, 15.0D),
            Block.box(3.0D, 3.0D, 3.0D, 13.0D, 9.0D, 13.0D),
            BooleanOp.ONLY_FIRST);

    public KraveMortarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.COCOA_BEANS)) {
            return InteractionResult.PASS;
        }

        int grinds = held.getCount() / COCOA_PER_DUST;
        if (grinds <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal(
                        "Needs at least " + COCOA_PER_DUST + " cocoa beans to grind.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild) {
                held.shrink(grinds * COCOA_PER_DUST);
            }

            Item dustItem = com.barbarajones.content.ModItems.KRAVE_DUST.get();
            ItemStack dust = new ItemStack(dustItem, grinds);
            if (!player.getInventory().add(dust)) {
                player.drop(dust, false);
            }

            level.playSound(null, pos, SoundEvents.GRAVEL_HIT, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.3F);
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS,
                    0.6F, 1.2F + level.random.nextFloat() * 0.3F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D,
                        6 + grinds, 0.18D, 0.08D, 0.18D, 0.01D);
            }

            player.displayClientMessage(Component.literal("+" + grinds + " Krave Dust")
                    .withStyle(ChatFormatting.GOLD), true);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
