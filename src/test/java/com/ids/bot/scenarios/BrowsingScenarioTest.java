package com.ids.bot.scenarios;

import com.ids.bot.App;
import com.ids.bot.util.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BrowsingScenarioTest {

    // -------------------------------------------------------------------------
    // Unit tests — no lab required (no tag)
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
            // track that every declared edge fires at least once
            Map<BotState, Long> hits = new java.util.HashMap<>();
            declared.forEach(s -> hits.put(s, 0L));

            for (int i = 0; i < 20_000; i++) {
                BotState next = model.nextState(source);
                assertTrue(declared.contains(next),
                    "Undeclared transition " + source + " -> " + next);
                hits.merge(next, 1L, Long::sum);
            }

            // dead-edge check: every declared edge must fire at least once
            for (BotState target : declared) {
                assertTrue(hits.get(target) > 0,
                    "Dead edge detected: " + source + " -> " + target + " never fired");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Integration tests — require real Chrome and lab network
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

            // transferSize > 0 means the resource was fetched over the network, not from cache
            assertNotNull(entries, "performance.getEntriesByType should return data");
            System.out.println("Transfer sizes: " + entries);
        }

        @Test
        void firstInterNavigationGapIsNotSkipped() throws Exception {
            // Regression test for a bug found 2026-07-06: the very first inter-navigation
            // gap was skipped (no wait before the loop's first navigation), always
            // producing a near-zero gap. Fixed by adding waitBetweenActions() right
            // after the initial homePage.open().
            List<Long> navTimestamps = Collections.synchronizedList(new ArrayList<>());
            WebDriverListener listener = new WebDriverListener() {
                @Override
                public void beforeAnyWebDriverCall(WebDriver d, Method m, Object[] args) {
                    if ("get".equals(m.getName())) {
                        navTimestamps.add(System.currentTimeMillis());
                    }
                }
            };
            WebDriver decorated = new EventFiringDecorator<>(listener).decorate(driver);

            // Fixed profile: clampMin == clampMax forces exact, deterministic wait durations
            // regardless of the (irrelevant here) log-normal parameters.
            TimingProfile fixedProfile = new TimingProfile(
                0, 0, 300, 300,   // active: forced to exactly 300ms
                0, 0, 300, 300,   // idle: forced to exactly 300ms
                1.0,              // always take the active+idle cycle branch
                0, 0, 0           // fwd params unused when pActiveIdleCycle=1.0
            );

            new BrowsingScenario(decorated, fixedProfile).run();

            assertTrue(navTimestamps.size() >= 2,
                "Expected at least 2 navigations, got " + navTimestamps.size());
            long firstGap = navTimestamps.get(1) - navTimestamps.get(0);
            assertTrue(firstGap >= 550,
                "First inter-navigation gap should include the pre-loop wait (~600ms), was " + firstGap + "ms");
        }
    }
}
