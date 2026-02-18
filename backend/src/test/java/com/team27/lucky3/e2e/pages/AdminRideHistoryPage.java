package com.team27.lucky3.e2e.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
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

    @FindBy(id = "date-from-input")
    private WebElement dateFromInput;

    @FindBy(id = "date-to-input")
    private WebElement dateToInput;

    @FindBy(id = "clear-date-btn")
    private WebElement clearDateBtn;

    @FindBy(id = "status-filter")
    private WebElement statusFilterSelect;

    // ----- Pagination -----

    @FindBy(id = "page-first")
    private WebElement pageFirstBtn;

    @FindBy(id = "page-prev")
    private WebElement pagePrevBtn;

    @FindBy(id = "page-next")
    private WebElement pageNextBtn;

    @FindBy(id = "page-last")
    private WebElement pageLastBtn;

    @FindBy(id = "pagination-controls")
    private WebElement paginationControls;

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

    // ----- Wait Helpers -----

    private static final By RIDE_ROW_LOCATOR = By.cssSelector("#rides-table tbody tr[id^='ride-row-']");
    private static final By NO_RIDES_LOCATOR = By.id("no-rides-message");
    private static final By TABLE_LOCATOR = By.id("rides-table");

    public void waitForTableToLoad() {
        wait.until(ExpectedConditions.visibilityOf(ridesTable));
        waitForTableContent();
    }

    // Wait for ride rows or the "No rides found" message to be present
    private void waitForTableContent() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(RIDE_ROW_LOCATOR),
                ExpectedConditions.presenceOfElementLocated(NO_RIDES_LOCATOR)
        ));
    }


    private String getCurrentLoadId() {
        try {
            WebElement table = driver.findElement(TABLE_LOCATOR);
            return table.getAttribute("data-load-id");
        } catch (Exception e) {
            return null;
        }
    }


    private void waitForDataRefresh(String beforeLoadId) {
        // Wait for data-load-id to change (Angular increments it on each API response)
        if (beforeLoadId != null) {
            wait.until(d -> {
                String currentId = getCurrentLoadId();
                return currentId != null && !currentId.equals(beforeLoadId);
            });
        }

        // After load-id changed, wait for content to be rendered
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(RIDE_ROW_LOCATOR),
                ExpectedConditions.presenceOfElementLocated(NO_RIDES_LOCATOR)
        ));
    }

    // ----- Search Actions -----

    public void selectSearchTypeDriver() {
        searchTypeDriverBtn.click();
    }

    public void selectSearchTypePassenger() {
        searchTypePassengerBtn.click();
    }

    public void enterSearchId(String id) {
        searchIdInput.clear();
        searchIdInput.sendKeys(id);
    }

    public void clearSearchId() {
        searchIdInput.clear();
    }

    public void clickSearch() {
        String loadId = getCurrentLoadId();
        searchBtn.click();
        waitForDataRefresh(loadId);
    }

    public void clickClearSearch() {
        String loadId = getCurrentLoadId();
        clearSearchBtn.click();
        waitForDataRefresh(loadId);
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

    private void setDateInputValue(WebElement input, String date) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                input, date);
    }

    // ----- Filter Actions -----

    public void setDateFromFilter(String date) {
        String loadId = getCurrentLoadId();
        setDateInputValue(dateFromInput, date);
        waitForDataRefresh(loadId);
    }

    public void setDateToFilter(String date) {
        String loadId = getCurrentLoadId();
        setDateInputValue(dateToInput, date);
        waitForDataRefresh(loadId);
    }

    public void setDateRange(String from, String to) {
        setDateFromFilter(from);
        setDateToFilter(to);
    }

    public void setDateFilter(String date) {
        setDateFromFilter(date);
    }

    public void clearDateFilter() {
        String loadId = getCurrentLoadId();
        clearDateBtn.click();
        waitForDataRefresh(loadId);
    }

    public void selectStatus(String status) {
        String loadId = getCurrentLoadId();
        Select select = new Select(statusFilterSelect);
        select.selectByValue(status);
        waitForDataRefresh(loadId);
    }

    public String getSelectedStatus() {
        Select select = new Select(statusFilterSelect);
        return select.getFirstSelectedOption().getAttribute("value");
    }

    // ----- Pagination Actions -----

    private static final By PAGINATION_LOCATOR = By.id("pagination-controls");

    // Check if pagination controls are visible (rendered when totalPages > 1)
    public boolean isPaginationVisible() {
        try {
            return driver.findElement(PAGINATION_LOCATOR).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Click a specific page number button (0-based page index)
    public void goToPage(int pageIndex) {
        String loadId = getCurrentLoadId();
        WebElement btn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("page-btn-" + pageIndex)));
        btn.click();
        waitForDataRefresh(loadId);
    }

    public void clickNextPage() {
        String loadId = getCurrentLoadId();
        pageNextBtn.click();
        waitForDataRefresh(loadId);
    }

    public void clickPrevPage() {
        String loadId = getCurrentLoadId();
        pagePrevBtn.click();
        waitForDataRefresh(loadId);
    }

    public void clickFirstPage() {
        String loadId = getCurrentLoadId();
        pageFirstBtn.click();
        waitForDataRefresh(loadId);
    }

    public void clickLastPage() {
        String loadId = getCurrentLoadId();
        pageLastBtn.click();
        waitForDataRefresh(loadId);
    }

    // Check if the Next button is disabled
    public boolean isNextPageDisabled() {
        return "true".equals(pageNextBtn.getAttribute("disabled"));
    }

    // Check if the Prev button is disabled
    public boolean isPrevPageDisabled() {
        return "true".equals(pagePrevBtn.getAttribute("disabled"));
    }

    // Check if the First button is disabled
    public boolean isFirstPageDisabled() {
        return "true".equals(pageFirstBtn.getAttribute("disabled"));
    }

    // Check if the Last button is disabled
    public boolean isLastPageDisabled() {
        return "true".equals(pageLastBtn.getAttribute("disabled"));
    }

    // Get the active (highlighted) page number (1-based display text)
    public int getActivePageNumber() {
        List<WebElement> pageButtons = driver.findElements(By.cssSelector("#pagination-controls button[id^='page-btn-']"));
        for (WebElement btn : pageButtons) {
            if (btn.getAttribute("class").contains("bg-yellow-500")) {
                return Integer.parseInt(btn.getText().trim());
            }
        }
        return -1;
    }

    // Get the pagination info text (e.g., "Showing 1 – 5 of 11")
    public String getPaginationInfoText() {
        try {
            WebElement infoDiv = driver.findElement(By.cssSelector("#pagination-controls .text-sm"));
            return infoDiv.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // Get list of all visible page button numbers (1-based display text)
    public List<Integer> getVisiblePageNumbers() {
        List<WebElement> pageButtons = driver.findElements(By.cssSelector("#pagination-controls button[id^='page-btn-']"));
        List<Integer> pages = new ArrayList<>();
        for (WebElement btn : pageButtons) {
            pages.add(Integer.parseInt(btn.getText().trim()));
        }
        return pages;
    }

    // Check if a specific page button exists (0-based index)
    public boolean isPageButtonPresent(int pageIndex) {
        try {
            return driver.findElement(By.id("page-btn-" + pageIndex)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ----- Sort Actions -----

    public void sortByStartTime() {
        String loadId = getCurrentLoadId();
        sortStartTimeBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByEndTime() {
        String loadId = getCurrentLoadId();
        sortEndTimeBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByRoute() {
        String loadId = getCurrentLoadId();
        sortRouteBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByStatus() {
        String loadId = getCurrentLoadId();
        sortStatusBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByCost() {
        String loadId = getCurrentLoadId();
        sortCostBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByCancelledBy() {
        String loadId = getCurrentLoadId();
        sortCancelledByBtn.click();
        waitForDataRefresh(loadId);
    }

    public void sortByPanic() {
        String loadId = getCurrentLoadId();
        sortPanicBtn.click();
        waitForDataRefresh(loadId);
    }

    // ----- Table Data Extraction -----

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

    // Get ride row by index (0-based)
    public WebElement getRideRow(int index) {
        return driver.findElement(By.id("ride-row-" + index));
    }

    // Get the displayed start date text of a ride row
    public String getRideStartDate(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-start-date")).getText().trim();
    }

    // Get the displayed start time text of a ride row
    public String getRideStartTime(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-start-time")).getText().trim();
    }

    // Get the status text of a ride row
    public String getRideStatus(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-status")).getText().trim();
    }

    // Get the cost text of a ride row (e.g., "$300.00")
    public String getRideCost(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-cost")).getText().trim();
    }

    // Parse cost value as double from a ride row
    // Returns 0.0 for rides displaying "—" (no cost, e.g. cancelled rides)
    public double getRideCostValue(int rowIndex) {
        String costText = getRideCost(rowIndex);
        if (costText.equals("\u2014") || costText.equals("—")) return 0.0;
        return Double.parseDouble(costText.replace("$", "").replace(",", ""));
    }

    // Get the cancelled-by text of a ride row
    public String getRideCancelledBy(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-cancelled-by")).getText().trim();
    }

    // Get the panic text of a ride row
    public String getRidePanic(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-panic")).getText().trim();
    }

    // Get the pickup address of a ride row
    public String getRidePickup(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-pickup")).getText().trim();
    }

    // Get the destination address of a ride row
    public String getRideDestination(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        return row.findElement(By.cssSelector(".ride-destination")).getText().trim();
    }

    // Get all statuses from all visible ride rows
    public List<String> getAllRideStatuses() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-status")).getText().trim())
                .collect(Collectors.toList());
    }

    // Get all cost values from all visible ride rows.
    // Returns 0.0 for rides displaying "—" (no cost, e.g. cancelled rides)
    public List<Double> getAllRideCostValues() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> {
                    String costText = row.findElement(By.cssSelector(".ride-cost")).getText().trim();
                    if (costText.equals("\u2014") || costText.equals("—")) return 0.0;
                    return Double.parseDouble(costText.replace("$", "").replace(",", ""));
                })
                .collect(Collectors.toList());
    }

    // Get all panic texts from all visible ride rows
    public List<String> getAllRidePanics() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-panic")).getText().trim())
                .collect(Collectors.toList());
    }

    // Get all cancelled-by texts from all visible ride rows
    public List<String> getAllRideCancelledBys() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-cancelled-by")).getText().trim())
                .collect(Collectors.toList());
    }

    // Click on a ride row to open details
    public void clickRideRow(int rowIndex) {
        WebElement row = getRideRow(rowIndex);
        wait.until(ExpectedConditions.elementToBeClickable(row));
        row.click();
    }

    // Get the rides count text displayed (e.g., "9 rides found")
    public String getAllRidesCountText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(allRidesCount));
            return allRidesCount.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // Get the rides count text for search results (e.g., "3 rides found")
    public String getSearchRidesCountText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(ridesCount));
            return ridesCount.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // Check if the sort indicator for a given direction is active on start time
    public String getSortIndicatorText(String sortButtonId) {
        WebElement button = driver.findElement(By.id(sortButtonId));
        List<WebElement> indicators = button.findElements(By.cssSelector("span.text-yellow-500"));
        if (indicators.isEmpty()) return "";
        return indicators.get(0).getText().trim();
    }

    // Check if the driver search type button is active (selected)
    public boolean isDriverSearchTypeActive() {
        return searchTypeDriverBtn.getAttribute("class").contains("bg-yellow-500");
    }

    // Check if the passenger search type button is active (selected)
    public boolean isPassengerSearchTypeActive() {
        return searchTypePassengerBtn.getAttribute("class").contains("bg-yellow-500");
    }

    // Get the search ID input value
    public String getSearchIdValue() {
        return searchIdInput.getAttribute("value");
    }

    // Get the date From filter input value
    public String getDateFromFilterValue() {
        return dateFromInput.getAttribute("value");
    }

    // Get the date To filter input value
    public String getDateToFilterValue() {
        return dateToInput.getAttribute("value");
    }

    // Get the date filter input value (from input - backward compatibility)
    public String getDateFilterValue() {
        return getDateFromFilterValue();
    }

    // Check if the clear date button is visible
    public boolean isClearDateButtonVisible() {
        try {
            return clearDateBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Check if the clear search button is visible
    public boolean isClearSearchButtonVisible() {
        try {
            return clearSearchBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get all start date strings from the table
    public List<String> getAllRideStartDates() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-start-date")).getText().trim())
                .collect(Collectors.toList());
    }

    // Get all route texts (pickup addresses) from the table for sort verification
    public List<String> getAllRidePickups() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> row.findElement(By.cssSelector(".ride-pickup")).getText().trim())
                .collect(Collectors.toList());
    }

    // Get all end time texts from the table for sort verification
    public List<String> getAllRideEndTimes() {
        List<WebElement> rows = getRideRows();
        return rows.stream()
                .map(row -> {
                    String date = row.findElement(By.cssSelector(".ride-end-date")).getText().trim();
                    String time = row.findElement(By.cssSelector(".ride-end-time")).getText().trim();
                    return date + " " + time;
                })
                .collect(Collectors.toList());
    }

    // ----- Dynamic Count Helpers -----

    public int getTotalRidesFromBadge() {
        // Check both badges immediately without waiting — data is already loaded
        // by the time this method is called (after waitForDataRefresh or waitForTableToLoad).
        // "all-rides-count" is shown when no search is active (*ngIf="!searchId && rides.length > 0")
        // "rides-count" is shown when searching by driver/passenger ID (*ngIf="selectedUserName")
        try {
            if (allRidesCount.isDisplayed()) {
                String text = allRidesCount.getText().trim();
                String digits = text.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {}

        try {
            if (ridesCount.isDisplayed()) {
                String text = ridesCount.getText().trim();
                String digits = text.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Integer.parseInt(digits);
            }
        } catch (Exception ignored) {}

        return getRideCount();
    }

    
    public int getTotalPageCount() {
        List<Integer> pages = getVisiblePageNumbers();
        if (pages.isEmpty()) return 1;
        return pages.stream().max(Integer::compareTo).orElse(1);
    }

    
    public int getPageSizeFromInfo() {
        String info = getPaginationInfoText();
        if (!info.isEmpty()) {
            // Extract all numbers from "Showing 1 – 5 of 11"
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(info);
            List<Integer> nums = new ArrayList<>();
            while (m.find()) nums.add(Integer.parseInt(m.group()));
            if (nums.size() >= 2) {
                return nums.get(1) - nums.get(0) + 1;
            }
        }
        return getRideCount();
    }

    // ----- Ride Detail Panel -----

    private static final By DETAIL_PANEL_LOCATOR = By.id("ride-detail-panel");
    private static final By DETAIL_MAP_LOCATOR = By.id("admin-history-map");

    // Click a ride row and wait for the detail panel to appear
    public void openRideDetails(int rowIndex) {
        clickRideRow(rowIndex);
        wait.until(ExpectedConditions.visibilityOfElementLocated(DETAIL_PANEL_LOCATOR));
    }

    // Check if the ride detail panel is currently visible
    public boolean isDetailPanelVisible() {
        try {
            return driver.findElement(By.id("ride-detail-panel")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Close the ride detail panel by clicking the close button
    public void closeRideDetails() {
        WebElement closeBtn = driver.findElement(By.id("ride-detail-close"));
        wait.until(ExpectedConditions.elementToBeClickable(closeBtn));
        closeBtn.click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(DETAIL_PANEL_LOCATOR));
    }

    // Check if the map is rendered inside the detail panel
    public boolean isDetailMapVisible() {
        try {
            WebElement map = driver.findElement(By.id("admin-history-map"));
            return map.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get the status text shown in the detail panel header
    public String getDetailStatus() {
        WebElement statusEl = driver.findElement(By.id("ride-detail-status"));
        wait.until(ExpectedConditions.visibilityOf(statusEl));
        return statusEl.getText().trim();
    }

    // Check if the PANIC badge is visible in the detail panel header
    public boolean isDetailPanicBadgeVisible() {
        try {
            return driver.findElement(By.id("ride-detail-panic-badge")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get the pickup address shown in the detail panel
    public String getDetailPickupAddress() {
        WebElement el = driver.findElement(By.id("ride-detail-pickup"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Get the destination address shown in the detail panel
    public String getDetailDestinationAddress() {
        WebElement el = driver.findElement(By.id("ride-detail-destination"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Get the cost text shown in the detail panel
    public String getDetailCost() {
        WebElement el = driver.findElement(By.id("ride-detail-cost"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Get the distance text shown in the detail panel
    public String getDetailDistance() {
        WebElement el = driver.findElement(By.id("ride-detail-distance"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Check if the driver section is visible in the detail panel
    public boolean isDetailDriverSectionVisible() {
        try {
            return driver.findElement(By.id("ride-detail-driver-section")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get the driver name shown in the detail panel
    public String getDetailDriverName() {
        WebElement el = driver.findElement(By.id("ride-detail-driver-name"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Get the driver email shown in the detail panel
    public String getDetailDriverEmail() {
        WebElement el = driver.findElement(By.id("ride-detail-driver-email"));
        wait.until(ExpectedConditions.visibilityOf(el));
        return el.getText().trim();
    }

    // Check if the passengers section is visible in the detail panel
    public boolean isDetailPassengersSectionVisible() {
        try {
            return driver.findElement(By.id("ride-detail-passengers-section")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get the number of passenger entries shown in the detail panel
    public int getDetailPassengerCount() {
        try {
            WebElement section = driver.findElement(By.id("ride-detail-passengers-section"));
            List<WebElement> passengers = section.findElements(By.cssSelector(".bg-gray-800\\/30"));
            return passengers.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // Check if the reviews section is visible in the detail panel
    public boolean isDetailReviewsSectionVisible() {
        try {
            return driver.findElement(By.id("ride-detail-reviews-section")).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // Get the number of review cards shown in the detail panel
    public int getDetailReviewCount() {
        try {
            WebElement section = driver.findElement(By.id("ride-detail-reviews-section"));
            List<WebElement> reviews = section.findElements(By.cssSelector(".review-card"));
            return reviews.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // Get the driver rating values from all reviews in the detail panel
    public List<String> getDetailDriverRatings() {
        List<String> ratings = new ArrayList<>();
        try {
            WebElement section = driver.findElement(By.id("ride-detail-reviews-section"));
            List<WebElement> reviewCards = section.findElements(By.cssSelector(".review-card"));
            for (WebElement card : reviewCards) {
                // The driver rating is the first badge with "DRIVER" label
                List<WebElement> badges = card.findElements(By.cssSelector(".bg-yellow-500\\/10"));
                if (!badges.isEmpty()) {
                    String ratingText = badges.get(0).findElement(By.cssSelector(".text-xs.font-bold")).getText().trim();
                    ratings.add(ratingText);
                }
            }
        } catch (Exception e) {
            // Return empty list
        }
        return ratings;
    }

    // Get the vehicle rating values from all reviews in the detail panel
    public List<String> getDetailVehicleRatings() {
        List<String> ratings = new ArrayList<>();
        try {
            WebElement section = driver.findElement(By.id("ride-detail-reviews-section"));
            List<WebElement> reviewCards = section.findElements(By.cssSelector(".review-card"));
            for (WebElement card : reviewCards) {
                List<WebElement> badges = card.findElements(By.cssSelector(".bg-yellow-500\\/10"));
                if (badges.size() > 1) {
                    String ratingText = badges.get(1).findElement(By.cssSelector(".text-xs.font-bold")).getText().trim();
                    ratings.add(ratingText);
                }
            }
        } catch (Exception e) {
            // Return empty list
        }
        return ratings;
    }
}
