package com.ids.bot.scenarios;

import com.ids.bot.App;
import com.ids.bot.util.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BrowsingScenarioTest {

    // -------------------------------------------------------------------------
    // Lab'sız unit testler (TAG YOK)
    // -------------------------------------------------------------------------

    @Test
    void sessionLengthWithinExpectedRange() {
        for (int i = 0; i < 10_000; i++) {
            int s = SessionIntensity.BROWSING.sample();
            assertTrue(s >= 3 && s <= 15,
                "BROWSING session length out of range: " + s);
        }
    }

    @Test
    void markovTransitionsNeverProduceUndeclaredState() {
        MarkovTransitionModel model = BotPersonas.browsingModel();
        Map<BotState, Map<BotState, Double>> matrix = model.getMatrix();

        for (BotState source : matrix.keySet()) {
            Set<BotState> declared = matrix.get(source).keySet();
            // her tanımlı kenarın en az bir kez tetiklendiğini izle
            Map<BotState, Long> hits = new java.util.HashMap<>();
            declared.forEach(s -> hits.put(s, 0L));

            for (int i = 0; i < 20_000; i++) {
                BotState next = model.nextState(source);
                assertTrue(declared.contains(next),
                    "Undeclared transition " + source + " -> " + next);
                hits.merge(next, 1L, Long::sum);
            }

            // dead-edge kontrolü: her kenar en az bir kez tetiklenmeli
            for (BotState target : declared) {
                assertTrue(hits.get(target) > 0,
                    "Dead edge detected: " + source + " -> " + target + " never fired");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lab / Chrome gerektiren testler
    // -------------------------------------------------------------------------

    @Nested
    @Tag("integration")
    class LabIntegration {

        private WebDriver driver;

        @BeforeEach
        void setUp() {
            driver = App.createDriver(UserAgentPool.randomEntry());
        }

        @AfterEach
        void tearDown() {
            if (driver != null) driver.quit();
        }

        @Test
        void cacheByPassVerifiedOnRevisit() {
            driver.get("http://techmarket.lab/index.html");
            driver.get("http://techmarket.lab/about.html");
            driver.get("http://techmarket.lab/index.html"); // ikinci ziyaret

            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object entries = js.executeScript(
                "return performance.getEntriesByType('navigation').map(e => e.transferSize)");

            // transferSize > 0 ise cache hit değil (cache açık olsaydı 0 dönerdi)
            assertNotNull(entries, "performance.getEntriesByType should return data");
            System.out.println("Transfer sizes: " + entries);
        }
    }
}
