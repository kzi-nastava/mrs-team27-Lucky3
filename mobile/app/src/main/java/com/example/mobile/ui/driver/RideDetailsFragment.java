package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.databinding.FragmentRideDetailsBinding;
import com.example.mobile.models.LocationDto;
import com.example.mobile.models.RideResponse;
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
        String token = "Bearer " + preferencesManager.getToken();
        ClientUtils.rideService.getRide(rideId, token).enqueue(new Callback<RideResponse>() {
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
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

            // Ride number
            TextView rideNumber = root.findViewById(R.id.ride_number);
            if (rideNumber != null) {
                rideNumber.setText("Ride #" + ride.getId());
            }

            // Status badge with color
            TextView rideStatus = root.findViewById(R.id.ride_status);
            if (rideStatus != null) {
                rideStatus.setText(ride.getDisplayStatus().toUpperCase());
                if (ride.isFinished()) {
                    rideStatus.setBackgroundResource(R.drawable.bg_badge_green);
                    rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500));
                } else if (ride.isCancelled()) {
                    rideStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
                    rideStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                }
            }

            // Price (top-right)
            double totalCost = ride.getEffectiveCost();
            TextView ridePrice = root.findViewById(R.id.ride_price);
            if (ridePrice != null) {
                ridePrice.setText(String.format(Locale.US, "%.0f RSD", totalCost));
            }

            // Date range (e.g., "Dec 21, 2025, 4:00 PM - 4:25 PM")
            TextView rideDateRange = root.findViewById(R.id.ride_date_range);
            if (rideDateRange != null) {
                Date startDate = parseDate(ride.getStartTime());
                Date endDate = parseDate(ride.getEndTime());
                if (startDate != null && endDate != null) {
                    rideDateRange.setText(dateTimeFormat.format(startDate) + ", " +
                        timeFormat.format(startDate) + " - " + timeFormat.format(endDate));
                } else if (startDate != null) {
                    rideDateRange.setText(dateTimeFormat.format(startDate) + ", " +
                        timeFormat.format(startDate));
                } else {
                    // For cancelled rides that never started, use scheduled time
                    Date scheduled = parseDate(ride.getScheduledTime());
                    if (scheduled != null) {
                        rideDateRange.setText(dateTimeFormat.format(scheduled) + ", " +
                            timeFormat.format(scheduled));
                    } else {
                        rideDateRange.setText("\u2014");
                    }
                }
            }

            // Cancellation section
            LinearLayout cancellationSection = root.findViewById(R.id.cancellation_section);
            if (cancellationSection != null) {
                if (ride.isCancelled()) {
                    cancellationSection.setVisibility(View.VISIBLE);

                    TextView cancellationLabel = root.findViewById(R.id.cancellation_label);
                    if (cancellationLabel != null) {
                        String status = ride.getStatus();
                        if ("CANCELLED_BY_DRIVER".equals(status)) {
                            cancellationLabel.setText("Cancelled by Driver");
                        } else if ("CANCELLED_BY_PASSENGER".equals(status)) {
                            cancellationLabel.setText("Cancelled by Passenger");
                        } else {
                            cancellationLabel.setText("Cancelled");
                        }
                    }

                    TextView cancellationReason = root.findViewById(R.id.cancellation_reason);
                    if (cancellationReason != null) {
                        String reason = ride.getRejectionReason();
                        if (reason != null && !reason.isEmpty()) {
                            cancellationReason.setVisibility(View.VISIBLE);
                            cancellationReason.setText(reason);
                        } else {
                            cancellationReason.setVisibility(View.GONE);
                        }
                    }
                } else {
                    cancellationSection.setVisibility(View.GONE);
                }
            }

            // Vehicle type
            TextView rideVehicleType = root.findViewById(R.id.ride_vehicle_type);
            if (rideVehicleType != null) {
                String vt = ride.getVehicleType();
                if (vt != null) {
                    // Format: STANDARD -> Standard, LUXURY -> Luxury, VAN -> Van
                    rideVehicleType.setText(vt.substring(0, 1).toUpperCase() + vt.substring(1).toLowerCase());
                } else {
                    rideVehicleType.setText("");
                }
            }

            // Fare breakdown
            double distance = ride.getEffectiveDistance();
            Double baseFare = ride.getRateBaseFare();
            Double perKm = ride.getRatePricePerKm();

            // Base fare
            TextView baseFareValue = root.findViewById(R.id.base_fare_value);
            if (baseFareValue != null) {
                baseFareValue.setText(baseFare != null
                    ? String.format(Locale.US, "%.0f RSD", baseFare) : "—");
            }

            // Distance cost
            TextView distanceLabel = root.findViewById(R.id.distance_label);
            TextView distanceValue = root.findViewById(R.id.distance_value);
            if (distanceLabel != null) {
                distanceLabel.setText(String.format(Locale.US, "Distance (%.1f km)", distance));
            }
            if (distanceValue != null) {
                double distCost = (perKm != null ? perKm : 0) * distance;
                distanceValue.setText(String.format(Locale.US, "%.0f RSD", distCost));
            }

            // Total
            TextView totalValue = root.findViewById(R.id.total_value);
            if (totalValue != null) {
                totalValue.setText(String.format(Locale.US, "%.0f RSD", totalCost));
            }

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
            RideResponse.VehicleInfo vehicle = (driver != null) ? driver.getVehicle() : null;
            
            // Vehicle model — try nested vehicle first, fall back to top-level
            TextView modelView = vehicleCard.findViewById(R.id.vehicle_model);
            if (modelView != null) {
                String modelStr = (vehicle != null && vehicle.getModel() != null)
                    ? vehicle.getModel() : ride.getModel();
                modelView.setText(modelStr != null ? modelStr : "\u2014");
            }
            
            // License plate — try nested vehicle first, fall back to top-level
            TextView plateView = vehicleCard.findViewById(R.id.vehicle_plate);
            if (plateView != null) {
                String plate = (vehicle != null && vehicle.getLicensePlates() != null)
                    ? vehicle.getLicensePlates() : ride.getLicensePlates();
                plateView.setText(plate != null ? plate : "\u2014");
            }
            
            // Vehicle type — try nested vehicle first, fall back to top-level
            TextView typeView = vehicleCard.findViewById(R.id.vehicle_type);
            if (typeView != null) {
                String vt = (vehicle != null && vehicle.getVehicleType() != null)
                    ? vehicle.getVehicleType() : ride.getVehicleType();
                if (vt != null) {
                    typeView.setText(vt.substring(0, 1).toUpperCase() + vt.substring(1).toLowerCase());
                } else {
                    typeView.setText("\u2014");
                }
            }
            
            // Transport features (baby/pet)
            TextView transportView = vehicleCard.findViewById(R.id.vehicle_transport);
            if (transportView != null) {
                Boolean baby = (vehicle != null) ? vehicle.getBabyTransport() : ride.getBabyTransport();
                Boolean pet = (vehicle != null) ? vehicle.getPetTransport() : ride.getPetTransport();
                List<String> features = new ArrayList<>();
                if (Boolean.TRUE.equals(baby)) features.add("Baby");
                if (Boolean.TRUE.equals(pet)) features.add("Pet");
                transportView.setText(features.isEmpty() ? "None" : String.join(", ", features));
            }
            
            // Capacity
            TextView capacityView = vehicleCard.findViewById(R.id.vehicle_capacity);
            if (capacityView != null) {
                Integer seats = (vehicle != null) ? vehicle.getSeatCount() : null;
                capacityView.setText(seats != null ? seats + " passengers" : "\u2014");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating vehicle info", e);
        }
    }
    
    private void updateTimeline(View root) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
            boolean cancelled = ride.isCancelled();
            boolean finished = ride.isFinished();

            // --- Requested ---
            TextView requestedView = root.findViewById(R.id.timeline_requested);
            if (requestedView != null) {
                Date scheduled = parseDate(ride.getScheduledTime());
                Date start = parseDate(ride.getStartTime());
                Date displayDate = scheduled != null ? scheduled : start;
                if (displayDate != null) {
                    requestedView.setText(dateTimeFormat.format(displayDate));
                }
            }
            // Requested dot: always green (it happened)
            View dotRequested = root.findViewById(R.id.dot_requested);
            if (dotRequested != null) {
                dotRequested.setBackgroundResource(R.drawable.bg_dot_green);
            }
            // Connecting line after requested: hide if cancelled (no more steps)
            View lineRequested = root.findViewById(R.id.line_requested);
            if (lineRequested != null) {
                lineRequested.setVisibility(cancelled ? View.INVISIBLE : View.VISIBLE);
            }

            // --- Ride Started (hidden for cancelled rides) ---
            LinearLayout startedRow = root.findViewById(R.id.timeline_started_row);
            if (startedRow != null) {
                if (cancelled) {
                    startedRow.setVisibility(View.GONE);
                } else {
                    startedRow.setVisibility(View.VISIBLE);
                    TextView startedView = root.findViewById(R.id.timeline_started);
                    if (startedView != null) {
                        Date start = parseDate(ride.getStartTime());
                        if (start != null) {
                            startedView.setText(dateTimeFormat.format(start));
                        }
                    }
                    View dotStarted = root.findViewById(R.id.dot_started);
                    if (dotStarted != null) {
                        dotStarted.setBackgroundResource(
                            finished ? R.drawable.bg_dot_green : R.drawable.bg_dot_gray);
                    }
                }
            }

            // --- Final row: Completed or Cancelled ---
            TextView finalTitle = root.findViewById(R.id.timeline_final_title);
            if (finalTitle != null) {
                finalTitle.setText(cancelled ? "Ride Cancelled" : "Ride Completed");
                if (cancelled) {
                    finalTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
                } else {
                    finalTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                }
            }

            View dotCompleted = root.findViewById(R.id.dot_completed);
            if (dotCompleted != null) {
                if (finished) {
                    dotCompleted.setBackgroundResource(R.drawable.bg_dot_green);
                } else if (cancelled) {
                    dotCompleted.setBackgroundResource(R.drawable.bg_dot_red);
                } else {
                    dotCompleted.setBackgroundResource(R.drawable.bg_dot_gray);
                }
            }

            TextView completedView = root.findViewById(R.id.timeline_completed);
            if (completedView != null) {
                Date end = parseDate(ride.getEndTime());
                if (end != null) {
                    completedView.setText(dateTimeFormat.format(end));
                } else if (cancelled) {
                    // Use start time or scheduled time as cancellation time
                    Date cancelTime = parseDate(ride.getStartTime());
                    if (cancelTime == null) cancelTime = parseDate(ride.getScheduledTime());
                    completedView.setText(cancelTime != null
                        ? dateTimeFormat.format(cancelTime) : "\u2014");
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