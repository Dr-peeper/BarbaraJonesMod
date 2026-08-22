package com.barbarajones.worldgen.feature;

import net.minecraft.util.RandomSource;

/**
 * Shared "make a circle look organic" helper for the Krave Kosmos terrain
 * features (mountains, peaks, valleys): summing a few random cosine "lobes"
 * against the angle around a shape's center turns a perfectly round
 * cross-section into an irregular, natural-looking one, without needing real
 * 2D/3D noise infrastructure. Pure functions only - no shared state, since
 * Feature instances are reused across concurrently-decorated chunks.
 */
final class KraveTerrainShape {

    /**
     * The furthest a feature may write from its own origin, horizontally.
     *
     * <p>During the {@code features} generation step a feature is only allowed
     * to touch the 3x3 chunks around the one being decorated. Its origin can
     * sit anywhere inside the middle chunk, so the only offset that is legal
     * no matter where the origin landed is +-16; this leaves a block of
     * margin on top of that.
     *
     * <p>Writes past it are not clipped, they are <em>dropped</em> -
     * {@code WorldGenRegion.setBlock} refuses them, logs "Detected setBlock in
     * a far chunk", and carries on. The mountain feature was overrunning this
     * by up to eleven blocks, so mountains near a chunk boundary generated
     * with whole slices missing and each one wrote a few hundred error lines
     * on its way. Nothing crashes, which is exactly why it survived so long.
     */
    static final int MAX_WRITE_OFFSET = 15;

    private KraveTerrainShape() { }

    /**
     * The largest value {@link #lobeMultiplier} can return for these lobes -
     * every cosine term peaking at once. Worst case rather than typical on
     * purpose: a shape only has to cross the write boundary at one angle to
     * lose blocks there.
     */
    static double maxMultiplier(double[][] lobes) {
        double m = 1.0;
        for (double[] lobe : lobes) {
            m += Math.abs(lobe[2]);
        }
        return m;
    }

    /**
     * Shrinks a base radius, if needed, so the widest point of the resulting
     * shape still lands inside {@link #MAX_WRITE_OFFSET}.
     *
     * <p>Scaling the radius rather than clamping the individual writes is what
     * keeps the silhouette intact: a clamp would cut the shape off against a
     * straight chunk-shaped edge, whereas this just builds a slightly smaller
     * mountain that still tapers to nothing on its own terms.
     */
    static int fitBaseRadius(int baseRadius, double[][] lobes) {
        double worst = maxMultiplier(lobes);
        int fitted = (int) Math.floor(MAX_WRITE_OFFSET / worst);
        return Math.max(2, Math.min(baseRadius, fitted));
    }

    /** Belt-and-braces guard for write loops, so a shape can never overrun even if its own maths drifts. */
    static boolean withinWriteRange(int dx, int dz) {
        return Math.abs(dx) <= MAX_WRITE_OFFSET && Math.abs(dz) <= MAX_WRITE_OFFSET;
    }

    /**
     * Each row is {phase, frequency, amplitude} for one lobe. Frequency is
     * an integer (2-4) so the lobe wraps cleanly around a full circle.
     */
    static double[][] randomLobes(RandomSource random, int minCount, int maxCount,
                                  double minAmp, double maxAmp) {
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        double[][] lobes = new double[count][3];
        for (int i = 0; i < count; i++) {
            lobes[i][0] = random.nextDouble() * Math.PI * 2.0;          // phase
            lobes[i][1] = 2 + random.nextInt(3);                       // frequency 2..4
            lobes[i][2] = minAmp + random.nextDouble() * (maxAmp - minAmp); // amplitude
        }
        return lobes;
    }

    /** Radius multiplier at the given angle (radians) - apply as baseRadius * this. */
    static double lobeMultiplier(double angle, double[][] lobes) {
        double m = 1.0;
        for (double[] lobe : lobes) {
            m += lobe[2] * Math.cos(lobe[1] * angle + lobe[0]);
        }
        return Math.max(0.35, m);
    }
}
