package com.example.mobile.ui.admin;

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.RideResponse;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.viewmodels.AdminRideHistoryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.mobile.utils.NavbarHelper;

/**
 * Admin Ride History Fragment.
 * Allows administrators to view ride history for any driver or passenger.
 * Features: search by driver/passenger ID, date filtering, status filtering,
 * sorting by any field, pagination, and shake-to-sort.
 *
 * Uses AdminRideHistoryViewModel to survive configuration changes (e.g. orientation).
 * Implements SensorEventListener to detect device shake events that sort by start time.
 */
public class AdminRideHistoryFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "AdminRideHistory";

    // Shake detection thresholds
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final long SHAKE_COOLDOWN_MS = 1000;

    private AdminRideHistoryViewModel viewModel;

    // Sensor for shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;

    // UI elements
    private TextView btnSearchDriver, btnSearchPassenger, btnSearch, btnSortDirection, btnShowAll;
    private TextView btnFromDate, btnToDate, btnLoadMore, tvEmpty;
    private EditText etUserId;
    private Spinner spinnerStatus, spinnerSort;
    private ProgressBar progressBar;
    private ListView ridesListView;
    private LinearLayout userInfoBanner;
    private TextView tvUserName, tvUserEmail, tvRideCount;
    private TextView tvShakeHint;

    // Adapter
    private AdminRidesAdapter adapter;
    private List<RideResponse> adapterRides = new ArrayList<>();

    // Prevent spinner callbacks during programmatic setup
    private boolean suppressSpinnerCallbacks = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModel scoped to this fragment — survives config changes
        viewModel = new ViewModelProvider(this).get(AdminRideHistoryViewModel.class);

        // Initialize shake sensor
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_ride_history, container, false);

        initViews(root);
        setupNavbar(root);
        setupSearchTypeToggle();
        setupSearchButton();
        setupDatePickers();
        setupSpinners();
        setupSortDirection();
        setupLoadMore();
        setupListView();

        // Observe ViewModel LiveData
        observeViewModel();

        // Restore UI state from ViewModel (after config change)
        restoreStateFromViewModel();

        // Load rides only on first creation
        if (!viewModel.isInitialLoadDone()) {
            viewModel.resetAndLoad();
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register shake sensor listener
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister shake sensor listener to save battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // ========================== Shake Detection (SensorEventListener) ==========================

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calculate acceleration magnitude (minus gravity ~9.8)
        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > SHAKE_THRESHOLD) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                lastShakeTime = now;
                onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    /**
     * Called when a shake event is detected.
     * Forces sort by start time and toggles the sorting direction (ASC / DESC), then reloads rides.
     */
    private void onShakeDetected() {
        if (getActivity() == null) return;
        requireActivity().runOnUiThread(() -> {
            // Force sort field to Start Time and toggle direction
            viewModel.setSortField("startTime");
            viewModel.setSortSpinnerPosition(0);
            if (spinnerSort != null) {
                suppressSpinnerCallbacks = true;
                spinnerSort.setSelection(0); // "Start Time" is index 0
                spinnerSort.post(() -> suppressSpinnerCallbacks = false);
            }
            viewModel.toggleSortDirection();
            Toast.makeText(getContext(),
                    "Sort by start time: " + (viewModel.isSortAsc() ? "Oldest first" : "Newest first"),
                    Toast.LENGTH_SHORT).show();
        });
    }

    // ========================== View Initialization ==========================

    private void initViews(View root) {
        btnSearchDriver = root.findViewById(R.id.btn_search_driver);
        btnSearchPassenger = root.findViewById(R.id.btn_search_passenger);
        btnSearch = root.findViewById(R.id.btn_search);
        etUserId = root.findViewById(R.id.et_user_id);
        spinnerStatus = root.findViewById(R.id.spinner_status);
        spinnerSort = root.findViewById(R.id.spinner_sort);
        btnSortDirection = root.findViewById(R.id.btn_sort_direction);
        btnFromDate = root.findViewById(R.id.btn_from_date);
        btnToDate = root.findViewById(R.id.btn_to_date);
        btnLoadMore = root.findViewById(R.id.btn_load_more);
        tvEmpty = root.findViewById(R.id.tv_empty);
        progressBar = root.findViewById(R.id.progress_bar);
        ridesListView = root.findViewById(R.id.rides_list_view);
        userInfoBanner = root.findViewById(R.id.user_info_banner);
        tvUserName = root.findViewById(R.id.tv_user_name);
        tvUserEmail = root.findViewById(R.id.tv_user_email);
        tvRideCount = root.findViewById(R.id.tv_ride_count);
        btnShowAll = root.findViewById(R.id.btn_show_all);
        tvShakeHint = root.findViewById(R.id.tv_shake_hint);

        root.findViewById(R.id.btn_clear_dates).setOnClickListener(v -> clearDates());
    }

    private void setupNavbar(View root) {
        NavbarHelper.setup(this, root, "Ride History");
    }

    // ========================== Search Type Toggle ==========================

    private void setupSearchTypeToggle() {
        btnSearchDriver.setOnClickListener(v -> {
            viewModel.setSearchType("driver");
            updateSearchTypeUI();
            etUserId.setHint("Enter Driver ID");
        });
        btnSearchPassenger.setOnClickListener(v -> {
            viewModel.setSearchType("passenger");
            updateSearchTypeUI();
            etUserId.setHint("Enter Passenger ID");
        });
        updateSearchTypeUI();
    }

    private void updateSearchTypeUI() {
        if ("driver".equals(viewModel.getSearchType())) {
            btnSearchDriver.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            btnSearchDriver.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            btnSearchPassenger.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_900));
            btnSearchPassenger.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        } else {
            btnSearchPassenger.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow_500));
            btnSearchPassenger.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            btnSearchDriver.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray_900));
            btnSearchDriver.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
        }
    }

    // ========================== Search Button ==========================

    private void setupSearchButton() {
        btnSearch.setOnClickListener(v -> {
            String idStr = etUserId.getText().toString().trim();
            if (idStr.isEmpty()) {
                viewModel.setSearchUserId(null);
            } else {
                try {
                    viewModel.setSearchUserId(Long.parseLong(idStr));
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please enter a valid numeric ID", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            viewModel.resetAndLoad();
        });

        if (btnShowAll != null) {
            btnShowAll.setOnClickListener(v -> {
                etUserId.setText("");
                btnFromDate.setText("");
                btnToDate.setText("");
                btnFromDate.setHint("Start date");
                btnToDate.setHint("End date");

                suppressSpinnerCallbacks = true;
                spinnerStatus.setSelection(0);
                spinnerSort.setSelection(0);
                suppressSpinnerCallbacks = false;

                updateSortDirectionUI(false);
                viewModel.clearAllFilters();
            });
        }
    }

    // ========================== Date Pickers ==========================

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
                        String apiDate = viewModel.formatDateForApi(selected.getTime(), false);
                        String display = displayFormat.format(selected.getTime());
                        viewModel.setFromDate(apiDate);
                        viewModel.setFromDateDisplay(display);
                        btnFromDate.setText(display);
                    } else {
                        selected.set(Calendar.HOUR_OF_DAY, 23);
                        selected.set(Calendar.MINUTE, 59);
                        selected.set(Calendar.SECOND, 59);
                        String apiDate = viewModel.formatDateForApi(selected.getTime(), true);
                        String display = displayFormat.format(selected.getTime());
                        viewModel.setToDate(apiDate);
                        viewModel.setToDateDisplay(display);
                        btnToDate.setText(display);
                    }
                    viewModel.resetAndLoad();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void clearDates() {
        viewModel.setFromDate(null);
        viewModel.setToDate(null);
        viewModel.setFromDateDisplay(null);
        viewModel.setToDateDisplay(null);
        btnFromDate.setText("");
        btnToDate.setText("");
        btnFromDate.setHint("Start date");
        btnToDate.setHint("End date");
        viewModel.resetAndLoad();
    }

    // ========================== Spinners ==========================

    private void setupSpinners() {
        // Status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, AdminRideHistoryViewModel.STATUS_OPTIONS);
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallbacks) return;
                viewModel.setStatusSpinnerPosition(position);
                viewModel.setStatusFilter(position == 0 ? null : AdminRideHistoryViewModel.STATUS_VALUES[position]);
                viewModel.resetAndLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, AdminRideHistoryViewModel.SORT_OPTIONS);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallbacks) return;
                viewModel.setSortSpinnerPosition(position);
                viewModel.setSortField(AdminRideHistoryViewModel.SORT_FIELDS[position]);
                viewModel.resetAndLoad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ========================== Sort Direction ==========================

    private void setupSortDirection() {
        updateSortDirectionUI(viewModel.isSortAsc());
        btnSortDirection.setOnClickListener(v -> viewModel.toggleSortDirection());
    }

    private void updateSortDirectionUI(boolean asc) {
        btnSortDirection.setText(asc ? "↑ ASC" : "↓ DESC");
    }

    // ========================== Load More ==========================

    private void setupLoadMore() {
        btnLoadMore.setOnClickListener(v -> viewModel.loadNextPage());
    }

    // ========================== ListView ==========================

    private void setupListView() {
        adapter = new AdminRidesAdapter(adapterRides);
        ridesListView.setAdapter(adapter);
        ridesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < adapterRides.size()) {
                RideResponse ride = adapterRides.get(position);
                Bundle args = new Bundle();
                args.putLong("rideId", ride.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_admin_ride_history_to_admin_ride_detail, args);
            }
        });
    }

    // ========================== Observe ViewModel ==========================

    private void observeViewModel() {
        // Rides list
        viewModel.getRides().observe(getViewLifecycleOwner(), rides -> {
            adapterRides.clear();
            if (rides != null) {
                adapterRides.addAll(rides);
            }
            adapter.notifyDataSetChanged();
            ListViewHelper.setListViewHeightBasedOnChildren(ridesListView);
        });

        // Loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        // More pages -> show/hide Load More button
        viewModel.getMorePages().observe(getViewLifecycleOwner(), more -> {
            btnLoadMore.setVisibility(Boolean.TRUE.equals(more) ? View.VISIBLE : View.GONE);
        });

        // Total elements -> ride count badge
        viewModel.getTotalElements().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                tvRideCount.setText(total + " rides found");
            }
        });

        // Error messages -> Toast
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getContext() != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Empty state text
        viewModel.getEmptyText().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(text);
            } else {
                tvEmpty.setVisibility(View.GONE);
            }
        });

        // Sort direction changed (from shake or manual toggle)
        viewModel.getSortDirectionChanged().observe(getViewLifecycleOwner(), asc -> {
            if (asc != null) {
                updateSortDirectionUI(asc);
            }
        });

        // Banner name and email
        viewModel.getBannerName().observe(getViewLifecycleOwner(), name -> {
            tvUserName.setText(name != null ? name : "All Rides");
            userInfoBanner.setVisibility(View.VISIBLE);
        });
        viewModel.getBannerEmail().observe(getViewLifecycleOwner(), email -> {
            tvUserEmail.setText(email != null ? email : "");
        });
    }

    // ========================== Restore State After Config Change ==========================

    /**
     * Restores all UI widgets from ViewModel state after orientation change.
     */
    private void restoreStateFromViewModel() {
        // Suppress spinner callbacks during restoration
        suppressSpinnerCallbacks = true;

        // Search type toggle
        updateSearchTypeUI();
        etUserId.setHint("driver".equals(viewModel.getSearchType())
                ? "Enter Driver ID" : "Enter Passenger ID");

        // User ID
        if (viewModel.getSearchUserId() != null) {
            etUserId.setText(String.valueOf(viewModel.getSearchUserId()));
        }

        // Date displays
        if (viewModel.getFromDateDisplay() != null) {
            btnFromDate.setText(viewModel.getFromDateDisplay());
        }
        if (viewModel.getToDateDisplay() != null) {
            btnToDate.setText(viewModel.getToDateDisplay());
        }

        // Spinner positions
        spinnerStatus.setSelection(viewModel.getStatusSpinnerPosition());
        spinnerSort.setSelection(viewModel.getSortSpinnerPosition());

        // Sort direction
        updateSortDirectionUI(viewModel.isSortAsc());

        // Re-enable spinner callbacks after a short delay
        spinnerStatus.post(() -> suppressSpinnerCallbacks = false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // ==================== Inner Adapter ====================

    private class AdminRidesAdapter extends BaseAdapter {

        private final List<RideResponse> rides;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        AdminRidesAdapter(List<RideResponse> rides) {
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
                        .inflate(R.layout.item_admin_ride, parent, false);
                holder = new ViewHolder();
                holder.tvStatus = convertView.findViewById(R.id.tv_status);
                holder.tvPanic = convertView.findViewById(R.id.tv_panic);
                holder.tvDate = convertView.findViewById(R.id.tv_date);
                holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
                holder.tvDestination = convertView.findViewById(R.id.tv_destination);
                holder.tvStartTime = convertView.findViewById(R.id.tv_start_time);
                holder.tvEndTime = convertView.findViewById(R.id.tv_end_time);
                holder.tvCost = convertView.findViewById(R.id.tv_cost);
                holder.tvDistance = convertView.findViewById(R.id.tv_distance);
                holder.tvCancelledBy = convertView.findViewById(R.id.tv_cancelled_by);
                holder.cardContent = convertView.findViewById(R.id.card_content);
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

            // Panic indicator
            boolean hasPanic = Boolean.TRUE.equals(ride.getPanicPressed());
            holder.tvPanic.setVisibility(hasPanic ? View.VISIBLE : View.GONE);

            // Date
            Date startDate = parseDate(ride.getStartTime());
            if (startDate == null) startDate = parseDate(ride.getScheduledTime());
            holder.tvDate.setText(startDate != null ? dateFormat.format(startDate) : "\u2014");

            // Route
            if (ride.getEffectiveStartLocation() != null) {
                String addr = ride.getEffectiveStartLocation().getAddress();
                holder.tvDeparture.setText(addr != null ? addr : "\u2014");
            } else {
                holder.tvDeparture.setText("\u2014");
            }
            if (ride.getEffectiveEndLocation() != null) {
                String addr = ride.getEffectiveEndLocation().getAddress();
                holder.tvDestination.setText(addr != null ? addr : "\u2014");
            } else {
                holder.tvDestination.setText("\u2014");
            }

            // Times
            Date endDate = parseDate(ride.getEndTime());
            holder.tvStartTime.setText("Start: " +
                    (startDate != null ? timeFormat.format(startDate) : "\u2014"));
            holder.tvEndTime.setText("End: " +
                    (endDate != null ? timeFormat.format(endDate) : "\u2014"));

            // Cost
            double cost = ride.getEffectiveCost();
            holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));

            // Distance + duration
            double distance = ride.getEffectiveDistance();
            String durationStr = "\u2014";
            if (startDate != null && endDate != null) {
                long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
                durationStr = durationMinutes + " min";
            } else if (ride.getEstimatedTimeInMinutes() != null) {
                durationStr = ride.getEstimatedTimeInMinutes() + " min";
            }
            holder.tvDistance.setText(String.format(Locale.US, "%.1f km • %s", distance, durationStr));

            // Cancelled by
            if (ride.isCancelled()) {
                String cancelledBy = ride.getCancelledBy();
                holder.tvCancelledBy.setVisibility(View.VISIBLE);
                holder.tvCancelledBy.setText("Cancelled by " + cancelledBy);
            } else {
                holder.tvCancelledBy.setVisibility(View.GONE);
            }

            // Highlight panic rides with red tint
            if (hasPanic) {
                holder.cardContent.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.red_500_10));
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

        class ViewHolder {
            TextView tvStatus, tvPanic, tvDate, tvDeparture, tvDestination;
            TextView tvStartTime, tvEndTime, tvCost, tvDistance, tvCancelledBy;
            LinearLayout cardContent;
        }
    }
}
