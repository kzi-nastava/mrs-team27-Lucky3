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
 * Page object for the passenger ride history page at /passenger/ride-history.
 * Handles ride table interactions, reviewable ride detection, and the "Leave a Review" button.
 */
public class PassengerRideHistoryPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String URL = "http://localhost:4200/passenger/ride-history";

    // Status filter buttons
    @FindBy(css = ".flex.gap-2.bg-gray-900 button")
    private List<WebElement> statusFilterButtons;

    // Table rows in the rides table
    @FindBy(css = "app-rides-table table tbody tr")
    private List<WebElement> rideRows;

    // Selected ride detail panel header
    @FindBy(css = "h3.text-lg.font-semibold")
    private WebElement rideDetailsHeading;

    // "Leave a Review" button in the selected ride detail panel
    @FindBy(xpath = "//button[contains(text(), 'Leave a Review')]")
    private WebElement leaveReviewButton;

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
     * Waits until the ride table is visible and has loaded.
     */
    public void waitForTableToLoad() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("app-rides-table table")));
        // Give a moment for rows to render
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("app-rides-table table tbody tr")));
    }

    /**
     * Returns all ride rows in the table.
     */
    public List<WebElement> getRideRows() {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("app-rides-table table tbody tr")));
        return driver.findElements(By.cssSelector("app-rides-table table tbody tr"));
    }

    /**
     * Returns only the reviewable ride rows (those with the blue box-shadow styling).
     */
    public List<WebElement> getReviewableRideRows() {
        return driver.findElements(By.cssSelector("app-rides-table table tbody tr.reviewable-row"));
    }

    /**
     * Checks if there are any reviewable rides in the table.
     */
    public boolean hasReviewableRides() {
        List<WebElement> reviewable = getReviewableRideRows();
        return !reviewable.isEmpty();
    }

    /**
     * Clicks on the first reviewable ride row in the table.
     */
    public void clickFirstReviewableRide() {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("app-rides-table table tbody tr.reviewable-row")));
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
            throw new IndexOutOfBoundsException("Ride row index " + index + " is out of bounds (total: " + rows.size() + ")");
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
     * Clicks a status filter button by its label text (e.g., "all", "Finished", "Cancelled").
     */
    public void clickStatusFilter(String status) {
        for (WebElement btn : statusFilterButtons) {
            if (btn.getText().trim().equalsIgnoreCase(status)) {
                wait.until(ExpectedConditions.elementToBeClickable(btn));
                btn.click();
                // Wait a moment for the filter to apply
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                return;
            }
        }
        throw new IllegalArgumentException("Status filter button not found: " + status);
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
     */
    public int getRideCount() {
        List<WebElement> rows = getRideRows();
        // If there's a single row with "No rides found", return 0
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
