package com.ids.bot.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Per-persona Gaussian session intensity — determines how many steps are taken per session.
 */
public enum SessionIntensity {

    BROWSING(8.0, 2.5, 3, 15),
    SEARCHING(5.0, 1.8, 2, 10),
    FORM_FILLING(3.0, 1.2, 1, 6);

    private final double meanSteps;
    private final double stdDev;
    private final int    minSteps;
    private final int    maxSteps;

    SessionIntensity(double meanSteps, double stdDev, int minSteps, int maxSteps) {
        if (stdDev < 0)          throw new IllegalArgumentException("stdDev must be >= 0");
        if (minSteps < 1)        throw new IllegalArgumentException("minSteps must be >= 1");
        if (minSteps > maxSteps) throw new IllegalArgumentException("minSteps must be <= maxSteps");

        this.meanSteps = meanSteps;
        this.stdDev    = stdDev;
        this.minSteps  = minSteps;
        this.maxSteps  = maxSteps;
    }

    /** Samples from Gaussian and clamps to [minSteps, maxSteps]. */
    public int sample() {
        double raw = ThreadLocalRandom.current().nextGaussian() * stdDev + meanSteps;
        int count  = (int) Math.round(raw);
        return Math.max(minSteps, Math.min(maxSteps, count));
    }

    /** Manual validation: checks empirical mean for each enum over 100 000 samples. */
    public static void main(String[] args) {
        for (SessionIntensity si : values()) {
            long sum = 0;
            int  N   = 100_000;
            for (int i = 0; i < N; i++) sum += si.sample();
            System.out.printf("%-12s mean=%.3f (target=%.1f)%n",
                si.name(), (double) sum / N, si.meanSteps);
        }
    }
}
