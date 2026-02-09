package com.example.mobile.ui.admin;

import com.example.mobile.Domain.DriverInfoCard;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentAdminDriversBinding;
import com.example.mobile.models.DriverResponse;
import com.example.mobile.models.DriverStatsResponse;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.viewmodels.AdminDriversViewModel;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

public class AdminDriversFragment extends Fragment {
    private FragmentAdminDriversBinding binding;
    private AdminDriversViewModel viewModel;
    private AdminDriverAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminDriversBinding.inflate(inflater, container, false);

        // Navbar setup
        View navbar = binding.getRoot().findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Drivers");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminDriversViewModel.class);

        // Initialize adapter with empty list
        adapter = new AdminDriverAdapter(requireContext(), new ArrayList<>());
        binding.listDrivers.setAdapter(adapter);

        setupListeners();
        observeViewModel();

        // Load data from API
        viewModel.loadAllDrivers();
    }

    private void setupListeners() {
        // Add new driver button
        binding.btnAddNewDriver.setOnClickListener(v -> openAddDriverDialog());

        // Search button
        binding.ivSearch.setOnClickListener(v -> {
            String query = binding.etSearch.getText().toString();
            viewModel.search(query); // Pass to ViewModel
        });

        // Filter buttons
        binding.filterAll.setOnClickListener(v -> {
            viewModel.filterByStatus("All");
            updateFilterStyles("All");
        });

        binding.filterActive.setOnClickListener(v -> {
            viewModel.filterByStatus("Active");
            updateFilterStyles("Active");
        });

        binding.filterInactive.setOnClickListener(v -> {
            viewModel.filterByStatus("Inactive");
            updateFilterStyles("Inactive");
        });

        binding.filterSuspended.setOnClickListener(v -> {
            viewModel.filterByStatus("Suspended");
            updateFilterStyles("Suspended");
        });
    }

    private void observeViewModel() {
        // Observe displayed drivers and UPDATE ADAPTER HERE
        viewModel.getDisplayedDrivers().observe(getViewLifecycleOwner(), drivers -> {
            if (drivers != null) {
                // Map DriverResponse to DriverInfoCard
                ArrayList<DriverInfoCard> driverCards = new ArrayList<>();
                for (DriverResponse driver : drivers) {
                    driverCards.add(mapToDriverInfoCard(driver));
                }
                adapter.setDrivers(driverCards);
                ListViewHelper.setListViewHeightBasedOnChildren(binding.listDrivers);
            }
        });

        // Observe loading state
        viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                // Show progress bar if you have one
                // binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                // binding.progressBar.setVisibility(View.GONE);
            }
        });

        // Observe errors
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe toast messages
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add this helper method to your Fragment
    // UPDATED: Map with statistics data
    private DriverInfoCard mapToDriverInfoCard(DriverResponse driver) {
        // Determine status string based on driver state
        String status = "";
        if (driver.isBlocked()) {
            status = "Suspended";
        } else if (driver.isActive()) {
            status = "Active";
            if (driver.getActive24h() != null && !driver.getActive24h().isEmpty()) {
                status += ",Online";
            }
        } else {
            status = "Inactive";
        }

        // GET STATS FROM VIEWMODEL
        DriverStatsResponse stats = viewModel.getDriverStats(driver.getId());

        float rating = 0.0f;
        int totalRides = 0;
        double earnings = 0.0;

        if (stats != null) {
            rating = stats.getAverageRating() != null ? stats.getAverageRating().floatValue() : 0.0f;
            totalRides = stats.getCompletedRides() != null ? stats.getCompletedRides() : 0;
            earnings = stats.getTotalEarnings() != null ? stats.getTotalEarnings() : 0.0;
        }

        return new DriverInfoCard(
                driver.getName() != null ? driver.getName() : "",
                driver.getSurname() != null ? driver.getSurname() : "",
                driver.getEmail() != null ? driver.getEmail() : "",
                driver.getProfilePictureUrl() != null ? driver.getProfilePictureUrl() : "",
                driver.getVehicle() != null && driver.getVehicle().getLicenseNumber() != null
                        ? driver.getVehicle().getLicenseNumber() : "",
                driver.getVehicle() != null && driver.getVehicle().getModel() != null
                        ? driver.getVehicle().getModel() : "",
                rating,           // ✅ Real rating from stats
                status,
                totalRides,       // ✅ Real total rides from stats
                earnings          // ✅ Real earnings from stats
        );
    }


    private void openAddDriverDialog() {
        // Update dialog to pass CreateDriverRequest instead of DriverResponse
        AdminAddsDriverDialog dialog = new AdminAddsDriverDialog((request, imageFile) -> {
            // Call ViewModel to make API call
            viewModel.createDriver(request, imageFile);
        });

        dialog.show(getParentFragmentManager(), "AddDriverDialog");
    }

    private void updateFilterStyles(String selectedFilter) {
        // Reset all to gray
        resetFilterStyle(binding.filterAll);
        resetFilterStyle(binding.filterActive);
        resetFilterStyle(binding.filterInactive);
        resetFilterStyle(binding.filterSuspended);

        // Highlight selected
        switch (selectedFilter) {
            case "All":
                setSelectedFilterStyle(binding.filterAll);
                break;
            case "Active":
                setSelectedFilterStyle(binding.filterActive);
                break;
            case "Inactive":
                setSelectedFilterStyle(binding.filterInactive);
                break;
            case "Suspended":
                setSelectedFilterStyle(binding.filterSuspended);
                break;
        }
    }

    private void resetFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_card);
        tv.setTextColor(getResources().getColor(R.color.gray_400));
    }

    private void setSelectedFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_rounded_yellow);
        tv.setTextColor(getResources().getColor(R.color.black));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
