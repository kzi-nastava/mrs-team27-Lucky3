package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.widget.ListView;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverDashboardBinding;
import com.example.mobile.models.DriverStatsResponse;
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
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverDashboardFragment extends Fragment {

    private static final String TAG = "DriverDashboard";
    private FragmentDriverDashboardBinding binding;
    private SharedPreferencesManager preferencesManager;
    
    private RideHistoryAdapter adapter;
    private List<RideHistoryAdapter.RideHistoryItem> rideItems = new ArrayList<>();
    
    // Stats
    private double totalEarnings = 0;
    private int finishedRides = 0;
    private double avgRating = 0;
    private int ratingCount = 0;
    private double totalDistance = 0;
    
    // All rides for the current time period (used for stats calculation)
    private List<RideResponse> allRidesInPeriod = new ArrayList<>();
    
    // Time filter: today, week, month, all
    private String timeFilter = "week";
    // Status filter: all, completed, cancelled
    private String statusFilter = "all";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverDashboardBinding.inflate(inflater, container, false);
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

        setupTimeFilterButtons();
        setupStatusFilterButtons();
        setupListView();
        
        loadDriverStats();
        loadRides();

        return root;
    }
    
    private void setupStatusFilterButtons() {
        View root = binding.getRoot();

        TextView statusAll = root.findViewById(R.id.status_all);
        TextView statusCompleted = root.findViewById(R.id.status_completed);
        TextView statusCancelled = root.findViewById(R.id.status_cancelled);

        View.OnClickListener listener = v -> {
            String filter = "";
            if (v.getId() == R.id.status_all) filter = "all";
            else if (v.getId() == R.id.status_completed) filter = "completed";
            else if (v.getId() == R.id.status_cancelled) filter = "cancelled";

            if (!filter.isEmpty()) {
                setStatusFilter(filter);
            }
        };

        if (statusAll != null) statusAll.setOnClickListener(listener);
        if (statusCompleted != null) statusCompleted.setOnClickListener(listener);
        if (statusCancelled != null) statusCancelled.setOnClickListener(listener);

        updateStatusFilterStyles();
    }

    private void setStatusFilter(String filter) {
        this.statusFilter = filter;
        updateStatusFilterStyles();
        // Only re-filter the list, don't reload from API (stats stay the same)
        applyStatusFilterToList();
    }

    private void updateStatusFilterStyles() {
        View root = binding.getRoot();
        if (root == null) return;

        TextView statusAll = root.findViewById(R.id.status_all);
        TextView statusCompleted = root.findViewById(R.id.status_completed);
        TextView statusCancelled = root.findViewById(R.id.status_cancelled);

        TextView[] filters = {statusAll, statusCompleted, statusCancelled};
        String[] filterNames = {"all", "completed", "cancelled"};

        for (int i = 0; i < filters.length; i++) {
            if (filters[i] != null) {
                if (filterNames[i].equals(statusFilter)) {
                    filters[i].setBackgroundResource(R.drawable.bg_filter_selected);
                    filters[i].setTextColor(getResources().getColor(R.color.black, null));
                } else {
                    filters[i].setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
                    filters[i].setTextColor(getResources().getColor(R.color.gray_400, null));
                }
            }
        }
    }

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
        
        // Set initial selection
        updateFilterButtonStyles();
    }
    
    private void setTimeFilter(String filter) {
        this.timeFilter = filter;
        rideItems.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateFilterButtonStyles();
        loadRides();
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

    private void setupListView() {
        ListView listView = binding.rideHistoryList;
        adapter = new RideHistoryAdapter(requireContext(), rideItems);
        listView.setAdapter(adapter);
    }
    
    private void loadDriverStats() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) {
            Log.e(TAG, "User ID is invalid, cannot load driver stats");
            return;
        }
        
        Log.d(TAG, "Loading driver stats for driver " + userId);
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getStats(userId, token).enqueue(new Callback<DriverStatsResponse>() {
            @Override
            public void onResponse(Call<DriverStatsResponse> call, Response<DriverStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverStatsResponse stats = response.body();
                    avgRating = stats.getAverageRating();
                    ratingCount = stats.getTotalRatings();
                    Log.d(TAG, "Driver stats loaded: rating=" + avgRating + " count=" + ratingCount);
                    updateStatsUI();
                } else {
                    Log.e(TAG, "Failed to load driver stats: HTTP " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<DriverStatsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load driver stats", t);
            }
        });
    }

    private void loadRides() {
        Long userId = preferencesManager.getUserId();
        if (userId == null || userId <= 0) {
            Log.e(TAG, "User ID is invalid (" + userId + "), cannot load rides");
            return;
        }
        
        // Calculate date range based on time filter
        String fromDate = getFromDateForFilter();
        String toDate = formatDateForApi(new Date(), true);
        
        Log.d(TAG, "Loading rides for driver " + userId + " fromDate=" + fromDate + " toDate=" + toDate);
        
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRidesHistory(
            userId,    // driverId
            null,      // passengerId
            null,      // status - get all, filter on frontend
            fromDate,
            toDate,
            0,         // page
            100,       // size - larger to capture full history
            "startTime,desc",
            token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> page = response.body();
                    List<RideResponse> content = page.getContent();
                    
                    Log.d(TAG, "Loaded " + (content != null ? content.size() : 0) + " rides");
                    
                    if (content != null) {
                        // Filter to only past rides (FINISHED or CANCELLED)
                        allRidesInPeriod = content.stream()
                            .filter(r -> r.isFinished() || r.isCancelled())
                            .collect(Collectors.toList());
                        
                        Log.d(TAG, "Filtered to " + allRidesInPeriod.size() + " past rides");
                        
                        // Calculate stats from all FINISHED rides in the period
                        calculateStats();
                        
                        // Apply status filter for the list display
                        applyStatusFilterToList();
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Could not read error body";
                    }
                    Log.e(TAG, "Failed to load rides: HTTP " + response.code() + " - " + errorBody);
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
    
    private void applyStatusFilterToList() {
        rideItems.clear();
        
        for (RideResponse r : allRidesInPeriod) {
            boolean matchesFilter = "all".equals(statusFilter) ||
                                    ("completed".equals(statusFilter) && r.isFinished()) ||
                                    ("cancelled".equals(statusFilter) && r.isCancelled());
            
            if (matchesFilter) {
                rideItems.add(mapToHistoryItem(r));
            }
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private RideHistoryAdapter.RideHistoryItem mapToHistoryItem(RideResponse ride) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
        
        Date startDate = parseDate(ride.getStartTime());
        Date endDate = parseDate(ride.getEndTime());
        
        String startDateStr = startDate != null ? dateFormat.format(startDate) : "-";
        String startTimeStr = startDate != null ? timeFormat.format(startDate) : "-";
        String endDateStr = endDate != null ? dateFormat.format(endDate) : "-";
        String endTimeStr = endDate != null ? timeFormat.format(endDate) : "-";
        
        String pickupAddr = "—";
        if (ride.getEffectiveStartLocation() != null && ride.getEffectiveStartLocation().getAddress() != null) {
            pickupAddr = truncate(ride.getEffectiveStartLocation().getAddress(), 30);
        }
        
        String destAddr = "—";
        if (ride.getEffectiveEndLocation() != null && ride.getEffectiveEndLocation().getAddress() != null) {
            destAddr = truncate(ride.getEffectiveEndLocation().getAddress(), 30);
        }
        
        int passengerCount = ride.getPassengers() != null ? ride.getPassengers().size() : 1;
        
        double distance = ride.getEffectiveDistance();
        String distanceStr = String.format(Locale.US, "%.1f km", distance);
        
        String durationStr = "—";
        if (startDate != null && endDate != null) {
            long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
            durationStr = durationMinutes + " min";
        } else if (ride.getEstimatedTimeInMinutes() != null) {
            durationStr = ride.getEstimatedTimeInMinutes() + " min";
        }
        
        double price = ride.getEffectiveCost();
        
        return new RideHistoryAdapter.RideHistoryItem(
            ride.getId() != null ? ride.getId() : 0,
            startDateStr, startTimeStr, endDateStr, endTimeStr,
            pickupAddr, destAddr, passengerCount, distanceStr, durationStr, price
        );
    }
    
    private String getFromDateForFilter() {
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
                return null; // All time - no from date
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
    
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                return sdf.parse(dateStr);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    
    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
    
    private void updateStatsUI() {
        if (binding == null) return;
        
        View root = binding.getRoot();
        
        // Update Total Earnings
        TextView earningsView = root.findViewById(R.id.total_earnings);
        if (earningsView != null) {
            earningsView.setText(String.format(Locale.US, "%.2f RSD", totalEarnings));
        }
        
        // Update Total Rides
        TextView ridesView = root.findViewById(R.id.total_rides);
        if (ridesView != null) {
            ridesView.setText(String.valueOf(finishedRides));
        }
        
        // Update Average per ride
        TextView avgPerRideView = root.findViewById(R.id.avg_per_ride);
        if (avgPerRideView != null) {
            double avg = finishedRides > 0 ? totalEarnings / finishedRides : 0;
            avgPerRideView.setText(String.format(Locale.US, "Avg %.2f RSD/ride", avg));
        }
        
        // Update Average Rating
        TextView ratingView = root.findViewById(R.id.avg_rating);
        if (ratingView != null) {
            ratingView.setText(String.format(Locale.US, "%.2f", avgRating));
        }
        
        // Update rating count
        TextView ratingCountView = root.findViewById(R.id.rating_count);
        if (ratingCountView != null) {
            ratingCountView.setText(ratingCount + " ratings");
        }
        
        // Update Total Distance
        TextView distanceView = root.findViewById(R.id.total_distance);
        if (distanceView != null) {
            distanceView.setText(String.format(Locale.US, "%.1f km", totalDistance));
        }
        
        // Update Average Distance per ride
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

