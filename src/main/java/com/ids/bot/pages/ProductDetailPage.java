package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ProductDetailPage extends BasePage {

    private static final By PRODUCT_NAME   = By.cssSelector("h1.product-title, h1, .product-name");
    private static final By ADD_TO_CART    = By.cssSelector(".add-to-cart, button[id*='cart'], input[value*='Sepet']");

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
     * Sepete ekle — buton bir JS alert() tetikliyor (network'e yansımaz).
     * Tıklamadan hemen sonra alert kabul edilir.
     */
    public void addToCart() {
        click(ADD_TO_CART);
        driver.switchTo().alert().accept();
    }
}
