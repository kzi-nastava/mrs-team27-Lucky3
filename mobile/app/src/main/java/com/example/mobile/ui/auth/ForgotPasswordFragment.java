package com.example.mobile.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentForgotPasswordBinding;
import com.example.mobile.viewmodels.ForgotPasswordViewModel;

/**
 * ForgotPasswordFragment - Handles password reset request.
 * 
 * Implements MVVM pattern with:
 * - ViewBinding for XML interaction
 * - ForgotPasswordViewModel for business logic and state management
 * - LiveData observation for UI updates
 */
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private ForgotPasswordViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        // Check for notice reason from arguments (e.g., expired-token, invalid-token)
        if (getArguments() != null) {
            String reason = getArguments().getString("reason", "");
            handleNoticeReason(reason);
        }

        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();
    }

    /**
     * Handles notice reason from navigation arguments.
     */
    private void handleNoticeReason(String reason) {
        String notice = "";
        switch (reason) {
            case "missing-token":
                notice = "Reset link is missing a token. Please request a new link.";
                break;
            case "invalid-token":
                notice = "This reset link is invalid. Please request a new link.";
                break;
            case "expired-token":
                notice = "This reset link has expired. Please request a new link.";
                break;
        }
        viewModel.setNotice(notice);
    }

    /**
     * Sets up click listeners and text watchers.
     */
    private void setupListeners() {
        // Back to login
        binding.backToLoginText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Email text change listener
        binding.emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setEmail(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send reset link button
        binding.sendButton.setOnClickListener(v -> {
            viewModel.requestPasswordReset();
        });
    }

    /**
     * Observes LiveData from ViewModel and updates UI accordingly.
     */
    private void observeViewModel() {
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.sendButton.setEnabled(!isLoading);
            binding.sendButton.setText(isLoading ? "" : "Send Reset Link");
            binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.emailEditText.setEnabled(!isLoading);
        });

        // Observe email validation error
        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            binding.emailInputLayout.setError(error);
        });

        // Observe notice message
        viewModel.getNoticeMessage().observe(getViewLifecycleOwner(), notice -> {
            if (notice != null && !notice.isEmpty()) {
                binding.noticeContainer.setVisibility(View.VISIBLE);
                binding.noticeText.setText(notice);
            } else {
                binding.noticeContainer.setVisibility(View.GONE);
            }
        });

        // Observe error message
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorText.setText(error);
                viewModel.clearError();
            } else {
                binding.errorContainer.setVisibility(View.GONE);
            }
        });

        // Observe reset link sent success
        viewModel.getResetLinkSent().observe(getViewLifecycleOwner(), sent -> {
            if (sent) {
                // Navigate to email sent confirmation screen
                Bundle args = new Bundle();
                args.putString("email", viewModel.getEmailValue());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_nav_forgot_password_to_nav_forgot_password_sent, args);
                viewModel.resetState();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
