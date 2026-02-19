package com.example.mobile.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentUserProfileBinding;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.models.User;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;
import com.example.mobile.viewmodels.UserProfileViewModel;

import com.example.mobile.utils.NavbarHelper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private UserProfileViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);

        // set up navbar
        NavbarHelper.setup(this, binding.getRoot(), "Profile");

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
        binding.btnEditPersonal.setOnClickListener(v -> {
            // Get token from your TokenManager or wherever you store it
            SharedPreferencesManager prefsManager = viewModel.getPrefsManager();
            String token = prefsManager.getToken();
            Long currentUserId = prefsManager.getUserId();

            ChangePersonalInfoDialog dialog = ChangePersonalInfoDialog.newInstance(currentUserId, token);
            dialog.show(getParentFragmentManager(), "ChangePersonalInfoDialog");
        });

        // Set up listener for dialog results
        getParentFragmentManager().setFragmentResultListener("updatePersonalInfo", this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        boolean success = result.getBoolean("success");
                        if (success) {
                            observeViewModel();
                        }
                    }
                }
        );
    }

    private void observeViewModel(){
        // Observe profile data
        viewModel.getUserProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                displayUserData(profile);
                updateUIBasedOnRole();  //to make slight changes based on role (passenger/admin)
            }
        });

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

        // Load profile image
        loadProfileImage(profile.getImageUrl(), binding.ivHeaderAvatar);
    }

    /**
     * Loads profile image from a relative URL (e.g. /api/users/5/profile-image)
     * into the given ImageView on a background thread.
     */
    private void loadProfileImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        // Build full URL from relative path
        String fullUrl = ClientUtils.SERVICE_API_PATH + imageUrl.replaceFirst("^/", "");

        new Thread(() -> {
            try {
                URL url = new URL(fullUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                if (bitmap != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e("UserProfileFragment", "Failed to load profile image", e);
            }
        }).start();
    }

    private void updateUIBasedOnRole() {
        SharedPreferencesManager prefsManager = viewModel.getPrefsManager();
        String userRole = prefsManager.getUserRole();

        // Check if user is administrator
        boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

        if (isAdmin) {
            // Show administrator badge
            binding.tvHeaderRating.setText("Administrator");
            binding.tvHeaderRating.setTextColor(getResources().getColor(R.color.yellow_400, null));
            binding.tvHeaderRating.setVisibility(View.VISIBLE);

            // Change page title
            View navbar = binding.getRoot().findViewById(R.id.navbar);
            if (navbar != null) {
                TextView toolbarTitle = navbar.findViewById(R.id.toolbar_title);
                toolbarTitle.setText("Admin Profile");
            }
        } else {
            // Hide administrator badge for regular users
            binding.tvHeaderRating.setVisibility(View.GONE);
        }
    }
}






