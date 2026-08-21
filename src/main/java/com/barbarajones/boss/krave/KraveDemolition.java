package com.barbarajones.boss.krave;

import com.barbarajones.behavior.DelayedEffects;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The arena losing the fight.
 *
 * <p>By the end of a full gauntlet the Kosmos should barely be standing. That is
 * not decoration - a boss whose attacks leave the ground exactly as they found
 * it reads as a light show played over the terrain rather than as something
 * happening to it. When a wave passes and the ridge it crossed is simply gone,
 * the attack has a weight no amount of particles can give it.
 *
 * <p>Three things make that survivable rather than a lag machine and a softlock.
 *
 * <p><b>It never digs past the floor.</b> The Kosmos is end-island terrain over
 * open void, so carving straight down through the arena drops the fight into
 * nothing. Each boss remembers the height he started fighting at, and nothing
 * removes a block below the slab under it. The island erodes to a scarred plate
 * and stops: still standable, still fightable, visibly ruined.
 *
 * <p><b>It is budgeted.</b> A twenty-block sphere is thirty thousand blocks; a
 * disc of it is still twelve hundred columns, and doing that on one tick inside
 * an attack that fires every few seconds would end the server. Work is capped
 * per call, spread across ticks as rings travelling outward, and skips a random
 * share of columns - which costs nothing and looks better than a clean
 * geometric bite anyway.
 *
 * <p><b>It does not swallow anything valuable.</b> Unbreakable blocks are left
 * alone, and a block with an inventory is broken properly so its contents drop
 * rather than being deleted. Everything else vanishes without drops on purpose:
 * a thousand floating items is its own kind of crash.
 */
public final class KraveDemolition {

    private KraveDemolition() { }

    /**
     * Blocks one call may remove, for the set-piece moves.
     *
     * <p>Per call rather than global, because the callers are nothing alike. A
     * shockwave lands once and should take the ridge with it; a claw swipe
     * lands three times a second and must not.
     */
    public static final int BUDGET = 1400;

    /** What a routine hit gets - enough to scar the ground, not to excavate it. */
    public static final int BUDGET_LIGHT = 250;

    /**
     * The ceiling that actually protects the tick rate.
     *
     * <p>Per-call budgets bound one attack; they do not bound two dozen of
     * them landing together, which is exactly what the late-form barrages do.
     * Everything draws from one pool per tick, so a bombardment spreads itself
     * out instead of arriving as a freeze.
     */
    private static final int GLOBAL_PER_TICK = 1200;

    private static long budgetTick = -1L;
    private static int budgetSpent;

    /** Claims as much of this tick's allowance as is left, up to what was asked. */
    private static int claim(ServerLevel level, int want) {
        long now = level.getGameTime();
        if (now != budgetTick) {
            budgetTick = now;
            budgetSpent = 0;
        }
        int give = Math.max(0, Math.min(want, GLOBAL_PER_TICK - budgetSpent));
        budgetSpent += give;
        return give;
    }

    /** Share of columns left standing, for a ragged edge rather than a cut circle. */
    private static final float RAGGED = 0.30F;

    /**
     * How far under his starting feet the arena may be dug before it is bedrock
     * in all but name. Deep enough for a real crater, shallow enough that the
     * island underneath survives it.
     */
    private static final int MAX_DEPTH_BELOW_ARENA = 7;

