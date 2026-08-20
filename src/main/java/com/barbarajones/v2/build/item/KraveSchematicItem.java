package com.barbarajones.v2.build.item;

import com.barbarajones.v2.build.KraveBuild;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * One dedicated schematic per building - right-click a block to stamp that
 * building down, growing in gradually over {@link StructureDef#buildTicks()}
 * via {@link KraveBuild}. Each instance is bound to exactly one structure id
 * ({@code KraveHouses} registers ten of these, one per building), which is
 * also what a refund elsewhere in the village module hands back.
 */
public class KraveSchematicItem extends Item {

    private final ResourceLocation structureId;

    public KraveSchematicItem(Item.Properties properties, ResourceLocation structureId) {
        super(properties);
        this.structureId = structureId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        StructureDef def = StructureRegistry.get(this.structureId);
        if (def == null) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        BlockPos placedAgainst = context.getClickedPos().relative(context.getClickedFace());
        BlockPos origin = placedAgainst.subtract(def.core());

        if (!groundIsLevelEnough(serverLevel, def, origin)) {
            if (player != null) {
                player.displayClientMessage(Component.literal(
                        "The ground here is too uneven for this building."), true);
            }
            return InteractionResult.FAIL;
        }

        KraveBuild.enqueue(serverLevel, def, origin);

        if (player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Scans every ground column under the footprint (from {@link
     * StructureDef#cells()}'s local X/Z bounds, translated to world space)
     * and rejects the placement if the terrain varies by more than {@link
     * StructureDef#maxGroundDelta()} - a lopsided building half-buried on one
     * side and floating on the other is worse than just refusing to build.
     */
    private boolean groundIsLevelEnough(ServerLevel level, StructureDef def, BlockPos origin) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (StructureDef.Cell cell : def.cells()) {
            minX = Math.min(minX, cell.pos().getX());
            maxX = Math.max(maxX, cell.pos().getX());
            minZ = Math.min(minZ, cell.pos().getZ());
            maxZ = Math.max(maxZ, cell.pos().getZ());
        }
        if (minX > maxX) {
            return true;   // an empty structure has nothing to validate
        }

        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int worldX = origin.getX() + x;
                int worldZ = origin.getZ() + z;
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
            }
        }
        return (highest - lowest) <= def.maxGroundDelta();
    }
}
