package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page object for the review page where passengers rate their rides.
 * All elements use {@code @FindBy} with {@code data-testid} selectors
 * matching the Angular template.
 */
public class ReviewPage {

    private WebDriver driver;
    private WebDriverWait wait;

    // Locator constant for the spinner (used in invisibility check)
    private static final By LOADING_SPINNER_LOCATOR =
            By.cssSelector("[data-testid='loading-spinner']");

    // ---- Status / state sections ----

    @FindBy(css = "[data-testid='loading-spinner']")
    private WebElement loadingSpinner;

    @FindBy(css = "[data-testid='token-expired']")
    private WebElement tokenExpiredContainer;

    @FindBy(css = "[data-testid='token-invalid']")
    private WebElement tokenInvalidContainer;

    @FindBy(css = "[data-testid='success-message']")
    private WebElement successMessageContainer;

    @FindBy(css = "[data-testid='review-form']")
    private WebElement reviewForm;

    @FindBy(css = "[data-testid='error-message']")
    private WebElement errorMessage;

    @FindBy(css = "[data-testid='validation-message']")
    private WebElement validationMessage;

    // ---- State-specific headings ----

    @FindBy(css = "[data-testid='token-expired-heading']")
    private WebElement tokenExpiredHeading;

    @FindBy(css = "[data-testid='token-invalid-heading']")
    private WebElement tokenInvalidHeading;

    @FindBy(css = "[data-testid='success-heading']")
    private WebElement successHeading;

    // ---- Form heading and subtitle ----

    @FindBy(css = "[data-testid='review-heading']")
    private WebElement pageHeading;

    @FindBy(css = "[data-testid='review-subtitle']")
    private WebElement pageSubtitle;

    // ---- Star rating buttons ----

    @FindBy(css = "[data-testid='driver-stars'] button")
    private List<WebElement> driverStars;

    @FindBy(css = "[data-testid='vehicle-stars'] button")
    private List<WebElement> vehicleStars;

    // ---- Star rating feedback text ----

    @FindBy(css = "[data-testid='driver-rating-feedback']")
    private WebElement driverRatingFeedback;

    @FindBy(css = "[data-testid='vehicle-rating-feedback']")
    private WebElement vehicleRatingFeedback;

    // ---- Comment, counter, and action buttons ----

    @FindBy(css = "[data-testid='comment-input']")
    private WebElement commentTextarea;

    @FindBy(css = "[data-testid='char-counter']")
    private WebElement charCounter;

    @FindBy(css = "[data-testid='submit-button']")
    private WebElement submitButton;

    @FindBy(css = "[data-testid='cancel-button']")
    private WebElement cancelButton;

    public ReviewPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    // ========== Wait helpers ==========

    // Waits until the loading spinner is gone so we know the page is ready.
    public void waitForPageToLoad() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_SPINNER_LOCATOR));
    }

    // ========== Star ratings ==========

    // Sets the rating for the driver by clicking the corresponding star.
    public void setDriverRating(int rating) {
        wait.until(ExpectedConditions.visibilityOfAllElements(driverStars));
        driverStars.get(rating - 1).click();
    }

    // Sets the rating for the vehicle by clicking the corresponding star.
    public void setVehicleRating(int rating) {
        wait.until(ExpectedConditions.visibilityOfAllElements(vehicleStars));
        vehicleStars.get(rating - 1).click();
    }

    // Gets the feedback text below the driver stars (e.g., "Click to rate" or "4 of 5 stars").
    public String getDriverRatingFeedbackText() {
        wait.until(ExpectedConditions.visibilityOf(driverRatingFeedback));
        return driverRatingFeedback.getText();
    }

    // Gets the feedback text below the vehicle stars (e.g., "Click to rate" or "3 of 5 stars").
    public String getVehicleRatingFeedbackText() {
        wait.until(ExpectedConditions.visibilityOf(vehicleRatingFeedback));
        return vehicleRatingFeedback.getText();
    }

    // ========== Comment ==========

    // Types a comment into the textarea.
    public void enterComment(String text) {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        commentTextarea.clear();
        commentTextarea.sendKeys(text);
    }

    // Gets whatever text is currently in the comment textarea.
    public String getCommentText() {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        return commentTextarea.getAttribute("value");
    }

    // Gets the visible character counter text (e.g., "0/500").
    public String getCharacterCounterText() {
        wait.until(ExpectedConditions.visibilityOf(charCounter));
        return charCounter.getText();
    }

    // ========== Action buttons ==========

    // Clicks the submit button once it's clickable.
    public void clickSubmit() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();
    }

    // Clicks the cancel button to go back without submitting.
    public void clickCancel() {
        wait.until(ExpectedConditions.elementToBeClickable(cancelButton));
        cancelButton.click();
    }

    // Checks if the submit button is enabled.
    public boolean isSubmitButtonEnabled() {
        wait.until(ExpectedConditions.visibilityOf(submitButton));
        return submitButton.isEnabled();
    }

    // ========== Page headings ==========

    // Gets the main heading text ("Rate Your Ride").
    public String getPageHeadingText() {
        wait.until(ExpectedConditions.visibilityOf(pageHeading));
        return pageHeading.getText();
    }

    // Gets the subtitle text ("How was your experience?").
    public String getPageSubtitleText() {
        wait.until(ExpectedConditions.visibilityOf(pageSubtitle));
        return pageSubtitle.getText();
    }

    // ========== State / visibility checks ==========

    // Checks if the review form is visible.
    public boolean isReviewFormVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(reviewForm));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Checks if the success message appeared.
    public boolean isSuccessMessageVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(successMessageContainer));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Gets the success heading text ("Thank You!").
    public String getSuccessHeadingText() {
        wait.until(ExpectedConditions.visibilityOf(successHeading));
        return successHeading.getText();
    }

    // Gets the success description paragraph text.
    public String getSuccessDescriptionText() {
        wait.until(ExpectedConditions.visibilityOf(successMessageContainer));
        return successMessageContainer.findElement(By.tagName("p")).getText();
    }

    // Gets the heading text from the "Link Expired" view.
    public String getTokenExpiredHeadingText() {
        wait.until(ExpectedConditions.visibilityOf(tokenExpiredHeading));
        return tokenExpiredHeading.getText();
    }

    // Gets the heading text from the "Invalid Link" view.
    public String getTokenInvalidHeadingText() {
        wait.until(ExpectedConditions.visibilityOf(tokenInvalidHeading));
        return tokenInvalidHeading.getText();
    }

    // Checks for the "Link Expired" view.
    public boolean isTokenExpiredVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(tokenExpiredContainer));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Checks for the "Invalid Link" view.
    public boolean isTokenInvalidVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(tokenInvalidContainer));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Returns the validation message text.
    public String getValidationMessageText() {
        wait.until(ExpectedConditions.visibilityOf(validationMessage));
        return validationMessage.getText();
    }

    // Checks if the validation warning is visible.
    public boolean isValidationMessageVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(validationMessage));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Returns the error message text.
    public String getErrorMessageText() {
        wait.until(ExpectedConditions.visibilityOf(errorMessage));
        return errorMessage.getText();
    }
}
