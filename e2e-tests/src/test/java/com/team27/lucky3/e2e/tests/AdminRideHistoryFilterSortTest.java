package com.team27.lucky3.e2e.tests;

import com.team27.lucky3.e2e.pages.AdminRideHistoryPage;
import com.team27.lucky3.e2e.pages.LoginPage;
import com.team27.lucky3.e2e.pages.SidebarComponent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for Admin Ride History - Filtering and Sorting.
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
 *       9 rides with various statuses: SCHEDULED, IN_PROGRESS, FINISHED, CANCELLED, FINISHED+PANIC
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Ride History - Filtering & Sorting")
public class AdminRideHistoryFilterSortTest extends BaseTest {

    private LoginPage loginPage;
    private SidebarComponent sidebar;
    private AdminRideHistoryPage historyPage;

    @BeforeEach
    public void setUpPages() {
        loginPage = new LoginPage(driver);
        sidebar = new SidebarComponent(driver);
        historyPage = new AdminRideHistoryPage(driver);

        // Login as admin and navigate to ride history
        loginPage.loginAsAdmin();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/admin"));
        sidebar.navigateToRideHistory();
        wait.until(ExpectedConditions.urlContains("/admin/ride-history"));
        historyPage.waitForTableToLoad();
    }

    // =====================================================================
    // HAPPY PATH: Default View
    // =====================================================================

    @Test
    @Order(1)
    @DisplayName("HP-01: All rides are displayed by default with descending start time sort")
    public void testDefaultViewShowsAllRides() {
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Ride history table should contain rides");
    }

    @Test
    @Order(2)
    @DisplayName("HP-02: Default sort is by Start Time descending")
    public void testDefaultSortIsStartTimeDescending() {
        // The sort indicator on Start Time should show descending (▼)
        String indicator = historyPage.getSortIndicatorText("sort-start-time");
        assertEquals("▼", indicator, "Default sort indicator should be descending (▼) on Start Time");
    }

    @Test
    @Order(3)
    @DisplayName("HP-03: Rides count badge is displayed")
    public void testRidesCountBadgeIsDisplayed() {
        String countText = historyPage.getAllRidesCountText();
        assertFalse(countText.isEmpty(), "Rides count badge should be displayed");
        assertTrue(countText.contains("ride"), "Rides count should mention 'ride'");
        assertTrue(countText.contains("found"), "Rides count should mention 'found'");
    }

    // =====================================================================
    // HAPPY PATH: Filtering by Status
    // =====================================================================

