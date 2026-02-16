package com.example.mobile.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for AdminRideHistoryFragment.
 * Holds all filter/sort/pagination state and the rides list so they survive configuration changes
 * (e.g. orientation rotation). Exposes LiveData for the Fragment to observe.
 */
public class AdminRideHistoryViewModel extends AndroidViewModel {

    private static final String TAG = "AdminRideHistoryVM";
    private static final int PAGE_SIZE = 15;

    // Sort / Status constants
    public static final String[] STATUS_OPTIONS = {
            "All", "FINISHED", "CANCELLED", "CANCELLED_BY_DRIVER", "CANCELLED_BY_PASSENGER"
    };
    public static final String[] SORT_OPTIONS = {
            "Start Time", "End Time", "Total Cost", "Status", "Distance", "Panic"
    };
    public static final String[] SORT_FIELDS = {
            "startTime", "endTime", "totalCost", "status", "distanceKm", "panicPressed"
    };

    private final SharedPreferencesManager preferencesManager;

    // ==================== Filter / Sort State ====================
    private String searchType = "driver";
    private Long searchUserId = null;
    private String fromDate = null;
    private String toDate = null;
    private String statusFilter = null;
    private String sortField = "startTime";
    private boolean sortAsc = false;
    private int statusSpinnerPosition = 0;
    private int sortSpinnerPosition = 0;

    // Display strings for date buttons (survive rotation)
    private String fromDateDisplay = null;
    private String toDateDisplay = null;

    // ==================== Pagination ====================
    private int currentPage = 0;
    private boolean hasMorePages = true;
    private boolean loadingInProgress = false;

    // Flag: has data been loaded at least once?
    private boolean initialLoadDone = false;

    // ==================== LiveData ====================
    private final MutableLiveData<List<RideResponse>> rides = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> morePages = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> totalElements = new MutableLiveData<>(0);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<String> emptyText = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> sortDirectionChanged = new MutableLiveData<>(false);

    // User info banner
    private final MutableLiveData<String> bannerName = new MutableLiveData<>("All Rides");
    private final MutableLiveData<String> bannerEmail = new MutableLiveData<>("Showing all ride history");

    public AdminRideHistoryViewModel(@NonNull Application application) {
        super(application);
        preferencesManager = new SharedPreferencesManager(application.getApplicationContext());
    }

    // ==================== LiveData Getters ====================

