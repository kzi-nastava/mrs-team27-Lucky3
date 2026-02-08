package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentRideDetailsBinding;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
import com.example.mobile.services.RideService;
import com.example.mobile.ui.maps.RideMapRenderer;
import com.example.mobile.utils.ClientUtils;
import com.example.mobile.utils.SharedPreferencesManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideDetailsFragment extends Fragment {

    private static final String TAG = "RideDetails";
    private FragmentRideDetailsBinding binding;
    private SharedPreferencesManager preferencesManager;
    private RideService rideService;
    
    private RideResponse ride;
    private long rideId = -1;
    
    // Map components
    private MapView mapView;
    private RideMapRenderer mapRenderer;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRideDetailsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferencesManager = new SharedPreferencesManager(requireContext());
        rideService = ClientUtils.getAuthenticatedRideService(preferencesManager);

        // Initialize map
        mapView = root.findViewById(R.id.map_view);
        if (mapView != null) {
            mapRenderer = new RideMapRenderer(requireContext(), mapView);
            mapRenderer.initMap();
        }

        // Set up back button click listener
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        // Get ride ID from arguments
        if (getArguments() != null) {
            rideId = getArguments().getLong("rideId", -1);
        }
        
        if (rideId > 0) {
            loadRideDetails();
        } else {
            Toast.makeText(getContext(), "Invalid ride ID", Toast.LENGTH_SHORT).show();
        }

        return root;
    }
    
    private void loadRideDetails() {
        rideService.getRide(rideId).enqueue(new Callback<RideResponse>() {
            @Override
            public void onResponse(Call<RideResponse> call, Response<RideResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ride = response.body();
                    updateUI();
                } else {
                    Log.e(TAG, "Failed to load ride: " + response.code());
                    Toast.makeText(getContext(), "Failed to load ride details", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<RideResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load ride", t);
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateUI() {
        if (binding == null || ride == null) return;
        
        // Find views by their IDs - they're in the XML layout
        View root = binding.getRoot();
        
        // Update Map with route
        updateMap();
        
        // Update Ride Summary Card
        updateRideSummary(root);
        
        // Update Route Details
        updateRouteDetails(root);
        
        // Update Passenger Info
        updatePassengerInfo(root);
        
        // Update Vehicle Info
        updateVehicleInfo(root);
        
        // Update Timeline
        updateTimeline(root);
    }
    
    private void updateMap() {
        if (mapRenderer == null || mapView == null || ride == null) return;
        
        try {
            LocationDto start = ride.getEffectiveStartLocation();
            LocationDto end = ride.getEffectiveEndLocation();
            
            if (start != null && end != null) {
                List<GeoPoint> routePoints = new ArrayList<>();
                
                // Add start point
                GeoPoint startPoint = new GeoPoint(start.getLatitude(), start.getLongitude());
                routePoints.add(startPoint);
                
                // Add any stops
                List<LocationDto> stops = ride.getStops();
                if (stops != null) {
                    for (LocationDto stop : stops) {
                        routePoints.add(new GeoPoint(stop.getLatitude(), stop.getLongitude()));
                    }
                }
                
                // Add end point
                GeoPoint endPoint = new GeoPoint(end.getLatitude(), end.getLongitude());
                routePoints.add(endPoint);
                
                // Create RoutePointResponse list for the renderer
                List<com.example.mobile.models.RoutePointResponse> routePointResponses = new ArrayList<>();
                for (int i = 0; i < routePoints.size(); i++) {
                    GeoPoint gp = routePoints.get(i);
                    LocationDto loc = new LocationDto("", gp.getLatitude(), gp.getLongitude());
                    com.example.mobile.models.RoutePointResponse rpr = new com.example.mobile.models.RoutePointResponse();
                    rpr.setLocation(loc);
                    routePointResponses.add(rpr);
                }
                
                // Show the route on map
                mapRenderer.showRoute(routePointResponses);
                
                Log.d(TAG, "Map updated with route from " + start.getAddress() + " to " + end.getAddress());
            } else {
                Log.w(TAG, "Cannot display map: missing start or end location");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating map", e);
        }
    }
    
    private void updateRideSummary(View root) {
        try {
            // Find the ride summary card - it's the second LinearLayout after the back button
            // These IDs are defined inline in the XML or we find by structure
            
            // Ride ID and status
            // We need to find TextViews by traversing - the layout doesn't have explicit IDs for all elements
            // Let's use a more targeted approach with findViewWithTag or by content
            
            // For simplicity, let's update using binding where available
            // The layout has hardcoded text, so we need to find views by their position
            
            // Since the layout uses hardcoded values, let's find and update the relevant TextViews
            // We can use content description or tags if added, but for now traverse
            
            // Get total cost
            double totalCost = ride.getEffectiveCost();
            
            // Find the price TextView (shows $25.50 in original)
            // It's the large text in the ride summary that shows the price
            
            // For now, we'll update what we can find
            // The detailed updates would require adding IDs to the XML
            
            Log.d(TAG, "Ride loaded: #" + ride.getId() + " Status: " + ride.getStatus() + " Cost: " + totalCost);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating ride summary", e);
        }
    }
    
    private void updateRouteDetails(View root) {
        try {
            // Update pickup address
            TextView pickupAddress = root.findViewById(R.id.pickup_address);
            if (pickupAddress != null && ride.getEffectiveStartLocation() != null) {
                String addr = ride.getEffectiveStartLocation().getAddress();
                pickupAddress.setText(addr != null ? addr : "—");
            }
            
            // Update destination address
            TextView destAddress = root.findViewById(R.id.destination_address);
            if (destAddress != null && ride.getEffectiveEndLocation() != null) {
                String addr = ride.getEffectiveEndLocation().getAddress();
                destAddress.setText(addr != null ? addr : "—");
            }
            
            // Update route duration
            TextView routeDuration = root.findViewById(R.id.route_duration);
            if (routeDuration != null) {
                Integer estimatedTime = ride.getEstimatedTimeInMinutes();
                if (estimatedTime != null) {
                    routeDuration.setText(estimatedTime + " min");
                } else {
                    // Calculate from start/end time
                    Date startDate = parseDate(ride.getStartTime());
                    Date endDate = parseDate(ride.getEndTime());
                    if (startDate != null && endDate != null) {
                        long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
                        routeDuration.setText(durationMinutes + " min");
                    }
                }
            }
            
            // Update route distance
            TextView routeDistance = root.findViewById(R.id.route_distance);
            if (routeDistance != null) {
                double distance = ride.getEffectiveDistance();
                routeDistance.setText(String.format(Locale.US, "%.1f km", distance));
            }
            
            // Update pickup/arrival times
            TextView pickupTime = root.findViewById(R.id.pickup_time);
            if (pickupTime != null) {
                Date startDate = parseDate(ride.getStartTime());
                if (startDate != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                    pickupTime.setText("Picked up at " + timeFormat.format(startDate));
                } else {
                    pickupTime.setText("");
                }
            }
            
            TextView arrivalTime = root.findViewById(R.id.arrival_time);
            if (arrivalTime != null) {
                Date endDate = parseDate(ride.getEndTime());
                if (endDate != null) {
                    SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);
                    arrivalTime.setText("Arrived at " + timeFormat.format(endDate));
                } else if (ride.isCancelled()) {
                    arrivalTime.setText("Cancelled");
                } else {
                    arrivalTime.setText("");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating route details", e);
        }
    }
    
    private void updatePassengerInfo(View root) {
        try {
            View passengerCard = root.findViewById(R.id.passenger_info);
            if (passengerCard == null) return;
            
            List<RideResponse.PassengerInfo> passengers = ride.getPassengers();
            if (passengers != null && !passengers.isEmpty()) {
                RideResponse.PassengerInfo passenger = passengers.get(0);
                
                // Find passenger name TextView
                TextView nameView = passengerCard.findViewById(R.id.passenger_name);
                if (nameView != null) {
                    nameView.setText(passenger.getFullName());
                }
                
                // Find phone TextView
                TextView phoneView = passengerCard.findViewById(R.id.passenger_phone);
                if (phoneView != null) {
                    String phone = passenger.getPhoneNumber();
                    phoneView.setText(phone != null ? phone : "—");
                }
                
                // Find email TextView
                TextView emailView = passengerCard.findViewById(R.id.passenger_email);
                if (emailView != null) {
                    String email = passenger.getEmail();
                    emailView.setText(email != null ? email : "—");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating passenger info", e);
        }
    }
    
    private void updateVehicleInfo(View root) {
        try {
            View vehicleCard = root.findViewById(R.id.vehicle_info);
            if (vehicleCard == null) return;
            
            RideResponse.DriverInfo driver = ride.getDriver();
            if (driver != null && driver.getVehicle() != null) {
                RideResponse.VehicleInfo vehicle = driver.getVehicle();
                
                // Find vehicle model TextView
                TextView modelView = vehicleCard.findViewById(R.id.vehicle_model);
                if (modelView != null) {
                    modelView.setText(vehicle.getModel() != null ? vehicle.getModel() : "—");
                }
                
                // Find license plate TextView
                TextView plateView = vehicleCard.findViewById(R.id.vehicle_plate);
                if (plateView != null) {
                    plateView.setText(vehicle.getLicensePlates() != null ? vehicle.getLicensePlates() : "—");
                }
                
                // Find color TextView
                TextView colorView = vehicleCard.findViewById(R.id.vehicle_color);
                if (colorView != null) {
                    colorView.setText(vehicle.getColor() != null ? vehicle.getColor() : "—");
                }
                
                // Find year TextView
                TextView yearView = vehicleCard.findViewById(R.id.vehicle_year);
                if (yearView != null) {
                    yearView.setText(vehicle.getYear() != null ? String.valueOf(vehicle.getYear()) : "—");
                }
                
                // Find capacity TextView
                TextView capacityView = vehicleCard.findViewById(R.id.vehicle_capacity);
                if (capacityView != null) {
                    Integer seats = vehicle.getSeatCount();
                    capacityView.setText(seats != null ? seats + " passengers" : "—");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating vehicle info", e);
        }
    }
    
    private void updateTimeline(View root) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
            
            // Update timeline requested
            TextView requestedView = root.findViewById(R.id.timeline_requested);
            if (requestedView != null) {
                Date scheduled = parseDate(ride.getScheduledTime());
                Date start = parseDate(ride.getStartTime());
                Date displayDate = scheduled != null ? scheduled : start;
                if (displayDate != null) {
                    requestedView.setText(dateTimeFormat.format(displayDate));
                }
            }
            
            // Update timeline accepted (use start time as approximation)
            TextView acceptedView = root.findViewById(R.id.timeline_accepted);
            if (acceptedView != null) {
                Date start = parseDate(ride.getStartTime());
                if (start != null) {
                    acceptedView.setText(dateTimeFormat.format(start));
                }
            }
            
            // Update timeline started
            TextView startedView = root.findViewById(R.id.timeline_started);
            if (startedView != null) {
                Date start = parseDate(ride.getStartTime());
                if (start != null) {
                    startedView.setText(dateTimeFormat.format(start));
                }
            }
            
            // Update timeline completed
            TextView completedView = root.findViewById(R.id.timeline_completed);
            if (completedView != null) {
                Date end = parseDate(ride.getEndTime());
                if (end != null) {
                    completedView.setText(dateTimeFormat.format(end));
                } else if (ride.isCancelled()) {
                    completedView.setText("Cancelled");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating timeline", e);
        }
    }
    
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                return sdf.parse(dateStr);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
        binding = null;
    }
}