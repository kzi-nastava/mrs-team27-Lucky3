package com.example.mobile.ui.maps;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import com.example.mobile.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RideMapRenderer {
    private final MapView map;
    private final Context context;
    private Geocoder geocoder;
    private Polyline currentRoadOverlay;   // keeps last road route
    private final List<Marker> currentMarkers = new ArrayList<>();

    public RideMapRenderer(Context context, MapView map) {
        this.context = context.getApplicationContext();
        this.map = map;
    }

    public void showRideByAddresses(List<String> locations) {
        if (locations == null || locations.isEmpty()) return;

        // Remove old markers
        clearMarkers();

        List<GeoPoint> routePoints = new ArrayList<>();

        for (String loc : locations) {
            try {
                List<Address> list = geocoder.getFromLocationName(loc, 1);
                if (list != null && !list.isEmpty()) {
                    Address a = list.get(0);
                    double lat = a.getLatitude();
                    double lon = a.getLongitude();

                    GeoPoint gp = new GeoPoint(lat, lon);
                    routePoints.add(gp);

                    addMarker(gp, loc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        drawRoadAlongRoute(routePoints);
        map.invalidate();
    }

    public void initMap() {
        // Geocoder
        if (geocoder == null) {
            geocoder = new Geocoder(context, Locale.getDefault());
        }

        // Tile source - CartoDB Dark Matter
        map.setTileSource(new XYTileSource(
            "CartoDark",
            0,
            20,
            256,
            ".png",
            new String[] {
                "https://a.basemaps.cartocdn.com/dark_all/",
                "https://b.basemaps.cartocdn.com/dark_all/",
                "https://c.basemaps.cartocdn.com/dark_all/"
            }
        ));

        // Controls
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false);

        // Controller
        IMapController mapController = map.getController();
        mapController.setZoom(13);

        // Initial center (Novi Sad)
        GeoPoint startPoint = new GeoPoint(45.2517, 19.8369);
        mapController.setCenter(startPoint);

        // Scale bar
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map);
        map.getOverlays().add(scaleBarOverlay);
    }

    private void addMarker(GeoPoint point, String title) {
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSnippet("Tap for details");
        // Set red icon from resources
        marker.setIcon(
                map.getContext().getResources().getDrawable(
                        R.drawable.ic_map_pin,
                        map.getContext().getTheme()
                )
        );

        map.getOverlays().add(marker);
        currentMarkers.add(marker);
    }

    public GeoPoint geocodeLocation(String addressStr) {
        if (geocoder == null) return null;
        try {
            List<Address> list = geocoder.getFromLocationName(addressStr, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                return new GeoPoint(a.getLatitude(), a.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void clearMarkers() {
        for (Marker m : currentMarkers) {
            map.getOverlays().remove(m);      // remove each marker overlay [web:50]
        }
        currentMarkers.clear();
        map.invalidate();
    }

    private void drawRoadAlongRoute(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return;

        new Thread(() -> {
            try {
                RoadManager roadManager =
                        new OSRMRoadManager(context, "your-app-user-agent");
                ArrayList<GeoPoint> waypoints = new ArrayList<>(points);

                Road road = roadManager.getRoad(waypoints); // builds route [web:59]

                if (road.mStatus != Road.STATUS_OK) {
                    showToast("Route error: " + road.mStatus);
                    return;
                }

                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
                roadOverlay.setColor(Color.parseColor("#eab308")); // Yellow like web
                roadOverlay.setWidth(15f);

                runOnUiThread(() -> {
                    // Remove previous road overlay if exists
                    if (currentRoadOverlay != null) {
                        map.getOverlays().remove(currentRoadOverlay);
                    }

                    // Add new one and remember it
                    map.getOverlays().add(roadOverlay);
                    currentRoadOverlay = roadOverlay;

                    map.invalidate();
                });

            } catch (Exception e) {
                e.printStackTrace();
                showToast("Routing failed: " + e.getMessage());
            }
        }).start();
    }


    private void showToast(String msg) {
        // called from background thread
        runOnUiThread(() ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    private void runOnUiThread(Runnable r) {
        // map.getContext() should be an Activity or you can pass FragmentActivity from fragment
        if (map.getContext() instanceof androidx.fragment.app.FragmentActivity) {
            ((androidx.fragment.app.FragmentActivity) map.getContext()).runOnUiThread(r);
        }
    }
}
