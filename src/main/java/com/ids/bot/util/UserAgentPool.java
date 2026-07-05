package com.ids.bot.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the User-Agent selected per session and the associated Client Hints metadata.
 *
 * DESIGN DECISION: The pool contains ONLY Chromium-family browsers (Chrome and Edge).
 * Firefox/Safari/iOS must NEVER be added. Reason: the real client is always headless
 * Chrome; even if the UA string says "Firefox", Chrome's Client Hints headers
 * (sec-ch-ua etc.) always carry a Chromium signature — creating a detectable fingerprint
 * inconsistency. Diversity is preserved across OS/version/arch/mobile dimensions while
 * the engine (Chromium) stays fixed.
 * (This was a real bug found and fixed during this project.)
 */
public class UserAgentPool {

    /**
     * One UA entry: the UA string, platform metadata, and Client Hints fields.
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
         * Builds the Map for CDP Network.setUserAgentOverride's userAgentMetadata parameter.
         * brands: GREASE token + Chromium + real brand.
         */
        public Map<String, Object> buildMetadata() {
            Map<String, Object> meta = new LinkedHashMap<>();

            // GREASE token — fixed example; the browser normally randomises this
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

            // fullVersionList — high-entropy hint
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
    // Pool of 20 entries: Chrome×14, Edge×6
    // IMPORTANT: Windows 11 UA strings still say "Windows NT 10.0";
    // Win11 is distinguished via platformVersion="15.0.0".
    // "NT 11.0" must never be written — this was a real bug found in this project.
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
        // Windows 11 (UA still says NT 10.0, Win11 identified by platformVersion 15.0.0)
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Windows", "15.0.0", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        // Edge on Windows 10
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "Windows", "10.0.0", "x86", "", false, "Microsoft Edge", 126, "126.0.2592.87"),
        // Edge on Windows 11
        new UserAgentEntry(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            "Windows", "15.0.0", "x86", "", false, "Microsoft Edge", 124, "124.0.2478.105"),

        // --- macOS Intel (UA string shows Intel regardless of actual arch) ---
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "macOS", "14.5.0", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
            "macOS", "14.4.0", "x86", "", false, "Google Chrome", 123, "123.0.6312.122"),
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "macOS", "13.6.0", "x86", "", false, "Google Chrome", 121, "121.0.6167.184"),
        // macOS arm (Apple M-series)
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "macOS", "14.5.0", "arm", "", false, "Google Chrome", 126, "126.0.6478.127"),
        // Edge on macOS arm
        new UserAgentEntry(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "macOS", "14.5.0", "arm", "", false, "Microsoft Edge", 126, "126.0.2592.87"),

        // --- Linux (x86_64) ---
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 126, "126.0.6478.127"),
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 124, "124.0.6367.207"),
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Linux", "", "x86", "", false, "Google Chrome", 120, "120.0.6099.224"),
        // Edge on Linux
        new UserAgentEntry(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
            "Linux", "", "x86", "", false, "Microsoft Edge", 124, "124.0.2478.105"),

        // --- Android (mobile=true, no arch field) ---
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

    /** Returns a random UserAgentEntry from the pool. Call once per session. */
    public static UserAgentEntry randomEntry() {
        return POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
    }

    /** Returns all entries (backward compatibility / testing). */
    public static List<UserAgentEntry> allEntries() {
        return POOL;
    }

    /**
     * Backward compatibility: returns the UA string directly for legacy callers.
     * @deprecated Use randomEntry().userAgent() instead.
     */
    @Deprecated
    public static String randomUserAgent() {
        return randomEntry().userAgent();
    }
}
