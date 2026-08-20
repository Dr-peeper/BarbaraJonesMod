package com.barbarajones.v2.build.block;

import com.barbarajones.v2.build.KraveBuild;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureGeometry;
import com.barbarajones.v2.build.def.StructureRegistry;
import com.barbarajones.v2.build.item.KraveSchematicItem;
import com.barbarajones.v2.build.place.BuildScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The heart of a placed building: what it is, who put it there, when, and
 * exactly what the ground looked like beforehand.
 *
 * <p>The undo snapshot is the interesting part. Every block the placer
 * overwrote is recorded as a block-state id in a flat array over the building's
 * bounding box, with -1 for positions that were never touched, then run-length
 * encoded before it goes into NBT. Terrain is enormously repetitive and
 * untouched runs collapse to a single pair, so a house that modified four
 * thousand positions typically stores a couple of hundred integers.
 *
 * <p>The one caveat, stated plainly: block-state ids are assigned at runtime
 * from the loaded block registry. They are stable for the life of a world whose
 * mod list does not change, which comfortably covers the
 * {@value com.barbarajones.v2.build.KraveBuild#REFUND_WINDOW_TICKS}-tick refund
 * window. If the mod list changes, an unexpired snapshot is treated as stale and
 * the refund is refused rather than restoring the wrong blocks.
 */
public class KraveCoreBlockEntity extends BlockEntity {

    private static final Logger LOG = LoggerFactory.getLogger("BarbaraJones/KraveCore");

    /** Beyond this many RLE integers the snapshot is dropped rather than bloating chunk NBT. */
    private static final int MAX_SNAPSHOT_INTS = 24000;

    @Nullable
    private ResourceLocation structureId;
    private Rotation rotation = Rotation.NONE;
    private BlockPos anchor = BlockPos.ZERO;
    @Nullable
    private UUID placer;
    @Nullable
    private BoundingBox box;
    @Nullable
    private int[] snapshotRle;
    private final List<UUID> spawned = new ArrayList<>();
    private long placedTime;
    private boolean consumed;
    /** Number of blocks in the registry when the snapshot was taken; a mismatch invalidates it. */
    private int registrySize;

    public KraveCoreBlockEntity(BlockPos pos, BlockState state) {
        super(KraveBuild.CORE_BLOCK_ENTITY.get(), pos, state);
    }

    // =====================================================================

    /** Called by the build job the moment the building finishes. */
    public void initialise(StructureDef def, Rotation rotation, BlockPos anchor, @Nullable UUID placer,
                           BoundingBox box, @Nullable int[] snapshot, List<UUID> spawned, long gameTime) {
        this.structureId = def.id();
        this.rotation = rotation;
        this.anchor = anchor;
        this.placer = placer;
        this.box = box;
        this.spawned.clear();
        this.spawned.addAll(spawned);
        this.placedTime = gameTime;
        this.consumed = false;
        this.registrySize = ForgeRegistries.BLOCKS.getValues().size();

        if (snapshot != null) {
            int[] rle = encode(snapshot);
            if (rle.length > MAX_SNAPSHOT_INTS) {
                LOG.info("Undo snapshot for {} is {} ints after packing, over the {} limit -"
                                + " keeping the building but dropping the undo.",
                        def.id(), rle.length, MAX_SNAPSHOT_INTS);
                this.snapshotRle = null;
            } else {
                this.snapshotRle = rle;
            }
        }
        setChanged();
    }

    /** Refreshes the spawned-entity list after the completion hooks have run. */
    public void setSpawned(List<UUID> ids) {
        spawned.clear();
        spawned.addAll(ids);
        setChanged();
    }

    // =====================================================================

    @Nullable
    public ResourceLocation structureId() {
        return structureId;
    }

    @Nullable
    public StructureDef def() {
        return StructureRegistry.get(structureId);
    }

    public Rotation rotation() {
        return rotation;
    }

    public BlockPos anchor() {
        return anchor;
    }

    @Nullable
    public UUID placer() {
        return placer;
    }

    @Nullable
    public BoundingBox box() {
        return box;
    }

    public long placedTime() {
        return placedTime;
    }

    /** Ticks left on the refund window; zero once it has closed. */
    public long refundTicksLeft(long gameTime) {
        if (consumed) {
            return 0L;
        }
        long elapsed = gameTime - placedTime;
        return Math.max(0L, KraveBuild.REFUND_WINDOW_TICKS - elapsed);
    }

    public boolean canRefund(long gameTime) {
        return !consumed && structureId != null && snapshotRle != null && box != null
                && refundTicksLeft(gameTime) > 0L;
    }

    // =====================================================================

    /**
     * The undo. Called from {@link KraveCoreBlock#playerWillDestroy} while the
     * block entity is still alive.
     *
     * @return a message for the player, or null if this was not a refundable break
     */
    @Nullable
    public Component tryRefund(ServerLevel level, Player player) {
        if (structureId == null) {
            return null;
        }
        if (consumed) {
            return null;
        }
        long now = level.getGameTime();
        if (refundTicksLeft(now) <= 0L) {
            return Component.translatable("barbarajones.build.undo.too_late");
        }
        if (placer != null && !placer.equals(player.getUUID())) {
            return Component.translatable("barbarajones.build.undo.not_yours");
        }
        if (snapshotRle == null || box == null) {
            return Component.translatable("barbarajones.build.undo.no_snapshot");
        }
        if (registrySize != 0 && registrySize != ForgeRegistries.BLOCKS.getValues().size()) {
            return Component.translatable("barbarajones.build.undo.stale");
        }

        consumed = true;
        setChanged();

        // Take the residents with it. Never a player, and never Cayden - rule
        // one of this mod is that Cayden does not die, and being deleted by an
        // undo counts.
        for (UUID id : spawned) {
            Entity entity = level.getEntity(id);
            if (entity == null || entity instanceof Player) {
                continue;
            }
            ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (type != null && type.getPath().contains("cayden")) {
                continue;
            }
            entity.discard();
        }

        int volume = box.getXSpan() * box.getYSpan() * box.getZSpan();
        int[] flat = decode(snapshotRle, volume);
        Map<BlockPos, BlockState> restore = new HashMap<>();
        int index = 0;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    int id = flat[index++];
                    if (id >= 0) {
                        restore.put(new BlockPos(x, y, z), Block.stateById(id));
                    }
                }
            }
        }

        BuildScheduler.submitUndo(level, box, restore);

        ItemStack refund = KraveSchematicItem.forStructure(structureId);
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
        level.playSound(null, getBlockPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.5F);

        StructureDef def = def();
        return Component.translatable("barbarajones.build.undo.done",
                def == null ? Component.literal(structureId.toString()) : Component.translatable(def.nameKey()));
    }

    // =====================================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (structureId != null) {
            tag.putString("Structure", structureId.toString());
        }
        tag.putByte("Rot", (byte) StructureGeometry.index(rotation));
        tag.putLong("Anchor", anchor.asLong());
        if (placer != null) {
            tag.putUUID("Placer", placer);
        }
        if (box != null) {
            tag.putIntArray("Box", new int[] {
                    box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
        }
        if (snapshotRle != null) {
            tag.putIntArray("Snap", snapshotRle);
        }
        if (!spawned.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID id : spawned) {
                list.add(NbtUtils.createUUID(id));
            }
            tag.put("Spawned", list);
        }
        tag.putLong("Placed", placedTime);
        tag.putBoolean("Consumed", consumed);
        tag.putInt("RegSize", registrySize);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        structureId = tag.contains("Structure")
                ? ResourceLocation.tryParse(tag.getString("Structure")) : null;
        rotation = StructureGeometry.byIndex(tag.getByte("Rot"));
        anchor = BlockPos.of(tag.getLong("Anchor"));
        placer = tag.hasUUID("Placer") ? tag.getUUID("Placer") : null;
        if (tag.contains("Box")) {
            int[] b = tag.getIntArray("Box");
            box = b.length == 6 ? new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]) : null;
        } else {
            box = null;
        }
        snapshotRle = tag.contains("Snap") ? tag.getIntArray("Snap") : null;
        spawned.clear();
        if (tag.contains("Spawned", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Spawned", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < list.size(); i++) {
                spawned.add(NbtUtils.loadUUID(list.get(i)));
            }
        }
        placedTime = tag.getLong("Placed");
        consumed = tag.getBoolean("Consumed");
        registrySize = tag.getInt("RegSize");
    }

    // =====================================================================
    // Run-length coding. Public so tests and debug tooling can round-trip it.
    // =====================================================================

    /** Packs a flat array into (count, value) pairs. */
    public static int[] encode(int[] data) {
        List<Integer> out = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int value = data[i];
            int run = 1;
            while (i + run < data.length && data[i + run] == value) {
                run++;
            }
            out.add(run);
            out.add(value);
            i += run;
        }
        int[] packed = new int[out.size()];
        for (int j = 0; j < packed.length; j++) {
            packed[j] = out.get(j);
        }
        return packed;
    }

    /** Unpacks (count, value) pairs back into a flat array of exactly {@code length}. */
    public static int[] decode(int[] packed, int length) {
        int[] out = new int[length];
        java.util.Arrays.fill(out, -1);
        int index = 0;
        for (int i = 0; i + 1 < packed.length; i += 2) {
            int run = packed[i];
            int value = packed[i + 1];
            for (int j = 0; j < run && index < length; j++) {
                out[index++] = value;
            }
        }
        return out;
    }
}
