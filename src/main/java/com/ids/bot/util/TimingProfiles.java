package com.ids.bot.util;

/**
 * Persona başına TimingProfile fabrikası.
 *
 * Bu parametreler CICIDS2017 BENIGN (Monday-WorkingHours) trafiğinin istatistiksel analizinden
 * türetildi: KMeans kümeleme (k=2, silhouette=0.556) ile HTTP/HTTPS flow'ları iki kümeye ayrıldı,
 * her küme için Active/Idle döngü süreleri ve Forward IAT (zero-inflated: near-zero bandı +
 * koşullu log-normal) ayrı ayrı fit edildi.
 *
 * Kaynak analiz scripti (extract_params.py) artık KULLANILMIYOR.
 * UYARI: log1p(ms) uzayında çalışıyordu, bu sınıftaki sabitler log1p(microseconds) uzayında;
 * karıştırılırsa 1000× hata oluşur (bu proje tarihinde gerçekten yaşanmış bir hataydı).
 */
public class TimingProfiles {

    private TimingProfiles() {}

    /** Gezinme-odaklı persona zamanlama profili. */
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

    /** Ürün-odaklı persona zamanlama profili. */
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
     * Form doldurma persona zamanlama profili.
     * = Searching parametreleri, sadece pActiveIdleCycle farklı (0.05).
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
     * Genel/ortalama referans profili — tüm personaların ortalaması.
     * Sadece tanısal/geriye dönük uyumluluk amaçlı, aktif kod yolunda kullanılmıyor.
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
