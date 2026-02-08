package com.example.mobile.ui.login;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.MainActivity;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentLoginBinding;
import com.example.mobile.viewmodels.LoginViewModel;

/**
 * LoginFragment - Handles user authentication.
 * 
 * Implements MVVM pattern with:
 * - ViewBinding for XML interaction
 * - LoginViewModel for business logic and state management
 * - LiveData observation for UI updates
 * 
 * Features (Spec 2.2.1):
 * - Email and password input validation
 * - API call to UserService.login
 * - JWT token storage on success
 * - Role-based navigation (Driver/Passenger/Admin)
 * - Forgot password link navigation
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel using ViewModelProvider (manual injection as per course requirements)
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();
    }

    /**
     * Sets up click listeners and text watchers using ViewBinding.
     */
    private void setupListeners() {
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

        // Password text change listener
        binding.passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setPassword(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Login button click listener
        binding.loginButton.setOnClickListener(v -> {
            viewModel.login();
        });

        // Navigate to registration
        binding.registerText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_register);
        });

        // Navigate to forgot password
        binding.forgotPasswordText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_forgot_password);
        });

        binding.backToHomeText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_guest_home);
        });
    }

    /**
     * Observes LiveData from ViewModel and updates UI accordingly.
     */
    private void observeViewModel() {
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.loginButton.setEnabled(!isLoading);
            binding.loginButton.setText(isLoading ? "" : "Sign In");
            binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.emailEditText.setEnabled(!isLoading);
            binding.passwordEditText.setEnabled(!isLoading);
        });

        // Observe email validation error - set on TextInputLayout
        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            binding.emailInputLayout.setError(error);
        });

        // Observe password validation error - set on TextInputLayout
        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.passwordInputLayout.setError(error);
        });

        // Observe general error messages - show in error container
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorText.setText(error);
                viewModel.clearError();
            } else {
                binding.errorContainer.setVisibility(View.GONE);
            }
        });

        // Observe login success
        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                handleLoginSuccess();
                viewModel.resetLoginSuccess();
            }
        });
    }

    /**
     * Handles successful login by navigating to the appropriate home screen based on user role.
     */
    private void handleLoginSuccess() {
        String role = viewModel.getUserRole().getValue();
        
        if (role == null) {
            role = "PASSENGER"; // Default fallback
        }

        // Show success message
        Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();

        // Setup navigation based on role and navigate
        MainActivity activity = (MainActivity) requireActivity();
        
        switch (role.toUpperCase()) {
            case "ADMIN":
                activity.setupNavigationForRole("ADMIN");
                Navigation.findNavController(requireView()).navigate(R.id.nav_admin_dashboard);
                break;
            case "DRIVER":
                activity.setupNavigationForRole("DRIVER");
                Navigation.findNavController(requireView()).navigate(R.id.nav_driver_overview);
                break;
            case "PASSENGER":
            default:
                activity.setupNavigationForRole("PASSENGER");
                Navigation.findNavController(requireView()).navigate(R.id.nav_passenger_home);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}