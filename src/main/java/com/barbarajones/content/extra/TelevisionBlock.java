package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The television. Switched off it is a dead grey box; switched on the screen
 * runs static - and every so often the static resolves into a face for a frame
 * before it goes back to snow. The face is in the animated texture, not in code,
 * so it costs nothing to run.
 *
 * <p>Left on, the set also mutters: a canned line from the tape every half
 * minute or so, at low volume, from across the room.
 */
public class TelevisionBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** How long between the set's random mutterings, in ticks. */
    private static final int CHATTER_INTERVAL = 620;

    public TelevisionBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(LIT, Boolean.FALSE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean nowOn = toggle(level, pos, state);
        player.displayClientMessage(Component.literal(nowOn
                ? ChatFormatting.WHITE + "*ktchk* ...static."
                : ChatFormatting.DARK_GRAY + "*ktchk*"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Flips the set and returns its new on/off state. Public so the Remote
     * Control can do it from across the room without duplicating the wiring.
     */
    public static boolean toggle(Level level, BlockPos pos, BlockState state) {
        boolean nowOn = !state.getValue(LIT);
        level.setBlock(pos, state.setValue(LIT, nowOn), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, nowOn ? 1.4F : 0.9F);
        if (nowOn) {
            level.scheduleTick(pos, state.getBlock(), CHATTER_INTERVAL);
        }
        return nowOn;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        // Only ever one queued tick per set: the reschedule below is the sole
        // source, and toggling off simply stops renewing it.
        level.playSound(null, pos, pickLine(random), SoundSource.BLOCKS, 0.45F, 1.0F);
        level.scheduleTick(pos, this, CHATTER_INTERVAL);
    }

    private static net.minecraft.sounds.SoundEvent pickLine(RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> ModSounds.EVT_OG.get();
            case 1 -> ModSounds.EVT_MANAGER.get();
            case 2 -> ModSounds.EVT_DEMOCRAT.get();
            case 3 -> ModSounds.EVT_HOUSE.get();
            case 4 -> ModSounds.EVT_MCD.get();
            default -> ModSounds.EVT_NOTREADY.get();
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        // A little CRT glow spill out of the front of the cabinet.
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5D + facing.getStepX() * 0.55D;
        double y = pos.getY() + 0.45D + random.nextDouble() * 0.4D;
        double z = pos.getZ() + 0.5D + facing.getStepZ() * 0.55D;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.4D, z, 0.0D, 0.01D, 0.0D);
        }
    }
}
