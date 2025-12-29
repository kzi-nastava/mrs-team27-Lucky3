package com.example.mobile.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentResetPasswordBinding;

public class ResetPasswordFragment extends Fragment {

    private FragmentResetPasswordBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnReset.setOnClickListener(v -> {
            if (validate()) {
                // Simulate API call
                binding.btnReset.setText("Resetting...");
                binding.btnReset.setEnabled(false);

                v.postDelayed(() -> {
                    Navigation.findNavController(v).navigate(R.id.action_nav_reset_password_to_nav_reset_password_success);
                }, 1000);
            }
        });

        return root;
    }

    private boolean validate() {
        boolean isValid = true;
        String password = binding.newPasswordInput.getText().toString();
        String confirmPassword = binding.confirmPasswordInput.getText().toString();

        // Reset errors
        binding.newPasswordError.setVisibility(View.GONE);
        binding.confirmPasswordError.setVisibility(View.GONE);
        binding.newPasswordInput.setBackgroundResource(R.drawable.bg_auth_input);
        binding.confirmPasswordInput.setBackgroundResource(R.drawable.bg_auth_input);

        if (password.isEmpty()) {
            binding.newPasswordError.setText("Password is required");
            binding.newPasswordError.setVisibility(View.VISIBLE);
            binding.newPasswordInput.setBackgroundResource(R.drawable.bg_auth_input_error);
            isValid = false;
        } else if (password.length() < 8) {
            binding.newPasswordError.setText("At least 8 characters");
            binding.newPasswordError.setVisibility(View.VISIBLE);
            binding.newPasswordInput.setBackgroundResource(R.drawable.bg_auth_input_error);
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordError.setText("Confirmation is required");
            binding.confirmPasswordError.setVisibility(View.VISIBLE);
            binding.confirmPasswordInput.setBackgroundResource(R.drawable.bg_auth_input_error);
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordError.setText("Passwords do not match");
            binding.confirmPasswordError.setVisibility(View.VISIBLE);
            binding.confirmPasswordInput.setBackgroundResource(R.drawable.bg_auth_input_error);
            isValid = false;
        }

        return isValid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}