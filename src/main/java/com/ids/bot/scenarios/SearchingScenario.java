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
        waitBetweenActions(timingProfile);
        BotState current = BotState.HOME;
        int actionCount  = SessionIntensity.SEARCHING.sample();

        for (int n = 1; n <= actionCount; n++) {
            BotState next = model.nextState(current);

            switch (next) {
                case HOME -> homePage.open();
                case PRODUCTS -> productsPage.open();
                case PRODUCT_DETAIL -> {
                    if (current != BotState.PRODUCTS) {
                        productsPage.open();
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
                    String productName = detailPage.getProductName();
                    // 40% chance to add to cart
                    if (ThreadLocalRandom.current().nextDouble() < 0.40) {
                        detailPage.addToCart();
                        System.out.println("[Searching] " + n + ": added to cart - " + productName);
                    } else {
                        System.out.println("[Searching] " + n + ": PRODUCT_DETAIL - " + productName);
                    }
                }
                case ABOUT -> driver.get("http://techmarket.lab/about.html");
                case CONTACT -> {
                    // Open the page only — no form filling
                    driver.get("http://techmarket.lab/contact.html");
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
