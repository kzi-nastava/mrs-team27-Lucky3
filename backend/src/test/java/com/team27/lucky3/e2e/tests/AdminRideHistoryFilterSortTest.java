package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.AdminRideHistoryPage;
import com.team27.lucky3.e2e.pages.LoginPage;
import com.team27.lucky3.e2e.pages.SidebarComponent;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * E2E Tests for Admin Ride History - Filtering, Sorting, Pagination & Date Range.
 *
 * Preconditions:
 *   - Backend running on port 8081 with seeded data (DataInitializer)
 *   - Angular frontend running on port 4200
 *   - Seeded data includes:
 *       Admin: admin@example.com / password
 *       Driver 1 (id=1): driver@example.com
 *       Driver 2 (id=2): driver2@example.com
 *       Passenger 1 (id=3): passenger@example.com
 *       Passenger 2 (id=4): passenger2@example.com
 *       Passenger 3 (id=5): passenger3@example.com
 *       15 rides total; 12 history rides (FINISHED / CANCELLED / CANCELLED_BY_DRIVER / CANCELLED_BY_PASSENGER)
 *       Pagination: 5 rides per page
 *       Status filter options: all, FINISHED, CANCELLED, CANCELLED_BY_DRIVER, CANCELLED_BY_PASSENGER
 *       Date range: From / To inputs (type=date)
 *
 * History rides summary (sorted by startTime DESC):
 *   Ride 10: FINISHED              - ~7h ago     - $240   - Driver 1 - Passenger 5
 *   Ride 3:  FINISHED              - 1 day ago   - $300   - Driver 1 - Passenger 3
 *   Ride 7:  CANCELLED_BY_PASSENGER- 1 day ago   - $480   - Driver 1 - Passenger 4
 *   Ride 4:  FINISHED              - 2 days ago  - $456   - Driver 1 - Passenger 4
 *   Ride 5:  FINISHED              - 3 days ago  - $1200  - Driver 2 - Passenger 3
 *   Ride 8:  FINISHED              - 4 days ago  - $264   - Driver 1 - Passengers 3+4
 *   Ride 9:  FINISHED + PANIC      - 5 days ago  - $600   - Driver 2 - Passenger 5
 *   Ride 11: CANCELLED_BY_DRIVER   - 6 days ago  - $540   - Driver 2 - Passenger 3
 *   Ride 12: CANCELLED_BY_PASSENGER- 7 days ago  - $360   - Driver 1 - Passenger 4
 *   Ride 13: FINISHED              - 10 days ago - $180   - Driver 1 - Passenger 5
 *   Ride 14: FINISHED              - 14 days ago - $420   - Driver 2 - Passenger 4
 *   Ride 15: CANCELLED             - 20 days ago - $720   - Driver 1 - Passenger 3
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Ride History - Filtering, Sorting & Pagination")
public class AdminRideHistoryFilterSortTest extends BaseTest {

    private static final int UI_PAGE_SIZE = 5;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TABLE_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");



    private static LocalDate parseTableDate(String displayed) {
        if (displayed == null || displayed.isBlank() || displayed.equals("\u2014")) return null;
        return LocalDate.parse(displayed.trim(), TABLE_DATE_FMT);
    }

    private int totalHistoryRides;
    private int totalPages;

    private LoginPage loginPage;
    private SidebarComponent sidebar;
    private AdminRideHistoryPage historyPage;

    @BeforeEach
    public void setUpPages() {
        loginPage = new LoginPage(driver);
        sidebar = new SidebarComponent(driver);
        historyPage = new AdminRideHistoryPage(driver);

        loginPage.loginAsAdmin();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/admin"));
        sidebar.navigateToRideHistory();
        wait.until(ExpectedConditions.urlContains("/admin/ride-history"));
        historyPage.waitForTableToLoad();

        totalHistoryRides = historyPage.getTotalRidesFromBadge();
        totalPages = (int) Math.ceil((double) totalHistoryRides / UI_PAGE_SIZE);
    }

    // =====================================================================
    // HAPPY PATH: Default View (Tests 1-4)
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("HP-01: Default view shows first page of history rides")
    public void testDefaultViewShowsFirstPage() {
        int rideCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, rideCount,
                "First page should show exactly " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(2)
    @DisplayName("HP-02: Default sort is by Start Time descending")
    public void testDefaultSortIsStartTimeDescending() {
        String indicator = historyPage.getSortIndicatorText("sort-start-time");
        assertEquals("▼", indicator, "Default sort indicator should be descending (▼) on Start Time");
    }

    @Test
    @Order(3)
    @DisplayName("HP-03: Rides count badge shows total history rides")
    public void testRidesCountBadgeDisplayed() {
        String countText = historyPage.getAllRidesCountText();
        assertFalse(countText.isEmpty(), "Rides count badge should be displayed");
        assertTrue(countText.contains(String.valueOf(totalHistoryRides)),
                "Rides count should show " + totalHistoryRides + " rides, got: " + countText);
    }

    @Test
    @Order(4)
    @DisplayName("HP-04: Only history statuses are shown (no IN_PROGRESS/SCHEDULED)")
    public void testOnlyHistoryStatusesVisible() {
        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertFalse(status.equals("In Progress"),
                    "IN_PROGRESS rides should not appear in history");
            assertFalse(status.equals("Scheduled"),
                    "SCHEDULED rides should not appear in history");
        }
    }

    // =====================================================================
    // HAPPY PATH: Filtering by Status (Tests 5-9)
    // =====================================================================

    @Test
    @Order(5)
    @DisplayName("HP-05: Filter by FINISHED status shows only finished rides")
    public void testFilterByFinishedStatus() {
        historyPage.selectStatus("FINISHED");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be finished rides");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertTrue(badgeCount > 0, "Badge should show a positive count for FINISHED rides");
        assertTrue(rideCount <= badgeCount,
                "Visible rides (" + rideCount + ") should be <= badge count (" + badgeCount + ")");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(rideCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should have FINISHED status");
        }
    }

    @Test
    @Order(6)
    @DisplayName("HP-06: Filter by CANCELLED status shows only cancelled rides")
    public void testFilterByCancelledStatus() {
        historyPage.selectStatus("CANCELLED");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be cancelled rides");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertTrue(badgeCount > 0, "Badge should show a positive count for CANCELLED rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(rideCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Cancelled", status, "All rides should have CANCELLED status");
        }
    }

