package com.example.mobile.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentForgotPasswordSentBinding;

public class ForgotPasswordSentFragment extends Fragment {

    private FragmentForgotPasswordSentBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordSentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnLogin.setOnClickListener(v -> {
            // Navigate back to login (pop back stack to login)
            Navigation.findNavController(v).popBackStack(R.id.nav_login, false);
        });

        binding.btnSimulateLink.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_forgot_password_sent_to_nav_reset_password);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}