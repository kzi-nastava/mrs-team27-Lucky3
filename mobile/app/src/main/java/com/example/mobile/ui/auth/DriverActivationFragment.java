package com.example.mobile.ui.auth;

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

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverActivationBinding;
import com.example.mobile.viewmodels.DriverActivationViewModel;

public class DriverActivationFragment extends Fragment {

    private FragmentDriverActivationBinding binding;
    private DriverActivationViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverActivationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverActivationViewModel.class);

        if (getArguments() != null) {
            String token = getArguments().getString("token", "");
            viewModel.setToken(token);
        }

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.setPassword(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.confirmPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.setConfirmPassword(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.activateButton.setOnClickListener(v -> viewModel.activateAccount());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.activateButton.setEnabled(!isLoading);
            binding.activateButton.setText(isLoading ? "" : "Activate Account");
            binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> binding.passwordInputLayout.setError(error));
        viewModel.getConfirmPasswordError().observe(getViewLifecycleOwner(), error -> binding.confirmPasswordInputLayout.setError(error));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorText.setText(error);
            } else {
                binding.errorContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getActivationSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(requireContext(), "Account activated! Please log in.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).navigate(R.id.nav_login);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
