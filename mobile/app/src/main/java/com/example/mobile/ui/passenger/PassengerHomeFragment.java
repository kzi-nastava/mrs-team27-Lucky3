package com.example.mobile.ui.passenger;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentPassengerHomeBinding;
import com.google.android.material.button.MaterialButton;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;


public class PassengerHomeFragment extends Fragment {

    private FragmentPassengerHomeBinding binding;
    private MapView map;
    private Geocoder geocoder;
    private IMapController mapController;
    private MaterialButton btnRequestRide;
    private MaterialButton btnLinkPassengers;
    //private MaterialButton btnAdditionalRideInfo;

    private ArrayList<String> rideLocations = new ArrayList<>();    // collected from rideRequest form, only string locations
    private ArrayList<String> passengerEmails = new ArrayList<>();
    private List<GeoPoint> routePoints = new ArrayList<>(); // this is used for drawing on map
    private String vehicleType;
    private boolean babyTransport;
    private boolean petTransport;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPassengerHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Navbar setup
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v -> {
                ((com.example.mobile.MainActivity) requireActivity()).openDrawer();
            });
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Home");
        }

        Context context = requireActivity().getApplicationContext();
        Configuration.getInstance().load(context,
                PreferenceManager.getDefaultSharedPreferences(context));

        // Set user agent for requests
        Configuration.getInstance().setUserAgentValue(requireActivity().getPackageName());

        // Get references
        map = binding.map;
        btnRequestRide = binding.btnRequestRide;
        btnLinkPassengers = binding.btnLinkPassengers;
        //btnAdditionalRideInfo = binding.btnAdditionalRideInfo;

        // Show dialog on button click
        btnRequestRide.setOnClickListener(v -> openRequestRideDialog());
        btnLinkPassengers.setOnClickListener(v -> openLinkPassengersDialog());
        //btnAdditionalRideInfo.setOnClickListener(v -> openAdditionalRideInfoDialog());


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

                        handleLocations(rideLocations); // your existing map markers
                        tryBuildRideRequest();          // new method
                    }
                });

        getParentFragmentManager().setFragmentResultListener(
                LinkPassengersDialog.REQUEST_KEY,
                this,
                (requestKey, bundle) -> {
                    ArrayList<String> emails =
                            bundle.getStringArrayList(LinkPassengersDialog.KEY_EMAILS);
                    if (emails != null) {
                        passengerEmails = emails;
                        //tryBuildRideRequest(); ne treba da se salje request kada popunjavam emailove
                    }
                });


        geocoder = new Geocoder(requireContext(), Locale.getDefault());

        // Set tile source to OpenStreetMap
        map.setTileSource(TileSourceFactory.MAPNIK);

        // Enable multi-touch controls (pinch zoom, etc.)
        map.setMultiTouchControls(true);

        // Enable built-in zoom controls
        map.setBuiltInZoomControls(true);

        // Get map controller
        mapController = map.getController();

        // Set initial zoom level
        mapController.setZoom(13);

        // Set initial location (Novi Sad, Serbia)
        GeoPoint startPoint = new GeoPoint(45.2517, 19.8369);
        mapController.setCenter(startPoint);

        // Add scale bar overlay
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map);
        map.getOverlays().add(scaleBarOverlay);


        return root;
    }

    private void tryBuildRideRequest() {

        //TODO: send dto object to backend
        Toast.makeText(requireContext(), "Request sent", Toast.LENGTH_LONG).show(); // debug [web:199]
    }

    private void drawRoutePolyline(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return;

        Polyline polyline = new Polyline();
        polyline.setPoints(points);
        polyline.setColor(Color.RED);     // choose color
        polyline.setWidth(8f);               // line thickness

        map.getOverlays().add(polyline);
        map.invalidate();
    }

    private void openRequestRideDialog() {
        RequestRideFormDialog dialog = RequestRideFormDialog.newInstance();
        dialog.show(getParentFragmentManager(), "RequestRideDialog");
    }

    private void openLinkPassengersDialog() {
        LinkPassengersDialog dialog = LinkPassengersDialog.newInstance();
        dialog.show(getParentFragmentManager(), "LinkPassengersDialog");
    }

    /*
    private void openAdditionalRideInfoDialog() {
        Toast.makeText(getContext(), "AdditionalInfo Not implemented yet", Toast.LENGTH_SHORT).show();
    }*/


    private void handleLocations(List<String> locations) {
// this is used only for debug
        locations = new ArrayList<>(
                Arrays.asList(
                        "Cirpanova 5, Novi Sad",
                        "Heroja Pinkija 10, Novi Sad",
                        "Rade Končara 3, Petrovaradin"
                ));

        routePoints.clear();
        rideLocations.clear();
        rideLocations.addAll(locations);

        for (String loc : locations) {
            try {
                List<Address> list = geocoder.getFromLocationName(loc, 1); // forward geocode [web:31][web:41]
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    double lat = a.getLatitude();
                    double lon = a.getLongitude();

                    GeoPoint gp = new GeoPoint(lat, lon);
                    routePoints.add(gp);

                    addMarker(lat, lon, loc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // draw polyline through all locations
        // drawRoutePolyline(routePoints);

        // draw real route along roads
        drawRoadAlongRoute(routePoints);

        map.invalidate();
    }
    private void drawRoadAlongRoute(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return;

        new Thread(() -> {
            try {
                RoadManager roadManager =
                        new OSRMRoadManager(requireContext(), "your-app-user-agent"); // set UA [web:283]
                ArrayList<GeoPoint> waypoints = new ArrayList<>(points);

                Road road = roadManager.getRoad(waypoints); // network call [web:278]

                if (road.mStatus != Road.STATUS_OK) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Route error: " + road.mStatus,
                                    Toast.LENGTH_SHORT).show());
                    return;
                }

                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                roadOverlay.setColor(Color.RED);
                roadOverlay.setWidth(10f);

                requireActivity().runOnUiThread(() -> {
                    map.getOverlays().add(roadOverlay);
                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Routing failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    /**
     * Add a marker to the map
     */
    private void addMarker(double latitude, double longitude, String title) {
        Toast.makeText(requireContext(), latitude + " " + longitude, Toast.LENGTH_LONG).show(); // debug [web:199]
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(title);
        marker.setSnippet("Tap for details");

        // Optional: Set marker icon
        // marker.setIcon(getResources().getDrawable(R.drawable.ic_marker));

        map.getOverlays().add(marker);
        map.invalidate();
    }

    /**
     * Draw a polyline route between two points
     */
    private void drawRoute() {
        Polyline line = new Polyline(map);

        // Create route coordinates (Kragujevac to nearby location)
        GeoPoint point1 = new GeoPoint(44.0165, 20.9105);
        GeoPoint point2 = new GeoPoint(44.0250, 20.9200);

        line.addPoint(point1);
        line.addPoint(point2);

        // Style the line
        line.setColor(0xFF2196F3);      // Blue color
        line.setWidth(5);                // 5 pixels wide

        map.getOverlays().add(line);
        map.invalidate();
    }

    /**
     * Called when activity is resumed
     */
    @Override
    public void onResume() {
        super.onResume();
        map.onResume(); // Needed for compass, location overlays
    }

    /**
     * Called when activity is paused
     */
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