package com.example.mobile.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile.databinding.DialogChangePersonalInfoBinding;
import com.example.mobile.models.ProfileUserResponse;
import com.example.mobile.utils.ClientUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePersonalInfoDialog extends BottomSheetDialogFragment {

    private DialogChangePersonalInfoBinding binding;
    private Long userId;
    private String token;
    private Uri selectedPhotoUri = null;

    // Activity result launcher for photo picker
    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    if (selectedPhotoUri != null) {
                        binding.photoPreview.setImageURI(selectedPhotoUri);
                        binding.photoPreview.setVisibility(View.VISIBLE);
                        binding.photoPlaceholder.setVisibility(View.GONE);
                    }
                }
            }
    );

    // Factory method to create instance with userId AND token
    public static ChangePersonalInfoDialog newInstance(Long userId, String token) {
        ChangePersonalInfoDialog dialog = new ChangePersonalInfoDialog();
        Bundle args = new Bundle();
        args.putLong("userId", userId);
        args.putString("token", token);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get userId and token from arguments
        if (getArguments() != null) {
            this.userId = getArguments().getLong("userId");
            this.token = getArguments().getString("token");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogChangePersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.photoPickerContainer.setOnClickListener(v -> openPhotoPicker());
        binding.btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        photoPickerLauncher.launch(intent);
    }

    private void handleSubmit() {
        String name = text(binding.inputName);
        String surname = text(binding.inputSurname);
        String email = text(binding.inputEmail);
        String phone = text(binding.inputPhone);
        String address = text(binding.inputAddress);

        boolean hasEmpty =
                TextUtils.isEmpty(name) ||
                        TextUtils.isEmpty(surname) ||
                        TextUtils.isEmpty(email) ||
                        TextUtils.isEmpty(phone) ||
                        TextUtils.isEmpty(address);

        if (hasEmpty) {
            showPopup(
                    "Error",
                    "Please fill in all fields before submitting."
            );
        } else {
            // Disable button and show loading
            binding.btnSubmit.setEnabled(false);
            // Call API to update personal info
            updatePersonalInfo(name, surname, email, phone, address);
        }
    }

    private void updatePersonalInfo(String name, String surname, String email, String phone, String address) {
        // Create the request object
        ProfileUserResponse request = new ProfileUserResponse(name, surname, email, phone, address, "");

        // Convert to JSON string using Gson
        Gson gson = new Gson();
        String jsonString = gson.toJson(request);

        // Create RequestBody for the JSON part
        RequestBody userData = RequestBody.create(
                MediaType.parse("application/json"),
                jsonString
        );

        // Create image part if a photo was selected
        MultipartBody.Part imagePart = null;
        if (selectedPhotoUri != null) {
            imagePart = buildImagePart(selectedPhotoUri);
        }

        String authToken = "Bearer " + token;

        Call<ProfileUserResponse> call = ClientUtils.userService.updatePersonalInfo(
                userId,
                userData,
                imagePart,
                authToken
        );

        call.enqueue(new Callback<ProfileUserResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileUserResponse> call,
                                   @NonNull Response<ProfileUserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bundle result = new Bundle();
                    result.putBoolean("success", true);
                    getParentFragmentManager().setFragmentResult("updatePersonalInfo", result);

                    showPopup("Success", "Your personal information was updated successfully.");
                } else {
                    binding.btnSubmit.setEnabled(true);
                    String errorMsg = "Failed to update. ";

                    if (response.code() == 401) {
                        errorMsg += "Please login again.";
                    } else if (response.code() == 400) {
                        errorMsg += "Invalid data provided.";
                    } else if (response.code() == 500) {
                        errorMsg += "Server error. Please try again later.";
                    } else {
                        errorMsg += "Error code: " + response.code();
                    }

                    showPopup("Error", errorMsg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileUserResponse> call, @NonNull Throwable t) {
                binding.btnSubmit.setEnabled(true);
                showPopup("Error", "Network error: " + t.getMessage());
            }
        });
    }

    private MultipartBody.Part buildImagePart(Uri uri) {
        try {
            android.content.ContentResolver resolver = requireContext().getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            // Read bytes from content URI
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            inputStream.close();
            byte[] imageBytes = buffer.toByteArray();

            // Determine file name from URI
            String fileName = "profile_image.jpg";
            try (android.database.Cursor cursor = resolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }

            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), imageBytes);
            return MultipartBody.Part.createFormData("profileImage", fileName, requestBody);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPopup(String title, String message) {
        // prevent multiple dialogs by double click
        binding.btnSubmit.setEnabled(false);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    clearInputs();
                    dismiss(); // close bottom sheet
                })
                .show();
    }

    private void clearInputs() {
        binding.inputName.setText("");
        binding.inputSurname.setText("");
        binding.inputEmail.setText("");
        binding.inputPhone.setText("");
        binding.inputAddress.setText("");
    }

    private String text(@NonNull android.widget.EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}