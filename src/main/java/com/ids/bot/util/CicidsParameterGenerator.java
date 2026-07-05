package com.ids.bot.util;

/**
 * Legacy access point for CICIDS parameters. New code should use TimingProfile/TimingProfiles.
 * This class is kept for backward compatibility only; it delegates to TimingProfiles.aggregate().
 *
 * @deprecated Use TimingProfiles and TimingProfile instead.
 */
@Deprecated
public class CicidsParameterGenerator {

    private CicidsParameterGenerator() {}

    @Deprecated
    public static double getActiveLogMean() {
        return TimingProfiles.aggregate().activeLogMean();
    }

    @Deprecated
    public static double getActiveLogStd() {
        return TimingProfiles.aggregate().activeLogStd();
    }

    @Deprecated
    public static double getIdleLogMean() {
        return TimingProfiles.aggregate().idleLogMean();
    }

    @Deprecated
    public static double getIdleLogStd() {
        return TimingProfiles.aggregate().idleLogStd();
    }

    @Deprecated
    public static double getFwdLogMean() {
        return TimingProfiles.aggregate().fwdLogMean();
    }

    @Deprecated
    public static double getFwdLogStd() {
        return TimingProfiles.aggregate().fwdLogStd();
    }
}
