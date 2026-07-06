package com.ids.bot.scenarios;

import com.ids.bot.pages.*;
import com.ids.bot.util.*;
import org.openqa.selenium.WebDriver;

public class BrowsingScenario {

    private final WebDriver            driver;
    private final MarkovTransitionModel model;
    private final TimingProfile         timingProfile;

    private final HomePage          homePage;
    private final ProductsPage      productsPage;
    private final ProductDetailPage detailPage;

    public BrowsingScenario(WebDriver driver) {
        this.driver        = driver;
        this.model         = BotPersonas.browsingModel();
        this.timingProfile = TimingProfiles.browsing();
        this.homePage      = new HomePage(driver);
        this.productsPage  = new ProductsPage(driver);
        this.detailPage    = new ProductDetailPage(driver);
    }

    public void run() throws InterruptedException {
        homePage.open();
        BotState current = BotState.HOME;
        int actionCount  = SessionIntensity.BROWSING.sample();

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
                        System.out.println("[Browsing] " + n + ": PRODUCT_DETAIL skipped (no products)");
                        current = BotState.PRODUCTS;
                        waitBetweenActions(timingProfile);
                        continue;
                    }
                    productsPage.clickRandomProduct();
                    String productName = detailPage.getProductName();
                    System.out.println("[Browsing] " + n + ": PRODUCT_DETAIL - " + productName);
                }
                case ABOUT -> driver.get("http://techmarket.lab/about.html");
                case CONTACT -> driver.get("http://techmarket.lab/contact.html");
            }

            if (next != BotState.PRODUCT_DETAIL) {
                System.out.println("[Browsing] " + n + ": " + next);
            }

            current = next;
            waitBetweenActions(timingProfile);
        }

        System.out.println("[Browsing] session ending — user left");
        System.out.println("[Browsing] Scenario completed (" + actionCount + " actions)");
    }

    static void waitBetweenActions(TimingProfile profile) throws InterruptedException {
        if (profile.shouldHaveActiveIdleCycle()) {
            Thread.sleep((long) profile.sampleActiveDurationMs());
            Thread.sleep((long) profile.sampleIdleDurationMs());
        } else {
            Thread.sleep((long) profile.sampleFwdWaitMs());
        }
    }
}
