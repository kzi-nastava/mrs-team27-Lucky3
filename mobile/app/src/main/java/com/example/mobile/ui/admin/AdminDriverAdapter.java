package com.example.mobile.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile.R;
import com.example.mobile.model.DriverInfoCard;

import java.util.ArrayList;

public class AdminDriverAdapter extends ArrayAdapter<DriverInfoCard> {

    private final ArrayList<DriverInfoCard> drivers;
    private final LayoutInflater inflater;

    public AdminDriverAdapter(@NonNull Context context, ArrayList<DriverInfoCard> resource) {
        super(context, R.layout.driver_card_info, resource);
        this.drivers = resource;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return drivers != null ? drivers.size() : 0;
    }

    @Nullable
    @Override
    public DriverInfoCard getItem(int position) {
        return drivers != null ? drivers.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        // If you have a stable unique ID inside DriverInfoCard, return it here
        return position;
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.driver_card_info, parent, false);

            holder = new ViewHolder();
            holder.ivAvatar = convertView.findViewById(R.id.ivHeaderAvatar);
            holder.tvDriverName = convertView.findViewById(R.id.tvDriverName);
            holder.tvDriverEmail = convertView.findViewById(R.id.tvDriverEmail);
            holder.tvVehicleModel = convertView.findViewById(R.id.tvVehicleModel);
            holder.tvVehiclePlate = convertView.findViewById(R.id.tvVehiclePlate);
            holder.tvStatus1 = convertView.findViewById(R.id.tvStatus1);
            holder.tvStatus2 = convertView.findViewById(R.id.tvStatus2);
            holder.ratingBar = convertView.findViewById(R.id.ratingBar);
            holder.tvTotalRides = convertView.findViewById(R.id.tvTotalRides);
            holder.tvEarnings = convertView.findViewById(R.id.tvEarnings);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DriverInfoCard driver = getItem(position);
        if (driver != null) {
            // Name + surname
            String fullName = driver.getName();
            holder.tvDriverName.setText(fullName);


            // Email
            holder.tvDriverEmail.setText(driver.getEmail() != null
                    ? driver.getEmail()
                    : "");

            // Avatar image
            String imageUrl = driver.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Later: load from URL with Glide/Picasso
                // Glide.with(holder.ivAvatar.getContext())
                //      .load(imageUrl)
                //      .placeholder(R.drawable.avatar)
                //      .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.avatar);
            }

            // Vehicle
            holder.tvVehicleModel.setText(driver.getVehicleModel());
            holder.tvVehiclePlate.setText(driver.getVehicleLicensePlate());

            // Statuses (simple approach: split by comma)
            String status = driver.getStatus(); // e.g. "Active,Online"
            if (status != null && status.contains(",")) {
                String[] parts = status.split(",");
                holder.tvStatus1.setText(parts[0].trim());
                holder.tvStatus1.setVisibility(View.VISIBLE);

                if (parts.length > 1) {
                    holder.tvStatus2.setText(parts[1].trim());
                    holder.tvStatus2.setVisibility(View.VISIBLE);
                } else {
                    holder.tvStatus2.setVisibility(View.GONE);
                }
            } else {
                holder.tvStatus1.setText(status != null ? status : "");
                holder.tvStatus1.setVisibility(View.VISIBLE);
                holder.tvStatus2.setVisibility(View.GONE);
            }

            // Rating (0..5)
            holder.ratingBar.setRating(driver.getRating());

            // Total rides
            holder.tvTotalRides.setText(String.valueOf(driver.getTotalRides()));

            // Earnings, formatted
            holder.tvEarnings.setText("$" + (int) driver.getEarnings());

            // Avatar image:
            // right now you only have imageUrl as String; you can:
            // - use a placeholder: holder.ivAvatar.setImageResource(R.drawable.avatar);
            // - or load URL via Glide/Picasso later.
            holder.ivAvatar.setImageResource(R.drawable.avatar);
        }

        return convertView;
    }

    // ViewHolder pattern for performance
    private static class ViewHolder {
        ImageView ivAvatar;
        TextView tvDriverName;
        TextView tvDriverEmail;
        TextView tvVehicleModel;
        TextView tvVehiclePlate;
        TextView tvStatus1;
        TextView tvStatus2;
        RatingBar ratingBar;
        TextView tvTotalRides;
        TextView tvEarnings;
    }
}
