package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Represents the passenger ride history view at /passenger/ride-history.
 * It takes care of scrolling through the table, spotting rides you can
 * actually review, and handling the detail panel.
 */
public class PassengerRideHistoryPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String URL = "http://localhost:4200/passenger/ride-history";

    // CSS selectors stored as constants for dynamic element lookups
    private static final String TABLE_SELECTOR = "[data-testid='ride-history-table']";
    private static final String ROW_SELECTOR = TABLE_SELECTOR + " tbody tr";
    private static final String REVIEWABLE_ROW_SELECTOR = ROW_SELECTOR + ".reviewable-row";
    private static final By TABLE_LOCATOR = By.cssSelector(TABLE_SELECTOR);
    private static final By ROW_LOCATOR = By.cssSelector(ROW_SELECTOR);
    private static final By REVIEWABLE_ROW_LOCATOR = By.cssSelector(REVIEWABLE_ROW_SELECTOR);

    // ---- Static page elements via @FindBy ----

    @FindBy(css = "[data-testid='ride-history-table']")
    private WebElement rideHistoryTable;

    @FindBy(css = "[data-testid='status-filter']")
    private WebElement statusFilterSelect;

    @FindBy(css = "[data-testid='ride-detail-panel']")
    private WebElement rideDetailPanel;

    @FindBy(css = "[data-testid='ride-detail-heading']")
    private WebElement rideDetailHeading;

    @FindBy(css = "[data-testid='ride-details-close-button']")
    private WebElement closeDetailsButton;

    @FindBy(css = "[data-testid='leave-review-button']")
    private WebElement leaveReviewButton;

    @FindBy(css = "[data-testid='no-rides-message']")
    private WebElement noRidesMessage;

    public PassengerRideHistoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    // Jump straight to the history page and wait for the rows to show up.
    public void navigateTo() {
        driver.get(URL);
        waitForTableToLoad();
    }

    // Makes sure the table is visible and has at least one row rendered.
    public void waitForTableToLoad() {
        wait.until(ExpectedConditions.visibilityOf(rideHistoryTable));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(ROW_LOCATOR));
    }

    // Grabs every single ride row currently in the table.
    public List<WebElement> getRideRows() {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(ROW_LOCATOR));
        return driver.findElements(ROW_LOCATOR);
    }


    //  Filters for rows that have the 'reviewable-row' highlight.
    //  These are the ones where the passenger is allowed to leave a review.
    public List<WebElement> getReviewableRideRows() {
        return driver.findElements(REVIEWABLE_ROW_LOCATOR);
    }

    // Quick check to see if there's anything selectable for review right now.
    public boolean hasReviewableRides() {
        return !getReviewableRideRows().isEmpty();
    }

    // Clicks the first highlighted ride in the list. Throws an error if none are found.
    public void clickFirstReviewableRide() {
        wait.until(ExpectedConditions.presenceOfElementLocated(REVIEWABLE_ROW_LOCATOR));
        List<WebElement> reviewable = getReviewableRideRows();
        if (reviewable.isEmpty()) {
            throw new IllegalStateException("Couldn't find any highlighted rides to review.");
        }
        reviewable.get(0).click();
    }

    // Selects a ride by its position in the list (0 is the top one).
    public void clickRideRow(int index) {
        List<WebElement> rows = getRideRows();
        if (index >= rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Tried to click ride at index " + index + " but only have " + rows.size() + " rows.");
        }
        rows.get(index).click();
    }

    // Checks if the ride detail panel is visible.
    public boolean isDetailPanelVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOf(rideDetailPanel));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Blocks until the ride detail panel is fully rendered.
    public void waitForDetailPanel() {
        wait.until(ExpectedConditions.visibilityOf(rideDetailHeading));
    }


    //  Checks if the button to start a review is present in the details view.
    //  We use a snappy timeout here because we often check this when we expect it to be gone.
    public boolean isLeaveReviewButtonVisible() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            shortWait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='leave-review-button']")));
            return leaveReviewButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Hits the 'Leave a Review' button. This should take the user to the review form.
    public void clickLeaveReview() {
        wait.until(ExpectedConditions.elementToBeClickable(leaveReviewButton));
        leaveReviewButton.click();
    }

    /**
     * Switches the status filter using the dropdown. Since it's a standard HTML select,
     * we use the Selenium Select helper. We wait for the table to refresh afterwards.
     */
    public void clickStatusFilter(String status) {
        wait.until(ExpectedConditions.visibilityOf(statusFilterSelect));
        Select select = new Select(statusFilterSelect);
        for (WebElement option : select.getOptions()) {
            if (option.getText().trim().equalsIgnoreCase(status)
                    || option.getText().trim().toLowerCase().contains(status.toLowerCase())) {
                select.selectByVisibleText(option.getText().trim());
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(ROW_LOCATOR));
                return;
            }
        }
        throw new IllegalArgumentException("Couldn't find a filter option matching: " + status);
    }

    // Tries to close the side panel if it's there.
    public void closeDetails() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(closeDetailsButton));
            closeDetailsButton.click();
        } catch (Exception e) {
            // Probably already closed, no big deal.
        }
    }

    /**
     * Counts the actual rides in the table.
     * Returns 0 if we see the 'No rides found' placeholder instead of data.
     */
    public int getRideCount() {
        List<WebElement> rows = getRideRows();
        if (rows.size() == 1) {
            String text = rows.get(0).getText().trim();
            if (text.contains("No rides found")) return 0;
        }
        return rows.size();
    }

    // Checks if a specific row has the blue 'reviewable' highlight.
    public boolean isRideRowReviewable(int index) {
        List<WebElement> rows = getRideRows();
        if (index >= rows.size()) return false;
        String classes = rows.get(index).getAttribute("class");
        return classes != null && classes.contains("reviewable-row");
    }

    // Waits for the browser to finish navigating to the review form.
    public void waitForReviewPageNavigation() {
        wait.until(ExpectedConditions.urlContains("/review"));
    }
}
