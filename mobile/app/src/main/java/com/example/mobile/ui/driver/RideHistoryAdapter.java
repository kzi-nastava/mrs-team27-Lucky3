package com.example.mobile.ui.driver;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.navigation.Navigation;

import com.example.mobile.R;

import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends BaseAdapter {

    private final List<RideHistoryItem> items;
    private final LayoutInflater inflater;

    public RideHistoryAdapter(Context context, List<RideHistoryItem> items) {
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RideHistoryItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).rideId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_ride_history, parent, false);
            holder = new ViewHolder();
            holder.tvStatus = convertView.findViewById(R.id.tv_status);
            holder.tvDate = convertView.findViewById(R.id.tv_date);
            holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
            holder.tvDestination = convertView.findViewById(R.id.tv_destination);
            holder.tvVehicleInfo = convertView.findViewById(R.id.tv_vehicle_info);
            holder.tvCost = convertView.findViewById(R.id.tv_cost);
            holder.tvDriverName = convertView.findViewById(R.id.tv_driver_name);
            holder.tvRejectionReason = convertView.findViewById(R.id.tv_rejection_reason);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RideHistoryItem item = getItem(position);

        // Status badge
        if (item.status != null && !item.status.isEmpty()) {
            holder.tvStatus.setText(item.status);
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Date
        holder.tvDate.setText(item.startDate + " " + item.startTime);

        // Route
        holder.tvDeparture.setText(item.pickupAddress);
        holder.tvDestination.setText(item.dropoffAddress);

        // Vehicle info: show distance and duration
        holder.tvVehicleInfo.setText(item.distance + " • " + item.duration);

        // Cost
        if (holder.tvCost != null) {
            holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", item.price));
        }

        // Driver name row: show end time info
        holder.tvDriverName.setText("End: " + item.endDate + " " + item.endTime);

        // Rejection reason hidden by default
        holder.tvRejectionReason.setVisibility(View.GONE);

        convertView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("rideId", item.rideId);
            Navigation.findNavController(v).navigate(R.id.action_nav_driver_overview_to_nav_ride_details, args);
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView tvStatus;
        TextView tvDate;
        TextView tvDeparture;
        TextView tvDestination;
        TextView tvVehicleInfo;
        TextView tvCost;
        TextView tvDriverName;
        TextView tvRejectionReason;
    }

    public static class RideHistoryItem {
        public long rideId;
        public String status;
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

