package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
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


        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Profile");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}