package com.ids.bot.scenarios;

import com.ids.bot.pages.*;
import com.ids.bot.util.*;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.ThreadLocalRandom;

public class FormFillingScenario {

    /**
     * Submit sonrası tarayıcının isteği tamamlamasına izin verilen bekleme.
     * Genel waitBetweenActions mantığından FARKLI, kasıtlı olarak ayrı bir zamanlama.
     * (Bu proje tarihinde bu farkın script analizinde kategori hatasına yol açtığı,
     * ama botun kendisinin doğru olduğu netleşmişti.)
     */
    private static final long SUBMIT_SETTLE = 2500L;

    private final WebDriver             driver;
    private final TimingProfile         timingProfile;
    private final MarkovTransitionModel entryModel;

    private final HomePage     homePage;
    private final ProductsPage productsPage;
    private final ContactPage  contactPage;

    public FormFillingScenario(WebDriver driver) {
        this.driver        = driver;
        this.timingProfile = TimingProfiles.formFilling();
        this.entryModel    = BotPersonas.formFillingModel();
        this.homePage      = new HomePage(driver);
        this.productsPage  = new ProductsPage(driver);
        this.contactPage   = new ContactPage(driver);
    }

    public void run() throws InterruptedException {
        runEntryPhase();
        runContactPhase();

        if (shouldReturnToHomeAfterSubmit()) {
            homePage.open();
            System.out.println("[FormFilling] Returned to HOME after submitting - session continues briefly");
            Thread.sleep(SUBMIT_SETTLE);
        }

        System.out.println("[FormFilling] session ending — user left");
        System.out.println("[FormFilling] Scenario completed");
    }

    void runEntryPhase() throws InterruptedException {
        int entryCount = SessionIntensity.FORM_FILLING.sample();
        BotState current = BotState.HOME;

        for (int n = 1; n <= entryCount; n++) {
            BotState next = entryModel.nextState(current);
            switch (next) {
                case PRODUCTS -> productsPage.open();
                case ABOUT    -> driver.get("http://techmarket.lab/about.html");
                default       -> homePage.open();
            }
            System.out.println("[FormFilling] Entry " + n + ": " + next + " (brief orientation)");
            current = next;
            waitBetweenActions(timingProfile);
        }
    }

    void runContactPhase() throws InterruptedException {
        contactPage.open();
        System.out.println("[FormFilling] CONTACT page opened - filling form");

        String name    = FormDataPool.randomName();
        String email   = FormDataPool.randomEmail();
        String subject = FormDataPool.randomSubject();
        String message = FormDataPool.randomMessage();

        contactPage.fillForm(name, email, subject, message);

        // İnsan yazma hızını taklit eden typing delay'leri
        for (int i = 0; i < 5; i++) {
            Thread.sleep(ThreadLocalRandom.current().nextLong(400, 1200));
        }

        contactPage.submit();
        System.out.println("[FormFilling] Contact form submitted");

        Thread.sleep(SUBMIT_SETTLE);
    }

    boolean shouldReturnToHomeAfterSubmit() {
        return ThreadLocalRandom.current().nextDouble() < returnToHomeProbability();
    }

    /** Ayrı metod — test edilebilirlik için (inline sabite çevrilmedi, bilinçli karar). */
    double returnToHomeProbability() {
        return 0.30;
    }

    static void waitBetweenActions(TimingProfile profile) throws InterruptedException {
        BrowsingScenario.waitBetweenActions(profile);
    }
}
