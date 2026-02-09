package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverRideHistoryBinding;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverRideHistoryFragment extends Fragment {

    private static final String TAG = "DriverRideHistory";
    private FragmentDriverRideHistoryBinding binding;
    private SharedPreferencesManager preferencesManager;
    
    private RidesAdapter adapter;
    private List<RideResponse> rides = new ArrayList<>();
    
    // Stats
    private double totalEarnings = 0;
    private int finishedRides = 0;
    private double totalDistance = 0;
    private double avgRating = 0;
    private double avgVehicleRating = 0;
    private int ratingCount = 0;
    private int vehicleRatingCount = 0;
    
    // Time filter: today, week, month
    private String timeFilter = "week";
    
    // Pagination
    private int currentPage = 0;
    private int pageSize = 20;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverRideHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferencesManager = new SharedPreferencesManager(requireContext());

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");
        }

        setupTimeFilterButtons();
        setupListView();
        setupScrollListener();
        
        loadDriverStats();
        loadRides();

        return root;
    }

    private void setupTimeFilterButtons() {
        // Find filter buttons by tags or text
        ViewGroup filterContainer = binding.filterContainer;
        if (filterContainer != null) {
            for (int i = 0; i < filterContainer.getChildCount(); i++) {
                View child = filterContainer.getChildAt(i);
                if (child instanceof TextView) {
                    TextView btn = (TextView) child;
                    String text = btn.getText().toString().toLowerCase();
                    btn.setOnClickListener(v -> {
                        setTimeFilter(text);
                    });
                }
            }
        }
    }
    
    private void setTimeFilter(String filter) {
        this.timeFilter = filter;
        currentPage = 0;
        hasMorePages = true;
        rides.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateFilterButtonStyles();
        loadRides();
    }
    
    private void updateFilterButtonStyles() {
        ViewGroup filterContainer = binding.filterContainer;
        if (filterContainer == null) return;
        
        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            View child = filterContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView btn = (TextView) child;
                String text = btn.getText().toString().toLowerCase();
                if (text.equals(timeFilter)) {
                    btn.setBackgroundColor(getResources().getColor(R.color.yellow_500, null));
                    btn.setTextColor(getResources().getColor(R.color.black, null));
                } else {
                    btn.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
                    btn.setTextColor(getResources().getColor(R.color.gray_400, null));
                }
            }
        }
    }

    private void setupListView() {
        adapter = new RidesAdapter(rides);
        binding.ridesListView.setAdapter(adapter);
        binding.ridesListView.setOnItemClickListener((parent, view, position, id) -> {
            RideResponse ride = rides.get(position);
            Bundle args = new Bundle();
            args.putLong("rideId", ride.getId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_nav_driver_history_to_nav_ride_details, args);
        });
    }
    
    private void setupScrollListener() {
        binding.ridesListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // no-op
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoading && hasMorePages && totalItemCount > 0
                        && (firstVisibleItem + visibleItemCount) >= totalItemCount - 2) {
                    loadMoreRides();
                }
            }
        });
    }

    private void loadDriverStats() {
        Long userId = preferencesManager.getUserId();
        if (userId == null) return;

        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.driverService.getStats(userId, token).enqueue(new Callback<DriverStatsResponse>() {
            @Override
            public void onResponse(Call<DriverStatsResponse> call, Response<DriverStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DriverStatsResponse stats = response.body();
                    avgRating = stats.getAverageRating();
                    avgVehicleRating = stats.getAverageVehicleRating();
                    ratingCount = stats.getTotalRatings();
                    vehicleRatingCount = stats.getTotalVehicleRatings();
                    updateStatsUI();
                }
            }
            
            @Override
            public void onFailure(Call<DriverStatsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load driver stats", t);
            }
        });
    }

    private void loadRides() {
        if (isLoading) return;
        isLoading = true;
        showLoading(true);
        
        Long userId = preferencesManager.getUserId();
        if (userId == null) {
            showLoading(false);
            isLoading = false;
            return;
        }
        
        // Calculate date range based on time filter
        String fromDate = getFromDateForFilter();
        String toDate = formatDateForApi(new Date(), true);
        
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRidesHistory(
            userId,    // driverId
            null,      // passengerId
            null,      // status - get all
            fromDate,
            toDate,
            currentPage,
            pageSize,
            "startTime,desc",
            token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call, Response<PageResponse<RideResponse>> response) {
                isLoading = false;
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> page = response.body();
                    List<RideResponse> content = page.getContent();
                    
                    if (content != null) {
                        // Filter to only past rides
                        List<RideResponse> pastRides = new ArrayList<>();
                        for (RideResponse r : content) {
                            if (r.isFinished() || r.isCancelled()) {
                                pastRides.add(r);
                            }
                        }
                        
                        if (currentPage == 0) {
                            rides.clear();
                            calculateStats(pastRides);
                        }
                        rides.addAll(pastRides);
                        adapter.notifyDataSetChanged();
                        
                        hasMorePages = page.getNumber() != null && page.getTotalPages() != null
                                && page.getNumber() < page.getTotalPages() - 1;
                    }
                } else {
                    Log.e(TAG, "Failed to load rides: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load rides", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                isLoading = false;
                showLoading(false);
                Log.e(TAG, "Failed to load rides", t);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadMoreRides() {
        currentPage++;
        loadRides();
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
    
    private void calculateStats(List<RideResponse> rideList) {
        totalEarnings = 0;
        finishedRides = 0;
        totalDistance = 0;
        
        for (RideResponse r : rideList) {
            if (r.isFinished()) {
                totalEarnings += r.getEffectiveCost();
                finishedRides++;
                totalDistance += r.getEffectiveDistance();
            }
        }
        
        updateStatsUI();
    }
    
    private void updateStatsUI() {
        if (binding == null) return;
        
        View root = binding.getRoot();
        
        // Update Total Earnings
        TextView earningsView = root.findViewById(R.id.stat_earnings);
        if (earningsView != null) {
            earningsView.setText(String.format(Locale.US, "%.2f RSD", totalEarnings));
        }
        
        // Update Rides Finished
        TextView ridesView = root.findViewById(R.id.stat_rides);
        if (ridesView != null) {
            ridesView.setText(String.valueOf(finishedRides));
        }
        
        // Update Driver Rating
        TextView driverRatingView = root.findViewById(R.id.stat_driver_rating);
        if (driverRatingView != null) {
            driverRatingView.setText(String.format(Locale.US, "%.2f", avgRating));
        }
        
        TextView driverRatingCountView = root.findViewById(R.id.stat_driver_rating_count);
        if (driverRatingCountView != null) {
            driverRatingCountView.setText(ratingCount + " ratings");
        }
        
        // Update Vehicle Rating
        TextView vehicleRatingView = root.findViewById(R.id.stat_vehicle_rating);
        if (vehicleRatingView != null) {
            vehicleRatingView.setText(String.format(Locale.US, "%.2f", avgVehicleRating));
        }
        
        TextView vehicleRatingCountView = root.findViewById(R.id.stat_vehicle_rating_count);
        if (vehicleRatingCountView != null) {
            vehicleRatingCountView.setText(vehicleRatingCount + " ratings");
        }
        
        // Update Distance
        TextView distanceView = root.findViewById(R.id.stat_distance);
        if (distanceView != null) {
            distanceView.setText(String.format(Locale.US, "%.2f km", totalDistance));
        }
    }
    
    private void showLoading(boolean show) {
        // Could add a ProgressBar to the layout
        // For now, just manage the state
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Inner adapter class
    private static class RidesAdapter extends BaseAdapter {

        private final List<RideResponse> rides;
        
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

        public RidesAdapter(List<RideResponse> rides) {
            this.rides = rides;
        }

        @Override
        public int getCount() {
            return rides.size();
        }

        @Override
        public RideResponse getItem(int position) {
            return rides.get(position);
        }

        @Override
        public long getItemId(int position) {
            RideResponse ride = rides.get(position);
            return ride.getId() != null ? ride.getId() : position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_ride_history, parent, false);
                holder = new ViewHolder();
                holder.tvStatus = convertView.findViewById(R.id.tv_status);
                holder.tvDate = convertView.findViewById(R.id.tv_date);
                holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
                holder.tvDestination = convertView.findViewById(R.id.tv_destination);
                holder.tvVehicleInfo = convertView.findViewById(R.id.tv_vehicle_info);
                holder.tvCost = convertView.findViewById(R.id.tv_cost);
                holder.tvDriverName = convertView.findViewById(R.id.tv_driver_name);
                holder.tvRejectionReason = convertView.findViewById(R.id.tv_rejection_reason);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RideResponse ride = getItem(position);
            
            // Status badge
            holder.tvStatus.setText(ride.getDisplayStatus().toUpperCase());
            
            // Parse dates
            Date startDate = parseDate(ride.getStartTime());
            Date endDate = parseDate(ride.getEndTime());
            
            // Date
            if (startDate != null) {
                holder.tvDate.setText(DATE_FORMAT.format(startDate));
            } else {
                Date scheduled = parseDate(ride.getScheduledTime());
                holder.tvDate.setText(scheduled != null ? DATE_FORMAT.format(scheduled) : "—");
            }
            
            // Departure
            if (ride.getEffectiveStartLocation() != null) {
                String pickup = ride.getEffectiveStartLocation().getAddress();
                holder.tvDeparture.setText(pickup != null ? truncate(pickup, 25) : "—");
            } else {
                holder.tvDeparture.setText("—");
            }
            
            // Destination
            if (ride.getEffectiveEndLocation() != null) {
                String dest = ride.getEffectiveEndLocation().getAddress();
                holder.tvDestination.setText(dest != null ? truncate(dest, 25) : "—");
            } else {
                holder.tvDestination.setText("—");
            }
            
            // Vehicle info: distance and duration
            double distance = ride.getEffectiveDistance();
            String durationStr = "—";
            if (startDate != null && endDate != null) {
                long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
                durationStr = durationMinutes + " min";
            } else if (ride.getEstimatedTimeInMinutes() != null) {
                durationStr = ride.getEstimatedTimeInMinutes() + " min";
            }
            holder.tvVehicleInfo.setText(String.format(Locale.US, "%.1f km • %s", distance, durationStr));
            
            // Cost
            double cost = ride.getEffectiveCost();
            holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));
            
            // Driver name (hidden for driver's own history)
            holder.tvDriverName.setVisibility(View.GONE);
            
            // Rejection reason
            String rejection = ride.getRejectionReason();
            if (rejection != null && !rejection.isEmpty()) {
                holder.tvRejectionReason.setVisibility(View.VISIBLE);
                holder.tvRejectionReason.setText(rejection);
            } else {
                holder.tvRejectionReason.setVisibility(View.GONE);
            }

            return convertView;
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

        static class ViewHolder {
            TextView tvStatus;
            TextView tvDate;
            TextView tvDeparture;
            TextView tvDestination;
            TextView tvVehicleInfo;
            TextView tvCost;
            TextView tvDriverName;
            TextView tvRejectionReason;
        }
    }
}