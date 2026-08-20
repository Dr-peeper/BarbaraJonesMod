package com.barbarajones.v2.bonds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * ONE OF EACH, ENFORCED: the world-level registry {@link CanonicalGuardEvents}
 * uses to keep exactly one Barbara Jones and one original Cayden Cobb alive.
 *
 * <p>A vanilla {@link SavedData}, so it lives in the world's own save file
 * (level data storage, keyed under {@link #NAME}) rather than in memory - it
 * has to survive a server restart, unlike {@code CaydenCobb}'s own
 * in-memory {@code ASCENSION_LEGACY} map, or the very first restart after a
 * death would make the enforcement forget who the canonical pair even were.
 *
 * <p>Only tracks identity (a UUID each) plus a periodically-refreshed
 * best-known snapshot of position and Krave progress, kept only as a
 * last-resort seed for {@link CanonicalGuardEvents}'s rebuild-if-truly-lost
 * path - the normal death/respawn cycle is (and stays) handled by
 * {@code KraveApocalypse}; this never tries to out-race it.
 */
public class CanonicalRegistry extends SavedData {

    private static final String NAME = "barbarajones_bonds_canonical";

    @Nullable private UUID barbaraId;
    @Nullable private UUID caydenId;

    private double caydenX, caydenY, caydenZ;
    private boolean hasCaydenPos;
    private int caydenFedSnapshot;
    private boolean caydenRageSnapshot;
    private int caydenKiSnapshot;
    private int caydenUnlockMaskSnapshot;

    private double barbaraX, barbaraY, barbaraZ;
    private boolean hasBarbaraPos;

    /** Ticks the canonical Cayden/Barbara have been observed missing; the restore safety-net's grace timer. */
    private int caydenMissingTicks;
    private int barbaraMissingTicks;

    public CanonicalRegistry() { }

    public static CanonicalRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CanonicalRegistry::load, CanonicalRegistry::new, NAME);
    }

    private static CanonicalRegistry load(CompoundTag tag) {
        CanonicalRegistry r = new CanonicalRegistry();
        if (tag.hasUUID("Barbara")) {
            r.barbaraId = tag.getUUID("Barbara");
        }
        if (tag.hasUUID("Cayden")) {
            r.caydenId = tag.getUUID("Cayden");
        }
        r.caydenFedSnapshot = tag.getInt("CaydenFed");
        r.caydenRageSnapshot = tag.getBoolean("CaydenRage");
        r.caydenKiSnapshot = tag.getInt("CaydenKi");
        r.caydenUnlockMaskSnapshot = tag.getInt("CaydenUnlocks");
        if (tag.contains("CaydenX")) {
            r.hasCaydenPos = true;
            r.caydenX = tag.getDouble("CaydenX");
            r.caydenY = tag.getDouble("CaydenY");
            r.caydenZ = tag.getDouble("CaydenZ");
        }
        if (tag.contains("BarbaraX")) {
            r.hasBarbaraPos = true;
            r.barbaraX = tag.getDouble("BarbaraX");
            r.barbaraY = tag.getDouble("BarbaraY");
            r.barbaraZ = tag.getDouble("BarbaraZ");
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.barbaraId != null) {
            tag.putUUID("Barbara", this.barbaraId);
        }
        if (this.caydenId != null) {
            tag.putUUID("Cayden", this.caydenId);
        }
        tag.putInt("CaydenFed", this.caydenFedSnapshot);
        tag.putBoolean("CaydenRage", this.caydenRageSnapshot);
        tag.putInt("CaydenKi", this.caydenKiSnapshot);
        tag.putInt("CaydenUnlocks", this.caydenUnlockMaskSnapshot);
        if (this.hasCaydenPos) {
            tag.putDouble("CaydenX", this.caydenX);
            tag.putDouble("CaydenY", this.caydenY);
            tag.putDouble("CaydenZ", this.caydenZ);
        }
        if (this.hasBarbaraPos) {
            tag.putDouble("BarbaraX", this.barbaraX);
            tag.putDouble("BarbaraY", this.barbaraY);
            tag.putDouble("BarbaraZ", this.barbaraZ);
        }
        return tag;
    }

    // ---- Cayden --------------------------------------------------------------

    @Nullable
    public UUID caydenId() {
        return this.caydenId;
    }

    public void setCayden(UUID id) {
        this.caydenId = id;
        this.caydenMissingTicks = 0;
        setDirty();
    }

    public void clearCaydenMissing() {
        this.caydenMissingTicks = 0;
    }

    /** Advances the "how long has he been gone" clock and returns the new value. */
    public int incCaydenMissing(int byTicks) {
        this.caydenMissingTicks += byTicks;
        return this.caydenMissingTicks;
    }

    public void snapshotCayden(int fed, boolean rage, int ki, int unlockMask, Vec3 pos) {
        this.caydenFedSnapshot = fed;
        this.caydenRageSnapshot = rage;
        this.caydenKiSnapshot = ki;
        this.caydenUnlockMaskSnapshot = unlockMask;
        this.caydenX = pos.x;
        this.caydenY = pos.y;
        this.caydenZ = pos.z;
        this.hasCaydenPos = true;
        setDirty();
    }

    public int caydenFedSnapshot() {
        return this.caydenFedSnapshot;
    }

    public boolean caydenRageSnapshot() {
        return this.caydenRageSnapshot;
    }

    public int caydenKiSnapshot() {
        return this.caydenKiSnapshot;
    }

    public int caydenUnlockMaskSnapshot() {
        return this.caydenUnlockMaskSnapshot;
    }

    public Vec3 caydenSnapshotPos(ServerLevel fallbackLevel) {
        return this.hasCaydenPos ? new Vec3(this.caydenX, this.caydenY, this.caydenZ)
                : Vec3.atBottomCenterOf(fallbackLevel.getSharedSpawnPos());
    }

    // ---- Barbara ---------------------------------------------------------------

    @Nullable
    public UUID barbaraId() {
        return this.barbaraId;
    }

    public void setBarbara(UUID id) {
        this.barbaraId = id;
        this.barbaraMissingTicks = 0;
        setDirty();
    }

    public void clearBarbaraMissing() {
        this.barbaraMissingTicks = 0;
    }

    public int incBarbaraMissing(int byTicks) {
        this.barbaraMissingTicks += byTicks;
        return this.barbaraMissingTicks;
    }

    public void snapshotBarbara(Vec3 pos) {
        this.barbaraX = pos.x;
        this.barbaraY = pos.y;
        this.barbaraZ = pos.z;
        this.hasBarbaraPos = true;
        setDirty();
    }

    public Vec3 barbaraSnapshotPos(ServerLevel fallbackLevel) {
        return this.hasBarbaraPos ? new Vec3(this.barbaraX, this.barbaraY, this.barbaraZ)
                : Vec3.atBottomCenterOf(fallbackLevel.getSharedSpawnPos());
    }
}
