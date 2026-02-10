package com.example.mobile.ui.passenger;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Modern dialog for linking passengers matching the web version design.
 * Allows users to add multiple passenger emails to share a ride.
 */
public class LinkPassengersNewDialog extends DialogFragment {

    public static final String REQUEST_KEY = "link_passengers_result";
    public static final String KEY_EMAILS = "emails";

    private LinearLayout emailsContainer;
    private LinearLayout btnAddEmail;
    private MaterialButton btnCancel;
    private MaterialButton btnLink;
    private ImageButton btnClose;

    private final ArrayList<View> emailViews = new ArrayList<>();

    public static LinkPassengersNewDialog newInstance() {
        return new LinkPassengersNewDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_Mobile_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_link_passengers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        emailsContainer = view.findViewById(R.id.emails_container);
        btnAddEmail = view.findViewById(R.id.btn_add_email);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnLink = view.findViewById(R.id.btn_link);
        btnClose = view.findViewById(R.id.btn_close);

        // Add initial email field
        addEmailField();

        // Setup listeners
        setupListeners();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupListeners() {
        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Cancel button
        btnCancel.setOnClickListener(v -> dismiss());

        // Add email button
        btnAddEmail.setOnClickListener(v -> addEmailField());

        // Link button
        btnLink.setOnClickListener(v -> submitEmails());
    }

    private void addEmailField() {
        View emailView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_email_input, emailsContainer, false);

        TextInputEditText etEmail = emailView.findViewById(R.id.et_email);
        ImageButton btnRemove = emailView.findViewById(R.id.btn_remove);

        // Disable remove button if this is the only email field
        updateRemoveButtonsState();

        btnRemove.setOnClickListener(v -> {
            if (emailViews.size() > 1) {
                emailsContainer.removeView(emailView);
                emailViews.remove(emailView);
                updateRemoveButtonsState();
            }
        });

        emailViews.add(emailView);
        emailsContainer.addView(emailView);
        updateRemoveButtonsState();
    }

    private void updateRemoveButtonsState() {
        boolean canRemove = emailViews.size() > 1;
        for (View emailView : emailViews) {
            ImageButton btnRemove = emailView.findViewById(R.id.btn_remove);
            btnRemove.setEnabled(canRemove);
            btnRemove.setAlpha(canRemove ? 1.0f : 0.3f);
        }
    }

    private void submitEmails() {
        ArrayList<String> validEmails = new ArrayList<>();
        boolean hasError = false;

        for (View emailView : emailViews) {
            TextInputEditText etEmail = emailView.findViewById(R.id.et_email);
            TextInputLayout inputLayout = (TextInputLayout) etEmail.getParent().getParent();
            
            String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
            
            if (!email.isEmpty()) {
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (!validEmails.contains(email)) {
                        validEmails.add(email);
                        inputLayout.setError(null);
                    } else {
                        inputLayout.setError("Duplicate email");
                        hasError = true;
                    }
                } else {
                    inputLayout.setError("Please enter a valid email address");
                    hasError = true;
                }
            }
        }

        if (hasError) {
            return;
        }

        // Build result bundle
        Bundle result = new Bundle();
        result.putStringArrayList(KEY_EMAILS, validEmails);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}
