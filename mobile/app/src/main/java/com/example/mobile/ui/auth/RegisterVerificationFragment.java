package com.example.mobile.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentRegisterVerificationBinding;

public class RegisterVerificationFragment extends Fragment {

    private FragmentRegisterVerificationBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterVerificationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_nav_register_verification_to_nav_login);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}