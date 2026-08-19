package com.barbarajones.content.extra;

import com.barbarajones.content.ModItems;
import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Daniel's Zippo. Daniel is the lighter guy; this is the lighter.
 *
 * <p>It lights fires like flint and steel, but its real trick is that clicking
 * it on a grass block scorches the grass straight off into a Burnt Grass, no
 * knife and no blowtorch involved - one step instead of three, at the cost of
 * the block underneath turning to dirt.
 */
public class ZippoLighterItem extends Item {

    public ZippoLighterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.GRASS_BLOCK)) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                popScorched(level, pos);
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.4F);
                level.playSound(null, pos, ModSounds.EVT_LIGHTER.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                damage(stack, player, ctx);
                if (player != null) {
                    player.displayClientMessage(Component.literal(ChatFormatting.GOLD
                            + "Scorched it right off the ground."), true);
                }
            } else {
                for (int i = 0; i < 8; i++) {
                    level.addParticle(ParticleTypes.FLAME,
                            pos.getX() + level.random.nextDouble(),
                            pos.getY() + 1.05D,
                            pos.getZ() + level.random.nextDouble(),
                            0.0D, 0.02D, 0.0D);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos above = pos.relative(ctx.getClickedFace());
        if (BaseFireBlock.canBePlacedAt(level, above, ctx.getHorizontalDirection())) {
            if (!level.isClientSide) {
                level.setBlock(above, BaseFireBlock.getState(level, above), 11);
                level.playSound(null, above, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
                damage(stack, player, ctx);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private void popScorched(Level level, BlockPos pos) {
        net.minecraft.world.level.block.Block.popResource(level, pos,
                new ItemStack(ModItems.BURNT_GRASS.get(), 1));
    }

    /** Creative-safe wear: never chews through the lighter in creative mode. */
    private void damage(ItemStack stack, Player player, UseOnContext ctx) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        EquipmentSlot slot = ctx.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(slot));
    }
}
