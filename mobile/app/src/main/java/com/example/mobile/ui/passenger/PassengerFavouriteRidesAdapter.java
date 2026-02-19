package com.example.mobile.ui.passenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import com.example.mobile.Domain.FavouriteRideCard;
import com.example.mobile.R;

import java.util.List;

public class PassengerFavouriteRidesAdapter extends BaseAdapter {

    public interface OnRideActionListener {
        void onRemoveClicked(FavouriteRideCard ride, int position);
        void onRequestClicked(FavouriteRideCard ride, int position);
    }

    private final Context context;
    private final List<FavouriteRideCard> rides;
    private final OnRideActionListener listener;

    public PassengerFavouriteRidesAdapter(Context context,
                                 List<FavouriteRideCard> rides,
                                 OnRideActionListener listener) {
        this.context = context;
        this.rides = rides;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return rides.size();
    }

    @Override
    public Object getItem(int position) {
        return rides.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView tvStart;
        TextView tvEnd;
        AppCompatButton btnRemove;
        AppCompatButton btnRequest;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.row_favourite_ride, parent, false);
            holder = new ViewHolder();
            holder.tvStart = convertView.findViewById(R.id.tv_start);
            holder.tvEnd = convertView.findViewById(R.id.tv_end);
            holder.btnRemove = convertView.findViewById(R.id.btn_remove);
            holder.btnRequest = convertView.findViewById(R.id.btn_request);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FavouriteRideCard ride = rides.get(position);
        holder.tvStart.setText(ride.getStart());
        holder.tvEnd.setText(ride.getEnd());

        holder.btnRemove.setOnClickListener(v ->
                listener.onRemoveClicked(ride, position));

        holder.btnRequest.setOnClickListener(v ->
                listener.onRequestClicked(ride, position));

        return convertView;
    }
}
