package com.example.mobile.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentAdminRequestsBinding;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.viewmodels.AdminRequestsViewModel;

public class AdminRequestsFragment extends Fragment {

    private FragmentAdminRequestsBinding binding;
    private AdminRequestsViewModel viewModel;
    private ChangeRequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminRequestsBinding.inflate(inflater, container, false);
        // Navbar setup
        View navbar = binding.getRoot().findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Requests");
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewModel();
        setupListView();
        setupListeners();
        observeData();

        viewModel.loadRequests();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(AdminRequestsViewModel.class);
    }

    private void setupListView() {
        adapter = new ChangeRequestAdapter(requireContext(), new ChangeRequestAdapter.OnActionListener() {
            @Override
            public void onApprove(Long requestId) {
                viewModel.approveRequest(requestId);
            }

            @Override
            public void onReject(Long requestId) {
                viewModel.rejectRequest(requestId);
            }
        });

        binding.recyclerRequests.setDivider(null);
        binding.recyclerRequests.setDividerHeight(0);
        binding.recyclerRequests.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnRefresh.setOnClickListener(v -> viewModel.loadRequests());
    }

    private void observeData() {
        viewModel.getRequests().observe(getViewLifecycleOwner(), requests -> {
            adapter.setRequests(requests);
            ListViewHelper.setListViewHeightBasedOnChildren(binding.recyclerRequests);
            binding.tvPendingCount.setText(String.valueOf(requests.size()));

            if (requests.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerRequests.setVisibility(View.GONE);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
                binding.recyclerRequests.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.errorCard.setVisibility(View.VISIBLE);
                binding.tvError.setText(error);
            } else {
                binding.errorCard.setVisibility(View.GONE);
            }
        });

        viewModel.getBusyIds().observe(getViewLifecycleOwner(), busyIds -> {
            adapter.setBusyIds(busyIds);
            ListViewHelper.setListViewHeightBasedOnChildren(binding.recyclerRequests);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

