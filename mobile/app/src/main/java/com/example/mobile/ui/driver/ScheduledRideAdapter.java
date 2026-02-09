package com.example.mobile.ui.driver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.mobile.R;
import com.example.mobile.models.RideResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying scheduled rides in a ListView.
 * Uses the BaseAdapter / convertView / ViewHolder pattern per project conventions.
 */
public class ScheduledRideAdapter extends BaseAdapter {

    private final Context context;
    private final List<RideResponse> rides;
    private OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(RideResponse ride, int position);
    }

    public ScheduledRideAdapter(Context context, List<RideResponse> rides) {
        this.context = context;
        this.rides = rides;
    }

    public void setOnCancelClickListener(OnCancelClickListener listener) {
        this.cancelClickListener = listener;
    }

    @Override
    public int getCount() {
        return rides.size();
    }

    @Override
    public RideResponse getItem(int position) {
        return rides.get(position);
    }

    @Override
    public long getItemId(int position) {
        RideResponse ride = rides.get(position);
        return ride.getId() != null ? ride.getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_scheduled_ride, parent, false);
            holder = new ViewHolder();
            holder.time = convertView.findViewById(R.id.scheduled_time);
            holder.status = convertView.findViewById(R.id.scheduled_status);
            holder.route = convertView.findViewById(R.id.scheduled_route);
            holder.passenger = convertView.findViewById(R.id.scheduled_passenger);
            holder.price = convertView.findViewById(R.id.scheduled_price);
            holder.cancelBtn = convertView.findViewById(R.id.btn_cancel_scheduled);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RideResponse ride = getItem(position);
        bindRide(holder, ride, position);
        return convertView;
    }

    private void bindRide(ViewHolder holder, RideResponse ride, int position) {
        // Scheduled time
        String scheduledTime = formatScheduledTime(ride.getScheduledTime());
        holder.time.setText(scheduledTime);

        // Status
        String status = ride.getStatus() != null ? ride.getStatus() : "SCHEDULED";
        holder.status.setText(status);

        // Route
        String from = "—";
        String to = "—";
        if (ride.getEffectiveStartLocation() != null
                && ride.getEffectiveStartLocation().getAddress() != null) {
            from = truncate(ride.getEffectiveStartLocation().getAddress(), 25);
        }
        if (ride.getEffectiveEndLocation() != null
                && ride.getEffectiveEndLocation().getAddress() != null) {
            to = truncate(ride.getEffectiveEndLocation().getAddress(), 25);
        }
        holder.route.setText(from + " → " + to);

        // Passenger
        if (ride.getPassengers() != null && !ride.getPassengers().isEmpty()) {
            RideResponse.PassengerInfo firstPassenger = ride.getPassengers().get(0);
            String name = firstPassenger.getFullName();
            int count = ride.getPassengers().size();
            holder.passenger.setText(count > 1 ? name + " + " + (count - 1) + " more" : name);
        } else {
            holder.passenger.setText("Unknown passenger");
        }

        // Price
        double price = ride.getEffectiveCost();
        holder.price.setText(String.format(Locale.US, "~ %.2f RSD", price));

        // Cancel button
        holder.cancelBtn.setOnClickListener(v -> {
            if (cancelClickListener != null) {
                cancelClickListener.onCancelClick(ride, position);
            }
        });
    }

    private String formatScheduledTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Scheduled";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = parser.parse(dateStr);
            if (date == null) return dateStr;

            Calendar now = Calendar.getInstance();
            Calendar scheduled = Calendar.getInstance();
            scheduled.setTime(date);

            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

            if (now.get(Calendar.YEAR) == scheduled.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == scheduled.get(Calendar.DAY_OF_YEAR)) {
                return "Today, " + timeFormat.format(date);
            }

            now.add(Calendar.DAY_OF_YEAR, 1);
            if (now.get(Calendar.YEAR) == scheduled.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == scheduled.get(Calendar.DAY_OF_YEAR)) {
                return "Tomorrow, " + timeFormat.format(date);
            }

            SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.US);
            return fullFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    static class ViewHolder {
        TextView time;
        TextView status;
        TextView route;
        TextView passenger;
        TextView price;
        View cancelBtn;
    }
}
