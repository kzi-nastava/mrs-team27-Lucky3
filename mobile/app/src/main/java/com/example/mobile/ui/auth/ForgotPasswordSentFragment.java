package com.example.mobile.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentForgotPasswordSentBinding;
import com.example.mobile.models.EmailRequest;
import com.example.mobile.utils.ClientUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ForgotPasswordSentFragment - Shows email sent confirmation.
 * Allows user to resend the reset email if needed.
 */
public class ForgotPasswordSentFragment extends Fragment {

    private static final String TAG = "ForgotPasswordSentFragment";
    private FragmentForgotPasswordSentBinding binding;
    private String email = "";
    private boolean isResending = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordSentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get email from arguments
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
        }

        // Display the email
        binding.emailText.setText(email);

        setupListeners();
    }

    private void setupListeners() {
        // Back to login
        binding.backToLoginText.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack(R.id.nav_login, false);
        });

        // Resend email
        binding.resendButton.setOnClickListener(v -> {
            if (!isResending && !email.isEmpty()) {
                resendResetEmail();
            }
        });
    }

    /**
     * Resends the password reset email.
     */
    private void resendResetEmail() {
        isResending = true;
        binding.resendButton.setEnabled(false);
        binding.resendLoading.setVisibility(View.VISIBLE);
        binding.messageText.setVisibility(View.GONE);

        EmailRequest request = new EmailRequest(email);

        ClientUtils.userService.forgotPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                isResending = false;
                
                if (getView() == null) return;

                binding.resendLoading.setVisibility(View.GONE);
                binding.resendButton.setEnabled(true);

                // Show success for 200, 404 (user not found - security), 409 (already sent)
                if (response.isSuccessful() || response.code() == 404 || response.code() == 409) {
                    String message = response.code() == 409 
                            ? "Email was already sent recently. Please check your inbox."
                            : "Email resent successfully!";
                    showMessage(message, true);
                } else {
                    showMessage("Failed to resend email. Please try again.", false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                isResending = false;
                
                if (getView() == null) return;

                binding.resendLoading.setVisibility(View.GONE);
                binding.resendButton.setEnabled(true);
                
                Log.e(TAG, "Resend failed: " + t.getMessage(), t);
                showMessage("Network error. Please check your connection.", false);
            }
        });
    }

    /**
     * Shows a success or error message.
     */
    private void showMessage(String message, boolean isSuccess) {
        binding.messageText.setText(message);
        binding.messageText.setTextColor(getResources().getColor(
                isSuccess ? R.color.green_500 : R.color.red_500, null));
        binding.messageText.setVisibility(View.VISIBLE);

        // Auto-hide after 5 seconds
        binding.messageText.postDelayed(() -> {
            if (binding != null) {
                binding.messageText.setVisibility(View.GONE);
            }
        }, 5000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}