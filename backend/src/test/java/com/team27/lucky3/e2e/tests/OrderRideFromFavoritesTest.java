package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for ordering rides from favorites list functionality.
 * Tests the complete user flow from favorites selection to order form prefill.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Passenger - Order Ride from Favorites Tests")
public class OrderRideFromFavoritesTest extends BaseTest {

    private static final Logger logger = LogManager.getLogger(OrderRideFromFavoritesTest.class);
    private FavoritesPage favoritesPage;
    private HomePage homePage;
    private SidebarComponent sidebar;
    private LoginPage loginPage;

    private static final String BASE_URL = "http://localhost:4200";
    private static final String FAVORITES_URL = BASE_URL + "/favorites";

    @BeforeEach
    public void setUpPages() {
        loginPage = new LoginPage(driver);
        homePage = new HomePage(driver);
        sidebar = new SidebarComponent(driver);
        favoritesPage = new FavoritesPage(driver);

        // Login as passenger and navigate to favorite rides
        loginPage.loginAsPassenger();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/passenger/home"));
        sidebar.navigateToFavoriteRides();
        wait.until(ExpectedConditions.urlContains("/passenger/favorites"));
    }

    @Test
    @DisplayName("Verify user can order ride from favorites and fields are prefilled correctly")
    public void testOrderRideFromFavorites() {
        logger.info("TEST: Order ride from favorites with prefilled data");

        assertTrue(favoritesPage.isPageLoaded(),
            "Favorites page should be loaded");
        logger.info("✓ Favorites page loaded successfully");

        favoritesPage.waitForLoadingToComplete();

        assertTrue(favoritesPage.getFavoriteRoutesCount() > 0,
            "User should have at least one favorite route");
        logger.info("✓ User has favorite routes available");

        String expectedStartLocation = favoritesPage.getStartLocationByIndex(0);
        String expectedDestination = favoritesPage.getDestinationLocationByIndex(0);
        String routeName = favoritesPage.getRouteNameByIndex(0);

        logger.info("Selected route: {}", routeName);
        logger.info("Expected start location: {}", expectedStartLocation);
        logger.info("Expected destination: {}", expectedDestination);

        homePage = favoritesPage.clickOrderButtonByIndex(0);

        assertTrue(homePage.isPageLoaded(),
            "Home page should be loaded after clicking Order button");
        logger.info("✓ Redirected to home page");

        assertTrue(homePage.isOrderingFormDisplayed(),
            "Order ride dialog should be opened automatically");
        logger.info("✓ Order ride dialog opened");

        String actualStartLocation = homePage.getPickupAddress();
        String actualDestination = homePage.getDestinationAddress();

        assertEquals(actualStartLocation, expectedStartLocation,
            "Pickup address should be prefilled with start location from favorite");
        logger.info("✓ Start location prefilled correctly: {}", actualStartLocation);

        assertEquals(actualDestination, expectedDestination,
            "Destination address should be prefilled with end location from favorite");
        logger.info("✓ Destination prefilled correctly: {}", actualDestination);

        assertTrue(homePage.isVehicleTypeSelected("standard"),
            "Standard vehicle type should be selected by default");
        logger.info("✓ Default vehicle type: Standard");

        assertFalse(homePage.isPetTransportSelected(),
            "Pet transport should not be selected by default");
        logger.info("✓ Pet transport: Not selected (default)");

        assertFalse(homePage.isBabyTransportSelected(),
            "Baby transport should not be selected by default");
        logger.info("✓ Baby transport: Not selected (default)");

        assertTrue(homePage.isScheduleNowSelected(),
            "Schedule 'Now' should be selected by default");
        logger.info("✓ Schedule time: Now (default)");

        logger.info("TEST PASSED: All fields prefilled correctly with default values");
    }
}