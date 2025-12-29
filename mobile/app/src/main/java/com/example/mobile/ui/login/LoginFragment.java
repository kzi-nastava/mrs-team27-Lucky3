package com.example.mobile.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.registerText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_register);
        });

        binding.forgotPasswordText.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_login_to_nav_forgot_password);
        });

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString();
            String password = binding.passwordEditText.getText().toString();

            if (email.equals("admin@mail.com") && password.equals("admin123")) {
                ((com.example.mobile.MainActivity) requireActivity()).setupNavigationForRole("ADMIN");
                Navigation.findNavController(v).navigate(R.id.nav_admin_dashboard);
            } else if (email.equals("mile@mail.com") && password.equals("sifra123")) {
                ((com.example.mobile.MainActivity) requireActivity()).setupNavigationForRole("DRIVER");
                Navigation.findNavController(v).navigate(R.id.nav_driver_dashboard);
            } else if (email.equals("boban@mail.com") && password.equals("sifra123")) {
                ((com.example.mobile.MainActivity) requireActivity()).setupNavigationForRole("PASSENGER");
                Navigation.findNavController(v).navigate(R.id.nav_passenger_home);
            } else {
                android.widget.Toast.makeText(getContext(), "Invalid credentials", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}