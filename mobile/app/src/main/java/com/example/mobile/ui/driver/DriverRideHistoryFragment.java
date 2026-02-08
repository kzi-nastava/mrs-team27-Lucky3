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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverRideHistoryBinding;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.services.DriverService;
import com.example.mobile.services.RideService;
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
    private RideService rideService;
    private DriverService driverService;
    
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
        rideService = ClientUtils.getAuthenticatedRideService(preferencesManager);
        driverService = ClientUtils.getAuthenticatedDriverService(preferencesManager);

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");
        }

        setupTimeFilterButtons();
        setupRecyclerView();
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

    private void setupRecyclerView() {
        adapter = new RidesAdapter(rides, ride -> {
            // Navigate to ride details
            Bundle args = new Bundle();
            args.putLong("rideId", ride.getId());
            Navigation.findNavController(requireView())
                .navigate(R.id.action_nav_driver_history_to_nav_ride_details, args);
        });
        binding.ridesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.ridesRecyclerView.setAdapter(adapter);
    }
    
    private void setupScrollListener() {
        binding.ridesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMorePages) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                            && firstVisibleItemPosition >= 0) {
                        loadMoreRides();
                    }
                }
            }
        });
    }

    private void loadDriverStats() {
        Long userId = preferencesManager.getUserId();
        if (userId == null) return;

        String token = "Bearer " + preferencesManager.getToken();
        driverService.getStats(userId, token).enqueue(new Callback<DriverStatsResponse>() {
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
        
        rideService.getRidesHistory(
            userId,    // driverId
            null,      // passengerId
            null,      // status - get all
            fromDate,
            toDate,
            currentPage,
            pageSize,
            "startTime,desc"
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
    private static class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.RideViewHolder> {

        private final List<RideResponse> rides;
        private final OnRideClickListener listener;
        
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

        interface OnRideClickListener {
            void onRideClick(RideResponse ride);
        }

        public RidesAdapter(List<RideResponse> rides, OnRideClickListener listener) {
            this.rides = rides;
            this.listener = listener;
        }

        @NonNull
        @Override
        public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ride_history, parent, false);
            return new RideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
            RideResponse ride = rides.get(position);
            
            // Parse dates
            Date startDate = parseDate(ride.getStartTime());
            Date endDate = parseDate(ride.getEndTime());
            
            // Set start date/time
            if (startDate != null) {
                holder.startDate.setText(DATE_FORMAT.format(startDate));
                holder.startTime.setText(TIME_FORMAT.format(startDate));
            } else {
                String scheduledTime = ride.getScheduledTime();
                Date scheduled = parseDate(scheduledTime);
                if (scheduled != null) {
                    holder.startDate.setText(DATE_FORMAT.format(scheduled));
                    holder.startTime.setText(TIME_FORMAT.format(scheduled));
                } else {
                    holder.startDate.setText("—");
                    holder.startTime.setText("—");
                }
            }
            
            // Set end date/time
            if (endDate != null) {
                holder.endDate.setText(DATE_FORMAT.format(endDate));
                holder.endTime.setText(TIME_FORMAT.format(endDate));
            } else {
                holder.endDate.setText("—");
                holder.endTime.setText("—");
            }
            
            // Set locations
            if (ride.getEffectiveStartLocation() != null) {
                String pickup = ride.getEffectiveStartLocation().getAddress();
                holder.pickupAddress.setText(pickup != null ? truncate(pickup, 25) : "—");
            } else {
                holder.pickupAddress.setText("—");
            }
            
            if (ride.getEffectiveEndLocation() != null) {
                String dest = ride.getEffectiveEndLocation().getAddress();
                holder.dropoffAddress.setText(dest != null ? truncate(dest, 25) : "—");
            } else {
                holder.dropoffAddress.setText("—");
            }
            
            // Passenger count
            int passengerCount = ride.getPassengers() != null ? ride.getPassengers().size() : 1;
            holder.passengerCount.setText(String.valueOf(passengerCount));
            
            // Distance
            double distance = ride.getEffectiveDistance();
            holder.distance.setText(String.format(Locale.US, "%.2f km", distance));
            
            // Duration
            if (startDate != null && endDate != null) {
                long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
                holder.duration.setText(durationMinutes + " min");
            } else if (ride.getEstimatedTimeInMinutes() != null) {
                holder.duration.setText(ride.getEstimatedTimeInMinutes() + " min");
            } else {
                holder.duration.setText("—");
            }
            
            // Price column (we need to add this to the layout or repurpose an existing view)
            // For now, we could show it in the distance area
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRideClick(ride);
                }
            });
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

        @Override
        public int getItemCount() {
            return rides.size();
        }

        static class RideViewHolder extends RecyclerView.ViewHolder {
            final TextView startDate;
            final TextView startTime;
            final TextView endDate;
            final TextView endTime;
            final TextView pickupAddress;
            final TextView dropoffAddress;
            final TextView passengerCount;
            final TextView distance;
            final TextView duration;

            public RideViewHolder(View view) {
                super(view);
                startDate = view.findViewById(R.id.start_date);
                startTime = view.findViewById(R.id.start_time);
                endDate = view.findViewById(R.id.end_date);
                endTime = view.findViewById(R.id.end_time);
                pickupAddress = view.findViewById(R.id.pickup_address);
                dropoffAddress = view.findViewById(R.id.dropoff_address);
                passengerCount = view.findViewById(R.id.passenger_count);
                distance = view.findViewById(R.id.distance);
                duration = view.findViewById(R.id.duration);
            }
        }
    }
}