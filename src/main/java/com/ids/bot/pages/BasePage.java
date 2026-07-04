package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

/**
 * Ortak Selenium altyapısı. Tüm page sınıfları bunu extend eder.
 *
 * Sadece explicit wait kullanılır — implicit wait KESİNLİKLE set edilmez.
 * İkisi karıştırılırsa Selenium dökümanına göre öngörülemez gecikme riski doğar.
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
        waitForElement(locator).click();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }
}
