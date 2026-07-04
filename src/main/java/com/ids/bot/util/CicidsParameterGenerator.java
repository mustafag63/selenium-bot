package com.ids.bot.util;

/**
 * CICIDS parametrelerine erişimin eski yolu. Yeni kod TimingProfile/TimingProfiles kullanmalı.
 * Bu sınıf sadece geriye dönük uyumluluk için duruyor, TimingProfiles.aggregate()'e delege ediyor.
 *
 * @deprecated TimingProfiles ve TimingProfile kullanın.
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
