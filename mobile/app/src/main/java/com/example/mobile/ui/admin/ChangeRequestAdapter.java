package com.example.mobile.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.mobile.R;
import com.example.mobile.models.DriverChangeRequest;
import com.example.mobile.models.VehicleInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChangeRequestAdapter extends BaseAdapter {

    private final Context context;
    private List<DriverChangeRequest> requests = new ArrayList<>();
    private Set<Long> busyIds = new java.util.HashSet<>();
    private OnActionListener listener;

    public interface OnActionListener {
        void onApprove(Long requestId);
        void onReject(Long requestId);
    }

    public ChangeRequestAdapter(Context context, OnActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setRequests(List<DriverChangeRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    public void setBusyIds(Set<Long> busyIds) {
        this.busyIds = busyIds;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return requests.size();
    }

    @Override
    public DriverChangeRequest getItem(int position) {
        return requests.get(position);
    }

    @Override
    public long getItemId(int position) {
        return requests.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_change_request_card, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DriverChangeRequest request = requests.get(position);
        boolean isBusy = busyIds.contains(request.getId());
        holder.bind(request, isBusy, listener, context);

        return convertView;
    }

    static class ViewHolder {
        TextView tvName, tvSurname, tvEmail, tvPhone, tvAddress;
        TextView tvBabyTransport, tvPetTransport, tvVehicleType;
        TextView tvPassengerSeats, tvLicenseNumber, tvModel;
        ImageView ivDriverImage;
        Button btnApprove, btnReject;

        ViewHolder(View itemView) {
            tvName = itemView.findViewById(R.id.tv_name);
            tvSurname = itemView.findViewById(R.id.tv_surname);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvBabyTransport = itemView.findViewById(R.id.tv_baby_transport);
            tvPetTransport = itemView.findViewById(R.id.tv_pet_transport);
            tvVehicleType = itemView.findViewById(R.id.tv_vehicle_type);
            tvPassengerSeats = itemView.findViewById(R.id.tv_passenger_seats);
            tvLicenseNumber = itemView.findViewById(R.id.tv_license_number);
            tvModel = itemView.findViewById(R.id.tv_model);
            ivDriverImage = itemView.findViewById(R.id.iv_driver_image);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }

        void bind(DriverChangeRequest request, boolean isBusy, OnActionListener listener, Context context) {
            tvName.setText(formatText(request.getName()));
            tvSurname.setText(formatText(request.getSurname()));
            tvEmail.setText(formatText(request.getEmail()));
            tvPhone.setText(formatText(request.getPhone()));
            tvAddress.setText(formatText(request.getAddress()));

            VehicleInformation vehicle = request.getVehicle();
            if (vehicle != null) {
                tvBabyTransport.setText(formatBoolean(vehicle.getBabyTransport()));
                tvPetTransport.setText(formatBoolean(vehicle.getPetTransport()));
                tvVehicleType.setText(formatText(String.valueOf(vehicle.getVehicleType())));
                tvPassengerSeats.setText(formatNumber(vehicle.getPassengerSeats()));
                tvLicenseNumber.setText(formatText(vehicle.getLicenseNumber()));
                tvModel.setText(formatText(vehicle.getModel()));
            }

            // Load driver image
            String imageUrl = "YOUR_BASE_URL/api/images/" + request.getRequestedDriverId();
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivDriverImage);

            btnApprove.setEnabled(!isBusy);
            btnReject.setEnabled(!isBusy);
            btnApprove.setText(isBusy ? "Working…" : "Confirm");

            btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(request.getId());
                }
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(request.getId());
                }
            });
        }

        private String formatText(String text) {
            return (text == null || text.isEmpty()) ? "—" : text;
        }

        private String formatBoolean(Boolean value) {
            if (value == null) return "—";
            return value ? "Yes" : "No";
        }

        private String formatNumber(Integer num) {
            return num == null ? "—" : String.valueOf(num);
        }
    }
}

