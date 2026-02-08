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
            holder.startDate = convertView.findViewById(R.id.start_date);
            holder.startTime = convertView.findViewById(R.id.start_time);
            holder.endDate = convertView.findViewById(R.id.end_date);
            holder.endTime = convertView.findViewById(R.id.end_time);
            holder.pickupAddress = convertView.findViewById(R.id.pickup_address);
            holder.dropoffAddress = convertView.findViewById(R.id.dropoff_address);
            holder.passengerCount = convertView.findViewById(R.id.passenger_count);
            holder.distance = convertView.findViewById(R.id.distance);
            holder.duration = convertView.findViewById(R.id.duration);
            holder.price = convertView.findViewById(R.id.price);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RideHistoryItem item = getItem(position);

        holder.startDate.setText(item.startDate);
        holder.startTime.setText(item.startTime);
        holder.endDate.setText(item.endDate);
        holder.endTime.setText(item.endTime);
        holder.pickupAddress.setText(item.pickupAddress);
        holder.dropoffAddress.setText(item.dropoffAddress);
        holder.passengerCount.setText(String.valueOf(item.passengerCount));
        holder.distance.setText(item.distance);
        holder.duration.setText(item.duration);

        if (holder.price != null) {
            holder.price.setText(String.format(Locale.US, "%.2f RSD", item.price));
        }

        convertView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("rideId", item.rideId);
            Navigation.findNavController(v).navigate(R.id.action_nav_driver_dashboard_to_nav_ride_details, args);
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView startDate;
        TextView startTime;
        TextView endDate;
        TextView endTime;
        TextView pickupAddress;
        TextView dropoffAddress;
        TextView passengerCount;
        TextView distance;
        TextView duration;
        TextView price;
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

