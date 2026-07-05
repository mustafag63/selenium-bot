package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

/**
 * Shared Selenium infrastructure. All page classes extend this.
 *
 * Only explicit waits are used — implicit wait must NEVER be set.
 * Mixing the two causes unpredictable timing behaviour per the Selenium documentation.
 */
public abstract class BasePage {

    protected static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);

    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    protected WebElement waitForElement(By locator) {
        return new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.elementToBeClickable(locator))
            .click();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }
}