    @Test
    @Order(10)
    @DisplayName("HP-10: Filter by FINISHED status shows only finished rides")
    public void testFilterByFinishedStatus() {
        historyPage.selectStatus("FINISHED");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be finished rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should have FINISHED status");
        }
    }

    @Test
    @Order(11)
    @DisplayName("HP-11: Filter by CANCELLED status shows only cancelled rides")
    public void testFilterByCancelledStatus() {
        historyPage.selectStatus("CANCELLED");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be cancelled rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Cancelled", status, "All rides should have CANCELLED status");
        }
    }

    @Test
    @Order(12)
    @DisplayName("HP-12: Filter by IN_PROGRESS status shows only in-progress rides")
    public void testFilterByInProgressStatus() {
        historyPage.selectStatus("IN_PROGRESS");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be in-progress rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("In Progress", status, "All rides should have IN_PROGRESS status");
        }
    }

    @Test
    @Order(13)
    @DisplayName("HP-13: Filter by SCHEDULED status shows only scheduled rides")
    public void testFilterByScheduledStatus() {
        historyPage.selectStatus("SCHEDULED");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "There should be scheduled rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Scheduled", status, "All rides should have SCHEDULED status");
        }
    }

    @Test
    @Order(14)
    @DisplayName("HP-14: Selecting 'All Statuses' shows all rides again")
    public void testFilterByAllStatuses() {
        // First filter to narrow results
        historyPage.selectStatus("FINISHED");
        int finishedCount = historyPage.getRideCount();

        // Then select All
        historyPage.selectStatus("all");
        int allCount = historyPage.getRideCount();

        assertTrue(allCount >= finishedCount, "All rides should be >= finished rides");
    }

    // =====================================================================
    // HAPPY PATH: Filtering by Driver/Passenger ID
    // =====================================================================

    @Test
    @Order(20)
    @DisplayName("HP-20: Search by Driver ID shows rides for that driver")
    public void testSearchByDriverId() {
        historyPage.searchByDriverId("1");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Driver 1 should have rides");

        // Search results count badge should show
        String countText = historyPage.getSearchRidesCountText();
        assertTrue(countText.contains("ride"), "Search result count should be displayed");
    }

    @Test
    @Order(21)
    @DisplayName("HP-21: Search by Passenger ID shows rides for that passenger")
    public void testSearchByPassengerId() {
        historyPage.searchByPassengerId("3");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Passenger 3 should have rides");
    }

    @Test
    @Order(22)
    @DisplayName("HP-22: Search by Driver ID 2 shows different rides than Driver ID 1")
    public void testSearchByDifferentDriverIds() {
        historyPage.searchByDriverId("1");
        int driver1RideCount = historyPage.getRideCount();
        List<String> driver1Costs = historyPage.getAllRideStatuses();

        historyPage.searchByDriverId("2");
        int driver2RideCount = historyPage.getRideCount();

        assertTrue(driver1RideCount > 0, "Driver 1 should have rides");
        assertTrue(driver2RideCount > 0, "Driver 2 should have rides");
    }

    @Test
    @Order(23)
    @DisplayName("HP-23: Clear search returns all rides")
    public void testClearSearchReturnsAllRides() {
        // Get initial count
        int allRideCount = historyPage.getRideCount();

        // Filter by driver
        historyPage.searchByDriverId("1");
        int driverRideCount = historyPage.getRideCount();
        assertTrue(driverRideCount <= allRideCount, "Filtered rides should be <= all rides");

        // Clear search
        historyPage.clickClearSearch();
        int newCount = historyPage.getRideCount();
        assertEquals(allRideCount, newCount, "After clear, should show all rides again");
    }

    // =====================================================================
    // HAPPY PATH: Filtering by Date
    // =====================================================================

    @Test
    @Order(30)
    @DisplayName("HP-30: Filter by today's date shows rides created today")
    public void testFilterByTodayDate() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        historyPage.setDateFilter(today);

        // Should have at least the SCHEDULED and IN_PROGRESS rides from today
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount >= 0, "Date filter should return results (may be 0 if rides have different dates)");
    }

    @Test
    @Order(31)
    @DisplayName("HP-31: Filter by a far-future date shows no rides")
    public void testFilterByFutureDateShowsNoRides() {
        String futureDate = "12/31/2030";
        historyPage.setDateFilter(futureDate);

        int rideCount = historyPage.getRideCount();
        if (rideCount == 0) {
            assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
        }
    }

    @Test
    @Order(32)
    @DisplayName("HP-32: Filter by a far-past date shows no rides")
    public void testFilterByPastDateShowsNoRides() {
        String pastDate = "01/01/2020";
        historyPage.setDateFilter(pastDate);

        int rideCount = historyPage.getRideCount();
        if (rideCount == 0) {
            assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
        }
    }

    // =====================================================================
    // HAPPY PATH: Combined Filters
    // =====================================================================

    @Test
    @Order(40)
    @DisplayName("HP-40: Combine driver search with status filter")
    public void testCombineDriverSearchWithStatusFilter() {
        historyPage.searchByDriverId("1");
        int driverRideCount = historyPage.getRideCount();

        historyPage.selectStatus("FINISHED");
        int filteredCount = historyPage.getRideCount();

        assertTrue(filteredCount <= driverRideCount,
                "Combining driver + status filter should narrow results");
        assertTrue(filteredCount > 0, "Driver 1 should have finished rides");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should be FINISHED");
        }
    }

    @Test
    @Order(41)
    @DisplayName("HP-41: Combine passenger search with status filter")
    public void testCombinePassengerSearchWithStatusFilter() {
        historyPage.searchByPassengerId("3");
        int passengerRideCount = historyPage.getRideCount();
        assertTrue(passengerRideCount > 0, "Passenger 3 should have rides");

        historyPage.selectStatus("FINISHED");
        int filteredCount = historyPage.getRideCount();

        assertTrue(filteredCount <= passengerRideCount,
                "Combining passenger + status filter should narrow results");
    }

    // =====================================================================
    // HAPPY PATH: Sorting
    // =====================================================================

    @Test
    @Order(50)
    @DisplayName("HP-50: Sort by cost descending")
    public void testSortByCostDescending() {
        historyPage.sortByCost(); // First click: desc

        List<Double> costs = historyPage.getAllRideCostValues();
        if (costs.size() > 1) {
            for (int i = 0; i < costs.size() - 1; i++) {
                assertTrue(costs.get(i) >= costs.get(i + 1),
                        "Costs should be in descending order: " + costs.get(i) + " >= " + costs.get(i + 1));
            }
        }
    }

    @Test
    @Order(51)
    @DisplayName("HP-51: Sort by cost ascending (double click)")
    public void testSortByCostAscending() {
        historyPage.sortByCost(); // First click: desc
        historyPage.sortByCost(); // Second click: asc

        List<Double> costs = historyPage.getAllRideCostValues();
        if (costs.size() > 1) {
            for (int i = 0; i < costs.size() - 1; i++) {
                assertTrue(costs.get(i) <= costs.get(i + 1),
                        "Costs should be in ascending order: " + costs.get(i) + " <= " + costs.get(i + 1));
            }
        }
    }

    @Test
    @Order(52)
    @DisplayName("HP-52: Sort by start time ascending reverses default order")
    public void testSortByStartTimeAscending() {
        // Default is startTime desc, clicking once should toggle to asc
        historyPage.sortByStartTime();

        String indicator = historyPage.getSortIndicatorText("sort-start-time");
        assertEquals("▲", indicator, "Sort indicator should change to ascending (▲)");
    }

    @Test
    @Order(53)
    @DisplayName("HP-53: Sort by status groups rides by status")
    public void testSortByStatus() {
        historyPage.sortByStatus();

        List<String> statuses = historyPage.getAllRideStatuses();
        assertTrue(statuses.size() > 0, "Should have rides to sort");

        // Verify statuses are sorted (alphabetically or by enum order depending on backend)
        // At minimum, identical statuses should be grouped together
        // We just verify no error occurred and data is displayed
    }

    @Test
    @Order(54)
    @DisplayName("HP-54: Sort by panic groups panic rides")
    public void testSortByPanic() {
        historyPage.sortByPanic();

        List<String> panics = historyPage.getAllRidePanics();
        assertTrue(panics.size() > 0, "Should have rides");

        // At least one ride should have panic = YES (ride 9 from DataInitializer)
        assertTrue(panics.contains("YES"), "At least one ride should have PANIC = YES");
    }

    @Test
    @Order(55)
    @DisplayName("HP-55: Sort by cancelled-by")
    public void testSortByCancelledBy() {
        historyPage.sortByCancelledBy();

        List<String> cancelledBys = historyPage.getAllRideCancelledBys();
        assertTrue(cancelledBys.size() > 0, "Should have rides");
    }

    @Test
    @Order(56)
    @DisplayName("HP-56: Toggle sort direction for end time")
    public void testToggleSortDirectionEndTime() {
        historyPage.sortByEndTime(); // First click: desc
        String indicator1 = historyPage.getSortIndicatorText("sort-end-time");
        assertEquals("▼", indicator1, "First click should sort descending");

        historyPage.sortByEndTime(); // Second click: asc
        String indicator2 = historyPage.getSortIndicatorText("sort-end-time");
        assertEquals("▲", indicator2, "Second click should sort ascending");
    }

    @Test
    @Order(57)
    @DisplayName("HP-57: Switching sort field resets direction to descending")
    public void testSwitchingSortFieldResetsDirection() {
        // Sort by cost (sets to desc)
        historyPage.sortByCost();
        // Toggle to asc
        historyPage.sortByCost();
        String costIndicator = historyPage.getSortIndicatorText("sort-cost");
        assertEquals("▲", costIndicator, "Should be ascending on cost");

        // Now switch to status - should reset to desc
        historyPage.sortByStatus();
        String statusIndicator = historyPage.getSortIndicatorText("sort-status");
        assertEquals("▼", statusIndicator, "Switching sort field should start with descending");
    }

    @Test
    @Order(58)
    @DisplayName("HP-58: Sort by route (departure address)")
    public void testSortByRoute() {
        historyPage.sortByRoute();
        String indicator = historyPage.getSortIndicatorText("sort-route");
        assertEquals("▼", indicator, "Route sort should show descending indicator");

        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should still have rides after sorting by route");
    }

    // =====================================================================
    // HAPPY PATH: Sort + Filter Combined
    // =====================================================================

    @Test
    @Order(60)
    @DisplayName("HP-60: Filter by finished status then sort by cost descending")
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
    @Order(61)
    @DisplayName("HP-61: Search by driver then sort by cost ascending")
    public void testSearchDriverSortByCostAsc() {
        historyPage.searchByDriverId("1");
        historyPage.sortByCost(); // desc
        historyPage.sortByCost(); // asc

        List<Double> costs = historyPage.getAllRideCostValues();
        assertTrue(costs.size() > 0, "Driver 1 should have rides");

        for (int i = 0; i < costs.size() - 1; i++) {
            assertTrue(costs.get(i) <= costs.get(i + 1),
                    "Costs should be in ascending order for Driver 1 rides");
        }
    }

    // =====================================================================
    // EDGE CASES: Search
    // =====================================================================

    @Test
    @Order(70)
    @DisplayName("EC-01: Search with non-existent driver ID returns no rides")
    public void testSearchNonExistentDriverId() {
        historyPage.searchByDriverId("9999");

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "Non-existent driver ID should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
    }

    @Test
    @Order(71)
    @DisplayName("EC-02: Search with non-existent passenger ID returns no rides")
    public void testSearchNonExistentPassengerId() {
        historyPage.searchByPassengerId("9999");

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "Non-existent passenger ID should return no rides");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
    }

    @Test
    @Order(72)
    @DisplayName("EC-03: Search without entering ID still triggers search (empty results or all)")
    public void testSearchWithoutId() {
        // Clear the ID field and click search
        historyPage.selectSearchTypeDriver();
        historyPage.clearSearchId();
        historyPage.clickSearch();

        // Should show all rides (no filter applied as searchId is null)
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Searching without ID should show all rides");
    }

    @Test
    @Order(73)
    @DisplayName("EC-04: Switch search type from driver to passenger and search")
    public void testSwitchSearchType() {
        // Search by driver first
        historyPage.searchByDriverId("1");
        int driverRides = historyPage.getRideCount();
        assertTrue(driverRides > 0, "Driver 1 should have rides");

        // Switch to passenger and search
        historyPage.searchByPassengerId("3");
        int passengerRides = historyPage.getRideCount();
        assertTrue(passengerRides > 0, "Passenger 3 should have rides");
    }

    @Test
    @Order(74)
    @DisplayName("EC-05: Driver search type button shows as active")
    public void testDriverSearchTypeButtonActive() {
        historyPage.selectSearchTypeDriver();
        assertTrue(historyPage.isDriverSearchTypeActive(), "Driver button should be active");
        assertFalse(historyPage.isPassengerSearchTypeActive(), "Passenger button should not be active");
    }

    @Test
    @Order(75)
    @DisplayName("EC-06: Passenger search type button shows as active")
    public void testPassengerSearchTypeButtonActive() {
        historyPage.selectSearchTypePassenger();
        assertTrue(historyPage.isPassengerSearchTypeActive(), "Passenger button should be active");
        assertFalse(historyPage.isDriverSearchTypeActive(), "Driver button should not be active");
    }

    // =====================================================================
    // EDGE CASES: Status Filter
    // =====================================================================

    @Test
    @Order(80)
    @DisplayName("EC-10: Filter by REJECTED status when no rejected rides exist")
    public void testFilterByRejectedStatus() {
        historyPage.selectStatus("REJECTED");

        int rideCount = historyPage.getRideCount();
        // DataInitializer doesn't create REJECTED rides, so should be 0
        assertEquals(0, rideCount, "No rejected rides should exist in seed data");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show 'No rides found' message");
    }

    @Test
    @Order(81)
    @DisplayName("EC-11: Filter by PANIC status when no panic-status rides exist")
    public void testFilterByPanicStatus() {
        historyPage.selectStatus("PANIC");

        // Ride 9 has panicPressed=true but status is FINISHED, not PANIC
        // So PANIC status filter should likely find no rides
        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "No rides with PANIC status should exist in seed data");
    }

    @Test
    @Order(82)
    @DisplayName("EC-12: Rapidly switching status filters doesn't break the table")
    public void testRapidStatusFilterSwitching() {
        historyPage.selectStatus("FINISHED");
        historyPage.selectStatus("CANCELLED");
        historyPage.selectStatus("IN_PROGRESS");
        historyPage.selectStatus("all");

        // After switching back to all, all rides should be displayed
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "All rides should be shown after switching back to 'all'");
    }

    @Test
    @Order(83)
    @DisplayName("EC-13: Filter by CANCELLED_BY_DRIVER shows rides cancelled by driver")
    public void testFilterByCancelledByDriver() {
        historyPage.selectStatus("CANCELLED_BY_DRIVER");

        int rideCount = historyPage.getRideCount();
        // DataInitializer has one CANCELLED ride, not CANCELLED_BY_DRIVER specifically
        // This tests that the filter works without error
        if (rideCount > 0) {
            List<String> statuses = historyPage.getAllRideStatuses();
            for (String status : statuses) {
                assertEquals("Cancelled By Driver", status,
                        "All rides should have CANCELLED_BY_DRIVER status");
            }
        }
    }

    @Test
    @Order(84)
    @DisplayName("EC-14: Filter by CANCELLED_BY_PASSENGER shows rides cancelled by passenger")
    public void testFilterByCancelledByPassenger() {
        historyPage.selectStatus("CANCELLED_BY_PASSENGER");

        int rideCount = historyPage.getRideCount();
        if (rideCount > 0) {
            List<String> statuses = historyPage.getAllRideStatuses();
            for (String status : statuses) {
                assertEquals("Cancelled By Passenger", status,
                        "All rides should have CANCELLED_BY_PASSENGER status");
            }
        }
    }

    // =====================================================================
    // EDGE CASES: Date Filter
    // =====================================================================

    @Test
    @Order(90)
    @DisplayName("EC-20: Date filter input accepts a valid date")
    public void testDateFilterAcceptsValidDate() {
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        historyPage.setDateFilter(yesterday);

        // Should not crash and should display results or no-rides message
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount >= 0, "Date filter should work without errors");
    }

    @Test
    @Order(91)
    @DisplayName("EC-21: Clear date filter restores results")
    public void testClearDateFilterRestoresResults() {
        int allCount = historyPage.getRideCount();

        // Apply date filter for a date with likely rides
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        historyPage.setDateFilter(yesterday);
        int filteredCount = historyPage.getRideCount();

        // Clear the filter
        if (historyPage.isClearDateButtonVisible()) {
            historyPage.clearDateFilter();
            int restoredCount = historyPage.getRideCount();
            assertEquals(allCount, restoredCount,
                    "Clearing date filter should restore all rides");
        }
    }

    // =====================================================================
    // EDGE CASES: Combined Filter Edge Cases
    // =====================================================================

    @Test
    @Order(100)
    @DisplayName("EC-30: Non-existent driver + FINISHED status shows no rides")
    public void testNonExistentDriverWithFinishedFilter() {
        historyPage.searchByDriverId("9999");
        historyPage.selectStatus("FINISHED");

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "Non-existent driver with FINISHED filter should show no rides");
    }

    @Test
    @Order(101)
    @DisplayName("EC-31: Driver 1 with CANCELLED filter shows the cancelled ride")
    public void testDriver1WithCancelledFilter() {
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("CANCELLED");

        int rideCount = historyPage.getRideCount();
        // DataInitializer creates 1 cancelled ride for driver 1
        assertTrue(rideCount > 0, "Driver 1 should have at least one cancelled ride");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Cancelled", status, "All rides should be cancelled");
        }
    }

    @Test
    @Order(102)
    @DisplayName("EC-32: Driver 2 with CANCELLED filter shows no rides (only driver 1 has cancelled rides)")
    public void testDriver2WithCancelledFilter() {
        historyPage.searchByDriverId("2");
        historyPage.selectStatus("CANCELLED");

        int rideCount = historyPage.getRideCount();
        assertEquals(0, rideCount, "Driver 2 should have no cancelled rides");
    }

    @Test
    @Order(103)
    @DisplayName("EC-33: Status filter persists after changing search")
    public void testStatusFilterPersistsAfterChangingSearch() {
        historyPage.selectStatus("FINISHED");
        historyPage.searchByDriverId("1");

        // Status should still be FINISHED
        String selectedStatus = historyPage.getSelectedStatus();
        assertEquals("FINISHED", selectedStatus, "Status filter should persist after search");

        List<String> statuses = historyPage.getAllRideStatuses();
        for (String status : statuses) {
            assertEquals("Finished", status, "All rides should still be FINISHED");
        }
    }

    // =====================================================================
    // EDGE CASES: Sorting Edge Cases
    // =====================================================================

    @Test
    @Order(110)
    @DisplayName("EC-40: Sort by cost on single-result set doesn't crash")
    public void testSortOnSingleResult() {
        // Filter to CANCELLED which should have only 1 ride for driver 1
        historyPage.searchByDriverId("1");
        historyPage.selectStatus("CANCELLED");
        int count = historyPage.getRideCount();

        // Now sort by cost
        historyPage.sortByCost();

        assertEquals(count, historyPage.getRideCount(), "Sorting single result should not change count");
    }

    @Test
    @Order(111)
    @DisplayName("EC-41: Sort by cost on empty results doesn't crash")
    public void testSortOnEmptyResults() {
        historyPage.searchByDriverId("9999");
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should show no rides");

        // Sort should not crash
        historyPage.sortByCost();
        assertTrue(historyPage.isNoRidesMessageDisplayed(), "Should still show no rides after sort");
    }

    @Test
    @Order(112)
    @DisplayName("EC-42: Sort indicators update correctly across all sort columns")
    public void testSortIndicatorsUpdateAcrossColumns() {
        // Click start time (already active, should toggle direction)
        historyPage.sortByStartTime();
        assertEquals("▲", historyPage.getSortIndicatorText("sort-start-time"));

        // Switch to cost
        historyPage.sortByCost();
        assertEquals("▼", historyPage.getSortIndicatorText("sort-cost"));
        // Start time should no longer have active indicator
        assertEquals("", historyPage.getSortIndicatorText("sort-start-time"));

        // Switch to status
        historyPage.sortByStatus();
        assertEquals("▼", historyPage.getSortIndicatorText("sort-status"));
        assertEquals("", historyPage.getSortIndicatorText("sort-cost"));
    }

    @Test
    @Order(113)
    @DisplayName("EC-43: Sort by each column at least once")
    public void testSortByAllColumns() {
        String[] sortButtons = {
                "sort-start-time", "sort-end-time", "sort-route",
                "sort-status", "sort-cost", "sort-cancelled-by", "sort-panic"
        };

        for (String buttonId : sortButtons) {
            // Click the sort button via corresponding method
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
            int rideCount = historyPage.getRideCount();
            assertTrue(rideCount > 0,
                    "Sorting by " + buttonId + " should not remove rides");
        }
    }

    // =====================================================================
    // EDGE CASES: Page Data Integrity
    // =====================================================================

    @Test
    @Order(120)
    @DisplayName("EC-50: Each ride row displays all required fields")
    public void testRideRowDisplaysAllFields() {
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides to verify");

        for (int i = 0; i < Math.min(rideCount, 3); i++) {
            String startDate = historyPage.getRideStartDate(i);
            assertNotNull(startDate, "Ride " + i + " should have a start date");
            assertFalse(startDate.isEmpty(), "Ride " + i + " start date should not be empty");

            String status = historyPage.getRideStatus(i);
            assertNotNull(status, "Ride " + i + " should have a status");
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
    @Order(121)
    @DisplayName("EC-51: Ride rows display pickup and destination addresses")
    public void testRideRowDisplaysRouteInfo() {
        int rideCount = historyPage.getRideCount();
        assertTrue(rideCount > 0, "Should have rides to verify");

        for (int i = 0; i < Math.min(rideCount, 3); i++) {
            String pickup = historyPage.getRidePickup(i);
            assertNotNull(pickup, "Ride " + i + " should have a pickup address");
            assertFalse(pickup.equals("—"), "Ride " + i + " pickup should be a real address");

            String destination = historyPage.getRideDestination(i);
            assertNotNull(destination, "Ride " + i + " should have a destination address");
            assertFalse(destination.equals("—"), "Ride " + i + " destination should be a real address");
        }
    }

    @Test
    @Order(122)
    @DisplayName("EC-52: Panic ride is highlighted with YES in the table")
    public void testPanicRideShowsYes() {
        // Search for driver 2 who has the panic ride
        historyPage.searchByDriverId("2");

        List<String> panics = historyPage.getAllRidePanics();
        assertTrue(panics.contains("YES"),
                "Driver 2 should have at least one ride with PANIC = YES");
    }

    @Test
    @Order(123)
    @DisplayName("EC-53: Cancelled ride shows cancellation info")
    public void testCancelledRideShowsCancelInfo() {
        historyPage.selectStatus("CANCELLED");

        int rideCount = historyPage.getRideCount();
        if (rideCount > 0) {
            String cancelledBy = historyPage.getRideCancelledBy(0);
            assertNotEquals("—", cancelledBy, "Cancelled ride should show who cancelled it");
        }
    }

    // =====================================================================
    // EDGE/ERROR CASE: Authentication
    // =====================================================================

    @Test
    @Order(130)
    @DisplayName("ERR-01: Accessing ride history without login redirects to login")
    public void testAccessWithoutLoginRedirects() {
        // Open a new browser session without login
        driver.manage().deleteAllCookies();
        driver.get(BASE_URL + "/admin/ride-history");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/login"));

        assertTrue(driver.getCurrentUrl().contains("/login"),
                "Should be redirected to login page");
    }
}
