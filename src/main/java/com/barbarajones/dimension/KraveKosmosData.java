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
 * Tracks whether the Kosmos's own resident boss has ever been spawned, so
 * {@code KraveDoorBlock}'s door-entry spawn only ever builds the den and its
 * boss once, world-wide - never again after that, whether he's still alive,
 * dead, or just unloaded somewhere. The two Overworld summon triggers (Krave
 * Box's SUMMON_KRAVE, the 10th Cayden death) deliberately do NOT go through
 * this - they always spawn their own fresh encounter regardless, so this
 * class has nothing to do with them.
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
    private boolean bossEverSpawned;
    private boolean bossEverDefeated;
    private boolean leviathansEverSpawned;
    private boolean chocolateRaining;
    private int chocolateWeatherTimer;

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

    /**
     * Guards KraveDoorBlock.ensureBossExists so the den and its boss are only
     * ever built once, world-wide - deliberately independent of whether the
     * boss is currently alive or locatable. {@code getEntity(UUID)} only
     * finds currently-loaded entities, so gating on "is he still around"
     * meant every entry where he'd wandered off (or nobody had been near the
     * den in a while) looked like "no boss" and rebuilt the den from
     * scratch, silently erasing any changes a player had made near it.
     */
    public boolean isBossEverSpawned() {
        return this.bossEverSpawned;
    }

    public void setBossEverSpawned(boolean value) {
        this.bossEverSpawned = value;
        setDirty();
    }

    /** One-time flag: the Krave Leviathans are spawned exactly once, ever, the same way the boss is. */
    public boolean isLeviathansEverSpawned() {
        return this.leviathansEverSpawned;
    }

    public void setLeviathansEverSpawned(boolean value) {
        this.leviathansEverSpawned = value;
        setDirty();
    }

    /** Chocolate rain state, ticked by KraveChocolateWeather - same shape as vanilla's own rain/clear timer. */
    public boolean isChocolateRaining() {
        return this.chocolateRaining;
    }

    public void setChocolateRaining(boolean value) {
        this.chocolateRaining = value;
        setDirty();
    }

    public int getChocolateWeatherTimer() {
        return this.chocolateWeatherTimer;
    }

    public void setChocolateWeatherTimer(int value) {
        this.chocolateWeatherTimer = value;
        setDirty();
    }

    /** True once the Kosmos-resident boss (the one this class's own bossId names) has died at final form at least once. */
    public boolean isBossEverDefeated() {
        return this.bossEverDefeated;
    }

    public void setBossEverDefeated(boolean value) {
        this.bossEverDefeated = value;
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
        // Existing worlds saved before this flag existed still have a
        // recorded BossId if the boss was ever spawned - treat that as
        // "already spawned" too, so upgrading doesn't rebuild the den once
        // more on the next entry.
        data.bossEverSpawned = tag.getBoolean("BossEverSpawned") || tag.hasUUID("BossId");
        data.bossEverDefeated = tag.getBoolean("BossEverDefeated");
        data.leviathansEverSpawned = tag.getBoolean("LeviathansEverSpawned");
        data.chocolateRaining = tag.getBoolean("ChocolateRaining");
        data.chocolateWeatherTimer = tag.getInt("ChocolateWeatherTimer");

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
        tag.putBoolean("BossEverSpawned", this.bossEverSpawned);
        tag.putBoolean("BossEverDefeated", this.bossEverDefeated);
        tag.putBoolean("LeviathansEverSpawned", this.leviathansEverSpawned);
        tag.putBoolean("ChocolateRaining", this.chocolateRaining);
        tag.putInt("ChocolateWeatherTimer", this.chocolateWeatherTimer);

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
