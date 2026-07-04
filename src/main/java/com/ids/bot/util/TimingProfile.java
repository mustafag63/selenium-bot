package com.ids.bot.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Bir personanın zamanlama parametrelerini taşıyan immutable record.
 * Tüm log-mean/log-std sabitleri log1p(microseconds) uzayındadır.
 *
 * @deprecated sampleTotalActions() metodu SessionIntensity ile değiştirildi (3 Temmuz 2026).
 * Record'un kendisi aktif; sadece eski metod deprecated.
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

    /** log1p(µs) parametrelerinden ms cinsinden örneklenmiş aktif süre döndürür. */
    public double sampleActiveDurationMs() {
        double logVal = ThreadLocalRandom.current().nextGaussian() * activeLogStd + activeLogMean;
        double us = Math.expm1(logVal);          // log1p ters: expm1
        double ms = us / 1000.0;
        return clamp(ms, activeClampMinMs, activeClampMaxMs);
    }

    /** log1p(µs) parametrelerinden ms cinsinden örneklenmiş idle süre döndürür. */
    public double sampleIdleDurationMs() {
        double logVal = ThreadLocalRandom.current().nextGaussian() * idleLogStd + idleLogMean;
        double us = Math.expm1(logVal);
        double ms = us / 1000.0;
        return clamp(ms, idleClampMinMs, idleClampMaxMs);
    }

    /**
     * Zero-inflated forward wait süresi (ms).
     * fwdNearZeroProb olasılığıyla [1,50]ms uniform; yoksa koşullu log-normal.
     */
    public double sampleFwdWaitMs() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        if (rng.nextDouble() < fwdNearZeroProb) {
            return rng.nextDouble(1.0, 50.0);
        }
        // Rejection sampling — negatif veya aşırı uç değerleri reddet
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

    /** Bu oturumda aktif+idle döngüsü mü yaşanacak, yoksa düz fwdWait mi? */
    public boolean shouldHaveActiveIdleCycle() {
        return ThreadLocalRandom.current().nextDouble() < pActiveIdleCycle;
    }

    /**
     * @deprecated SessionIntensity ile değiştirildi, 3 Temmuz 2026 — sadece
     * TimingProfiles.aggregate() referans profili için tutuluyor.
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
