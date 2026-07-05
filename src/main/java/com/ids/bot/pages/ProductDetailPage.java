package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ProductDetailPage extends BasePage {

    private static final By PRODUCT_NAME = By.cssSelector(".info h2");
    private static final By ADD_TO_CART  = By.xpath("//button[contains(text(),'Sepete Ekle')]");

    public ProductDetailPage(WebDriver driver) {
        super(driver);
    }

    public String getProductName() {
        try {
            return waitForElement(PRODUCT_NAME).getText();
        } catch (Exception e) {
            return "(unknown product)";
        }
    }

    /**
     * Adds to cart — the button triggers a JS alert() (no network request is made).
     * The alert is accepted immediately after the click.
     */
    public void addToCart() {
        click(ADD_TO_CART);
        org.openqa.selenium.Alert alert = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.alertIsPresent());
        String alertText = alert.getText();
        alert.accept();
        System.out.println("[ProductDetail] Alert: " + alertText);
    }
}
