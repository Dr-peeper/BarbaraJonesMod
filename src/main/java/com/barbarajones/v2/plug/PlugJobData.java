package com.barbarajones.v2.plug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every job The Plug is out on and everything he thinks of everyone, saved with
 * the world.
 *
 * <p>Stored as a {@code SavedData}, the same way {@code KraveVillageData} and
 * {@code KraveKosmosData} store their state, and for the same reason: a job has
 * to survive a logout, a chunk unload and a server restart, and a static field
 * survives none of those. It is deliberately kept on the <b>overworld</b> and
 * nowhere else, giving one table for the whole server, because both halves of
 * what it holds outlive the dimension they were created in - the player can hire
 * him in the Kosmos and collect in the overworld, and his opinion of you is not
 * per-dimension.
 *
 * <p>One contract per employer, not per Plug. You cannot have two of him out at
 * once; that is the scarcity the whole thing runs on, and it also means "is
 * there a job on" is a single map lookup by player id rather than a search.
 *
 * <p><b>setDirty.</b> Every mutating method here calls {@link #setDirty()}
 * itself. Anything that reaches a {@link PlugContract} or {@link PlugReputation}
 * through this class and changes it must call {@link #setDirty()} afterwards -
 * this is the usual way save-data changes silently vanish on reload.
 */
public class PlugJobData extends SavedData {

    /** The SavedData file name, written into the overworld's data folder. */
    public static final String KEY = "barbarajones_plug_jobs";

    private final Map<UUID, PlugContract> contracts = new LinkedHashMap<>();
    private final Map<UUID, PlugReputation> reputations = new LinkedHashMap<>();

    /** Fetches (or creates) the one table. Cheap - the storage caches the instance. */
    public static PlugJobData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(PlugJobData::load, PlugJobData::new, KEY);
    }

    /**
     * The table if one has ever been written, otherwise null.
     *
     * <p>Used by the slow timer and by the presence sync, so a world where
     * nobody has ever hired him never gets the file written at all and pays one
     * cached map lookup for having the module installed.
     */
    @Nullable
    public static PlugJobData getExisting(MinecraftServer server) {
        return server.overworld().getDataStorage().get(PlugJobData::load, KEY);
    }

    // ---- reputation ----------------------------------------------------------

    /**
     * This player's record if he has one, otherwise null.
     *
     * <p>For the paths that only want to <em>read</em> - swinging on him is the
     * one that matters - so a player who has never spoken to him does not get an
     * empty record written out just for throwing a punch.
     */
    @Nullable
    public PlugReputation peek(UUID player) {
        return this.reputations.get(player);
    }

    /** This player's record, created on first use. */
    public PlugReputation reputation(UUID player) {
        PlugReputation rep = this.reputations.get(player);
        if (rep == null) {
            rep = new PlugReputation();
            this.reputations.put(player, rep);
            setDirty();
        }
        return rep;
    }

    // ---- contracts -----------------------------------------------------------

    @Nullable
    public PlugContract contract(UUID employer) {
        return this.contracts.get(employer);
    }

    public void put(PlugContract contract) {
        this.contracts.put(contract.employer(), contract);
        setDirty();
    }

    public void clearContract(UUID employer) {
        if (this.contracts.remove(employer) != null) {
            setDirty();
        }
    }

    /**
     * Whether this particular Plug is currently out on somebody's job.
     *
     * <p>Walks the contract map, which holds at most one entry per player who
     * has ever hired him and is realistically a handful of entries. That is
     * cheap enough to ask once a second per loaded Plug and keeps the answer in
     * one place instead of caching a second copy that could drift.
     */
    public boolean isPlugAway(UUID plugId) {
        for (PlugContract contract : this.contracts.values()) {
            if (contract.isAway() && plugId.equals(contract.plug())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Brings every contract up to date with the clock: anything that has come
     * due gets its haul rolled and its employer told.
     *
     * <p>Called from the slow timer in {@link PlugEvents} and again from any
     * interaction with him, so the two can never disagree about whether a job is
     * finished - there is one routine and it is idempotent.
     */
    public void refresh(MinecraftServer server) {
        if (this.contracts.isEmpty()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        boolean changed = false;

        for (PlugContract contract : new ArrayList<>(this.contracts.values())) {
            if (!contract.isAway() || !contract.isDue(now)) {
                continue;
            }
            contract.finish(overworld.getRandom());
            changed = true;

            ServerPlayer employer = server.getPlayerList().getPlayer(contract.employer());
            if (employer != null) {
                PlugLines.say(employer, PlugLines.pick(overworld.getRandom(), PlugLines.RETURNED));
            }
        }
        if (changed) {
            setDirty();
        }
    }

    // ---- persistence ---------------------------------------------------------

    public static PlugJobData load(CompoundTag tag) {
        PlugJobData data = new PlugJobData();

        ListTag jobs = tag.getList("Contracts", Tag.TAG_COMPOUND);
        for (int i = 0; i < jobs.size(); i++) {
            try {
                PlugContract contract = PlugContract.load(jobs.getCompound(i));
                data.contracts.put(contract.employer(), contract);
            } catch (RuntimeException broken) {
                // One unreadable contract must not cost everyone else their job
                // and every player their standing with him.
                com.mojang.logging.LogUtils.getLogger().error(
                        "[barbarajones] dropped an unreadable Plug contract", broken);
            }
        }

        ListTag reps = tag.getList("Reputations", Tag.TAG_COMPOUND);
        for (int i = 0; i < reps.size(); i++) {
            CompoundTag entry = reps.getCompound(i);
            if (!entry.hasUUID("Player")) {
                continue;
            }
            data.reputations.put(entry.getUUID("Player"), PlugReputation.load(entry));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag jobs = new ListTag();
        for (PlugContract contract : this.contracts.values()) {
            jobs.add(contract.save());
        }
        tag.put("Contracts", jobs);

        ListTag reps = new ListTag();
        for (Map.Entry<UUID, PlugReputation> entry : this.reputations.entrySet()) {
            CompoundTag saved = entry.getValue().save();
            saved.putUUID("Player", entry.getKey());
            reps.add(saved);
        }
        tag.put("Reputations", reps);
        return tag;
    }
}
