package com.example.mobile.ui.admin;

import com.example.mobile.R;
import com.example.mobile.model.DriverInfoCard;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.os.Bundle;

import java.util.ArrayList;

public class AdminDriversFragment extends Fragment {
    private ListView lvDrivers;
    private AdminDriverAdapter adapter;
    private TextView tvFilterAll, tvFilterActive, tvFilterInactive, tvFilterSuspended;
    private ArrayList<DriverInfoCard> allDrivers;
    private ArrayList<DriverInfoCard> displayedDrivers;
    private String currentFilter = "All";
    private String searchQuery = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin_drivers, container, false);

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Drivers");
        }


        lvDrivers = root.findViewById(R.id.listDrivers);
        EditText etSearch = root.findViewById(R.id.etSearch);
        ImageView ivSearch = root.findViewById(R.id.ivSearch);

        // 1) Prepare data (mock for now)
        allDrivers = createMockDrivers();
        displayedDrivers = new ArrayList<>(allDrivers);   // copy all initially

        // 2) Create adapter, pass context + list
        adapter = new AdminDriverAdapter(requireContext(), displayedDrivers);

        // 3) Attach adapter to ListView
        lvDrivers.setAdapter(adapter);


        // Find filter views
        tvFilterAll = root.findViewById(R.id.filter_all);
        tvFilterActive = root.findViewById(R.id.filter_active);
        tvFilterInactive = root.findViewById(R.id.filter_inactive);
        tvFilterSuspended = root.findViewById(R.id.filter_suspended);

        applyFilter();

        setupFilterClicks();


        // setup searching
        ivSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString();
            applySearch(query);
        });
        return root;
    }

    private void setupFilterClicks() {
        View.OnClickListener listener = v -> {
            if (v.getId() == R.id.filter_all) {
                currentFilter = "All";
            } else if (v.getId() == R.id.filter_active) {
                currentFilter = "Active";
            } else if (v.getId() == R.id.filter_inactive) {
                currentFilter = "Inactive";
            } else if (v.getId() == R.id.filter_suspended) {
                currentFilter = "Suspended";
            }

            updateFilterStyles();
            applyFilter();
        };

        tvFilterAll.setOnClickListener(listener);
        tvFilterActive.setOnClickListener(listener);
        tvFilterInactive.setOnClickListener(listener);
        tvFilterSuspended.setOnClickListener(listener);
    }

    private void updateFilterStyles() {
        // Reset all to gray
        resetFilterStyle(tvFilterAll);
        resetFilterStyle(tvFilterActive);
        resetFilterStyle(tvFilterInactive);
        resetFilterStyle(tvFilterSuspended);

        // Highlight selected
        switch (currentFilter) {
            case "All":
                setSelectedFilterStyle(tvFilterAll);
                break;
            case "Active":
                setSelectedFilterStyle(tvFilterActive);
                break;
            case "Inactive":
                setSelectedFilterStyle(tvFilterInactive);
                break;
            case "Suspended":
                setSelectedFilterStyle(tvFilterSuspended);
                break;
        }
    }

    private void resetFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_card);
        tv.setTextColor(getResources().getColor(R.color.gray_400));
    }

    private void setSelectedFilterStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_rounded_yellow);
        tv.setTextColor(getResources().getColor(R.color.black));
    }

    private void applyFilter() {
        displayedDrivers.clear();

        if ("All".equals(currentFilter)) {
            displayedDrivers.addAll(allDrivers);
        } else {
            for (DriverInfoCard d : allDrivers) {
                String status = d.getStatus();
                if (status != null && status.toLowerCase().contains(currentFilter.toLowerCase())) {
                    displayedDrivers.add(d);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void applySearch(String query) {
        searchQuery = query.toLowerCase().trim();
        displayedDrivers.clear();

        for (DriverInfoCard d : allDrivers) {
            // 1) status filter
            if (!"All".equals(currentFilter)) {
                String status = d.getStatus();
                if (status == null ||
                        !status.toLowerCase().contains(currentFilter.toLowerCase())) {
                    continue; // skip drivers not in current status filter
                }
            }

            // 2) search by name OR email
            if (!searchQuery.isEmpty()) {
                String fullName = (d.getName() + " " + d.getSurname()).toLowerCase();
                String email = d.getEmail() != null ? d.getEmail().toLowerCase() : "";
                if (!fullName.contains(searchQuery) && !email.contains(searchQuery)) {
                    continue;
                }
            }

            displayedDrivers.add(d);
        }

        adapter.notifyDataSetChanged(); // update ListView
    }
    private ArrayList<DriverInfoCard> createMockDrivers() {
        ArrayList<DriverInfoCard> list = new ArrayList<>();

        list.add(new DriverInfoCard(
                "Đura", "Ristić",
                "djura.ristic@kg-taxi.com",
                "",                        // imageUrl
                "KG-123-AB",               // Kragujevac
                "Toyota Prius",
                1.2f,
                "Active,Online",
                120,
                45120.0
        ));

        list.add(new DriverInfoCard(
                "Milica", "Jovanović",
                "milica.jovanovic@bg-taxi.com",
                "",
                "BG-987-CD",               // Beograd
                "Škoda Octavia",
                4.0f,
                "Active",
                95,
                31250.0
        ));

        list.add(new DriverInfoCard(
                "Marko", "Petrović",
                "marko.petrovic@ns-taxi.com",
                "",
                "NS-456-EF",               // Novi Sad
                "VW Golf",
                4.8f,
                "Active,Online",
                210,
                68210.0
        ));

        list.add(new DriverInfoCard(
                "Jelena", "Nikolić",
                "jelena.nikolic@su-taxi.com",
                "",
                "SU-789-GH",               // Subotica
                "Hyundai i30",
                3.5f,
                "Suspended",
                40,
                11200.0
        ));

        list.add(new DriverInfoCard(
                "Stefan", "Stanković",
                "stefan.stankovic@so-taxi.com",
                "",
                "SO-321-IJ",               // Sombor
                "Renault Clio",
                4.2f,
                "Active,Online",
                150,
                42780.0
        ));

        // add more mock drivers...
        return list;
    }
}
