// LinkPassengersDialogFragment.java
package com.example.mobile.ui.passenger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mobile.R;

import java.util.ArrayList;

public class LinkPassengersDialog extends DialogFragment {

    public static final String REQUEST_KEY = "link_passengers_result";
    public static final String KEY_EMAILS = "emails";

    private final ArrayList<String> emails = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText etEmail;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_link_passengers, null);

        etEmail = root.findViewById(R.id.etEmail);
        Button btnAdd = root.findViewById(R.id.btnAddEmail);
        Button btnCancel = root.findViewById(R.id.btnCancel);
        Button btnFinish = root.findViewById(R.id.btnFinish);
        ListView lvEmails = root.findViewById(R.id.lvEmails);

        adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                emails
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(getResources().getColor(R.color.white));
                return view;
            }
        };
        lvEmails.setAdapter(adapter);

        // Add email
        btnAdd.setOnClickListener(v -> addPassengerByEmail());

        // Remove email on click
        lvEmails.setOnItemClickListener((parent, view, position, id) -> {
            String removed = emails.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "Removed: " + removed, Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnFinish.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putStringArrayList(KEY_EMAILS, emails);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        });

        builder.setView(root);
        return builder.create();
    }

    private void addPassengerByEmail(){
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Invalid email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (emails.contains(email)) {
            Toast.makeText(requireContext(), "Email already added", Toast.LENGTH_SHORT).show();
            return;
        }

        emails.add(email);
        adapter.notifyDataSetChanged();
        etEmail.setText("");
    }

    public static LinkPassengersDialog newInstance() {
        return new LinkPassengersDialog();
    }
}
