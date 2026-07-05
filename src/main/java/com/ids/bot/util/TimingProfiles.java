package com.ids.bot.util;

/**
 * Factory that produces per-persona TimingProfiles.
 *
 * Parameters are derived from statistical analysis of CICIDS2017 BENIGN (Monday-WorkingHours)
 * traffic: KMeans clustering (k=2, silhouette=0.556) split HTTP/HTTPS flows into two clusters;
 * Active/Idle cycle durations and Forward IAT (zero-inflated: near-zero band + conditional
 * log-normal) were fit separately for each cluster.
 *
 * The source analysis script (extract_params.py) is NO LONGER USED.
 * WARNING: it operated in log1p(ms) space; constants in this class are in log1p(microseconds).
 * Mixing the two causes a 1000× error — this has happened before in this project.
 */
public class TimingProfiles {

    private TimingProfiles() {}

    /** Timing profile for the browsing-focused persona. */
    public static TimingProfile browsing() {
        return new TimingProfile(
            /* activeLogMean */    12.0997,
            /* activeLogStd  */    1.3301,
            /* activeClampMin */   10.0,
            /* activeClampMax */   7010.0,

            /* idleLogMean */      16.4975,
            /* idleLogStd  */      0.7036,
            /* idleClampMin */     6000.0,
            /* idleClampMax */     60000.0,

            /* pActiveIdleCycle */ 0.915,

            /* fwdNearZeroProb */  0.001,
            /* fwdLogMean */       14.8389,
            /* fwdLogStd  */       1.4648
        );
    }

    /** Timing profile for the product-searching persona. */
    public static TimingProfile searching() {
        return new TimingProfile(
            /* activeLogMean */    11.7473,
            /* activeLogStd  */    2.1462,
            /* activeClampMin */   10.0,
            /* activeClampMax */   3000.0,

            /* idleLogMean */      15.9133,
            /* idleLogStd  */      0.6580,
            /* idleClampMin */     5000.0,
            /* idleClampMax */     65000.0,

            /* pActiveIdleCycle */ 0.164,

            /* fwdNearZeroProb */  0.001,
            /* fwdLogMean */       12.0586,
            /* fwdLogStd  */       2.3283
        );
    }

    /**
     * Timing profile for the form-filling persona.
     * Uses the same parameters as searching, but with a lower pActiveIdleCycle (0.05).
     */
    public static TimingProfile formFilling() {
        return new TimingProfile(
            11.7473, 2.1462, 10.0, 3000.0,
            15.9133, 0.6580, 5000.0, 65000.0,
            /* pActiveIdleCycle */ 0.05,
            0.001, 12.0586, 2.3283
        );
    }

    /**
     * Aggregate reference profile — average across all personas.
     * For diagnostic and backward-compatibility purposes only; not used in active code paths.
     */
    public static TimingProfile aggregate() {
        return new TimingProfile(
            11.95, 1.74, 10.0, 5000.0,
            16.20, 0.69, 5500.0, 62000.0,
            0.378,
            0.001, 13.45, 1.90
        );
    }
}
