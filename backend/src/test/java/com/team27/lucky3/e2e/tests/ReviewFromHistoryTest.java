package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.LoginPage;
import com.team27.lucky3.e2e.pages.PassengerRideHistoryPage;
import com.team27.lucky3.e2e.pages.ReviewPage;
import com.team27.lucky3.e2e.pages.SidebarComponent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the end-to-end flow of reviewing a ride through the passenger's 
 * history page. A passenger logs in, finds a recent ride, and leaves feedback.
 *
 * We use Ride 10 for these tests. It belongs to 'passenger3', finished recently, 
 * and starts without any reviews so we have a clean state to work with.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Review via Ride History — Authenticated Flow")
public class ReviewFromHistoryTest extends BaseTest {

    // Ride 10 belongs to passenger3@example.com (ID 5), finished ~6h ago, no reviews.
    private static final String PASSENGER3_EMAIL = "passenger3@example.com";
    private static final String PASSENGER3_PASSWORD = "password";
    private static final long E2E_RIDE_ID = 10L;

    @BeforeAll
    static void setupClass() throws Exception {
        Map<String, String> env = E2ETestUtils.loadEnvFile("DB_URL", "DB_USERNAME", "DB_PASSWORD");
        // Sweep the DB clean before we start so the tests are repeatable.
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }

    // Helper to get the passenger logged in and landed on the history page.
    private PassengerRideHistoryPage loginAndGoToRideHistory() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        // Wait for the redirect to the dashboard.
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        // Navigate to history via the sidebar.
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

