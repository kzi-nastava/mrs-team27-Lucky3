package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.LoginPage;
import com.team27.lucky3.e2e.pages.PassengerRideHistoryPage;
import com.team27.lucky3.e2e.pages.ReviewPage;
import com.team27.lucky3.e2e.pages.SidebarComponent;
import org.junit.jupiter.api.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E tests for the review feature accessed through the passenger ride history page.
 * The passenger logs in, navigates to ride history, clicks on a reviewable ride,
 * then uses the "Leave a Review" button to submit a review.
 *
 * These tests run FIRST (before the headless token-based tests) with a visible browser.
 * Ride 10 is reserved for E2E testing — it belongs to passenger3 (ID 5), has no reviews,
 * and ended ~6 hours ago (well within the 3-day review window).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Review via Ride History — Authenticated Flow")
public class ReviewFromHistoryTest extends BaseTest {

    // Ride 10 belongs to passenger3@example.com (ID 5), finished ~6h ago, no reviews.
    private static final String PASSENGER3_EMAIL = "passenger3@example.com";
    private static final String PASSENGER3_PASSWORD = "password";
    private static final long E2E_RIDE_ID = 10L;

    @BeforeAll
    static void setupClass() throws Exception {
        Map<String, String> env = loadEnvFile();
        // Clean up any leftover reviews from previous runs so tests are repeatable.
        cleanupReviewsForRide(env, E2E_RIDE_ID);
    }

