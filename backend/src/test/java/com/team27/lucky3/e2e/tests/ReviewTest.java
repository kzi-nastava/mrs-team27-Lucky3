package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.ReviewPage;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These are E2E tests for the ride review feature using direct token links.
 * Passengers should be able to rate both the driver and the vehicle after a ride.
 * We're testing both successful submissions and various error cases like expired links or missing ratings.
 *
 * These tests use direct URL access with review tokens and don't require any UI navigation or login flow.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Review via Token Link")
public class ReviewTest extends BaseTest {

    // The JWT secret is pulled from the .env file so we don't have to hardcode it.
    private static String JWT_SECRET;

    // These IDs are from the default data seeded by DataInitializer.
    // Ride 10 is finished and has no reviews yet.
    private static final long E2E_RIDE_ID = 10L;
    private static final long E2E_DRIVER_ID = 1L;
    private static final long E2E_PASSENGER_ID = 5L;

    // Ride 3 already has a review, so we use it to test duplicate prevention.
    private static final long REVIEWED_RIDE_ID = 3L;
    private static final long REVIEWED_PASSENGER_ID = 3L;
    private static final long REVIEWED_DRIVER_ID = 1L;

    // Ride 5 finished more than 3 days ago, which is the time limit for reviews.
    private static final long EXPIRED_PERIOD_RIDE_ID = 5L;
    private static final long EXPIRED_PERIOD_PASSENGER_ID = 3L;
    private static final long EXPIRED_PERIOD_DRIVER_ID = 2L;

    @BeforeAll
    static void setupClass() throws Exception {
        Map<String, String> env = E2ETestUtils.loadEnvFile("JWT_SECRET", "DB_URL", "DB_USERNAME", "DB_PASSWORD");
        JWT_SECRET = env.get("JWT_SECRET");
        if (JWT_SECRET == null || JWT_SECRET.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is missing from the env!");
        }
        // We clean up any existing reviews for ride 10 so the test is repeatable.
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }

    /**
     * Generates a signed review token for testing.
     */
    private String generateReviewToken(long rideId, long passengerId, long driverId, long expiryMs) {
        Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .setIssuer("lucky3-review")
                .setSubject("review")
                .claim("rideId", rideId)
                .claim("passengerId", passengerId)
                .claim("driverId", driverId)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // A valid token with a 3-day expiry.
    private String generateValidToken() {
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        return generateReviewToken(E2E_RIDE_ID, E2E_PASSENGER_ID, E2E_DRIVER_ID, threeDaysMs);
    }

    // A token that has already expired.
    private String generateExpiredToken() {
        return generateReviewToken(E2E_RIDE_ID, E2E_PASSENGER_ID, E2E_DRIVER_ID, -1000);
    }

    // A token for a ride that was already reviewed.
    private String generateAlreadyReviewedToken() {
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        return generateReviewToken(REVIEWED_RIDE_ID, REVIEWED_PASSENGER_ID, REVIEWED_DRIVER_ID, threeDaysMs);
    }

    // A token for a ride that ended more than 3 days ago.
    private String generateExpiredPeriodToken() {
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        return generateReviewToken(EXPIRED_PERIOD_RIDE_ID, EXPIRED_PERIOD_PASSENGER_ID, EXPIRED_PERIOD_DRIVER_ID, threeDaysMs);
    }

    // Happy Path Tests

    @Test
    @Order(1)
    @DisplayName("Happy Path: Successfully submit a review with ratings and comment")
    void testReviewSubmissionHappyPath() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Check if the form shows up as expected.
        assertTrue(reviewPage.isReviewFormVisible(),
                "The review form should be visible since our token is valid.");

        assertEquals("Rate Your Ride", reviewPage.getPageHeadingText());

        // Fill out the ratings and a comment.
        reviewPage.setDriverRating(5);
        reviewPage.setVehicleRating(4);
        reviewPage.enterComment("Great service!");

        reviewPage.clickSubmit();

        // Make sure the "Thank You!" message pops up.
        assertTrue(reviewPage.isSuccessMessageVisible(),
                "We should see a success message after submitting.");
        assertEquals("Thank You!", reviewPage.getSuccessHeadingText());
    }

