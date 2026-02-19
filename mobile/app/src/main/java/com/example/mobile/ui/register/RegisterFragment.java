package com.example.mobile.ui.register;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentRegisterBinding;
import com.example.mobile.viewmodels.RegisterViewModel;

/**
 * RegisterFragment - Handles new user registration.
 * 
 * Implements MVVM pattern with:
 * - ViewBinding for XML interaction
 * - RegisterViewModel for business logic and state management
 * - LiveData observation for UI updates
 * 
 * Features (Spec 2.2.2):
 * - Fields: Name, Surname, Email, Password, Confirm Password, Address, Phone Number
 * - Password validation (must match)
 * - API call to UserService.register
 * - Success dialog for activation email notification
 * - Optional profile picture picker
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;
    private Uri selectedPhotoUri = null;

    // Activity result launcher for photo picker
    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    if (selectedPhotoUri != null) {
                        binding.photoPreview.setImageURI(selectedPhotoUri);
                        binding.photoPreview.setVisibility(View.VISIBLE);
                        binding.photoPlaceholder.setVisibility(View.GONE);
                        viewModel.setProfilePhotoUri(selectedPhotoUri.toString());
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel using ViewModelProvider (manual injection as per course requirements)
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();
    }

    /**
     * Sets up click listeners and text watchers using ViewBinding.
     */
    private void setupListeners() {
        binding.backToLoginText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_register_to_nav_login);
        });

        binding.loginText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_register_to_nav_login);
        });

        // First name text change listener
        binding.firstNameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setFirstName(s.toString());
            }
        });

        // Last name text change listener
        binding.lastNameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setLastName(s.toString());
            }
        });

        // Email text change listener
        binding.emailEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setEmail(s.toString());
            }
        });

        // Phone number text change listener
        binding.phoneEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setPhoneNumber(s.toString());
            }
        });

        // Address text change listener
        binding.addressEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setAddress(s.toString());
            }
        });

        // Password text change listener
        binding.passwordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setPassword(s.toString());
            }
        });

        // Confirm password text change listener
        binding.confirmPasswordEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setConfirmPassword(s.toString());
            }
        });

        // Photo picker click listener
        binding.photoPickerContainer.setOnClickListener(v -> {
            openPhotoPicker();
        });

        // Register button click listener
        binding.registerButton.setOnClickListener(v -> {
            viewModel.register();
        });
    }

    /**
     * Opens photo picker to select profile photo.
     */
    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        photoPickerLauncher.launch(intent);
    }

    /**
     * Observes LiveData from ViewModel and updates UI accordingly.
     */
    private void observeViewModel() {
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.registerButton.setEnabled(!isLoading);
            binding.registerButton.setText(isLoading ? "" : "Create Account");
            binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setFormEnabled(!isLoading);
        });

        // Observe validation errors - use new TextInputLayout IDs
        viewModel.getFirstNameError().observe(getViewLifecycleOwner(), error -> {
            binding.firstNameInputLayout.setError(error);
        });

        viewModel.getLastNameError().observe(getViewLifecycleOwner(), error -> {
            binding.lastNameInputLayout.setError(error);
        });

        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            binding.emailInputLayout.setError(error);
        });

        viewModel.getPhoneError().observe(getViewLifecycleOwner(), error -> {
            binding.phoneInputLayout.setError(error);
        });

        viewModel.getAddressError().observe(getViewLifecycleOwner(), error -> {
            binding.addressInputLayout.setError(error);
        });

        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.passwordInputLayout.setError(error);
        });

        viewModel.getConfirmPasswordError().observe(getViewLifecycleOwner(), error -> {
            binding.confirmPasswordInputLayout.setError(error);
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

        // Observe registration success
        viewModel.getRegistrationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                showSuccessDialog();
                viewModel.resetRegistrationSuccess();
            }
        });
    }

    /**
     * Enables or disables all form inputs.
     */
    private void setFormEnabled(boolean enabled) {
        binding.firstNameEditText.setEnabled(enabled);
        binding.lastNameEditText.setEnabled(enabled);
        binding.emailEditText.setEnabled(enabled);
        binding.phoneEditText.setEnabled(enabled);
        binding.addressEditText.setEnabled(enabled);
        binding.passwordEditText.setEnabled(enabled);
        binding.confirmPasswordEditText.setEnabled(enabled);
        binding.photoPickerContainer.setEnabled(enabled);
    }

    /**
     * Shows success dialog informing user about activation email.
     * As per spec: activation is via email link, do not auto-login.
     */
    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Registration Successful!")
                .setMessage("An activation email has been sent to your email address. " +
                        "Please check your inbox and click the activation link to complete your registration.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Navigate to verification screen
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_nav_register_to_nav_register_verification);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Simple TextWatcher implementation to reduce boilerplate code.
     */
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}