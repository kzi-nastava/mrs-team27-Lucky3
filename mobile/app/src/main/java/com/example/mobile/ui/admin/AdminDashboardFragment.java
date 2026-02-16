package com.example.mobile.ui.admin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentAdminDashboardBinding;
import com.example.mobile.models.AdminStatsResponse;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private final List<RideResponse> rides = new ArrayList<>();
    private ActiveRidesAdapter adapter;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private Handler searchDebounceHandler;
    private Runnable searchDebounceRunnable;

    // Cache: driverId -> average driver rating
    private final Map<Long, Double> driverRatingCache = new HashMap<>();

    // Filter/sort state
    private String searchQuery = "";
    private String selectedStatus = null;       // null = All
    private String selectedVehicleType = null;   // null = All
    private String sortField = "status";
    private boolean sortAsc = true;
    private int currentPage = 0;
    private boolean spinnersInitialized = false;
    private int spinnerInitCount = 0;
    private static final int SPINNER_COUNT = 3;
    private static final int PAGE_SIZE = 50;
    private static final int INITIAL_DISPLAY = 3;
    private static final int LOAD_MORE_COUNT = 5;
    private int displayLimit = INITIAL_DISPLAY;

    // Spinner labels
    private static final String[] STATUS_LABELS = {"All", "In Progress", "Pending", "Scheduled"};
    private static final String[] STATUS_VALUES = {null, "IN_PROGRESS", "PENDING", "SCHEDULED"};
    private static final String[] VEHICLE_LABELS = {"All Types", "Standard", "Luxury", "Van"};
    private static final String[] VEHICLE_VALUES = {null, "STANDARD", "LUXURY", "VAN"};
    private static final String[] SORT_LABELS = {"Driver Name", "Status"};
    private static final String[] SORT_FIELDS = {"driver.name", "status"};

    private static final long REFRESH_INTERVAL_MS = 60_000; // 60 seconds

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupNavbar();
        setupSpinners();
        setupSearch();
        setupSortDirection();
        setupList();

        loadStats();
        loadRides();
        startAutoRefresh();
    }

    private void setupNavbar() {
        View navbar = binding.navbar.getRoot();
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v ->
                    ((com.example.mobile.MainActivity) requireActivity()).openDrawer());
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Admin Dashboard");
        }
    }

    private void setupSpinners() {
        // Status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, STATUS_LABELS);
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        binding.spinnerStatus.setAdapter(statusAdapter);
        binding.spinnerStatus.setOnItemSelectedListener(new SimpleSpinnerListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatus = STATUS_VALUES[position];
                if (spinnersInitialized) {
                    currentPage = 0;
                    loadRides();
                } else {
                    spinnerInitCount++;
                    if (spinnerInitCount >= SPINNER_COUNT) spinnersInitialized = true;
                }
            }
        });

        // Vehicle type spinner
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, VEHICLE_LABELS);
        vehicleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        binding.spinnerVehicleType.setAdapter(vehicleAdapter);
        binding.spinnerVehicleType.setOnItemSelectedListener(new SimpleSpinnerListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedVehicleType = VEHICLE_VALUES[position];
                if (spinnersInitialized) {
                    currentPage = 0;
                    loadRides();
                } else {
                    spinnerInitCount++;
                    if (spinnerInitCount >= SPINNER_COUNT) spinnersInitialized = true;
                }
            }
        });

        // Sort spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, SORT_LABELS);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        binding.spinnerSort.setAdapter(sortAdapter);
        binding.spinnerSort.setSelection(1);
        binding.spinnerSort.setOnItemSelectedListener(new SimpleSpinnerListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortField = SORT_FIELDS[position];
                if (spinnersInitialized) {
                    currentPage = 0;
                    loadRides();
                } else {
                    spinnerInitCount++;
                    if (spinnerInitCount >= SPINNER_COUNT) spinnersInitialized = true;
                }
            }
        });
    }

    private void setupSearch() {
        searchDebounceHandler = new Handler(Looper.getMainLooper());
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }
                searchDebounceRunnable = () -> {
                    searchQuery = s.toString().trim();
                    currentPage = 0;
                    loadRides();
                };
                searchDebounceHandler.postDelayed(searchDebounceRunnable, 400);
            }
        });
    }

    private void setupSortDirection() {
        updateSortDirectionLabel();
        binding.btnSortDirection.setOnClickListener(v -> {
            sortAsc = !sortAsc;
            updateSortDirectionLabel();
            currentPage = 0;
            loadRides();
        });
    }

    private void updateSortDirectionLabel() {
        binding.btnSortDirection.setText(sortAsc ? "\u2191" : "\u2193");
    }

    private void setupList() {
        adapter = new ActiveRidesAdapter();
        binding.lvActiveRides.setAdapter(adapter);

        binding.btnLoadMore.setOnClickListener(v -> {
            displayLimit += LOAD_MORE_COUNT;
            adapter.notifyDataSetChanged();
            updateLoadMoreVisibility();
        });
    }

    private void updateLoadMoreVisibility() {
        if (binding == null) return;
        boolean hasMore = rides.size() > displayLimit;
        binding.btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        if (hasMore) {
            int remaining = rides.size() - displayLimit;
            binding.btnLoadMore.setText("Load More (" + remaining + " remaining)");
        }
    }

    // ---- Data Loading ----

    private void loadStats() {
        String token = "Bearer " + new SharedPreferencesManager(requireContext()).getToken();
        ClientUtils.rideService.getAdminStats(token).enqueue(new Callback<AdminStatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<AdminStatsResponse> call,
                                   @NonNull Response<AdminStatsResponse> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    AdminStatsResponse s = response.body();
                    binding.tvStatActiveRides.setText(String.valueOf(s.getActiveRidesCount()));
                    binding.tvStatAvgRating.setText(s.getAverageDriverRating() > 0
                            ? String.format(Locale.US, "%.1f", s.getAverageDriverRating()) : "\u2014");
                    binding.tvStatDriversOnline.setText(String.valueOf(s.getDriversOnlineCount()));
                    binding.tvStatPassengers.setText(String.valueOf(s.getTotalPassengersInRides()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AdminStatsResponse> call, @NonNull Throwable t) {
                // silently fail for stats
            }
        });
    }

    private void loadRides() {
        if (!isAdded() || binding == null) return;
        binding.progressBar.setVisibility(View.VISIBLE);

        String token = "Bearer " + new SharedPreferencesManager(requireContext()).getToken();
        String sortParam = sortField + "," + (sortAsc ? "asc" : "desc");
        String search = searchQuery.isEmpty() ? null : searchQuery;

        ClientUtils.rideService.getAllActiveRides(
                currentPage, PAGE_SIZE, sortParam, search, selectedStatus, selectedVehicleType, token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(@NonNull Call<PageResponse<RideResponse>> call,
                                   @NonNull Response<PageResponse<RideResponse>> response) {
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    rides.clear();
                    rides.addAll(response.body().getContent());
                    if ("status".equals(sortField)) {
                        sortRidesByStatus();
                    }
                    displayLimit = INITIAL_DISPLAY;
                    adapter.notifyDataSetChanged();
                    fetchDriverRatings();
                    updateLoadMoreVisibility();

                    boolean empty = rides.isEmpty();
                    binding.lvActiveRides.setVisibility(empty ? View.GONE : View.VISIBLE);
                    binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                } else {
                    rides.clear();
                    adapter.notifyDataSetChanged();
                    binding.lvActiveRides.setVisibility(View.GONE);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PageResponse<RideResponse>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("Failed to load rides");
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.lvActiveRides.setVisibility(View.GONE);
            }
        });
    }

    private void fetchDriverRatings() {
        String token = "Bearer " + new SharedPreferencesManager(requireContext()).getToken();
        Set<Long> driverIds = new HashSet<>();
        for (RideResponse ride : rides) {
            if (ride.getDriver() != null && ride.getDriver().getId() != null) {
                driverIds.add(ride.getDriver().getId());
            }
        }
        for (Long driverId : driverIds) {
            if (driverRatingCache.containsKey(driverId)) continue;
            ClientUtils.driverService.getStats(driverId, token).enqueue(new Callback<DriverStatsResponse>() {
                @Override
                public void onResponse(@NonNull Call<DriverStatsResponse> call,
                                       @NonNull Response<DriverStatsResponse> response) {
                    if (!isAdded() || binding == null) return;
                    if (response.isSuccessful() && response.body() != null) {
                        Double avg = response.body().getAverageRating();
                        driverRatingCache.put(driverId, avg != null ? avg : 0.0);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<DriverStatsResponse> call, @NonNull Throwable t) {
                    // silently ignore
                }
            });
        }
    }

    // ---- Auto-refresh ----

    private void startAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = () -> {
            if (isAdded()) {
                loadStats();
                loadRides();
                refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        if (searchDebounceHandler != null && searchDebounceRunnable != null) {
            searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
        }
        binding = null;
    }

    // ---- Adapter ----

    private class ActiveRidesAdapter extends BaseAdapter {

        @Override
        public int getCount() { return Math.min(displayLimit, rides.size()); }

        @Override
        public RideResponse getItem(int position) { return rides.get(position); }

        @Override
        public long getItemId(int position) {
            return rides.get(position).getId() != null ? rides.get(position).getId() : position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_admin_active_ride, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RideResponse ride = getItem(position);
            bindRide(holder, ride);

            convertView.setOnClickListener(v -> {
                if (ride.getId() != null) {
                    Bundle args = new Bundle();
                    args.putLong("rideId", ride.getId());
                    Navigation.findNavController(v)
                            .navigate(R.id.action_admin_dashboard_to_admin_ride_detail, args);
                }
            });

            return convertView;
        }

        private void bindRide(ViewHolder h, RideResponse ride) {
            // Status badge
            String status = ride.getStatus() != null ? ride.getStatus() : "";
            h.tvStatus.setText(ride.getDisplayStatus());
            applyStatusStyle(h.tvStatus, status);

            // Ride ID
            h.tvRideId.setText(ride.getId() != null ? "#" + ride.getId() : "");

            // Driver
            RideResponse.DriverInfo driver = ride.getDriver();
            if (driver != null) {
                String name = "";
                if (driver.getName() != null) name = driver.getName();
                if (driver.getSurname() != null) name += " " + driver.getSurname();
                h.tvDriverName.setText(name.trim().isEmpty() ? "Unknown Driver" : name.trim());
                h.tvDriverEmail.setText(driver.getEmail() != null ? driver.getEmail() : "");

                // Profile picture
                loadDriverAvatar(h.ivDriverAvatar, driver.getProfilePicture());

                // Vehicle (from driver.vehicle)
                RideResponse.VehicleInfo vehicle = driver.getVehicle();
                if (vehicle != null) {
                    String vModel = vehicle.getModel() != null ? vehicle.getModel() : "";
                    String plates = vehicle.getLicensePlates() != null ? vehicle.getLicensePlates() : "";
                    h.tvVehicleInfo.setText(!vModel.isEmpty() || !plates.isEmpty()
                            ? vModel + " \u2022 " + plates : "—");

                    String vType = vehicle.getVehicleType() != null ? vehicle.getVehicleType() : "";
                    h.tvVehicleType.setText(vType);
                    h.tvVehicleTypeIcon.setText(getVehicleIcon(vType));
                } else {
                    // Fallback to ride-level fields
                    String vModel = ride.getModel() != null ? ride.getModel() : "";
                    String plates = ride.getLicensePlates() != null ? ride.getLicensePlates() : "";
                    h.tvVehicleInfo.setText(!vModel.isEmpty() || !plates.isEmpty()
                            ? vModel + " \u2022 " + plates : "—");
                    String vType = ride.getVehicleType() != null ? ride.getVehicleType() : "";
                    h.tvVehicleType.setText(vType);
                    h.tvVehicleTypeIcon.setText(getVehicleIcon(vType));
                }
            } else {
                h.tvDriverName.setText("No driver assigned");
                h.tvDriverEmail.setText("");
                h.ivDriverAvatar.setImageResource(R.drawable.ic_person);
                // Use ride-level vehicle fields
                String vModel = ride.getModel() != null ? ride.getModel() : "";
                String plates = ride.getLicensePlates() != null ? ride.getLicensePlates() : "";
                h.tvVehicleInfo.setText(!vModel.isEmpty() || !plates.isEmpty()
                        ? vModel + " \u2022 " + plates : "—");
                String vType = ride.getVehicleType() != null ? ride.getVehicleType() : "";
                h.tvVehicleType.setText(vType);
                h.tvVehicleTypeIcon.setText(getVehicleIcon(vType));
            }

            // Route
            if (ride.getDeparture() != null && ride.getDeparture().getAddress() != null) {
                h.tvDeparture.setText(ride.getDeparture().getAddress());
            } else {
                h.tvDeparture.setText("—");
            }
            if (ride.getDestination() != null && ride.getDestination().getAddress() != null) {
                h.tvDestination.setText(ride.getDestination().getAddress());
            } else {
                h.tvDestination.setText("—");
            }

            // Passengers
            int passengerCount = ride.getPassengers() != null ? ride.getPassengers().size() : 0;
            h.tvPassengers.setText("\uD83D\uDC65 " + passengerCount + " passenger" + (passengerCount != 1 ? "s" : ""));

            // Est. time left (only for IN_PROGRESS)
            if ("IN_PROGRESS".equals(status)) {
                int estMinLeft = computeEstimatedTimeLeft(ride);
                if (estMinLeft > 0) {
                    h.tvEstTime.setText("\u23F1 ~" + estMinLeft + " min");
                    h.tvEstTime.setVisibility(View.VISIBLE);
                } else {
                    h.tvEstTime.setText("\u23F1 —");
                    h.tvEstTime.setVisibility(View.VISIBLE);
                }
            } else {
                h.tvEstTime.setVisibility(View.GONE);
            }

            // Cost
            double cost = ride.getEffectiveCost();
            h.tvCost.setText(cost > 0 ? String.format(Locale.US, "%.0f RSD", cost) : "—");

            // Distance
            double dist = ride.getEffectiveDistance();
            h.tvDistance.setText(dist > 0 ? String.format(Locale.US, "\uD83D\uDCCD %.1f km", dist) : "");

            // Scheduled time
            if ("SCHEDULED".equals(status) && ride.getScheduledTime() != null) {
                try {
                    LocalDateTime scheduled = LocalDateTime.parse(ride.getScheduledTime(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    h.tvScheduledTime.setText("\uD83D\uDCC5 " + scheduled.format(
                            DateTimeFormatter.ofPattern("MMM d, HH:mm")));
                    h.tvScheduledTime.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    h.tvScheduledTime.setVisibility(View.GONE);
                }
            } else {
                h.tvScheduledTime.setVisibility(View.GONE);
            }

            // Rating + time active (small, at bottom)
            if (driver != null && driver.getId() != null && driverRatingCache.containsKey(driver.getId())) {
                double avgRating = driverRatingCache.get(driver.getId());
                h.tvRating.setText(avgRating > 0
                        ? String.format(Locale.US, "\u2B50 %.1f", avgRating)
                        : "\u2B50 \u2014");
            } else {
                h.tvRating.setText("\u2B50 \u2014");
            }
            h.tvTimeActive.setText("Active: \u2014");

            if ("IN_PROGRESS".equals(status) && ride.getStartTime() != null) {
                try {
                    LocalDateTime start = LocalDateTime.parse(ride.getStartTime(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    long mins = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
                    if (mins >= 60) {
                        h.tvTimeActive.setText("Active: " + (mins / 60) + "h " + (mins % 60) + "m");
                    } else {
                        h.tvTimeActive.setText("Active: " + mins + "m");
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // Panic indicator
            if (Boolean.TRUE.equals(ride.getPanicPressed())) {
                h.tvPanic.setVisibility(View.VISIBLE);
                // Red border on card
                if (h.cardRoot != null) {
                    h.cardRoot.setStrokeColor(0xFFEF4343);
                    h.cardRoot.setStrokeWidth(2);
                }
            } else {
                h.tvPanic.setVisibility(View.GONE);
                if (h.cardRoot != null) {
                    h.cardRoot.setStrokeWidth(0);
                }
            }
        }

        private int computeEstimatedTimeLeft(RideResponse ride) {
            // Method 1: use distance remaining at ~30 km/h
            Double distKm = ride.getDistanceKm();
            Double traveled = ride.getDistanceTraveled();
            if (distKm != null && traveled != null && distKm > traveled) {
                double remaining = distKm - traveled;
                return (int) Math.ceil((remaining / 30.0) * 60);
            }
            // Method 2: use estimatedTimeInMinutes minus elapsed
            if (ride.getEstimatedTimeInMinutes() != null && ride.getStartTime() != null) {
                try {
                    LocalDateTime start = LocalDateTime.parse(ride.getStartTime(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    long elapsed = ChronoUnit.MINUTES.between(start, LocalDateTime.now());
                    int remaining = ride.getEstimatedTimeInMinutes() - (int) elapsed;
                    return Math.max(remaining, 0);
                } catch (Exception e) { /* ignore */ }
            }
            // Method 3: just return estimated time
            if (ride.getEstimatedTimeInMinutes() != null) {
                return ride.getEstimatedTimeInMinutes();
            }
            return 0;
        }

        private void loadDriverAvatar(ImageView iv, String profilePicture) {
            if (profilePicture != null && !profilePicture.isEmpty()) {
                new Thread(() -> {
                    try {
                        byte[] decoded = Base64.decode(profilePicture, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bmp != null && isAdded()) {
                            requireActivity().runOnUiThread(() -> iv.setImageBitmap(bmp));
                        }
                    } catch (Exception e) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    iv.setImageResource(R.drawable.ic_person));
                        }
                    }
                }).start();
            } else {
                iv.setImageResource(R.drawable.ic_person);
            }
        }

        private String getVehicleIcon(String type) {
            if (type == null) return "\uD83D\uDE97";
            switch (type.toUpperCase()) {
                case "LUXURY": return "\uD83C\uDFC6";
                case "VAN": return "\uD83D\uDE90";
                default: return "\uD83D\uDE97";
            }
        }

        private void applyStatusStyle(TextView tv, String status) {
            int textColor;
            int bgColor;
            switch (status) {
                case "IN_PROGRESS":
                    textColor = 0xFF4CAF50;
                    bgColor = 0x1A4CAF50;
                    break;
                case "ACCEPTED":
                    textColor = 0xFF3B82F6;
                    bgColor = 0x1A3B82F6;
                    break;
                case "PENDING":
                    textColor = 0xFFFFD045;
                    bgColor = 0x1AFFD045;
                    break;
                case "SCHEDULED":
                    textColor = 0xFFBB86FC;
                    bgColor = 0x1ABB86FC;
                    break;
                default:
                    textColor = 0xFFB3B3B3;
                    bgColor = 0x1AB3B3B3;
                    break;
            }
            tv.setTextColor(textColor);
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.RECTANGLE);
            badge.setCornerRadius(24f);
            badge.setColor(bgColor);
            tv.setBackground(badge);
        }
    }

    private static class ViewHolder {
        final com.google.android.material.card.MaterialCardView cardRoot;
        final TextView tvStatus, tvRideId;
        final ImageView ivDriverAvatar;
        final TextView tvDriverName, tvDriverEmail;
        final TextView tvVehicleTypeIcon, tvVehicleInfo, tvVehicleType;
        final TextView tvDeparture, tvDestination;
        final TextView tvPassengers, tvEstTime, tvCost;
        final TextView tvDistance, tvScheduledTime;
        final TextView tvRating, tvTimeActive;
        final TextView tvPanic;

        ViewHolder(View v) {
            cardRoot = v.findViewById(R.id.card_root);
            tvStatus = v.findViewById(R.id.tv_status);
            tvRideId = v.findViewById(R.id.tv_ride_id);
            ivDriverAvatar = v.findViewById(R.id.iv_driver_avatar);
            tvDriverName = v.findViewById(R.id.tv_driver_name);
            tvDriverEmail = v.findViewById(R.id.tv_driver_email);
            tvVehicleTypeIcon = v.findViewById(R.id.tv_vehicle_type_icon);
            tvVehicleInfo = v.findViewById(R.id.tv_vehicle_info);
            tvVehicleType = v.findViewById(R.id.tv_vehicle_type);
            tvDeparture = v.findViewById(R.id.tv_departure);
            tvDestination = v.findViewById(R.id.tv_destination);
            tvPassengers = v.findViewById(R.id.tv_passengers);
            tvEstTime = v.findViewById(R.id.tv_est_time);
            tvCost = v.findViewById(R.id.tv_cost);
            tvDistance = v.findViewById(R.id.tv_distance);
            tvScheduledTime = v.findViewById(R.id.tv_scheduled_time);
            tvRating = v.findViewById(R.id.tv_rating);
            tvTimeActive = v.findViewById(R.id.tv_time_active);
            tvPanic = v.findViewById(R.id.tv_panic);
        }
    }

    private static int statusSortOrder(String status) {
        if (status == null) return 99;
        switch (status) {
            case "IN_PROGRESS": return 0;
            case "PENDING":     return 1;
            case "ACCEPTED":    return 2;
            case "SCHEDULED":   return 3;
            default:             return 99;
        }
    }

    private void sortRidesByStatus() {
        Collections.sort(rides, (a, b) -> {
            int cmp = Integer.compare(statusSortOrder(a.getStatus()), statusSortOrder(b.getStatus()));
            return sortAsc ? cmp : -cmp;
        });
    }

    // Helper to avoid initial spinner fire
    private abstract static class SimpleSpinnerListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }
}