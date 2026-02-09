package com.example.mobile.ui.admin;

import android.app.DatePickerDialog;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.PageResponse;
import com.example.mobile.models.RideResponse;
import com.example.mobile.models.UserResponse;
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
 * Admin Ride History Fragment.
 * Allows administrators to view ride history for any driver or passenger.
 * Features: search by driver/passenger ID, date filtering, status filtering,
 * sorting by any field, pagination.
 */
public class AdminRideHistoryFragment extends Fragment {

    private static final String TAG = "AdminRideHistory";

    private SharedPreferencesManager preferencesManager;

    // UI elements
    private TextView btnSearchDriver, btnSearchPassenger, btnSearch, btnSortDirection, btnShowAll;
    private TextView btnFromDate, btnToDate, btnLoadMore, tvEmpty;
    private EditText etUserId;
    private Spinner spinnerStatus, spinnerSort;
    private ProgressBar progressBar;
    private ListView ridesListView;
    private LinearLayout userInfoBanner;
    private TextView tvUserName, tvUserEmail, tvRideCount;

    // State
    private String searchType = "driver"; // "driver" or "passenger"
    private Long searchUserId = null;
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
    private AdminRidesAdapter adapter;

    // Sort/Status options
    private static final String[] STATUS_OPTIONS = {
            "All", "FINISHED", "CANCELLED", "CANCELLED_BY_DRIVER", "CANCELLED_BY_PASSENGER",
            "PENDING", "ACCEPTED", "IN_PROGRESS", "REJECTED", "PANIC", "SCHEDULED"
    };
    private static final String[] SORT_OPTIONS = {
            "Start Time", "End Time", "Total Cost", "Status", "Distance"
    };
    private static final String[] SORT_FIELDS = {
            "startTime", "endTime", "totalCost", "status", "distanceKm"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_ride_history, container, false);

        preferencesManager = new SharedPreferencesManager(requireContext());

        initViews(root);
        setupNavbar(root);
        setupSearchTypeToggle();
        setupSearchButton();
        setupDatePickers();
        setupSpinners();
        setupSortDirection();
        setupLoadMore();
        setupListView();

        // Load all rides by default
        loadRides();