    @Test
    @Order(2)
    @DisplayName("Happy Path: Submit button becomes enabled without comment (comment is optional)")
    void testSubmitEnabledWithoutComment() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible());

        // Set ratings but leave the comment empty.
        reviewPage.setDriverRating(3);
        reviewPage.setVehicleRating(3);

        // It should still let us submit.
        assertTrue(reviewPage.isSubmitButtonEnabled(),
                "Submit should be enabled without a comment since it's optional.");
    }

    @Test
    @Order(3)
    @DisplayName("Happy Path: Verify review form heading, labels, and textarea visible")
    void testReviewFormElementsPresent() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible());
        assertEquals("Rate Your Ride", reviewPage.getPageHeadingText());
        assertEquals("", reviewPage.getCommentText(), "Textarea should start empty.");
    }

    // Validation and Error Case Tests

    @Test
    @Order(4)
    @DisplayName("Negative: Submit button is disabled when no ratings are set")
    void testSubmitDisabledWhenNoRatings() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // Submit should be off by default.
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Button should be disabled until ratings are provided.");

        // We should also see a warning message.
        assertTrue(reviewPage.isValidationMessageVisible());
        String msg = reviewPage.getValidationMessageText();
        assertTrue(msg.contains("rate both the driver and vehicle"));
    }

    @Test
    @Order(5)
    @DisplayName("Negative: Submit button disabled with only driver rating set")
    void testSubmitDisabledWithOnlyDriverRating() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        reviewPage.setDriverRating(5);
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Still shouldn't be able to submit without a vehicle rating.");
    }

    @Test
    @Order(6)
    @DisplayName("Negative: Submit button disabled with only vehicle rating set")
    void testSubmitDisabledWithOnlyVehicleRating() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        reviewPage.setVehicleRating(3);
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Still shouldn't be able to submit without a driver rating.");
    }

    @Test
    @Order(7)
    @DisplayName("Validation: Submit button becomes enabled when both ratings are set")
    void testSubmitEnabledWhenBothRatingsSet() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertFalse(reviewPage.isSubmitButtonEnabled());

        reviewPage.setDriverRating(5);
        assertFalse(reviewPage.isSubmitButtonEnabled());

        reviewPage.setVehicleRating(3);
        assertTrue(reviewPage.isSubmitButtonEnabled(),
                "Now that both ratings are here, it should be clickable.");
    }

    @Test
    @Order(8)
    @DisplayName("Negative: No token in URL redirects to 404 page")
    void testNoTokenRedirectsTo404() {
        driver.get(BASE_URL + "/review");

        // The guard should kick in and send us to /404.
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/404"));

        assertTrue(driver.getCurrentUrl().contains("/404"));
    }

    @Test
    @Order(9)
    @DisplayName("Negative: Link Expired shown for expired token")
    void testExpiredTokenShowsExpiredState() {
        String expiredToken = generateExpiredToken();
        driver.get(BASE_URL + "/review?token=" + expiredToken);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isTokenExpiredVisible());
        assertEquals("Link Expired", reviewPage.getPageHeadingText());
    }

    @Test
    @Order(10)
    @DisplayName("Negative: Link Expired shown for malformed token")
    void testMalformedTokenShowsExpiredState() {
        driver.get(BASE_URL + "/review?token=invalid_token");

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isTokenExpiredVisible());
        assertEquals("Link Expired", reviewPage.getPageHeadingText());
    }

    @Test
    @Order(11)
    @DisplayName("Negative: Duplicate review submission shows error")
    void testDuplicateReviewSubmission() {
        String token = generateAlreadyReviewedToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible());

        reviewPage.setDriverRating(4);
        reviewPage.setVehicleRating(4);
        reviewPage.clickSubmit();

        // The backend should tell us we've already done this.
        String errorText = reviewPage.getErrorMessageText();
        assertTrue(errorText.contains("already submitted"));
    }

    @Test
    @Order(12)
    @DisplayName("Negative: Comment textarea respects maxlength")
    void testCommentMaxLength() {
        String token = generateValidToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        // If we try to type too much, it should stop at 500 characters.
        String longText = "A".repeat(600);
        reviewPage.enterComment(longText);

        String actualText = reviewPage.getCommentText();
        assertTrue(actualText.length() <= 500);
    }

    @Test
    @Order(13)
    @DisplayName("Negative: 3-day review period expired — submission is rejected")
    void testReviewPeriodExpiredAfterThreeDays() {
        String token = generateExpiredPeriodToken();
        driver.get(BASE_URL + "/review?token=" + token);

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isReviewFormVisible());

        reviewPage.setDriverRating(4);
        reviewPage.setVehicleRating(4);
        reviewPage.clickSubmit();

        // We submitted too late, so the page should switch to the expired view.
        assertTrue(reviewPage.isTokenExpiredVisible());
        assertEquals("Link Expired", reviewPage.getPageHeadingText());
    }

    @AfterAll
    static void cleanupAfterAll() throws Exception {
        // Clean up the review created during tests so re-runs start fresh.
        Map<String, String> env = E2ETestUtils.loadEnvFile("DB_URL", "DB_USERNAME", "DB_PASSWORD");
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }
}
