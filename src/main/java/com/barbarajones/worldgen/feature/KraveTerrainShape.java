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

    private KraveTerrainShape() { }

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
