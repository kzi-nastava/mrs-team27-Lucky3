package com.example.mobile.ui.driver;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverOverviewBinding;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.utils.SharedPreferencesManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverOverviewFragment extends Fragment {

    private static final String TAG = "DriverOverview";
    private FragmentDriverOverviewBinding binding;
    private SharedPreferencesManager preferencesManager;
    
    private RideHistoryAdapter adapter;
    private List<RideResponse> rideItems = new ArrayList<>();
    private List<RideResponse> allFilteredRides = new ArrayList<>();
    private int visibleCount = 3;
    private static final int INITIAL_COUNT = 3;
    private static final int INCREMENT = 5;
    
    // Stats
    private double totalEarnings = 0;
    private int finishedRides = 0;
    private double avgRating = 0;
    private int ratingCount = 0;
    private double totalDistance = 0;
    
    // All rides for the current stats time period
    private List<RideResponse> allRidesInPeriod = new ArrayList<>();
    
    // Time filter for stats: today, week, month, all
    private String timeFilter = "week";

    // Ride history filters
    private String fromDate = null;
    private String toDate = null;
    private String sortField = "startTime";
    private boolean sortAsc = false;
    private String statusFilter = null; // null = all

    // Sort/Status options
    private static final String[] STATUS_OPTIONS = {"All", "Finished", "Cancelled"};
    private static final String[] STATUS_VALUES = {null, "FINISHED", "CANCELLED,CANCELLED_BY_DRIVER,CANCELLED_BY_PASSENGER"};
    private static final String[] SORT_OPTIONS = {
            "Start Time", "End Time", "Total Cost", "Distance"
    };
    private static final String[] SORT_FIELDS = {
            "startTime", "endTime", "totalCost", "distanceKm"
    };

    // UI elements for ride history filters
    private TextView btnFromDate, btnToDate, btnSortDirection;
    private Spinner spinnerSort, spinnerStatus;
    private boolean spinnerInitializing = true;

    // Show more UI
    private LinearLayout showMoreContainer;
    private TextView tvShowingCount, btnShowMore;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverOverviewBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferencesManager = new SharedPreferencesManager(requireContext());

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Overview");
        }

        initRideHistoryFilterViews(root);
        setupTimeFilterButtons();
        setupDatePickers();
        setupSortSpinner();
        setupStatusSpinner();
        setupSortDirection();
        setupShowMore(root);
        setupListView();
        
        loadDriverStats();
        loadStatsRides();
        loadRides();

        return root;
    }

    private void initRideHistoryFilterViews(View root) {
        btnFromDate = root.findViewById(R.id.btn_from_date);
        btnToDate = root.findViewById(R.id.btn_to_date);
        btnSortDirection = root.findViewById(R.id.btn_sort_direction);
        spinnerSort = root.findViewById(R.id.spinner_sort);
        spinnerStatus = root.findViewById(R.id.spinner_status);

        root.findViewById(R.id.btn_clear_dates).setOnClickListener(v -> clearDates());
    }

    // ---- Date Pickers ----

    private void setupDatePickers() {
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.DatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    if (isFrom) {
                        selected.set(Calendar.HOUR_OF_DAY, 0);
                        selected.set(Calendar.MINUTE, 0);
                        selected.set(Calendar.SECOND, 0);
                        fromDate = formatDateForApi(selected.getTime(), false);
                        btnFromDate.setText(displayFormat.format(selected.getTime()));
                    } else {
                        selected.set(Calendar.HOUR_OF_DAY, 23);
                        selected.set(Calendar.MINUTE, 59);
                        selected.set(Calendar.SECOND, 59);
                        toDate = formatDateForApi(selected.getTime(), true);
                        btnToDate.setText(displayFormat.format(selected.getTime()));
                    }
                    loadRides();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void clearDates() {
        fromDate = null;
        toDate = null;
        btnFromDate.setText("");
        btnToDate.setText("");
        btnFromDate.setHint("Start date");
        btnToDate.setHint("End date");
        visibleCount = INITIAL_COUNT;
        loadRides();
    }

    // ---- Sort Spinner ----

    private void setupSortSpinner() {
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, SORT_OPTIONS);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortField = SORT_FIELDS[position];
                if (!spinnerInitializing) {
                    loadRides();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ---- Sort Direction ----

    private void setupSortDirection() {
        updateSortDirectionUI();
        btnSortDirection.setOnClickListener(v -> {
            sortAsc = !sortAsc;
            updateSortDirectionUI();
            loadRides();
        });
    }

    private void updateSortDirectionUI() {
        btnSortDirection.setText(sortAsc ? "↑ ASC" : "↓ DESC");
    }

    // ---- Status Spinner ----

    private void setupStatusSpinner() {
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, STATUS_OPTIONS);
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                statusFilter = STATUS_VALUES[position];
                if (!spinnerInitializing) {
                    loadRides();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // Done initializing spinners — next selections will trigger reload
        spinnerInitializing = false;
    }

    // ---- Time Filter Buttons (for stats) ----

    private void setupTimeFilterButtons() {
        View root = binding.getRoot();
        
        TextView filterToday = root.findViewById(R.id.filter_today);
        TextView filterWeek = root.findViewById(R.id.filter_week);
        TextView filterMonth = root.findViewById(R.id.filter_month);
        TextView filterAll = root.findViewById(R.id.filter_all);
        
        View.OnClickListener filterClickListener = v -> {
            String filter = "";
            if (v.getId() == R.id.filter_today) filter = "today";
            else if (v.getId() == R.id.filter_week) filter = "week";
            else if (v.getId() == R.id.filter_month) filter = "month";
            else if (v.getId() == R.id.filter_all) filter = "all";
            setTimeFilter(filter);
        };
        
        if (filterToday != null) filterToday.setOnClickListener(filterClickListener);
        if (filterWeek != null) filterWeek.setOnClickListener(filterClickListener);
        if (filterMonth != null) filterMonth.setOnClickListener(filterClickListener);
        if (filterAll != null) filterAll.setOnClickListener(filterClickListener);
        
        updateFilterButtonStyles();
    }
    
    private void setTimeFilter(String filter) {
        this.timeFilter = filter;
        updateFilterButtonStyles();
        loadStatsRides();
    }
    
    private void updateFilterButtonStyles() {
        View root = binding.getRoot();
        
        TextView filterToday = root.findViewById(R.id.filter_today);
        TextView filterWeek = root.findViewById(R.id.filter_week);
        TextView filterMonth = root.findViewById(R.id.filter_month);
        TextView filterAll = root.findViewById(R.id.filter_all);
        
        TextView[] filters = {filterToday, filterWeek, filterMonth, filterAll};
        String[] filterNames = {"today", "week", "month", "all"};
        
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] != null) {
                if (filterNames[i].equals(timeFilter)) {
                    filters[i].setBackgroundResource(R.drawable.bg_filter_selected);
                    filters[i].setTextColor(getResources().getColor(R.color.black, null));
                } else {
                    filters[i].setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
                    filters[i].setTextColor(getResources().getColor(R.color.gray_400, null));
                }
            }
        }
    }

    // ---- Show More ----

    private void setupShowMore(View root) {
        showMoreContainer = root.findViewById(R.id.show_more_container);
        tvShowingCount = root.findViewById(R.id.tv_showing_count);
        btnShowMore = root.findViewById(R.id.btn_show_more);

        btnShowMore.setOnClickListener(v -> {
            visibleCount += INCREMENT;
            applyVisibleCount();
        });
    }

    private void applyVisibleCount() {
        rideItems.clear();
        int total = allFilteredRides.size();
        int show = Math.min(visibleCount, total);
        for (int i = 0; i < show; i++) {
            rideItems.add(allFilteredRides.get(i));
        }
        adapter.notifyDataSetChanged();
        ListViewHelper.setListViewHeightBasedOnChildren(binding.rideHistoryList);

        // Update count text and button visibility
        if (total > INITIAL_COUNT) {
            showMoreContainer.setVisibility(View.VISIBLE);
            tvShowingCount.setText(String.format(Locale.US, "Showing %d of %d", show, total));
            btnShowMore.setVisibility(show < total ? View.VISIBLE : View.GONE);
        } else {
            showMoreContainer.setVisibility(View.GONE);
        }
    }

    // ---- ListView ----

    private void setupListView() {
        ListView listView = binding.rideHistoryList;
        adapter = new RideHistoryAdapter(requireContext(), rideItems);
        listView.setAdapter(adapter);
    }
    
    // ---- Data Loading ----

    private void loadDriverStats() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) {
            Log.e(TAG, "User ID is invalid, cannot load driver stats");
            return;
        }
        
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getStats(userId, token).enqueue(new Callback<DriverStatsResponse>() {
            @Override
            public void onResponse(Call<DriverStatsResponse> call, Response<DriverStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverStatsResponse stats = response.body();
                    avgRating = stats.getAverageRating();
                    ratingCount = stats.getTotalRatings();
                    updateStatsUI();
                }
            }
            
            @Override
            public void onFailure(Call<DriverStatsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load driver stats", t);
            }
        });
    }

    /**
     * Load rides for the stats time period (today/week/month/all).
     * Used only for calculating earnings, distance, ride count stats.
     */
    private void loadStatsRides() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        String statsFromDate = getFromDateForTimeFilter();
        String statsToDate = formatDateForApi(new Date(), true);
        String token = "Bearer " + preferencesManager.getToken();

        ClientUtils.rideService.getRidesHistory(
            userId, null, "FINISHED,CANCELLED,CANCELLED_BY_DRIVER,CANCELLED_BY_PASSENGER",
            statsFromDate, statsToDate, 0, 200, "startTime,desc", token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RideResponse> content = response.body().getContent();
                    if (content != null) {
                        allRidesInPeriod = content.stream()
                            .filter(r -> r.isFinished() || r.isCancelled())
                            .collect(Collectors.toList());
                        calculateStats();
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to load stats rides", t);
            }
        });
    }

    /**
     * Load rides for the history list using ride history filters
     * (date range, sort, status).
     */
    private void loadRides() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) return;

        // Build status param
        String historyStatuses = "FINISHED,CANCELLED,CANCELLED_BY_DRIVER,CANCELLED_BY_PASSENGER";
        String statusParam = statusFilter != null ? statusFilter : historyStatuses;

        // Build sort param
        String sortParam = sortField + "," + (sortAsc ? "asc" : "desc");

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRidesHistory(
            userId, null, statusParam,
            fromDate, toDate,
            0, 200, sortParam, token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<RideResponse> content = response.body().getContent();
                    allFilteredRides.clear();
                    if (content != null) {
                        for (RideResponse r : content) {
                            if (r.isFinished() || r.isCancelled()) {
                                allFilteredRides.add(r);
                            }
                        }
                    }
                    visibleCount = INITIAL_COUNT;
                    applyVisibleCount();
                } else {
                    Log.e(TAG, "Failed to load rides: HTTP " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load rides: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                Log.e(TAG, "Failed to load rides", t);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    // ---- Stats ----

    private void calculateStats() {
        totalEarnings = 0;
        finishedRides = 0;
        totalDistance = 0;
        
        for (RideResponse r : allRidesInPeriod) {
            if (r.isFinished()) {
                totalEarnings += r.getEffectiveCost();
                totalDistance += r.getEffectiveDistance();
                finishedRides++;
            }
        }
        
        updateStatsUI();
    }
    
    // ---- Helpers ----

    private String getFromDateForTimeFilter() {
        Calendar cal = Calendar.getInstance();
        switch (timeFilter) {
            case "today":
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case "week":
                cal.add(Calendar.DAY_OF_YEAR, -7);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case "month":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            default:
                return null;
        }
        return formatDateForApi(cal.getTime(), false);
    }
    
    private String formatDateForApi(Date date, boolean endOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (endOfDay) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(cal.getTime());
    }
    
    private void updateStatsUI() {
        if (binding == null) return;
        
        View root = binding.getRoot();
        
        TextView earningsView = root.findViewById(R.id.total_earnings);
        if (earningsView != null) {
            earningsView.setText(String.format(Locale.US, "%.2f RSD", totalEarnings));
        }
        
        TextView ridesView = root.findViewById(R.id.total_rides);
        if (ridesView != null) {
            ridesView.setText(String.valueOf(finishedRides));
        }
        
        TextView avgPerRideView = root.findViewById(R.id.avg_per_ride);
        if (avgPerRideView != null) {
            double avg = finishedRides > 0 ? totalEarnings / finishedRides : 0;
            avgPerRideView.setText(String.format(Locale.US, "Avg %.2f RSD/ride", avg));
        }
        
        TextView ratingView = root.findViewById(R.id.avg_rating);
        if (ratingView != null) {
            ratingView.setText(String.format(Locale.US, "%.2f", avgRating));
        }
        
        TextView ratingCountView = root.findViewById(R.id.rating_count);
        if (ratingCountView != null) {
            ratingCountView.setText(ratingCount + " ratings");
        }
        
        TextView distanceView = root.findViewById(R.id.total_distance);
        if (distanceView != null) {
            distanceView.setText(String.format(Locale.US, "%.1f km", totalDistance));
        }
        
        TextView avgDistanceView = root.findViewById(R.id.avg_distance);
        if (avgDistanceView != null) {
            double avgDist = finishedRides > 0 ? totalDistance / finishedRides : 0;
            avgDistanceView.setText(String.format(Locale.US, "Avg %.1f km/ride", avgDist));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

