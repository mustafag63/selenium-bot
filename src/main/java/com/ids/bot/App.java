package com.ids.bot;

import com.ids.bot.scenarios.*;
import com.ids.bot.util.UserAgentPool;
import com.ids.bot.util.UserAgentPool.UserAgentEntry;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

public class App {

    public static void main(String[] args) throws Exception {
        String scenario = (args.length > 0) ? args[0] : "browsing";
        System.out.println("Scenario: " + scenario);

        UserAgentEntry ua = UserAgentPool.randomEntry();
        System.out.println("Session user-agent: " + ua.userAgent());

        WebDriver driver = createDriver(ua);
        try {
            dispatch(driver, scenario);
        } finally {
            driver.quit();
        }
        System.out.println("Bot completed.");
    }

    /**
     * PUBLIC static fabrika — App, ParallelRunner ve testler tarafından paylaşılır.
     *
     * --disable-features=AsyncDns: Bu olmadan Chrome kendi resolver'ını kullanıp
     * techmarket.lab'ı çözemez ve split-DNS'i atlar.
     */
    public static WebDriver createDriver(UserAgentEntry uaEntry) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
            "--headless",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-features=AsyncDns",
            "--disk-cache-size=0",
            "--incognito",
            "--user-agent=" + uaEntry.userAgent()
        );

        ChromeDriver driver = new ChromeDriver(opts);
        applyUserAgentOverride(driver, uaEntry);
        disableBrowserCache(driver);
        return driver;
    }

    /**
     * Untyped/ham CDP komutu ile UA ve Client Hints override.
     *
     * TASARIM KARARI: Selenium'un typed CDP binding'leri (org.openqa.selenium.devtools.v125...)
     * sadece Chrome v125'e kadar destekler. Kurulu Chrome daha yeniyse "Unable to find CDP
     * implementation matching" uyarısı çıkar ve typed API kırılgan olur.
     * Bunun yerine untyped executeCdpCommand kullanılır — hem UA string'ini hem Client Hints
     * header'larını (sec-ch-ua vb.) tutarlı hale getirir.
     */
    private static void applyUserAgentOverride(ChromeDriver driver, UserAgentEntry uaEntry) {
        driver.executeCdpCommand("Network.setUserAgentOverride", Map.of(
            "userAgent",         uaEntry.userAgent(),
            "platform",          uaEntry.platform(),
            "userAgentMetadata", uaEntry.buildMetadata()
        ));
    }

    /**
     * CDP üzerinden tarayıcı önbelleğini devre dışı bırakır.
     *
     * SIRA ÖNEMLİ: Network.enable çağrılmadan setCacheDisabled sessizce no-op kalır.
     * (Bu proje tarihinde cache kapatmanın sessizce başarısız olabildiği ve tekrar-ziyaret
     * edilen sayfaların Zeek/Nginx loglarında hiç görünmemesine yol açtığı bulunmuştu.)
     */
    private static void disableBrowserCache(ChromeDriver driver) {
        Map<String, Object> enableResult = driver.executeCdpCommand("Network.enable", Map.of());
        System.out.println("CDP Network.enable result: " + enableResult);

        Map<String, Object> cacheResult = driver.executeCdpCommand(
            "Network.setCacheDisabled", Map.of("cacheDisabled", true));
        System.out.println("CDP setCacheDisabled result: " + cacheResult);

        if (cacheResult != null && cacheResult.containsKey("error")) {
            throw new RuntimeException("CDP setCacheDisabled failed: " + cacheResult.get("error"));
        }
        System.out.println("CDP cache disable VERIFIED — cache is off for this session.");
    }

    private static void dispatch(WebDriver driver, String scenarioName) throws Exception {
        switch (scenarioName.toLowerCase()) {
            case "browsing"     -> new BrowsingScenario(driver).run();
            case "searching"    -> new SearchingScenario(driver).run();
            case "formfilling"  -> new FormFillingScenario(driver).run();
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenarioName
                + " (use: browsing | searching | formfilling)");
        }
    }

    static String tag(String threadLabel) {
        return "[" + threadLabel + "]";
    }
}
