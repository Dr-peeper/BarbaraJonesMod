package com.barbarajones.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks which Krave Monster is the Kosmos's own resident boss, so
 * {@code KraveDoorBlock}'s door-entry spawn never creates a second one while
 * he's still alive. The two Overworld summon triggers (Krave Box's
 * SUMMON_KRAVE, the 10th Cayden death) deliberately do NOT go through this -
 * they always spawn their own fresh encounter regardless of whether the
 * Kosmos-resident boss is alive, so this class has nothing to do with them.
 *
 * <p>Also the permanent registry of portal pairs: every chocolate room a
 * player builds anywhere gets its own independent partner room here in the
 * Kosmos, created once and remembered forever after - closing either door
 * always returns to the same partner, never a fresh one.
 */
public class KraveKosmosData extends SavedData {

    private static final String KEY = "barbarajones_krave_kosmos";

    @Nullable
    private UUID bossId;
    private boolean landingBoxesSpawned;

    private final Map<BlockPos, GlobalPos> kosmosToExternal = new HashMap<>();
    private final Map<GlobalPos, BlockPos> externalToKosmos = new HashMap<>();

    public static KraveKosmosData get(ServerLevel kosmos) {
        return kosmos.getDataStorage().computeIfAbsent(KraveKosmosData::load, KraveKosmosData::new, KEY);
    }

    @Nullable
    public UUID getBossId() {
        return this.bossId;
    }

    public void setBossId(@Nullable UUID id) {
        this.bossId = id;
        setDirty();
    }

    /** Guards KraveDoorBlock.ensureLandingBoxesExist so the four landing-island boxes are placed exactly once. */
    public boolean isLandingBoxesSpawned() {
        return this.landingBoxesSpawned;
    }

    public void setLandingBoxesSpawned(boolean value) {
        this.landingBoxesSpawned = value;
        setDirty();
    }

    /** The Kosmos-side door paired with this overworld (or other-dimension) door, or null if it has never been used. */
    @Nullable
    public BlockPos kosmosDoorFor(GlobalPos external) {
        return this.externalToKosmos.get(external);
    }

    /** The far door paired with this Kosmos-side door, or null if it's somehow orphaned (should never happen). */
    @Nullable
    public GlobalPos externalDoorFor(BlockPos kosmosDoorPos) {
        return this.kosmosToExternal.get(kosmosDoorPos);
    }

    /** Records a new permanent pairing. Call exactly once, the moment a Kosmos-side room is first built for a door. */
    public void link(GlobalPos external, BlockPos kosmosDoorPos) {
        this.kosmosToExternal.put(kosmosDoorPos, external);
        this.externalToKosmos.put(external, kosmosDoorPos);
        setDirty();
    }

    private static KraveKosmosData load(CompoundTag tag) {
        KraveKosmosData data = new KraveKosmosData();
        if (tag.hasUUID("BossId")) {
            data.bossId = tag.getUUID("BossId");
        }
        data.landingBoxesSpawned = tag.getBoolean("LandingBoxesSpawned");

        if (tag.contains("PortalLinks")) {
            ListTag links = tag.getList("PortalLinks", Tag.TAG_COMPOUND);
            for (int i = 0; i < links.size(); i++) {
                CompoundTag entry = links.getCompound(i);
                BlockPos kosmosPos = new BlockPos(entry.getInt("KX"), entry.getInt("KY"), entry.getInt("KZ"));
                ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                        new ResourceLocation(entry.getString("EDim")));
                BlockPos externalPos = new BlockPos(entry.getInt("EX"), entry.getInt("EY"), entry.getInt("EZ"));
                GlobalPos external = GlobalPos.of(dim, externalPos);
                data.kosmosToExternal.put(kosmosPos, external);
                data.externalToKosmos.put(external, kosmosPos);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.bossId != null) {
            tag.putUUID("BossId", this.bossId);
        }
        tag.putBoolean("LandingBoxesSpawned", this.landingBoxesSpawned);

        ListTag links = new ListTag();
        for (Map.Entry<BlockPos, GlobalPos> e : this.kosmosToExternal.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("KX", e.getKey().getX());
            entry.putInt("KY", e.getKey().getY());
            entry.putInt("KZ", e.getKey().getZ());
            entry.putString("EDim", e.getValue().dimension().location().toString());
            entry.putInt("EX", e.getValue().pos().getX());
            entry.putInt("EY", e.getValue().pos().getY());
            entry.putInt("EZ", e.getValue().pos().getZ());
            links.add(entry);
        }
        tag.put("PortalLinks", links);
        return tag;
    }
}
