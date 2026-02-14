package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class AdminRideHistoryPage {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String URL = "http://localhost:4200/admin/ride-history";

    // ----- Search Controls -----

    @FindBy(id = "search-type-driver")
    private WebElement searchTypeDriverBtn;

    @FindBy(id = "search-type-passenger")
    private WebElement searchTypePassengerBtn;

    @FindBy(id = "search-id-input")
    private WebElement searchIdInput;

    @FindBy(id = "search-btn")
    private WebElement searchBtn;

    @FindBy(id = "clear-search-btn")
    private WebElement clearSearchBtn;

    // ----- Filters -----

    @FindBy(id = "date-filter-input")
    private WebElement dateFilterInput;

    @FindBy(id = "clear-date-btn")
    private WebElement clearDateBtn;

    @FindBy(id = "status-filter")
    private WebElement statusFilterSelect;

    // ----- Sort Headers -----

    @FindBy(id = "sort-start-time")
    private WebElement sortStartTimeBtn;

    @FindBy(id = "sort-end-time")
    private WebElement sortEndTimeBtn;

    @FindBy(id = "sort-route")
    private WebElement sortRouteBtn;

    @FindBy(id = "sort-status")
    private WebElement sortStatusBtn;

    @FindBy(id = "sort-cost")
    private WebElement sortCostBtn;

    @FindBy(id = "sort-cancelled-by")
    private WebElement sortCancelledByBtn;

    @FindBy(id = "sort-panic")
    private WebElement sortPanicBtn;

    // ----- Table -----

    @FindBy(id = "rides-table")
    private WebElement ridesTable;

    @FindBy(id = "no-rides-message")
    private WebElement noRidesMessage;

    // ----- Ride Count -----

    @FindBy(id = "all-rides-count")
    private WebElement allRidesCount;

    @FindBy(id = "rides-count")
    private WebElement ridesCount;

    public AdminRideHistoryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    public void navigateTo() {
        driver.get(URL);
        waitForTableToLoad();
    }

    // ========== Wait Helpers ==========

    public void waitForTableToLoad() {
        wait.until(ExpectedConditions.visibilityOf(ridesTable));
        // Short pause for data to populate via API
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    private void waitForDataRefresh() {
        // Wait after an action that triggers loadRides()
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    // ========== Search Actions ==========

    public void selectSearchTypeDriver() {
        wait.until(ExpectedConditions.elementToBeClickable(searchTypeDriverBtn));
        searchTypeDriverBtn.click();
    }

    public void selectSearchTypePassenger() {
        wait.until(ExpectedConditions.elementToBeClickable(searchTypePassengerBtn));
        searchTypePassengerBtn.click();
    }

    public void enterSearchId(String id) {
        wait.until(ExpectedConditions.visibilityOf(searchIdInput));
        searchIdInput.clear();
        searchIdInput.sendKeys(id);
    }

    public void clearSearchId() {
        wait.until(ExpectedConditions.visibilityOf(searchIdInput));
        searchIdInput.clear();
    }

    public void clickSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(searchBtn));
        searchBtn.click();
        waitForDataRefresh();
    }

    public void clickClearSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(clearSearchBtn));
        clearSearchBtn.click();
        waitForDataRefresh();
    }

    public void searchByDriverId(String driverId) {
        selectSearchTypeDriver();
        enterSearchId(driverId);
        clickSearch();
    }

    public void searchByPassengerId(String passengerId) {
        selectSearchTypePassenger();
        enterSearchId(passengerId);
        clickSearch();
    }

    // ========== Filter Actions ==========

    public void setDateFilter(String date) {
        wait.until(ExpectedConditions.visibilityOf(dateFilterInput));
        // Clear existing value using keyboard
        dateFilterInput.click();
        dateFilterInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        dateFilterInput.sendKeys(date);
        dateFilterInput.sendKeys(Keys.TAB); // trigger change event
        waitForDataRefresh();
    }

    public void clearDateFilter() {
        wait.until(ExpectedConditions.elementToBeClickable(clearDateBtn));
        clearDateBtn.click();
        waitForDataRefresh();
    }

    public void selectStatus(String status) {
        wait.until(ExpectedConditions.visibilityOf(statusFilterSelect));
        Select select = new Select(statusFilterSelect);
        select.selectByValue(status);
        waitForDataRefresh();
    }

    public String getSelectedStatus() {
        wait.until(ExpectedConditions.visibilityOf(statusFilterSelect));
        Select select = new Select(statusFilterSelect);
        return select.getFirstSelectedOption().getAttribute("value");
    }

    // ========== Sort Actions ==========

    public void sortByStartTime() {
        wait.until(ExpectedConditions.elementToBeClickable(sortStartTimeBtn));
        sortStartTimeBtn.click();
        waitForDataRefresh();
    }

    public void sortByEndTime() {
        wait.until(ExpectedConditions.elementToBeClickable(sortEndTimeBtn));
        sortEndTimeBtn.click();
        waitForDataRefresh();
    }

    public void sortByRoute() {
        wait.until(ExpectedConditions.elementToBeClickable(sortRouteBtn));
        sortRouteBtn.click();
        waitForDataRefresh();
    }

    public void sortByStatus() {
        wait.until(ExpectedConditions.elementToBeClickable(sortStatusBtn));
        sortStatusBtn.click();
        waitForDataRefresh();
    }

    public void sortByCost() {
        wait.until(ExpectedConditions.elementToBeClickable(sortCostBtn));
        sortCostBtn.click();
        waitForDataRefresh();
    }

    public void sortByCancelledBy() {
        wait.until(ExpectedConditions.elementToBeClickable(sortCancelledByBtn));
        sortCancelledByBtn.click();
        waitForDataRefresh();
    }

    public void sortByPanic() {
        wait.until(ExpectedConditions.elementToBeClickable(sortPanicBtn));
        sortPanicBtn.click();
        waitForDataRefresh();
    }

    // ========== Table Data Extraction ==========

    public List<WebElement> getRideRows() {
        return driver.findElements(By.cssSelector("#rides-table tbody tr[id^='ride-row-']"));
    }

    public int getRideCount() {
        return getRideRows().size();
    }

    public boolean isNoRidesMessageDisplayed() {
        try {
            return driver.findElement(By.id("no-rides-message")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getNoRidesMessageText() {
        return driver.findElement(By.id("no-rides-message")).getText().trim();
    }

    /**
     * Get ride row by index (0-based).
     */
    public WebElement getRideRow(int index) {
        return driver.findElement(By.id("ride-row-" + index));
    }

    /**
     * Get the displayed start date text of a ride row.
     */
    public String getRideStartDate(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-start-date")).getText().trim();
    }

    /**
     * Get the displayed start time text of a ride row.
     */
    public String getRideStartTime(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-start-time")).getText().trim();
    }

    /**
     * Get the status text of a ride row.
     */
    public String getRideStatus(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-status")).getText().trim();
    }

    /**
     * Get the cost text of a ride row (e.g., "$300.00").
     */
    public String getRideCost(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-cost")).getText().trim();
    }

    /**
     * Parse cost value as double from a ride row.
     */
    public double getRideCostValue(int rowIndex) {
        String costText = getRideCost(rowIndex);
        return Double.parseDouble(costText.replace("$", "").replace(",", ""));
    }

    /**
     * Get the cancelled-by text of a ride row.
     */
    public String getRideCancelledBy(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-cancelled-by")).getText().trim();
    }

    /**
     * Get the panic text of a ride row.
     */
    public String getRidePanic(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-panic")).getText().trim();
    }

    /**
     * Get the pickup address of a ride row.
     */
    public String getRidePickup(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-pickup")).getText().trim();
    }

    /**
     * Get the destination address of a ride row.
     */
    public String getRideDestination(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-destination")).getText().trim();
    }

    /**
     * Get all statuses from all visible ride rows.
     */
    public List<String> getAllRideStatuses() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-status")).getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * Get all cost values from all visible ride rows.
     */
    public List<Double> getAllRideCostValues() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> {
                    String costText = row.findElement(By.cssSelector(".ride-cost")).getText().trim();
                    return Double.parseDouble(costText.replace("$", "").replace(",", ""));
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all panic texts from all visible ride rows.
     */
    public List<String> getAllRidePanics() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-panic")).getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * Get all cancelled-by texts from all visible ride rows.
     */
    public List<String> getAllRideCancelledBys() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-cancelled-by")).getText().trim())
                .collect(Collectors.toList());
    }

    /**
     * Click on a ride row to open details.
     */
    public void clickRideRow(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        wait.until(ExpectedConditions.elementToBeClickable(row));
        row.click();
    }

    /**
     * Get the rides count text displayed (e.g., "9 rides found").
     */
    public String getAllRidesCountText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(allRidesCount));
            return allRidesCount.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get the rides count text for search results (e.g., "3 rides found").
     */
    public String getSearchRidesCountText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(ridesCount));
            return ridesCount.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if the sort indicator for a given direction is active on start time.
     */
    public String getSortIndicatorText(String sortButtonId) {
        WebElement button = driver.findElement(By.id(sortButtonId));
        List<WebElement> indicators = button.findElements(By.cssSelector("span.text-yellow-500"));
        if (indicators.isEmpty()) return "";
        return indicators.get(0).getText().trim();
    }

    /**
     * Check if the driver search type button is active (selected).
     */
    public boolean isDriverSearchTypeActive() {
        return searchTypeDriverBtn.getAttribute("class").contains("bg-yellow-500");
    }

    /**
     * Check if the passenger search type button is active (selected).
     */
    public boolean isPassengerSearchTypeActive() {
        return searchTypePassengerBtn.getAttribute("class").contains("bg-yellow-500");
    }

    /**
     * Get the search ID input value.
     */
    public String getSearchIdValue() {
        return searchIdInput.getAttribute("value");
    }

    /**
     * Get the date filter input value.
     */
    public String getDateFilterValue() {
        return dateFilterInput.getAttribute("value");
    }

    /**
     * Check if the clear date button is visible.
     */
    public boolean isClearDateButtonVisible() {
        try {
            return clearDateBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the clear search button is visible.
     */
    public boolean isClearSearchButtonVisible() {
        try {
            return clearSearchBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all start date strings from the table.
     */
    public List<String> getAllRideStartDates() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-start-date")).getText().trim())
                .collect(Collectors.toList());
    }
}
