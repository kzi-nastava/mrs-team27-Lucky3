package com.example.mobile.ui.passenger;

import android.app.DatePickerDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Passenger Ride History Fragment.
 * Shows ride history with date filtering, status filtering, sorting by any field,
 * and pagination. Includes shake-to-sort sensor.
 */
public class PassengerHistoryFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "PassengerHistory";

    private SharedPreferencesManager preferencesManager;

    // UI elements
    private TextView btnFromDate, btnToDate, btnSortDirection, btnLoadMore, tvEmpty, tvRideCount;
    private TextView btnFilterAll, btnFilterPending, btnFilterAccepted, btnFilterFinished;
    private TextView btnFilterRejected, btnFilterCancelled;
    private Spinner spinnerSort;
    private ProgressBar progressBar;
    private ListView ridesListView;

    // State
    private String fromDate = null;
    private String toDate = null;
    private String statusFilter = null;
    private String sortField = "startTime";
    private boolean sortAsc = false;

    // Pagination
    private int currentPage = 0;
    private static final int PAGE_SIZE = 15;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    // Data
    private List<RideResponse> rides = new ArrayList<>();
    private PassengerRidesAdapter adapter;

    // Sort options
    private static final String[] SORT_OPTIONS = {
            "Start Time", "End Time", "Total Cost", "Distance"
    };
    private static final String[] SORT_FIELDS = {
            "startTime", "endTime", "totalCost", "distanceKm"
    };

    // Shake sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 12;
    private static final int SHAKE_COOLDOWN_MS = 1000;

    // Currently selected filter button
    private TextView activeFilterButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_passenger_history, container, false);

        preferencesManager = new SharedPreferencesManager(requireContext());

        initViews(root);
        setupNavbar(root);
        setupFilterButtons();
        setupDatePickers();
        setupSortSpinner();
        setupSortDirection();
        setupLoadMore();
        setupListView();
        setupShakeSensor();

        // Load all rides by default
        loadRides();

        return root;
    }

    private void initViews(View root) {
        btnFilterAll = root.findViewById(R.id.btn_filter_all);
        btnFilterPending = root.findViewById(R.id.btn_filter_pending);
        btnFilterAccepted = root.findViewById(R.id.btn_filter_accepted);
        btnFilterFinished = root.findViewById(R.id.btn_filter_finished);
        btnFilterRejected = root.findViewById(R.id.btn_filter_rejected);
        btnFilterCancelled = root.findViewById(R.id.btn_filter_cancelled);

        btnFromDate = root.findViewById(R.id.btn_from_date);
        btnToDate = root.findViewById(R.id.btn_to_date);
        btnSortDirection = root.findViewById(R.id.btn_sort_direction);
        btnLoadMore = root.findViewById(R.id.btn_load_more);
        tvEmpty = root.findViewById(R.id.tv_empty);
        tvRideCount = root.findViewById(R.id.tv_ride_count);
        spinnerSort = root.findViewById(R.id.spinner_sort);
        progressBar = root.findViewById(R.id.progress_bar);
        ridesListView = root.findViewById(R.id.rides_list_view);

        root.findViewById(R.id.btn_clear_dates).setOnClickListener(v -> clearDates());

        activeFilterButton = btnFilterAll;
    }

    private void setupNavbar(View root) {
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v ->
                    ((com.example.mobile.MainActivity) requireActivity()).openDrawer());
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");
        }
    }

    // ==================== FILTER BUTTONS ====================

    private void setupFilterButtons() {
        btnFilterAll.setOnClickListener(v -> applyStatusFilter(btnFilterAll, null));
        btnFilterPending.setOnClickListener(v -> applyStatusFilter(btnFilterPending, "PENDING"));
        btnFilterAccepted.setOnClickListener(v -> applyStatusFilter(btnFilterAccepted, "ACCEPTED"));
        btnFilterFinished.setOnClickListener(v -> applyStatusFilter(btnFilterFinished, "FINISHED"));
        btnFilterRejected.setOnClickListener(v -> applyStatusFilter(btnFilterRejected, "REJECTED"));
        btnFilterCancelled.setOnClickListener(v -> applyStatusFilter(btnFilterCancelled, "CANCELLED"));
    }

    private void applyStatusFilter(TextView button, String status) {
        // Update UI
        resetFilterButton(activeFilterButton);
        setActiveFilterButton(button);
        activeFilterButton = button;

        // Apply filter
        statusFilter = status;
        currentPage = 0;
        hasMorePages = true;
        rides.clear();
        adapter.notifyDataSetChanged();
        loadRides();
    }

    private void setActiveFilterButton(TextView button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
    }

    private void resetFilterButton(TextView button) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_900));
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
    }

    // ==================== DATE PICKERS ====================

    private void setupDatePickers() {
        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                R.style.DatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
                    if (isFromDate) {
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
                    // Auto-refresh
                    currentPage = 0;
                    hasMorePages = true;
                    rides.clear();
                    adapter.notifyDataSetChanged();
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
        if (!rides.isEmpty()) {
            currentPage = 0;
            hasMorePages = true;
            rides.clear();
            adapter.notifyDataSetChanged();
            loadRides();
        }
    }

    // ==================== SORT CONTROLS ====================

    private void setupSortSpinner() {
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, SORT_OPTIONS);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortField = SORT_FIELDS[position];
                if (!rides.isEmpty()) {
                    currentPage = 0;
                    hasMorePages = true;
                    rides.clear();
                    adapter.notifyDataSetChanged();
                    loadRides();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSortDirection() {
        updateSortDirectionUI();
        btnSortDirection.setOnClickListener(v -> toggleSortDirection());
    }

    private void toggleSortDirection() {
        sortAsc = !sortAsc;
        updateSortDirectionUI();
        currentPage = 0;
        hasMorePages = true;
        rides.clear();
        adapter.notifyDataSetChanged();
        loadRides();
    }

    private void updateSortDirectionUI() {
        btnSortDirection.setText(sortAsc ? "↑ ASC" : "↓ DESC");
    }

    // ==================== LIST & PAGINATION ====================

    private void setupLoadMore() {
        btnLoadMore.setOnClickListener(v -> {
            currentPage++;
            loadRides();
        });
    }

    private void setupListView() {
        adapter = new PassengerRidesAdapter(rides);
        ridesListView.setAdapter(adapter);
        ridesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < rides.size()) {
                RideResponse ride = rides.get(position);
                Bundle args = new Bundle();
                args.putLong("rideId", ride.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_passenger_history_to_passenger_ride_detail, args);
            }
        });
    }

    // ==================== SHAKE SENSOR ====================

    private void setupShakeSensor() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        toggleSortDirection();
                        Toast.makeText(getContext(),
                                "Sort order: " + (sortAsc ? "Oldest first" : "Newest first"),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // ==================== API CALLS ====================

    private void loadRides() {
        if (isLoading) return;
        isLoading = true;
        showLoading(true);

        String token = "Bearer " + preferencesManager.getToken();
        Long passengerId = preferencesManager.getUserId();
        String sortParam = sortField + "," + (sortAsc ? "asc" : "desc");

        ClientUtils.rideService.getRidesHistory(
                null,           // driverId
                passengerId,    // passengerId
                statusFilter,   // status
                fromDate,       // fromDate
                toDate,         // toDate
                currentPage,
                PAGE_SIZE,
                sortParam,
                token
        ).enqueue(new Callback<PageResponse<RideResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<RideResponse>> call,
                                   Response<PageResponse<RideResponse>> response) {
                isLoading = false;
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<RideResponse> page = response.body();
                    List<RideResponse> content = page.getContent();

                    if (content != null) {
                        if (currentPage == 0) {
                            rides.clear();
                        }
                        rides.addAll(content);
                        adapter.notifyDataSetChanged();
                        ListViewHelper.setListViewHeightBasedOnChildren(ridesListView);

                        hasMorePages = page.getNumber() != null && page.getTotalPages() != null
                                && page.getNumber() < page.getTotalPages() - 1;
                        btnLoadMore.setVisibility(hasMorePages ? View.VISIBLE : View.GONE);

                        // Update ride count
                        int total = page.getTotalElements() != null ? page.getTotalElements() : rides.size();
                        tvRideCount.setText(total + " ride" + (total != 1 ? "s" : "") + " found");
                        tvRideCount.setVisibility(View.VISIBLE);
                    }

                    tvEmpty.setVisibility(rides.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "Failed to load rides: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load rides", Toast.LENGTH_SHORT).show();
                    }
                    tvEmpty.setVisibility(rides.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<PageResponse<RideResponse>> call, Throwable t) {
                isLoading = false;
                showLoading(false);
                Log.e(TAG, "Network error loading rides", t);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
                tvEmpty.setVisibility(rides.isEmpty() ? View.VISIBLE : View.GONE);
                tvEmpty.setText("Network error. Please try again.");
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private String formatDateForApi(Date date, boolean endOfDay) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // ==================== Inner Adapter ====================

    private class PassengerRidesAdapter extends BaseAdapter {

        private final List<RideResponse> rides;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        PassengerRidesAdapter(List<RideResponse> rides) {
            this.rides = rides;
        }

        @Override
        public int getCount() { return rides.size(); }

        @Override
        public RideResponse getItem(int position) { return rides.get(position); }

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
                        .inflate(R.layout.item_passenger_ride, parent, false);
                holder = new ViewHolder();
                holder.tvStatus = convertView.findViewById(R.id.tv_status);
                holder.tvDate = convertView.findViewById(R.id.tv_date);
                holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
                holder.tvDestination = convertView.findViewById(R.id.tv_destination);
                holder.tvStartTime = convertView.findViewById(R.id.tv_start_time);
                holder.tvEndTime = convertView.findViewById(R.id.tv_end_time);
                holder.tvCost = convertView.findViewById(R.id.tv_cost);
                holder.tvDriverName = convertView.findViewById(R.id.tv_driver_name);
                holder.tvDistance = convertView.findViewById(R.id.tv_distance);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            RideResponse ride = getItem(position);

            // Status badge
            String displayStatus = ride.getDisplayStatus().toUpperCase();
            holder.tvStatus.setText(displayStatus);
            if (ride.isFinished()) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            } else if (ride.isCancelled()) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
            } else if (ride.isInProgress()) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_blue);
            } else if (ride.isScheduled()) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_gray);
            } else if ("PANIC".equals(ride.getStatus())) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_panic);
            } else {
                holder.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_active);
            }

            // Date
            Date startDate = parseDate(ride.getStartTime());
            if (startDate == null) startDate = parseDate(ride.getScheduledTime());
            holder.tvDate.setText(startDate != null ? dateFormat.format(startDate) : "—");

            // Route
            if (ride.getEffectiveStartLocation() != null) {
                String addr = ride.getEffectiveStartLocation().getAddress();
                holder.tvDeparture.setText(addr != null ? addr : "—");
            } else {
                holder.tvDeparture.setText("—");
            }
            if (ride.getEffectiveEndLocation() != null) {
                String addr = ride.getEffectiveEndLocation().getAddress();
                holder.tvDestination.setText(addr != null ? addr : "—");
            } else {
                holder.tvDestination.setText("—");
            }

            // Times
            Date endDate = parseDate(ride.getEndTime());
            holder.tvStartTime.setText("Start: " +
                    (startDate != null ? timeFormat.format(startDate) : "—"));
            holder.tvEndTime.setText("End: " +
                    (endDate != null ? timeFormat.format(endDate) : "—"));

            // Cost
            double cost = ride.getEffectiveCost();
            holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));

            // Driver name
            if (ride.getDriver() != null) {
                String dName = (ride.getDriver().getName() != null ? ride.getDriver().getName() : "") + " " +
                        (ride.getDriver().getSurname() != null ? ride.getDriver().getSurname() : "");
                holder.tvDriverName.setText("Driver: " + dName.trim());
                holder.tvDriverName.setVisibility(View.VISIBLE);
            } else {
                holder.tvDriverName.setVisibility(View.GONE);
            }

            // Distance
            double distance = ride.getEffectiveDistance();
            String durationStr = "—";
            if (startDate != null && endDate != null) {
                long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
                durationStr = durationMinutes + " min";
            } else if (ride.getEstimatedTimeInMinutes() != null) {
                durationStr = ride.getEstimatedTimeInMinutes() + " min";
            }
            holder.tvDistance.setText(String.format(Locale.US, "%.1f km • %s", distance, durationStr));

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

        class ViewHolder {
            TextView tvStatus, tvDate, tvDeparture, tvDestination;
            TextView tvStartTime, tvEndTime, tvCost, tvDriverName, tvDistance;
        }
    }
}
