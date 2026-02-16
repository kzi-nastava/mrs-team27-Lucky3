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
 * This page object handles the review page which passengers use to rate their rides.
 * It uses Page Factory to find elements and explicit waits to interact with them.
 */
public class ReviewPage {

    private WebDriver driver;
    private WebDriverWait wait;

    // Loading and status sections
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

    // Star rating buttons for driver and vehicle
    @FindBy(css = "[data-testid='driver-stars'] button")
    private List<WebElement> driverStars;

    @FindBy(css = "[data-testid='vehicle-stars'] button")
    private List<WebElement> vehicleStars;

    // Comment input and the submit button
    @FindBy(css = "[data-testid='comment-input']")
    private WebElement commentTextarea;

    @FindBy(css = "[data-testid='submit-button']")
    private WebElement submitButton;

    // Cancel button (X icon + "Cancel" text) in the review form
    @FindBy(xpath = "//button[contains(text(), 'Cancel')]")
    private WebElement cancelButton;

    public ReviewPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    /**
     * Just waits until the loading spinner is gone so we know the page is ready.
     */
    public void waitForPageToLoad() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("[data-testid='loading-spinner']")));
    }

    /**
     * Sets the rating for the driver by clicking the corresponding star.
     */
    public void setDriverRating(int rating) {
        wait.until(ExpectedConditions.visibilityOfAllElements(driverStars));
        driverStars.get(rating - 1).click();
    }

    /**
     * Sets the rating for the vehicle by clicking the corresponding star.
     */
    public void setVehicleRating(int rating) {
        wait.until(ExpectedConditions.visibilityOfAllElements(vehicleStars));
        vehicleStars.get(rating - 1).click();
    }

    /**
     * Types a comment into the textarea.
     */
    public void enterComment(String text) {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        commentTextarea.clear();
        commentTextarea.sendKeys(text);
    }

    /**
     * Gets whatever text is currently in the comment textarea.
     */
    public String getCommentText() {
        wait.until(ExpectedConditions.visibilityOf(commentTextarea));
        return commentTextarea.getAttribute("value");
    }

    /**
     * Clicks the submit button once it's clickable.
     */
    public void clickSubmit() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();
    }

    /**
     * Clicks the cancel button to go back without submitting.
     */
    public void clickCancel() {
        wait.until(ExpectedConditions.elementToBeClickable(cancelButton));
        cancelButton.click();
    }

    /**
     * Checks if the submit button is enabled.
     */
    public boolean isSubmitButtonEnabled() {
        wait.until(ExpectedConditions.visibilityOf(submitButton));
        return submitButton.isEnabled();
    }

    /**
     * Checks if the success message appeared.
     */
    public boolean isSuccessMessageVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='success-message']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the success heading text, usually "Thank You!".
     */
    public String getSuccessHeadingText() {
        wait.until(ExpectedConditions.visibilityOf(successMessageContainer));
        return successMessageContainer.findElement(By.tagName("h1")).getText();
    }

    /**
     * Checks if the review form is actually showing on the screen.
     */
    public boolean isReviewFormVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='review-form']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks for the "Link Expired" view.
     */
    public boolean isTokenExpiredVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='token-expired']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks for the "Invalid Link" view.
     */
    public boolean isTokenInvalidVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='token-invalid']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the validation message text.
     */
    public String getValidationMessageText() {
        wait.until(ExpectedConditions.visibilityOf(validationMessage));
        return validationMessage.getText();
    }

    /**
     * Checks if the validation warning is visible.
     */
    public boolean isValidationMessageVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='validation-message']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the error message text if something went wrong during submission.
     */
    public String getErrorMessageText() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("[data-testid='error-message']")));
        return errorMessage.getText();
    }

    /**
     * Gets the main heading text of the page.
     */
    public String getPageHeadingText() {
        WebElement heading = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.tagName("h1")));
        return heading.getText();
    }
}
