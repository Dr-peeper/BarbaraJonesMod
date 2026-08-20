package com.barbarajones.v2.bonds;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.housing.HousingResult;
import com.barbarajones.housing.HousingValidator;
import com.barbarajones.v2.village.KraveVillage;
import com.barbarajones.v2.village.VillageView;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * House Assignment: finds Cayden and Barbara somewhere to live in an actual
 * village (a real bed, in a real enclosed, lit, doored room - reusing the
 * exact {@link HousingValidator} Cayden's own manual claim already trusts)
 * and then keeps them there when the player is not around.
 *
 * <p>Genuinely calls the village module ({@code com.barbarajones.v2.village}):
 * {@link #findAndAssignHouse} asks {@code KraveVillage.nearest} for the
 * closest chartered settlement and, when there is one, prefers a bed inside
 * its claim over one outside it, so the companions settle IN the village the
 * player actually built rather than the first bed found in any direction.
 * {@link #settleTick} then calls {@code KraveVillage.adjustHappiness} on a
 * slow drip while they are home - a real, mechanical "the village is happier
 * for having them" rather than a flavour line.
 *
 * <p>Cayden already has a full manual "stand in the room, right-click him"
 * claim system (see {@code CaydenCobb.tryClaimHome}), but it is
 * player-position-driven and its {@code home} field has no public setter -
 * see the module doc for the one-line addition ({@code assignHome(BlockPos,
 * String)}) that would let this hand a discovered house straight to it. Until
 * that lands, both residents are driven the same way here: independently of
 * {@code CaydenCobb}'s own field, via direct navigation, which is enough to
 * make "settle in a village house and follow the player from there" true in
 * play even though Cayden's own {@code isHoused()} flag will not reflect an
 * auto-assigned house (only a manually claimed one).
 */
final class VillageHouseFinder {

    private static final int SEARCH_RADIUS = 48;
    /** Re-scan for a house this often once one search has already failed. */
    private static final int RETRY_TICKS = 20 * 30;
    /** How far a resident will wander from their assigned house before turning back. */
    private static final double HOME_LEASH = 20.0D;
    /** Inside this range of the owner, let normal follow/combat goals drive instead. */
    private static final double OWNER_NEAR_RANGE = 10.0D;
    /** How often a settled resident nudges their village's happiness. */
    private static final int HAPPINESS_INTERVAL = 20 * 60 * 2;

    private VillageHouseFinder() { }

    /**
     * Looks for an unclaimed bed inside a room that already passes every
     * {@link HousingValidator} requirement, closest first, preferring any bed
     * that falls inside a chartered {@code KraveVillage} claim over one that
     * does not. "Unclaimed" is checked against every other tracked resident
     * within the search radius so two companions never settle on the same bed.
     */
    static boolean findAndAssignHouse(ServerLevel level, Mob resident) {
        BlockPos origin = resident.blockPosition();
        List<BlockPos> beds;
        Optional<VillageView> chartered;
        try {
            PoiManager poi = level.getPoiManager();
            List<BlockPos> found = poi.findAll(h -> h.is(PoiTypes.HOME), pos -> true, origin, SEARCH_RADIUS,
                            PoiManager.Occupancy.ANY)
                    .map(BlockPos::immutable)
                    .collect(Collectors.toList());
            chartered = KraveVillage.nearest(level, origin);
            beds = found.stream()
                    // inside the chartered village claim sorts first, then closest first within each group
                    .sorted(Comparator
                            .<BlockPos>comparingInt(p -> chartered.isPresent() && chartered.get().contains(p) ? 0 : 1)
                            .thenComparingDouble(p -> p.distSqr(origin)))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            // Village POI lookups reach into chunk data; never let a bad scan
            // take the companion down with it. Rule #1 covers Cayden dying to
            // gameplay, not to a housing search throwing on an odd chunk.
            return false;
        }

        for (BlockPos bed : beds) {
            if (isClaimedByAnotherResident(level, resident, bed)) {
                continue;
            }
            HousingResult result = HousingValidator.validate(level, bed);
            if (result.valid) {
                BondState.setAssignedHouse(resident, result.anchor, level.dimension().location().toString());
                return true;
            }
        }
        return false;
    }

    private static boolean isClaimedByAnotherResident(ServerLevel level, Mob resident, BlockPos bed) {
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(bed).inflate(SEARCH_RADIUS + 8),
                e -> (e instanceof CaydenCobb || e instanceof BarbaraJones) && e != resident)) {
            BlockPos assigned = BondState.assignedHousePos(other);
            if (assigned != null && assigned.equals(bed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Call every tick (cheaply - it does nothing most calls). When the owner
     * is close, this steps back and lets the resident's own follow/combat AI
     * drive; otherwise it walks them home and lets them sit there, exactly
     * the shape of Cayden's own {@code CaydenFollowOrHomeGoal}.
     */
    static void settleTick(ServerLevel level, Mob resident, @Nullable LivingEntity owner) {
        if (owner != null && resident.distanceToSqr(owner) < OWNER_NEAR_RANGE * OWNER_NEAR_RANGE) {
            return;
        }
        if (resident.getTarget() != null) {
            return;   // mid-fight; do not drag them home under someone's fists
        }

        if (!BondState.hasAssignedHouse(resident)) {
            int cd = BondState.houseSearchCooldown(resident);
            if (cd > 0) {
                BondState.setHouseSearchCooldown(resident, Math.max(0, cd - BondState.TICK_STRIDE));
                return;
            }
            if (!findAndAssignHouse(level, resident)) {
                BondState.setHouseSearchCooldown(resident, RETRY_TICKS);
            }
            return;
        }

        BlockPos home = BondState.assignedHousePos(resident);
        String dim = BondState.assignedHouseDim(resident);
        if (home == null || dim == null || !dim.equals(level.dimension().location().toString())) {
            return;
        }
        if (resident.blockPosition().distSqr(home) > HOME_LEASH * HOME_LEASH) {
            resident.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D);
        } else {
            tickHappiness(level, resident, home);
        }
    }

    /** A slow, real happiness nudge (via the village module's own API) for having a bonded companion living there. */
    private static void tickHappiness(ServerLevel level, Mob resident, BlockPos home) {
        int cd = BondState.happinessCooldown(resident);
        if (cd > 0) {
            BondState.setHappinessCooldown(resident, Math.max(0, cd - BondState.TICK_STRIDE));
            return;
        }
        BondState.setHappinessCooldown(resident, HAPPINESS_INTERVAL);
        KraveVillage.adjustHappiness(level, home, 1);
    }
}
