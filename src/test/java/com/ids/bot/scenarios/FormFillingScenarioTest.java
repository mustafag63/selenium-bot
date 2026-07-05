package com.ids.bot.scenarios;

import com.ids.bot.App;
import com.ids.bot.pages.ContactPage;
import com.ids.bot.util.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

class FormFillingScenarioTest {

    // -------------------------------------------------------------------------
    // Unit tests — no lab required
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
        // shouldReturnToHomeAfterSubmit() is called 100 000 times; expected rate 0.30±0.02.
        // Minimal subclass so FormFillingScenario can be exercised without a driver.
        FormFillingScenario scenario = new FormFillingScenario(null) {
            // driver is null — only shouldReturnToHomeAfterSubmit is exercised
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
        void allSubjectsSelectableWithoutError() {
            // Rastgele seçim yerine her seçeneği teker teker dener —
            // "şans eseri geçen test" riskini ortadan kaldıran deterministik kapsama testi.
            for (String subject : FormDataPool.allSubjects()) {
                driver.get("http://techmarket.lab/contact.html"); // her iterasyonda temiz form
                ContactPage page = new ContactPage(driver);
                assertDoesNotThrow(
                    () -> page.fillForm("Test User", "test@example.com", subject, "Test message"),
                    "selectByVisibleText failed for subject: \"" + subject + "\""
                );
                System.out.println("[SubjectTest] OK: \"" + subject + "\"");
            }
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

            // Fields should be populated before submit
            String nameVal = driver.findElement(
                org.openqa.selenium.By.cssSelector("#name, input[name='name']")).getAttribute("value");
            assertFalse(nameVal == null || nameVal.isBlank(),
                "Name field should be filled before submit");

            // Submit should not throw
            // (no success-banner on the site — intentional descope; absence of exception is enough)
            assertDoesNotThrow(page::submit,
                "submit() should complete without throwing");
        }
    }
}
