package com.example.mobile.ui.admin;

import com.example.mobile.R;
import com.example.mobile.model.DriverInfoCard;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;

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
    private ArrayList<DriverInfoCard> driverList;

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

        // 1) Prepare data (mock for now)
        driverList = createMockDrivers();

        // 2) Create adapter, pass context + list
        adapter = new AdminDriverAdapter(requireContext(), driverList);

        // 3) Attach adapter to ListView
        lvDrivers.setAdapter(adapter);

        return root;
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
