package com.example.mobile.ui.passenger;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobile.Domain.FavouriteRideCard;
import com.example.mobile.R;

import java.util.ArrayList;
import java.util.List;

public class PassengerFavouritesFragment extends Fragment {

    private final List<FavouriteRideCard> mockRides = new ArrayList<>();
    private PassengerFavouriteRidesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_passenger_favourites, container, false);

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Favourite rides");
        }

        // Mock data
        mockRides.clear();
        mockRides.add(new FavouriteRideCard("Kragujevac Center", "Aerodrom"));
        mockRides.add(new FavouriteRideCard("Kragujevac Center", "Beograd"));
        mockRides.add(new FavouriteRideCard("FON", "Zvezdara"));
        mockRides.add(new FavouriteRideCard("ETF", "Novi Beograd"));

        ListView listView = root.findViewById(R.id.list_favourites);

        adapter = new PassengerFavouriteRidesAdapter(
                requireContext(),
                mockRides,
                new PassengerFavouriteRidesAdapter.OnRideActionListener() {
                    @Override
                    public void onRemoveClicked(FavouriteRideCard ride, int position) {
                        mockRides.remove(position);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(),
                                "Removed: " + ride.getStart() + " -> " + ride.getEnd(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRequestClicked(FavouriteRideCard ride, int position) {
                        Toast.makeText(requireContext(),
                                "Requesting: " + ride.getStart() + " -> " + ride.getEnd(),
                                Toast.LENGTH_SHORT).show();
                        // TODO: trigger your real request-ride flow here
                    }
                });

        listView.setAdapter(adapter);

        return root;
    }
}
