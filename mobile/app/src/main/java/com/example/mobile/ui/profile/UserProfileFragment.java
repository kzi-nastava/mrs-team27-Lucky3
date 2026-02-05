package com.example.mobile.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentUserProfileBinding;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.models.User;
import com.example.mobile.viewmodels.UserProfileViewModel;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private UserProfileViewModel viewModel;
    private TextView nameTextView, surnameTextView, emailTextView, phoneTextView, addressTextView;
    private Button editButton;
    private ProfileUserResponse currentProfile;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);

        // set up navbar
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

        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        // Setup UI listeners
        setupListeners();

        // Observe ViewModel state
        observeViewModel();
    }

    private void setupListeners(){
        // Setup listeners if needed
        binding.btnEditPersonal.setOnClickListener(v -> {
            new ChangePersonalInfoDialog().show(getParentFragmentManager(), "ChangePersonalInfoDialog");
        });

        /*// Edit button click - open dialog
        editButton.setOnClickListener(v -> {
            if (currentProfile != null) {
                //EditProfileDialog dialog = EditProfileDialog.newInstance(currentProfile);
                //dialog.show(getChildFragmentManager(), "EditProfileDialog");
            }
        });*/
    }

    private void observeViewModel(){
        // Observe profile data
        viewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentProfile = profile;
                displayUserData(profile);
            }
        });

        /*
        // Observe loading state
        viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            editButton.setEnabled(!isLoading);
        });*/

        // Observe errors
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Load user profile
        viewModel.loadUserProfile();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void displayUserData(ProfileUserResponse profile) {
        binding.textName.setText(profile.getName() + " " + profile.getSurname());
        binding.textEmail.setText(profile.getEmail());
        binding.textPhone.setText(profile.getPhoneNumber());
        binding.textAddress.setText(profile.getAddress());

        // Update header section too
        binding.tvHeaderFullName.setText(profile.getName() + " " + profile.getSurname());
        binding.tvHeaderEmail.setText(profile.getEmail());
    }
}






