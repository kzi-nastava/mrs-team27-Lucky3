package com.example.mobile.ui.driver;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.RideResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends BaseAdapter {

    private final List<RideResponse> items;
    private final LayoutInflater inflater;
    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

    public RideHistoryAdapter(Context context, List<RideResponse> items) {
        this.items = items;
        this.inflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RideResponse getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        RideResponse ride = items.get(position);
        return ride.getId() != null ? ride.getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_passenger_ride, parent, false);
            holder = new ViewHolder();
            holder.tvStatus = convertView.findViewById(R.id.tv_status);
            holder.tvDate = convertView.findViewById(R.id.tv_date);
            holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
            holder.tvDestination = convertView.findViewById(R.id.tv_destination);
            holder.tvStartTime = convertView.findViewById(R.id.tv_start_time);
            holder.tvEndTime = convertView.findViewById(R.id.tv_end_time);
            holder.tvCost = convertView.findViewById(R.id.tv_cost);
            holder.tvDriverName = convertView.findViewById(R.id.tv_driver_name);
            holder.tvDistance = convertView.findViewById(R.id.tv_distance);
            holder.btnLeaveReview = convertView.findViewById(R.id.btn_leave_review);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RideResponse ride = getItem(position);

        // Status badge with color coding
        String displayStatus = ride.getDisplayStatus().toUpperCase();
        holder.tvStatus.setText(displayStatus);
        if (ride.isFinished()) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_500));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
        } else if (ride.isCancelled()) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red_500));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled);
        } else if (ride.isInProgress()) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.blue_500));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_blue);
        } else if (ride.isScheduled()) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gray_400));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_gray);
        } else if ("PANIC".equals(ride.getStatus())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red_500));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_panic);
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.yellow_500));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_active);
        }

        // Date
        Date startDate = parseDate(ride.getStartTime());
        if (startDate == null) startDate = parseDate(ride.getScheduledTime());
        holder.tvDate.setText(startDate != null ? dateFormat.format(startDate) : "—");

        // Route
        if (ride.getEffectiveStartLocation() != null && ride.getEffectiveStartLocation().getAddress() != null) {
            holder.tvDeparture.setText(ride.getEffectiveStartLocation().getAddress());
        } else {
            holder.tvDeparture.setText("—");
        }
        if (ride.getEffectiveEndLocation() != null && ride.getEffectiveEndLocation().getAddress() != null) {
            holder.tvDestination.setText(ride.getEffectiveEndLocation().getAddress());
        } else {
            holder.tvDestination.setText("—");
        }

        // Times
        Date endDate = parseDate(ride.getEndTime());
        holder.tvStartTime.setText("Start: " +
                (startDate != null ? timeFormat.format(startDate) : "—"));
        holder.tvEndTime.setText("End: " +
                (endDate != null ? timeFormat.format(endDate) : "—"));

        // Cost
        double cost = ride.getEffectiveCost();
        holder.tvCost.setText(String.format(Locale.US, "%.0f RSD", cost));

        // Driver name: hide in driver's own view
        holder.tvDriverName.setVisibility(View.GONE);

        // Distance + duration
        double distance = ride.getEffectiveDistance();
        String durationStr = "—";
        if (startDate != null && endDate != null) {
            long durationMinutes = (endDate.getTime() - startDate.getTime()) / 60000;
            durationStr = durationMinutes + " min";
        } else if (ride.getEstimatedTimeInMinutes() != null) {
            durationStr = ride.getEstimatedTimeInMinutes() + " min";
        }
        holder.tvDistance.setText(String.format(Locale.US, "%.1f km • %s", distance, durationStr));

        // No review button for driver
        holder.btnLeaveReview.setVisibility(View.GONE);

        // Click handler
        convertView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("rideId", ride.getId() != null ? ride.getId() : 0);
            Navigation.findNavController(v).navigate(R.id.action_nav_driver_overview_to_nav_ride_details, args);
        });

        return convertView;
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

    private static class ViewHolder {
        TextView tvStatus, tvDate, tvDeparture, tvDestination;
        TextView tvStartTime, tvEndTime, tvCost, tvDriverName, tvDistance;
        TextView btnLeaveReview;
    }
}

