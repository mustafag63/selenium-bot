package com.ids.bot.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable record carrying the timing parameters for one persona.
 * All log-mean/log-std constants are in log1p(microseconds) space.
 *
 * @deprecated sampleTotalActions() was replaced by SessionIntensity (3 July 2026).
 * The record itself is active; only that one method is deprecated.
 */
public record TimingProfile(
    double activeLogMean,
    double activeLogStd,
    double activeClampMinMs,
    double activeClampMaxMs,

    double idleLogMean,
    double idleLogStd,
    double idleClampMinMs,
    double idleClampMaxMs,

    double pActiveIdleCycle,

    double fwdNearZeroProb,
    double fwdLogMean,
    double fwdLogStd
) {

    /** Returns a sampled active duration in ms from log1p(µs) parameters. */
    public double sampleActiveDurationMs() {
        double logVal = ThreadLocalRandom.current().nextGaussian() * activeLogStd + activeLogMean;
        double us = Math.expm1(logVal);          // log1p ters: expm1
        double ms = us / 1000.0;
        return clamp(ms, activeClampMinMs, activeClampMaxMs);
    }

    /** Returns a sampled idle duration in ms from log1p(µs) parameters. */
    public double sampleIdleDurationMs() {
        double logVal = ThreadLocalRandom.current().nextGaussian() * idleLogStd + idleLogMean;
        double us = Math.expm1(logVal);
        double ms = us / 1000.0;
        return clamp(ms, idleClampMinMs, idleClampMaxMs);
    }

    /**
     * Zero-inflated forward wait duration (ms).
     * With probability fwdNearZeroProb draws from [1,50]ms uniform; otherwise conditional log-normal.
     */
    public double sampleFwdWaitMs() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < fwdNearZeroProb) {
            return rng.nextDouble(1.0, 50.0);
        }
        // Rejection sampling — discard negative or extreme outliers
        for (int attempt = 0; attempt < 100; attempt++) {
            double logVal = rng.nextGaussian() * fwdLogStd + fwdLogMean;
            double us = Math.expm1(logVal);
            double ms = us / 1000.0;
            if (ms > 0 && ms < 120_000.0) {
                return ms;
            }
        }
        return 500.0; // rejection fallback
    }

    /** Returns true if this wait should be an active+idle cycle rather than a plain fwdWait. */
    public boolean shouldHaveActiveIdleCycle() {
        return ThreadLocalRandom.current().nextDouble() < pActiveIdleCycle;
    }

    /**
     * @deprecated Replaced by SessionIntensity on 3 July 2026 — retained only
     * for the TimingProfiles.aggregate() reference profile.
     */
    @Deprecated
    public int sampleTotalActions() {
        double logMean = 2.3;
        double logStd  = 0.6;
        int    min     = 3;
        int    max     = 20;
        double raw = ThreadLocalRandom.current().nextGaussian() * logStd + logMean;
        int count = (int) Math.round(Math.exp(raw));
        return (int) clamp(count, min, max);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
