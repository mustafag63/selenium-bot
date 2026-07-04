package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

public class ContactPage extends BasePage {

    private static final String URL = "http://techmarket.lab/contact.html";

    private static final By FIELD_NAME    = By.cssSelector("#name, input[name='name']");
    private static final By FIELD_EMAIL   = By.cssSelector("#email, input[name='email'], input[type='email']");
    private static final By FIELD_SUBJECT = By.cssSelector("#subject, select[name='subject']");
    private static final By FIELD_MESSAGE = By.cssSelector("#message, textarea[name='message']");
    private static final By BTN_SUBMIT    = By.cssSelector("button[type='submit'], input[type='submit']");

    public ContactPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(URL);
    }

    public void fillForm(String name, String email, String subject, String message) {
        waitForElement(FIELD_NAME).sendKeys(name);
        driver.findElement(FIELD_EMAIL).sendKeys(email);

        Select subjectSelect = new Select(driver.findElement(FIELD_SUBJECT));
        subjectSelect.selectByVisibleText(subject);

        driver.findElement(FIELD_MESSAGE).sendKeys(message);
    }

    /**
     * Formu native HTML submit ile gönderir (gerçek POST /submit-contact isteği).
     * Nginx backend'i olmadığı için 404 dönebilir ama bu isteğin loglanmasını etkilemez.
     */
    public void submit() {
        click(BTN_SUBMIT);
    }
}
