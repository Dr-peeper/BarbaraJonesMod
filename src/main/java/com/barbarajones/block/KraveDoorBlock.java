package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.dimension.KraveLanding;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

/**
 * The Krave Kosmos portal: a 3-wide x 3-tall frame of {@link ModBlocks#KRAVE_BLOCK},
 * bottom-middle two cells replaced by this door. Opening the door with a
 * complete frame around it steps the player through into the Kosmos.
 */
public class KraveDoorBlock extends DoorBlock {

    public KraveDoorBlock(BlockBehaviour.Properties props, BlockSetType setType) {
        super(props, setType);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = super.use(state, level, pos, player, hand, hit);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (lowerState.getValue(OPEN) && isFrameComplete(level, lowerPos, lowerState)) {
                enterKosmos(serverPlayer);
            }
        }
        return result;
    }

    private boolean isFrameComplete(Level level, BlockPos lowerPos, BlockState lowerState) {
        Direction facing = lowerState.getValue(FACING);
        Direction side = facing.getClockWise();

        BlockPos left = lowerPos.relative(side.getOpposite());
        BlockPos right = lowerPos.relative(side);

        return isKraveBlock(level, lowerPos.above(2))
                && isKraveBlock(level, left) && isKraveBlock(level, left.above()) && isKraveBlock(level, left.above(2))
                && isKraveBlock(level, right) && isKraveBlock(level, right.above()) && isKraveBlock(level, right.above(2));
    }

    private boolean isKraveBlock(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.KRAVE_BLOCK.get());
    }

    private void enterKosmos(ServerPlayer player) {
        ServerLevel dest = player.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (dest == null) {
            return;
        }
        ensureBossExists(dest);

        // Remember where (and in which dimension) the player stepped in, so a
        // Krave Tether can send them straight back to this exact doorway.
        CompoundTag persist = persisted(player);
        persist.putString("KraveReturnDim", player.level().dimension().location().toString());
        persist.putDouble("KraveReturnX", player.getX());
        persist.putDouble("KraveReturnY", player.getY());
        persist.putDouble("KraveReturnZ", player.getZ());

        // Nobody should be able to walk in without a way out: top the player up
        // to at least one Krave Tether on every entry, regardless of whether
        // they thought to craft/bring one themselves.
        if (player.getInventory().countItem(ModItems.KRAVE_TETHER.get()) < 1) {
            ItemStack tether = new ItemStack(ModItems.KRAVE_TETHER.get(), 1);
            if (!player.getInventory().add(tether)) {
                player.drop(tether, false);
            }
        }

        // Actually search for solid ground near the seed point instead of
        // trusting a fixed coordinate to have terrain under it - this is what
        // used to drop players straight into the void.
        Vec3 landing = KraveLanding.findLanding(dest, KraveDimensions.PORTAL_LANDING, 6)
                .orElse(KraveDimensions.PORTAL_LANDING);

        player.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destLevel,
                                            java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(landing, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }
        });
        // Defense-in-depth: if the bounded search above somehow failed (should
        // be effectively unreachable given the guaranteed-landmass assumption
        // near the dimension origin), Slow Falling still keeps a fallback
        // fixed-point landing non-fatal instead of a hard crash-into-void.
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
    }

    /** The Kosmos always has exactly one Krave Monster - spawn him near the boss island the first time. */
    private void ensureBossExists(ServerLevel kosmos) {
        KraveKosmosData data = KraveKosmosData.get(kosmos);
        var id = data.getBossId();
        if (id != null) {
            var existing = kosmos.getEntity(id);
            if (existing instanceof KraveMonster monster && monster.isAlive()) {
                return;
            }
        }
        Vec3 pos = KraveDimensions.BOSS_ISLAND;
        BlockPos denCenter = net.minecraft.core.BlockPos.containing(pos.x, pos.y, pos.z);
        // Build the den (and its guaranteed-solid platform) before the boss
        // spawns, so he lands on authored ground rather than whatever the
        // procedural terrain happened to generate at the origin.
        com.barbarajones.dimension.KraveDenBuilder.buildDen(kosmos, denCenter);

        KraveMonster monster = ModEntities.KRAVE_MONSTER.get().create(kosmos);
        if (monster == null) {
            return;
        }
        monster.setPos(pos.x, pos.y, pos.z);
        kosmos.addFreshEntity(monster);
        data.setBossId(monster.getUUID());
        // The den's healing boxes were built before the boss existed, so they
        // had no target to latch onto yet - KraveHealingBox.resolveTarget()'s
        // KraveKosmosData fallback picks him up automatically on their next
        // heal tick now that setBossId() above has run, no extra wiring needed.
    }

    private CompoundTag persisted(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
