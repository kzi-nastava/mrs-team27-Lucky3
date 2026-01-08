package com.example.mobile.ui.passenger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobile.databinding.FragmentPassengerHomeBinding;
import com.example.mobile.databinding.ViewCustomNavbarBinding;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.google.android.material.button.MaterialButton;

import android.widget.Toast;

import org.osmdroid.views.MapView;
import java.util.ArrayList;

public class PassengerHomeFragment extends Fragment {
    private FragmentPassengerHomeBinding binding;
    private MapView map;
    private MaterialButton btnRequestRide;
    private MaterialButton btnLinkPassengers;

    private ArrayList<String> rideLocations = new ArrayList<>();    // collected from rideRequest form, only string locations
    private ArrayList<String> passengerEmails = new ArrayList<>();
    private String vehicleType;
    private boolean babyTransport;
    private boolean petTransport;


    private RideMapRenderer mapRenderer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPassengerHomeBinding.inflate(inflater, container, false);

        // Navbar setup
        ViewCustomNavbarBinding navbar = binding.navbar;
        if (navbar != null) {
            navbar.btnMenu.setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            (navbar.toolbarTitle).setText("Home");
        }

        // Get references
        btnRequestRide = binding.btnRequestRide;
        btnLinkPassengers = binding.btnLinkPassengers;

        // setting up the map
        map = binding.map;
        mapRenderer = new RideMapRenderer(requireActivity(), map);
        mapRenderer.initMap();

        // Show dialog on button click
        btnRequestRide.setOnClickListener(v -> openRequestRideDialog());
        btnLinkPassengers.setOnClickListener(v -> openLinkPassengersDialog());

        //when dialogs are called, this function listens for data inserted in dialog form
        listenRideRequestAndEmails();

        return binding.getRoot();
    }

    private void listenRideRequestAndEmails(){
        // listening for locations and ride request
        getParentFragmentManager().setFragmentResultListener(
                RequestRideFormDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> locations =
                            bundle.getStringArrayList(RequestRideFormDialog.KEY_LOCATIONS);
                    String vehicle = bundle.getString(RequestRideFormDialog.KEY_VEHICLE_TYPE);
                    boolean baby = bundle.getBoolean(RequestRideFormDialog.KEY_BABY_TRANSPORT, false);
                    boolean pet = bundle.getBoolean(RequestRideFormDialog.KEY_PET_TRANSPORT, false);

                    if (locations != null && !locations.isEmpty()) {
                        rideLocations = locations;
                        vehicleType = vehicle;
                        babyTransport = baby;
                        petTransport = pet;

                        mapRenderer.showRideByAddresses(rideLocations);
                        tryBuildRideRequest();      //sends ride request
                    }
                });

        //listening for emails
        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> emails =
                            bundle.getStringArrayList(LinkPassengersDialog.KEY_EMAILS);
                    if (emails != null) {
                        passengerEmails = emails;
                    }
                });
    }

    private void tryBuildRideRequest() {
        //TODO: send dto object to backend
        //TODO: send emails, notifications, check if ride can be created...
        //Toast.makeText(requireContext(), "Request sent", Toast.LENGTH_LONG).show(); // debug [web:199]
        onRideCreatedSuccessfully(123);
    }

    private void openRequestRideDialog() {
        RequestRideFormDialog dialog = RequestRideFormDialog.newInstance();
        dialog.show(getParentFragmentManager(), "RequestRideDialog");
    }

    private void openLinkPassengersDialog() {
        LinkPassengersDialog dialog = LinkPassengersDialog.newInstance();
        dialog.show(getParentFragmentManager(), "LinkPassengersDialog");
    }

    private void onRideCreatedSuccessfully(double price) {
        RideCreatedDialog dialog =
                RideCreatedDialog.newInstance(price, passengerEmails);
        dialog.show(getParentFragmentManager(), "RideCreatedDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume(); // Needed for compass, location overlays
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause(); // Save configuration changes
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}