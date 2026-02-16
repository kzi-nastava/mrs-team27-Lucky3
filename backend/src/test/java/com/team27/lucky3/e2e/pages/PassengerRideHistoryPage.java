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
 * Page object for the passenger ride history page at /passenger/ride-history.
 * Handles ride table interactions, reviewable ride detection, and the "Leave a Review" button.
 *
 * Note: The ride-history page uses an inline table (not the shared app-rides-table component),
 * so selectors target the table via data-testid="ride-history-table".
 */
public class PassengerRideHistoryPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String URL = "http://localhost:4200/passenger/ride-history";
    private static final String TABLE_SELECTOR = "[data-testid='ride-history-table']";
    private static final String ROW_SELECTOR = TABLE_SELECTOR + " tbody tr";
    private static final String REVIEWABLE_ROW_SELECTOR = ROW_SELECTOR + ".reviewable-row";

    // Status filter dropdown
    @FindBy(css = "[data-testid='status-filter']")
    private WebElement statusFilterSelect;

    // Close details button
    @FindBy(css = "[data-testid='ride-details-close-button']")
    private WebElement closeDetailsButton;

    public PassengerRideHistoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(driver, this);
    }

    /**
     * Navigates directly to the ride history page.
     */
    public void navigateTo() {
        driver.get(URL);
        waitForTableToLoad();
    }

    /**
     * Waits until the ride table is visible and has at least one row rendered.
     */
    public void waitForTableToLoad() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(TABLE_SELECTOR)));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(ROW_SELECTOR)));
    }

    /**
     * Returns all ride rows in the table.
     */
    public List<WebElement> getRideRows() {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(ROW_SELECTOR)));
        return driver.findElements(By.cssSelector(ROW_SELECTOR));
    }

    /**
     * Returns only the reviewable ride rows (those with the reviewable-row CSS class).
     */
    public List<WebElement> getReviewableRideRows() {
        return driver.findElements(By.cssSelector(REVIEWABLE_ROW_SELECTOR));
    }

    /**
     * Checks if there are any reviewable rides in the table.
     */
    public boolean hasReviewableRides() {
        return !getReviewableRideRows().isEmpty();
    }

    /**
     * Clicks on the first reviewable ride row in the table.
     */
    public void clickFirstReviewableRide() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(REVIEWABLE_ROW_SELECTOR)));
        List<WebElement> reviewable = getReviewableRideRows();
        if (reviewable.isEmpty()) {
            throw new IllegalStateException("No reviewable rides found in the table.");
        }
        reviewable.get(0).click();
    }

    /**
     * Clicks on a specific ride row by index (0-based).
     */
    public void clickRideRow(int index) {
        List<WebElement> rows = getRideRows();
        if (index >= rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Ride row index " + index + " is out of bounds (total: " + rows.size() + ")");
        }
        rows.get(index).click();
    }

    /**
     * Checks if the ride detail panel is currently visible.
     */
    public boolean isDetailPanelVisible() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h3[contains(text(), 'Ride Details')]")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Waits for the ride detail panel to appear after clicking a ride.
     */
    public void waitForDetailPanel() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[contains(text(), 'Ride Details')]")));
    }

    /**
     * Checks if the "Leave a Review" button is visible in the detail panel.
     * Uses a shorter timeout (3s) to avoid long waits when the button is expected to be absent.
     */
    public boolean isLeaveReviewButtonVisible() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement btn = shortWait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//button[contains(text(), 'Leave a Review')]")));
            return btn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clicks the "Leave a Review" button in the ride detail panel.
     * This navigates to /review?rideId=<id> in authenticated mode.
     */
    public void clickLeaveReview() {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(), 'Leave a Review')]")));
        btn.click();
    }

    /**
     * Selects a status filter option by its visible text (e.g., "All Statuses", "Finished").
     * The ride-history page uses a &lt;select&gt; dropdown, not buttons.
     */
    public void clickStatusFilter(String status) {
        wait.until(ExpectedConditions.visibilityOf(statusFilterSelect));
        Select select = new Select(statusFilterSelect);
        // Try matching by visible text (case-insensitive partial match)
        for (WebElement option : select.getOptions()) {
            if (option.getText().trim().equalsIgnoreCase(status)
                    || option.getText().trim().toLowerCase().contains(status.toLowerCase())) {
                select.selectByVisibleText(option.getText().trim());
                // Wait for the table to reload after the filter change
                wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(ROW_SELECTOR)));
                return;
            }
        }
        throw new IllegalArgumentException("Status filter option not found: " + status);
    }

    /**
     * Closes the ride detail panel.
     */
    public void closeDetails() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(closeDetailsButton));
            closeDetailsButton.click();
        } catch (Exception e) {
            // Panel may not be open
        }
    }

    /**
     * Gets the number of ride rows in the table.
     * Returns 0 if the only row is the "No rides found" placeholder.
     */
    public int getRideCount() {
        List<WebElement> rows = getRideRows();
        if (rows.size() == 1) {
            String text = rows.get(0).getText().trim();
            if (text.contains("No rides found")) return 0;
        }
        return rows.size();
    }

    /**
     * Checks if a specific ride row is reviewable (has the reviewable-row class).
     */
    public boolean isRideRowReviewable(int index) {
        List<WebElement> rows = getRideRows();
        if (index >= rows.size()) return false;
        String classes = rows.get(index).getAttribute("class");
        return classes != null && classes.contains("reviewable-row");
    }

    /**
     * Waits for the URL to change to the review page.
     */
    public void waitForReviewPageNavigation() {
        wait.until(ExpectedConditions.urlContains("/review"));
    }
}
