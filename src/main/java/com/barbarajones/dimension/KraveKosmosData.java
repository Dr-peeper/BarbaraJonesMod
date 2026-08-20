package com.barbarajones.dimension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Tracks which Krave Monster is the Kosmos's own resident boss, so
 * {@code KraveDoorBlock}'s door-entry spawn never creates a second one while
 * he's still alive. The two Overworld summon triggers (Krave Box's
 * SUMMON_KRAVE, the 10th Cayden death) deliberately do NOT go through this -
 * they always spawn their own fresh encounter regardless of whether the
 * Kosmos-resident boss is alive, so this class has nothing to do with them.
 */
public class KraveKosmosData extends SavedData {

    private static final String KEY = "barbarajones_krave_kosmos";

    @Nullable
    private UUID bossId;
    private boolean landingBoxesSpawned;

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

    private static KraveKosmosData load(CompoundTag tag) {
        KraveKosmosData data = new KraveKosmosData();
        if (tag.hasUUID("BossId")) {
            data.bossId = tag.getUUID("BossId");
        }
        data.landingBoxesSpawned = tag.getBoolean("LandingBoxesSpawned");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.bossId != null) {
            tag.putUUID("BossId", this.bossId);
        }
        tag.putBoolean("LandingBoxesSpawned", this.landingBoxesSpawned);
        return tag;
    }
}
