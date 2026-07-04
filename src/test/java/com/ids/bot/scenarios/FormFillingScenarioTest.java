package com.ids.bot.scenarios;

import com.ids.bot.App;
import com.ids.bot.pages.ContactPage;
import com.ids.bot.util.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

class FormFillingScenarioTest {

    // -------------------------------------------------------------------------
    // Lab'sız unit testler
    // -------------------------------------------------------------------------

    @Test
    void entryCountWithinExpectedRange() {
        for (int i = 0; i < 10_000; i++) {
            int s = SessionIntensity.FORM_FILLING.sample();
            assertTrue(s >= 1 && s <= 6,
                "FORM_FILLING entry count out of range: " + s);
        }
    }

    @Test
    void homeReturnRateMatchesTarget() {
        // shouldReturnToHomeAfterSubmit() 100.000 kez çağrılır, oran 0.30±0.02 olmalı.
        // FormFillingScenario'yu driver olmadan test etmek için minimal subclass.
        FormFillingScenario scenario = new FormFillingScenario(null) {
            // driver null — sadece shouldReturnToHomeAfterSubmit çağrılıyor
        };

        int trueCount = 0;
        int N = 100_000;
        for (int i = 0; i < N; i++) {
            if (scenario.shouldReturnToHomeAfterSubmit()) trueCount++;
        }

        double rate = (double) trueCount / N;
        assertEquals(0.30, rate, 0.02,
            "shouldReturnToHomeAfterSubmit rate should be 0.30 ± 0.02, got " + rate);
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
        void contactFormAlwaysSubmitsWithoutError() {
            ContactPage page = new ContactPage(driver);
            page.open();

            String name    = FormDataPool.randomName();
            String email   = FormDataPool.randomEmail();
            String subject = FormDataPool.randomSubject();
            String message = FormDataPool.randomMessage();

            page.fillForm(name, email, subject, message);

            // Alanlar dolu olmalı (submit öncesi kontrol)
            String nameVal = driver.findElement(
                org.openqa.selenium.By.cssSelector("#name, input[name='name']")).getAttribute("value");
            assertFalse(nameVal == null || nameVal.isBlank(),
                "Name field should be filled before submit");

            // Submit exception fırlatmamalı
            // (success-banner sitede yok — bilinçli descope; sadece exception yokluğu yeterli)
            assertDoesNotThrow(page::submit,
                "submit() should complete without throwing");
        }
    }
}
