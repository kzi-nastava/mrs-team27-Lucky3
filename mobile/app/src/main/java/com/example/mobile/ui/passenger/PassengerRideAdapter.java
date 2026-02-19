package com.example.mobile.ui.passenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile.R;
import com.example.mobile.models.RideResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying passenger ride history.
 * Uses item_ride_history.xml layout with the correct view IDs.
 */
public class PassengerRideAdapter extends RecyclerView.Adapter<PassengerRideAdapter.ViewHolder> {

    private List<RideResponse> rides = new ArrayList<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    public void setRides(List<RideResponse> rides) {
        this.rides = rides != null ? rides : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideResponse ride = rides.get(position);

        // Status badge
        String displayStatus = ride.getDisplayStatus();
        if ("Pending".equalsIgnoreCase(displayStatus)) {
            displayStatus = "In Progress";
        }
        holder.tvStatus.setText(displayStatus.toUpperCase());

        // Date
        Date startDate = parseDate(ride.getStartTime());
        if (startDate == null) startDate = parseDate(ride.getScheduledTime());
        holder.tvDate.setText(startDate != null ? DATE_FORMAT.format(startDate) : "—");

        // Departure
        if (ride.getEffectiveStartLocation() != null && ride.getEffectiveStartLocation().getAddress() != null) {
            holder.tvDeparture.setText(ride.getEffectiveStartLocation().getAddress());
        } else {
            holder.tvDeparture.setText("—");
        }

        // Destination
        if (ride.getEffectiveEndLocation() != null && ride.getEffectiveEndLocation().getAddress() != null) {
            holder.tvDestination.setText(ride.getEffectiveEndLocation().getAddress());
        } else {
            holder.tvDestination.setText("—");
        }

        // Vehicle info: distance + duration
        double distance = ride.getEffectiveDistance();
        Date endDate = parseDate(ride.getEndTime());
        String durationStr = "—";
        if (startDate != null && endDate != null) {
            long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
            durationStr = durationMinutes + " min";
        } else if (ride.getEstimatedTimeInMinutes() != null) {
            durationStr = ride.getEstimatedTimeInMinutes() + " min";
        }
        holder.tvVehicleInfo.setText(String.format(Locale.US, "%.1f km • %s", distance, durationStr));

        // Cost
        double cost = ride.getEffectiveCost();
        holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));

        // Driver name
        if (ride.getDriver() != null) {
            String dName = (ride.getDriver().getName() != null ? ride.getDriver().getName() : "") + " " +
                    (ride.getDriver().getSurname() != null ? ride.getDriver().getSurname() : "");
            holder.tvDriverName.setText("Driver: " + dName.trim());
            holder.tvDriverName.setVisibility(View.VISIBLE);
        } else {
            holder.tvDriverName.setVisibility(View.GONE);
        }

        // Rejection reason
        String rejection = ride.getRejectionReason();
        if (rejection != null && !rejection.isEmpty()) {
            holder.tvRejectionReason.setVisibility(View.VISIBLE);
            holder.tvRejectionReason.setText(rejection);
        } else {
            holder.tvRejectionReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return rides.size();
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvDate, tvDeparture, tvDestination;
        TextView tvVehicleInfo, tvCost, tvDriverName, tvRejectionReason;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDeparture = itemView.findViewById(R.id.tv_departure);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvVehicleInfo = itemView.findViewById(R.id.tv_vehicle_info);
            tvCost = itemView.findViewById(R.id.tv_cost);
            tvDriverName = itemView.findViewById(R.id.tv_driver_name);
            tvRejectionReason = itemView.findViewById(R.id.tv_rejection_reason);
        }
    }
}
