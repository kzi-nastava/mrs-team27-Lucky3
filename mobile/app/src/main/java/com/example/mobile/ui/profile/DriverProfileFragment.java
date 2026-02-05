package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverProfileBinding;
import com.example.mobile.models.DriverProfileResponse;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.viewmodels.DriverProfileViewModel;
import com.example.mobile.viewmodels.UserProfileViewModel;

public class DriverProfileFragment extends Fragment {

    private FragmentDriverProfileBinding binding;
    private DriverProfileViewModel viewModel;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverProfileBinding.inflate(inflater, container, false);

        // Navbar setup
        View navbar = binding.getRoot().findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Profile");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DriverProfileViewModel.class);
        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();
    }

    private void setupListeners(){
        binding.btnEditPersonal.setOnClickListener(v -> {
            new ChangePersonalInfoDialog().show(getParentFragmentManager(), "ChangePersonalInfoDialog");
        });
    }

    private void observeViewModel(){
        // Observe profile data
        viewModel.getDriverProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                displayDriverData(profile);
            }
        });

        // Observe errors
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load user profile
        viewModel.loadDriverProfile();
    }

    private void displayDriverData(DriverProfileResponse profile) {
        binding.textName.setText(profile.getName() + " " + profile.getSurname());
        binding.textEmail.setText(profile.getEmail());
        binding.textPhone.setText(profile.getPhoneNumber());
        binding.textAddress.setText(profile.getAddress());

        binding.textModel.setText(profile.getVehicle().getModel());
        binding.textPlate.setText(profile.getVehicle().getLicenseNumber());
        binding.textSeats.setText(String.valueOf(profile.getVehicle().getPassengerSeats()));

        // Set baby transport availability
        boolean babyAvailable = profile.getVehicle().getBabyTransport() != null
                && profile.getVehicle().getBabyTransport();
        updateTransportFeature(
                binding.iconBabyTransport,
                binding.textBabyTransport,
                babyAvailable,
                "Baby Transport"
        );

        // Set pet transport availability
        boolean petAvailable = profile.getVehicle().getPetTransport() != null
                && profile.getVehicle().getPetTransport();
        updateTransportFeature(
                binding.iconPetTransport,
                binding.textPetTransport,
                petAvailable,
                "Pet Transport"
        );

        // Update header section
        binding.tvHeaderFullName.setText(profile.getName() + " " + profile.getSurname());
        binding.tvHeaderEmail.setText(profile.getEmail());
    }

    /**
     * Updates transport feature icon and text based on availability
     */
    private void updateTransportFeature(ImageView icon, TextView text, boolean isAvailable, String featureName) {
        if (isAvailable) {
            // Green checkmark for available (no background)
            icon.setImageResource(R.drawable.ic_check);
            icon.setColorFilter(getResources().getColor(R.color.green_500, null));
            text.setText(featureName + " Available");
            text.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            // Red X for unavailable
            icon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            icon.setColorFilter(getResources().getColor(R.color.red_500, null));
            text.setText(featureName + " Unavailable");
            text.setTextColor(getResources().getColor(R.color.white, null));
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}