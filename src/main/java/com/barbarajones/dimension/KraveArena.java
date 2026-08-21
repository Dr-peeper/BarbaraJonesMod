package com.barbarajones.dimension;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;

/**
 * The Kraved Castle, declared off-limits to the fight happening on top of it.
 *
 * <h2>Why the castle was disappearing</h2>
 *
 * <p>It was never a worldgen bug, which is why looking at the generation code
 * found nothing wrong with it: {@link KraveDenBuilder#buildDen} runs exactly
 * once, behind a persisted {@code bossEverSpawned} flag in
 * {@link KraveKosmosData}, and re-entering the dimension does not call it
 * again. The structure was not being regenerated, overwritten or reset.
 *
 * <p>It was being blown up. The castle courtyard is the boss arena - the
 * Monster spawns in the open shaft at its centre by design - and everything
 * that happens in that arena destroys terrain: the Monster's own attacks carve
 * discs up to twenty-four blocks across, {@code KraveKosmosBattle} rains
 * meteors that detonate at explosion power ten with block damage on, and the
 * elite healing box detonates where it stands, which is the middle of the
 * courtyard. All of it lands on the castle.
 *
 * <p>What made it look like a dimension-travel bug is that until now Cayden
 * would acquire the Monster the moment a player set foot in the Kosmos and fly
 * off to fight him unprompted. The battle then ran - and demolished the
 * castle - whether or not anybody went to watch. The player would come back
 * later, find chunks of fortress missing, and reasonably conclude that
 * returning to the dimension had deleted them. The travel was a coincidence;
 * the unrequested fight was the cause.
 *
 * <h2>The fix</h2>
 *
 * <p>The castle's real placed bounds are recorded when it is built and saved
 * with the world, and every path that removes a block in the Kosmos asks here
 * first. Two chokepoints cover all of them:
 *
 * <ul>
 *   <li>{@code KraveDemolition.removeOne} for the Monster's own carving, and
 *   <li>{@link ExplosionEvent.Detonate} for everything else - meteors, healing
 *       boxes, the apocalypse sequence, Cayden's own attacks, and anything
 *       added later that nobody thought to wire up. Filtering the affected
 *       block list is what makes this robust against the next explosion
 *       somebody adds.
 * </ul>
 *
 * <p>Bounds are read from the placed structure rather than assumed from
 * constants, so the protected volume cannot drift out of step with the castle
 * if the schematic is ever swapped or moved.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class KraveArena {

    private KraveArena() { }

    /**
     * Slack around the recorded structure box.
     *
     * <p>The castle sits flush on an authored island, and an explosion that
     * eats the ground out from under a wall has destroyed the castle just as
     * surely as one that ate the wall. A few blocks of margin protects the
     * footing too.
     */
    private static final int MARGIN = 3;

    /**
     * Records where the castle actually landed. Called once, by the builder,
     * with the bounds the structure template reported - not with constants that
     * could drift away from it.
     */
    public static void protectCastle(ServerLevel kosmos, BoundingBox placed) {
        KraveKosmosData.get(kosmos).setCastleBounds(placed.inflatedBy(MARGIN));
    }

    /**
     * Whether this block is part of the castle and therefore not something the
     * boss fight is allowed to remove.
     *
     * <p>Deliberately a volume test rather than a block-identity test. The
     * castle is built from ordinary Krave stone that a player may also have
     * built with elsewhere, so protecting the material would make those blocks
     * indestructible all over the dimension; and protecting only the blocks
     * that are currently castle would let an attack punch a hole and then widen
     * it on the next pass, because the hole is air and air is not castle.
     */
    public static boolean isProtected(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)
                || !level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
            return false;
        }
        BoundingBox box = KraveKosmosData.get(server).getCastleBounds();
        return box != null && box.isInside(pos);
    }

    /**
     * Strips protected blocks out of every explosion in the Kosmos.
     *
     * <p>This is the catch-all. Meteors, the elite healing box, the apocalypse
     * sequence and Cayden's own detonations all reach the world through
     * {@code Level.explode}, and none of them know the castle exists. Editing
     * the affected-block list is the one place all of them pass through, which
     * also means an explosion added later is protected without anybody
     * remembering to wire it up.
     */
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel server)
                || !level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
            return;
        }
        BoundingBox box = KraveKosmosData.get(server).getCastleBounds();
        if (box == null) {
            return;
        }
        Iterator<BlockPos> it = event.getAffectedBlocks().iterator();
        while (it.hasNext()) {
            if (box.isInside(it.next())) {
                it.remove();
            }
        }
    }
}
