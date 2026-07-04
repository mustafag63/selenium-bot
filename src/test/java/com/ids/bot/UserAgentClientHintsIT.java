package com.ids.bot;

import com.ids.bot.util.UserAgentPool;
import com.ids.bot.util.UserAgentPool.UserAgentEntry;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Her UserAgentEntry için gerçek Client Hints header'larının UA metadata ile tutarlı
 * olduğunu doğrular. about:blank'te Client Hints güvenli-bağlam gerektirdiği için
 * embedded yerel HTTP sunucusu kullanılır.
 */
@Tag("integration")
class UserAgentClientHintsIT {

    private static HttpServer server;
    private static int        serverPort;

    /** Sunucuya gelen son request'in header'larını depolar. */
    private static final CopyOnWriteArrayList<Map<String, String>> capturedHeaders =
        new CopyOnWriteArrayList<>();

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverPort = server.getAddress().getPort();

        server.createContext("/", exchange -> {
            // Gelen header'ları yakala
            Map<String, String> headers = new LinkedHashMap<>();
            exchange.getRequestHeaders().forEach((k, v) ->
                headers.put(k.toLowerCase(Locale.ROOT), String.join(", ", v)));
            capturedHeaders.add(headers);

            // Accept-CH ile high-entropy hint'leri talep et
            String body = "<html><head></head><body>ok</body></html>";
            exchange.getResponseHeaders().add("Accept-CH",
                "Sec-CH-UA, Sec-CH-UA-Platform, Sec-CH-UA-Mobile, " +
                "Sec-CH-UA-Platform-Version, Sec-CH-UA-Arch, Sec-CH-UA-Model, " +
                "Sec-CH-UA-Full-Version-List");
            exchange.sendResponseHeaders(200, body.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void allEntriesHaveConsistentClientHints() {
        List<UserAgentEntry> entries = UserAgentPool.allEntries();
        int passing = 0;

        for (UserAgentEntry entry : entries) {
            capturedHeaders.clear();
            WebDriver driver = App.createDriver(entry);
            try {
                // İlk istek — Accept-CH set edilir
                driver.get("http://127.0.0.1:" + serverPort + "/");
                // İkinci istek — tarayıcı hint'leri gönderir
                driver.get("http://127.0.0.1:" + serverPort + "/");

                if (capturedHeaders.isEmpty()) {
                    System.out.println("WARN: No headers captured for " + entry.userAgent());
                    continue;
                }

                Map<String, String> h = capturedHeaders.get(capturedHeaders.size() - 1);
                String platform = h.getOrDefault("sec-ch-ua-platform", "").toLowerCase(Locale.ROOT);
                String mobile   = h.getOrDefault("sec-ch-ua-mobile", "");

                // platform tutarlılığı
                String expectedPlatform = entry.platform().toLowerCase(Locale.ROOT);
                if (!platform.isBlank()) {
                    assertTrue(platform.contains(expectedPlatform),
                        "Platform mismatch for " + entry.userAgent()
                        + ": header=" + platform + ", expected=" + expectedPlatform);
                }

                // mobile tutarlılığı
                if (!mobile.isBlank()) {
                    String expectedMobile = entry.mobile() ? "?1" : "?0";
                    assertEquals(expectedMobile, mobile,
                        "Mobile mismatch for " + entry.userAgent());
                }

                passing++;
                System.out.println("OK  [" + entry.platform() + "/" + entry.brandName()
                    + " v" + entry.majorVersion() + "] mobile=" + entry.mobile());
            } finally {
                driver.quit();
            }
        }

        System.out.println(passing + "/" + entries.size() + " entries passed Client Hints check");
        assertTrue(passing == entries.size(),
            "Not all entries passed: " + passing + "/" + entries.size());
    }
}
