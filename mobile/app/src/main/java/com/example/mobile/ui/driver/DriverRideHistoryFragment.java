package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverRideHistoryBinding;
import com.example.mobile.databinding.ItemRideHistoryBinding;

import java.util.ArrayList;
import java.util.List;

public class DriverRideHistoryFragment extends Fragment {

    private FragmentDriverRideHistoryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverRideHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Ride History");
        }

        setupRecyclerView();

        return root;
    }

    private void setupRecyclerView() {
        RidesAdapter adapter = new RidesAdapter();
        binding.ridesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.ridesRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.RideViewHolder> {

        private final List<String> dummyRides;

        public RidesAdapter() {
            dummyRides = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                dummyRides.add("Ride " + (i + 1));
            }
        }

        @NonNull
        @Override
        public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRideHistoryBinding binding = ItemRideHistoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new RideViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
            holder.itemView.setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_nav_driver_history_to_nav_ride_details);
            });
        }

        @Override
        public int getItemCount() {
            return dummyRides.size();
        }

        class RideViewHolder extends RecyclerView.ViewHolder {
            public RideViewHolder(ItemRideHistoryBinding binding) {
                super(binding.getRoot());
            }
        }
    }
}