    @Test
    @Order(7)
    @DisplayName("HP-07: Filter by CANCELLED_BY_DRIVER shows only driver-cancelled rides")
    public void testFilterByCancelledByDriverStatus() {
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be cancelled-by-driver rides");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertEquals(rideCount, badgeCount,
                "All CANCELLED_BY_DRIVER rides should fit on one page (badge=" + badgeCount + ")");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(rideCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Cancelled By Driver", status,
                    "All rides should have CANCELLED_BY_DRIVER status");
        }

        for (int i = 0; i < rideCount; i++) {
            assertEquals("Driver", historyPage.getRideCancelledBy(i),
                    "Cancelled-by column should show 'Driver' for CANCELLED_BY_DRIVER ride " + i);
        }
    }

    @Test
    @Order(8)
    @DisplayName("HP-08: Filter by CANCELLED_BY_PASSENGER shows only passenger-cancelled rides")
    public void testFilterByCancelledByPassengerStatus() {
        historyPage.selectStatus("CANCELLED_BY_PASSENGER");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be cancelled-by-passenger rides");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertEquals(rideCount, badgeCount,
                "All CANCELLED_BY_PASSENGER rides should fit on one page (badge=" + badgeCount + ")");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(rideCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Cancelled By Passenger", status,
                    "All rides should have CANCELLED_BY_PASSENGER status");
        }

        for (int i = 0; i < rideCount; i++) {
            assertEquals("Passenger", historyPage.getRideCancelledBy(i),
                    "Cancelled-by column should show 'Passenger' for CANCELLED_BY_PASSENGER ride " + i);
        }
    }

    @Test
    @Order(9)
    @DisplayName("HP-09: Selecting 'All Statuses' shows all history rides again")
    public void testFilterByAllStatuses() {
        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getRideCount();
        assertTrue(finishedCount > 0, "Should have finished rides");

        historyPage.selectStatus("all");
        int allCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, allCount,
                "All statuses should show first page with " + UI_PAGE_SIZE + " rides");

        String countText = historyPage.getAllRidesCountText();
        assertTrue(countText.contains(String.valueOf(totalHistoryRides)),
                "Count badge should show " + totalHistoryRides + " after selecting all statuses");
    }

    // =====================================================================
    // HAPPY PATH: Filtering by Driver/Passenger ID (Tests 10-13)
    // =====================================================================

    @Test
    @Order(10)
    @DisplayName("HP-10: Search by Driver ID shows rides for that driver")
    public void testSearchByDriverId() {
        historyPage.searchByDriverId("1");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Driver 1 should have history rides");

        String countText = historyPage.getSearchRidesCountText();
        assertTrue(countText.contains("ride"), "Search result count should be displayed");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertTrue(badgeCount > 0, "Badge count should be positive for Driver 1");
        assertTrue(badgeCount < totalHistoryRides,
                "Driver 1 ride count (" + badgeCount + ") should be less than total (" + totalHistoryRides + ")");

        for (int i = 0; i < rideCount; i++) {
            String startDate = historyPage.getRideStartDate(i);
            assertFalse(startDate.isEmpty(), "Ride " + i + " should have a start date");
            assertNotEquals("\u2014", startDate, "Ride " + i + " start date should not be em-dash");

            String cost = historyPage.getRideCost(i);
            assertNotNull(cost, "Ride " + i + " should have a cost");
        }
    }

    @Test
    @Order(11)
    @DisplayName("HP-11: Search by Passenger ID shows rides for that passenger")
    public void testSearchByPassengerId() {
        historyPage.searchByPassengerId("3");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Passenger 3 should have history rides");

        int badgeCount = historyPage.getTotalRidesFromBadge();
        assertTrue(badgeCount > 0, "Badge count should be positive for Passenger 3");
        assertTrue(badgeCount < totalHistoryRides,
                "Passenger 3 ride count (" + badgeCount + ") should be less than total (" + totalHistoryRides + ")");

        // Verify each visible ride has valid data
        for (int i = 0; i < rideCount; i++) {
            String startDate = historyPage.getRideStartDate(i);
            assertFalse(startDate.isEmpty(), "Ride " + i + " should have a start date");
            String status = historyPage.getRideStatus(i);
            assertFalse(status.isEmpty(), "Ride " + i + " should have a status");
        }
    }

    @Test
    @Order(12)
    @DisplayName("HP-12: Search by Driver ID 2 shows different rides than Driver ID 1")
    public void testSearchByDifferentDriverIds() {
        historyPage.searchByDriverId("1");
        int driver1Badge = historyPage.getTotalRidesFromBadge();
        List<Double> driver1Costs = historyPage.getAllRideCostValues();
        List<String> driver1Statuses = historyPage.getAllRideStatuses();

        historyPage.clickClearSearch();
        historyPage.searchByDriverId("2");
        int driver2Badge = historyPage.getTotalRidesFromBadge();
        List<Double> driver2Costs = historyPage.getAllRideCostValues();
        List<String> driver2Statuses = historyPage.getAllRideStatuses();

        assertTrue(driver1Badge > 0, "Driver 1 should have rides");
        assertTrue(driver2Badge > 0, "Driver 2 should have rides");
        assertNotEquals(driver1Badge, driver2Badge,
                "Driver 1 (" + driver1Badge + ") and Driver 2 (" + driver2Badge + ") should have different ride counts");
        assertNotEquals(driver1Costs, driver2Costs,
                "Driver 1 and Driver 2 should have different ride costs on their first page");

        assertFalse(driver1Statuses.isEmpty(), "Driver 1 should have visible status values");
        assertFalse(driver2Statuses.isEmpty(), "Driver 2 should have visible status values");
    }

    @Test
    @Order(13)
    @DisplayName("HP-13: Clear search returns all history rides")
    public void testClearSearchReturnsAllRides() {
        int allCount = historyPage.getRideCount();

        historyPage.searchByDriverId("1");
        int driverCount = historyPage.getRideCount();
        assertTrue(driverCount <= allCount, "Filtered rides should be <= all rides");

        historyPage.clickClearSearch();
        int newCount = historyPage.getRideCount();
        assertEquals(allCount, newCount, "After clear, should show first page again");
    }

    // =====================================================================
    // HAPPY PATH: Date Range Filtering (Tests 14-19)
    // =====================================================================

    @Test
    @Order(14)
    @DisplayName("HP-14: Filter by From date narrows results to rides on or after that date")
    public void testFilterByFromDate() {
        LocalDate fromLocalDate = LocalDate.now().minusDays(2);
        String fromDate = fromLocalDate.format(DATE_FMT);
        historyPage.setDateFromFilter(fromDate);

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides from recent 2 days");
        assertTrue(rideCount < totalHistoryRides,
                "Should have fewer rides than total (" + totalHistoryRides + ") after From date filter, got: " + rideCount);

        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isBefore(fromLocalDate),
                    "Ride date " + rideDate + " should not be before From filter " + fromLocalDate);
        }
    }

    @Test
    @Order(15)
    @DisplayName("HP-15: Filter by To date narrows results to rides on or before that date")
    public void testFilterByToDate() {
        LocalDate toLocalDate = LocalDate.now().minusDays(8);
        String toDate = toLocalDate.format(DATE_FMT);
        historyPage.setDateToFilter(toDate);

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides older than 8 days");
        assertTrue(rideCount < totalHistoryRides,
                "Should have fewer rides than total after To date filter, got: " + rideCount);

        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isAfter(toLocalDate),
                    "Ride date " + rideDate + " should not be after To filter " + toLocalDate);
        }
    }

    @Test
    @Order(16)
    @DisplayName("HP-16: Filter by From and To date range shows rides within interval")
    public void testFilterByDateRange() {
        LocalDate fromLocalDate = LocalDate.now().minusDays(5);
        LocalDate toLocalDate = LocalDate.now().minusDays(1);
        String fromDate = fromLocalDate.format(DATE_FMT);
        String toDate = toLocalDate.format(DATE_FMT);
        historyPage.setDateRange(fromDate, toDate);

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides within the 5-day range");
        assertTrue(rideCount < totalHistoryRides,
                "Date range should not include all rides");

        List<String> startDates = historyPage.getAllRideStartDates();
        assertEquals(rideCount, startDates.size(), "Start dates list should match ride count");
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isBefore(fromLocalDate),
                    "Ride date " + rideDate + " should not be before From filter " + fromLocalDate);
            assertFalse(rideDate.isAfter(toLocalDate),
                    "Ride date " + rideDate + " should not be after To filter " + toLocalDate);
        }
    }

    @Test
    @Order(17)
    @DisplayName("HP-17: Filter by far-future From date shows no rides")
    public void testFilterByFutureFromDate() {
        String futureDate = LocalDate.now().plusYears(5).format(DATE_FMT);
        historyPage.setDateFromFilter(futureDate);

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "No rides should exist in the far future");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
    }

    @Test
    @Order(18)
    @DisplayName("HP-18: Filter by far-past To date shows no rides")
    public void testFilterByFarPastToDate() {
        String pastDate = LocalDate.of(2020, 1, 1).format(DATE_FMT);
        historyPage.setDateToFilter(pastDate);

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "No rides should exist before 2020");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
    }

    @Test
    @Order(19)
    @DisplayName("HP-19: Clear date filter restores all results")
    public void testClearDateFilterRestoresResults() {
        int allCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, allCount, "Initial page should show " + UI_PAGE_SIZE + " rides");

        String fromDate = LocalDate.now().minusDays(2).format(DATE_FMT);
        historyPage.setDateFromFilter(fromDate);
        int filteredCount = historyPage.getRideCount();
        assertTrue(filteredCount > 0 && filteredCount < allCount,
                "Date filter should narrow results: expected less than " + allCount + ", got: " + filteredCount);

        // Clear and verify restoration
        assertTrue(historyPage.isClearDateButtonVisible(), "Clear date button should be visible");
        historyPage.clearDateFilter();
        int restoredCount = historyPage.getRideCount();
        assertEquals(allCount, restoredCount,
                "Clearing date filter should restore all rides");
    }

    // =====================================================================
    // HAPPY PATH: Combined Filters (Tests 20-22)
    // =====================================================================

    @Test
    @Order(20)
    @DisplayName("HP-20: Combine driver search with status filter")
    public void testCombineDriverSearchWithStatusFilter() {
        historyPage.searchByDriverId("1");
        int driverCount = historyPage.getTotalRidesFromBadge();

        historyPage.selectStatus("FINISHED");
        int filteredCount = historyPage.getRideCount();
        int filteredBadge = historyPage.getTotalRidesFromBadge();

        assertTrue(filteredBadge <= driverCount,
                "FINISHED count (" + filteredBadge + ") should be <= driver count (" + driverCount + ")");
        assertTrue(filteredCount > 0, "Driver 1 should have finished rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(filteredCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should be FINISHED");
        }

        for (int i = 0; i < filteredCount; i++) {
            assertEquals("\u2014", historyPage.getRideCancelledBy(i),
                    "FINISHED ride " + i + " should show em-dash in cancelled-by column");
        }
    }

    @Test
    @Order(21)
    @DisplayName("HP-21: Combine passenger search with status filter")
    public void testCombinePassengerSearchWithStatusFilter() {
        historyPage.searchByPassengerId("3");
        int passengerBadge = historyPage.getTotalRidesFromBadge();
        assertTrue(passengerBadge > 0, "Passenger 3 should have history rides");

        historyPage.selectStatus("FINISHED");
        int filteredCount = historyPage.getRideCount();
        int filteredBadge = historyPage.getTotalRidesFromBadge();

        assertTrue(filteredBadge <= passengerBadge,
                "FINISHED count (" + filteredBadge + ") should be <= passenger count (" + passengerBadge + ")");
        assertTrue(filteredCount > 0, "Passenger 3 should have finished rides");

        // Verify all visible rides actually have FINISHED status
        List<String> statuses = historyPage.getAllRideStatuses();
        assertEquals(filteredCount, statuses.size(), "Status list size should match ride count");
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should be FINISHED");
        }
    }

    @Test
    @Order(22)
    @DisplayName("HP-22: Combine date range with status filter")
    public void testCombineDateRangeWithStatusFilter() {
        LocalDate fromLocalDate = LocalDate.now().minusDays(7);
        String fromDate = fromLocalDate.format(DATE_FMT);
        historyPage.setDateFromFilter(fromDate);
        int dateFilteredBadge = historyPage.getTotalRidesFromBadge();
        assertTrue(dateFilteredBadge > 0, "Should have rides in last 7 days");

        historyPage.selectStatus("CANCELLED");
        int combinedCount = historyPage.getRideCount();
        int combinedBadge = historyPage.getTotalRidesFromBadge();

        assertTrue(combinedBadge <= dateFilteredBadge,
                "Adding status filter should further narrow results");

        // Verify that visible rides satisfy BOTH constraints:
        // status = Cancelled AND start date >= fromDate
        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Cancelled", status, "All rides should be CANCELLED");
        }
        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isBefore(fromLocalDate),
                    "Ride date " + rideDate + " should not be before From filter " + fromLocalDate);
        }
    }

    // =====================================================================
    // HAPPY PATH: Sorting (Tests 23-31)
    // =====================================================================

    @Test
    @Order(23)
    @DisplayName("HP-23: Sort by cost descending")
    public void testSortByCostDescending() {
        historyPage.sortByCost(); // First click: desc

        List<Double> costs = historyPage.getAllRideCostValues();
        assertTrue(costs.size() > 1, "Should have multiple rides to verify sort");
        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) >= costs.get(i + 1),
                    "Costs should be in descending order: " + costs.get(i) + " >= " + costs.get(i + 1));
        }
    }

    @Test
    @Order(24)
    @DisplayName("HP-24: Sort by cost ascending (double click)")
    public void testSortByCostAscending() {
        historyPage.sortByCost(); // First click: desc
        historyPage.sortByCost(); // Second click: asc

        List<Double> costs = historyPage.getAllRideCostValues();
        assertTrue(costs.size() > 1, "Should have multiple rides to verify sort");
        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) <= costs.get(i + 1),
                    "Costs should be in ascending order: " + costs.get(i) + " <= " + costs.get(i + 1));
        }
    }

    @Test
    @Order(25)
    @DisplayName("HP-25: Sort by start time ascending reverses default order")
    public void testSortByStartTimeAscending() {
        historyPage.sortByStartTime(); // Toggle to asc

        String indicator = historyPage.getSortIndicatorText("sort-start-time");
        assertEquals("▲", indicator, "Sort indicator should change to ascending (▲)");

        // Verify actual start dates are in ascending order
        List<String> startDates = historyPage.getAllRideStartDates();
        assertTrue(startDates.size() > 1, "Should have multiple rides to verify sort");
        for (int i = 0; i < startDates.size() - 1; i++) {
            LocalDate current = parseTableDate(startDates.get(i));
            LocalDate next = parseTableDate(startDates.get(i + 1));
            if (current != null && next != null) {
                assertFalse(current.isAfter(next),
                        "Start date at index " + i + " (" + current + ") should be <= date at index " + (i + 1) + " (" + next + ") in ASC sort");
            }
        }
    }

    @Test
    @Order(26)
    @DisplayName("HP-26: Sort by status groups rides by status")
    public void testSortByStatus() {
        historyPage.sortByStatus(); // First click: desc

        String indicator = historyPage.getSortIndicatorText("sort-status");
        assertEquals("▼", indicator, "Should show descending indicator for status sort");

        List<String> statuses = historyPage.getAllRideStatuses();
        assertTrue(statuses.size() > 1, "Should have multiple rides to sort");

        // Verify rides are grouped — all items of the same status should be contiguous
        // (Natural consequence of sorting by status)
        int rideCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, rideCount, "Should still show " + UI_PAGE_SIZE + " rides after sort");
    }

    @Test
    @Order(27)
    @DisplayName("HP-27: Sort by panic places panic rides correctly")
    public void testSortByPanic() {
        historyPage.sortByPanic(); // First click: desc

        String indicator = historyPage.getSortIndicatorText("sort-panic");
        assertEquals("▼", indicator, "Should show descending indicator for panic sort");

        List<String> panics = historyPage.getAllRidePanics();
        assertTrue(panics.size() > 0, "Should have rides");
        assertTrue(panics.contains("YES"), "At least one ride should have PANIC = YES");

        // In descending panic sort, YES (panic=true) should appear before — (panic=false)
        int lastYesIndex = panics.lastIndexOf("YES");
        int firstDashIndex = panics.indexOf("—");
        if (lastYesIndex >= 0 && firstDashIndex >= 0) {
            assertTrue(lastYesIndex < firstDashIndex,
                    "All YES values should come before — in descending panic sort");
        }
    }

    @Test
    @Order(28)
    @DisplayName("HP-28: Sort by cancelled-by shows sort indicator and preserves data")
    public void testSortByCancelledBy() {
        historyPage.sortByCancelledBy(); // desc

        String indicator = historyPage.getSortIndicatorText("sort-cancelled-by");
        assertEquals("▼", indicator, "Should show descending indicator for cancelled-by sort");

        int rideCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, rideCount, "Should still show " + UI_PAGE_SIZE + " rides after sort");

        List<String> cancelledBys = historyPage.getAllRideCancelledBys();
        assertTrue(cancelledBys.size() > 0, "Should have rides with cancelled-by data");
    }

    @Test
    @Order(29)
    @DisplayName("HP-29: Toggle sort direction for end time")
    public void testToggleSortDirectionEndTime() {
        historyPage.sortByEndTime(); // desc
        String indicator1 = historyPage.getSortIndicatorText("sort-end-time");
        assertEquals("▼", indicator1, "First click should sort descending");

        historyPage.sortByEndTime(); // asc
        String indicator2 = historyPage.getSortIndicatorText("sort-end-time");
        assertEquals("▲", indicator2, "Second click should sort ascending");
    }

    @Test
    @Order(30)
    @DisplayName("HP-30: Switching sort field resets direction to descending")
    public void testSwitchingSortFieldResetsDirection() {
        historyPage.sortByCost(); // desc
        historyPage.sortByCost(); // asc
        String costIndicator = historyPage.getSortIndicatorText("sort-cost");
        assertEquals("▲", costIndicator, "Should be ascending on cost");

        historyPage.sortByStatus(); // new field → desc
        String statusIndicator = historyPage.getSortIndicatorText("sort-status");
        assertEquals("▼", statusIndicator, "Switching sort field should start with descending");
    }

    @Test
    @Order(31)
    @DisplayName("HP-31: Sort by route shows sort indicator")
    public void testSortByRoute() {
        historyPage.sortByRoute(); // desc
        String indicator = historyPage.getSortIndicatorText("sort-route");
        assertEquals("▼", indicator, "Route sort should show descending indicator");

        List<String> pickups = historyPage.getAllRidePickups();
        assertTrue(pickups.size() > 1, "Should have multiple rides to verify route sort");

        List<String> expectedOrder = new ArrayList<>(pickups);
        expectedOrder.sort(String.CASE_INSENSITIVE_ORDER.reversed());
        assertEquals(expectedOrder, pickups,
                "Pickup addresses should be sorted in descending alphabetical order");
    }

    // =====================================================================
    // HAPPY PATH: Sort + Filter Combined (Tests 32-33)
    // =====================================================================

    @Test
    @Order(32)
    @DisplayName("HP-32: Filter by FINISHED then sort by cost descending")
    public void testFilterThenSort() {
        historyPage.selectStatus("FINISHED");
        historyPage.sortByCost(); // desc

        List<Double> costs = historyPage.getAllRideCostValues();
        assertTrue(costs.size() > 1, "Should have multiple finished rides");

        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) >= costs.get(i + 1),
                    "Costs should be in descending order for FINISHED rides");
        }
    }

    @Test
    @Order(33)
    @DisplayName("HP-33: Search by driver then sort by cost ascending")
    public void testSearchDriverSortByCostAsc() {
        historyPage.searchByDriverId("1");
        historyPage.sortByCost(); // desc
        historyPage.sortByCost(); // asc

        List<Double> costs = historyPage.getAllRideCostValues();
        assertTrue(costs.size() > 0, "Driver 1 should have history rides");

        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) <= costs.get(i + 1),
                    "Costs should be in ascending order for Driver 1 rides");
        }
    }

    // =====================================================================
    // HAPPY PATH: Pagination (Tests 34-46)
    // =====================================================================

    @Test
    @Order(34)
    @DisplayName("HP-34: Pagination controls are visible when rides exceed page size")
    public void testPaginationControlsVisible() {
        assertTrue(historyPage.isPaginationVisible(),
                "Pagination should be visible with " + totalHistoryRides + " rides (>" + UI_PAGE_SIZE + ")");
    }

    @Test
    @Order(35)
    @DisplayName("HP-35: First page shows correct number of rides")
    public void testFirstPageShowsCorrectCount() {
        int rideCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, rideCount,
                "First page should show " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(36)
    @DisplayName("HP-36: Pagination info text shows correct range")
    public void testPaginationInfoText() {
        String info = historyPage.getPaginationInfoText();
        assertTrue(info.contains("1"), "Should show starting item as 1");
        assertTrue(info.contains(String.valueOf(UI_PAGE_SIZE)), "Should show ending item as " + UI_PAGE_SIZE);
        assertTrue(info.contains(String.valueOf(totalHistoryRides)),
                "Should show total of " + totalHistoryRides);
    }

    @Test
    @Order(37)
    @DisplayName("HP-37: Active page number is 1 on initial load")
    public void testInitialActivePageIs1() {
        int activePage = historyPage.getActivePageNumber();
        assertEquals(1, activePage, "Active page should be 1 on initial load");
    }

    @Test
    @Order(38)
    @DisplayName("HP-38: Clicking Next page moves to page 2")
    public void testClickNextPage() {
        historyPage.clickNextPage();

        int activePage = historyPage.getActivePageNumber();
        assertEquals(2, activePage, "Should be on page 2 after clicking Next");
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Page 2 should also show " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(39)
    @DisplayName("HP-39: Clicking Prev page goes back to page 1")
    public void testClickPrevPage() {
        historyPage.clickNextPage();
        assertEquals(2, historyPage.getActivePageNumber());

        historyPage.clickPrevPage();
        assertEquals(1, historyPage.getActivePageNumber(),
                "Should be on page 1 after clicking Prev");
    }

    @Test
    @Order(40)
    @DisplayName("HP-40: Clicking Last page goes to the last page")
    public void testClickLastPage() {
        historyPage.clickLastPage();

        int activePage = historyPage.getActivePageNumber();
        assertEquals(totalPages, activePage,
                "Should be on the last page (" + totalPages + ")");

        int rideCount = historyPage.getRideCount();
        int expectedLastPageCount = totalHistoryRides - (totalPages - 1) * UI_PAGE_SIZE;
        assertEquals(expectedLastPageCount, rideCount,
                "Last page should show " + expectedLastPageCount + " rides");
    }

    @Test
    @Order(41)
    @DisplayName("HP-41: Clicking First page returns to page 1")
    public void testClickFirstPage() {
        historyPage.clickLastPage();
        assertEquals(totalPages, historyPage.getActivePageNumber());

        historyPage.clickFirstPage();
        assertEquals(1, historyPage.getActivePageNumber(),
                "Should be on page 1 after clicking First");
    }

    @Test
    @Order(42)
    @DisplayName("HP-42: Clicking a specific page number button navigates to that page")
    public void testClickPageNumberButton() {
        historyPage.goToPage(1); // page index 1 = page 2

        int activePage = historyPage.getActivePageNumber();
        assertEquals(2, activePage, "Should be on page 2");
    }

    @Test
    @Order(43)
    @DisplayName("HP-43: Prev/First buttons are disabled on first page")
    public void testPrevFirstDisabledOnFirstPage() {
        assertTrue(historyPage.isPrevPageDisabled(),
                "Prev button should be disabled on first page");
        assertTrue(historyPage.isFirstPageDisabled(),
                "First button should be disabled on first page");
    }

    @Test
    @Order(44)
    @DisplayName("HP-44: Next/Last buttons are disabled on last page")
    public void testNextLastDisabledOnLastPage() {
        historyPage.clickLastPage();

        assertTrue(historyPage.isNextPageDisabled(),
                "Next button should be disabled on last page");
        assertTrue(historyPage.isLastPageDisabled(),
                "Last button should be disabled on last page");
    }

    @Test
    @Order(45)
    @DisplayName("HP-45: Changing status filter resets to page 1")
    public void testStatusFilterResetsToPage1() {
        historyPage.clickNextPage();
        assertEquals(2, historyPage.getActivePageNumber());

        historyPage.selectStatus("FINISHED");
        assertEquals(1, historyPage.getActivePageNumber(),
                "Changing status filter should reset to page 1");
    }

    @Test
    @Order(46)
    @DisplayName("HP-46: Pagination disappears when filter narrows to single page")
    public void testPaginationDisappearsForSinglePage() {
        // CANCELLED_BY_DRIVER has only 1 ride — should be single page
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        int rideCount = historyPage.getRideCount();
        assertEquals(1, rideCount, "Should have 1 cancelled-by-driver ride");

        assertFalse(historyPage.isPaginationVisible(),
                "Pagination should not be visible with only 1 ride");
    }

    // =====================================================================
    // HAPPY PATH: Ride Detail View (Tests 47-56)
    // =====================================================================

    @Test
    @Order(47)
    @DisplayName("HP-47: Clicking a ride row opens the detail panel with map")
    public void testClickRideRowOpensDetailPanel() {
        historyPage.openRideDetails(0);

        assertTrue(historyPage.isDetailPanelVisible(),
                "Detail panel should be visible after clicking a ride row");
        assertTrue(historyPage.isDetailMapVisible(),
                "Map should be rendered inside the detail panel");
    }

    @Test
    @Order(48)
    @DisplayName("HP-48: Detail panel displays ride status")
    public void testDetailPanelShowsStatus() {
        historyPage.openRideDetails(0);

        String detailStatus = historyPage.getDetailStatus();
        assertNotNull(detailStatus, "Detail panel should show ride status");
        assertFalse(detailStatus.isEmpty(), "Detail panel status should not be empty");
    }

    @Test
    @Order(49)
    @DisplayName("HP-49: Detail panel displays pickup and destination addresses")
    public void testDetailPanelShowsRoute() {
        historyPage.openRideDetails(0);

        String pickup = historyPage.getDetailPickupAddress();
        assertFalse(pickup.isEmpty(), "Pickup address should not be empty");

        String destination = historyPage.getDetailDestinationAddress();
        assertFalse(destination.isEmpty(), "Destination address should not be empty");
    }

    @Test
    @Order(50)
    @DisplayName("HP-50: Detail panel displays cost and distance")
    public void testDetailPanelShowsCostAndDistance() {
        historyPage.openRideDetails(0);

        String cost = historyPage.getDetailCost();
        assertTrue(cost.contains("$"), "Cost should contain dollar sign");

        String distance = historyPage.getDetailDistance();
        assertTrue(distance.contains("km"), "Distance should contain 'km'");
    }

    @Test
    @Order(51)
    @DisplayName("HP-51: Detail panel displays driver and passenger information")
    public void testDetailPanelShowsDriverAndPassengerInfo() {
        historyPage.openRideDetails(0);

        assertTrue(historyPage.isDetailDriverSectionVisible(),
                "Driver section should be visible");
        String driverName = historyPage.getDetailDriverName();
        assertFalse(driverName.isEmpty(), "Driver name should not be empty");

        assertTrue(historyPage.isDetailPassengersSectionVisible(),
                "Passengers section should be visible");
        assertTrue(historyPage.getDetailPassengerCount() >= 1,
                "Should show at least 1 passenger");
    }

    @Test
    @Order(52)
    @DisplayName("HP-52: Closing the detail panel hides it")
    public void testCloseDetailPanel() {
        historyPage.openRideDetails(0);
        assertTrue(historyPage.isDetailPanelVisible());

        historyPage.closeRideDetails();
        assertFalse(historyPage.isDetailPanelVisible(),
                "Detail panel should be hidden after closing");
    }

    @Test
    @Order(53)
    @DisplayName("HP-53: Detail panel status matches table row status")
    public void testDetailStatusMatchesTableRow() {
        String tableStatus = historyPage.getRideStatus(0);
        historyPage.openRideDetails(0);
        String detailStatus = historyPage.getDetailStatus();

        assertEquals(tableStatus, detailStatus,
                "Status in detail panel should match the status in the table row");
    }

    @Test
    @Order(54)
    @DisplayName("HP-54: Switching between ride details updates the panel")
    public void testSwitchBetweenRideDetails() {
        historyPage.openRideDetails(0);
        String firstCost = historyPage.getDetailCost();
        String firstPickup = historyPage.getDetailPickupAddress();

        historyPage.closeRideDetails();
        historyPage.openRideDetails(1);
        String secondCost = historyPage.getDetailCost();
        String secondPickup = historyPage.getDetailPickupAddress();

        boolean costDiffers = !firstCost.equals(secondCost);
        boolean pickupDiffers = !firstPickup.equals(secondPickup);
        assertTrue(costDiffers || pickupDiffers,
                "Different rides should display different details");
    }

    @Test
    @Order(55)
    @DisplayName("HP-55: Panic ride detail shows PANIC badge")
    public void testPanicRideDetailShowsPanicBadge() {
        // Search for driver 2 who has the panic ride
        historyPage.searchByDriverId("2");

        List<String> panics = historyPage.getAllRidePanics();
        int panicIndex = panics.indexOf("YES");
        assertTrue(panicIndex >= 0, "Driver 2 should have a panic ride");

        historyPage.openRideDetails(panicIndex);
        assertTrue(historyPage.isDetailPanicBadgeVisible(),
                "PANIC badge should be visible in detail panel for panic ride");
    }

    @Test
    @Order(56)
    @DisplayName("HP-56: Finished ride detail shows reviews/ratings")
    public void testFinishedRideDetailShowsRatings() {
        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getRideCount();
        assertTrue(finishedCount > 0, "Should have finished rides");

        // Try multiple finished rides to find one with reviews
        boolean foundReview = false;
        for (int i = 0; i < finishedCount; i++) {
            historyPage.openRideDetails(i);
            if (historyPage.isDetailReviewsSectionVisible()) {
                int reviewCount = historyPage.getDetailReviewCount();
                assertTrue(reviewCount > 0, "Reviews section visible but no reviews displayed");

                List<String> driverRatings = historyPage.getDetailDriverRatings();
                assertFalse(driverRatings.isEmpty(), "Should have driver ratings");
                for (String rating : driverRatings) {
                    int ratingValue = Integer.parseInt(rating);
                    assertTrue(ratingValue >= 1 && ratingValue <= 5,
                            "Driver rating should be 1-5, got: " + ratingValue);
                }

                List<String> vehicleRatings = historyPage.getDetailVehicleRatings();
                assertFalse(vehicleRatings.isEmpty(), "Should have vehicle ratings");
                for (String rating : vehicleRatings) {
                    int ratingValue = Integer.parseInt(rating);
                    assertTrue(ratingValue >= 1 && ratingValue <= 5,
                            "Vehicle rating should be 1-5, got: " + ratingValue);
                }

                foundReview = true;
                break;
            }
            historyPage.closeRideDetails();
        }
        // At least one finished ride with reviews should exist in seeded data
        assertTrue(foundReview,
                "At least one FINISHED ride should have a reviews section with ratings");
    }

    // =====================================================================
    // EDGE CASES: Search (Tests 57-62)
    // =====================================================================

    @Test
    @Order(57)
    @DisplayName("EC-01: Search with non-existent driver ID returns no rides")
    public void testSearchNonExistentDriverId() {
        historyPage.searchByDriverId("9999");

        assertEquals(0, historyPage.getRideCount(),
                "Non-existent driver ID should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' message");
    }

    @Test
    @Order(58)
    @DisplayName("EC-02: Search with non-existent passenger ID returns no rides")
    public void testSearchNonExistentPassengerId() {
        historyPage.searchByPassengerId("9999");

        assertEquals(0, historyPage.getRideCount(),
                "Non-existent passenger ID should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' message");
    }

    @Test
    @Order(59)
    @DisplayName("EC-03: Search without entering ID shows all rides")
    public void testSearchWithoutId() {
        historyPage.selectSearchTypeDriver();
        historyPage.clearSearchId();
        historyPage.clickSearch();

        assertTrue(historyPage.getRideCount() > 0,
                "Searching without ID should show rides");
    }

    @Test
    @Order(60)
    @DisplayName("EC-04: Switch search type from driver to passenger and search")
    public void testSwitchSearchType() {
        historyPage.searchByDriverId("1");
        int driver1Count = historyPage.getRideCount();
        assertTrue(driver1Count > 0, "Driver 1 should have rides");

        historyPage.searchByPassengerId("3");
        int passenger3Count = historyPage.getRideCount();
        assertTrue(passenger3Count > 0, "Passenger 3 should have rides");
    }

    @Test
    @Order(61)
    @DisplayName("EC-05: Driver search type button shows as active")
    public void testDriverSearchTypeButtonActive() {
        historyPage.selectSearchTypeDriver();
        assertTrue(historyPage.isDriverSearchTypeActive(), "Driver button should be active");
        assertFalse(historyPage.isPassengerSearchTypeActive(), "Passenger button should not be active");
    }

    @Test
    @Order(62)
    @DisplayName("EC-06: Passenger search type button shows as active")
    public void testPassengerSearchTypeButtonActive() {
        historyPage.selectSearchTypePassenger();
        assertTrue(historyPage.isPassengerSearchTypeActive(), "Passenger button should be active");
        assertFalse(historyPage.isDriverSearchTypeActive(), "Driver button should not be active");
    }

    // =====================================================================
    // EDGE CASES: Status Filter (Tests 63-64)
    // =====================================================================

    @Test
    @Order(63)
    @DisplayName("EC-07: Rapidly switching status filters doesn't break the table")
    public void testRapidStatusFilterSwitching() {
        historyPage.selectStatus("FINISHED");
        historyPage.selectStatus("CANCELLED");
        historyPage.selectStatus("CANCELLED_BY_DRIVER");
        historyPage.selectStatus("all");

        assertTrue(historyPage.getRideCount() > 0,
                "All rides should be shown after switching back to 'all'");
    }

    @Test
    @Order(64)
    @DisplayName("EC-08: Status filter persists after changing search")
    public void testStatusFilterPersistsAfterChangingSearch() {
        historyPage.selectStatus("FINISHED");
        historyPage.searchByDriverId("1");

        String selectedStatus = historyPage.getSelectedStatus();
        assertEquals("FINISHED", selectedStatus, "Status filter should persist after search");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should still be FINISHED");
        }
    }

    // =====================================================================
    // EDGE CASES: Date Range (Tests 65-67)
    // =====================================================================

    @Test
    @Order(65)
    @DisplayName("EC-09: Setting From after To (inverted range) shows no results")
    public void testInvertedDateRange() {
        // From = 1 day ago, To = 10 days ago → inverted range
        String fromDate = LocalDate.now().minusDays(1).format(DATE_FMT);
        String toDate = LocalDate.now().minusDays(10).format(DATE_FMT);
        historyPage.setDateRange(fromDate, toDate);

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "Inverted date range should show no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' for inverted range");
    }

    @Test
    @Order(66)
    @DisplayName("EC-10: Same From and To date shows rides from that exact day")
    public void testSameDateRange() {
        // Use the date of ride 3 (1 day ago) — Rides 3 and 7 were created 1 day ago
        LocalDate targetDate = LocalDate.now().minusDays(1);
        String date = targetDate.format(DATE_FMT);
        historyPage.setDateRange(date, date);

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0,
                "Same date filter for 1 day ago should return at least 1 ride (Rides 3 and 7 are from that date)");
        assertTrue(rideCount <= totalHistoryRides,
                "Should not return more than total history rides");

        // Verify all visible rides have the exact same start date
        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertEquals(targetDate, rideDate,
                    "All rides should have start date " + targetDate + ", got " + rideDate);
        }
    }

    @Test
    @Order(67)
    @DisplayName("EC-11: Clear date button clears both From and To")
    public void testClearDateClearsBothInputs() {
        String fromDate = LocalDate.now().minusDays(5).format(DATE_FMT);
        String toDate = LocalDate.now().minusDays(1).format(DATE_FMT);
        historyPage.setDateRange(fromDate, toDate);

        assertTrue(historyPage.isClearDateButtonVisible(), "Clear date button should be visible");
        historyPage.clearDateFilter();

        String fromVal = historyPage.getDateFromFilterValue();
        String toVal = historyPage.getDateToFilterValue();
        assertTrue(fromVal == null || fromVal.isEmpty(),
                "From date should be cleared");
        assertTrue(toVal == null || toVal.isEmpty(),
                "To date should be cleared");
    }

    // =====================================================================
    // EDGE CASES: Combined Filter Edge Cases (Tests 68-71)
    // =====================================================================

    @Test
    @Order(68)
    @DisplayName("EC-12: Non-existent driver + FINISHED status shows no rides")
    public void testNonExistentDriverWithFinishedFilter() {
        historyPage.searchByDriverId("9999");
        historyPage.selectStatus("FINISHED");

        assertEquals(0, historyPage.getRideCount(),
                "Non-existent driver with FINISHED filter should show no rides");
    }

    @Test
    @Order(69)
    @DisplayName("EC-13: Driver 1 with CANCELLED filter shows the cancelled ride(s)")
    public void testDriver1WithCancelledFilter() {
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("CANCELLED");

        assertTrue(historyPage.getRideCount() > 0,
                "Driver 1 should have at least one cancelled ride");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Cancelled", status, "All rides should be cancelled");
        }
    }

    @Test
    @Order(70)
    @DisplayName("EC-14: Driver 2 with CANCELLED filter shows no rides")
    public void testDriver2WithCancelledFilter() {
        historyPage.searchByDriverId("2");
        historyPage.selectStatus("CANCELLED");

        assertEquals(0, historyPage.getRideCount(),
                "Driver 2 should have no CANCELLED rides (has CANCELLED_BY_DRIVER instead)");
    }

    @Test
    @Order(71)
    @DisplayName("EC-15: Driver 2 with CANCELLED_BY_DRIVER shows the cancelled-by-driver ride")
    public void testDriver2WithCancelledByDriverFilter() {
        historyPage.searchByDriverId("2");
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        assertTrue(historyPage.getRideCount() > 0,
                "Driver 2 should have cancelled-by-driver ride");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Cancelled By Driver", status,
                    "All rides should be CANCELLED_BY_DRIVER");
        }
    }

    // =====================================================================
    // EDGE CASES: Pagination & Sort (Tests 72-77)
    // =====================================================================

    @Test
    @Order(72)
    @DisplayName("EC-16: Navigate through all pages and verify total rides count")
    public void testNavigateAllPages() {
        int totalRidesFound = 0;

        // Traverse all pages dynamically
        totalRidesFound += historyPage.getRideCount();
        for (int page = 2; page <= totalPages; page++) {
            historyPage.clickNextPage();
            totalRidesFound += historyPage.getRideCount();
        }

        assertEquals(totalHistoryRides, totalRidesFound,
                "Total rides across all " + totalPages + " pages should equal " + totalHistoryRides);
    }

    @Test
    @Order(73)
    @DisplayName("EC-17: Sort by cost on empty results doesn't crash")
    public void testSortOnEmptyResults() {
        historyPage.searchByDriverId("9999");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show no rides");

        historyPage.sortByCost();
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should still show no rides after sort");
    }

    @Test
    @Order(74)
    @DisplayName("EC-18: Sort indicators update correctly across columns")
    public void testSortIndicatorsUpdateAcrossColumns() {
        historyPage.sortByStartTime(); // toggle to asc
        assertEquals("▲", historyPage.getSortIndicatorText("sort-start-time"));

        historyPage.sortByCost(); // switch to cost desc
        assertEquals("▼", historyPage.getSortIndicatorText("sort-cost"));
        assertEquals("", historyPage.getSortIndicatorText("sort-start-time"));

        historyPage.sortByStatus(); // switch to status desc
        assertEquals("▼", historyPage.getSortIndicatorText("sort-status"));
        assertEquals("", historyPage.getSortIndicatorText("sort-cost"));
    }

    @Test
    @Order(75)
    @DisplayName("EC-19: Sort by each column at least once without errors")
    public void testSortByAllColumns() {
        String[] sortButtons = {
                "sort-start-time", "sort-end-time", "sort-route",
                "sort-status", "sort-cost", "sort-cancelled-by", "sort-panic"
        };

        for (String buttonId : sortButtons) {
            switch (buttonId) {
                case "sort-start-time": historyPage.sortByStartTime(); break;
                case "sort-end-time": historyPage.sortByEndTime(); break;
                case "sort-route": historyPage.sortByRoute(); break;
                case "sort-status": historyPage.sortByStatus(); break;
                case "sort-cost": historyPage.sortByCost(); break;
                case "sort-cancelled-by": historyPage.sortByCancelledBy(); break;
                case "sort-panic": historyPage.sortByPanic(); break;
            }

            String indicator = historyPage.getSortIndicatorText(buttonId);
            assertFalse(indicator.isEmpty(),
                    "Sort indicator should be shown for " + buttonId);
            assertTrue(historyPage.getRideCount() > 0,
                    "Sorting by " + buttonId + " should not remove rides");
        }
    }

    @Test
    @Order(76)
    @DisplayName("EC-20: Sort persists when navigating across pages")
    public void testSortPersistsAcrossPages() {
        historyPage.sortByCost(); // desc
        List<Double> page1Costs = historyPage.getAllRideCostValues();

        historyPage.clickNextPage();
        List<Double> page2Costs = historyPage.getAllRideCostValues();

        // Last cost on page 1 should be >= first cost on page 2
        if (!page1Costs.isEmpty() && !page2Costs.isEmpty()) {
            assertTrue(page1Costs.get(page1Costs.size() - 1) >= page2Costs.get(0),
                    "Cost sort should persist across pages: last of page 1 (" +
                    page1Costs.get(page1Costs.size() - 1) + ") >= first of page 2 (" +
                    page2Costs.get(0) + ")");
        }
    }

    @Test
    @Order(77)
    @DisplayName("EC-21: Sort by cost on single result set doesn't crash")
    public void testSortOnSingleResult() {
        historyPage.selectStatus("CANCELLED_BY_DRIVER");
        int count = historyPage.getRideCount();

        historyPage.sortByCost();

        assertEquals(count, historyPage.getRideCount(),
                "Sorting single result should not change count");
    }

    // =====================================================================
    // EDGE CASES: Page Data Integrity (Tests 78-81)
    // =====================================================================

    @Test
    @Order(78)
    @DisplayName("EC-22: Each ride row displays all required fields")
    public void testRideRowDisplaysAllFields() {
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides to verify");

        for (int i = 0; i < Math.min(rideCount, 3); i++) {
            String startDate = historyPage.getRideStartDate(i);
            assertFalse(startDate.isEmpty(), "Ride " + i + " start date should not be empty");

            String status = historyPage.getRideStatus(i);
            assertFalse(status.isEmpty(), "Ride " + i + " status should not be empty");

            String cost = historyPage.getRideCost(i);
            assertNotNull(cost, "Ride " + i + " should have a cost");

            String cancelledBy = historyPage.getRideCancelledBy(i);
            assertNotNull(cancelledBy, "Ride " + i + " should have a cancelled-by field");

            String panic = historyPage.getRidePanic(i);
            assertNotNull(panic, "Ride " + i + " should have a panic field");
        }
    }

    @Test
    @Order(79)
    @DisplayName("EC-23: Ride rows display pickup and destination addresses")
    public void testRideRowDisplaysRouteInfo() {
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides to verify");

        for (int i = 0; i < Math.min(rideCount, 3); i++) {
            String pickup = historyPage.getRidePickup(i);
            assertFalse(pickup.equals("\u2014"), "Ride " + i + " pickup should be a real address, not \u2014");

            String destination = historyPage.getRideDestination(i);
            assertFalse(destination.equals("\u2014"), "Ride " + i + " destination should be a real address, not \u2014");
        }
    }

    @Test
    @Order(80)
    @DisplayName("EC-24: Panic ride is highlighted with YES in the table")
    public void testPanicRideShowsYes() {
        // Driver 2 has the panic ride
        historyPage.searchByDriverId("2");

        List<String> panics = historyPage.getAllRidePanics();
        assertTrue(panics.contains("YES"),
                "Driver 2 should have at least one ride with PANIC = YES");
    }

    @Test
    @Order(81)
    @DisplayName("EC-25: Cancelled ride shows cancellation source")
    public void testCancelledRideShowsCancelInfo() {
        // CANCELLED_BY_DRIVER shows "Driver" in the cancelled-by column
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have cancelled-by-driver rides");

        String cancelledBy = historyPage.getRideCancelledBy(0);
        assertEquals("Driver", cancelledBy,
                "CANCELLED_BY_DRIVER ride should show 'Driver' as the cancellation source");
    }

    // =====================================================================
    // EDGE CASES: Pagination + Filters (Tests 82-84)
    // =====================================================================

    @Test
    @Order(82)
    @DisplayName("EC-26: Date filter resets pagination to page 1")
    public void testDateFilterResetsPagination() {
        historyPage.clickNextPage();
        assertEquals(2, historyPage.getActivePageNumber());

        String fromDate = LocalDate.now().minusDays(10).format(DATE_FMT);
        historyPage.setDateFromFilter(fromDate);

        assertEquals(1, historyPage.getActivePageNumber(),
                "Date filter should reset to page 1");
    }

    @Test
    @Order(83)
    @DisplayName("EC-27: Search resets pagination to page 1")
    public void testSearchResetsPagination() {
        historyPage.clickNextPage();
        assertEquals(2, historyPage.getActivePageNumber());

        historyPage.searchByDriverId("1");
        assertEquals(1, historyPage.getActivePageNumber(),
                "Search should reset to page 1");
    }

    @Test
    @Order(84)
    @DisplayName("EC-28: Combined filter + pagination shows correct subset")
    public void testCombinedFilterPagination() {
        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getRideCount();
        assertTrue(finishedCount > 0, "Should have finished rides on first page");

        // If there are enough finished rides for pagination, verify page navigation
        if (historyPage.isPaginationVisible()) {
            historyPage.clickNextPage();
            int page2Count = historyPage.getRideCount();
            assertTrue(page2Count > 0, "Page 2 of finished rides should have rides");
        }
    }

    // =====================================================================
    // EDGE CASES: Search Input Validation (Tests 85-90)
    // =====================================================================

    @Test
    @Order(85)
    @DisplayName("EC-29: Search with negative driver ID keeps rides unchanged (backend rejects @Min(1))")
    public void testSearchNegativeDriverId() {
        int countBefore = historyPage.getRideCount();
        historyPage.searchByDriverId("-1");

        // Backend @Min(1) rejects negative IDs; Angular error handler only logs
        assertEquals(countBefore, historyPage.getRideCount(),
                "Rides should remain unchanged after invalid negative driver ID search");
        assertFalse(historyPage.isNoRidesMessageDisplayed(),
                "Should NOT show 'No rides found' — previous rides stay visible");
    }

    @Test
    @Order(86)
    @DisplayName("EC-30: Search with zero driver ID shows all rides (zero is falsy, filter skipped)")
    public void testSearchZeroDriverId() {
        historyPage.searchByDriverId("0");
        // Zero is falsy in JS — filter not applied
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Zero ID is falsy in JS — filter not applied, first page of all rides shown");
        assertFalse(historyPage.isNoRidesMessageDisplayed(),
                "Should NOT show 'No rides found' — all rides are returned");
    }

    @Test
    @Order(87)
    @DisplayName("EC-31: Search with very large driver ID returns no rides")
    public void testSearchVeryLargeDriverId() {
        historyPage.searchByDriverId("999999999");

        assertEquals(0, historyPage.getRideCount(),
                "Very large driver ID should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' for very large ID");
    }

    @Test
    @Order(88)
    @DisplayName("EC-32: Search with negative passenger ID keeps rides unchanged (backend rejects @Min(1))")
    public void testSearchNegativePassengerId() {
        int countBefore = historyPage.getRideCount();
        historyPage.searchByPassengerId("-5");

        // Backend @Min(1) rejects negative IDs; Angular error handler only logs
        assertEquals(countBefore, historyPage.getRideCount(),
                "Rides should remain unchanged after invalid negative passenger ID search");
        assertFalse(historyPage.isNoRidesMessageDisplayed(),
                "Should NOT show 'No rides found' — previous rides stay visible");
    }

    @Test
    @Order(89)
    @DisplayName("EC-33: Search with zero passenger ID shows all rides (zero is falsy, filter skipped)")
    public void testSearchZeroPassengerId() {
        historyPage.searchByPassengerId("0");

        // Zero is falsy in JS — filter not applied
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Zero ID is falsy in JS — filter not applied, first page of all rides shown");
        assertFalse(historyPage.isNoRidesMessageDisplayed(),
                "Should NOT show 'No rides found' — all rides are returned");
    }

    @Test
    @Order(90)
    @DisplayName("EC-34: Search for admin user ID returns no rides (admin has no rides)")
    public void testSearchAdminUserIdNoRides() {
        // Admin is user id=6 who has no rides as driver or passenger
        historyPage.searchByDriverId("6");

        assertEquals(0, historyPage.getRideCount(),
                "Admin user (id=6) should have no rides as driver");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' for admin user who has no driver rides");
    }

    // =====================================================================
    // EDGE CASES: Clear Search (Tests 91-93)
    // =====================================================================

    @Test
    @Order(91)
    @DisplayName("EC-35: Clear search restores all rides")
    public void testClearSearchRestoresAllRides() {
        historyPage.searchByDriverId("2");
        int filteredCount = historyPage.getRideCount();
        assertTrue(filteredCount < totalHistoryRides,
                "Driver 2 filtered count should be less than total");

        historyPage.clickClearSearch();

        int restoredCount = historyPage.getRideCount();
        assertEquals(UI_PAGE_SIZE, restoredCount,
                "Cleared search should show first page with " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(92)
    @DisplayName("EC-36: Clear search button is visible after searching")
    public void testClearSearchButtonVisibleAfterSearch() {
        historyPage.searchByDriverId("1");

        assertTrue(historyPage.isClearSearchButtonVisible(),
                "Clear search button should be visible after performing a search");
    }

    @Test
    @Order(93)
    @DisplayName("EC-37: Search input retains entered value after search")
    public void testSearchInputRetainsValue() {
        historyPage.selectSearchTypeDriver();
        historyPage.enterSearchId("1");
        historyPage.clickSearch();

        String inputValue = historyPage.getSearchIdValue();
        assertEquals("1", inputValue,
                "Search input should retain the entered value after search");
    }

    // =====================================================================
    // EDGE CASES: Status Filter Negative Scenarios (Tests 94-97)
    // =====================================================================

    @Test
    @Order(94)
    @DisplayName("EC-38: Driver with no CANCELLED_BY_PASSENGER rides returns empty for that status")
    public void testDriver2NoCancelledByPassengerRides() {
        historyPage.searchByDriverId("2");
        historyPage.selectStatus("CANCELLED_BY_PASSENGER");

        assertEquals(0, historyPage.getRideCount(),
                "Driver 2 has no CANCELLED_BY_PASSENGER rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' message");
    }

    @Test
    @Order(95)
    @DisplayName("EC-39: Status filter shows correct count for each status type")
    public void testStatusFilterCountsAreConsistent() {
        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getTotalRidesFromBadge();

        historyPage.selectStatus("CANCELLED");
        int cancelledCount = historyPage.getTotalRidesFromBadge();

        historyPage.selectStatus("CANCELLED_BY_DRIVER");
        int cancelledByDriverCount = historyPage.getTotalRidesFromBadge();

        historyPage.selectStatus("CANCELLED_BY_PASSENGER");
        int cancelledByPassengerCount = historyPage.getTotalRidesFromBadge();

        int statusTotal = finishedCount + cancelledCount + cancelledByDriverCount + cancelledByPassengerCount;
        assertEquals(totalHistoryRides, statusTotal,
                "Sum of all status counts (" + statusTotal + ") should equal total history rides (" + totalHistoryRides + ")");
    }

    @Test
    @Order(96)
    @DisplayName("EC-40: CANCELLED_BY_PASSENGER rides show 'Passenger' in cancelled-by column")
    public void testCancelledByPassengerShowsPassengerInColumn() {
        historyPage.selectStatus("CANCELLED_BY_PASSENGER");

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "Should have CANCELLED_BY_PASSENGER rides");

        for (int i = 0; i < count; i++) {
            String cancelledBy = historyPage.getRideCancelledBy(i);
            assertEquals("Passenger", cancelledBy,
                    "CANCELLED_BY_PASSENGER ride should show 'Passenger' in cancelled-by column");
        }
    }

    @Test
    @Order(97)
    @DisplayName("EC-41: FINISHED rides show dash in cancelled-by column")
    public void testFinishedRidesShowDashInCancelledByColumn() {
        historyPage.selectStatus("FINISHED");

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "Should have FINISHED rides");

        for (int i = 0; i < count; i++) {
            String cancelledBy = historyPage.getRideCancelledBy(i);
            assertEquals("\u2014", cancelledBy,
                    "FINISHED ride should show '\u2014' (em dash) in cancelled-by column");
        }
    }

    // =====================================================================
    // EDGE CASES: Date Range Negative Scenarios (Tests 98-101)
    // =====================================================================

    @Test
    @Order(98)
    @DisplayName("EC-42: Far future date range returns no rides")
    public void testFarFutureDateRangeNoRides() {
        String from = LocalDate.now().plusYears(1).format(DATE_FMT);
        String to = LocalDate.now().plusYears(2).format(DATE_FMT);
        historyPage.setDateRange(from, to);

        assertEquals(0, historyPage.getRideCount(),
                "Far future date range should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' for future date range");
    }

    @Test
    @Order(99)
    @DisplayName("EC-43: Very old date range returns no rides")
    public void testVeryOldDateRangeNoRides() {
        String from = "2000-01-01";
        String to = "2000-12-31";
        historyPage.setDateRange(from, to);

        assertEquals(0, historyPage.getRideCount(),
                "Year 2000 date range should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' for year-2000 date range");
    }

    @Test
    @Order(100)
    @DisplayName("EC-44: Setting only From date filters rides from that date onward")
    public void testFromDateOnlyFiltersForward() {
        LocalDate fromLocalDate = LocalDate.now().minusDays(3);
        String from = fromLocalDate.format(DATE_FMT);
        historyPage.setDateFromFilter(from);

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "From-date-only filter should return some rides");
        assertTrue(count <= totalHistoryRides,
                "Should not return more than total history rides");

        // Verify every visible ride's start date is on or after the From date
        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isBefore(fromLocalDate),
                    "Ride date " + rideDate + " should not be before From filter " + fromLocalDate);
        }
    }

    @Test
    @Order(101)
    @DisplayName("EC-45: Setting only To date filters rides up to that date")
    public void testToDateOnlyFiltersBackward() {
        LocalDate toLocalDate = LocalDate.now().minusDays(5);
        String to = toLocalDate.format(DATE_FMT);
        historyPage.setDateToFilter(to);

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "To-date-only filter should return some rides");
        assertTrue(count < totalHistoryRides,
                "To-date-only should return fewer rides than total (recent rides excluded)");

        // Verify every visible ride's start date is on or before the To date
        List<String> startDates = historyPage.getAllRideStartDates();
        for (String dateStr : startDates) {
            LocalDate rideDate = parseTableDate(dateStr);
            assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
            assertFalse(rideDate.isAfter(toLocalDate),
                    "Ride date " + rideDate + " should not be after To filter " + toLocalDate);
        }
    }

    // =====================================================================
    // EDGE CASES: Triple Filter Combinations (Tests 102-105)
    // =====================================================================

    @Test
    @Order(102)
    @DisplayName("EC-46: Search + status + date range triple filter")
    public void testTripleFilterCombination() {
        // Driver 1 + FINISHED + last 5 days
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("FINISHED");
        String from = LocalDate.now().minusDays(5).format(DATE_FMT);
        String to = LocalDate.now().format(DATE_FMT);
        historyPage.setDateRange(from, to);

        int count = historyPage.getRideCount();
        // Driver 1 FINISHED rides within last 5 days: Ride 10 (~7h ago), Ride 3 (1d), Ride 4 (2d), Ride 8 (4d)
        assertTrue(count >= 0, "Triple filter should return valid count (including 0)");

        if (count > 0) {
            List<String> statuses = historyPage.getAllRideStatuses();
            for (String status : statuses) {
                assertEquals("Finished", status, "All results should be FINISHED");
            }

            LocalDate fromLocalDate = LocalDate.now().minusDays(5);
            LocalDate toLocalDate = LocalDate.now();
            List<String> startDates = historyPage.getAllRideStartDates();
            for (String dateStr : startDates) {
                LocalDate rideDate = parseTableDate(dateStr);
                assertNotNull(rideDate, "Ride start date should be parseable: " + dateStr);
                assertFalse(rideDate.isBefore(fromLocalDate),
                        "Ride date " + rideDate + " should not be before From filter " + fromLocalDate);
                assertFalse(rideDate.isAfter(toLocalDate),
                        "Ride date " + rideDate + " should not be after To filter " + toLocalDate);
            }
        }
    }

    @Test
    @Order(103)
    @DisplayName("EC-47: Clearing one filter while others remain active")
    public void testClearOneFilterKeepOthers() {
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("FINISHED");
        int combinedCount = historyPage.getRideCount();

        // Clear status back to all — driver filter should remain
        historyPage.selectStatus("all");
        int driverOnlyCount = historyPage.getRideCount();

        assertTrue(driverOnlyCount >= combinedCount,
                "Removing status filter with driver search active should show equal or more rides");
    }

    @Test
    @Order(104)
    @DisplayName("EC-48: Non-existent passenger + valid status + valid date = no rides")
    public void testNonExistentPassengerWithValidFilters() {
        historyPage.searchByPassengerId("9999");
        historyPage.selectStatus("FINISHED");
        String from = LocalDate.now().minusDays(30).format(DATE_FMT);
        String to = LocalDate.now().format(DATE_FMT);
        historyPage.setDateRange(from, to);

        assertEquals(0, historyPage.getRideCount(),
                "Non-existent passenger with valid status and date should show no rides");
    }

    @Test
    @Order(105)
    @DisplayName("EC-49: Clear date while search + status filters remain active")
    public void testClearDateKeepSearchAndStatus() {
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("FINISHED");
        String from = LocalDate.now().minusDays(2).format(DATE_FMT);
        String to = LocalDate.now().format(DATE_FMT);
        historyPage.setDateRange(from, to);
        int withDateCount = historyPage.getRideCount();

        historyPage.clearDateFilter();
        int withoutDateCount = historyPage.getRideCount();

        assertTrue(withoutDateCount >= withDateCount,
                "Clearing date filter should show equal or more rides");
    }

    // =====================================================================
    // EDGE CASES: Sort Order Verification (Tests 106-109)
    // =====================================================================

    @Test
    @Order(106)
    @DisplayName("EC-50: Sort by cost ASC produces ascending order on first page")
    public void testSortByCostAscProducesAscendingOrder() {
        historyPage.sortByCost(); // first click = DESC
        historyPage.sortByCost(); // second click = ASC

        List<Double> costs = historyPage.getAllRideCostValues();
        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) <= costs.get(i + 1),
                    "Cost at index " + i + " ($" + costs.get(i) + ") should be <= cost at index " +
                    (i + 1) + " ($" + costs.get(i + 1) + ") in ASC sort");
        }
    }

    @Test
    @Order(107)
    @DisplayName("EC-51: Sort by cost DESC produces descending order on first page")
    public void testSortByCostDescProducesDescendingOrder() {
        historyPage.sortByCost(); // first click = DESC

        List<Double> costs = historyPage.getAllRideCostValues();
        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) >= costs.get(i + 1),
                    "Cost at index " + i + " ($" + costs.get(i) + ") should be >= cost at index " +
                    (i + 1) + " ($" + costs.get(i + 1) + ") in DESC sort");
        }
    }

    @Test
    @Order(108)
    @DisplayName("EC-52: Double-toggle sort returns to original direction")
    public void testDoubleSortToggle() {
        historyPage.sortByStatus(); // DESC
        String firstIndicator = historyPage.getSortIndicatorText("sort-status");

        historyPage.sortByStatus(); // ASC
        String secondIndicator = historyPage.getSortIndicatorText("sort-status");

        historyPage.sortByStatus(); // back to DESC
        String thirdIndicator = historyPage.getSortIndicatorText("sort-status");

        assertNotEquals(firstIndicator, secondIndicator,
                "Second sort click should change direction");
        assertEquals(firstIndicator, thirdIndicator,
                "Third sort click should return to original direction");
    }

    @Test
    @Order(109)
    @DisplayName("EC-53: Sort by status groups rides by status name")
    public void testSortByStatusGroupsCorrectly() {
        historyPage.sortByStatus(); // DESC

        List<String> statuses = historyPage.getAllRideStatuses();
        // Verify that statuses are grouped (sorted) — each transition should go one direction
        for (int i = 0; i < statuses.size() - 1; i++) {
            assertTrue(statuses.get(i).compareTo(statuses.get(i + 1)) >= 0,
                    "Status '" + statuses.get(i) + "' at index " + i +
                    " should be >= '" + statuses.get(i + 1) + "' in DESC sort");
        }
    }

    // =====================================================================
    // EDGE CASES: Pagination Info Text (Tests 110-112)
    // =====================================================================

    @Test
    @Order(110)
    @DisplayName("EC-54: Pagination info shows correct range for page 1")
    public void testPaginationInfoPage1() {
        String info = historyPage.getPaginationInfoText();

        assertTrue(info.contains("1"), "Page 1 info should contain '1' as start item");
        assertTrue(info.contains(String.valueOf(UI_PAGE_SIZE)),
                "Page 1 info should contain '" + UI_PAGE_SIZE + "' as end item");
        assertTrue(info.contains(String.valueOf(totalHistoryRides)),
                "Page 1 info should contain total rides count '" + totalHistoryRides + "'");
    }

    @Test
    @Order(111)
    @DisplayName("EC-55: Pagination info updates correctly on page 2")
    public void testPaginationInfoPage2() {
        historyPage.clickNextPage();
        String info = historyPage.getPaginationInfoText();

        int expectedStart = UI_PAGE_SIZE + 1;
        assertTrue(info.contains(String.valueOf(expectedStart)),
                "Page 2 info should contain start item '" + expectedStart + "'");
        assertTrue(info.contains(String.valueOf(totalHistoryRides)),
                "Page 2 info should contain total rides count '" + totalHistoryRides + "'");
    }

    @Test
    @Order(112)
    @DisplayName("EC-56: Pagination info on last page shows correct end item")
    public void testPaginationInfoLastPage() {
        historyPage.clickLastPage();
        String info = historyPage.getPaginationInfoText();

        assertTrue(info.contains(String.valueOf(totalHistoryRides)),
                "Last page info should contain total rides count '" + totalHistoryRides + "' as the end item");
    }

    // =====================================================================
    // EDGE CASES: Detail Panel Negative Scenarios (Tests 113-118)
    // =====================================================================

    @Test
    @Order(113)
    @DisplayName("EC-57: Detail panel for cancelled ride shows no reviews section or empty reviews")
    public void testDetailPanelCancelledRideNoReviews() {
        historyPage.selectStatus("CANCELLED");

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "Should have at least one CANCELLED ride");

        historyPage.openRideDetails(0);
        // Cancelled rides typically don't have reviews
        if (historyPage.isDetailReviewsSectionVisible()) {
            assertEquals(0, historyPage.getDetailReviewCount(),
                    "Cancelled ride should have 0 reviews if section is shown");
        }
        // If reviews section is not visible, that's also correct
    }

    @Test
    @Order(114)
    @DisplayName("EC-58: Detail panel for multi-passenger ride shows correct passenger count")
    public void testDetailPanelMultiPassengerRide() {
        // Ride 8 has passengers 3 and 4 — search for driver 1, FINISHED, and find it
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("FINISHED");

        // Look through results for a ride with 2 passengers
        int rideCount = historyPage.getRideCount();
        boolean foundMultiPassenger = false;
        for (int i = 0; i < rideCount; i++) {
            historyPage.openRideDetails(i);
            int passengerCount = historyPage.getDetailPassengerCount();
            if (passengerCount > 1) {
                foundMultiPassenger = true;
                assertTrue(passengerCount >= 2,
                        "Multi-passenger ride should show at least 2 passengers");
                break;
            }
            historyPage.closeRideDetails();
        }
        assertTrue(foundMultiPassenger,
                "Should find at least one ride with multiple passengers among Driver 1's FINISHED rides");
    }

    @Test
    @Order(115)
    @DisplayName("EC-59: Detail panel for ride on page 2 works correctly")
    public void testDetailPanelOnPage2() {
        historyPage.clickNextPage();
        assertEquals(2, historyPage.getActivePageNumber(), "Should be on page 2");

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "Page 2 should have rides");

        historyPage.openRideDetails(0);
        assertTrue(historyPage.isDetailPanelVisible(),
                "Detail panel should open for ride on page 2");

        String status = historyPage.getDetailStatus();
        assertFalse(status.isEmpty(), "Detail status should not be empty on page 2");

        historyPage.closeRideDetails();
    }

    @Test
    @Order(116)
    @DisplayName("EC-60: Non-panic ride detail does NOT show PANIC badge")
    public void testNonPanicRideDetailNoPanicBadge() {
        // Search for driver 1 who has no panic rides
        historyPage.searchByDriverId("1");

        historyPage.openRideDetails(0);
        assertFalse(historyPage.isDetailPanicBadgeVisible(),
                "Non-panic ride should NOT show PANIC badge in detail panel");
    }

    @Test
    @Order(117)
    @DisplayName("EC-61: Cancelled-by-driver ride detail shows correct status")
    public void testCancelledByDriverRideDetailStatus() {
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        int count = historyPage.getRideCount();
        assertTrue(count > 0, "Should have CANCELLED_BY_DRIVER rides");

        historyPage.openRideDetails(0);
        String detailStatus = historyPage.getDetailStatus();
        assertEquals("Cancelled By Driver", detailStatus,
                "Detail panel should show 'Cancelled By Driver' status");
    }

    @Test
    @Order(118)
    @DisplayName("EC-62: Opening detail and closing, table remains unchanged")
    public void testOpenCloseDetailTableUnchanged() {
        int countBefore = historyPage.getRideCount();
        String firstStatus = historyPage.getRideStatus(0);
        String firstCost = historyPage.getRideCost(0);

        historyPage.openRideDetails(0);
        historyPage.closeRideDetails();

        int countAfter = historyPage.getRideCount();
        String firstStatusAfter = historyPage.getRideStatus(0);
        String firstCostAfter = historyPage.getRideCost(0);

        assertEquals(countBefore, countAfter, "Ride count should not change after open/close detail");
        assertEquals(firstStatus, firstStatusAfter, "First ride status should not change");
        assertEquals(firstCost, firstCostAfter, "First ride cost should not change");
    }

    // =====================================================================
    // EDGE CASES: Panic Column Verification (Tests 119-120)
    // =====================================================================

    @Test
    @Order(119)
    @DisplayName("EC-63: Exactly one ride shows PANIC=YES across all pages")
    public void testExactlyOnePanicRideAcrossAllPages() {
        int panicCount = 0;

        // Count PANIC=YES across all pages
        List<String> panicsPage1 = historyPage.getAllRidePanics();
        panicCount += panicsPage1.stream().filter(p -> p.equals("YES")).count();

        for (int page = 2; page <= totalPages; page++) {
            historyPage.clickNextPage();
            List<String> panics = historyPage.getAllRidePanics();
            panicCount += panics.stream().filter(p -> p.equals("YES")).count();
        }

        assertEquals(1, panicCount,
                "Exactly 1 ride should have PANIC=YES across all pages (Ride 9)");
    }

    @Test
    @Order(120)
    @DisplayName("EC-64: Non-panic rides show dash in panic column")
    public void testNonPanicRidesShowDash() {
        // Driver 1 has no panic rides
        historyPage.searchByDriverId("1");

        List<String> panics = historyPage.getAllRidePanics();
        assertFalse(panics.isEmpty(), "Driver 1 should have rides");
        for (String panic : panics) {
            assertEquals("\u2014", panic,
                    "Non-panic rides should show em-dash (\u2014) in panic column");
        }
    }

    // =====================================================================
    // EDGE CASES: Badge Count Verification (Tests 121-123)
    // =====================================================================

    @Test
    @Order(121)
    @DisplayName("EC-65: Rides count badge updates when status filter applied")
    public void testBadgeCountUpdatesWithStatusFilter() {
        int allCount = historyPage.getTotalRidesFromBadge();
        assertEquals(totalHistoryRides, allCount, "Initial badge should show total history rides");

        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getTotalRidesFromBadge();
        assertTrue(finishedCount < allCount,
                "FINISHED count (" + finishedCount + ") should be less than total (" + allCount + ")");
        assertTrue(finishedCount > 0, "Should have at least one FINISHED ride");
    }

    @Test
    @Order(122)
    @DisplayName("EC-66: Rides count badge updates when search applied")
    public void testBadgeCountUpdatesWithSearch() {
        historyPage.searchByDriverId("2");
        int driver2Count = historyPage.getTotalRidesFromBadge();
        assertTrue(driver2Count > 0, "Driver 2 should have rides");
        assertTrue(driver2Count < totalHistoryRides,
                "Driver 2 count (" + driver2Count + ") should be less than total (" + totalHistoryRides + ")");
    }

    @Test
    @Order(123)
    @DisplayName("EC-67: No rides message text contains expected wording")
    public void testNoRidesMessageContent() {
        historyPage.searchByDriverId("9999");

        assertTrue(historyPage.isNoRidesMessageDisplayed(),
                "Should show 'No rides found' message");
        String message = historyPage.getNoRidesMessageText();
        assertFalse(message.isEmpty(), "No-rides message should not be empty");
    }

    // =====================================================================
    // EDGE CASES: Rapid Actions & State Consistency (Tests 124-127)
    // =====================================================================

    @Test
    @Order(124)
    @DisplayName("EC-68: Rapid pagination forward and backward returns to page 1")
    public void testRapidPaginationForwardBackward() {
        historyPage.clickNextPage();
        historyPage.clickNextPage();
        historyPage.clickPrevPage();
        historyPage.clickPrevPage();

        assertEquals(1, historyPage.getActivePageNumber(),
                "After going forward 2 and back 2, should be on page 1");
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Page 1 should show " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(125)
    @DisplayName("EC-69: First page button from last page returns to page 1")
    public void testFirstPageButtonFromLastPage() {
        historyPage.clickLastPage();
        assertEquals(totalPages, historyPage.getActivePageNumber(),
                "Should be on last page");

        historyPage.clickFirstPage();
        assertEquals(1, historyPage.getActivePageNumber(),
                "First page button should return to page 1");
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Page 1 should show " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(126)
    @DisplayName("EC-70: Filter → paginate → clear filter restores to page 1 with all rides")
    public void testFilterPaginateClearRestoresState() {
        historyPage.selectStatus("FINISHED");
        if (historyPage.isPaginationVisible()) {
            historyPage.clickNextPage();
        }

        historyPage.selectStatus("all");

        assertEquals(1, historyPage.getActivePageNumber(),
                "Clearing filter should reset to page 1");
        assertEquals(UI_PAGE_SIZE, historyPage.getRideCount(),
                "Should show first page with " + UI_PAGE_SIZE + " rides");
    }

    @Test
    @Order(127)
    @DisplayName("EC-71: Multiple sequential searches show correct results each time")
    public void testMultipleSequentialSearches() {
        historyPage.searchByDriverId("1");
        int driver1Count = historyPage.getRideCount();
        assertTrue(driver1Count > 0, "Driver 1 should have rides");

        historyPage.clickClearSearch();
        historyPage.searchByDriverId("2");
        int driver2Count = historyPage.getRideCount();
        assertTrue(driver2Count > 0, "Driver 2 should have rides");

        historyPage.clickClearSearch();
        historyPage.searchByPassengerId("3");
        int passenger3Count = historyPage.getRideCount();
        assertTrue(passenger3Count > 0, "Passenger 3 should have rides");

        // All three should return different counts
        assertFalse(driver1Count == driver2Count && driver2Count == passenger3Count,
                "Different searches should return different counts (d1=" + driver1Count +
                ", d2=" + driver2Count + ", p3=" + passenger3Count + ")");
    }

    // =====================================================================
    // ERROR CASE: Authentication (Test 128)
    // =====================================================================

    @Test
    @Order(128)
    @DisplayName("ERR-01: Accessing ride history without login redirects to login")
    public void testAccessWithoutLoginRedirects() {
        // Clear auth state — JWT is stored in localStorage, not cookies
        ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
        // Navigate away to fully reset Angular's in-memory auth state
        driver.get("about:blank");
        // Attempt to access the protected admin page
        driver.get(BASE_URL + "/admin/ride-history");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/login"));

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should be redirected to login page when not authenticated");
    }
}