    /** Whether this world lets a mob rearrange it at all. */
    private static boolean allowed(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /**
     * Flattens a disc: everything standing above the ground for {@code up}
     * blocks, and the ground itself for {@code down}.
     *
     * <p>Sized in blocks, not in fractions of him - callers pass the radius the
     * attack actually covers.
     */
    public static void carve(ServerLevel level, KraveMonster boss, Vec3 centre,
                             double radius, int up, int down, int budget) {
        if (!allowed(level)) {
            return;
        }
        carveBand(level, boss, centre, 0.0D, radius, up, down, budget);
    }

    /**
     * A wave of destruction travelling outward, one ring per step.
     *
     * <p>Carving the whole disc at once is both the expensive way and the
     * unreadable one - the ground is simply gone the instant the attack fires.
     * Rings arriving in sequence are what make it look like something crossing
     * the arena, and they spread the cost over the ticks it takes to get there.
     */
    public static void carveWave(ServerLevel level, KraveMonster boss, Vec3 centre,
                                 double maxRadius, int steps, int up, int down) {
        if (!allowed(level)) {
            return;
        }
        double band = maxRadius / steps;
        for (int s = 1; s <= steps; s++) {
            final double inner = band * (s - 1);
            final double outer = band * s;
            DelayedEffects.scheduleWorld(level, s * 3, () -> {
                if (boss.isAlive()) {
                    carveBand(level, boss, centre, inner, outer, up, down, BUDGET);
                }
            });
        }
    }

    /**
     * One annulus. The whole-disc case is just this with an inner radius of
     * zero, so there is one loop to get right instead of two that drift apart.
     */
    private static void carveBand(ServerLevel level, KraveMonster boss, Vec3 centre,
                                  double inner, double outer, int up, int down, int want) {
        int budget = claim(level, want);
        if (budget <= 0) {
            return;
        }
        int floor = floorFor(boss, centre);
        int r = Mth.ceil(outer);
        int cx = Mth.floor(centre.x);
        int cy = Mth.floor(centre.y);
        int cz = Mth.floor(centre.z);
        double inSqr = inner * inner;
        double outSqr = outer * outer;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r && budget > 0; dx++) {
            for (int dz = -r; dz <= r && budget > 0; dz++) {
                double d = dx * dx + dz * dz;
                if (d < inSqr || d > outSqr) {
                    continue;
                }
                if (level.random.nextFloat() < RAGGED) {
                    continue;
                }
                int bottom = Math.max(floor, cy - down);
                // Start from the actual surface, not from the top of the band.
                // Over open ground - which is most of a twenty-block disc - the
                // band is empty air, and scanning it block by block costs the
                // same as scanning solid rock while removing nothing. One
                // heightmap lookup replaces the whole wasted column.
                int top = Math.min(cy + up, level.getHeight(
                        Heightmap.Types.WORLD_SURFACE, cx + dx, cz + dz));
                for (int y = top; y >= bottom && budget > 0; y--) {
                    pos.set(cx + dx, y, cz + dz);
                    if (removeOne(level, pos)) {
                        budget--;
                    }
                }
            }
        }
    }

    /**
     * A crater under a single impact - deeper than a passing wave, and narrow
     * enough to afford the extra depth.
     */
    public static void crater(ServerLevel level, KraveMonster boss, Vec3 at,
                              double radius, int depth) {
        if (!allowed(level)) {
            return;
        }
        int floor = floorFor(boss, at);
        int cx = Mth.floor(at.x);
        int cy = Mth.floor(at.y);
        int cz = Mth.floor(at.z);
        int r = Mth.ceil(radius);
        int budget = claim(level, BUDGET);
        if (budget <= 0) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r && budget > 0; dx++) {
            for (int dz = -r; dz <= r && budget > 0; dz++) {
                double flat = Math.sqrt(dx * dx + dz * dz);
                if (flat > radius) {
                    continue;
                }
                // A bowl, not a cylinder: deepest at the point of impact and
                // shallowing to nothing at the rim.
                int dig = (int) Math.round(depth * (1.0D - flat / radius));
                int bottom = Math.max(floor, cy - dig);
                for (int y = cy + 2; y >= bottom && budget > 0; y--) {
                    pos.set(cx + dx, y, cz + dz);
                    if (removeOne(level, pos)) {
                        budget--;
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 1.0D, at.z,
                6, radius * 0.4D, 1.0D, radius * 0.4D, 0.0D);
    }

    /**
     * @return true only if a block was actually removed, so the budget pays for
     *         real work rather than for scanning empty air.
     */
    private static boolean removeOne(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        // Bedrock, barriers, portal frames - anything the game itself considers
        // unbreakable stays. A boss that eats the world border is not
        // impressive, it is a bug report.
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        // The Kraved Castle is not scenery to be cleared. Its courtyard is
        // this arena, so without this the boss demolishes the building he is
        // standing in - which is exactly what was happening.
        if (com.barbarajones.dimension.KraveArena.isProtected(level, pos)) {
            return false;
        }
        if (state.hasBlockEntity()) {
            // Break it properly so a chest spills rather than being deleted.
            // Rare enough that the extra cost does not matter.
            level.destroyBlock(pos, true);
            return true;
        }
        // One in twenty gets the full break effect. Every block sending its own
        // particle is a packet storm, and a sample reads exactly the same.
        if (level.random.nextInt(20) == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    4, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        // UPDATE_CLIENTS only: neighbour updates across thousands of blocks
        // cascade into far more work than the removal itself, and the leftover
        // floating torches are honestly an improvement.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    /**
     * The lowest level anything may be dug to around this boss.
     *
     * <p>Anchored to where he started fighting, not to where he is now. Tied to
     * his current position it would sink with him as the ground gave way, and
     * the arena would be dug out from under the fight one attack at a time until
     * everybody was in the void.
     */
    private static int floorFor(KraveMonster boss, Vec3 centre) {
        int anchor = boss.arenaFloor();
        int local = Mth.floor(centre.y) - MAX_DEPTH_BELOW_ARENA;
        // The HIGHER of the two, because both are floors and a guard has to
        // honour whichever binds first. An attack landing on a rise digs seven
        // blocks into the rise; one landing in a pit still stops at the arena
        // anchor rather than punching through the island.
        return Math.max(anchor, local);
    }
}