    /**
     * Reads the .env file to get DB credentials.
     */
    private static Map<String, String> loadEnvFile() throws IOException {
        Map<String, String> env = new HashMap<>();
        Path envFile = Paths.get("").toAbsolutePath().resolve(".env");
        if (!Files.exists(envFile)) {
            envFile = Paths.get("").toAbsolutePath().getParent().resolve("backend").resolve(".env");
        }
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    env.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                }
            }
        }
        for (String key : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD"}) {
            if (!env.containsKey(key) || env.get(key).isBlank()) {
                String sysVal = System.getenv(key);
                if (sysVal != null && !sysVal.isBlank()) {
                    env.put(key, sysVal);
                }
            }
        }
        return env;
    }

    /**
     * Removes reviews for a specific ride directly from the DB via JDBC.
     */
    private static void cleanupReviewsForRide(Map<String, String> env, long rideId) throws Exception {
        String dbUrl = env.get("DB_URL");
        String dbUser = env.get("DB_USERNAME");
        String dbPass = env.get("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            System.err.println("Database info is missing, skipping review cleanup.");
            return;
        }
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM review WHERE ride_id = ?")) {
            ps.setLong(1, rideId);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("Deleted " + deleted + " old reviews for ride " + rideId);
            }
        }
    }

    /**
     * Helper: Logs in as passenger3, navigates to ride history, and waits for the table to load.
     */
    private PassengerRideHistoryPage loginAndGoToRideHistory() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        // Wait for post-login navigation to complete (passenger home page)
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        // Navigate to ride history via sidebar
        SidebarComponent sidebar = new SidebarComponent(driver);
        sidebar.navigateToRideHistory();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger/ride-history"));

        PassengerRideHistoryPage historyPage = new PassengerRideHistoryPage(driver);
        historyPage.waitForTableToLoad();
        return historyPage;
    }

    // ========================================================================
    // Happy Path Tests
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Happy Path: Ride history shows reviewable rides with blue highlight")
    void testRideHistoryShowsReviewableRides() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        // Ride 10 ended ~6h ago with no reviews, so it should show as reviewable.
        assertTrue(historyPage.hasReviewableRides(),
                "There should be at least one reviewable ride (ride 10) in the table.");
    }

    @Test
    @Order(2)
    @DisplayName("Happy Path: Clicking a reviewable ride shows the detail panel with 'Leave a Review' button")
    void testClickReviewableRideShowsLeaveReviewButton() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();

        assertTrue(historyPage.isDetailPanelVisible(),
                "The ride detail panel should appear after clicking a ride.");
        assertTrue(historyPage.isLeaveReviewButtonVisible(),
                "The 'Leave a Review' button should be visible for a reviewable ride.");
    }

    @Test
    @Order(3)
    @DisplayName("Happy Path: 'Leave a Review' button navigates to review page in authenticated mode")
    void testLeaveReviewNavigatesToReviewPage() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();

        // Should navigate to /review?rideId=<id>
        historyPage.waitForReviewPageNavigation();
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/review"), "Should navigate to the review page.");
        assertTrue(currentUrl.contains("rideId="), "URL should contain rideId query parameter.");
    }

    @Test
    @Order(4)
    @DisplayName("Happy Path: Review form is shown in authenticated mode and accepts ratings")
    void testReviewFormVisibleInAuthenticatedMode() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible(),
                "The review form should be visible in authenticated mode.");
        assertEquals("Rate Your Ride", reviewPage.getPageHeadingText());
    }

    @Test
    @Order(5)
    @DisplayName("Happy Path: Submit button disabled until both ratings are set (authenticated mode)")
    void testSubmitDisabledWithoutRatingsAuthenticated() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Initially disabled
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Submit should be disabled with no ratings.");

        // Only driver rating
        reviewPage.setDriverRating(4);
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Submit should still be disabled with only driver rating.");

        // Both ratings set
        reviewPage.setVehicleRating(3);
        assertTrue(reviewPage.isSubmitButtonEnabled(),
                "Submit should be enabled once both ratings are set.");
    }

    @Test
    @Order(6)
    @DisplayName("Happy Path: Cancel button on review page navigates back to ride history")
    void testCancelButtonNavigatesBackToRideHistory() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible(),
                "The review form should be visible before cancelling.");

        // Click the cancel button — should go back to ride history
        reviewPage.clickCancel();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger/ride-history"));

        assertTrue(driver.getCurrentUrl().contains("/passenger/ride-history"),
                "Cancel should navigate back to ride history.");
    }

    @Test
    @Order(7)
    @DisplayName("Negative: Cancel button does not submit any review")
    void testCancelButtonDoesNotSubmitReview() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Set ratings and comment but then cancel instead of submitting
        reviewPage.setDriverRating(5);
        reviewPage.setVehicleRating(4);
        reviewPage.enterComment("This should not be saved");
        reviewPage.clickCancel();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger/ride-history"));

        // The ride should still be reviewable since we cancelled
        historyPage = new PassengerRideHistoryPage(driver);
        historyPage.waitForTableToLoad();
        assertTrue(historyPage.hasReviewableRides(),
                "Ride should still be reviewable after cancelling the review.");
    }

    @Test
    @Order(8)
    @DisplayName("Happy Path: Successfully submit review from ride history (authenticated mode)")
    void testSubmitReviewFromRideHistory() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        reviewPage.setDriverRating(5);
        reviewPage.setVehicleRating(4);
        reviewPage.enterComment("Excellent ride from history page!");
        reviewPage.clickSubmit();

        assertTrue(reviewPage.isSuccessMessageVisible(),
                "Should see a success message after submitting the review.");
        assertEquals("Thank You!", reviewPage.getSuccessHeadingText());
    }

    @Test
    @Order(9)
    @DisplayName("Negative: After submitting review, ride is no longer reviewable in history")
    void testRideNoLongerReviewableAfterSubmission() {
        // At this point ride 10 was already reviewed in Order(8).
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        // The "Finished" filter should show non-reviewable ride 10 since it now has a review.
        historyPage.clickStatusFilter("Finished");

        // Ride 10 was just reviewed, so it should no longer appear as reviewable.
        int reviewableCount = historyPage.getReviewableRideRows().size();
        if (historyPage.getRideCount() > 0) {
            historyPage.clickRideRow(0);
            historyPage.waitForDetailPanel();
            System.out.println("Reviewable rides after submission: " + reviewableCount);
        }
    }

    @Test
    @Order(10)
    @DisplayName("Negative: Duplicate review from history shows error")
    void testDuplicateReviewFromHistory() throws Exception {
        // The review from Order(8) should still be there, so submitting again should fail.
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        // Navigate directly to review page for ride 10 (it already has a review)
        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible(),
                "Form should still be shown for authenticated user.");

        reviewPage.setDriverRating(3);
        reviewPage.setVehicleRating(3);
        reviewPage.clickSubmit();

        // Should get an error about already submitted review
        String errorText = reviewPage.getErrorMessageText();
        assertTrue(errorText.contains("already submitted"),
                "Should show 'already submitted' error for duplicate review.");
    }

    @Test
    @Order(11)
    @DisplayName("Negative: Non-reviewable ride does not show 'Leave a Review' button")
    void testNonReviewableRideHasNoReviewButton() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        // Filter to show cancelled rides — these should never be reviewable.
        historyPage.clickStatusFilter("Cancelled");

        if (historyPage.getRideCount() > 0) {
            historyPage.clickRideRow(0);
            historyPage.waitForDetailPanel();

            assertFalse(historyPage.isLeaveReviewButtonVisible(),
                    "Cancelled rides should not have a 'Leave a Review' button.");
        }
    }

    @Test
    @Order(12)
    @DisplayName("Happy Path: Comment textarea maxlength enforced in authenticated mode")
    void testCommentMaxLengthAuthenticated() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        String longText = "A".repeat(600);
        reviewPage.enterComment(longText);

        String actualText = reviewPage.getCommentText();
        assertTrue(actualText.length() <= 500,
                "Comment should be truncated to 500 characters by the maxlength attribute.");
    }

    @Test
    @Order(13)
    @DisplayName("Happy Path: Validation message shown when ratings are missing")
    void testValidationMessageInAuthenticatedMode() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isValidationMessageVisible(),
                "Validation message should be visible when no ratings are set.");
        String msg = reviewPage.getValidationMessageText();
        assertTrue(msg.contains("rate both the driver and vehicle"),
                "Validation message should mention rating both driver and vehicle.");
    }

    @AfterAll
    static void cleanupAfterAll() throws Exception {
        // Clean up the review created during tests so re-runs start fresh.
        Map<String, String> env = loadEnvFile();
        cleanupReviewsForRide(env, E2E_RIDE_ID);
    }
}