        return root;
    }

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

        root.findViewById(R.id.btn_clear_dates).setOnClickListener(v -> clearDates());

        // Set default banner to "All Rides"
        tvUserName.setText("All Rides");
        tvUserEmail.setText("Showing all ride history");
        userInfoBanner.setVisibility(View.VISIBLE);
    }

    private void setupNavbar(View root) {
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v ->
                    ((com.example.mobile.MainActivity) requireActivity()).openDrawer());
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");
        }
    }

    private void setupSearchTypeToggle() {
        btnSearchDriver.setOnClickListener(v -> {
            searchType = "driver";
            updateSearchTypeUI();
            etUserId.setHint("Enter Driver ID");
        });
        btnSearchPassenger.setOnClickListener(v -> {
            searchType = "passenger";
            updateSearchTypeUI();
            etUserId.setHint("Enter Passenger ID");
        });
        updateSearchTypeUI();
    }

    private void updateSearchTypeUI() {
        if ("driver".equals(searchType)) {
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

    private void setupSearchButton() {
        btnSearch.setOnClickListener(v -> {
            String idStr = etUserId.getText().toString().trim();
            if (idStr.isEmpty()) {
                // Search all rides (no user filter)
                searchUserId = null;
                showAllRidesBanner();
            } else {
                try {
                    searchUserId = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please enter a valid numeric ID", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            currentPage = 0;
            hasMorePages = true;
            rides.clear();
            adapter.notifyDataSetChanged();
            loadRides();
        });

        // Show All button — clears user filter and resets
        if (btnShowAll != null) {
            btnShowAll.setOnClickListener(v -> {
                searchUserId = null;
                etUserId.setText("");
                fromDate = null;
                toDate = null;
                btnFromDate.setText("");
                btnToDate.setText("");
                btnFromDate.setHint("Start date");
                btnToDate.setHint("End date");
                statusFilter = null;
                spinnerStatus.setSelection(0);
                sortField = "startTime";
                spinnerSort.setSelection(0);
                sortAsc = false;
                updateSortDirectionUI();
                showAllRidesBanner();
                currentPage = 0;
                hasMorePages = true;
                rides.clear();
                adapter.notifyDataSetChanged();
                loadRides();
            });
        }
    }

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
        // Refresh if already searched
        if (searchUserId != null || !rides.isEmpty()) {
            currentPage = 0;
            hasMorePages = true;
            rides.clear();
            adapter.notifyDataSetChanged();
            loadRides();
        }
    }

    private void setupSpinners() {
        // Status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, STATUS_OPTIONS);
        statusAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                statusFilter = position == 0 ? null : STATUS_OPTIONS[position];
                // Don't auto-search on initial spinner setup
                if (searchUserId != null || !rides.isEmpty()) {
                    currentPage = 0;
                    hasMorePages = true;
                    rides.clear();
                    adapter.notifyDataSetChanged();
                    loadRides();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Sort spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_dark, SORT_OPTIONS);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_dark);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortField = SORT_FIELDS[position];
                if (searchUserId != null || !rides.isEmpty()) {
                    currentPage = 0;
                    hasMorePages = true;
                    rides.clear();
                    adapter.notifyDataSetChanged();
                    loadRides();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSortDirection() {
        updateSortDirectionUI();
        btnSortDirection.setOnClickListener(v -> {
            sortAsc = !sortAsc;
            updateSortDirectionUI();
            if (searchUserId != null || !rides.isEmpty()) {
                currentPage = 0;
                hasMorePages = true;
                rides.clear();
                adapter.notifyDataSetChanged();
                loadRides();
            }
        });
    }

    private void updateSortDirectionUI() {
        btnSortDirection.setText(sortAsc ? "↑ ASC" : "↓ DESC");
    }

    private void setupLoadMore() {
        btnLoadMore.setOnClickListener(v -> {
            currentPage++;
            loadRides();
        });
    }

    private void setupListView() {
        adapter = new AdminRidesAdapter(rides);
        ridesListView.setAdapter(adapter);
        ridesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < rides.size()) {
                RideResponse ride = rides.get(position);
                Bundle args = new Bundle();
                args.putLong("rideId", ride.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_admin_ride_history_to_admin_ride_detail, args);
            }
        });
    }

    private void loadRides() {
        if (isLoading) return;
        isLoading = true;
        showLoading(true);

        String token = "Bearer " + preferencesManager.getToken();
        Long driverId = "driver".equals(searchType) ? searchUserId : null;
        Long passengerId = "passenger".equals(searchType) ? searchUserId : null;
        String sortParam = sortField + "," + (sortAsc ? "asc" : "desc");

        ClientUtils.rideService.getRidesHistory(
                driverId,
                passengerId,
                statusFilter,
                fromDate,
                toDate,
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
                        tvRideCount.setText(total + " rides found");

                        // Update user info banner
                        if (searchUserId != null && !rides.isEmpty()) {
                            updateUserInfoFromRides(driverId, passengerId);
                        } else if (searchUserId == null) {
                            showAllRidesBanner();
                        }
                    }

                    // Show empty state
                    tvEmpty.setVisibility(rides.isEmpty() ? View.VISIBLE : View.GONE);
                    if (rides.isEmpty()) {
                        tvEmpty.setText(searchUserId == null
                                ? "No rides found"
                                : "No rides found for this user");
                    }
                } else {
                    Log.e(TAG, "Failed to load rides: " + response.code());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load rides (Error " + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                    tvEmpty.setVisibility(rides.isEmpty() ? View.VISIBLE : View.GONE);
                    tvEmpty.setText("Failed to load rides");
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

    private void updateUserInfoFromRides(Long driverId, Long passengerId) {
        if (rides.isEmpty()) {
            userInfoBanner.setVisibility(View.GONE);
            return;
        }

        RideResponse firstRide = rides.get(0);
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

        tvUserName.setText(name.trim());
        tvUserEmail.setText(email);
        userInfoBanner.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showAllRidesBanner() {
        tvUserName.setText("All Rides");
        tvUserEmail.setText("Showing all ride history");
        userInfoBanner.setVisibility(View.VISIBLE);
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

            // Cancelled by
            if (ride.isCancelled()) {
                String cancelledBy = ride.getCancelledBy();
                holder.tvCancelledBy.setVisibility(View.VISIBLE);
                holder.tvCancelledBy.setText("Cancelled by " + cancelledBy);
            } else {
                holder.tvCancelledBy.setVisibility(View.GONE);
            }

            // Highlight panic rides with red border effect
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
            TextView tvStartTime, tvEndTime, tvCost, tvCancelledBy;
            LinearLayout cardContent;
        }
    }
}
