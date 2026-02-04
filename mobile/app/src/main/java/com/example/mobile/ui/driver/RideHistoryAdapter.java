package com.example.mobile.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mobile.R;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private final List<RideHistoryItem> items;

    public RideHistoryAdapter(List<RideHistoryItem> items) {
        this.items = items;
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
        RideHistoryItem item = items.get(position);
        holder.startDate.setText(item.startDate);
        holder.startTime.setText(item.startTime);
        holder.endDate.setText(item.endDate);
        holder.endTime.setText(item.endTime);
        holder.pickupAddress.setText(item.pickupAddress);
        holder.dropoffAddress.setText(item.dropoffAddress);
        holder.passengerCount.setText(String.valueOf(item.passengerCount));
        holder.distance.setText(item.distance);
        holder.duration.setText(item.duration);
        
        // Set price
        if (holder.price != null) {
            holder.price.setText(String.format(Locale.US, "%.2f RSD", item.price));
        }

        holder.itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("rideId", item.rideId);
            Navigation.findNavController(v).navigate(R.id.action_nav_driver_dashboard_to_nav_ride_details, args);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView startDate;
        public final TextView startTime;
        public final TextView endDate;
        public final TextView endTime;
        public final TextView pickupAddress;
        public final TextView dropoffAddress;
        public final TextView passengerCount;
        public final TextView distance;
        public final TextView duration;
        public final TextView price;

        public ViewHolder(View view) {
            super(view);
            startDate = view.findViewById(R.id.start_date);
            startTime = view.findViewById(R.id.start_time);
            endDate = view.findViewById(R.id.end_date);
            endTime = view.findViewById(R.id.end_time);
            pickupAddress = view.findViewById(R.id.pickup_address);
            dropoffAddress = view.findViewById(R.id.dropoff_address);
            passengerCount = view.findViewById(R.id.passenger_count);
            distance = view.findViewById(R.id.distance);
            duration = view.findViewById(R.id.duration);
            price = view.findViewById(R.id.price);
        }
    }

    public static class RideHistoryItem {
        public long rideId;
        public String startDate;
        public String startTime;
        public String endDate;
        public String endTime;
        public String pickupAddress;
        public String dropoffAddress;
        public int passengerCount;
        public String distance;
        public String duration;
        public double price;

        public RideHistoryItem(String startDate, String startTime, String endDate, String endTime,
                               String pickupAddress, String dropoffAddress, int passengerCount,
                               String distance, String duration) {
            this(0, startDate, startTime, endDate, endTime, pickupAddress, dropoffAddress, 
                 passengerCount, distance, duration, 0);
        }
        
        public RideHistoryItem(long rideId, String startDate, String startTime, String endDate, String endTime,
                               String pickupAddress, String dropoffAddress, int passengerCount,
                               String distance, String duration, double price) {
            this.rideId = rideId;
            this.startDate = startDate;
            this.startTime = startTime;
            this.endDate = endDate;
            this.endTime = endTime;
            this.pickupAddress = pickupAddress;
            this.dropoffAddress = dropoffAddress;
            this.passengerCount = passengerCount;
            this.distance = distance;
            this.duration = duration;
            this.price = price;
        }
    }
}

