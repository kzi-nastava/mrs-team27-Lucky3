package com.team27.lucky3.e2e.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Foundation for our E2E suite. Sets up Chrome with the right flags 
 * and handles the browser lifecycle.
 *
 * Pro tip: Set E2E_HEADLESS=true in your environment if you want to run 
 * these in CI without a visible browser window.
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
        // We force US English so that date formats (like 12/31/2025) stay 
        // consistent no matter where the test is running.
        options.addArguments("--lang=en-US");

        // CI-friendly headless mode.
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
