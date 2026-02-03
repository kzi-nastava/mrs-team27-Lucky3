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
import com.example.mobile.databinding.FragmentResetPasswordBinding;
import com.example.mobile.viewmodels.ResetPasswordViewModel;

/**
 * ResetPasswordFragment - Handles setting a new password with a reset token.
 * 
 * Implements MVVM pattern with:
 * - ViewBinding for XML interaction
 * - ResetPasswordViewModel for business logic and state management
 * - LiveData observation for UI updates
 */
public class ResetPasswordFragment extends Fragment {

    private FragmentResetPasswordBinding binding;
    private ResetPasswordViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

        // Get token from arguments (from deep link or navigation)
        if (getArguments() != null) {
            String token = getArguments().getString("token", "");
            viewModel.setToken(token);
        }

        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();

        // Validate the token on fragment start
        viewModel.validateToken();
    }

    /**
     * Sets up click listeners and text watchers.
     */
    private void setupListeners() {
        // Back link
        binding.backToForgotPasswordText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_forgot_password);
        });

        // New password text change listener
        binding.newPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setNewPassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Confirm password text change listener
        binding.confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setConfirmPassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Reset button click listener
        binding.resetButton.setOnClickListener(v -> {
            viewModel.resetPassword();
        });
    }

    /**
     * Observes LiveData from ViewModel and updates UI accordingly.
     */
    private void observeViewModel() {
        // Observe token validation state
        viewModel.getIsValidatingToken().observe(getViewLifecycleOwner(), isValidating -> {
            binding.validatingContainer.setVisibility(isValidating ? View.VISIBLE : View.GONE);
            binding.formContainer.setVisibility(isValidating ? View.GONE : View.VISIBLE);
        });

        // Observe token validation errors (redirect to forgot password)
        viewModel.getTokenValidationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                // Navigate to forgot password with error reason
                Bundle args = new Bundle();
                args.putString("reason", error);
                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_forgot_password, args);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.resetButton.setEnabled(!isLoading);
            binding.resetButton.setText(isLoading ? "" : "Reset Password");
            binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.newPasswordEditText.setEnabled(!isLoading);
            binding.confirmPasswordEditText.setEnabled(!isLoading);
        });

        // Observe password validation errors
        viewModel.getNewPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.newPasswordInputLayout.setError(error);
        });

        viewModel.getConfirmPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.confirmPasswordInputLayout.setError(error);
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorText.setText(error);
                viewModel.clearError();
            } else {
                binding.errorContainer.setVisibility(View.GONE);
            }
        });

        // Observe password reset success
        viewModel.getPasswordResetSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                // Navigate to success screen
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_nav_reset_password_to_nav_reset_password_success);
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