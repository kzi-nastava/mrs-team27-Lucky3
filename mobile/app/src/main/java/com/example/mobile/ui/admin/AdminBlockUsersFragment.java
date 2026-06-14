package com.example.mobile.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentAdminBlockUsersBinding;
import com.example.mobile.models.UserProfile;
import com.example.mobile.utils.NavbarHelper;
import com.example.mobile.viewmodels.AdminBlockUsersViewModel;

public class AdminBlockUsersFragment extends Fragment implements AdminBlockUserAdapter.OnUserActionListener {

    private FragmentAdminBlockUsersBinding binding;
    private AdminBlockUsersViewModel viewModel;
    private AdminBlockUserAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBlockUsersBinding.inflate(inflater, container, false);
        
        // Setup navbar
        NavbarHelper.setup(this, binding.getRoot(), "Block Users");
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(AdminBlockUsersViewModel.class);
        
        setupRecyclerView();
        setupSearch();
        setupFilters();
        observeViewModel();
        
        viewModel.loadData();
    }

    private void setupRecyclerView() {
        adapter = new AdminBlockUserAdapter(this);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUsers.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearchUser.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                viewModel.search(v.getText().toString());
                return true;
            }
            return false;
        });

        binding.etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    viewModel.search("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        binding.tvFilterAll.setOnClickListener(v -> {
            viewModel.setFilter("All");
            updateFilterStyles("All");
        });
        
        binding.tvFilterBlocked.setOnClickListener(v -> {
            viewModel.setFilter("Blocked");
            updateFilterStyles("Blocked");
        });
        
        binding.tvFilterUnblocked.setOnClickListener(v -> {
            viewModel.setFilter("Unblocked");
            updateFilterStyles("Unblocked");
        });
    }

    private void updateFilterStyles(String activeFilter) {
        // Reset all
        resetFilterStyle(binding.tvFilterAll);
        resetFilterStyle(binding.tvFilterBlocked);
        resetFilterStyle(binding.tvFilterUnblocked);
        
        // Highlight active
        switch (activeFilter) {
            case "All":
                setActiveFilterStyle(binding.tvFilterAll);
                break;
            case "Blocked":
                setActiveFilterStyle(binding.tvFilterBlocked);
                break;
            case "Unblocked":
                setActiveFilterStyle(binding.tvFilterUnblocked);
                break;
        }
    }

    private void resetFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_card);
        tv.setTextColor(getResources().getColor(R.color.gray_400));
    }

    private void setActiveFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_rounded_yellow);
        tv.setTextColor(getResources().getColor(R.color.black));
    }

    private void observeViewModel() {
        viewModel.getDisplayedUsers().observe(getViewLifecycleOwner(), users -> {
            adapter.setUsers(users);
        });
        
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Show/hide progress bar if you have one
        });
        
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBlock(UserProfile user) {
        showBlockDialog(user);
    }

    @Override
    public void onUnblock(UserProfile user) {
        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Unblock User")
                .setMessage("Are you sure you want to unblock " + user.getEmail() + "?")
                .setPositiveButton("Unblock", (dialog, which) -> viewModel.unblockUser(user.getEmail()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBlockDialog(UserProfile user) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_block_user, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        EditText etReason = dialogView.findViewById(R.id.et_block_reason);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel_block);
        View btnConfirm = dialogView.findViewById(R.id.btn_confirm_block);
        View errorContainer = dialogView.findViewById(R.id.error_container);
        TextView tvError = dialogView.findViewById(R.id.tv_error);

        tvTitle.setText("Block " + user.getName());
        tvSubtitle.setText("Provide a reason for blocking " + user.getEmail());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                errorContainer.setVisibility(View.VISIBLE);
                tvError.setText("Block reason is required");
            } else {
                viewModel.blockUser(user.getEmail(), reason);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
