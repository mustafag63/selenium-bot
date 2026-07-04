package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {

    private static final String URL = "http://techmarket.lab/index.html";

    private static final By NAV_PRODUCTS = By.cssSelector("a[href*='products']");
    private static final By NAV_ABOUT    = By.cssSelector("a[href*='about']");
    private static final By NAV_CONTACT  = By.cssSelector("a[href*='contact']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(URL);
    }

    public void goToProducts() {
        click(NAV_PRODUCTS);
    }

    public void goToAbout() {
        click(NAV_ABOUT);
    }

    public void goToContact() {
        click(NAV_CONTACT);
    }
}
