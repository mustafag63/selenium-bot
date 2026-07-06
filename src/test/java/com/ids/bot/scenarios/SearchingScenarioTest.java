package com.ids.bot.scenarios;

import com.ids.bot.App;
import com.ids.bot.pages.*;
import com.ids.bot.util.*;
import org.junit.jupiter.api.*;
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

class SearchingScenarioTest {

    // -------------------------------------------------------------------------
    // Unit tests — no lab required
    // -------------------------------------------------------------------------

    @Test
    void sessionLengthWithinExpectedRange() {
        for (int i = 0; i < 10_000; i++) {
            int s = SessionIntensity.SEARCHING.sample();
            assertTrue(s >= 2 && s <= 10,
                "SEARCHING session length out of range: " + s);
        }
    }

    @Test
    void markovTransitionsNeverProduceUndeclaredState() {
        MarkovTransitionModel model = BotPersonas.searchingModel();
        Map<BotState, Map<BotState, Double>> matrix = model.getMatrix();

        for (BotState source : matrix.keySet()) {
            Set<BotState> declared = matrix.get(source).keySet();
            Map<BotState, Long> hits = new java.util.HashMap<>();
            declared.forEach(s -> hits.put(s, 0L));

            for (int i = 0; i < 20_000; i++) {
                BotState next = model.nextState(source);
                assertTrue(declared.contains(next),
                    "Undeclared transition " + source + " -> " + next);
                hits.merge(next, 1L, Long::sum);
            }

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
        void addToCartTriggersAlert() {
            ProductsPage products = new ProductsPage(driver);
            products.open();
            org.junit.jupiter.api.Assumptions.assumeTrue(
                products.getProductCount() > 0, "Skipping: no products found");

            products.clickRandomProduct();
            ProductDetailPage detail = new ProductDetailPage(driver);

            // addToCart alert açıp accept etmeli, NoAlertPresentException fırlatmamalı
            assertDoesNotThrow(detail::addToCart,
                "addToCart should accept alert without throwing");
        }

        @Test
        void contactVisitLeavesFormEmpty() {
            driver.get("http://techmarket.lab/contact.html");

            // Form fields should be empty after a fresh GET
            String nameVal = driver.findElement(
                org.openqa.selenium.By.cssSelector("#name, input[name='name']")).getAttribute("value");
            assertTrue(nameVal == null || nameVal.isBlank(),
                "Name field should be empty after GET");
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

            TimingProfile fixedProfile = new TimingProfile(
                0, 0, 300, 300,
                0, 0, 300, 300,
                1.0,
                0, 0, 0
            );

            new SearchingScenario(decorated, fixedProfile).run();

            assertTrue(navTimestamps.size() >= 2,
                "Expected at least 2 navigations, got " + navTimestamps.size());
            long firstGap = navTimestamps.get(1) - navTimestamps.get(0);
            assertTrue(firstGap >= 550,
                "First inter-navigation gap should include the pre-loop wait (~600ms), was " + firstGap + "ms");
        }
    }
}
