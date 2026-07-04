package com.ids.bot.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Oturum başına seçilen User-Agent ve ilgili Client Hints meta verilerini yönetir.
 *
 * TASARIM KARARI: Havuz SADECE Chromium-ailesi tarayıcılardan oluşur (Chrome ve Edge).
 * Firefox/Safari/iOS KESİNLİKLE EKLENMEMELİ. Sebebi: gerçek istemci her zaman headless
 * Chrome'dur; UA string'i "Firefox" dese bile Chrome'un gönderdiği Client Hints header'ları
 * (sec-ch-ua vb.) her zaman Chromium imzası taşır — bu bir fingerprint tutarsızlığı yaratır.
 * Çeşitlilik OS/sürüm/mimari/mobil boyutunda korunuyor, motor (Chromium) sabit.
 * (Bu proje tarihinde gerçekten bulunmuş ve düzeltilmiş bir sorun.)
 */
public class UserAgentPool {

    /**
     * Tek bir UA girdisi: string, platform meta verisi ve Client Hints alanları.
     */
    public record UserAgentEntry(
        String userAgent,
        String platform,
        String platformVersion,
        String arch,
        String model,
        boolean mobile,
        String brandName,
        int majorVersion,
        String fullVersion
    ) {
        /**
         * CDP Network.setUserAgentOverride'ın userAgentMetadata parametresi için Map üretir.
         * brands: GREASE + Chromium + gerçek marka.
         */
        public Map<String, Object> buildMetadata() {
            Map<String, Object> meta = new LinkedHashMap<>();

            // GREASE token — sabit örnek, tarayıcı normalde rastgele seçer
            Map<String, Object> grease = new LinkedHashMap<>();
            grease.put("brand", "Not/A)Brand");
            grease.put("version", "8");

            Map<String, Object> chromium = new LinkedHashMap<>();
            chromium.put("brand", "Chromium");
            chromium.put("version", String.valueOf(majorVersion));

            Map<String, Object> real = new LinkedHashMap<>();
            real.put("brand", brandName);
            real.put("version", String.valueOf(majorVersion));

            meta.put("brands", List.of(grease, chromium, real));

            // fullVersionList — yüksek-entropi
            Map<String, Object> greaseF = new LinkedHashMap<>();
            greaseF.put("brand", "Not/A)Brand");
            greaseF.put("version", "8.0.0.0");

            Map<String, Object> chromiumF = new LinkedHashMap<>();
            chromiumF.put("brand", "Chromium");
            chromiumF.put("version", fullVersion);

            Map<String, Object> realF = new LinkedHashMap<>();
            realF.put("brand", brandName);
            realF.put("version", fullVersion);

            meta.put("fullVersionList", List.of(greaseF, chromiumF, realF));

            meta.put("platform",        platform);
            meta.put("platformVersion", platformVersion);
            meta.put("architecture",    arch);
            meta.put("model",           model);
            meta.put("mobile",          mobile);

            return meta;
        }
    }

    // -------------------------------------------------------------------------
    // 20 girişlik havuz: Chrome×14, Edge×6
    // ÖNEMLİ: Windows 11 UA string'i HÂLÂ "Windows NT 10.0" yazar;
    // Win11 ayrımı platformVersion="15.0.0" ile yapılır.
    // "NT 11.0" YAZILMAZ — bu proje tarihinde bulunan gerçek bir hataydı.
    // -------------------------------------------------------------------------
    private static final List<UserAgentEntry> POOL = List.of(

        // --- Windows (NT 10.0 = Win10, platformVersion 15.0.0 = Win11) ---
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Windows", "10.0.0", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Windows", "10.0.0", "x86", "", false, "Google Chrome", 124, "124.0.6367.207"),
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Windows", "10.0.0", "x86", "", false, "Google Chrome", 122, "122.0.6261.128"),
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Windows", "10.0.0", "x86", "", false, "Google Chrome", 119, "119.0.6045.199"),
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
            "Windows", "10.0.0", "x86", "", false, "Google Chrome", 125, "125.0.6422.142"),
        // Windows 11 (NT 10.0 string, platformVersion 15.0.0)
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Windows", "15.0.0", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        // Edge Windows 10
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "Windows", "10.0.0", "x86", "", false, "Microsoft Edge", 126, "126.0.2592.87"),
        // Edge Windows 11
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            "Windows", "15.0.0", "x86", "", false, "Microsoft Edge", 124, "124.0.2478.105"),

        // --- macOS Intel ---
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "macOS", "14.5.0", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "macOS", "14.4.0", "x86", "", false, "Google Chrome", 123, "123.0.6312.122"),
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "macOS", "13.6.0", "x86", "", false, "Google Chrome", 121, "121.0.6167.184"),
        // macOS arm (M-serisi)
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "macOS", "14.5.0", "arm", "", false, "Google Chrome", 126, "126.0.6478.127"),
        // Edge macOS arm
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "macOS", "14.5.0", "arm", "", false, "Microsoft Edge", 126, "126.0.2592.87"),

        // --- Linux ---
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 124, "124.0.6367.207"),
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 120, "120.0.6099.224"),
        // Edge Linux
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            "Linux", "", "x86", "", false, "Microsoft Edge", 124, "124.0.2478.105"),

        // --- Android (mobile=true) ---
        new UserAgentEntry(
            "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.6478.122 Mobile Safari/537.36",
            "Android", "14.0.0", "", "SM-S928B", true, "Google Chrome", 126, "126.0.6478.122"),
        new UserAgentEntry(
            "Mozilla/5.0 (Linux; Android 13; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36",
            "Android", "13.0.0", "", "Pixel 8", true, "Google Chrome", 124, "124.0.6367.82"),
        new UserAgentEntry(
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36 EdgA/124.0.0.0",
            "Android", "14.0.0", "", "Pixel 8", true, "Microsoft Edge", 124, "124.0.2478.105")
    );

    /** Havuzdan rastgele bir UserAgentEntry döndürür. Her oturum için bir kez çağır. */
    public static UserAgentEntry randomEntry() {
        return POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
    }

    /** Tüm girişleri döndürür (geriye dönük uyumluluk / test). */
    public static List<UserAgentEntry> allEntries() {
        return POOL;
    }

    /**
     * Geriye dönük uyumluluk: eski çağıran kodlar için UA string'ini doğrudan döndürür.
     * @deprecated randomEntry().userAgent() kullanın.
     */
    @Deprecated
    public static String randomUserAgent() {
        return randomEntry().userAgent();
    }
}
