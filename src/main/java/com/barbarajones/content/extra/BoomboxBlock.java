package com.barbarajones.content.extra;

import com.barbarajones.content.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The boombox. Right-click it to start the tape; it keeps replaying until you
 * click it off. Loud, obnoxious, and audible from a long way away - which is
 * the point, because the neighbours are the ones who called the news crew.
 */
public class BoomboxBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");

    /** Roughly the length of the longest clip in the set, plus a beat of silence. */
    private static final int TRACK_LENGTH = 220;

    public BoomboxBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PLAYING, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PLAYING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(PLAYING, Boolean.FALSE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean nowPlaying = !state.getValue(PLAYING);
        level.setBlock(pos, state.setValue(PLAYING, nowPlaying), Block.UPDATE_ALL);
        if (nowPlaying) {
            spin(level, pos, level.getRandom());
            level.scheduleTick(pos, this, TRACK_LENGTH);
            player.displayClientMessage(Component.literal(ChatFormatting.LIGHT_PURPLE
                    + "The boombox kicks in."), true);
        } else {
            player.displayClientMessage(Component.literal(ChatFormatting.GRAY
                    + "Tape off."), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PLAYING)) {
            return;
        }
        spin(level, pos, random);
        level.scheduleTick(pos, this, TRACK_LENGTH);
    }

    /**
     * RECORDS is the right source: it puts the boombox on the music slider
     * rather than the block-effects one, so it can be turned down separately
     * from footsteps like an actual stereo.
     */
    private static void spin(Level level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, pickTrack(random), SoundSource.RECORDS, 2.4F, 1.0F);
    }

    /**
     * The boombox is the only thing in the mod that plays a Barbara clip purely
     * to be heard, so it is where the voice lines that no cutscene or
     * apocalypse stage claims belong. Seven of these (intro, bits, donuts,
     * nugget, ohgod, ohgod2, shower) were recorded, packaged, and listed in
     * sounds.json but never registered, so nothing could play them at all -
     * putting them here is what actually makes them audible in game.
     */
    private static SoundEvent pickTrack(RandomSource random) {
        return switch (random.nextInt(12)) {
            case 0 -> ModSounds.EVT_MUSIC.get();
            case 1 -> ModSounds.EVT_ROLL.get();
            case 2 -> ModSounds.EVT_CHEPINA.get();
            case 3 -> ModSounds.CAYDEN_SHOUT.get();
            case 4 -> ModSounds.EVT_INTRO.get();
            case 5 -> ModSounds.EVT_BITS.get();
            case 6 -> ModSounds.EVT_DONUTS.get();
            case 7 -> ModSounds.EVT_NUGGET.get();
            case 8 -> ModSounds.EVT_OHGOD.get();
            case 9 -> ModSounds.EVT_OHGOD2.get();
            case 10 -> ModSounds.EVT_SHOWER.get();
            default -> ModSounds.BARBARA_IDLE.get();
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PLAYING) || random.nextInt(4) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.NOTE,
                pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D,
                pos.getY() + 1.1D,
                pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.6D,
                random.nextInt(24) / 24.0D, 0.0D, 0.0D);
    }
}
