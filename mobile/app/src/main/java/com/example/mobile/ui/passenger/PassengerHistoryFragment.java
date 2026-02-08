package com.example.mobile.ui.passenger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentPassengerHistoryBinding;
import com.example.mobile.ui.driver.RideHistoryAdapter;
import com.google.android.material.button.MaterialButton;

public class PassengerHistoryFragment extends Fragment {

    private FragmentPassengerHistoryBinding binding;
    private PassengerHistoryViewModel viewModel;
    private RideHistoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPassengerHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Navbar setup
        View navbar = binding.navbar.getRoot();
        navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
            ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
        });
        ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PassengerHistoryViewModel.class);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter buttons
        setupFilterButtons();

        // Observe LiveData
        observeViewModel();

        // Load rides
        viewModel.loadRides();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter();
        binding.recyclerRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRides.setAdapter(adapter);
    }

    private void setupFilterButtons() {
        binding.btnFilterAll.setOnClickListener(v -> updateFilter(binding.btnFilterAll, null));
        binding.btnFilterPending.setOnClickListener(v -> updateFilter(binding.btnFilterPending, "PENDING"));
        binding.btnFilterFinished.setOnClickListener(v -> updateFilter(binding.btnFilterFinished, "FINISHED"));
        binding.btnFilterRejected.setOnClickListener(v -> updateFilter(binding.btnFilterRejected, "REJECTED"));
        binding.btnFilterCanceled.setOnClickListener(v -> updateFilter(binding.btnFilterCanceled, "CANCELED"));
    }

    private void updateFilter(MaterialButton selectedButton, String status) {
        // Reset all buttons to inactive state
        resetFilterButton(binding.btnFilterAll);
        resetFilterButton(binding.btnFilterPending);
        resetFilterButton(binding.btnFilterFinished);
        resetFilterButton(binding.btnFilterRejected);
        resetFilterButton(binding.btnFilterCanceled);

        // Set selected button to active state
        selectedButton.setBackgroundTintList(getResources().getColorStateList(R.color.yellow, null));
        selectedButton.setTextColor(getResources().getColor(R.color.black, null));

        // Apply filter
        adapter.filter(status);
    }

    private void resetFilterButton(MaterialButton button) {
        button.setBackgroundTintList(getResources().getColorStateList(R.color.light_gray, null));
        button.setTextColor(getResources().getColor(R.color.gray, null));
    }

    private void observeViewModel() {
        viewModel.getRides().observe(getViewLifecycleOwner(), rides -> {
            if (rides != null && !rides.isEmpty()) {
                adapter.setRides(rides);
                binding.emptyState.setVisibility(View.GONE);
                binding.recyclerRides.setVisibility(View.VISIBLE);
            } else {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.recyclerRides.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
