package com.barbarajones.apocalypse;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.KraveTornado;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The world-reaction spectacle for a Cayden Cobb ascension.
 *
 * <p><b>The one call the orchestrator must add</b> - inside
 * {@code CaydenCobb.announceTier(int tier)}, right next to (after) its
 * existing call to {@code transformationSpectacle(tier)}, which that
 * method's own comment already calls "the ACTUAL transformation moment":
 *
 * <pre>{@code
 *     transformationSpectacle(tier);
 *     KraveQuake.onAscend((ServerLevel) level(), position(), tier);
 * }</pre>
 *
 * {@code CaydenCobb.onEnterKosmos()} also calls {@code becomeSuperSaiyan()}
 * directly, bypassing {@code announceTier} entirely (arriving in the Kosmos
 * is a quieter gate, not a fight-mid-transformation beat) - that path is
 * deliberately NOT wired to this by default. If a world reaction is wanted
 * there too, the same one-line call works from inside
 * {@code onEnterKosmos()} once {@code becomeSuperSaiyan()} has set the tier.
 *
 * <p>This class does not touch Cayden, his health, or his AI in any way -
 * everything here reads a position and a tier and acts on the world around
 * it, never on him. Rule #1 stays intact regardless of what this file does.
 *
 * <h2>What it does, scaled by tier (1 SSJ .. 6 ULTRA)</h2>
 * <ul>
 *   <li>An outward shockwave ring that travels {@link Quake#frontSpeed} blocks/tick
 *       out to {@link Quake#maxRadius} - SSJ barely reaches past melee range,
 *       ULTRA reaches ~65 blocks. Entities caught in the ring get a gentle
 *       upward/outward toss (never fall-damage strength) and creative
 *       players are skipped entirely.</li>
 *   <li>Loose natural blocks (flowers, saplings, leaves, grass, mushrooms -
 *       an explicit allow-list, never anything a player built) near the ring
 *       get kicked into the air as real vanilla {@link FallingBlockEntity}
 *       instances, which land and re-place themselves - "shaken loose", not
 *       destroyed.</li>
 *   <li>SSJ2 and up leave real, if sparse, ground scarring: a natural grass
 *       surface block a real chance is swapped for its bare-dirt equivalent
 *       - dimension-aware (krave_grass -&gt; krave_dirt in the Kosmos,
 *       grass_block -&gt; dirt anywhere else). SSJ1 stays cosmetic-only (the
 *       "tremor"), matching the brief.</li>
 *   <li>SSJ2 and up spawn {@link KraveTornado} funnels touching down at
 *       random points inside the disturbed radius, more of them and more
 *       often at higher tiers.</li>
 *   <li>Every player within 400 blocks gets a distant rumble - not on the
 *       same tick as the flash, but delayed by roughly how far away they
 *       are, so it arrives after, the way real thunder trails real
 *       lightning instead of landing on top of it.</li>
 * </ul>
 *
 * <p>Self-ticking and self-subscribed, the same shape as {@code
 * KraveApocalypse}/{@code KraveKosmosAmbience} but subscribed directly
 * ({@code @Mod.EventBusSubscriber}) instead of centrally ticked from
 * EventHandler, since this package cannot edit that file.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KraveQuake {

    private KraveQuake() { }

    private static final List<Quake> ACTIVE = new ArrayList<>();
    private static final double SUPPRESS_RADIUS_SQR = 10.0D * 10.0D;

    /**
     * The single entry point - see the class doc for the exact call site.
     * tier is an {@link AscensionLadder} rung; anything below {@code SSJ} is
     * ignored. Safe to call repeatedly - a new quake close to one already
     * running is suppressed rather than stacked, so a fight that escalates
     * tiers in quick succession does not spawn overlapping shockwaves.
     */
    public static void onAscend(@Nullable ServerLevel level, @Nullable Vec3 epicenter, int tier) {
        if (level == null || epicenter == null || tier < AscensionLadder.SSJ) {
            return;
        }
        for (Quake q : ACTIVE) {
            if (q.level == level && q.pos.distanceToSqr(epicenter) < SUPPRESS_RADIUS_SQR) {
                return;
            }
        }
        ACTIVE.add(new Quake(level, epicenter, Math.min(tier, AscensionLadder.ULTRA)));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Quake> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Quake q = it.next();
            boolean done;
            try {
                done = q.tick();
            } catch (Throwable err) {
                done = true;   // a cosmetic system must never wedge or crash the server
            }
            if (done) {
                it.remove();
            }
        }
    }

    /** Blocks natural enough to toss without it reading as griefing a player build. */
    private static boolean isLoose(BlockState state) {
        var b = state.getBlock();
        return state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.LEAVES)
                || b == Blocks.GRASS || b == Blocks.TALL_GRASS
                || b == Blocks.FERN || b == Blocks.LARGE_FERN
                || b == Blocks.DEAD_BUSH || b == Blocks.BROWN_MUSHROOM
                || b == Blocks.RED_MUSHROOM || b == Blocks.SWEET_BERRY_BUSH;
    }

    /** The one real, sparse, dimension-aware ground scar SSJ2+ leaves behind - null means "leave this block alone". */
    @Nullable
    private static BlockState crackedVariant(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.getBlock() == ModBlocks.KRAVE_GRASS.get()) {
            return ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        }
        return null;
    }

    /** One in-flight ascension event: an outward wavefront plus a queue of delayed, distance-scheduled rumbles. */
    private static final class Quake {
        final ServerLevel level;
        final Vec3 pos;
        final int tier;
        final RandomSource random;

        final double maxRadius;        // SSJ ~19 blocks .. ULTRA ~64 blocks
        final double frontSpeed;       // blocks/tick the shockwave travels outward
        final boolean crackGround;     // SSJ2+ only - SSJ1 stays cosmetic
        final int tornadoBudget;
        final int lifespanTicks;

        double frontRadius = 0.0D;
        int age = 0;
        int tornadoesSpawned = 0;
        int nextTornadoAt;

        final List<ServerPlayer> rumblePlayers = new ArrayList<>();
        final List<Integer> rumbleAt = new ArrayList<>();

        Quake(ServerLevel level, Vec3 pos, int tier) {
            this.level = level;
            this.pos = pos;
            this.tier = tier;
            this.random = level.random;
            this.maxRadius = 10.0D + tier * 9.0D;
            this.frontSpeed = 0.6D + tier * 0.18D;
            this.crackGround = tier >= AscensionLadder.SSJ2;
            this.tornadoBudget = tier >= AscensionLadder.SSJ2 ? (tier - 1) : 0;
            this.nextTornadoAt = 8 + random.nextInt(15);

            // Light before sound: the flash already fired (transformationSpectacle,
            // called right before KraveQuake.onAscend). Schedule the rumble to
            // arrive at each player on its own delay, roughly proportional to
            // distance, like real thunder trailing real lightning.
            double soundSpeed = 7.0D;   // blocks/tick - fast, not instant, at range
            int maxDelay = 0;
            for (ServerPlayer p : level.players()) {
                double dist = p.position().distanceTo(pos);
                if (dist > 400.0D) {
                    continue;
                }
                int delay = 4 + (int) (dist / soundSpeed);
                rumblePlayers.add(p);
                rumbleAt.add(delay);
                maxDelay = Math.max(maxDelay, delay);
            }

            int frontTicks = (int) Math.ceil(maxRadius / frontSpeed);
            int settleTicks = 20 + tier * 15;
            this.lifespanTicks = frontTicks + settleTicks + maxDelay + 5;
        }

        /** @return true once this quake is finished and should be removed from ACTIVE */
        boolean tick() {
            age++;
            fireDueRumbles();

            if (frontRadius < maxRadius) {
                double prev = frontRadius;
                frontRadius = Math.min(maxRadius, frontRadius + frontSpeed);
                shakeRing(prev, frontRadius);
            }

            if (tornadoesSpawned < tornadoBudget && age >= nextTornadoAt) {
                spawnTornado();
                tornadoesSpawned++;
                nextTornadoAt = age + 15 + random.nextInt(25);
            }

            return age >= lifespanTicks;
        }

        private void fireDueRumbles() {
            for (int i = rumbleAt.size() - 1; i >= 0; i--) {
                if (age < rumbleAt.get(i)) {
                    continue;
                }
                ServerPlayer p = rumblePlayers.get(i);
                if (p.isAlive()) {
                    float vol = 1.3F + tier * 0.18F;
                    level.playSound(null, p.blockPosition(), ModSounds.KRAVE_RUMBLE.get(),
                            SoundSource.AMBIENT, vol, 0.65F + random.nextFloat() * 0.15F);
                    if (tier >= AscensionLadder.ULTRA) {
                        level.playSound(null, p.blockPosition(), ModSounds.KRAVE_BOOM.get(),
                                SoundSource.AMBIENT, 1.1F, 0.45F);
                    }
                }
                rumbleAt.remove(i);
                rumblePlayers.remove(i);
            }
        }

        /** The wavefront: real entity/loose-block disturbance in the ring the shockwave just crossed. */
        private void shakeRing(double innerR, double outerR) {
            tossEntities(innerR, outerR);
            disturbGround(innerR, outerR);
        }

        private void tossEntities(double innerR, double outerR) {
            AABB box = new AABB(pos.x - outerR, pos.y - 6.0D, pos.z - outerR,
                    pos.x + outerR, pos.y + 10.0D, pos.z + outerR);
            double strength = 0.10D + tier * 0.045D;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    le -> !(le instanceof Player pl) || !pl.isCreative())) {
                double d = e.position().distanceTo(pos);
                if (d < innerR || d > outerR) {
                    continue;
                }
                Vec3 away = e.position().subtract(pos);
                double len = Math.max(0.5D, away.length());
                double kick = strength * (1.0D - d / maxRadius);
                e.setDeltaMovement(e.getDeltaMovement().add(
                        away.x / len * kick, 0.10D + kick * 0.35D, away.z / len * kick));
                e.hurtMarked = true;
            }
        }

        private void disturbGround(double innerR, double outerR) {
            int samples = 3 + tier * 2;
            for (int i = 0; i < samples; i++) {
                double ang = random.nextDouble() * Math.PI * 2.0D;
                double r = innerR + random.nextDouble() * Math.max(0.01D, outerR - innerR);
                int bx = (int) Math.floor(pos.x + Math.cos(ang) * r);
                int bz = (int) Math.floor(pos.z + Math.sin(ang) * r);
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);

                level.sendParticles(ParticleTypes.POOF, bx + 0.5D, surface + 0.1D, bz + 0.5D,
                        2, 0.3D, 0.05D, 0.3D, 0.01D);
                level.sendParticles(ParticleTypes.CRIT, bx + 0.5D, surface + 0.1D, bz + 0.5D,
                        1, 0.2D, 0.05D, 0.2D, 0.02D);

                BlockPos loose = new BlockPos(bx, surface, bz);
                BlockState looseState = level.getBlockState(loose);
                if (isLoose(looseState) && random.nextInt(3) == 0) {
                    tossBlock(loose, looseState);
                    continue;
                }

                if (crackGround && random.nextInt(6) == 0) {
                    BlockPos ground = new BlockPos(bx, surface - 1, bz);
                    BlockState groundState = level.getBlockState(ground);
                    BlockState cracked = crackedVariant(groundState);
                    if (cracked != null) {
                        level.setBlock(ground, cracked, 3);
                    }
                }
            }
        }

        private void tossBlock(BlockPos at, BlockState state) {
            FallingBlockEntity fb = FallingBlockEntity.fall(level, at, state);
            if (fb == null) {
                return;
            }
            double ang = random.nextDouble() * Math.PI * 2.0D;
            fb.setDeltaMovement(Math.cos(ang) * 0.15D, 0.35D + random.nextDouble() * 0.25D, Math.sin(ang) * 0.15D);
        }

        private void spawnTornado() {
            double ang = random.nextDouble() * Math.PI * 2.0D;
            double dist = 6.0D + random.nextDouble() * Math.max(6.0D, frontRadius);
            double tx = pos.x + Math.cos(ang) * dist;
            double tz = pos.z + Math.sin(ang) * dist;
            int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) tx, (int) tz);

            KraveTornado t = ModEntities.TORNADO.get().create(level);
            if (t == null) {
                return;
            }
            t.setPos(tx, ty, tz);
            level.addFreshEntity(t);
            level.playSound(null, new BlockPos((int) tx, ty, (int) tz), ModSounds.KRAVE_TORNADO.get(),
                    SoundSource.AMBIENT, 1.1F, 0.85F + random.nextFloat() * 0.2F);
        }
    }
}
