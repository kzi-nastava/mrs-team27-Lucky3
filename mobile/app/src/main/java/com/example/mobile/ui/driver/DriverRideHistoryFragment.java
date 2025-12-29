package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.mobile.R;
import com.example.mobile.databinding.FragmentDriverRideHistoryBinding;

public class DriverRideHistoryFragment extends Fragment {

    private FragmentDriverRideHistoryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDriverRideHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // In a real app, we would set up the RecyclerView here.
        // For now, we can simulate a click on a list item if we had one, 
        // or just add a temporary button or click listener to the placeholder to navigate.
        
        // Since I don't have a real adapter, I'll just make the whole view clickable for demo purposes
        // or assume the user will implement the adapter later.
        // Let's add a click listener to the "Recent Rides" title to simulate navigation to details
        binding.historyTitle.setOnClickListener(v -> {
             Navigation.findNavController(v).navigate(R.id.action_nav_driver_history_to_nav_ride_details);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}