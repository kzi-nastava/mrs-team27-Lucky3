package com.example.mobile.ui.passenger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentPassengerHistoryBinding;
import com.example.mobile.ui.passenger.PassengerRideAdapter;
import com.google.android.material.button.MaterialButton;

public class PassengerHistoryFragment extends Fragment {

    private FragmentPassengerHistoryBinding binding;
    private PassengerHistoryViewModel viewModel;
    private PassengerRideAdapter adapter;

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

        // Load rides (initially all rides, no status filter)
        viewModel.loadRides();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new PassengerRideAdapter();
        binding.recyclerRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRides.setAdapter(adapter);
    }

    private void setupFilterButtons() {
        binding.btnFilterAll.setOnClickListener(v -> {
            updateFilterUI(binding.btnFilterAll);
            viewModel.loadRides(null, null, null); // Load all rides
        });

        binding.btnFilterPending.setOnClickListener(v -> {
            updateFilterUI(binding.btnFilterPending);
            viewModel.loadRides("PENDING", null, null);
        });

        binding.btnFilterFinished.setOnClickListener(v -> {
            updateFilterUI(binding.btnFilterFinished);
            viewModel.loadRides("FINISHED", null, null);
        });

        binding.btnFilterRejected.setOnClickListener(v -> {
            updateFilterUI(binding.btnFilterRejected);
            viewModel.loadRides("REJECTED", null, null);
        });

        binding.btnFilterCanceled.setOnClickListener(v -> {
            updateFilterUI(binding.btnFilterCanceled);
            viewModel.loadRides("CANCELED", null, null);
        });
    }

    private void updateFilterUI(MaterialButton selectedButton) {
        // Reset all buttons to inactive state
        resetFilterButton(binding.btnFilterAll);
        resetFilterButton(binding.btnFilterPending);
        resetFilterButton(binding.btnFilterFinished);
        resetFilterButton(binding.btnFilterRejected);
        resetFilterButton(binding.btnFilterCanceled);

        // Set selected button to active state (Yellow background, Black text)
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow_500));
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_900));
    }

    private void resetFilterButton(MaterialButton button) {
        // Inactive state (Light gray background, Gray text)
        button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray_200));
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_400));
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
