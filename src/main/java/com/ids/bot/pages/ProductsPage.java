package com.ids.bot.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ProductsPage extends BasePage {

    private static final String URL = "http://techmarket.lab/products.html";

    private static final By PRODUCT_CARDS = By.cssSelector(".product-card a, .product-item a, a[href*='product-']");

    public ProductsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(URL);
    }

    public int getProductCount() {
        List<WebElement> cards = driver.findElements(PRODUCT_CARDS);
        return cards.size();
    }

    public void clickRandomProduct() {
        List<WebElement> cards = driver.findElements(PRODUCT_CARDS);
        if (cards.isEmpty()) return;
        int idx = ThreadLocalRandom.current().nextInt(cards.size());
        cards.get(idx).click();
    }
}
