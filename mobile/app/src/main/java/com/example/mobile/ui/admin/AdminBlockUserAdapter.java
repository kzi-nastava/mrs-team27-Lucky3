package com.example.mobile.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile.R;
import com.example.mobile.models.UserProfile;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AdminBlockUserAdapter extends RecyclerView.Adapter<AdminBlockUserAdapter.ViewHolder> {

    private List<UserProfile> users = new ArrayList<>();
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onBlock(UserProfile user);
        void onUnblock(UserProfile user);
    }

    public AdminBlockUserAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserProfile> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_block_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        TextView tvEmail;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            btnAction = itemView.findViewById(R.id.btn_block_action);
        }

        public void bind(UserProfile user, OnUserActionListener listener) {
            tvName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            
            // Set avatar (placeholder for now)
            ivAvatar.setImageResource(R.drawable.avatar);

            if (user.isBlocked()) {
                btnAction.setText("Unblock");
                btnAction.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.gray_700));
                btnAction.setOnClickListener(v -> listener.onUnblock(user));
            } else {
                btnAction.setText("Block");
                btnAction.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.red_500));
                btnAction.setOnClickListener(v -> listener.onBlock(user));
            }
        }
    }
}
