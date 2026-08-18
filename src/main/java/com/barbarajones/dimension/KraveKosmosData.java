package com.barbarajones.dimension;

import com.barbarajones.entity.KraveMonster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Tracks the Krave Kosmos's one singleton Krave Monster, so the Overworld
 * pull triggers (Krave Box's SUMMON_KRAVE, the 10th Cayden death) can
 * relocate the SAME boss instead of spawning a duplicate, and so a second one
 * never gets created if a player re-enters the Kosmos while he's away.
 */
public class KraveKosmosData extends SavedData {

    private static final String KEY = "barbarajones_krave_kosmos";

    @Nullable
    private UUID bossId;

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

    /**
     * The two Overworld triggers (Krave Box summon, 10th Cayden death) call
     * this instead of spawning a fresh boss - it finds the SAME Kosmos-resident
     * Krave Monster (if he's alive and home) and relocates him. Returns null
     * if there's no living boss to pull, so callers can fall back to spawning
     * one fresh rather than softlocking the quest.
     */
    @Nullable
    public static KraveMonster pullBossToOverworld(MinecraftServer server, ServerLevel destLevel, Vec3 pos) {
        ServerLevel kosmos = server.getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null) {
            return null;
        }
        KraveKosmosData data = get(kosmos);
        UUID id = data.getBossId();
        if (id == null) {
            return null;
        }
        if (!(kosmos.getEntity(id) instanceof KraveMonster monster) || !monster.isAlive()) {
            return null;
        }
        if (monster.level() == destLevel) {
            // already out here somehow - just reposition it
            monster.teleportTo(pos.x, pos.y, pos.z);
            return monster;
        }
        var moved = monster.changeDimension(destLevel, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(net.minecraft.world.entity.Entity entity, ServerLevel dest,
                                            java.util.function.Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }
        });
        return moved instanceof KraveMonster m ? m : null;
    }

    private static KraveKosmosData load(CompoundTag tag) {
        KraveKosmosData data = new KraveKosmosData();
        if (tag.hasUUID("BossId")) {
            data.bossId = tag.getUUID("BossId");
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.bossId != null) {
            tag.putUUID("BossId", this.bossId);
        }
        return tag;
    }
}
