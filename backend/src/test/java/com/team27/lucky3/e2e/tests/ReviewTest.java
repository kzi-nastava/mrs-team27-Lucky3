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
 * Focuses on the token-based review flow—the kind of links passengers get 
 * via email or SMS. No login required here, just the right token in the URL.
 * We're testing successful submissions and edge cases like expired or reused links.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Review via Token Link")
public class ReviewTest extends BaseTest {

    // Pull the JWT secret from .env so we don't have it floating around in the code.
    private static String JWT_SECRET;

    // These IDs match the default seed data from DataInitializer.
    // Ride 10 is our go-to 'fresh' ride for E2E testing.
    private static final long E2E_RIDE_ID = 10L;
    private static final long E2E_DRIVER_ID = 1L;
    private static final long E2E_PASSENGER_ID = 5L;

    // Ride 3 already has reviews, perfect for testing duplicate prevention.
    private static final long REVIEWED_RIDE_ID = 3L;
    private static final long REVIEWED_PASSENGER_ID = 3L;
    private static final long REVIEWED_DRIVER_ID = 1L;

    // Ride 5 is way too old to be reviewed (3-day limit).
    private static final long EXPIRED_PERIOD_RIDE_ID = 5L;
    private static final long EXPIRED_PERIOD_PASSENGER_ID = 3L;
    private static final long EXPIRED_PERIOD_DRIVER_ID = 2L;

    @BeforeAll
    static void setupClass() throws Exception {
        Map<String, String> env = E2ETestUtils.loadEnvFile("JWT_SECRET", "DB_URL", "DB_USERNAME", "DB_PASSWORD");
        JWT_SECRET = env.get("JWT_SECRET");
        if (JWT_SECRET == null || JWT_SECRET.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is missing! Did you forget to update your .env file?");
        }
        // Sweep the DB clean before we start so the tests are repeatable.
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }

    /**
     * Helper to craft a signed JWT review token on the fly. 
     * Very handy for testing expiration and various ride combinations.
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

    // Standard valid token with plenty of time left.
    private String generateValidToken() {
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        return generateReviewToken(E2E_RIDE_ID, E2E_PASSENGER_ID, E2E_DRIVER_ID, threeDaysMs);
    }

    // Token that literally just expired.
    private String generateExpiredToken() {
        return generateReviewToken(E2E_RIDE_ID, E2E_PASSENGER_ID, E2E_DRIVER_ID, -1000);
    }

    // Token for a ride that's already been polished off with a review.
    private String generateAlreadyReviewedToken() {
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        return generateReviewToken(REVIEWED_RIDE_ID, REVIEWED_PASSENGER_ID, REVIEWED_DRIVER_ID, threeDaysMs);
    }

    // Token for a ride that ended more than 3 days ago (business rule violation).
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

        // Check if the form actually loaded for our valid token.
        assertTrue(reviewPage.isReviewFormVisible(),
                "The form should be visible since the token is valid.");

        assertEquals("Rate Your Ride", reviewPage.getPageHeadingText());

        // Fill out the ratings and leave a nice comment.
        reviewPage.setDriverRating(5);
        reviewPage.setVehicleRating(4);
        reviewPage.enterComment("Great service!");

        reviewPage.clickSubmit();

        // Verify the success screen appears.
        assertTrue(reviewPage.isSuccessMessageVisible(),
                "We should be looking at the 'Thank You' screen now.");
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

        // Stars only, no text.
        reviewPage.setDriverRating(3);
        reviewPage.setVehicleRating(3);

        // This should be allowed.
        assertTrue(reviewPage.isSubmitButtonEnabled(),
                "Submit should be clickable since text comments are totally optional.");
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
        assertEquals("", reviewPage.getCommentText(), "Text area should be empty on load.");
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

        // No ratings should mean no submission.
        assertFalse(reviewPage.isSubmitButtonEnabled(),
                "Submit should be locked until ratings are provided.");

        // And we should see a 'please rate' warning.
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
                "Should still be locked without a vehicle rating.");
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
                "Should still be locked without a driver rating.");
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
                "Both sets of stars are in, so the button should wake up.");
    }

    @Test
    @Order(8)
    @DisplayName("Negative: No token in URL redirects to 404 page")
    void testNoTokenRedirectsTo404() {
        driver.get(BASE_URL + "/review");

        // The auth guard should catch this and kick us to 404.
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
        assertEquals("Link Expired", reviewPage.getTokenExpiredHeadingText());
    }

    @Test
    @Order(10)
    @DisplayName("Negative: Link Expired shown for malformed token")
    void testMalformedTokenShowsExpiredState() {
        driver.get(BASE_URL + "/review?token=completely_invalid_garbage");

        ReviewPage reviewPage = new ReviewPage(driver);
        reviewPage.waitForPageToLoad();

        assertTrue(reviewPage.isTokenExpiredVisible());
        assertEquals("Link Expired", reviewPage.getTokenExpiredHeadingText());
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

        // Backend should reject this as a duplicate.
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

        // Blast it with text and ensure it doesn't overflow.
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

        // Even if the token is valid, the business logic should block this.
        assertTrue(reviewPage.isTokenExpiredVisible());
        assertEquals("Link Expired", reviewPage.getTokenExpiredHeadingText());
    }

    @AfterAll
    static void cleanupAfterAll() throws Exception {
        // Leave the DB clean for the next test run.
        Map<String, String> env = E2ETestUtils.loadEnvFile("DB_URL", "DB_USERNAME", "DB_PASSWORD");
        E2ETestUtils.cleanupReviewsForRide(env, E2E_RIDE_ID);
    }
}
