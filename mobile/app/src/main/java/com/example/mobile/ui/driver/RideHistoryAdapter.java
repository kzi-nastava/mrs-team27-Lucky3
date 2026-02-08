package com.example.mobile.ui.driver;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile.R;
import com.example.mobile.models.RideResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.RideViewHolder> {

    private List<RideResponse> rides = new ArrayList<>();

    public void setRides(List<RideResponse> rides) {
        this.rides = rides != null ? rides : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        RideResponse ride = rides.get(position);
        holder.bind(ride);
    }

    @Override
    public int getItemCount() {
        return rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStatus;
        private final TextView tvDate;
        private final TextView tvDeparture;
        private final TextView tvDestination;
        private final TextView tvVehicleInfo;
        private final TextView tvCost;
        private final TextView tvDriverName;
        private final TextView tvRejectionReason;

        public RideViewHolder(@NonNull View itemView) {
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

        public void bind(RideResponse ride) {
            // Status
            if (ride.getStatus() != null) {
                tvStatus.setText(ride.getStatus());
                int statusColor = getStatusColor(ride.getStatus());
                tvStatus.setTextColor(statusColor);
            }

            // Date
            if (ride.getStartTime() != null) {
                tvDate.setText(ride.getStartTime());
            } else if (ride.getScheduledTime() != null) {
                tvDate.setText(ride.getScheduledTime());
            }

            // Locations
            if (ride.getDeparture() != null) {
                tvDeparture.setText(ride.getDeparture().getAddress());
            }
            if (ride.getDestination() != null) {
                tvDestination.setText(ride.getDestination().getAddress());
            }

            // Vehicle info
            if (ride.getModel() != null && ride.getLicensePlates() != null) {
                tvVehicleInfo.setText(ride.getModel() + " • " + ride.getLicensePlates());
            } else if (ride.getVehicleType() != null) {
                tvVehicleInfo.setText(ride.getVehicleType());
            }

            // Cost
            if (ride.getTotalCost() != null) {
                tvCost.setText(String.format(Locale.US, "$%.2f", ride.getTotalCost()));
            } else if (ride.getEstimatedCost() != null) {
                tvCost.setText(String.format(Locale.US, "~$%.2f", ride.getEstimatedCost()));
            }

            // Driver
            if (ride.getDriver() != null) {
                tvDriverName.setText("Driver: " + ride.getDriver().getName());
            } else {
                tvDriverName.setText("Driver: Not assigned");
            }

            // Rejection reason
            if (ride.getRejectionReason() != null && !ride.getRejectionReason().isEmpty()) {
                tvRejectionReason.setVisibility(View.VISIBLE);
                tvRejectionReason.setText("Reason: " + ride.getRejectionReason());
            } else {
                tvRejectionReason.setVisibility(View.GONE);
            }
        }

        private int getStatusColor(String status) {
            switch (status.toUpperCase()) {
                case "FINISHED":
                    return Color.parseColor("#00C950"); // green_500
                case "PENDING":
                    return Color.parseColor("#F9C31F"); // yellow_500
                case "REJECTED":
                case "CANCELED":
                    return Color.parseColor("#EF4343"); // red_500
                case "ACCEPTED":
                case "IN_PROGRESS":
                    return Color.parseColor("#FFD045"); // yellow_400
                default:
                    return Color.parseColor("#B3B3B3"); // gray_400
            }
        }

        private String formatDate(LocalDateTime dateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US);
            return dateTime.format(formatter);
        }
    }
}
