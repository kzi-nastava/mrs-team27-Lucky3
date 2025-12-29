package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverDashboardBinding;
import java.util.ArrayList;
import java.util.List;

public class DriverDashboardFragment extends Fragment {

    private FragmentDriverDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Dashboard");
        }

        // RecyclerView setup
        RecyclerView recyclerView = binding.rideHistoryRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<RideHistoryAdapter.RideHistoryItem> items = new ArrayList<>();
        items.add(new RideHistoryAdapter.RideHistoryItem("21/12/2025", "4:00 PM", "21/12/2025", "4:25 PM", "111 Pine St San Francisco, CA...", "222 Oak St San Francisco, CA...", 1, "6.3 km", "25 min"));
        items.add(new RideHistoryAdapter.RideHistoryItem("21/12/2025", "11:35 AM", "21/12/2025", "11:50 AM", "123 Market St San Francisco...", "456 Mission St San Francisco...", 1, "5.2 km", "15 min"));
        items.add(new RideHistoryAdapter.RideHistoryItem("20/12/2025", "12:15 PM", "20/12/2025", "12:45 PM", "567 California St San Francisco...", "890 Stockton St San Francisco...", 1, "5.8 km", "30 min"));
        items.add(new RideHistoryAdapter.RideHistoryItem("18/12/2025", "5:45 PM", "-", "-", "1234 Valencia St San Francisco...", "5678 Divisadero St San Francisco...", 1, "0 km", "-"));
        items.add(new RideHistoryAdapter.RideHistoryItem("15/12/2025", "9:00 AM", "15/12/2025", "9:30 AM", "999 Baker St San Francisco, CA...", "101 1st St San Francisco, CA...", 1, "12.5 km", "30 min"));

        RideHistoryAdapter adapter = new RideHistoryAdapter(items);
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