        // Ride 10 should be begging for a review right now.
        assertTrue(historyPage.hasReviewableRides(),
                "Ride 10 should be highlighted as reviewable in the table.");
    }

    @Test
    @Order(2)
    @DisplayName("Happy Path: Clicking a reviewable ride shows the detail panel with 'Leave a Review' button")
    void testClickReviewableRideShowsLeaveReviewButton() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();

        assertTrue(historyPage.isDetailPanelVisible(),
                "The ride details should pop up after clicking.");
        assertTrue(historyPage.isLeaveReviewButtonVisible(),
                "We expect to see the 'Leave a Review' button for this ride.");
    }

    @Test
    @Order(3)
    @DisplayName("Happy Path: 'Leave a Review' button navigates to review page in authenticated mode")
    void testLeaveReviewNavigatesToReviewPage() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();

        // Check if we landed on the review form with the right ID in the URL.
        historyPage.waitForReviewPageNavigation();
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("/review"), "Should have navigated to the review page.");
        assertTrue(currentUrl.contains("rideId="), "The URL should specify which ride we're reviewing.");
    }

    @Test
    @Order(4)
    @DisplayName("Happy Path: Review form heading and subtitle are visible after navigating from history")
    void testReviewFormVisibleFromHistory() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible(),
                "The review form should be visible on the page.");
        assertEquals("Rate Your Ride", reviewPage.getPageHeadingText(),
                "The page heading should read 'Rate Your Ride'.");
        assertEquals("How was your experience?", reviewPage.getPageSubtitleText(),
                "The user should see the subtitle asking about their experience.");
    }

    @Test
    @Order(5)
    @DisplayName("Happy Path: Submit button disabled until both ratings are set")
    void testSubmitDisabledWithoutRatings() {
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickFirstReviewableRide();
        historyPage.waitForDetailPanel();
        historyPage.clickLeaveReview();
        historyPage.waitForReviewPageNavigation();

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Button should be off initially, and stars should prompt the user to click.
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Submit shouldn't be allowed without any ratings.");
        assertEquals("Click to rate", reviewPage.getDriverRatingFeedbackText(),
                "The driver stars should show 'Click to rate' before any selection.");
        assertEquals("Click to rate", reviewPage.getVehicleRatingFeedbackText(),
                "The vehicle stars should show 'Click to rate' before any selection.");

        // Just one rating isn't enough.
        reviewPage.setDriverRating(4);
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "We still need the vehicle rating before we can submit.");
        assertEquals("4 of 5 stars", reviewPage.getDriverRatingFeedbackText(),
                "After clicking 4 stars, the user should see '4 of 5 stars' under the driver rating.");

        // Both ratings set: green light.
        reviewPage.setVehicleRating(3);
        assertTrue(reviewPage.isSubmitButtonEnabled(),
                "Submit should be ready now that both ratings are in.");
        assertEquals("3 of 5 stars", reviewPage.getVehicleRatingFeedbackText(),
                "After clicking 3 stars, the user should see '3 of 5 stars' under the vehicle rating.");
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
                "Form should be there before we hit cancel.");

        reviewPage.clickCancel();

        // Should drop us back where we came from.
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger/ride-history"));

        assertTrue(driver.getCurrentUrl().contains("/passenger/ride-history"),
                "Cancel should take us back to the history list.");

        // The ride history table should be visible again.
        PassengerRideHistoryPage backOnHistory = new PassengerRideHistoryPage(driver);
        backOnHistory.waitForTableToLoad();
        assertTrue(backOnHistory.getRideCount() > 0,
                "The ride history table should be visible with rides after cancelling.");
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

        // Fill out the form but change our mind.
        reviewPage.setDriverRating(5);
        reviewPage.setVehicleRating(4);
        reviewPage.enterComment("This should not be saved");
        reviewPage.clickCancel();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger/ride-history"));

        // Confirm the ride is still highlighted as reviewable.
        historyPage = new PassengerRideHistoryPage(driver);
        historyPage.waitForTableToLoad();
        assertTrue(historyPage.hasReviewableRides(),
                "The ride should still be reviewable since we didn't submit.");
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

        // Wait for the success state.
        assertTrue(reviewPage.isSuccessMessageVisible(),
                "We should see the 'Thank You' screen after submission.");
        assertEquals("Thank You!", reviewPage.getSuccessHeadingText(),
                "The success heading should read 'Thank You!'.");
        assertTrue(reviewPage.getSuccessDescriptionText().contains("submitted successfully"),
                "The user should see a confirmation that the review was submitted successfully.");
    }

    @Test
    @Order(9)
    @DisplayName("Negative: After submitting review, ride is no longer reviewable in history")
    void testRideNoLongerReviewableAfterSubmission() {
        // Ride 10 was just polished off in the previous test.
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        // Switch to the 'Finished' view to find our ride.
        historyPage.clickStatusFilter("Finished");

        // It should no longer have the blue highlight.
        int reviewableCount = historyPage.getReviewableRideRows().size();
        assertEquals(0, reviewableCount,
                "Ride 10 should be a regular row now, no longer reviewable.");

        // And the button in the side panel should be hidden.
        if (historyPage.getRideCount() > 0) {
            historyPage.clickRideRow(0);
            historyPage.waitForDetailPanel();
            assertFalse(historyPage.isLeaveReviewButtonVisible(),
                    "The review button should be gone for this ride.");
        }
    }

    @Test
    @Order(10)
    @DisplayName("Negative: Duplicate review from history shows error")
    void testDuplicateReviewFromHistory() throws Exception {
        // Ride 10 already has a review, but we'll try to force another one.
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        // Sneak direct to the review URL.
        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible());

        reviewPage.setDriverRating(3);
        reviewPage.setVehicleRating(3);
        reviewPage.clickSubmit();

        // Verify the backend tells us 'No Way'.
        String errorText = reviewPage.getErrorMessageText();
        assertTrue(errorText.contains("already submitted"),
                "Should show an error preventing a second review.");
    }

    @Test
    @Order(11)
    @DisplayName("Negative: Non-reviewable ride (past 3-day window) does not show 'Leave a Review' button")
    void testNonReviewableRideHasNoReviewButton() {
        // Passenger3 has some older rides that are way past the 3-day cutoff.
        PassengerRideHistoryPage historyPage = loginAndGoToRideHistory();

        historyPage.clickStatusFilter("Finished");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Wait, where did the rides go?");

        // Grab the oldest one in the list.
        historyPage.clickRideRow(rideCount - 1);
        historyPage.waitForDetailPanel();

        assertFalse(historyPage.isLeaveReviewButtonVisible(),
                "Old rides shouldn't have a review button.");
    }

    @Test
    @Order(12)
    @DisplayName("Happy Path: Comment textarea truncates text at 500 characters")
    void testCommentMaxLength() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Blast it with text.
        String longText = "A".repeat(600);
        reviewPage.enterComment(longText);

        // It should be chopped off at 500.
        String actualText = reviewPage.getCommentText();
        assertTrue(actualText.length() <= 500,
                "Text should be truncated by the HTML maxlength attribute.");

        // The user should see the character counter showing the limit was reached.
        assertEquals("500/500", reviewPage.getCharacterCounterText(),
                "The visible character counter should show '500/500' when the limit is reached.");
    }

    @Test
    @Order(13)
    @DisplayName("Happy Path: Validation message tells user to rate both driver and vehicle")
    void testValidationMessageShownWhenRatingsMissing() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginAs(PASSENGER3_EMAIL, PASSENGER3_PASSWORD);

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/passenger"));

        driver.get(BASE_URL + "/review?rideId=" + E2E_RIDE_ID);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Should see the 'please rate' warning immediately.
        assertTrue(reviewPage.isValidationMessageVisible(),
                "A validation hint should be visible before any ratings are set.");
        String msg = reviewPage.getValidationMessageText();
        assertEquals("Please rate both the driver and vehicle to submit", msg,
                "The validation message should tell the user exactly what's needed.");
    }

    @AfterAll
    static void cleanupAfterAll() throws Exception {
        // Clean up our mess so we can run this again.
        Map<String, String> env = E2ETestUtils.loadEnvFile("DB_URL", "DB_USERNAME", "DB_PASSWORD");
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }
}
