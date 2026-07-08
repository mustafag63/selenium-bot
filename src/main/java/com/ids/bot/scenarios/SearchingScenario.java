package com.ids.bot.scenarios;

import com.ids.bot.pages.*;
import com.ids.bot.util.*;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.ThreadLocalRandom;

public class SearchingScenario {

    private final WebDriver             driver;
    private final MarkovTransitionModel model;
    private final TimingProfile         timingProfile;

    private final HomePage          homePage;
    private final ProductsPage      productsPage;
    private final ProductDetailPage detailPage;

    public SearchingScenario(WebDriver driver) {
        this.driver        = driver;
        this.model         = BotPersonas.searchingModel();
        this.timingProfile = TimingProfiles.searching();
        this.homePage      = new HomePage(driver);
        this.productsPage  = new ProductsPage(driver);
        this.detailPage    = new ProductDetailPage(driver);
    }

    /** Test-only constructor: inject a custom TimingProfile (e.g. a fast/fixed profile for regression tests). */
    SearchingScenario(WebDriver driver, TimingProfile timingProfile) {
        this.driver        = driver;
        this.model         = BotPersonas.searchingModel();
        this.timingProfile = timingProfile;
        this.homePage      = new HomePage(driver);
        this.productsPage  = new ProductsPage(driver);
        this.detailPage    = new ProductDetailPage(driver);
    }

    public void run() throws InterruptedException {
        homePage.open();
        NavLog.logNav("HOME");
        waitBetweenActions(timingProfile);
        BotState current = BotState.HOME;
        int actionCount  = SessionIntensity.SEARCHING.sample();

        for (int n = 1; n <= actionCount; n++) {
            BotState next = model.nextState(current);

            switch (next) {
                case HOME -> { homePage.open(); NavLog.logNav("HOME"); }
                case PRODUCTS -> { productsPage.open(); NavLog.logNav("PRODUCTS"); }
                case PRODUCT_DETAIL -> {
                    if (current != BotState.PRODUCTS) {
                        productsPage.open();
                        NavLog.logNav("PRODUCTS");
                        waitBetweenActions(timingProfile);
                    }
                    // GUARD: skip if no products are listed
                    if (productsPage.getProductCount() == 0) {
                        System.out.println("[Searching] " + n + ": PRODUCT_DETAIL skipped (no products)");
                        current = BotState.PRODUCTS;
                        waitBetweenActions(timingProfile);
                        continue;
                    }
                    productsPage.clickRandomProduct();
                    // 40% chance to add to cart — decided (and logged) before the alert-wait,
                    // so the NavLog timestamp still marks the true navigation instant, not the
                    // post-alert instant. The CART/NOCART tag lets the *following* gap (which
                    // now legitimately includes the alert-wait for CART) be analyzed separately.
                    boolean willAddToCart = ThreadLocalRandom.current().nextDouble() < 0.40;
                    NavLog.logNav(willAddToCart ? "PRODUCT_DETAIL_CART" : "PRODUCT_DETAIL_NOCART");
                    String productName = detailPage.getProductName();
                    if (willAddToCart) {
                        detailPage.addToCart();
                        System.out.println("[Searching] " + n + ": added to cart - " + productName);
                    } else {
                        System.out.println("[Searching] " + n + ": PRODUCT_DETAIL - " + productName);
                    }
                }
                case ABOUT -> { driver.get("http://techmarket.lab/about.html"); NavLog.logNav("ABOUT"); }
                case CONTACT -> {
                    // Open the page only — no form filling
                    driver.get("http://techmarket.lab/contact.html");
                    NavLog.logNav("CONTACT");
                    System.out.println("[Searching] " + n + ": CONTACT (form not filled)");
                }
            }

            if (next != BotState.PRODUCT_DETAIL && next != BotState.CONTACT) {
                System.out.println("[Searching] " + n + ": " + next);
            }

            current = next;
            waitBetweenActions(timingProfile);
        }

        System.out.println("[Searching] session ending — user left");
        System.out.println("[Searching] Scenario completed (" + actionCount + " actions)");
    }

    static void waitBetweenActions(TimingProfile profile) throws InterruptedException {
        BrowsingScenario.waitBetweenActions(profile);
    }
}
