package com.team27.lucky3.e2e.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Base test class for all E2E tests.
 * Sets up Chrome in incognito mode with consistent English locale.
 *
 * Set the environment variable E2E_HEADLESS=true to run in headless mode (useful for CI).
 */
@Tag("e2e")
public class BaseTest {

    protected static final String BASE_URL = "http://localhost:4200";
    protected WebDriver driver;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        // Force English locale to ensure consistent date formats (MM/dd/yyyy) across environments
        options.addArguments("--lang=en-US");

        // Support headless mode for CI environments
        if ("true".equalsIgnoreCase(System.getenv("E2E_HEADLESS"))
                || "true".equalsIgnoreCase(System.getProperty("e2e.headless"))) {
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
        }

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.get(BASE_URL);
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
