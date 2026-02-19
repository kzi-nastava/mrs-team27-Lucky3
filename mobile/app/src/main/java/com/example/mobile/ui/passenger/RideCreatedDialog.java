package com.example.mobile.ui.passenger;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.mobile.R;

import java.util.ArrayList;

public class RideCreatedDialog extends DialogFragment {

    private static final String ARG_PRICE = "arg_price";
    private static final String ARG_EMAILS = "arg_emails";

    public static RideCreatedDialog newInstance(double price, ArrayList<String> emails) {
        RideCreatedDialog dialog = new RideCreatedDialog();
        Bundle args = new Bundle();
        args.putDouble(ARG_PRICE, price);
        args.putStringArrayList(ARG_EMAILS, emails);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_ride_created, null);

        Button btnOk = root.findViewById(R.id.btnOk);
        TextView tvMessage = root.findViewById(R.id.tvMessage);
        TextView tvPrice = root.findViewById(R.id.tvPrice);
        TextView tvEmails = root.findViewById(R.id.tvEmails);

        double price = getArguments() != null ? getArguments().getDouble(ARG_PRICE) : 0.0;
        ArrayList<String> emails = getArguments() != null
                ? getArguments().getStringArrayList(ARG_EMAILS)
                : new ArrayList<>();

        tvMessage.setText("Your ride was successfully created!");
        tvPrice.setText("Price is " + Math.round(price) + " RSD.");

        if (emails != null && !emails.isEmpty()) {
            StringBuilder sb = new StringBuilder("Linked passengers:\n");
            for (String email : emails) {
                sb.append("• ").append(email).append("\n");
            }
            tvEmails.setText(sb.toString().trim());
        } else {
            tvEmails.setText("No passengers linked.");
        }

        btnOk.setOnClickListener(v -> dismiss());

        // IMPORTANT: attach your custom view to the dialog
        builder.setView(root);

        return builder.create();
    }

}