    public LiveData<List<RideResponse>> getRides() { return rides; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getMorePages() { return morePages; }
    public LiveData<Integer> getTotalElements() { return totalElements; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getEmptyText() { return emptyText; }
    public LiveData<Boolean> getSortDirectionChanged() { return sortDirectionChanged; }
    public LiveData<String> getBannerName() { return bannerName; }
    public LiveData<String> getBannerEmail() { return bannerEmail; }

    // ==================== State Getters / Setters ====================

    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }

    public Long getSearchUserId() { return searchUserId; }
    public void setSearchUserId(Long id) { this.searchUserId = id; }

    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }

    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }

    public String getFromDateDisplay() { return fromDateDisplay; }
    public void setFromDateDisplay(String d) { this.fromDateDisplay = d; }

    public String getToDateDisplay() { return toDateDisplay; }
    public void setToDateDisplay(String d) { this.toDateDisplay = d; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public boolean isSortAsc() { return sortAsc; }
    public void setSortAsc(boolean sortAsc) { this.sortAsc = sortAsc; }

    public int getStatusSpinnerPosition() { return statusSpinnerPosition; }
    public void setStatusSpinnerPosition(int pos) { this.statusSpinnerPosition = pos; }

    public int getSortSpinnerPosition() { return sortSpinnerPosition; }
    public void setSortSpinnerPosition(int pos) { this.sortSpinnerPosition = pos; }

    public boolean isInitialLoadDone() { return initialLoadDone; }

    public int getCurrentPage() { return currentPage; }

    // ==================== Actions ====================

    /**
     * Toggle sort direction (ASC ↔ DESC) — used by shake sensor and manual toggle.
     */
    public void toggleSortDirection() {
        sortAsc = !sortAsc;
        sortDirectionChanged.postValue(sortAsc);
        resetAndLoad();
    }

    /**
     * Reset pagination and reload from page 0.
     */
    public void resetAndLoad() {
        currentPage = 0;
        hasMorePages = true;
        List<RideResponse> current = rides.getValue();
        if (current != null) current.clear();
        rides.setValue(current);
        loadRides();
    }

    /**
     * Load next page (appends to current list).
     */
    public void loadNextPage() {
        currentPage++;
        loadRides();
    }

    /**
     * Clear all filters and reload.
     */
    public void clearAllFilters() {
        searchUserId = null;
        fromDate = null;
        toDate = null;
        fromDateDisplay = null;
        toDateDisplay = null;
        statusFilter = null;
        sortField = "startTime";
        sortAsc = false;
        statusSpinnerPosition = 0;
        sortSpinnerPosition = 0;
        bannerName.setValue("All Rides");
        bannerEmail.setValue("Showing all ride history");
        sortDirectionChanged.setValue(false);
        resetAndLoad();
    }

    // Default filter: only show history-relevant statuses (finished + cancelled)
    private static final String DEFAULT_STATUS_FILTER =
            "FINISHED,CANCELLED,CANCELLED_BY_DRIVER,CANCELLED_BY_PASSENGER";

    /**
     * Perform the API call to load rides with current filters.
     */
    public void loadRides() {
        if (loadingInProgress) return;
        loadingInProgress = true;
        isLoading.setValue(true);
        emptyText.setValue(null);

        String token = "Bearer " + preferencesManager.getToken();
        Long driverId = "driver".equals(searchType) ? searchUserId : null;
        Long passengerId = "passenger".equals(searchType) ? searchUserId : null;
        String sortParam = sortField + "," + (sortAsc ? "asc" : "desc");

        // When "All" is selected (statusFilter == null), use the default filter
        // so only finished/cancelled rides appear in history
        String effectiveStatus = statusFilter != null ? statusFilter : DEFAULT_STATUS_FILTER;

        ClientUtils.rideService.getRidesHistory(
                driverId, passengerId, effectiveStatus,
                fromDate, toDate,
                currentPage, PAGE_SIZE, sortParam, token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<RideResponse>> call,
                                   @NonNull Response<PageResponse<RideResponse>> response) {
                loadingInProgress = false;
                isLoading.postValue(false);
                initialLoadDone = true;

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> page = response.body();
                    List<RideResponse> content = page.getContent();

                    List<RideResponse> current = rides.getValue();
                    if (current == null) current = new ArrayList<>();
                    if (currentPage == 0) current.clear();

                    if (content != null) {
                        current.addAll(content);
                    }
                    rides.postValue(current);

                    hasMorePages = page.getNumber() != null && page.getTotalPages() != null
                            && page.getNumber() < page.getTotalPages() - 1;
                    morePages.postValue(hasMorePages);

                    int total = page.getTotalElements() != null ? page.getTotalElements() : current.size();
                    totalElements.postValue(total);

                    // Update banner
                    if (searchUserId != null && !current.isEmpty()) {
                        updateUserInfoFromRides(driverId, passengerId, current);
                    } else if (searchUserId == null) {
                        bannerName.postValue("All Rides");
                        bannerEmail.postValue("Showing all ride history");
                    }

                    // Empty state
                    if (current.isEmpty()) {
                        emptyText.postValue(searchUserId == null
                                ? "No rides found"
                                : "No rides found for this user");
                    }
                } else {
                    Log.e(TAG, "Failed to load rides: " + response.code());
                    errorMessage.postValue("Failed to load rides (Error " + response.code() + ")");
                    List<RideResponse> current = rides.getValue();
                    if (current == null || current.isEmpty()) {
                        emptyText.postValue("Failed to load rides");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<RideResponse>> call, @NonNull Throwable t) {
                loadingInProgress = false;
                isLoading.postValue(false);
                initialLoadDone = true;
                Log.e(TAG, "Network error loading rides", t);
                errorMessage.postValue("Network error. Please try again.");
                List<RideResponse> current = rides.getValue();
                if (current == null || current.isEmpty()) {
                    emptyText.postValue("Network error. Please try again.");
                }
            }
        });
    }

    private void updateUserInfoFromRides(Long driverId, Long passengerId, List<RideResponse> ridesList) {
        if (ridesList.isEmpty()) return;

        RideResponse firstRide = ridesList.get(0);
        String name = "Unknown";
        String email = "";

        if (driverId != null && firstRide.getDriver() != null) {
            RideResponse.DriverInfo driver = firstRide.getDriver();
            name = (driver.getName() != null ? driver.getName() : "") + " " +
                    (driver.getSurname() != null ? driver.getSurname() : "");
            email = driver.getEmail() != null ? driver.getEmail() : "";
        } else if (passengerId != null && firstRide.getPassengers() != null) {
            for (RideResponse.PassengerInfo p : firstRide.getPassengers()) {
                if (passengerId.equals(p.getId())) {
                    name = p.getFullName();
                    email = p.getEmail() != null ? p.getEmail() : "";
                    break;
                }
            }
        }

        bannerName.postValue(name.trim());
        bannerEmail.postValue(email);
    }

    public String formatDateForApi(Date date, boolean endOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (endOfDay) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }
}
