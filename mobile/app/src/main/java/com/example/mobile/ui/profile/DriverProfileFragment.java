package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.mobile.databinding.FragmentDriverProfileBinding;

public class DriverProfileFragment extends Fragment {

    private FragmentDriverProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.btnEditPersonal.setOnClickListener(v -> {
            new ChangePersonalInfoDialog().show(getParentFragmentManager(), "ChangePersonalInfoDialog");
        });

        binding.btnEditVehicle.setOnClickListener(v -> {
            new ChangeVehicleInfoDialog().show(getParentFragmentManager(), "ChangeVehicleInfoDialog");
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}