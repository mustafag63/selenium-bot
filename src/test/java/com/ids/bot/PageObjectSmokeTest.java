package com.ids.bot;

import com.ids.bot.pages.*;
import com.ids.bot.util.UserAgentPool;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gerçek Chrome + lab (techmarket.lab) gerektirir.
 * mvn test -Dgroups=integration
 */
@Tag("integration")
class PageObjectSmokeTest {

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
    void homePageOpens() {
        new HomePage(driver).open();
        assertTrue(driver.getCurrentUrl().contains("techmarket.lab"),
            "URL should contain techmarket.lab");
    }

    @Test
    void productsPageReturnsProductList() {
        ProductsPage page = new ProductsPage(driver);
        page.open();
        assertTrue(page.getProductCount() > 0, "Products page should list at least one product");
    }

    @Test
    void aboutPageLoads() {
        driver.get("http://techmarket.lab/about.html");
        assertFalse(driver.getPageSource().isEmpty(), "About page source should not be empty");
    }

    @Test
    void contactPageLoads() {
        new ContactPage(driver).open();
        assertTrue(driver.getCurrentUrl().contains("contact"),
            "URL should contain 'contact'");
    }

    @Test
    void productDetailPageLoads() {
        ProductsPage products = new ProductsPage(driver);
        products.open();
        assumeProductsExist(products);
        products.clickRandomProduct();
        String name = new ProductDetailPage(driver).getProductName();
        assertFalse(name.isBlank(), "Product name should not be blank");
    }

    private void assumeProductsExist(ProductsPage page) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            page.getProductCount() > 0, "Skipping: no products found on page");
    }
}
